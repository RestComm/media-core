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

package org.restcomm.media.control.mgcp.pkg.au;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.util.concurrent.FutureCallback;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;
import org.restcomm.media.control.mgcp.pkg.au.OperationComplete;
import org.restcomm.media.control.mgcp.pkg.au.OperationFailed;
import org.restcomm.media.control.mgcp.pkg.au.PlayAnnouncement;
import org.restcomm.media.control.mgcp.pkg.au.ReturnCode;
import org.restcomm.media.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.spi.ResourceUnavailableException;
import org.restcomm.media.spi.player.Player;
import org.restcomm.media.spi.player.PlayerEvent;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class PlayAnnouncementTest {

    @Test
    public void testPlaySingleAnnouncement() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "track1.wav");
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, "1", parameters);
        final PlayerEvent playerEvent = mock(PlayerEvent.class);
        when(playerEvent.getID()).thenReturn(PlayerEvent.STOP);

        // when
        final FutureCallback<MgcpEvent> callback = mock(FutureCallback.class);
        signal.execute(callback);
        signal.process(playerEvent);

        // then
        verify(player).setInitialDelay(0);
        verify(player).setURL(parameters.get(SignalParameters.ANNOUNCEMENT.symbol()));
        verify(player).activate();

        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        verify(callback, timeout(10)).onSuccess(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
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
        final PlayAnnouncement signal = new PlayAnnouncement(player, "1", parameters);
        final PlayerEvent playerEvent = mock(PlayerEvent.class);
        when(playerEvent.getID()).thenReturn(PlayerEvent.STOP);

        // when
        final FutureCallback<MgcpEvent> callback = mock(FutureCallback.class);
        signal.execute(callback);
        for (int i = 0; i < iterations; i++) {
            signal.process(playerEvent);
        }

        // then
        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        verify(callback, timeout(10)).onSuccess(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testPlayNoAnnouncement() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "");
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, "1", parameters);

        // when
        final FutureCallback<MgcpEvent> callback = mock(FutureCallback.class);
        signal.execute(callback);

        // then
        verify(player, never()).activate();

        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        verify(callback, timeout(10)).onSuccess(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertEquals(String.valueOf(ReturnCode.BAD_AUDIO_ID.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testPlayMultipleAnnouncements() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "track1.wav,track2.wav");
        parameters.put(SignalParameters.INTERVAL.symbol(), "50");
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, "1", parameters);
        final PlayerEvent playerEvent = mock(PlayerEvent.class);
        when(playerEvent.getID()).thenReturn(PlayerEvent.STOP);

        // when
        final FutureCallback<MgcpEvent> callback = mock(FutureCallback.class);
        signal.execute(callback);
        signal.process(playerEvent);
        signal.process(playerEvent);

        // then
        verify(player).setInitialDelay(0);
        verify(player).setURL("track1.wav");
        verify(player).setInitialDelay(50 * 1000000);
        verify(player).setURL("track2.wav");
        verify(player, times(2)).activate();

        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        verify(callback, timeout(10)).onSuccess(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testCancelPlayAnnouncement() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "track1.wav");
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, "1", parameters);
        final PlayerEvent playerEvent = mock(PlayerEvent.class);
        when(playerEvent.getID()).thenReturn(PlayerEvent.STOP);

        // when
        final FutureCallback<MgcpEvent> executeCallback = mock(FutureCallback.class);
        signal.execute(executeCallback);

        final FutureCallback<MgcpEvent> cancelCallback = mock(FutureCallback.class);
        signal.cancel(cancelCallback);
        signal.process(playerEvent);

        // then
        verify(player).activate();
        verify(player).deactivate();

        verify(executeCallback, never()).onSuccess(any(MgcpEvent.class));

        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        verify(cancelCallback, timeout(10)).onSuccess(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertEquals(String.valueOf(ReturnCode.SUCCESS.code()), eventCaptor.getValue().getParameter("rc"));
    }

    @Test
    public void testPlayerFailure() throws MalformedURLException, ResourceUnavailableException {
        // given
        final Map<String, String> parameters = new HashMap<>(5);
        parameters.put(SignalParameters.ANNOUNCEMENT.symbol(), "track1.wav");
        final Player player = mock(Player.class);
        final PlayAnnouncement signal = new PlayAnnouncement(player, "1", parameters);
        final PlayerEvent playerEvent = mock(PlayerEvent.class);
        when(playerEvent.getID()).thenReturn(PlayerEvent.FAILED);


        // when
        final FutureCallback<MgcpEvent> callback = mock(FutureCallback.class);
        signal.execute(callback);
        signal.process(playerEvent);

        // then
        verify(player).setInitialDelay(0);
        verify(player).setURL(parameters.get(SignalParameters.ANNOUNCEMENT.symbol()));
        verify(player).activate();

        final ArgumentCaptor<MgcpEvent> eventCaptor = ArgumentCaptor.forClass(MgcpEvent.class);
        verify(callback, timeout(10)).onSuccess(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertEquals(String.valueOf(ReturnCode.UNSPECIFIED_FAILURE.code()), eventCaptor.getValue().getParameter("rc"));
    }

}
