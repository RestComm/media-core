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

package org.restcomm.media.rtp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.audio.Sine;
import org.restcomm.media.component.audio.SpectraAnalyzer;
import org.restcomm.media.core.spi.ConnectionMode;
import org.restcomm.media.network.deprecated.RtpPortManager;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.rtp.ChannelsManager;
import org.restcomm.media.rtp.LocalDataChannel;
import org.restcomm.media.rtp.crypto.DtlsSrtpServer;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;

/**
 *
 * @author oifa yulian
 */
public class LocalChannelTest {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;
    private final Scheduler scheduler;

    private ChannelsManager channelsManager;
    private UdpManager udpManager;

    private SpectraAnalyzer analyzer1,analyzer2;
    private Sine source1,source2;

    private LocalDataChannel channel1,channel2;
    
    private int fcount;

    private AudioMixer audioMixer1,audioMixer2;
    private AudioComponent component1,component2;
    
    private static final int cipherSuites[] = { 0xc030, 0xc02f, 0xc028, 0xc027, 0xc014, 0xc013, 0x009f, 0x009e, 0x006b, 0x0067,
            0x0039, 0x0033, 0x009d, 0x009c, 0x003d, 0x003c, 0x0035, 0x002f, 0xc02b };
    
    public LocalChannelTest() {
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

        source1 = new Sine(mediaScheduler);
        source1.setFrequency(50);        
        
        source2 = new Sine(mediaScheduler);
        source2.setFrequency(100);
        
        analyzer1 = new SpectraAnalyzer("analyzer",mediaScheduler);        
        analyzer2 = new SpectraAnalyzer("analyzer",mediaScheduler);
        
        channel1 = channelsManager.getLocalChannel();
        channel2 = channelsManager.getLocalChannel();
        channel1.join(channel2);
        
        audioMixer1=new AudioMixer(mediaScheduler);
        audioMixer2=new AudioMixer(mediaScheduler);
        
        component1=new AudioComponent(1);
        component1.addInput(source1.getAudioInput());
        component1.addOutput(analyzer1.getAudioOutput());
        component1.updateMode(true,true);
        
        audioMixer1.addComponent(component1);
        audioMixer1.addComponent(channel1.getAudioComponent());
        
        component2=new AudioComponent(2);
        component2.addInput(source2.getAudioInput());
        component2.addOutput(analyzer2.getAudioOutput());
        component2.updateMode(true,true);
        
        audioMixer2.addComponent(component2);
        audioMixer2.addComponent(channel2.getAudioComponent());        
    }

    @After
    public void tearDown() {
    	source1.deactivate();
    	source2.deactivate();
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
    	channel1.unjoin();    	    	
    	channel2.unjoin();
    	
    	audioMixer1.stop();
    	audioMixer2.stop();
    	
        udpManager.stop();
        mediaScheduler.stop();
        scheduler.stop();
    }

    @Test
    public void testTransmission() throws Exception {
    	channel1.updateMode(ConnectionMode.SEND_RECV);
    	channel2.updateMode(ConnectionMode.SEND_RECV);
    	
        source1.activate();
        source2.activate();
        analyzer1.activate();
        analyzer2.activate();
    	audioMixer1.start();
    	audioMixer2.start();
        
        Thread.sleep(5000);
        
        analyzer1.deactivate();
        analyzer2.deactivate();
        source1.deactivate();
        source2.deactivate();
        audioMixer1.stop();        
        audioMixer2.stop();
        channel1.updateMode(ConnectionMode.INACTIVE);
        
        int s1[] = analyzer1.getSpectra();
        int s2[] = analyzer2.getSpectra();
        
        if (s1.length != 1 || s2.length != 1) {
            System.out.println("Failure ,s1:" + s1.length + ",s2:" + s2.length);
            fcount++;
        } else System.out.println("Passed");

        assertEquals(1, s1.length);    	
        assertEquals(1, s2.length);
        
        assertEquals(100, s1[0], 5);
        assertEquals(50, s2[0], 5);
    }
}