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
import org.squirrelframework.foundation.fsm.StateMachineStatus;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpLocalConnectionFsmImpl extends AbstractStateMachine<MgcpLocalConnectionFsm, MgcpLocalConnectionState, MgcpLocalConnectionEvent, MgcpLocalConnectionTransitionContext> {

    private static final Logger log = Logger.getLogger(MgcpLocalConnectionFsmImpl.class);

    private final MgcpLocalConnectionContext context;

    public MgcpLocalConnectionFsmImpl(MgcpLocalConnectionContext context) {
        super();
        this.context = context;
    }

    public MgcpLocalConnectionContext getContext() {
        return context;
    }

    @Override
    protected void afterTransitionCompleted(MgcpLocalConnectionState fromState, MgcpLocalConnectionState toState, MgcpLocalConnectionEvent event, MgcpLocalConnectionTransitionContext context) {
        if (log.isDebugEnabled()) {
            log.debug("MGCP local connection " + this.context.getHexIdentifier() + " is " + toState.name());
        }
    }
    
    @Override
    protected void afterTransitionCausedException(MgcpLocalConnectionState fromState, MgcpLocalConnectionState toState,
            MgcpLocalConnectionEvent event, MgcpLocalConnectionTransitionContext context) {
        // Set FSM state to IDLE so it can be process other events
        setStatus(StateMachineStatus.IDLE);
        
        // Move FSM to CORRUPTED so it can be cleaned properly in future
        context.set(MgcpLocalConnectionParameter.ERROR, getLastException().getTargetException());
        fireImmediate(MgcpLocalConnectionEvent.FAILURE, context);
    }

}
