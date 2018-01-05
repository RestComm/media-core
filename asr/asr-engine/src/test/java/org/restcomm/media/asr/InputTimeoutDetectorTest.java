/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.asr;

import static com.google.common.primitives.Ints.min;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author anikiforov
 */
public class InputTimeoutDetectorTest {

    private final static Logger logger = LogManager.getLogger(InputTimeoutDetectorTest.class);

    private ListeningScheduledExecutorService realExecutor;
    private ListeningScheduledExecutorService executor;
    private InputTimeoutDetector testee;
    TreadSafeMock listener;
    ExpectedSpeechDetectorListenerCounter expectedListenerCounter;
    ScheduledDetectorCounter scheduledDetectorCounter;

    private static final int FIRST_INPUT_TIMEOUT = 250;
    private static final int NEXT_INPUT_TIMEOUT = 1250;
    private static final int MAX_INPUT_TIMEOUT = 5000;
    private static final int EPSILON = min(FIRST_INPUT_TIMEOUT, NEXT_INPUT_TIMEOUT, MAX_INPUT_TIMEOUT) / 10 + 1;

    @Before
    public void setUp() {
        realExecutor = MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(1));
        executor = mock(ListeningScheduledExecutorService.class);
        doAnswer(new Answer<ListenableScheduledFuture<?>>() {
            @Override
            public ListenableScheduledFuture<?> answer(InvocationOnMock invocation) throws Throwable {
                final Runnable detector = invocation.getArgumentAt(0, Runnable.class);
                scheduledDetectorCounter.onDetectorScheduled(detector);
                return realExecutor.schedule(detector, invocation.getArgumentAt(1, Long.class),
                        invocation.getArgumentAt(2, TimeUnit.class));
            }
        }).when(executor).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

        testee = new InputTimeoutDetector(executor);
        listener = new TreadSafeMock();
        expectedListenerCounter = new ExpectedSpeechDetectorListenerCounter();
        scheduledDetectorCounter = new ScheduledDetectorCounter();

