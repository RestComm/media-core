/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.restcomm.media.core.rtp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.codec.g711.alaw.Decoder;
import org.restcomm.media.codec.g711.alaw.Encoder;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.dsp.Dsp;
import org.restcomm.media.component.dsp.DspFactoryImpl;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.core.rtp.ChannelsManager;
import org.restcomm.media.core.rtp.RTPDataChannel;
import org.restcomm.media.core.rtp.RtpPacket;
import org.restcomm.media.core.rtp.crypto.DtlsSrtpServer;
import org.restcomm.media.core.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.core.scheduler.Clock;
import org.restcomm.media.core.scheduler.PriorityQueueScheduler;
import org.restcomm.media.core.scheduler.Scheduler;
import org.restcomm.media.core.scheduler.ServiceScheduler;
import org.restcomm.media.core.scheduler.WallClock;
import org.restcomm.media.core.sdp.format.AVProfile;
import org.restcomm.media.core.spi.ConnectionMode;
import org.restcomm.media.core.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.core.spi.dtmf.DtmfEvent;
import org.restcomm.media.core.spi.format.AudioFormat;
import org.restcomm.media.core.spi.format.FormatFactory;
import org.restcomm.media.core.spi.format.Formats;
import org.restcomm.media.network.deprecated.RtpPortManager;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.resource.dtmf.DetectorImpl;

/**
 *
 * @author oifa yulian
 */
public class RTPEventTest implements DtmfDetectorListener {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;
    private Scheduler scheduler;

    private ChannelsManager channelsManager;
    private UdpManager udpManager;

//    private SpectraAnalyzer analyzer;
    private DetectorImpl detector;
    
    private RTPDataChannel channel;
    
    private DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    private Dsp dsp11, dsp12;
    private Dsp dsp21, dsp22;

    private Sender sender;
    
    private AudioMixer audioMixer;
    private OOBMixer oobMixer;
    
    private AudioComponent detectorComponent;
    private OOBComponent oobComponent;
    
    private int count=0;
    
    private static final int cipherSuites[] = { 0xc030, 0xc02f, 0xc028, 0xc027, 0xc014, 0xc013, 0x009f, 0x009e, 0x006b, 0x0067,
            0x0039, 0x0033, 0x009d, 0x009c, 0x003d, 0x003c, 0x0035, 0x002f, 0xc02b };
    
    public RTPEventTest() {
        scheduler = new ServiceScheduler();
    }

    @Before
    public void setUp() throws Exception {
        // given
        DtlsSrtpServerProvider mockedDtlsServerProvider = mock(DtlsSrtpServerProvider.class);
        DtlsSrtpServer mockedDtlsSrtpServer = mock(DtlsSrtpServer.class);
        
        // when
        when(mockedDtlsServerProvider.provide()).thenReturn(mockedDtlsSrtpServer);
        when(mockedDtlsSrtpServer.getCipherSuites()).thenReturn(cipherSuites);
        
        // then
        AudioFormat pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
        Formats fmts = new Formats();
        fmts.add(pcma);
        
        Formats dstFormats = new Formats();
        dstFormats.add(FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1));
        
        dspFactory.addCodec(Encoder.class.getName());
        dspFactory.addCodec(Decoder.class.getName());

        dsp11 = dspFactory.newProcessor();
        dsp12 = dspFactory.newProcessor();

        dsp21 = dspFactory.newProcessor();
        dsp22 = dspFactory.newProcessor();
        
        //use default clock
        clock = new WallClock();

        //create single thread scheduler
        mediaScheduler = new PriorityQueueScheduler();
        mediaScheduler.setClock(clock);
        mediaScheduler.start();

        udpManager = new UdpManager(scheduler, new RtpPortManager(), new RtpPortManager());
        scheduler.start();
        udpManager.start();
        
        channelsManager = new ChannelsManager(udpManager, mockedDtlsServerProvider);
        channelsManager.setScheduler(mediaScheduler);
        
        detector = new DetectorImpl("dtmf", -35, 40, 100, mediaScheduler);
        detector.addListener(this);
        
        channel = channelsManager.getChannel();
        channel.bind(false);

        sender = new Sender(channel.getLocalPort());
        
        channel.setPeer(new InetSocketAddress("127.0.0.1", 9200));
        channel.setInputDsp(dsp11);
        channel.setFormatMap(AVProfile.audio);

        audioMixer=new AudioMixer(mediaScheduler);
        audioMixer.addComponent(channel.getAudioComponent());
        
