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

package org.mobicents.media.server.mgcp.endpoint.connection;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderFactory;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderPool;
import org.mobicents.media.server.impl.resource.dtmf.DtmfDetectorFactory;
import org.mobicents.media.server.impl.resource.dtmf.DtmfDetectorPool;
import org.mobicents.media.server.impl.resource.dtmf.DtmfGeneratorFactory;
import org.mobicents.media.server.impl.resource.dtmf.DtmfGeneratorPool;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerPool;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetectorFactory;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetectorPool;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGeneratorFactory;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGeneratorPool;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.mgcp.connection.BaseConnection;
import org.mobicents.media.server.mgcp.connection.LocalConnectionFactory;
import org.mobicents.media.server.mgcp.connection.LocalConnectionPool;
import org.mobicents.media.server.mgcp.connection.RtpConnectionFactory;
import org.mobicents.media.server.mgcp.connection.RtpConnectionPool;
import org.mobicents.media.server.mgcp.endpoint.MyTestEndpoint;
import org.mobicents.media.server.mgcp.resources.ResourcesPool;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author yulian oifa
 */
public class BaseConnectionFSM_FR_Test {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;

    //endpoint and connection
    private BaseConnection connection;
    private MyTestEndpoint endpoint;

    // Resources
    private ResourcesPool resourcesPool;
    private RtpConnectionFactory rtpConnectionFactory;
    private RtpConnectionPool rtpConnectionPool;
    private LocalConnectionFactory localConnectionFactory;
    private LocalConnectionPool localConnectionPool;
    private AudioPlayerFactory playerFactory;
    private AudioPlayerPool playerPool;
    private AudioRecorderFactory recorderFactory;
    private AudioRecorderPool recorderPool;
    private DtmfDetectorFactory dtmfDetectorFactory;
    private DtmfDetectorPool dtmfDetectorPool;
    private DtmfGeneratorFactory dtmfGeneratorFactory;
    private DtmfGeneratorPool dtmfGeneratorPool;
    private PhoneSignalDetectorFactory signalDetectorFactory;
    private PhoneSignalDetectorPool signalDetectorPool;
    private PhoneSignalGeneratorFactory signalGeneratorFactory;
    private PhoneSignalGeneratorPool signalGeneratorPool;
        
    //RTP
    private ChannelsManager channelsManager;
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
        
    private volatile int failureRate;

    private Random rnd = new Random();

    @Before
    public void setUp() throws ResourceUnavailableException, IOException {    	
        ConnectionState.OPEN.setTimeout(5);
        ConnectionState.HALF_OPEN.setTimeout(5);

        //use default clock
        clock = new WallClock();
        
        //create single thread scheduler 
        mediaScheduler = new PriorityQueueScheduler();
        mediaScheduler.setClock(clock);
        mediaScheduler.start();

        channelsManager = new ChannelsManager(new UdpManager(new ServiceScheduler()));
        channelsManager.setScheduler(mediaScheduler);
        
        // Resource
        this.rtpConnectionFactory = new RtpConnectionFactory(channelsManager, dspFactory);
        this.rtpConnectionPool = new RtpConnectionPool(0, rtpConnectionFactory);
        this.localConnectionFactory = new LocalConnectionFactory(channelsManager);
        this.localConnectionPool = new LocalConnectionPool(0, localConnectionFactory);
        this.playerFactory = new AudioPlayerFactory(mediaScheduler, dspFactory);
        this.playerPool = new AudioPlayerPool(0, playerFactory);
        this.recorderFactory = new AudioRecorderFactory(mediaScheduler);
        this.recorderPool = new AudioRecorderPool(0, recorderFactory);
        this.dtmfDetectorFactory = new DtmfDetectorFactory(mediaScheduler);
        this.dtmfDetectorPool = new DtmfDetectorPool(0, dtmfDetectorFactory);
        this.dtmfGeneratorFactory = new DtmfGeneratorFactory(mediaScheduler);
        this.dtmfGeneratorPool = new DtmfGeneratorPool(0, dtmfGeneratorFactory);
        this.signalDetectorFactory = new PhoneSignalDetectorFactory(mediaScheduler);
        this.signalDetectorPool = new PhoneSignalDetectorPool(0, signalDetectorFactory);
        this.signalGeneratorFactory = new PhoneSignalGeneratorFactory(mediaScheduler);
        this.signalGeneratorPool = new PhoneSignalGeneratorPool(0, signalGeneratorFactory);
        resourcesPool=new ResourcesPool(rtpConnectionPool, localConnectionPool, playerPool, recorderPool, dtmfDetectorPool, dtmfGeneratorPool, signalDetectorPool, signalGeneratorPool);
        
        //assign scheduler to the endpoint
        endpoint = new MyTestEndpoint("test");
        endpoint.setScheduler(mediaScheduler);
        endpoint.setResourcesPool(resourcesPool);
        endpoint.start();    	
    }

    @After
    public void tearDown() {
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
        }
        endpoint.deleteAllConnections();
        endpoint.stop();
        mediaScheduler.stop();
        
    }

    /**
     * Simulates connection behaivior.
     * 
     * Any expection or missmatch detected breakes test execution.
     *  
     * @throws Exception
     */
    private void doTestCreate() throws Exception {
        //step #1: create connection;
        connection = (BaseConnection) endpoint.createConnection(ConnectionType.LOCAL,false);

        //step #2: check state, expected state is NULL;
//        if (connection.getState() != ConnectionState.NULL) {
//            System.out.println("Created connection in not NULL state");
//            failureRate++;
//            return;
//        }

        //step #3: shift to HALF_OPEN state
//        System.out.println("Creating connection");
//        connection.bind();

        //step #4: wait to allow transition
        Thread.sleep(1000);
        if (connection.getState() != ConnectionState.HALF_OPEN) {
            System.out.println("Bound connection in state: " + connection.getState());
            failureRate++;
            return;
        }

        //step #5: generate next action.
        boolean timeout = rnd.nextBoolean();

        //step #6: follow to action
        if (timeout) {
            doTestTimeout();
        } else {
            doTestOpen();
        }
    }

    private void doTestTimeout() throws Exception {
        //step #1: wait for timeout
        Thread.sleep(7000);

        //step #2: check state
        if (connection.getState() != ConnectionState.NULL) {
            System.out.println("Timeed out connection in state: " + connection.getState());
            failureRate++;
        }
    }

    private void doTestOpen() throws Exception {
        //step #1: shift to OPEN state
//        System.out.println("Opening connection");
        connection.join();
        Thread.sleep(1000);

        //step #2: check state
        if (connection.getState() != ConnectionState.OPEN) {
            System.out.println("Opened connection in state: " + connection.getState());
            failureRate++;
            return;
        }

        //step #3: generate next action.
        boolean timeout = rnd.nextBoolean();

        //step #4: follow to action
        if (timeout) {
            doTestTimeout();
        } else {
            doTestClose();
        }
    }

    private void doTestClose() throws InterruptedException {
        //step #1: shift to OPEN state
//        System.out.println("Closing connection");
        connection.close();
        Thread.sleep(1000);

        //step #2: check state
        if (connection.getState() != ConnectionState.NULL) {
            System.out.println("Closed connection in state: " + connection.getState());
            failureRate++;
        }
    }

    @Test
    public void testFailureRate() throws Exception {
        int N = 1;
        for (int i = 0; i < N; i++) {
            System.out.println("Run test #" + i + ": failure rate = " + (double)failureRate/N);
            doTestCreate();
            assertTrue("Failure rate too big", failureRate == 0);
        }

    }

    @Test
    public void testNothing() {
        //unit tests without tests is not working
    }
}