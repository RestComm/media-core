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

package org.restcomm.media.pcap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.network.netty.NettyNetworkManager;
import org.restcomm.media.network.netty.channel.NettyNetworkChannelGlobalContext;
import org.restcomm.media.pcap.AsyncPcapChannel;
import org.restcomm.media.pcap.AsyncPcapChannelHandler;
import org.restcomm.media.pcap.PcapPacketEncoder;
import org.restcomm.media.pcap.PcapPlayer;

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

    private ScheduledThreadPoolExecutor executor;
    private ListeningScheduledExecutorService scheduler;
    private NioEventLoopGroup eventGroup;
    private Bootstrap bootstrap;
    private NettyNetworkManager networkManager;
    private AsyncPcapChannel channel;
    private PcapPlayer player;
    private DatagramChannel remotePeer;

    @Before
    public void before() {
        this.executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
        this.executor.prestartAllCoreThreads();
        this.executor.setRemoveOnCancelPolicy(true);

        this.scheduler = MoreExecutors.listeningDecorator(executor);

        this.eventGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), scheduler);
        this.bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventGroup);

        this.networkManager = new NettyNetworkManager(bootstrap);
    }

    @SuppressWarnings("unchecked")
    @After
    public void after() {
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player = null;
        }

        if (channel != null) {
            if (channel.isOpen()) {
                channel.close(mock(FutureCallback.class));
            }
            channel = null;
        }
        
        if(remotePeer != null) {
            try {
                remotePeer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.networkManager = null;
        this.bootstrap = null;
        
        if(this.eventGroup != null) {
            if(!this.eventGroup.isShutdown()) {
                this.eventGroup.shutdownGracefully(0L, 0L, TimeUnit.NANOSECONDS);
            }
            this.eventGroup = null;
        }
        
        if(this.scheduler != null) {
            if(!this.scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            scheduler = null;
        }
        
        if(this.executor != null) {
            if(!this.executor.isShutdown()) {
                this.executor.shutdown();
            }
            this.executor = null;
        }
    }
    
    private DatagramChannel openRemotePeer(SocketAddress address) throws IOException {
        DatagramChannel datagramChannel = DatagramChannel.open();
        try {
            datagramChannel.bind(address);
        } catch (Exception e) {
            datagramChannel.close();
            throw e;
        }
        return datagramChannel;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPlayHashRfc2833() throws Exception {
        // given
        int rtpEventPacketCount = 35;
        int rtpEventPacketLength = 24;
        int rtpPacketCount = 39;
        int rtpPacketLength = 180;
        long rtpStreamDuration = 900;
        int totalOctets = (rtpEventPacketLength * rtpEventPacketCount) + (rtpPacketCount * rtpPacketLength);
        int totalPackets = rtpEventPacketCount + rtpPacketCount;
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 64000);
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 65000);

        String filepath = "dtmf-oob-one-hash.cap.gz";
        PcapPacketEncoder packetEncoder = new PcapPacketEncoder();
        AsyncPcapChannelHandler channelInitializer = new AsyncPcapChannelHandler(packetEncoder);
        bootstrap.handler(channelInitializer);
        NettyNetworkChannelGlobalContext context = new NettyNetworkChannelGlobalContext(networkManager);
        channel = new AsyncPcapChannel(context);
        remotePeer = openRemotePeer(remoteAddress);
        player = new PcapPlayer(channel, scheduler);

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

        // then
        Assert.assertTrue(player.isPlaying());
        Thread.sleep(rtpStreamDuration * 2);
        Assert.assertFalse(player.isPlaying());
        Assert.assertEquals(totalPackets, player.countPacketsSent());
        Assert.assertEquals(totalOctets, player.countOctetsSent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPlayHashThenHashRfc2833() throws Exception {
        // given
        int rtpEventPacketCount = 72;
        int rtpEventPacketLength = 24;
        int rtpPacketCount = 429;
        int rtpPacketLength = 180;
        long rtpStreamDuration = 9000;
        int totalOctets = (rtpEventPacketLength * rtpEventPacketCount) + (rtpPacketCount * rtpPacketLength);
        int totalPackets = rtpEventPacketCount + rtpPacketCount;
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 64000);
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 65000);
        
        String filepath = "dtmf-oob-two-hash.cap.gz";
        PcapPacketEncoder packetEncoder = new PcapPacketEncoder();
        AsyncPcapChannelHandler channelInitializer = new AsyncPcapChannelHandler(packetEncoder);
        bootstrap.handler(channelInitializer);
        NettyNetworkChannelGlobalContext context = new NettyNetworkChannelGlobalContext(networkManager);
        channel = new AsyncPcapChannel(context);
        remotePeer = openRemotePeer(remoteAddress);
        player = new PcapPlayer(channel, scheduler);
        
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
        
        // then
        Assert.assertTrue(player.isPlaying());
        Thread.sleep(rtpStreamDuration * 2);
        Assert.assertFalse(player.isPlaying());
        Assert.assertEquals(totalPackets, player.countPacketsSent());
        Assert.assertEquals(totalOctets, player.countOctetsSent());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGigasetN510IpProRfc2833() throws Exception {
        // given
        int rtpEventPacketCount = 70;
        int rtpEventPacketLength = 24;
        int rtpPacketCount = 2494;
        int rtpPacketLength = 180;
        long rtpStreamDuration = 50000;
        int totalOctets = (rtpEventPacketLength * rtpEventPacketCount) + (rtpPacketCount * rtpPacketLength);
        int totalPackets = rtpEventPacketCount + rtpPacketCount;
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 64000);
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 65000);
        
        String filepath = "gigaset-n510-ip-pro-rfc2833.pcap";
        PcapPacketEncoder packetEncoder = new PcapPacketEncoder();
        AsyncPcapChannelHandler channelInitializer = new AsyncPcapChannelHandler(packetEncoder);
        bootstrap.handler(channelInitializer);
        NettyNetworkChannelGlobalContext context = new NettyNetworkChannelGlobalContext(networkManager);
        channel = new AsyncPcapChannel(context);
        remotePeer = openRemotePeer(remoteAddress);
        player = new PcapPlayer(channel, scheduler);
        
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
        
        // then
        Assert.assertTrue(player.isPlaying());
        Thread.sleep(rtpStreamDuration + 10000);
        Assert.assertFalse(player.isPlaying());
        Assert.assertEquals(totalPackets, player.countPacketsSent());
        Assert.assertEquals(totalOctets, player.countOctetsSent());
    }

}
