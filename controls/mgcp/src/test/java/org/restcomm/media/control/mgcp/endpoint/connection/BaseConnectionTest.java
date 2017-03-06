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

package org.restcomm.media.control.mgcp.endpoint.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.spi.ConnectionEvent;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.restcomm.media.control.mgcp.connection.BaseConnection;
import org.restcomm.media.control.mgcp.connection.LocalConnectionFactory;
import org.restcomm.media.control.mgcp.connection.LocalConnectionPool;
import org.restcomm.media.control.mgcp.connection.RtpConnectionFactory;
import org.restcomm.media.control.mgcp.connection.RtpConnectionPool;
import org.restcomm.media.control.mgcp.endpoint.MyTestEndpoint;
import org.restcomm.media.control.mgcp.resources.ResourcesPool;
import org.restcomm.media.core.configuration.DtlsConfiguration;
import org.restcomm.media.network.UdpManager;
import org.restcomm.media.resource.player.audio.AudioPlayerFactory;
import org.restcomm.media.resource.player.audio.AudioPlayerPool;
import org.restcomm.media.resource.player.audio.CachedRemoteStreamProvider;
import org.restcomm.media.resource.recorder.audio.AudioRecorderFactory;
import org.restcomm.media.resource.recorder.audio.AudioRecorderPool;
import org.restcomm.media.resources.dtmf.DtmfDetectorFactory;
import org.restcomm.media.resources.dtmf.DtmfDetectorPool;
import org.restcomm.media.resources.dtmf.DtmfGeneratorFactory;
import org.restcomm.media.resources.dtmf.DtmfGeneratorPool;
import org.restcomm.media.rtp.ChannelsManager;
import org.restcomm.media.rtp.crypto.AlgorithmCertificate;
import org.restcomm.media.rtp.crypto.CipherSuite;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.server.component.dsp.DspFactoryImpl;

/**
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class BaseConnectionTest implements ConnectionListener {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;

    //endpoint and connection
    private BaseConnection connection;
    private MyTestEndpoint endpoint;

    private boolean halfOpenState;
    private boolean openState;
    private boolean nullState;

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

    //Dtls Server Provider
    protected ProtocolVersion minVersion = ProtocolVersion.DTLSv10;
    protected ProtocolVersion maxVersion = ProtocolVersion.DTLSv12;
    protected CipherSuite[] cipherSuites = new DtlsConfiguration().getCipherSuites();
    protected String certificatePath = DtlsConfiguration.CERTIFICATE_PATH;
    protected String keyPath = DtlsConfiguration.KEY_PATH;
    protected AlgorithmCertificate algorithmCertificate = AlgorithmCertificate.RSA;
    protected DtlsSrtpServerProvider dtlsServerProvider = new DtlsSrtpServerProvider(minVersion, maxVersion, cipherSuites,
            certificatePath, keyPath, algorithmCertificate);
    
    private ChannelsManager channelsManager;
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    @Before
    public void setUp() throws ResourceUnavailableException, IOException, TooManyConnectionsException {
        //use default clock

        clock = new WallClock();
        
        //create single thread scheduler 
        mediaScheduler = new PriorityQueueScheduler();
        mediaScheduler.setClock(clock);
        mediaScheduler.start();

        channelsManager = new ChannelsManager(new UdpManager(new ServiceScheduler()), dtlsServerProvider);
        channelsManager.setScheduler(mediaScheduler);        

        // Resource
        this.rtpConnectionFactory = new RtpConnectionFactory(channelsManager, dspFactory);
        this.rtpConnectionPool = new RtpConnectionPool(0, rtpConnectionFactory);
        this.localConnectionFactory = new LocalConnectionFactory(channelsManager);
        this.localConnectionPool = new LocalConnectionPool(0, localConnectionFactory);
        this.playerFactory = new AudioPlayerFactory(mediaScheduler, dspFactory, new CachedRemoteStreamProvider(100));
        this.playerPool = new AudioPlayerPool(0, playerFactory);
        this.recorderFactory = new AudioRecorderFactory(mediaScheduler);
        this.recorderPool = new AudioRecorderPool(0, recorderFactory);
        this.dtmfDetectorFactory = new DtmfDetectorFactory(mediaScheduler);
        this.dtmfDetectorPool = new DtmfDetectorPool(0, dtmfDetectorFactory);
        this.dtmfGeneratorFactory = new DtmfGeneratorFactory(mediaScheduler);
        this.dtmfGeneratorPool = new DtmfGeneratorPool(0, dtmfGeneratorFactory);
        resourcesPool=new ResourcesPool(rtpConnectionPool, localConnectionPool, playerPool, recorderPool, dtmfDetectorPool, dtmfGeneratorPool);

        //assign scheduler to the endpoint
        endpoint = new MyTestEndpoint("test");
        endpoint.setScheduler(mediaScheduler);
        endpoint.setResourcesPool(resourcesPool);
        endpoint.start();

        connection = (BaseConnection) endpoint.createConnection(ConnectionType.LOCAL,false);
        connection.addListener(this);
    }

    @After
    public void tearDown() {
        endpoint.deleteAllConnections();
        
        endpoint.stop();
        mediaScheduler.stop();
    }

    /**
     * Test of getId method, of class BaseConnection.
     */
    @Test
    public void testGetId() {
        //assertEquals("1", connection.getId());
    }

    /**
     * Test of getState method, of class BaseConnection.
     */
//    @Test
    public void testGetState() {
        assertEquals(ConnectionState.NULL, connection.getState());
    }

    /**
     * Test of getEndpoint method, of class BaseConnection.
     */
//    @Test
    public void testGetEndpoint() {
        assertEquals(endpoint, connection.getEndpoint());
    }
    
    /**
     * Test of bind method, of class BaseConnection.
     */
//    @Test
    public void testBind() throws Exception {
//        assertEquals(ConnectionState.NULL, connection.getState());
//        connection.bind();

        Thread.sleep(1000);
        assertEquals(ConnectionState.HALF_OPEN, connection.getState());
        assertTrue("Listener did not receive event", halfOpenState);
    }

    /**
     * Test of bind method, of class BaseConnection.
     */
//    @Test
    public void testTimeout() throws Exception {
//        assertEquals(ConnectionState.NULL, connection.getState());
//        connection.bind();

        Thread.sleep(10000);
        assertEquals(ConnectionState.NULL, connection.getState());
    }

    /**
     * Test of join method, of class BaseConnection.
     */
//    @Test
    public void testJoin() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());
        connection.bind();
        Thread.sleep(500);
        
        connection.join();

        Thread.sleep(1000);
        assertEquals(ConnectionState.OPEN, connection.getState());
        assertTrue("Listener did not receive event", openState);
    }

    /**
     * Test of close method, of class BaseConnection.
     */
//    @Test
    public void testClose() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());

        connection.bind();
        Thread.sleep(500);

        connection.join();
        Thread.sleep(500);
        
        connection.close();
        Thread.sleep(1000);

        assertEquals(ConnectionState.NULL, connection.getState());

        assertTrue("Listener did not receive event", nullState);
    }

    public void process(ConnectionEvent event) {
        if (event.getId() == ConnectionEvent.STATE_CHANGE) {
            BaseConnection conn = (BaseConnection) event.getSource();

            if (conn.getState() == ConnectionState.HALF_OPEN) {
                halfOpenState = true;
            }

            if (conn.getState() == ConnectionState.OPEN) {
                openState = true;
            }

            if (conn.getState() == ConnectionState.NULL) {
                nullState = true;
            }
        }
    }

}