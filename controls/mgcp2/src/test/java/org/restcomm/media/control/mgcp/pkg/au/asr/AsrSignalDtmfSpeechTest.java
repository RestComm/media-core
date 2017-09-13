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

package org.restcomm.media.control.mgcp.pkg.au.asr;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.asr.AsrEngine;
import org.restcomm.media.asr.AsrEngineListener;
import org.restcomm.media.asr.SpeechDetectorListener;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;
import org.restcomm.media.control.mgcp.pkg.au.ReturnCode;
import org.restcomm.media.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.resource.dtmf.DtmfEventImpl;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.listener.TooManyListenersException;
import org.restcomm.media.spi.player.Player;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AsrSignalDtmfSpeechTest {

    private static final Logger log = Logger.getLogger(AsrSignalDtmfSpeechTest.class);

    private static final int RESPONSE_TIMEOUT_IN_MILLISECONDS = 100;
    private static final int EPSILON_IN_MILLISECONDS = 10;

    private ScheduledExecutorService threadPool;
    private Player player;
    private DtmfDetector detector;
    private AsrEngine asrEngine;

    private ListeningScheduledExecutorService executor;
    private MgcpEventObserver observer;
    private SpeechDetectorListener speechDetectorListener;
    private AsrEngineListener asrEngineListener;
    private DtmfDetectorListener detectorListener;

    @Before
    public void before() throws TooManyListenersException {
        threadPool = Executors.newScheduledThreadPool(5);
        player = mock(Player.class);
        detector = mock(DtmfDetector.class);
        asrEngine = mock(AsrEngine.class);
        when(asrEngine.getResponseTimeoutInMilliseconds()).thenReturn(RESPONSE_TIMEOUT_IN_MILLISECONDS);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                speechDetectorListener = invocation.getArgumentAt(0, SpeechDetectorListener.class);
                return null;
            }
        }).when(asrEngine).startSpeechDetection(any(SpeechDetectorListener.class));
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                speechDetectorListener = null;
                return null;
            }
        }).when(asrEngine).stopSpeechDetection();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final AsrEngineListener newListener = invocation.getArgumentAt(0, AsrEngineListener.class);
                asrEngineListener = newListener;
                return null;
            }
        }).when(asrEngine).setListener(any(AsrEngineListener.class));
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                detectorListener = invocation.getArgumentAt(0, DtmfDetectorListener.class);
                return null;
            }
        }).when(detector).addListener(any(DtmfDetectorListener.class));
        executor = MoreExecutors.listeningDecorator(threadPool);
        observer = mock(MgcpEventObserver.class);
    }

    @After
    public void after() {
        threadPool.shutdown();
    }

    @Test
    public void testCollectSpeechWithEndInputKey() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "60");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "10");
        parameters.put(SignalParameters.POST_SPEECH_TIMER.symbol(), "10");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), "true");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        asrEngineListener.onSpeechRecognized("text", true);
        detectorListener.process(new DtmfEventImpl(detector, "#", -30));

        waitForResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(events.get(0).getParameter("asrr").toCharArray())));
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(1).getParameter("rc"));
    }

    @Test
    public void testCollectWithEndInputKeyAndResponseAfterIt() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "60");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "10");
        parameters.put(SignalParameters.POST_SPEECH_TIMER.symbol(), "10");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), "true");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        Thread.sleep(EPSILON_IN_MILLISECONDS);

        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, never()).onEvent(eq(asr), eventCaptor.capture());

        speechDetectorListener.onSpeechDetected();
        detectorListener.process(new DtmfEventImpl(detector, "#", -30));
        asrEngineListener.onSpeechRecognized("text", true);

        waitForResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(events.get(0).getParameter("asrr").toCharArray())));
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(1).getParameter("rc"));
    }

    @Test
    public void testAsrCancelNoResult() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "60");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "30");
        parameters.put(SignalParameters.POST_SPEECH_TIMER.symbol(), "10");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), "true");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        asr.cancel();

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testAsrCancelWithResult() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "60");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "30");
        parameters.put(SignalParameters.POST_SPEECH_TIMER.symbol(), "10");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), "true");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        asrEngineListener.onSpeechRecognized("text", true);
        asr.cancel();

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(events.get(0).getParameter("asrr").toCharArray())));

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(1).getParameter("rc"));
    }

    @Test
    public void testFirstInputTimeout() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "3");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), "true");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        asr.getInputTimeoutDetectorListener().onPreSpeechTimer();

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), events.get(0).getParameter("rc"));
    }

    @Test
    public void testMrtNoResult() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "3");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "3");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), "true");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        asr.getInputTimeoutDetectorListener().onMaximumRecognitionTime();

        waitForResponse();

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), events.get(0).getParameter("rc"));
    }

    @Test
    public void testMrtWithResult() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "3");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "3");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), "true");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        asrEngineListener.onSpeechRecognized("text", true);

        asr.getInputTimeoutDetectorListener().onMaximumRecognitionTime();

        waitForResponse();

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(events.get(0).getParameter("asrr").toCharArray())));

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(1).getParameter("rc"));
    }

    @Test
    public void testPstWithResult() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "3");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "3");
        parameters.put(SignalParameters.POST_SPEECH_TIMER.symbol(), "3");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), "true");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        asrEngineListener.onSpeechRecognized("text", true);
        asr.getInputTimeoutDetectorListener().onPostSpeechTimer();

        waitForResponse();
        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        waitForResponse();

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(events.get(0).getParameter("asrr").toCharArray())));

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(1).getParameter("rc"));
    }

    @Ignore
    @Test
    public void testCollectDigitsWithEndInputKey() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "60");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "10");
        parameters.put(SignalParameters.POST_SPEECH_TIMER.symbol(), "10");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.MINIMUM_NUM_DIGITS.symbol(), "1");
        parameters.put(SignalParameters.MAXIMUM_NUM_DIGITS.symbol(), "3");
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), "true");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        asrEngineListener.onSpeechRecognized("text", true);

        log.info("We are sending '1'");
        detectorListener.process(new DtmfEventImpl(detector, "1", -30));
        log.info("We sent '1'");
        log.info("We are sending '2'");
        detectorListener.process(new DtmfEventImpl(detector, "2", -30));
        log.info("We sent '2'");
        detectorListener.process(new DtmfEventImpl(detector, "#", -30));

        waitForResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(events.get(0).getParameter("asrr").toCharArray())));
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(1).getParameter("rc"));
        assertEquals("12", new String(events.get(1).getParameter("dc")));
    }

    @Ignore
    @Test
    public void testCollectSpeechAfterDigitsCollectingStarted() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "60");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "10");
        parameters.put(SignalParameters.POST_SPEECH_TIMER.symbol(), "10");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.MINIMUM_NUM_DIGITS.symbol(), "1");
        parameters.put(SignalParameters.MAXIMUM_NUM_DIGITS.symbol(), "2");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        asrEngineListener.onSpeechRecognized("first text", true);

        log.info("We are sending '1'");
        detectorListener.process(new DtmfEventImpl(detector, "1", -30));
        log.info("We sent '1'");
        asrEngineListener.onSpeechRecognized("second text", true); // will be ignored
        asrEngineListener.onSpeechRecognized("third text", true); // will be ignored

        waitForResponse();

        log.info("We are sending '2'");
        detectorListener.process(new DtmfEventImpl(detector, "2", -30));
        log.info("We sent '2'");

        waitForResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("first text", new String(Hex.decodeHex(events.get(0).getParameter("asrr").toCharArray())));
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(1).getParameter("rc"));
        assertEquals("12", new String(events.get(1).getParameter("dc")));
    }

    @Ignore
    @Test
    public void testPstForDigits() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), "3");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), "3");
        parameters.put(SignalParameters.POST_SPEECH_TIMER.symbol(), "3");
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF SPEECH");
        parameters.put(SignalParameters.MINIMUM_NUM_DIGITS.symbol(), "1");
        parameters.put(SignalParameters.MAXIMUM_NUM_DIGITS.symbol(), "2");

        final AsrSignal asr = new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        log.info("We are sending '9'");
        detectorListener.process(new DtmfEventImpl(detector, "9", -30));
        log.info("We sent '9'");

        Thread.sleep(3 * 100 + EPSILON_IN_MILLISECONDS);
        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("9", new String(events.get(0).getParameter("dc")));
    }

    private void waitForResponse() throws InterruptedException {
        Thread.sleep(RESPONSE_TIMEOUT_IN_MILLISECONDS + EPSILON_IN_MILLISECONDS);
    }

}
