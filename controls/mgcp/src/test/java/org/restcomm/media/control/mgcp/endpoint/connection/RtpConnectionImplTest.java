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

import java.io.IOException;

import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.restcomm.media.component.dsp.DspFactoryImpl;
import org.restcomm.media.control.mgcp.connection.LocalConnectionFactory;
import org.restcomm.media.control.mgcp.connection.LocalConnectionPool;
import org.restcomm.media.control.mgcp.connection.RtpConnectionFactory;
import org.restcomm.media.control.mgcp.connection.RtpConnectionImpl;
import org.restcomm.media.control.mgcp.connection.RtpConnectionPool;
import org.restcomm.media.control.mgcp.endpoint.MyTestEndpoint;
import org.restcomm.media.control.mgcp.resources.ResourcesPool;
import org.restcomm.media.core.configuration.DtlsConfiguration;
import org.restcomm.media.network.RtpPortManager;
import org.restcomm.media.network.UdpManager;
import org.restcomm.media.resource.dtmf.DtmfDetectorFactory;
import org.restcomm.media.resource.dtmf.DtmfDetectorPool;
import org.restcomm.media.resource.dtmf.DtmfGeneratorFactory;
import org.restcomm.media.resource.dtmf.DtmfGeneratorPool;
import org.restcomm.media.resource.player.audio.AudioPlayerFactory;
import org.restcomm.media.resource.player.audio.AudioPlayerPool;
import org.restcomm.media.resource.player.audio.CachedRemoteStreamProvider;
import org.restcomm.media.resource.recorder.audio.AudioRecorderFactory;
import org.restcomm.media.resource.recorder.audio.AudioRecorderPool;
import org.restcomm.media.rtp.ChannelsManager;
import org.restcomm.media.rtp.crypto.AlgorithmCertificate;
import org.restcomm.media.rtp.crypto.CipherSuite;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.spi.ConnectionType;
import org.restcomm.media.spi.ResourceUnavailableException;

/**
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtpConnectionImplTest {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;
    
    //Dtls Server Provider
    protected ProtocolVersion minVersion = ProtocolVersion.DTLSv10;
    protected ProtocolVersion maxVersion = ProtocolVersion.DTLSv12;
    protected CipherSuite[] cipherSuites = new DtlsConfiguration().getCipherSuites();
    protected String certificatePath = DtlsConfiguration.CERTIFICATE_PATH;
    protected String keyPath = DtlsConfiguration.KEY_PATH;
    protected AlgorithmCertificate algorithmCertificate = AlgorithmCertificate.RSA;
    protected DtlsSrtpServerProvider dtlsServerProvider = new DtlsSrtpServerProvider(minVersion, maxVersion, cipherSuites,
            certificatePath, keyPath, algorithmCertificate);

    //RTP
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
    private AudioRecorderFactory recorderFactory;
    private AudioRecorderPool recorderPool;
    private DtmfDetectorFactory dtmfDetectorFactory;
    private DtmfDetectorPool dtmfDetectorPool;
    private DtmfGeneratorFactory dtmfGeneratorFactory;
    private DtmfGeneratorPool dtmfGeneratorPool;
    
    //endpoint and connection
    private RtpConnectionImpl connection;
    private MyTestEndpoint endpoint;
    
    @Before
    public void setUp() throws ResourceUnavailableException, IOException {
        //use default clock
        clock = new WallClock();

        //create single thread scheduler
        mediaScheduler = new PriorityQueueScheduler();
        mediaScheduler.setClock(clock);
        mediaScheduler.start();

        channelsManager = new ChannelsManager(new UdpManager(new ServiceScheduler(), new RtpPortManager(), new RtpPortManager()), dtlsServerProvider);
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

    }

    @After
    public void tearDown() {
    	try
    	{
        endpoint.deleteAllConnections();
        endpoint.stop();
        mediaScheduler.stop();
    	}
    	catch(Exception ex)
    	{
    		ex.printStackTrace();
    	}
    }

    /**
     * Test of setOtherParty method, of class RtpConnectionImpl.
     */
    @Ignore
    @Test
    public void testDescription() throws Exception {    	
        connection = (RtpConnectionImpl) endpoint.createConnection(ConnectionType.RTP,false);
//        connection.bind();
        Thread.sleep(1000);

        System.out.println(connection.getDescriptor());
    }

}