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

package org.restcomm.media.rtp.rfc2833;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.codec.g711.alaw.Decoder;
import org.restcomm.media.codec.g711.ulaw.Encoder;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.dsp.DspFactoryImpl;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.core.sdp.format.AVProfile;
import org.restcomm.media.core.spi.ConnectionMode;
import org.restcomm.media.core.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.core.spi.dtmf.DtmfEvent;
import org.restcomm.media.core.spi.format.AudioFormat;
import org.restcomm.media.core.spi.format.FormatFactory;
import org.restcomm.media.core.spi.format.Formats;
import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.network.deprecated.RtpPortManager;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.network.netty.NettyNetworkManager;
import org.restcomm.media.network.netty.channel.NettyNetworkChannelGlobalContext;
import org.restcomm.media.pcap.AsyncPcapChannel;
import org.restcomm.media.pcap.AsyncPcapChannelHandler;
import org.restcomm.media.pcap.PcapPacketEncoder;
import org.restcomm.media.pcap.PcapPlayer;
import org.restcomm.media.resource.dtmf.DetectorImpl;
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.rtp.statistics.RtpStatistics;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;

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
public class DtmfRfc2833Test {

    // Netty IO Stack
    private ScheduledThreadPoolExecutor executor;
    private ListeningScheduledExecutorService scheduler;
    private NioEventLoopGroup eventGroup;
    private Bootstrap bootstrap;
    private NettyNetworkManager networkManager;

    // PCAP stack
    private AsyncPcapChannel channel;
    private PcapPlayer player;

    // Legacy IO Stack
    private Scheduler ioScheduler;
    private UdpManager udpManager;
    private PortManager portManager;

    // Media Stack
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;
    private AudioMixer mixer;
    private OOBMixer oobMixer;
    private AudioComponent inbandDetectorComponent;
    private OOBComponent oobDetectorComponent;
    private DspFactoryImpl dspFactory;

    // DTMF Detector
    private DetectorImpl dtmfDetector;

    // RTP stack
    private RtpClock rtpClock;
    private RtpClock oobClock;
    private RtpStatistics rtpStatistics;
    private RtpChannel rtpChannel;

    @Before
    public void before() throws Exception {
        // Media Formats
        AudioFormat pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
        Formats fmts = new Formats();
        fmts.add(pcma);

        Formats dstFormats = new Formats();
        dstFormats.add(FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1));

