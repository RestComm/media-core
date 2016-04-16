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

package org.mobicents.media.core.connections;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.core.MyTestEndpoint;
import org.mobicents.media.core.ResourcesPool;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerPool;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;

/**
 * @author oifa yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class ReclaimingTest {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;

    //endpoint and connection
    private MyTestEndpoint endpoint1;
    private MyTestEndpoint endpoint2;

    private ChannelsManager channelsManager;
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    // Resources
    private ResourcesPool resourcesPool;
    private RtpConnectionFactory rtpConnectionFactory;
    private RtpConnectionPool rtpConnectionPool;
    private LocalConnectionFactory localConnectionFactory;
    private LocalConnectionPool localConnectionPool;
    private AudioPlayerFactory playerFactory;
    private AudioPlayerPool playerPool;
    
    @Before
    public void setUp() throws ResourceUnavailableException, TooManyConnectionsException, IOException {
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
        resourcesPool=new ResourcesPool(mediaScheduler, channelsManager, dspFactory, rtpConnectionPool, localConnectionPool, playerPool);

        //assign scheduler to the endpoint
        endpoint1 = new MyTestEndpoint("test-1");
        endpoint1.setScheduler(mediaScheduler);
        endpoint1.setResourcesPool(resourcesPool);
        endpoint1.setFreq(200);
        endpoint1.start();

        endpoint2 = new MyTestEndpoint("test-2");
        endpoint2.setScheduler(mediaScheduler);
        endpoint2.setResourcesPool(resourcesPool);
        endpoint2.setFreq(200);
        endpoint2.start();

    }

    @After
    public void tearDown() {
        if (endpoint1 != null) {
            endpoint1.stop();
        }

        if (endpoint2 != null) {
            endpoint2.stop();
        }

        mediaScheduler.stop();
    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
    public void testForLocalConnections() throws Exception {
        Connection connection1 = endpoint1.createConnection(ConnectionType.LOCAL,false);
        endpoint1.deleteConnection(connection1);
    }

    public void testForRTPConnections() throws Exception {
        Connection connection1 = endpoint1.createConnection(ConnectionType.RTP,false);
        endpoint1.deleteConnection(connection1);
    }
    
//    @Test
    public void testLocalConnections() throws Exception {
        for (int i = 0; i < 500; i++) {
            //System.out.println("Test #" + i);
            this.testForLocalConnections();
        }
    }

    @Test
    public void testRTPConnections() throws Exception {
        for (int i = 0; i < 100; i++) {
            System.out.println("Test #" + i);
            Thread.sleep(50);
            this.testForRTPConnections();
        }
    }
    

}