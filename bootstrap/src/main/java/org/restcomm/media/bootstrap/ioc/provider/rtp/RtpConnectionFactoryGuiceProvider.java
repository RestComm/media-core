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

package org.restcomm.media.bootstrap.ioc.provider.rtp;

import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.core.configuration.NetworkConfiguration;
import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.rtp.CnameGenerator;
import org.restcomm.media.rtp.RtpConnectionFactory;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.rtp.connection.RtpConnectionFsmBuilder;
import org.restcomm.media.rtp.connection.RtpConnectionImplFactory;
import org.restcomm.media.rtp.sdp.SdpBuilder;
import org.restcomm.media.sdp.SessionDescriptionParser;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionFactoryGuiceProvider implements Provider<RtpConnectionFactory> {

    private final MediaServerConfiguration configuration;
    private final SdpBuilder sdpBuilder;
    private final SessionDescriptionParser sdpParser;
    private final PortManager portManager;
    private final CnameGenerator cnameGenerator;
    private final RtpSessionFactory rtpSessionFactory;
    private final RtpConnectionFsmBuilder fsmBuilder;

    @Inject
    public RtpConnectionFactoryGuiceProvider(MediaServerConfiguration configuration, SdpBuilder sdpBuilder,
            SessionDescriptionParser sdpParser, PortManager portManager, CnameGenerator cnameGenerator,
            RtpSessionFactory rtpSessionFactory, RtpConnectionFsmBuilder fsmBuilder) {
        super();
        this.configuration = configuration;
        this.sdpBuilder = sdpBuilder;
        this.sdpParser = sdpParser;
        this.portManager = portManager;
        this.cnameGenerator = cnameGenerator;
        this.rtpSessionFactory = rtpSessionFactory;
        this.fsmBuilder = fsmBuilder;
    }

    @Override
    public RtpConnectionFactory get() {
        NetworkConfiguration networkConfig = this.configuration.getNetworkConfiguration();
        String localAddress = networkConfig.getBindAddress();
        String externalAddress = networkConfig.getExternalAddress();
        return new RtpConnectionImplFactory(externalAddress, localAddress, this.cnameGenerator, this.sdpBuilder, this.sdpParser, this.portManager, this.rtpSessionFactory, this.fsmBuilder);
    }

}
