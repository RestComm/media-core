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

import java.net.SocketAddress;

import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * Builds a new RTP Session and binds it to a local address.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>BIND_ADDRESS</li>
 * <li>RTP_SESSION_FACTORY</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>RTP_SESSION</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AllocateSessionAction extends AnonymousAction<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> {

    @Override
    public void execute(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext context, RtpConnectionFsm stateMachine) {
        // Get relevant data from context
        final SocketAddress bindAddress = context.get(RtpConnectionTransitionParameter.BIND_ADDRESS, SocketAddress.class);
        final RtpSessionFactory sessionFactory = context.get(RtpConnectionTransitionParameter.RTP_SESSION_FACTORY, RtpSessionFactory.class);

        // Create new RTP session
        RtpSession session = sessionFactory.build();
        
        // Update context
        context.set(RtpConnectionTransitionParameter.RTP_SESSION, session);
        stateMachine.getContext().setRtpSession(session);

        // Open session
        AllocateSessionCallback callback = new AllocateSessionCallback(context, stateMachine);
        session.open(bindAddress, callback);
    }

}
