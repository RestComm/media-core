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
        
package org.restcomm.media.rtp.pcap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.network.netty.NettyNetworkManager;
import org.restcomm.media.network.netty.channel.NettyNetworkChannelGlobalContext;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PcapPlayerTest {
    
    private ListeningScheduledExecutorService scheduler;
    
    @Before
    public void before() {
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
        executor.prestartAllCoreThreads();
        executor.setRemoveOnCancelPolicy(true);
        scheduler = MoreExecutors.listeningDecorator(executor);
    }
    
    @After
    public void after() {
        scheduler.shutdown();
        scheduler = null;
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testPlayToneOneOutOfBand() throws Exception {
        // given
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 64001);
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 65000);
        
        final String filepath = "dtmf-oob-two-hash.cap.gz";
        final NioEventLoopGroup eventGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), scheduler);
        final PcapPacketEncoder packetEncoder = new PcapPacketEncoder();
        final AsyncPcapChannelHandler channelInitializer = new AsyncPcapChannelHandler(packetEncoder);
        final Bootstrap bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventGroup).handler(channelInitializer);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final NettyNetworkChannelGlobalContext context = new NettyNetworkChannelGlobalContext(networkManager);
        final AsyncPcapChannel channel = new AsyncPcapChannel(context);
        final PcapPlayer player = new PcapPlayer(channel, scheduler);
        
        FutureCallback<Void> openCallback = mock(FutureCallback.class);
        FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        FutureCallback<Void> connectCallback = mock(FutureCallback.class);
        
        // when
        channel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);
        channel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);
        channel.connect(remoteAddress, connectCallback);
        verify(connectCallback, timeout(100)).onSuccess(null);
        
        URL pcap = PcapPlayerTest.class.getResource(filepath);
        player.play(pcap);
        Thread.sleep(5000);
        Assert.assertTrue(player.isPlaying());
    }
    

}
