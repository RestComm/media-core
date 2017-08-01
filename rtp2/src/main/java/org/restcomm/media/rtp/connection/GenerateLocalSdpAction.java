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

import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.connection.exception.RtpConnectionException;
import org.restcomm.media.rtp.sdp.SdpBuilder;
import org.restcomm.media.sdp.SessionDescription;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * Generates the local session description of the RTP Connection.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>CNAME</li>
 * <li>INBOUND</li>
 * <li>BIND_ADDRESS</li>
 * <li>EXTERNAL_ADDRESS</li>
 * <li>RTP_SESSION</li>
 * <li>SDP_BUILDER</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>LOCAL_SDP</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenerateLocalSdpAction extends AnonymousAction<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> {

    static final GenerateLocalSdpAction INSTANCE = new GenerateLocalSdpAction();

    GenerateLocalSdpAction() {
        super();
    }

    @Override
    public void execute(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext context, RtpConnectionFsm stateMachine) {
        // Get input parameters
        final String cname = context.get(RtpConnectionTransitionParameter.CNAME, String.class);
        final boolean inbound = context.get(RtpConnectionTransitionParameter.INBOUND, Boolean.class);
        final String localAddress = context.get(RtpConnectionTransitionParameter.BIND_ADDRESS, String.class);
        final String externalAddress = context.get(RtpConnectionTransitionParameter.EXTERNAL_ADDRESS, String.class);
        final RtpSession session = context.get(RtpConnectionTransitionParameter.RTP_SESSION, RtpSession.class);
        final SdpBuilder sdpBuilder = context.get(RtpConnectionTransitionParameter.SDP_BUILDER, SdpBuilder.class);

        try {
            // Generate local description
            SessionDescription localSdp = sdpBuilder.buildSessionDescription(!inbound, cname, localAddress, externalAddress, session);

            // Update context
            context.set(RtpConnectionTransitionParameter.LOCAL_SDP, localSdp);
            stateMachine.getContext().setLocalDescription(localSdp);

            // Fire event to move to next state
            stateMachine.fire(RtpConnectionEvent.GENERATED_LOCAL_SDP, context);
        } catch (Exception e) {
            // Update context
            RtpConnectionException error = new RtpConnectionException(e);
            context.set(RtpConnectionTransitionParameter.ERROR, error);

            // Move to corrupted state
            stateMachine.fire(RtpConnectionEvent.FAILURE, context);
        }
    }

}
