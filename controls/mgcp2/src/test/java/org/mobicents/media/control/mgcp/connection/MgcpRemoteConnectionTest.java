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

package org.mobicents.media.control.mgcp.connection;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.listener.MgcpConnectionListener;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;
import org.mobicents.media.control.mgcp.pkg.MgcpEventObserver;
import org.mobicents.media.control.mgcp.pkg.r.RtpTimeout;
import org.mobicents.media.server.impl.rtp.channels.AudioChannel;
import org.mobicents.media.server.impl.rtp.channels.MediaChannelProvider;
import org.mobicents.media.server.io.sdp.format.AVProfile;
import org.mockito.ArgumentCaptor;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRemoteConnectionTest {

    private ListeningScheduledExecutorService executor;

    @Before
    public void before() {
        this.executor = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1));
    }

    public void after() {
        this.executor.shutdownNow();
        this.executor = null;
    }

    @Test
    public void testMaxDurationTimerWhenHalfOpen() throws MgcpConnectionException, InterruptedException {
        // given
        final int identifier = 1;
        final int halfOpenTimeout = 2;
        final int openTimeout = 3;
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final ArgumentCaptor<RtpTimeout> timeoutCaptor = ArgumentCaptor.forClass(RtpTimeout.class);
        final AudioChannel audioChannel = mock(AudioChannel.class);
        final MediaChannelProvider channelProvider = mock(MediaChannelProvider.class);

        // when
        when(channelProvider.provideAudioChannel()).thenReturn(audioChannel);
        when(audioChannel.getFormats()).thenReturn(AVProfile.audio);
        when(audioChannel.getMediaType()).thenReturn(AudioChannel.MEDIA_TYPE);

        final MgcpRemoteConnection connection = new MgcpRemoteConnection(identifier, halfOpenTimeout, openTimeout, channelProvider, this.executor);
        connection.observe(observer);
        connection.halfOpen(new LocalConnectionOptions());
        Thread.sleep(halfOpenTimeout * 1000 + 200);

        // then
        assertEquals(MgcpConnectionState.CLOSED, connection.state);
        verify(observer, only()).onEvent(eq(connection), timeoutCaptor.capture());
        assertNotNull(timeoutCaptor.getValue());
        assertEquals(halfOpenTimeout, timeoutCaptor.getValue().getTimeout());
    }

    @Test
    public void testMaxDurationTimerWhenOpen() throws MgcpConnectionException, InterruptedException {
        // given
        final int openTimeout = 4;
        final int halfOpenTimeout = openTimeout / 2;
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final ArgumentCaptor<RtpTimeout> timeoutCaptor = ArgumentCaptor.forClass(RtpTimeout.class);
        final AudioChannel audioChannel = mock(AudioChannel.class);
        final MediaChannelProvider channelProvider = mock(MediaChannelProvider.class);

        // when
        when(channelProvider.provideAudioChannel()).thenReturn(audioChannel);
        when(audioChannel.getFormats()).thenReturn(AVProfile.audio);
        when(audioChannel.getMediaType()).thenReturn(AudioChannel.MEDIA_TYPE);
        when(audioChannel.containsNegotiatedFormats()).thenReturn(true);

        final MgcpRemoteConnection connection1 = new MgcpRemoteConnection(1, halfOpenTimeout, openTimeout, channelProvider, this.executor);
        connection1.observe(observer);
        final String sdp1 = connection1.halfOpen(new LocalConnectionOptions());

        final MgcpRemoteConnection connection2 = new MgcpRemoteConnection(2, halfOpenTimeout, openTimeout, channelProvider, this.executor);
        connection2.observe(observer);
        connection2.open(sdp1);

        // then
        Thread.sleep(halfOpenTimeout * 1000 + 200);
        assertEquals(MgcpConnectionState.CLOSED, connection1.state);
        verify(observer, times(1)).onEvent(eq(connection1), timeoutCaptor.capture());
        assertNotNull(timeoutCaptor.getValue());
        assertEquals(halfOpenTimeout, timeoutCaptor.getValue().getTimeout());
        assertEquals(MgcpConnectionState.OPEN, connection2.state);

        Thread.sleep(halfOpenTimeout * 1000 + 200);
        assertEquals(MgcpConnectionState.CLOSED, connection2.state);
        verify(observer, times(1)).onEvent(eq(connection2), timeoutCaptor.capture());
        assertNotNull(timeoutCaptor.getValue());
        assertEquals(openTimeout, timeoutCaptor.getValue().getTimeout());
    }

    @Test
    public void testHalfOpenTimerCancelationWhenMovingToOpen() throws MgcpConnectionException, InterruptedException {
        // given
        final int openTimeout = 4;
        final int halfOpenTimeout = openTimeout / 2;
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final ArgumentCaptor<RtpTimeout> timeoutCaptor = ArgumentCaptor.forClass(RtpTimeout.class);
        final AudioChannel audioChannel = mock(AudioChannel.class);
        final MediaChannelProvider channelProvider = mock(MediaChannelProvider.class);

        // when
        when(channelProvider.provideAudioChannel()).thenReturn(audioChannel);
        when(audioChannel.getFormats()).thenReturn(AVProfile.audio);
        when(audioChannel.getMediaType()).thenReturn(AudioChannel.MEDIA_TYPE);
        when(audioChannel.containsNegotiatedFormats()).thenReturn(true);

        final MgcpRemoteConnection connection1 = new MgcpRemoteConnection(1, halfOpenTimeout, openTimeout, channelProvider, this.executor);
        connection1.observe(observer);
        final String sdp1 = connection1.halfOpen(new LocalConnectionOptions());

        final MgcpRemoteConnection connection2 = new MgcpRemoteConnection(2, halfOpenTimeout, openTimeout, channelProvider, this.executor);
        connection2.observe(observer);
        final String sdp2 = connection2.open(sdp1);

        connection1.open(sdp2);

        // then
        Thread.sleep(halfOpenTimeout * 1000 + 200);
        assertEquals(MgcpConnectionState.OPEN, connection1.state);
        assertEquals(MgcpConnectionState.OPEN, connection2.state);

        Thread.sleep(halfOpenTimeout * 1000 + 200);
        assertEquals(MgcpConnectionState.CLOSED, connection1.state);
        verify(observer, times(1)).onEvent(eq(connection1), timeoutCaptor.capture());
        assertNotNull(timeoutCaptor.getValue());
        assertEquals(openTimeout, timeoutCaptor.getValue().getTimeout());
        
        assertEquals(MgcpConnectionState.CLOSED, connection2.state);
        verify(observer, times(1)).onEvent(eq(connection2), timeoutCaptor.capture());
        assertNotNull(timeoutCaptor.getValue());
        assertEquals(openTimeout, timeoutCaptor.getValue().getTimeout());
    }
    
    @Test
    public void testInactiveHalfOpenTimer() throws MgcpConnectionException, InterruptedException {
        // given
        final int identifier = 1;
        final int halfOpenTimeout = 0;
        final int openTimeout = 4;
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final ArgumentCaptor<RtpTimeout> timeoutCaptor = ArgumentCaptor.forClass(RtpTimeout.class);
        final AudioChannel audioChannel = mock(AudioChannel.class);
        final MediaChannelProvider channelProvider = mock(MediaChannelProvider.class);

        // when
        when(channelProvider.provideAudioChannel()).thenReturn(audioChannel);
        when(audioChannel.getFormats()).thenReturn(AVProfile.audio);
        when(audioChannel.getMediaType()).thenReturn(AudioChannel.MEDIA_TYPE);

        final MgcpRemoteConnection connection = new MgcpRemoteConnection(identifier, halfOpenTimeout, openTimeout, channelProvider, this.executor);
        connection.observe(observer);
        connection.halfOpen(new LocalConnectionOptions());

        Thread.sleep(openTimeout * 1000);
        
        // then
        assertEquals(MgcpConnectionState.HALF_OPEN, connection.state);
        verify(observer, never()).onEvent(eq(connection), timeoutCaptor.capture());
    }
    
    @Test
    public void testInactiveOpenTimer() throws MgcpConnectionException, InterruptedException {
        // given
        final int openTimeout = 0;
        final int halfOpenTimeout = 2;
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final ArgumentCaptor<RtpTimeout> timeoutCaptor = ArgumentCaptor.forClass(RtpTimeout.class);
        final AudioChannel audioChannel = mock(AudioChannel.class);
        final MediaChannelProvider channelProvider = mock(MediaChannelProvider.class);

        // when
        when(channelProvider.provideAudioChannel()).thenReturn(audioChannel);
        when(audioChannel.getFormats()).thenReturn(AVProfile.audio);
        when(audioChannel.getMediaType()).thenReturn(AudioChannel.MEDIA_TYPE);
        when(audioChannel.containsNegotiatedFormats()).thenReturn(true);

        final MgcpRemoteConnection connection1 = new MgcpRemoteConnection(1, halfOpenTimeout, openTimeout, channelProvider, this.executor);
        connection1.observe(observer);
        final String sdp1 = connection1.halfOpen(new LocalConnectionOptions());

        final MgcpRemoteConnection connection2 = new MgcpRemoteConnection(2, halfOpenTimeout, openTimeout, channelProvider, this.executor);
        connection2.observe(observer);
        connection2.open(sdp1);

        // then
        Thread.sleep(halfOpenTimeout * 1000 + 200);
        assertEquals(MgcpConnectionState.CLOSED, connection1.state);
        verify(observer, times(1)).onEvent(eq(connection1), timeoutCaptor.capture());
        assertNotNull(timeoutCaptor.getValue());
        assertEquals(halfOpenTimeout, timeoutCaptor.getValue().getTimeout());
        assertEquals(MgcpConnectionState.OPEN, connection2.state);

        Thread.sleep(halfOpenTimeout * 1000 + 200);
        assertEquals(MgcpConnectionState.OPEN, connection2.state);
        verify(observer, never()).onEvent(eq(connection2), timeoutCaptor.capture());
    }

}
