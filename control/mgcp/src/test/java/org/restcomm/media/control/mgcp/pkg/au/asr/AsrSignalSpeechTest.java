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
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.au.ReturnCode;
import org.restcomm.media.control.mgcp.pkg.au.ReturnParameters;
import org.restcomm.media.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.core.resource.dtmf.DtmfEventImpl;
import org.restcomm.media.core.spi.listener.TooManyListenersException;
import org.restcomm.media.drivers.asr.AsrDriverException;
import org.restcomm.media.drivers.asr.UnknownAsrDriverException;

/**
 * @author gdubina
 * @author anikiforov
 */
public class AsrSignalSpeechTest extends AsrSignalBaseTest {

    @Before
    public void before() throws TooManyListenersException {
        super.before();
    }

    @After
    public void after() {
        super.after();
    }

    @Test
    public void testCollectWithIncludeEndInputKey() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        detectorListener.process(new DtmfEventImpl(detector, "1", -30)); // should be ignored
        speakRecognizedText("one");
        speakRecognizedText("two");
        speakRecognizedText("three");
        detectorListener.process(new DtmfEventImpl(detector, "#", -30));

        waitForFinalResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(4)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), firstEvent.getParameter("rc"));
        assertEquals("one", getArsResult(firstEvent));
        final MgcpEvent secondEvent = events.get(1);
        assertEquals("two", getArsResult(secondEvent));
        final MgcpEvent thirdEvent = events.get(2);
        assertEquals("three", getArsResult(thirdEvent));
        final MgcpEvent fourthEvent = events.get(3);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), fourthEvent.getParameter("rc"));
        assertEquals("one" + System.lineSeparator() + "two" + System.lineSeparator() + "three", getArsResult(fourthEvent));
    }

    @Test
    public void testCollectWithEndInputKeyAndResponseAfterIt() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateTestParameters(true);
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
        detectorListener.process(new DtmfEventImpl(detector, "1", -30)); // should be ignored
        detectorListener.process(new DtmfEventImpl(detector, "#", -30));
        asrEngineListener.onSpeechRecognized("text", true);

        waitForFinalResponse();

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), firstEvent.getParameter("rc"));
        assertEquals("text", getArsResult(firstEvent));
        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
        assertEquals("text", getArsResult(secondEvent));
    }

    @Test
    public void testCollectWithUnknownDriver() throws Exception {
        // given
        final Map<String, String> parameters = generateTestParameters(true);

        doThrow(new UnknownAsrDriverException("null")).when(asrEngine).configure(anyString(), anyString(), anyListOf(String.class));

        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PROVISIONING_ERROR.code()), events.get(0).getParameter("rc"));
    }

    @Test
    public void testCollectWithDriverError() throws Exception {
        // given
        final Map<String, String> parameters = generateTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        Thread.sleep(EPSILON_IN_MILLISECONDS);

        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, never()).onEvent(eq(asr), eventCaptor.capture());

        speakRecognizedText("text");
        asrEngineListener.onDriverError(new AsrDriverException("test purposes"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(2)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        assertEquals(String.valueOf(ReturnCode.PARTIAL_SUCCESS.code()), events.get(0).getParameter("rc"));
        assertEquals(String.valueOf(ReturnCode.PROVISIONING_ERROR.code()), events.get(1).getParameter("rc"));
    }

    @Test
    public void testAsrCancelNoResult() throws InterruptedException {
        // given
        final Map<String, String> parameters = generateTestParameters(true);
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
        final Map<String, String> parameters = generateTestParameters(true);
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
        assertEquals("text", new String(Hex.decodeHex(firstEvent.getParameter(ReturnParameters.ASR_RESULT.symbol()).toCharArray())));
        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
    }

    @Test
    public void testFirstInputTimeout() throws InterruptedException {
        // given
        final Map<String, String> parameters = generateTestParameters(true);
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
        final Map<String, String> parameters = generateTestParameters(true);
        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speechDetectorListener.onVoiceActivityDetected();

        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();

        speakUnrecognizedText(MRT_IN_MILLISECONDS);

        verify(observer, never()).onEvent(eq(asr), eventCaptor.capture());

        waitForFinalResponse();

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
    public void testMrtWithResult() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateTestParameters(true);
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
        assertEquals("text", getArsResult(firstEvent));

        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
        assertEquals("text", getArsResult(secondEvent));
    }

    @Test
    public void testPstWithResult() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateTestParameters(true);
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
        assertEquals("text", getArsResult(firstEvent));

        final MgcpEvent secondEvent = events.get(1);
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), secondEvent.getParameter("rc"));
        assertEquals("text", getArsResult(secondEvent));
    }

    @Test
    public void testNoPartialResultWithoutParameter() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateTestParameters();
        parameters.put(SignalParameters.INPUT.symbol(), "SPEECH");

        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speakRecognizedText("text");

        waitForInterimResponse();

        // then
        verify(observer, never()).onEvent(eq(asr), eventCaptor.capture());
    }

    @Test
    public void testNoPartialResultWithParameterFalse() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateTestParameters(false);
        parameters.put(SignalParameters.INPUT.symbol(), "SPEECH");

        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        speakRecognizedText("text");

        waitForInterimResponse();

        // then
        verify(observer, never()).onEvent(eq(asr), eventCaptor.capture());
    }

    @Test
    public void testAsrWithTooLongHotWord() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateTestParameters(true);
        String hotWords = "Telestax’s RestcommONE platform is changing the way real-time communications are developed and delivered. "
                + "It is fast becoming the platform of choice for rapidly building enterprise class real-time messaging, voice and video applications. "
                + "Our platform is scalable, highly available and the only WebRTC platform that supports cloud, on premise and hybrid deployment configurations.";
        parameters.put(SignalParameters.HOT_WORDS.symbol(), Hex.encodeHexString(hotWords.getBytes()));

        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.VARIABLE_VALUE_OUT_OF_RANGE.code()), firstEvent.getParameter("rc"));
    }

    @Test
    public void testAsrWithTooManyHotWords() throws InterruptedException, DecoderException {
        // given
        final Map<String, String> parameters = generateTestParameters(true);
        final String hotWords = "zero,one,two,three,four,five,six,seven,eight,nine,ten,eleven,twelve,thirteen,fourteen,fifteen,"
                + "sixteen,seventeen,eighteen,nineteen,twenty,twenty one,twenty two,twenty three,twenty four,"
                + "twenty five,twenty six,twenty seven,twenty eight,twenty nine,"
                + "thirty,thirty one,thirty two,thirty three,thirty four,thirty five,thirty six,thirty seven,"
                + "thirty eight,thirty nine,"
                + "fourty,fourty one,fourty two,fourty three,fourty four,fourty five,fourty six,fourty seven,"
                + "fourty eight,fourty nine,fifty";
        parameters.put(SignalParameters.HOT_WORDS.symbol(), Hex.encodeHexString(hotWords.getBytes()));

        final AsrSignal asr = generateAsrSignal(parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        asr.observe(observer);
        asr.execute();

        // then
        verify(asrEngine, times(1)).activate();
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, times(1)).onEvent(eq(asr), eventCaptor.capture());

        final List<MgcpEvent> events = eventCaptor.getAllValues();
        final MgcpEvent firstEvent = events.get(0);
        assertEquals(String.valueOf(ReturnCode.VARIABLE_VALUE_OUT_OF_RANGE.code()), firstEvent.getParameter("rc"));
    }

    private static String getArsResult(final MgcpEvent event) throws DecoderException {
        return new String(Hex.decodeHex(event.getParameter(ReturnParameters.ASR_RESULT.symbol()).toCharArray()));
    }

}
