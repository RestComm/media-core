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

import java.net.SocketException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.restcomm.media.network.UdpManager;
import org.restcomm.media.resources.dtmf.DetectorImpl;
import org.restcomm.media.rtp.ChannelsManager;
import org.restcomm.media.rtp.LocalDataChannel;
import org.restcomm.media.rtp.crypto.DtlsSrtpServer;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.server.component.AbstractSource;
import org.restcomm.media.server.component.oob.OOBComponent;
import org.restcomm.media.server.component.oob.OOBInput;
import org.restcomm.media.server.component.oob.OOBSplitter;

/**
 *
 * @author oifa yulian
 */
public class LocalEventTest implements DtmfDetectorListener {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;
    private final Scheduler scheduler;

    private UdpManager udpManager;

    private ChannelsManager channelsManager;
    
    private DetectorImpl detector;
    
    private LocalDataChannel channel1,channel2;
    
    private Sender sender;
    
    private OOBSplitter oobSplitter1,oobSplitter2;
    
    private OOBComponent inputComponent;
    private OOBComponent outputComponent;
    
    private int count=0;
    
    private static final int cipherSuites[] = { 0xc030, 0xc02f, 0xc028, 0xc027, 0xc014, 0xc013, 0x009f, 0x009e, 0x006b, 0x0067,
            0x0039, 0x0033, 0x009d, 0x009c, 0x003d, 0x003c, 0x0035, 0x002f, 0xc02b };
    
    public LocalEventTest() {
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

        udpManager = new UdpManager(scheduler);
        scheduler.start();
        udpManager.start();
        
        channelsManager = new ChannelsManager(udpManager, mockedDtlsServerProvider);
        channelsManager.setScheduler(mediaScheduler);
        
        detector = new DetectorImpl("dtmf", mediaScheduler);
        detector.setVolume(-35);
        detector.setDuration(40);
        detector.addListener(this);
        
        channel1 = channelsManager.getLocalChannel();
        channel2 = channelsManager.getLocalChannel();
        channel1.join(channel2);
        
        oobSplitter1=new OOBSplitter(mediaScheduler);
        oobSplitter2=new OOBSplitter(mediaScheduler);
        
        sender = new Sender();                

        oobSplitter1.addOutsideComponent(channel1.getOOBComponent());        
        oobSplitter2.addInsideComponent(channel2.getOOBComponent());  
        
        outputComponent=new OOBComponent(1);
        outputComponent.addOutput(detector.getOOBOutput());
        outputComponent.updateMode(true,true);
        oobSplitter1.addInsideComponent(outputComponent);               
        
        inputComponent=new OOBComponent(2);
        inputComponent.addInput(sender.getOOBInput());
        inputComponent.updateMode(true,true);
        oobSplitter2.addOutsideComponent(inputComponent);    	
    }

    @After
    public void tearDown() {
    	channel1.unjoin();
    	oobSplitter1.stop();
    	oobSplitter2.stop();
    	sender.deactivate();
        udpManager.stop();
        mediaScheduler.stop();
        scheduler.stop();
    }

    @Test
    public void testTransmission() throws Exception {
    	channel1.updateMode(ConnectionMode.SEND_RECV);
    	channel2.updateMode(ConnectionMode.SEND_RECV);
    	oobSplitter1.start();
    	oobSplitter2.start();
    	detector.activate();
    	sender.activate();
    	
        Thread.sleep(5000);
        
        channel1.updateMode(ConnectionMode.INACTIVE);
        channel2.updateMode(ConnectionMode.INACTIVE);
        oobSplitter1.stop();
        oobSplitter2.stop();
    	detector.deactivate();
    	sender.deactivate();
    	
    	assertEquals(4,count);
    }

    @Override
    public void process(DtmfEvent event) {
    	count++;
        System.out.println("TONE=" + event.getTone());
    }
    
    private class Sender extends AbstractSource {
        
		private static final long serialVersionUID = 4468618469974148422L;

//		private Frame currFrame;        
        private OOBInput oobInput;        
        int index=0;
        
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
        
        public Sender() throws SocketException {
        	super("oob generator", mediaScheduler, PriorityQueueScheduler.INPUT_QUEUE);            
        	
        	index=0;
        	this.oobInput=new OOBInput(ComponentType.DTMF_GENERATOR.getType());
        	this.connect(oobInput);
        }
        
        public OOBInput getOOBInput()
        {
        	return this.oobInput;
        } 
        
        @Override
        public Frame evolve(long timestamp) {
    		if(index >= 200)
        		return null;    		    		
    	
    		Frame frame ;
    		if(index%50>=7)
    		{
    			frame = Memory.allocate(3);
    			frame.setOffset(0);
                frame.setLength(3);
                frame.setTimestamp(getMediaTime());
                frame.setDuration(20000000L);
    		}
    		else
    		{
    			frame = Memory.allocate(4);
    			byte[] data=frame.getData();
    			System.arraycopy(evt1[index%50], 0, data, 0, 4);
    			frame.setOffset(0);
                frame.setLength(4);
                frame.setTimestamp(getMediaTime());
                frame.setDuration(20000000L);
    		}
    		
            index++;
            return frame;
    	}
    	
    	@Override
        public void activate() {
            start();
        }
    	
    	@Override
        public void deactivate() {
            stop();
        } 
    }
}