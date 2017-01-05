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

package org.mobicents.media.control.mgcp.pkg.au;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mobicents.media.control.mgcp.pkg.MgcpEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpEventObserver;
import org.mobicents.media.control.mgcp.pkg.base.OperationFailure;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayAnnouncementTest {

    @Test
    public void testPlaySingleAnnouncement() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "track1.wav");
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, 1, parameters);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayerEvent playerEvent = mock(PlayerEvent.class);

        // when
        when(playerEvent.getID()).thenReturn(PlayerEvent.STOP);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpEvent event = invocation.getArgumentAt(1, MgcpEvent.class);
                assertNotNull(event);
                assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), event.getParameter("rc"));
                return null;
            }

        }).when(observer).onEvent(eq(signal), any(OperationComplete.class));

        signal.observe(observer);
        signal.execute();
        signal.process(playerEvent);

        // then
        verify(player).setInitialDelay(0);
        verify(player).setURL(parameters.get(SignalParameters.ANNOUNCEMENT.symbol()));
        verify(player).activate();
        verify(observer).onEvent(eq(signal), any(OperationComplete.class));
    }

    @Test
    public void testPlaySingleAnnouncementWithMultipleIterations() throws MalformedURLException, ResourceUnavailableException {
        // given
        final String announcements = "track1.wav";
        final int iterations = 2;

        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), announcements);
        parameters.put(SignalParameters.ITERATIONS.symbol(), String.valueOf(iterations));
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, 1, parameters);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayerEvent playerEvent = mock(PlayerEvent.class);

        // when
        when(playerEvent.getID()).thenReturn(PlayerEvent.STOP);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpEvent event = invocation.getArgumentAt(1, MgcpEvent.class);
                assertNotNull(event);
                assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), event.getParameter("rc"));
                return null;
            }

        }).when(observer).onEvent(eq(signal), any(OperationComplete.class));

        signal.observe(observer);
        signal.execute();
        for (int i = 0; i < iterations; i++) {
            signal.process(playerEvent);
        }

        // then
        verify(player, times(iterations)).activate();
        verify(observer).onEvent(eq(signal), any(OperationComplete.class));
    }

    @Test
    public void testPlayNoAnnouncement() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "");
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, 1, parameters);

        // when
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpEvent event = invocation.getArgumentAt(1, MgcpEvent.class);
                assertNotNull(event);
                assertEquals(String.valueOf(ReturnCode.BAD_AUDIO_ID.code()), event.getParameter("rc"));
                return null;
            }

        }).when(observer).onEvent(eq(signal), any(OperationFailed.class));

        signal.observe(observer);
        signal.execute();

        // then
        verify(player, never()).activate();
        verify(observer).onEvent(eq(signal), any(OperationFailed.class));
    }

    @Test
    public void testPlayMultipleAnnouncements() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "track1.wav,track2.wav");
        parameters.put(SignalParameters.INTERVAL.symbol(), "50");
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, 1, parameters);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayerEvent playerEvent = mock(PlayerEvent.class);

        // when
        when(playerEvent.getID()).thenReturn(PlayerEvent.STOP);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpEvent event = invocation.getArgumentAt(1, MgcpEvent.class);
                assertNotNull(event);
                assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), event.getParameter("rc"));
                return null;
            }

        }).when(observer).onEvent(eq(signal), any(OperationComplete.class));

        signal.observe(observer);
        signal.execute();
        signal.process(playerEvent);
        signal.process(playerEvent);

        // then
        verify(player).setInitialDelay(0);
        verify(player).setURL("track1.wav");
        verify(player).setInitialDelay(50 * 1000000);
        verify(player).setURL("track2.wav");
        verify(player, times(2)).activate();
    }

    @Test
    public void testCancelPlayAnnouncement() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "track1.wav");
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, 1, parameters);

        // when
        signal.execute();
        signal.cancel();

        // then
        verify(player).activate();
        verify(player).deactivate();
    }

    @Test
    public void testPlayerFailure() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "track1.wav");
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, 1, parameters);
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final PlayerEvent playerEvent = mock(PlayerEvent.class);

        // when
        when(playerEvent.getID()).thenReturn(PlayerEvent.FAILED);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpEvent event = invocation.getArgumentAt(1, MgcpEvent.class);
                assertNotNull(event);
                assertEquals(String.valueOf(ReturnCode.UNSPECIFIED_FAILURE.code()), event.getParameter("rc"));
                return null;
            }

        }).when(observer).onEvent(eq(signal), any(OperationComplete.class));

        signal.observe(observer);
        signal.execute();
        signal.process(playerEvent);

        // then
        verify(player).setInitialDelay(0);
        verify(player).setURL(parameters.get(SignalParameters.ANNOUNCEMENT.symbol()));
        verify(player).activate();
        verify(observer).onEvent(eq(signal), any(OperationFailure.class));
    }

}
