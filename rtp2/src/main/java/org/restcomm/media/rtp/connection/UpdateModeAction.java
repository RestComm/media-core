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
import org.restcomm.media.spi.ConnectionMode;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * Updates the mode of an RTP Connection.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>MODE</li>
 * <li>RTP_SESSION</li>
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
public class UpdateModeAction extends AnonymousAction<RtpConnectionFsm, RtpConnectionState, RtpConnectionEvent, RtpConnectionTransitionContext> {
    
    static final UpdateModeAction INSTANCE = new UpdateModeAction();
    
    UpdateModeAction() {
        super();
    }
    
    @Override
    public void execute(RtpConnectionState from, RtpConnectionState to, RtpConnectionEvent event, RtpConnectionTransitionContext context, RtpConnectionFsm stateMachine) {
        // Get relevant data from context
        final ConnectionMode mode = context.get(RtpConnectionTransitionParameter.MODE, ConnectionMode.class);
        final RtpSession session = context.get(RtpConnectionTransitionParameter.RTP_SESSION, RtpSession.class);
        
        // Do nothing if required mode equals current mode
        final ConnectionMode currentMode = stateMachine.getContext().getMode();
        if(currentMode.equals(mode)) {
            stateMachine.fire(RtpConnectionEvent.MODE_UPDATED, context);
        } else {
            // Update context
            stateMachine.getContext().setMode(mode);
            
            // Ask session to update its mode
            UpdateModeCallback callback = new UpdateModeCallback(context, stateMachine);
            session.updateMode(mode, callback);
        }
    }

}
