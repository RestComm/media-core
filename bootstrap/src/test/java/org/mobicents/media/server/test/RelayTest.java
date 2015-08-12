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

package org.mobicents.media.server.test;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.ResourcesPool;
import org.mobicents.media.core.endpoints.impl.ConferenceEndpoint;
import org.mobicents.media.core.endpoints.impl.IvrEndpoint;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.utils.Text;

/**
 *
 * @author yulian oifa
 */
public class RelayTest {

    // clock and scheduler
    protected Clock clock;
    protected Scheduler scheduler;

    protected ChannelsManager channelsManager;

    private ResourcesPool resourcesPool;

    protected UdpManager udpManager;
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();

    // ivr endpoint
    private IvrEndpoint ivr;
    // analyzer
    private SoundSystem soundcard;

    // packet relay bridge
    private ConferenceEndpoint cnfBridge;

    public RelayTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException, TooManyConnectionsException, IOException {
        // use default clock
        clock = new DefaultClock();

        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder");

        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");

        // create single thread scheduler
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.setBindAddress("127.0.0.1");
        udpManager.start();

        channelsManager = new ChannelsManager(udpManager, dspFactory);
        channelsManager.setScheduler(scheduler);

        resourcesPool = new ResourcesPool(scheduler, channelsManager, dspFactory);

        // assign scheduler to the endpoint
        ivr = new IvrEndpoint("test-1", RelayType.MIXER, dspFactory.newProcessor());
        ivr.setScheduler(scheduler);
        ivr.setResourcesPool(resourcesPool);
        ivr.start();

        soundcard = new SoundSystem("test-2", dspFactory.newProcessor());
        soundcard.setScheduler(scheduler);
        soundcard.setResourcesPool(resourcesPool);
        soundcard.start();

        cnfBridge = new ConferenceEndpoint("test-3", RelayType.MIXER, dspFactory.newProcessor());
        cnfBridge.setScheduler(scheduler);
        cnfBridge.setResourcesPool(resourcesPool);
        cnfBridge.start();
    }

    @After
    public void tearDown() {
        udpManager.stop();
        scheduler.stop();

        if (ivr != null) {
            ivr.stop();
        }

        if (soundcard != null) {
            soundcard.stop();
        }

        if (cnfBridge != null) {
            cnfBridge.stop();
        }

    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
    // @Test
    public void testTransmission() throws Exception {
        // create client
        Connection connection2 = soundcard.createConnection(ConnectionType.RTP, false);
        Text sd2 = new Text(connection2.getDescriptor());
        connection2.setMode(ConnectionMode.SEND_RECV);
        Thread.sleep(50);

        // create server with known sdp in cnf mode
        Connection connection02 = cnfBridge.createConnection(ConnectionType.RTP, false);
        Text sd1 = new Text(connection02.getDescriptor());

        connection02.setOtherParty(sd2);
        connection02.setMode(ConnectionMode.CONFERENCE);
        Thread.sleep(50);

        // modify client
        connection2.setOtherParty(sd1);
        connection2.setMode(ConnectionMode.SEND_RECV);
        Thread.sleep(50);

        // create local connection
        Connection connection1 = ivr.createConnection(ConnectionType.LOCAL, false);
        Connection connection01 = cnfBridge.createConnection(ConnectionType.LOCAL, false);
        Thread.sleep(50);

        // create in send_recv mode initially
        connection1.setOtherParty(connection01);
        connection1.setMode(ConnectionMode.INACTIVE);
        connection01.setMode(ConnectionMode.SEND_RECV);

        Thread.sleep(150);

        // modify mode
        connection01.setMode(ConnectionMode.CONFERENCE);
        connection1.setMode(ConnectionMode.SEND_RECV);

        Thread.sleep(350);

        Player player = (Player) ivr.getResource(MediaType.AUDIO, ComponentType.PLAYER);
        player.setURL("file:///home/kulikov/jsr-309-tck/media/dtmfs-1-9.wav");
        player.start();

        Thread.sleep(10000);
        ivr.deleteConnection(connection1);
        soundcard.deleteConnection(connection2);
        cnfBridge.deleteAllConnections();
    }

}