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

package org.restcomm.media.control.mgcp.endpoint.splitter;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointAction;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointEvent;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointFsm;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointState;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointTransitionContext;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * Activates the in-band and out-of-band mixers of the endpoint.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>n/a</li>
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
public class ActivateSplitterAction
        extends AnonymousAction<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext>
        implements MgcpEndpointAction {

    private static final Logger log = Logger.getLogger(ActivateSplitterAction.class);
    
    static final MgcpEndpointAction INSTANCE = new ActivateSplitterAction();

    @Override
    public void execute(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event, MgcpEndpointTransitionContext context, MgcpEndpointFsm stateMachine) {
        MgcpSplitterEndpointContext globalContext = (MgcpSplitterEndpointContext) stateMachine.getContext();

        // Activate in-band mixer
        globalContext.getSplitter().start();

        if (log.isTraceEnabled()) {
            EndpointIdentifier endpointId = globalContext.getEndpointId();
            log.trace("Endpoint " + endpointId + " activated its in-band splitter.");
        }

        // Register connection to out-of-band mixer
        globalContext.getOobSplitter().start();

        if (log.isTraceEnabled()) {
            EndpointIdentifier endpointId = globalContext.getEndpointId();
            log.trace("Endpoint " + endpointId + " activated its out-of-band splitter.");
        }
    }

}