        testee.startSession(listener, FIRST_INPUT_TIMEOUT, MAX_INPUT_TIMEOUT, NEXT_INPUT_TIMEOUT);
        verify(executor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @After
    public void cleanUp() {
        testee.stopSession();
        scheduledDetectorCounter = null;
        expectedListenerCounter = null;
        listener = null;
        testee = null;
        executor = null;
        realExecutor = null;
    }

    @Test
    public void testFirstInputTimeout() throws InterruptedException {
        startWaitingForFirstInput(true);
        final int timeToSleep = getSlightlyMoreThanHalf(FIRST_INPUT_TIMEOUT);
        expectedListenerCounter.verifyListener();
        doNothing(timeToSleep);
        expectedListenerCounter.verifyListener();
        doNothing(timeToSleep);
        expectedListenerCounter.incrementExpectedPreSpeechTimerCounter();
        expectedListenerCounter.verifyListener();
    }

    @Test
    public void testNextInputTimeout() throws InterruptedException {
        startWaitingForFirstInput(true);
        expectedListenerCounter.verifyListener();
        speakSomething(getSlightlyMoreThanHalf(MAX_INPUT_TIMEOUT));
        expectedListenerCounter.verifyListener();
        doNothing(NEXT_INPUT_TIMEOUT + EPSILON);
        expectedListenerCounter.incrementPostSpeechTimerCounter();
        expectedListenerCounter.verifyListener();
    }

    @Test
    public void testMaxInputTimeout() throws InterruptedException {
        startWaitingForFirstInput(true);
        final int timeToSpeak = getSlightlyMoreThanHalf(MAX_INPUT_TIMEOUT);
        expectedListenerCounter.verifyListener();
        speakSomething(timeToSpeak);
        expectedListenerCounter.verifyListener();
        speakSomething(timeToSpeak);
        expectedListenerCounter.incrementMaximumRecognitionTimeCounter();
        expectedListenerCounter.verifyListener();
    }

    @Test
    public void testSpeechBeforeWaitingForFirstInput() throws InterruptedException {
        testee.processInput();
        verify(executor, times(2)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        assertEquals(0, scheduledDetectorCounter.getFirstInputTimeoutDetectorCounter());
        assertEquals(1, scheduledDetectorCounter.getNextInputTimeoutDetectorCounter());
        assertEquals(1, scheduledDetectorCounter.getMaxInputTimeoutDetectorCounter());
    }

    private class ExpectedSpeechDetectorListenerCounter {
        private int expectedPreSpeechTimerCounter = 0;
        private int expectedPostSpeechTimerCounter = 0;
        private int expectedMaximumRecognitionTimeCounter = 0;

        public void incrementExpectedPreSpeechTimerCounter() {
            expectedPreSpeechTimerCounter++;
        }

        public void incrementPostSpeechTimerCounter() {
            expectedPostSpeechTimerCounter++;
        }

        public void incrementMaximumRecognitionTimeCounter() {
            expectedMaximumRecognitionTimeCounter++;
        }

        public void verifyListener() {
            assertEquals(expectedPreSpeechTimerCounter, listener.onPreSpeechTimerCounter);
            assertEquals(expectedPostSpeechTimerCounter, listener.onPostSpeechTimerCounter);
            assertEquals(expectedMaximumRecognitionTimeCounter, listener.onMaximumRecognitionTimeCounter);
        }
    }

    private class ScheduledDetectorCounter {
        private int firstInputTimeoutDetectorCounter = 0;
        private int nextInputTimeoutDetectorCounter = 0;
        private int maxInputTimeoutDetector = 0;

        public int getFirstInputTimeoutDetectorCounter() {
            return firstInputTimeoutDetectorCounter;
        }

        public int getNextInputTimeoutDetectorCounter() {
            return nextInputTimeoutDetectorCounter;
        }

        public int getMaxInputTimeoutDetectorCounter() {
            return maxInputTimeoutDetector;
        }

        public void onDetectorScheduled(final Runnable detector) {
            if (detector instanceof InputTimeoutDetector.FirstInputTimeoutDetector) {
                firstInputTimeoutDetectorCounter++;
            } else if (detector instanceof InputTimeoutDetector.NextInputTimeoutDetector) {
                nextInputTimeoutDetectorCounter++;
            } else if (detector instanceof InputTimeoutDetector.MaxInputTimeoutDetector) {
                maxInputTimeoutDetector++;
            } else {
                logger.warn("Unexpected detector class: " + detector.getClass());
            }
        }

        @Override
        public String toString() {
            return "firstInputTimeoutDetectorCounter=" + firstInputTimeoutDetectorCounter + ", nextInputTimeoutDetectorCounter="
                    + nextInputTimeoutDetectorCounter + ", maxInputTimeoutDetector=" + maxInputTimeoutDetector;
        }
    }

    private static class TreadSafeMock implements InputTimeoutListener {
        volatile int onPreSpeechTimerCounter = 0;
        volatile int onPostSpeechTimerCounter = 0;
        volatile int onMaximumRecognitionTimeCounter = 0;

        @Override
        public void onPreSpeechTimer() {
            onPreSpeechTimerCounter++;
        }

        @Override
        public void onPostSpeechTimer() {
            onPostSpeechTimerCounter++;
        }

        @Override
        public void onMaximumRecognitionTime() {
            onMaximumRecognitionTimeCounter++;
        }
    }

    private static int getSlightlyMoreThanHalf(final int value) {
        return value / 2 + EPSILON;
    }

    private void startWaitingForFirstInput(final boolean shouldInputTimeoutDetectorBeStarted) {
        testee.startWaitingForFirstInput();
        assertEquals((shouldInputTimeoutDetectorBeStarted ? 1 : 0),
                this.scheduledDetectorCounter.getFirstInputTimeoutDetectorCounter());
    }

    private void speakSomething(final int duration) throws InterruptedException {
        final long start = getCurrentTime();
        final long finish = start + TimeUnit.NANOSECONDS.convert(duration, TimeUnit.MILLISECONDS);
        do {
            testee.processInput();
            Thread.sleep(EPSILON);
            testee.processInput();
        } while (getCurrentTime() < finish);
    }

    private void doNothing(final int duration) throws InterruptedException {
        final long start = getCurrentTime();
        final long finish = start + TimeUnit.NANOSECONDS.convert(duration, TimeUnit.MILLISECONDS);
        do {
            Thread.sleep(EPSILON);
        } while (getCurrentTime() < finish);
    }

    private long getCurrentTime() {
        return System.nanoTime();
    }

}