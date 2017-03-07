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
package org.restcomm.media.control.mgcp.endpoint.connection;

import java.io.IOException;

import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.restcomm.media.codec.g711.alaw.Decoder;
import org.restcomm.media.codec.g711.alaw.Encoder;
import org.restcomm.media.component.dsp.DspFactoryImpl;
import org.restcomm.media.core.configuration.DtlsConfiguration;
import org.restcomm.media.network.UdpManager;
import org.restcomm.media.rtp.ChannelsManager;
import org.restcomm.media.rtp.crypto.AlgorithmCertificate;
import org.restcomm.media.rtp.crypto.CipherSuite;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;

/**
 * Super class for all RTP transmission tests
 * 
 * @author yulian oifa
 */
public class RTPEnvironment {
    //clock and scheduler
    protected Clock clock;
    protected PriorityQueueScheduler mediaScheduler;
    protected final Scheduler scheduler = new ServiceScheduler();
    
    //Dtls Server Provider
    protected ProtocolVersion minVersion = ProtocolVersion.DTLSv10;
    protected ProtocolVersion maxVersion = ProtocolVersion.DTLSv12;
    protected CipherSuite[] cipherSuites = new DtlsConfiguration().getCipherSuites();
    protected String certificatePath = DtlsConfiguration.CERTIFICATE_PATH;
    protected String keyPath = DtlsConfiguration.KEY_PATH;
    protected AlgorithmCertificate algorithmCertificate = AlgorithmCertificate.RSA;
    protected DtlsSrtpServerProvider dtlsServerProvider = new DtlsSrtpServerProvider(minVersion, maxVersion, cipherSuites,
            certificatePath, keyPath, algorithmCertificate);

    protected ChannelsManager channelsManager;

    protected UdpManager udpManager;
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    public void setup() throws IOException {
        //use default clock
        clock = new WallClock();

        dspFactory.addCodec(Encoder.class.getName());
        dspFactory.addCodec(Decoder.class.getName());
        
        //create single thread scheduler
        mediaScheduler = new PriorityQueueScheduler();
        mediaScheduler.setClock(clock);
        mediaScheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.setBindAddress("127.0.0.1");
        scheduler.start();
        udpManager.start();

        channelsManager = new ChannelsManager(udpManager, dtlsServerProvider);
        channelsManager.setScheduler(mediaScheduler);
    }
    
    public void tearDown() {        
        udpManager.stop();
        scheduler.stop();
        mediaScheduler.stop();
    }
}