        detectorComponent=new AudioComponent(1);
        detectorComponent.addOutput(detector.getAudioOutput());
        detectorComponent.updateMode(true,true);
        audioMixer.addComponent(detectorComponent);               
        
        oobMixer=new OOBMixer(mediaScheduler);
        oobMixer.addComponent(channel.getOOBComponent());
        
        oobComponent=new OOBComponent(1);
        oobComponent.addOutput(detector.getOOBOutput());
        oobComponent.updateMode(true,true);
        oobMixer.addComponent(oobComponent);
    }

    @After
    public void tearDown() {
    	channel.close();
    	audioMixer.stop();
    	oobMixer.stop();
        udpManager.stop();
        mediaScheduler.stop();
        scheduler.stop();
        sender.close();
    }

    @Test
    public void testTransmission() throws Exception {
    	channel.updateMode(ConnectionMode.SEND_RECV);
    	audioMixer.start();
    	oobMixer.start();
    	detector.activate();
    	
        new Thread(sender).start();

        Thread.sleep(5000);
        
        channel.updateMode(ConnectionMode.INACTIVE);
    	audioMixer.stop();
    	oobMixer.stop();
    	detector.deactivate();
    	
    	assertEquals(4,count);
    }

    public void process(DtmfEvent event) {
    	count++;
        System.out.println("TONE=" + event.getTone());
    }
    
    private class Sender implements Runnable {
        
        private DatagramSocket socket;
        private ArrayList<byte[]> stream = new ArrayList<byte[]>();
        
        private int port;
        private InetSocketAddress dst;
        
        private byte[][] evt = new byte[][]{
            new byte[] {0x03, 0x0a, 0x00, (byte)0xa0},
            new byte[] {0x03, 0x0a, 0x01, (byte)0x40},
            new byte[] {0x03, 0x0a, 0x01, (byte)0xe0}
        };

        private byte[][] evt1 = new byte[][]{
            new byte[] {0x0b, 0x0a, 0x00, (byte)0xa0},
            new byte[] {0x0b, 0x0a, 0x01, (byte)0x40},
            new byte[] {0x0b, 0x0a, 0x01, (byte)0xe0},
            new byte[] {0x0b, 0x0a, 0x02, (byte)0x80},
            new byte[] {0x0b, 0x0a, 0x03, (byte)0x20},
            new byte[] {0x0b, 0x0a, 0x03, (byte)0xc0},
            new byte[] {0x0b, 0x0a, 0x04, (byte)0x60},
            new byte[] {0x0b, 0x0a, 0x05, (byte)0x00},
            new byte[] {0x0b, (byte)0x8a, 0x05, (byte)0xa0},
            new byte[] {0x0b, (byte)0x8a, 0x05, (byte)0xa0},
            new byte[] {0x0b, (byte)0x8a, 0x05, (byte)0xa0}
        };
        
        //lets make empty content for alaw
        private byte[] zeroContent=new byte[160];
        
        public Sender(int port) throws SocketException {
        	for(int i=0;i<zeroContent.length;i++)
        		zeroContent[i]=(byte)0xD5;
        	
            this.port = port;
            dst = new InetSocketAddress("127.0.0.1", port);
            socket = new DatagramSocket(new InetSocketAddress("127.0.0.1", 9200));
            this.generateSequence(250);
        }
        
        private void generateSequence(int len) {
            boolean event = false;
            boolean marker = false;
            int t=0;
            int count = 0;
            
            int it = 12345;
            for (int i = 0; i < len; i++) {
                RtpPacket p = new RtpPacket(172, false);
                
                if (i % 50 == 0 && i > 0) {
                   event = true;  
                   marker = true;
                   t = 160 * (i + 1);
                   count = 0;
                } 
                
                if (event) {
                    p.wrap(marker, 101, i + 1, t + it, 123, evt1[count++], 0, 4);
                    marker = false;
                    if (count == evt1.length) {
                        event = false;
                    }
                } else {
                    p.wrap(marker, 8, i + 1, 160 * (i + 1) + it, 123, zeroContent, 0, 160);
                }
                
                byte[] data = new byte[p.getBuffer().limit()];
                p.getBuffer().get(data);
                stream.add(data);
            }
        }
        
        @Override
        public void run() {
            while (!stream.isEmpty()) {
                byte[] data = stream.remove(0);
                try {
                    DatagramPacket p = new DatagramPacket(data, 0, data.length, dst);
                    socket.send(p);
                    
                    Thread.sleep(20);
                } catch (Exception e) {
                }
            }
        }
        
        public void close() {
            if (socket != null) {
                socket.close();
            }
        }
    }        
}