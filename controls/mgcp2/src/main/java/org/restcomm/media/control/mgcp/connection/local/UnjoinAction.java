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
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UnjoinAction extends AnonymousAction<MgcpLocalConnectionFsm, MgcpLocalConnectionState, MgcpLocalConnectionEvent, MgcpLocalConnectionTransitionContext> implements MgcpLocalConnectionAction {

    private static final Logger log = Logger.getLogger(UnjoinAction.class);
    
    static final UnjoinAction INSTANCE = new UnjoinAction();
    
    UnjoinAction() {
        super();
    }

    @Override
    public void execute(MgcpLocalConnectionState from, MgcpLocalConnectionState to, MgcpLocalConnectionEvent event, MgcpLocalConnectionTransitionContext context, MgcpLocalConnectionFsm stateMachine) {
        final MgcpLocalConnectionContext globalContext = stateMachine.getContext();
        LocalDataChannel audioChannel = globalContext.getAudioChannel();
        
        // Disconnect connection
        audioChannel.unjoin();
        
        if(log.isDebugEnabled()) {
            log.debug("MGCP Connection " + globalContext.getHexIdentifier() + " unjoined.");
        }
    }

}
