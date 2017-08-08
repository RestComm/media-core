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

import org.restcomm.media.sdp.SessionDescription;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Warns caller that request operation succeeded and RTP Connection is open.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>CALLBACK</li>
 * <li>LOCAL_SDP</li>
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
public class NotifyHalfOpenAction extends AnonymousAction<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> {

    static final NotifyHalfOpenAction INSTANCE = new NotifyHalfOpenAction();

    NotifyHalfOpenAction() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext context, RtpConnectionFsm stateMachine) {
        final FutureCallback<String> callback = context.get(RtpConnectionTransitionParameter.CALLBACK, FutureCallback.class);
        final SessionDescription localSdp = context.get(RtpConnectionTransitionParameter.LOCAL_SDP, SessionDescription.class);
        callback.onSuccess(localSdp.toString());
    }

}
