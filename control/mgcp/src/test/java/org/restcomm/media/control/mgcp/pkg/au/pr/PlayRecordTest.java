/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.pkg.au.pr;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;
import org.restcomm.media.control.mgcp.pkg.au.ReturnCode;
import org.restcomm.media.control.mgcp.pkg.au.pr.PlayRecord;
import org.restcomm.media.core.resource.recorder.audio.RecorderEventImpl;
import org.restcomm.media.core.spi.dtmf.DtmfDetector;
import org.restcomm.media.core.spi.player.Player;
import org.restcomm.media.core.spi.player.PlayerEvent;
import org.restcomm.media.core.spi.recorder.Recorder;
import org.restcomm.media.core.spi.recorder.RecorderEvent;
import org.restcomm.media.resource.dtmf.DtmfEventImpl;
import org.restcomm.media.resource.player.audio.AudioPlayerEvent;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayRecordTest {

    private static final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);

    @AfterClass
    public static void cleanup() {
        threadPool.shutdown();
    }

    @Test
    public void testRecord() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("eik", "#");
        parameters.put("rlt", "100");

        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pr.observe(observer);
        pr.execute();

        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.SUCCESS);
        pr.recorderListener.process(recorderStop);

        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, never()).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }

    @Test
    public void testRecordWithInitialPrompt() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("ri", "RE0001");
        parameters.put("eik", "#");
        parameters.put("rlt", "100");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.SUCCESS);
        pr.recorderListener.process(recorderStop);
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(3)).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }
    
    @Test
    public void testRecordWithMaxDurationExceeded() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("eik", "#");
        parameters.put("rlt", "100");

        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pr.observe(observer);
        pr.execute();

        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.MAX_DURATION_EXCEEDED);
        pr.recorderListener.process(recorderStop);

        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, never()).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SPOKE_TOO_LONG.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testRecordWithNoSpeech() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.NO_SPEECH);
        pr.recorderListener.process(recorderStop);
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, never()).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testRecordWithSuccessAnnouncement() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        parameters.put("sa", "success1.wav,success2.wav,success3.wav");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.SUCCESS);
        pr.recorderListener.process(recorderStop);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(3)).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }

    @Test
    public void testRecordWithFailureAnnouncement() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        parameters.put("fa", "failure1.wav,failure2.wav,failure3.wav");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.NO_SPEECH);
        pr.recorderListener.process(recorderStop);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(3)).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testNoSpeechReprompt() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav,reprompt3.wav");
        parameters.put("ns", "nospeech1.wav,nospeech2.wav,nospeech3.wav");
        parameters.put("sa", "success1.wav,success2.wav,success3.wav");
        parameters.put("na", "2");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        // no speech
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.NO_SPEECH);
        pr.recorderListener.process(recorderStop);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // restart
        recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.SUCCESS);
        pr.recorderListener.process(recorderStop);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // then
        verify(detector, times(2)).activate();
        verify(recorder, times(2)).activate();
        verify(player, times(6)).activate();
        verify(detector, times(2)).deactivate();
        verify(recorder, times(2)).deactivate();
        verify(player, times(2)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }

    @Test
    public void testRepromptAfterMaxDurationExceeded() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav,reprompt3.wav");
        parameters.put("ns", "nospeech1.wav,nospeech2.wav,nospeech3.wav");
        parameters.put("sa", "success1.wav,success2.wav,success3.wav");
        parameters.put("na", "2");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        // no speech
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.MAX_DURATION_EXCEEDED);
        pr.recorderListener.process(recorderStop);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // restart
        recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.SUCCESS);
        pr.recorderListener.process(recorderStop);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // then
        verify(detector, times(2)).activate();
        verify(recorder, times(2)).activate();
        verify(player, times(6)).activate();
        verify(detector, times(2)).deactivate();
        verify(recorder, times(2)).deactivate();
        verify(player, times(2)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }

    @Test
    public void testRestart() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rsk", "*");
        parameters.put("rlt", "100");
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav,reprompt3.wav");
        parameters.put("ns", "nospeech1.wav,nospeech2.wav,nospeech3.wav");
        parameters.put("na", "2");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        // initial prompt and recording    
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // restart
        pr.detectorListener.process(new DtmfEventImpl(detector, "*", 0));
        
        // replay initial prompt and do successful recording
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.SUCCESS);
        pr.recorderListener.process(recorderStop);
        
        // then
        verify(detector, times(2)).activate();
        verify(recorder, times(2)).activate();
        verify(player, times(6)).activate();
        verify(player, times(2)).deactivate();
        verify(detector, times(2)).deactivate();
        verify(recorder, times(2)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }

    @Test
    public void testRestartExceedsMaximumAttempts() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rsk", "*");
        parameters.put("rlt", "100");
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav,reprompt3.wav");
        parameters.put("ns", "nospeech1.wav,nospeech2.wav,nospeech3.wav");
        parameters.put("na", "1");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        // initial prompt and recording    
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // restart
        pr.detectorListener.process(new DtmfEventImpl(detector, "*", 0));
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(3)).activate();
        verify(player, times(1)).deactivate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }
    
    @Test
    public void testReinput() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rik", "*");
        parameters.put("rlt", "100");
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav,reprompt3.wav");
        parameters.put("ns", "nospeech1.wav,nospeech2.wav,nospeech3.wav");
        parameters.put("na", "2");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        // initial prompt and recording    
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // re-input
        pr.detectorListener.process(new DtmfEventImpl(detector, "*", 0));
        
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.SUCCESS);
        pr.recorderListener.process(recorderStop);
        
        // then
        verify(detector, times(2)).activate();
        verify(recorder, times(2)).activate();
        verify(player, times(3)).activate();
        verify(player, times(1)).deactivate();
        verify(detector, times(2)).deactivate();
        verify(recorder, times(2)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }
    
    @Test
    public void testReinputExceedsMaximumAttempts() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rik", "*");
        parameters.put("rlt", "100");
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav,reprompt3.wav");
        parameters.put("ns", "nospeech1.wav,nospeech2.wav,nospeech3.wav");
        parameters.put("na", "1");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        // initial prompt and recording    
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        // restart
        pr.detectorListener.process(new DtmfEventImpl(detector, "*", 0));
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(3)).activate();
        verify(player, times(1)).deactivate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }
    
    @Test
    public void testEndInputKey() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("eik", "*");
        parameters.put("rlt", "100");

        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pr.observe(observer);
        pr.execute();

        pr.detectorListener.process(new DtmfEventImpl(detector, "*", 0));

        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, never()).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }

    @Test
    public void testDefaultEndInputKey() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        pr.detectorListener.process(new DtmfEventImpl(detector, "#", 0));
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, never()).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }

    @Test
    public void testCancelOnPrompting() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        pr.cancel();
        
        // then
        verify(detector, never()).activate();
        verify(recorder, never()).activate();
        verify(player, times(1)).activate();
        verify(detector, never()).deactivate();
        verify(recorder, never()).deactivate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCancelOnSuccessfulRecording() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();

        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        pr.recorderListener.process(new RecorderEventImpl(RecorderEvent.SPEECH_DETECTED, recorder));
        pr.cancel();
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(3)).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }

    @Test
    public void testCancelOnNoSpeechRecording() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();

        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        pr.cancel();

        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(3)).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCancelOnPlayingSuccess() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("sa", "success1.wav,success2.wav,success3.wav");
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        pr.recorderListener.process(new RecorderEventImpl(RecorderEvent.SPEECH_DETECTED, recorder));
        
        RecorderEventImpl stopRecorder = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        stopRecorder.setQualifier(RecorderEvent.SUCCESS);
        pr.recorderListener.process(stopRecorder);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.cancel();
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(5)).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(player, times(2)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
        assertEquals("false", eventCaptor.getValue().getParameter("vi"));
        assertEquals("RE0001", eventCaptor.getValue().getParameter("ri"));
    }

    @Test
    public void testCancelOnPlayingFailure() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "prompt1.wav,prompt2.wav,prompt3.wav");
        parameters.put("fa", "failure1.wav,failure2.wav,failure3.wav");
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        
        pr.recorderListener.process(new RecorderEventImpl(RecorderEvent.SPEECH_DETECTED, recorder));
        
        RecorderEventImpl stopRecorder = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        stopRecorder.setQualifier(RecorderEvent.MAX_DURATION_EXCEEDED);
        pr.recorderListener.process(stopRecorder);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.cancel();
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(5)).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(player, times(2)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.SPOKE_TOO_LONG.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }
    
    @Test
    public void testCancelOnReprompt() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav,reprompt3.wav");
        parameters.put("ns", "nospeech1.wav,nospeech2.wav,nospeech3.wav");
        parameters.put("sa", "success1.wav,success2.wav,success3.wav");
        parameters.put("na", "2");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        pr.recorderListener.process(new RecorderEventImpl(RecorderEvent.SPEECH_DETECTED, recorder));
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.MAX_DURATION_EXCEEDED);
        pr.recorderListener.process(recorderStop);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.cancel();
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(2)).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCancelOnNoSpeechReprompt() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ri", "RE0001");
        parameters.put("rlt", "100");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav,reprompt3.wav");
        parameters.put("ns", "nospeech1.wav,nospeech2.wav,nospeech3.wav");
        parameters.put("sa", "success1.wav,success2.wav,success3.wav");
        parameters.put("na", "2");
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Recorder recorder = mock(Recorder.class);
        final DtmfDetector detector = mock(DtmfDetector.class);
        final Player player = mock(Player.class);
        final PlayRecord pr = new PlayRecord(player, detector, recorder, 1, parameters);
        
        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        
        pr.observe(observer);
        pr.execute();
        
        RecorderEventImpl recorderStop = new RecorderEventImpl(RecorderEvent.STOP, recorder);
        recorderStop.setQualifier(RecorderEvent.NO_SPEECH);
        pr.recorderListener.process(recorderStop);
        
        pr.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pr.cancel();
        
        // then
        verify(detector, times(1)).activate();
        verify(recorder, times(1)).activate();
        verify(player, times(2)).activate();
        verify(detector, times(1)).deactivate();
        verify(recorder, times(1)).deactivate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pr), eventCaptor.capture());
        
        assertEquals(String.valueOf(ReturnCode.NO_SPEECH.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

}
