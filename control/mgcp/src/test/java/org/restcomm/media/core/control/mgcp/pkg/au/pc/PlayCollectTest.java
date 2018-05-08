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

package org.restcomm.media.core.control.mgcp.pkg.au.pc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.core.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.core.control.mgcp.pkg.MgcpEventObserver;
import org.restcomm.media.core.control.mgcp.pkg.au.ReturnCode;
import org.restcomm.media.core.resource.dtmf.detector.DtmfEvent;
import org.restcomm.media.core.resource.dtmf.detector.DtmfSinkFacade;
import org.restcomm.media.core.resource.player.audio.AudioPlayerEvent;
import org.restcomm.media.core.resource.player.audio.AudioPlayerImpl;
import org.restcomm.media.core.spi.ResourceUnavailableException;
import org.restcomm.media.core.spi.player.Player;
import org.restcomm.media.core.spi.player.PlayerEvent;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayCollectTest {

    private ScheduledExecutorService threadPool;

    @Before
    public void before() {
        threadPool = Executors.newScheduledThreadPool(5);
    }

    @After
    public void after() {
        threadPool.shutdown();
    }

    @Test
    public void testCollectOneDigitWithEndInputKey() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mn", "1");
        parameters.put("mx", "5");
        parameters.put("eik", "#");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("5"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(1)).activate();
        verify(detector, times(1)).deactivate();
        verify(player, never()).activate();
        verify(player, never()).deactivate();
        verify(observer, timeout(5)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("5", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testPlayCollectOneDigitWithEndInputKeyAndInterruptiblePrompt() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "prompt.wav");
        parameters.put("ni", "false");
        parameters.put("mn", "1");
        parameters.put("mx", "2");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("5"));
        verify(player, times(1)).deactivate();
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("5"));

        // then

        verify(detector, times(1)).activate();
        verify(detector, times(1)).deactivate();
        verify(player, times(1)).activate();
        verify(player, times(1)).deactivate();

        verify(observer, timeout(5)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("55", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testPlayCollectWithNonInterruptiblePrompt() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "prompt.wav");
        parameters.put("mn", "1");
        parameters.put("mx", "2");
        parameters.put("eik", "#");
        parameters.put("ni", "true");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("5"));
        verify(player, times(0)).deactivate();
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(1)).activate();
        verify(detector, times(1)).deactivate();
        verify(player, times(1)).activate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(5)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("5", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCollectWithReinputKeyAndEndInputKey() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "100");
        parameters.put("eik", "#");
        parameters.put("rik", "A");
        parameters.put("na", "2");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("3"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("4"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("5"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("6"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(2)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(5)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("456", eventCaptor.getValue().getParameter("dc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCollectWithIncludeEndInputKey() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "100");
        parameters.put("iek", "true");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("3"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("123#", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCollectWithMaximumNumberOfDigits() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "3");
        parameters.put("iek", "false");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("3"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("123", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCollectWithDefaultMaximumNumberOfDigits() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("iek", "false");
        // mx parameter defaults to 1

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("6"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("6", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCollectWithStartInputKeys() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("sik", "345");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("4"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("4", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCollectWithMinimumNumberOfDigitsNotSatisfied() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "3");
        parameters.put("mn", "3");
        parameters.put("eik", "#");
        parameters.put("na", "1");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testSuccessfulCollectWithDigitPatternOfFiveDigits() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("dp", "xxxxx");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("3"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("4"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("5"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("12345", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testSuccessfulCollectWithDigitPatternOfAtLeastOneDigitFollowedByTwoDistinctLetters()
            throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("dp", "x.AB");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("3"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("4"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("5"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("B"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("12345AB", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testSuccessfulCollectWithDigitPatternOfNineFollowedByAnyDigitFollowedByOptionalLetter()
            throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("dp", "9xA*");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("9"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("91", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testSuccessfulCollectWithTwoDigitPatterns() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("dp", "9xA*|9AC");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("9"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("C"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("9AC", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCollectFirstDigitTimeout() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("fdt", "10");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(10 * 100);

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.NO_DIGITS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testPlayCollectFirstDigitTimeout() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "prompt.wav");
        parameters.put("mn", "1");
        parameters.put("mx", "2");
        parameters.put("fdt", "5");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        Thread.sleep(6 * 100);

        // then
        verify(detector, times(1)).activate();
        verify(detector, times(1)).deactivate();
        verify(player, times(1)).activate();
        verify(player, times(1)).deactivate();
        verify(observer, timeout(5)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.NO_DIGITS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testCollectTimeoutInterDigit() throws InterruptedException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "5");
        parameters.put("mn", "2");
        parameters.put("fdt", "50");
        parameters.put("idt", "30");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(40 * 100);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("9"));
        Thread.sleep(31 * 100);

        // then
        verify(detector, times(1)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testInitialPromptWithMultipleSegments()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("ip", "dummy1.wav,dummy2.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Simulate playback time to assure FirstDigitTimer is not interrupting
        Thread.sleep(5);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        Thread.sleep(5);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("9"));

        // then
        verify(detector, times(1)).activate();
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("dummy1.wav");
        verify(player, times(1)).setURL("dummy2.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals("9", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testFailAllAtemptsDueToTimeout()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "3");
        parameters.put("fdt", "5");
        parameters.put("idt", "5");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // fail all 3 attempts (10 * 100ms each)
        Thread.sleep(3 * 5 * 100);

        // then
        verify(detector, times(3)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(200)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.NO_DIGITS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testFailAllAtemptsDueToBadPattern()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("dp", "xx");

        final Player player = mock(Player.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(detector, times(2)).activate();
        verify(player, never()).activate();
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testPlayCollectWithRestartKey()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "dummy.wav");
        parameters.put("rsk", "A");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(1000);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));

        Thread.sleep(1000);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));

        // then
        verify(detector, times(2)).activate();
        verify(player, times(2)).activate();
        verify(player, times(2)).setURL("dummy.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("dc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testPlayCollectWithRestartKeyUntilMaxAttemptAreExceeded()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "dummy.wav");
        parameters.put("rsk", "A");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(1000);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));

        Thread.sleep(1000);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));

        // then
        verify(player, times(2)).activate();
        verify(player, times(2)).setURL("dummy.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testRepromptWhenTimeoutWithoutMinDigitsCollected()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "3");
        parameters.put("ip", "prompt.wav");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav");
        parameters.put("mn", "2");
        parameters.put("mx", "2");
        parameters.put("fdt", "20");
        parameters.put("idt", "10");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        pc.observe(observer);
        pc.execute();
        // Play initial prompt
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect one digit and wait for timeout
        Thread.sleep(5);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        Thread.sleep(10 * 100);

        // Play reprompt
        Thread.sleep(5);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        Thread.sleep(5);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        // Collect one digit and wait for timeout
        Thread.sleep(5);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        Thread.sleep(10 * 100);

        // Play reprompt
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        Thread.sleep(5);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect two digits
        Thread.sleep(5);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        Thread.sleep(5);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("5"));

        // then
        verify(player, times(5)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(2)).setURL("reprompt1.wav");
        verify(player, times(2)).setURL("reprompt2.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("25", eventCaptor.getValue().getParameter("dc"));
        assertEquals("3", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testRepromptWhenTimeoutWithoutMatchingDigitsPattern()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "prompt.wav");
        parameters.put("rp", "reprompt.wav");
        parameters.put("dp", "xxA");
        parameters.put("fdt", "5");
        parameters.put("idt", "5");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Play initial prompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect one digit and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("B"));
        Thread.sleep(5 * 100);

        // Play reprompt
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect two digits and end input
        Thread.sleep(5);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        Thread.sleep(5 * 100);

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(1)).setURL("reprompt.wav");
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("12A", eventCaptor.getValue().getParameter("dc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testRepromptWhenEndingInputWithoutMinDigitsCollected()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "prompt.wav");
        parameters.put("rp", "reprompt.wav");
        parameters.put("mn", "2");
        parameters.put("mx", "3");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Play initial prompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect one digit and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // Play reprompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect two digits and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("4"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(1)).setURL("reprompt.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("24", eventCaptor.getValue().getParameter("dc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testRepromptWhenEndingInputWithoutMatchingDigitsPattern()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "prompt.wav");
        parameters.put("rp", "reprompt.wav");
        parameters.put("dp", "xxA");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Play initial prompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect one digit and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("B"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // Play reprompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect two digits and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(1)).setURL("reprompt.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("12A", eventCaptor.getValue().getParameter("dc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testNoDigitsRepromptWhenTimeoutWithoutMinDigitsCollected()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "3");
        parameters.put("ip", "prompt.wav");
        parameters.put("nd", "nodigits1.wav,nodigits2.wav");
        parameters.put("mn", "2");
        parameters.put("mx", "2");
        parameters.put("fdt", "5");
        parameters.put("idt", "3");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Play initial prompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect no digits and wait for timeout
        Thread.sleep(6 * 100);

        // Play reprompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect no digits and wait for timeout
        Thread.sleep(6 * 100);

        // Play reprompt
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect two digits
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("5"));

        // then
        verify(player, times(5)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(2)).setURL("nodigits1.wav");
        verify(player, times(2)).setURL("nodigits2.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("25", eventCaptor.getValue().getParameter("dc"));
        assertEquals("3", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testNoDigitsRepromptWhenTimeoutWithoutMatchingDigitsPattern()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "prompt.wav");
        parameters.put("nd", "nodigits.wav");
        parameters.put("dp", "xxA");
        parameters.put("fdt", "5");
        parameters.put("idt", "5");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Play initial prompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect no digits and timeout
        Thread.sleep(6 * 100);

        // Play reprompt
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect two digits and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        Thread.sleep(5 * 100);

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(1)).setURL("nodigits.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("12A", eventCaptor.getValue().getParameter("dc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testNoDigitsRepromptWhenEndingInputWithoutMinDigitsCollected()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "prompt.wav");
        parameters.put("nd", "nodigits.wav");
        parameters.put("mn", "2");
        parameters.put("mx", "3");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Play initial prompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect no digits and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // Play reprompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect two digits and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("4"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(1)).setURL("nodigits.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("24", eventCaptor.getValue().getParameter("dc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testNoDigitsRepromptWhenEndingInputWithoutMatchingDigitsPattern()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "prompt.wav");
        parameters.put("nd", "nodigits.wav");
        parameters.put("dp", "xxA");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Play initial prompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect no digits and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // Play reprompt
        Thread.sleep(20);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // Collect two digits and end input
        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(1)).setURL("nodigits.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("12A", eventCaptor.getValue().getParameter("dc"));
        assertEquals("2", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testPlaySuccessAnnouncementWhenEndInputDigit()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "100");
        parameters.put("eik", "#");
        parameters.put("sa", "success1.wav,success2.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("3"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("success1.wav");
        verify(player, times(1)).setURL("success2.wav");
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("123", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testPlaySuccessAnnouncementWhenEndInputDigitAndDigitPatternMatches()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "100");
        parameters.put("eik", "#");
        parameters.put("dp", "12A");
        parameters.put("sa", "success1.wav,success2.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("success1.wav");
        verify(player, times(1)).setURL("success2.wav");
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("12A", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testPlaySuccessAnnouncementWhenTimeout()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "2");
        parameters.put("eik", "#");
        parameters.put("sa", "success1.wav,success2.wav");
        parameters.put("fdt", "5");
        parameters.put("idt", "5");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        Thread.sleep(6 * 100);

        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("success1.wav");
        verify(player, times(1)).setURL("success2.wav");
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("12", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testPlayFailureAnnouncementWhenEndInputKey()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "100");
        parameters.put("mn", "2");
        parameters.put("na", "1");
        parameters.put("eik", "#");
        parameters.put("fa", "failure1.wav,failure2.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("failure1.wav");
        verify(player, times(1)).setURL("failure2.wav");
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testPlayFailureAnnouncementWhenDigitPatternNotMatched()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "100");
        parameters.put("mn", "2");
        parameters.put("na", "1");
        parameters.put("eik", "#");
        parameters.put("dp", "123");
        parameters.put("fa", "failure1.wav,failure2.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("B"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));

        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("failure1.wav");
        verify(player, times(1)).setURL("failure2.wav");
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testPlayFailureAnnouncementWhenTimeout()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "100");
        parameters.put("mn", "2");
        parameters.put("na", "1");
        parameters.put("eik", "#");
        parameters.put("fa", "failure1.wav,failure2.wav");
        parameters.put("ftd", "5");
        parameters.put("idt", "5");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        Thread.sleep(6 * 100);

        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("failure1.wav");
        verify(player, times(1)).setURL("failure2.wav");
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testCancelWhenPrompting() throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "3");
        parameters.put("mn", "2");
        parameters.put("ip", "prompt1.wav,prompt2.wav");
        parameters.put("fa", "failure.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        pc.cancel();

        // then
        verify(player, times(2)).activate();
        verify(player, times(1)).setURL("prompt1.wav");
        verify(player, times(1)).setURL("prompt2.wav");
        verify(player, never()).setURL("failure.wav");
        verify(player, times(1)).deactivate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.NO_DIGITS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testCancelWhenCollectingAndMinimumDigitsCollected()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "3");
        parameters.put("mn", "2");
        parameters.put("pa", "success.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.cancel();

        // then
        verify(player, never()).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("12", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCancelWhenCollectingAndNoDigitsCollected()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "3");
        parameters.put("mn", "2");
        parameters.put("pa", "success.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();
        pc.cancel();

        // then
        verify(player, never()).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.NO_DIGITS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testCancelWhenCollectingAndDigitPatternMatched()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("dp", "xABCx");
        parameters.put("pa", "success.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("B"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("C"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("2"));
        pc.cancel();

        // then
        verify(player, never()).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1ABC2", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCancelWhenCollectingAndDigitPatternDidNotMatch()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("dp", "xABCx");
        parameters.put("sa", "success.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("A"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("B"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("C"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("D"));
        pc.cancel();

        // then
        verify(player, never()).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testCancelWhenPlayingSuccessAnnouncement()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("sa", "success1.wav,success2.wav,success3.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));
        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        pc.cancel();

        // then
        verify(player, times(2)).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
        assertEquals("1", eventCaptor.getValue().getParameter("dc"));
        assertEquals("1", eventCaptor.getValue().getParameter("na"));
    }

    @Test
    public void testCancelWhenPlayingFailureAnnouncement()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("mx", "3");
        parameters.put("mn", "2");
        parameters.put("fa", "failure1.wav,failure2.wav,failure3.wav");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        Thread.sleep(10);
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("#"));
        Thread.sleep(10);
        pc.playerListener.process(new AudioPlayerEvent(player, AudioPlayerEvent.STOP));
        pc.cancel();

        // then
        verify(player, times(2)).activate();
        verify(observer, timeout(100)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.DIGIT_PATTERN_NOT_MATCHED.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testCancelWhenReprompting() throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "prompt.wav");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav");
        parameters.put("mn", "2");
        parameters.put("mx", "2");
        parameters.put("fdt", "20");
        parameters.put("idt", "10");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Play initial prompt
        Thread.sleep(5);
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        Thread.sleep(5);

        // collect one digit and wait for timeout
        pc.detectorObserver.onDtmfEvent(new DtmfEvent("1"));
        Thread.sleep(11 * 100);

        // Play reprompt
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pc.cancel();

        // then
        verify(player, times(3)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(1)).setURL("reprompt1.wav");
        verify(player, times(1)).setURL("reprompt2.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.NO_DIGITS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testCancelWhenNoDigitsReprompting()
            throws InterruptedException, MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put("na", "2");
        parameters.put("ip", "prompt.wav");
        parameters.put("rp", "reprompt1.wav,reprompt2.wav");
        parameters.put("nd", "nodigits1.wav,nodigits2.wav");
        parameters.put("mn", "2");
        parameters.put("mx", "2");
        parameters.put("fdt", "20");
        parameters.put("idt", "10");

        final AudioPlayerImpl player = mock(AudioPlayerImpl.class);
        final DtmfSinkFacade detector = mock(DtmfSinkFacade.class);
        final ListeningScheduledExecutorService executor = MoreExecutors.listeningDecorator(threadPool);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayCollect pc = new PlayCollect(player, detector, 1, parameters, executor);

        // when
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);

        pc.observe(observer);
        pc.execute();

        // Play initial prompt
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));

        // wait for timeout without collecting any digit
        Thread.sleep(21 * 100);

        // Play reprompt
        pc.playerListener.process(new AudioPlayerEvent(player, PlayerEvent.STOP));
        pc.cancel();

        // then
        verify(player, times(3)).activate();
        verify(player, times(1)).setURL("prompt.wav");
        verify(player, times(1)).setURL("nodigits1.wav");
        verify(player, times(1)).setURL("nodigits2.wav");
        verify(observer, timeout(50)).onEvent(eq(pc), eventCaptor.capture());

        assertEquals(String.valueOf(ReturnCode.NO_DIGITS.code()), eventCaptor.getValue().getParameter("rc"));
    }

}
