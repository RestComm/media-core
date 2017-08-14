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
        
package org.restcomm.media.control.mgcp.connection.local;

import org.apache.log4j.Logger;
import org.restcomm.media.rtp.LocalDataChannel;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.ModeNotSupportedException;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;


/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UpdateModeAction extends AnonymousAction<MgcpLocalConnectionFsm, MgcpLocalConnectionState, MgcpLocalConnectionEvent, MgcpLocalConnectionTransitionContext> implements MgcpLocalConnectionAction {

    private static final Logger log = Logger.getLogger(UpdateModeAction.class);
    
    static final UpdateModeAction INSTANCE = new UpdateModeAction();
    
    UpdateModeAction() {
        super();
    }

    @Override
    public void execute(MgcpLocalConnectionState from, MgcpLocalConnectionState to, MgcpLocalConnectionEvent event, MgcpLocalConnectionTransitionContext context, MgcpLocalConnectionFsm stateMachine) {
        MgcpLocalConnectionContext globalContext = stateMachine.getContext();
        LocalDataChannel audioChannel = globalContext.getAudioChannel();
        ConnectionMode currentMode = globalContext.getMode();
        ConnectionMode newMode = context.get(MgcpLocalConnectionParameter.MODE, ConnectionMode.class);
        FutureCallback<?> callback = context.get(MgcpLocalConnectionParameter.CALLBACK, FutureCallback.class);

        if(currentMode.equals(newMode)) {
            // Mode is the same. Take no further action
            callback.onSuccess(null);
        } else {
            try {
                // Update data channel mode
                audioChannel.updateMode(newMode);
                
                // Update mode in context
                globalContext.setMode(newMode);
                
                if(log.isDebugEnabled()) {
                    log.debug("MGCP local connection " + globalContext.getHexIdentifier() + " mode is set to " + newMode.name());
                }
                
                // Notify callback
                callback.onSuccess(null);
            } catch (ModeNotSupportedException e) {
                log.warn("Could not update data channel mode of local connection " + globalContext.getHexIdentifier());
                callback.onFailure(e);
            }
        }
    }

}