        // Netty IO Stack
        this.executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);
        this.executor.prestartAllCoreThreads();
        this.executor.setRemoveOnCancelPolicy(true);
        this.scheduler = MoreExecutors.listeningDecorator(executor);
        this.eventGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), scheduler);
        this.bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventGroup);
        this.networkManager = new NettyNetworkManager(bootstrap);

        // Legacy IO Stack
        this.ioScheduler = new ServiceScheduler(this.clock);
        this.ioScheduler.start();
        this.portManager = new RtpPortManager(6000, 65000);
        this.udpManager = new UdpManager(this.ioScheduler, this.portManager, this.portManager);
        this.udpManager.setBindAddress("127.0.0.1");
        this.udpManager.setLocalBindAddress("127.0.0.1");
        this.udpManager.start();

        // Media Stack
        this.clock = new WallClock();
        this.mediaScheduler = new PriorityQueueScheduler(this.clock);
        this.mediaScheduler.start();
        this.mixer = new AudioMixer(this.mediaScheduler);
        this.oobMixer = new OOBMixer(this.mediaScheduler);
        this.dspFactory = new DspFactoryImpl();
        this.dspFactory.addCodec(Encoder.class.getName());
        this.dspFactory.addCodec(Decoder.class.getName());
        this.dspFactory.addCodec(org.restcomm.media.codec.g711.alaw.Encoder.class.getName());
        this.dspFactory.addCodec(org.restcomm.media.codec.g711.alaw.Decoder.class.getName());

        // RTP Stack
        this.rtpClock = new RtpClock(this.clock);
        this.oobClock = new RtpClock(this.clock);
        this.rtpStatistics = new RtpStatistics(this.rtpClock);
        this.rtpChannel = new RtpChannel(1, 50, this.rtpStatistics, this.rtpClock, this.oobClock, mediaScheduler, udpManager, mock(DtlsSrtpServerProvider.class));
        this.rtpChannel.setInputDsp(this.dspFactory.newProcessor());
        this.mixer.addComponent(this.rtpChannel.getAudioComponent());
        this.oobMixer.addComponent(this.rtpChannel.getOobComponent());
    }

    @SuppressWarnings("unchecked")
    @After
    public void after() {
        // DTMF Detector
        if (this.dtmfDetector != null) {
            this.dtmfDetector.deactivate();
            this.dtmfDetector = null;
        }

        // RTP stack
        if (this.rtpChannel != null) {
            this.rtpChannel.close();
            this.rtpChannel = null;
        }
        if (this.rtpStatistics != null) {
            this.rtpStatistics.reset();
            this.rtpStatistics = null;
        }
        this.rtpClock = null;

        // Media Stack
        this.oobMixer.stop();
        this.mixer.stop();
        this.mediaScheduler.stop();

        // Legacy IO
        this.udpManager.stop();
        this.ioScheduler.stop();

        // PCAP Stack
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

        if (dtmfDetector != null) {
            this.dtmfDetector.deactivate();
            this.dtmfDetector = null;
        }

        this.networkManager = null;
        this.bootstrap = null;

        if (this.eventGroup != null) {
            if (!this.eventGroup.isShutdown()) {
                this.eventGroup.shutdownGracefully(0L, 0L, TimeUnit.NANOSECONDS);
            }
            this.eventGroup = null;
        }

        if (this.scheduler != null) {
            if (!this.scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            scheduler = null;
        }

        if (this.executor != null) {
            if (!this.executor.isShutdown()) {
                this.executor.shutdown();
            }
            this.executor = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDetectHashFromPcap() throws Exception {
        // given
        // int rtpEventPacketCount = 35;
        // int rtpEventPacketLength = 24;
        // int rtpPacketCount = 39;
        // int rtpPacketLength = 180;
        // int totalOctets = (rtpEventPacketLength * rtpEventPacketCount) + (rtpPacketCount * rtpPacketLength);
        // int totalPackets = rtpEventPacketCount + rtpPacketCount;
        long rtpStreamDuration = 900;
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 64000);

        String filepath = "dtmf-oob-one-hash.cap.gz";
        PcapPacketEncoder packetEncoder = new PcapPacketEncoder();
        AsyncPcapChannelHandler channelInitializer = new AsyncPcapChannelHandler(packetEncoder);
        bootstrap.handler(channelInitializer);
        NettyNetworkChannelGlobalContext context = new NettyNetworkChannelGlobalContext(networkManager);
        channel = new AsyncPcapChannel(context);
        player = new PcapPlayer(channel, scheduler);

        FutureCallback<Void> openCallback = mock(FutureCallback.class);
        FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        FutureCallback<Void> connectCallback = mock(FutureCallback.class);

        rtpChannel.bind(false, false);
        rtpChannel.connect(localAddress);
        SocketAddress remoteAddress = rtpChannel.getLocalAddress();
        rtpChannel.setFormatMap(AVProfile.audio);
        rtpChannel.updateMode(ConnectionMode.RECV_ONLY);

        channel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);
        channel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);
        channel.connect(remoteAddress, connectCallback);
        verify(connectCallback, timeout(100)).onSuccess(null);

        this.dtmfDetector = new DetectorImpl("dtmf-detector", 0, 40, 0, this.mediaScheduler);
        this.inbandDetectorComponent = new AudioComponent(8);
        this.inbandDetectorComponent.addOutput(this.dtmfDetector.getAudioOutput());
        this.inbandDetectorComponent.updateMode(true, true);
        this.mixer.addComponent(this.inbandDetectorComponent);

        this.oobDetectorComponent = new OOBComponent(9);
        this.oobDetectorComponent.addOutput(this.dtmfDetector.getOOBOutput());
        this.oobDetectorComponent.updateMode(true, true);
        this.oobMixer.addComponent(this.oobDetectorComponent);

        final DtmfListener dtmfListener = new DtmfListener();
        this.dtmfDetector.addListener(dtmfListener);

        // when
        this.mixer.start();
        this.oobMixer.start();
        this.dtmfDetector.activate();
        Thread.sleep(1000);

        URL pcap = DtmfRfc2833Test.class.getResource(filepath);
        player.play(pcap);

        // then
        Assert.assertTrue(player.isPlaying());
        Thread.sleep(rtpStreamDuration * 2);
        Assert.assertFalse(player.isPlaying());
        // Assert.assertEquals(totalPackets, player.countPacketsSent());
        // Assert.assertEquals(totalOctets, player.countOctetsSent());

        this.dtmfDetector.flushBuffer();
        Assert.assertEquals(1, dtmfListener.countTones());
        Assert.assertEquals("#", dtmfListener.pollTone().getTone());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDetectHashThenHashFromPcap() throws Exception {
        // given
        // int rtpEventPacketCount = 72;
        // int rtpEventPacketLength = 24;
        // int rtpPacketCount = 429;
        // int rtpPacketLength = 180;
        // int totalOctets = (rtpEventPacketLength * rtpEventPacketCount) + (rtpPacketCount * rtpPacketLength);
        // int totalPackets = rtpEventPacketCount + rtpPacketCount;
        long rtpStreamDuration = 9000;
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 64000);

        String filepath = "dtmf-oob-two-hash.cap.gz";
        PcapPacketEncoder packetEncoder = new PcapPacketEncoder();
        AsyncPcapChannelHandler channelInitializer = new AsyncPcapChannelHandler(packetEncoder);
        bootstrap.handler(channelInitializer);
        NettyNetworkChannelGlobalContext context = new NettyNetworkChannelGlobalContext(networkManager);
        channel = new AsyncPcapChannel(context);
        player = new PcapPlayer(channel, scheduler);

        FutureCallback<Void> openCallback = mock(FutureCallback.class);
        FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        FutureCallback<Void> connectCallback = mock(FutureCallback.class);

        rtpChannel.bind(false, false);
        rtpChannel.connect(localAddress);
        SocketAddress remoteAddress = rtpChannel.getLocalAddress();
        rtpChannel.setFormatMap(AVProfile.audio);
        rtpChannel.updateMode(ConnectionMode.RECV_ONLY);

        channel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);
        channel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);
        channel.connect(remoteAddress, connectCallback);
        verify(connectCallback, timeout(100)).onSuccess(null);

        this.dtmfDetector = new DetectorImpl("dtmf-detector", 0, 40, 0, this.mediaScheduler);
        this.inbandDetectorComponent = new AudioComponent(8);
        this.inbandDetectorComponent.addOutput(this.dtmfDetector.getAudioOutput());
        this.inbandDetectorComponent.updateMode(true, true);
        this.mixer.addComponent(this.inbandDetectorComponent);

        this.oobDetectorComponent = new OOBComponent(9);
        this.oobDetectorComponent.addOutput(this.dtmfDetector.getOOBOutput());
        this.oobDetectorComponent.updateMode(true, true);
        this.oobMixer.addComponent(this.oobDetectorComponent);

        final DtmfListener dtmfListener = new DtmfListener();
        this.dtmfDetector.addListener(dtmfListener);

        // when
        this.mixer.start();
        this.oobMixer.start();
        this.dtmfDetector.activate();
        Thread.sleep(1000);

        URL pcap = DtmfRfc2833Test.class.getResource(filepath);
        player.play(pcap);

        // then
        Assert.assertTrue(player.isPlaying());
        Thread.sleep(rtpStreamDuration * 2);
        Assert.assertFalse(player.isPlaying());
        // Assert.assertEquals(totalPackets, player.countPacketsSent());
        // Assert.assertEquals(totalOctets, player.countOctetsSent());

        this.dtmfDetector.flushBuffer();
        Assert.assertEquals(2, dtmfListener.countTones());
        Assert.assertEquals("#", dtmfListener.pollTone().getTone());
        Assert.assertEquals("#", dtmfListener.pollTone().getTone());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGigasetN510IpProRfc2833() throws Exception {
        // given
        // int rtpEventPacketCount = 70;
        // int rtpEventPacketLength = 24;
        // int rtpPacketCount = 2494;
        // int rtpPacketLength = 180;
        // int totalOctets = (rtpEventPacketLength * rtpEventPacketCount) + (rtpPacketCount * rtpPacketLength);
        // int totalPackets = rtpEventPacketCount + rtpPacketCount;
        long rtpStreamDuration = 50000;
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 64000);

        String filepath = "gigaset-n510-ip-pro-rfc2833.pcap";
        PcapPacketEncoder packetEncoder = new PcapPacketEncoder();
        AsyncPcapChannelHandler channelInitializer = new AsyncPcapChannelHandler(packetEncoder);
        bootstrap.handler(channelInitializer);
        NettyNetworkChannelGlobalContext context = new NettyNetworkChannelGlobalContext(networkManager);
        channel = new AsyncPcapChannel(context);
        player = new PcapPlayer(channel, scheduler);

        FutureCallback<Void> openCallback = mock(FutureCallback.class);
        FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        FutureCallback<Void> connectCallback = mock(FutureCallback.class);

        rtpChannel.bind(false, false);
        rtpChannel.connect(localAddress);
        SocketAddress remoteAddress = rtpChannel.getLocalAddress();
        rtpChannel.setFormatMap(AVProfile.audio);
        rtpChannel.updateMode(ConnectionMode.RECV_ONLY);

        channel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);
        channel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);
        channel.connect(remoteAddress, connectCallback);
        verify(connectCallback, timeout(100)).onSuccess(null);

        this.dtmfDetector = new DetectorImpl("dtmf-detector", 0, 100, 100, this.mediaScheduler);
        this.inbandDetectorComponent = new AudioComponent(8);
        this.inbandDetectorComponent.addOutput(this.dtmfDetector.getAudioOutput());
        this.inbandDetectorComponent.updateMode(true, true);
        this.mixer.addComponent(this.inbandDetectorComponent);

        this.oobDetectorComponent = new OOBComponent(9);
        this.oobDetectorComponent.addOutput(this.dtmfDetector.getOOBOutput());
        this.oobDetectorComponent.updateMode(true, true);
        this.oobMixer.addComponent(this.oobDetectorComponent);

        final DtmfListener dtmfListener = new DtmfListener();
        this.dtmfDetector.addListener(dtmfListener);

        // when
        this.mixer.start();
        this.oobMixer.start();
        this.dtmfDetector.activate();
        Thread.sleep(1000);

        URL pcap = DtmfRfc2833Test.class.getResource(filepath);
        player.play(pcap);

        // then
        Assert.assertTrue(player.isPlaying());
        Thread.sleep(rtpStreamDuration + 10000);
        Assert.assertFalse(player.isPlaying());
        // Assert.assertEquals(totalPackets, player.countPacketsSent());
        // Assert.assertEquals(totalOctets, player.countOctetsSent());

        this.dtmfDetector.flushBuffer();
        Assert.assertEquals(10, dtmfListener.countTones());
        Assert.assertEquals("1", dtmfListener.pollTone().getTone());
        Assert.assertEquals("2", dtmfListener.pollTone().getTone());
        Assert.assertEquals("1", dtmfListener.pollTone().getTone());
        Assert.assertEquals("1", dtmfListener.pollTone().getTone());
        Assert.assertEquals("#", dtmfListener.pollTone().getTone());
        Assert.assertEquals("1", dtmfListener.pollTone().getTone());
        Assert.assertEquals("2", dtmfListener.pollTone().getTone());
        Assert.assertEquals("1", dtmfListener.pollTone().getTone());
        Assert.assertEquals("1", dtmfListener.pollTone().getTone());
        Assert.assertEquals("#", dtmfListener.pollTone().getTone());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCiscoSpa525G2InboundAudioForZendesk34432() throws Exception {
        // given
        long rtpStreamDuration = 11000;
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 64000);
        
        String filepath = "cisco-spa-525G2-zendesk34432.pcap";
        PcapPacketEncoder packetEncoder = new PcapPacketEncoder();
        AsyncPcapChannelHandler channelInitializer = new AsyncPcapChannelHandler(packetEncoder);
        bootstrap.handler(channelInitializer);
        NettyNetworkChannelGlobalContext context = new NettyNetworkChannelGlobalContext(networkManager);
        channel = new AsyncPcapChannel(context);
        player = new PcapPlayer(channel, scheduler);
        
        FutureCallback<Void> openCallback = mock(FutureCallback.class);
        FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        FutureCallback<Void> connectCallback = mock(FutureCallback.class);
        
        rtpChannel.bind(false, false);
        rtpChannel.connect(localAddress);
        SocketAddress remoteAddress = rtpChannel.getLocalAddress();
        rtpChannel.setFormatMap(AVProfile.audio);
        rtpChannel.updateMode(ConnectionMode.RECV_ONLY);
        
        channel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);
        channel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);
        channel.connect(remoteAddress, connectCallback);
        verify(connectCallback, timeout(100)).onSuccess(null);
        
        this.dtmfDetector = new DetectorImpl("dtmf-detector", -30, 100, 300, this.mediaScheduler);
        this.inbandDetectorComponent = new AudioComponent(8);
        this.inbandDetectorComponent.addOutput(this.dtmfDetector.getAudioOutput());
        this.inbandDetectorComponent.updateMode(true, true);
        this.mixer.addComponent(this.inbandDetectorComponent);
        
        this.oobDetectorComponent = new OOBComponent(9);
        this.oobDetectorComponent.addOutput(this.dtmfDetector.getOOBOutput());
        this.oobDetectorComponent.updateMode(true, true);
        this.oobMixer.addComponent(this.oobDetectorComponent);
        
        final DtmfListener dtmfListener = new DtmfListener();
        this.dtmfDetector.addListener(dtmfListener);
        
        // when
        this.mixer.start();
        this.oobMixer.start();
        this.dtmfDetector.activate();
        Thread.sleep(1000);
        
        URL pcap = DtmfRfc2833Test.class.getResource(filepath);
        player.play(pcap);
        
        // then
        Assert.assertTrue(player.isPlaying());
        Thread.sleep(rtpStreamDuration + 10000);
        Assert.assertFalse(player.isPlaying());
        
        this.dtmfDetector.flushBuffer();
        Assert.assertEquals(10, dtmfListener.countTones());
        Assert.assertEquals("6", dtmfListener.pollTone().getTone());
        Assert.assertEquals("6", dtmfListener.pollTone().getTone());
        Assert.assertEquals("8", dtmfListener.pollTone().getTone());
        Assert.assertEquals("8", dtmfListener.pollTone().getTone());
        Assert.assertEquals("#", dtmfListener.pollTone().getTone());
        Assert.assertEquals("6", dtmfListener.pollTone().getTone());
        Assert.assertEquals("6", dtmfListener.pollTone().getTone());
        Assert.assertEquals("8", dtmfListener.pollTone().getTone());
        Assert.assertEquals("8", dtmfListener.pollTone().getTone());
        Assert.assertEquals("#", dtmfListener.pollTone().getTone());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testZendesk34401Rfc2833() throws Exception {
        // given
        // int rtpEventPacketCount = 70;
        // int rtpEventPacketLength = 24;
        // int rtpPacketCount = 2494;
        // int rtpPacketLength = 180;
        // int totalOctets = (rtpEventPacketLength * rtpEventPacketCount) + (rtpPacketCount * rtpPacketLength);
        // int totalPackets = rtpEventPacketCount + rtpPacketCount;
        long rtpStreamDuration = 3000;
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 64000);

        String filepath = "zendesk-34401-rfc2833.pcap";
        PcapPacketEncoder packetEncoder = new PcapPacketEncoder();
        AsyncPcapChannelHandler channelInitializer = new AsyncPcapChannelHandler(packetEncoder);
        bootstrap.handler(channelInitializer);
        NettyNetworkChannelGlobalContext context = new NettyNetworkChannelGlobalContext(networkManager);
        channel = new AsyncPcapChannel(context);
        player = new PcapPlayer(channel, scheduler);

        FutureCallback<Void> openCallback = mock(FutureCallback.class);
        FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        FutureCallback<Void> connectCallback = mock(FutureCallback.class);

        rtpChannel.bind(false, false);
        rtpChannel.connect(localAddress);
        SocketAddress remoteAddress = rtpChannel.getLocalAddress();
        rtpChannel.setFormatMap(AVProfile.audio);
        rtpChannel.updateMode(ConnectionMode.RECV_ONLY);

        channel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);
        channel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);
        channel.connect(remoteAddress, connectCallback);
        verify(connectCallback, timeout(100)).onSuccess(null);

        this.dtmfDetector = new DetectorImpl("dtmf-detector", 0, 100, 200, this.mediaScheduler);
        this.inbandDetectorComponent = new AudioComponent(8);
        this.inbandDetectorComponent.addOutput(this.dtmfDetector.getAudioOutput());
        this.inbandDetectorComponent.updateMode(true, true);
        this.mixer.addComponent(this.inbandDetectorComponent);

        this.oobDetectorComponent = new OOBComponent(9);
        this.oobDetectorComponent.addOutput(this.dtmfDetector.getOOBOutput());
        this.oobDetectorComponent.updateMode(true, true);
        this.oobMixer.addComponent(this.oobDetectorComponent);

        final DtmfListener dtmfListener = new DtmfListener();
        this.dtmfDetector.addListener(dtmfListener);

        // when
        this.mixer.start();
        this.oobMixer.start();
        this.dtmfDetector.activate();
        Thread.sleep(1000);

        URL pcap = DtmfRfc2833Test.class.getResource(filepath);
        player.play(pcap);

        // then
        Assert.assertTrue(player.isPlaying());
        Thread.sleep(rtpStreamDuration + 1000);
        Assert.assertFalse(player.isPlaying());
        // Assert.assertEquals(totalPackets, player.countPacketsSent());
        // Assert.assertEquals(totalOctets, player.countOctetsSent());

        this.dtmfDetector.flushBuffer();
        Assert.assertEquals(5, dtmfListener.countTones());
        Assert.assertEquals("1", dtmfListener.pollTone().getTone());
        Assert.assertEquals("2", dtmfListener.pollTone().getTone());
        Assert.assertEquals("1", dtmfListener.pollTone().getTone());
        Assert.assertEquals("1", dtmfListener.pollTone().getTone());
        Assert.assertEquals("#", dtmfListener.pollTone().getTone());
    }

    private class DtmfListener implements DtmfDetectorListener {

        private final Queue<DtmfEvent> tones;

        public DtmfListener() {
            this.tones = new ConcurrentLinkedQueue<>();
        }

        public int countTones() {
            return this.tones.size();
        }

        public DtmfEvent pollTone() {
            return tones.poll();
        }

        @Override
        public void process(DtmfEvent event) {
            this.tones.offer(event);

        }

    }

}
