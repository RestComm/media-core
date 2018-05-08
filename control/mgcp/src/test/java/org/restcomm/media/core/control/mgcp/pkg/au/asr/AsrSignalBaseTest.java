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

package org.restcomm.media.core.control.mgcp.pkg.au.asr;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.core.asr.AsrEngine;
import org.restcomm.media.core.asr.AsrEngineListener;
import org.restcomm.media.core.control.mgcp.pkg.MgcpEventObserver;
import org.restcomm.media.core.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.core.resource.vad.VoiceActivityDetectorListener;
import org.restcomm.media.core.resource.dtmf.detector.DtmfSinkFacade;
import org.restcomm.media.core.resource.dtmf.detector.DtmfEventObserver;
import org.restcomm.media.core.spi.listener.TooManyListenersException;
import org.restcomm.media.core.spi.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author anikiforov
 *
 */
public abstract class AsrSignalBaseTest {

    private static final Logger logger = LogManager.getLogger(AsrSignalBaseTest.class);

    private static final int WIT_IN_MILLISECONDS = 10000;
    private static final String WIT_PARAM = Integer.toString(WIT_IN_MILLISECONDS / 100);
    protected static final int PST_IN_MILLISECONDS = 5000;
    private static final String PST_PARAM = Integer.toString(PST_IN_MILLISECONDS / 100);
    protected static final int MRT_IN_MILLISECONDS = 30000;
    private static final String MRT_PARAM = Integer.toString(MRT_IN_MILLISECONDS / 100);
    protected static final int RESPONSE_TIMEOUT_IN_MILLISECONDS = 1000;
    protected static final int EPSILON_IN_MILLISECONDS = 10;

    private ScheduledExecutorService threadPool;
    protected Player player;
    protected DtmfSinkFacade detector;
    protected AsrEngine asrEngine;

    private ListeningScheduledExecutorService executor;
    protected MgcpEventObserver observer;
    protected VoiceActivityDetectorListener speechDetectorListener;
    protected AsrEngineListener asrEngineListener;
    protected DtmfEventObserver detectorObserver;

    protected void before() throws TooManyListenersException {
        threadPool = Executors.newScheduledThreadPool(5);
        player = mock(Player.class);
        detector = mock(DtmfSinkFacade.class);
        asrEngine = mock(AsrEngine.class);
        when(asrEngine.getResponseTimeoutInMilliseconds()).thenReturn(RESPONSE_TIMEOUT_IN_MILLISECONDS);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                speechDetectorListener = invocation.getArgumentAt(0, VoiceActivityDetectorListener.class);
                return null;
            }
        }).when(asrEngine).startSpeechDetection(any(VoiceActivityDetectorListener.class));
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
                asrEngineListener = invocation.getArgumentAt(0, AsrEngineListener.class);
                return null;
            }
        }).when(asrEngine).setListener(any(AsrEngineListener.class));
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                detectorObserver = invocation.getArgumentAt(0, DtmfEventObserver.class);
                return null;
            }
        }).when(detector).observe(any(DtmfEventObserver.class));
        executor = MoreExecutors.listeningDecorator(threadPool);
        observer = mock(MgcpEventObserver.class);
    }

    protected void after() {
        threadPool.shutdown();
    }

    protected static Map<String, String> generateTestParameters() {
        final Map<String, String> parameters = new HashMap<>(6);
        parameters.put(SignalParameters.DRIVER.symbol(), "stub");
        parameters.put(SignalParameters.END_INPUT_KEY.symbol(), "#");
        parameters.put(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol(), MRT_PARAM);
        parameters.put(SignalParameters.WAITING_TIME_FOR_INPUT.symbol(), WIT_PARAM);
        parameters.put(SignalParameters.POST_SPEECH_TIMER.symbol(), PST_PARAM);
        return parameters;
    }

    protected static Map<String, String> generateTestParameters(final boolean partialResult) {
        final Map<String, String> parameters = generateTestParameters();
        parameters.put(SignalParameters.PARTIAL_RESULT.symbol(), (partialResult ? "true" : "false"));
        return parameters;
    }

    protected static Map<String, String> generateDtmfSpeechTestParameters(final boolean partialResult) {
        final Map<String, String> parameters = generateTestParameters(partialResult);
        parameters.put(SignalParameters.INPUT.symbol(), "DTMF_SPEECH");
        return parameters;
    }

    protected AsrSignal generateAsrSignal(final Map<String, String> parameters) {
        return new AsrSignal(player, detector, asrEngine, 1, null, parameters, executor);
    }

    protected void waitForFinalResponse() throws InterruptedException {
        verify(asrEngine, timeout(100)).stopSpeechDetection();
        Thread.sleep(RESPONSE_TIMEOUT_IN_MILLISECONDS / 2);
        verify(asrEngine, never()).deactivate();
        Thread.sleep(RESPONSE_TIMEOUT_IN_MILLISECONDS / 2 + EPSILON_IN_MILLISECONDS);
        verify(asrEngine, times(1)).stopSpeechDetection();
        verify(asrEngine, times(1)).deactivate();
    }

    protected void waitForInterimResponse() throws InterruptedException {
        Thread.sleep(RESPONSE_TIMEOUT_IN_MILLISECONDS + EPSILON_IN_MILLISECONDS);
    }

    protected void speakRecognizedText(final String text) {
        speechDetectorListener.onVoiceActivityDetected();
        asrEngineListener.onSpeechRecognized(text, true);
    }

    protected void speakUnrecognizedText(final long durationInMilliseconds) throws InterruptedException {
        final long startTimestampInMilliseconds = System.currentTimeMillis();
        final long finishTimestampInMilliseconds = startTimestampInMilliseconds + durationInMilliseconds;
        do {
            if (speechDetectorListener != null) {
                speechDetectorListener.onVoiceActivityDetected();
            } else {
                logger.info("speechDetectorListener is null");
            }
            Thread.sleep(EPSILON_IN_MILLISECONDS);
        } while (System.currentTimeMillis() < finishTimestampInMilliseconds);
    }

}
