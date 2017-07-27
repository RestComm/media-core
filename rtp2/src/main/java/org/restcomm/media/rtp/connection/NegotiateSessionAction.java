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

import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.connection.exception.RtpConnectionException;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * Negotiates the RTP session and connects it to the remote peer.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>RTP_SESSION</li>
 * <li>REMOTE_SDP</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>n/a</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NegotiateSessionAction extends AnonymousAction<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> {

    @Override
    public void execute(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext context, RtpConnectionFsm stateMachine) {
        // Get input parameters
        final RtpSession session = context.get(RtpConnectionTransitionParameter.RTP_SESSION, RtpSession.class);
        final SessionDescription remoteSdp = context.get(RtpConnectionTransitionParameter.REMOTE_SDP, SessionDescription.class);

        // Retrieve remote audio session description
        MediaDescriptionField audioDescription = remoteSdp.getMediaDescription(MediaType.AUDIO.name().toLowerCase());

        if (audioDescription == null) {
            // Abort negotiation if remote peer does not declare an audio session
            RtpConnectionException exception = new RtpConnectionException("Remote peer did not declare an audio session");
            context.set(RtpConnectionTransitionParameter.ERROR, exception);
            stateMachine.fire(RtpConnectionEvent.FAILURE, context);
        } else {
            // Negotiate session
            NegotiateSessionCallback callback = new NegotiateSessionCallback(context, stateMachine);
            session.negotiate(audioDescription, callback);
        }
    }

}
