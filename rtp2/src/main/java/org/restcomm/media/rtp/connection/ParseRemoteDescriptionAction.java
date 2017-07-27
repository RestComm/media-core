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

import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.SessionDescriptionParser;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * Parses remote session description.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>REMOTE_SDP_STRING</li>
 * <li>SDP_PARSER</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>REMOTE_SDP</li>
 * </ul>
 * </p>
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ParseRemoteDescriptionAction extends AnonymousAction<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> {

    @Override
    public void execute(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext context, RtpConnectionFsm stateMachine) {
        // Get input parameters
        final String remoteSdp = context.get(RtpConnectionTransitionParameter.REMOTE_SDP_STRING, String.class);
        final SessionDescriptionParser parser = context.get(RtpConnectionTransitionParameter.SDP_PARSER, SessionDescriptionParser.class);
        
        try {
            // Parse SDP
            SessionDescription remoteSessionDescription = parser.parse(remoteSdp);
            context.set(RtpConnectionTransitionParameter.REMOTE_SDP, remoteSessionDescription);
            stateMachine.fire(RtpConnectionEvent.PARSED_REMOTE_SDP, context);
        } catch (SdpException e) {
            // Move to corrupted state if parsing fails
            context.set(RtpConnectionTransitionParameter.ERROR, e);
            stateMachine.fire(RtpConnectionEvent.FAILURE, context);
        }
    }

}
