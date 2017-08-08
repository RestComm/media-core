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

package org.restcomm.media.rtp.connection;

import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.rtp.CnameGenerator;
import org.restcomm.media.rtp.RtpConnection;
import org.restcomm.media.rtp.RtpConnectionFactory;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.rtp.sdp.SdpBuilder;
import org.restcomm.media.sdp.SessionDescriptionParser;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionImplFactory implements RtpConnectionFactory {

    private final String externalAddress;
    private final String localAddress;
    private final CnameGenerator cnameGenerator;
    private final SdpBuilder sdpBuilder;
    private final SessionDescriptionParser sdpParser;
    private final PortManager portManager;
    private final RtpSessionFactory sessionFactory;
    private final RtpConnectionFsmBuilder fsmBuilder;

    public RtpConnectionImplFactory(String externalAddress, String localAddress, CnameGenerator cnameGenerator,
            SdpBuilder sdpBuilder, SessionDescriptionParser sdpParser, PortManager portManager,
            RtpSessionFactory sessionFactory, RtpConnectionFsmBuilder fsmBuilder) {
        super();
        this.externalAddress = externalAddress;
        this.localAddress = localAddress;
        this.cnameGenerator = cnameGenerator;
        this.sdpBuilder = sdpBuilder;
        this.sdpParser = sdpParser;
        this.portManager = portManager;
        this.sessionFactory = sessionFactory;
        this.fsmBuilder = fsmBuilder;
    }

    public RtpConnection build() {
        final String cname = this.cnameGenerator.generateCname();
        RtpConnectionContext context = new RtpConnectionContext(cname, localAddress, externalAddress);
        RtpConnection connection = new RtpConnectionImpl(sessionFactory, portManager, sdpParser, sdpBuilder, context, fsmBuilder);
        return connection;
    }

}
