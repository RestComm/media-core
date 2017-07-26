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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AllocatingSessionAction extends AnonymousAction<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionContext> {

    @Override
    public void execute(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionContext context, RtpConnectionFsm stateMachine) {
        // Get relevant data from context
        final String bindAddress = context.getBindAddress();
        final PortManager portManager = context.getPortManager();
        final RtpSessionFactory sessionFactory = context.getSessionFactory();

        // Create new RTP session
        RtpSession session = sessionFactory.build();

        // Allocate address for new session
        SocketAddress address = new InetSocketAddress(bindAddress, portManager.next());

        // Open session
        AllocatingSessionCallback callback = new AllocatingSessionCallback(context, stateMachine);
        session.open(address, callback);
    }

}
