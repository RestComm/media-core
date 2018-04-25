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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.core.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.core.control.mgcp.pkg.au.ReturnCode;
import org.restcomm.media.core.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.core.control.mgcp.pkg.au.asr.AsrSignal;
import org.restcomm.media.core.resource.dtmf.DtmfEvent;
import org.restcomm.media.core.spi.listener.TooManyListenersException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AsrSignalDtmfSpeechTest extends AsrSignalBaseTest {

    @Before
    public void before() throws TooManyListenersException {
        super.before();
    }

    @After
    public void after() {
        super.after();
    }

    @Test
    public void testCollectSpeechWithEndInputKey() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speakRecognizedText("text");
        detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        waitForFinalResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), firstEvent.getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(firstEvent.getParameter("asrr").toCharArray())));
        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
    }

    @Test
    public void testCollectWithEndInputKeyAndResponseAfterIt() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        Thread.sleep(EPSILON_IN_MILLISECONDS);

        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, never()).onEvent(eq(asr), eventCaptor.capture());

        speechDetectorListener.onVoiceActivityDetected();
        detectorObserver.onDtmfEvent(new DtmfEvent("#"));
        asrEngineListener.onSpeechRecognized("text", true);

        waitForFinalResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), firstEvent.getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(firstEvent.getParameter("asrr").toCharArray())));
        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
    }

    @Test
    public void testAsrCancelNoResult() throws InterruptedException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

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
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speakRecognizedText("text");
        asr.cancel();

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), firstEvent.getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(firstEvent.getParameter("asrr").toCharArray())));
        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
    }

    @Test
    public void testFirstInputTimeout() throws InterruptedException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        Thread.sleep(MRT_IN_MILLISECONDS + EPSILON_IN_MILLISECONDS);

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), firstEvent.getParameter("rc"));
    }

    @Test
    public void testMrtNoResult() throws InterruptedException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speechDetectorListener.onVoiceActivityDetected();

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        
        // when
        speakUnrecognizedText(MRT_IN_MILLISECONDS);
        
        // then
        verify(player, never()).activate();
        verify(observer, never()).onEvent(eq(asr), eventCaptor.capture());
        
        // when
        waitForFinalResponse();

        // then
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());
        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), firstEvent.getParameter("rc"));
    }

    @Test
    public void testMrtWithResult() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speakRecognizedText("text");
        
        Thread.sleep(MRT_IN_MILLISECONDS + EPSILON_IN_MILLISECONDS);

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), firstEvent.getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(firstEvent.getParameter("asrr").toCharArray())));

        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
    }

    @Test
    public void testPstWithResult() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speakRecognizedText("text");
        Thread.sleep(PST_IN_MILLISECONDS + EPSILON_IN_MILLISECONDS);

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), firstEvent.getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(firstEvent.getParameter("asrr").toCharArray())));
        
        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
    }

    @Test
    public void testCollectDigitsWithEndInputKey() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        parameters.put(SignalParameters.MINIMUM_NUM_DIGITS.symbol(), "1");
        parameters.put(SignalParameters.MAXIMUM_NUM_DIGITS.symbol(), "3");

        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speakRecognizedText("text");

        detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        waitForInterimResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), firstEvent.getParameter("rc"));
        assertEquals("text", new String(Hex.decodeHex(firstEvent.getParameter("asrr").toCharArray())));
        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
        assertEquals("12", secondEvent.getParameter("dc"));
    }

    @Test
    public void testCollectSpeechAfterDigitsCollectingStarted() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(true);
        parameters.put(SignalParameters.MINIMUM_NUM_DIGITS.symbol(), "1");
        parameters.put(SignalParameters.MAXIMUM_NUM_DIGITS.symbol(), "2");

        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speakRecognizedText("first text");

        detectorObserver.onDtmfEvent(new DtmfEvent("1"));

        speakRecognizedText("second text");
        speakRecognizedText("third text");

        waitForInterimResponse();

        detectorObserver.onDtmfEvent(new DtmfEvent("2"));

        waitForInterimResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("first text", new String(Hex.decodeHex(events.get(0).getParameter("asrr").toCharArray())));
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(1).getParameter("rc"));
        assertEquals("12", events.get(1).getParameter("dc"));
    }

    @Test
    public void testPstForDigits() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateDtmfSpeechTestParameters(false);
        parameters.put(SignalParameters.MINIMUM_NUM_DIGITS.symbol(), "1");
        parameters.put(SignalParameters.MAXIMUM_NUM_DIGITS.symbol(), "2");

        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        detectorObserver.onDtmfEvent(new DtmfEvent("9"));

        Thread.sleep(PST_IN_MILLISECONDS + EPSILON_IN_MILLISECONDS);

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());

        List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals("9", events.get(0).getParameter("dc"));
    }

}
