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
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioSplitter;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBSplitter;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointAction;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointEvent;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointFsm;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointParameter;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointState;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointTransitionContext;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * Unregisters a set of connections from the in-band and out-of-band mixers of the endpoint.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>UNREGISTERED_CONNECTIONS</li>
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
public class UnregisterConnectionsFromSplitterAction
        extends AnonymousAction<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext>
        implements MgcpEndpointAction {

    private static final Logger log = Logger.getLogger(UnregisterConnectionsFromSplitterAction.class);

    static final MgcpEndpointAction INSTANCE = new UnregisterConnectionsFromSplitterAction();

    @Override
    public void execute(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event, MgcpEndpointTransitionContext context, MgcpEndpointFsm stateMachine) {
        MgcpSplitterEndpointContext globalContext = (MgcpSplitterEndpointContext) stateMachine.getContext();
        EndpointIdentifier endpointId = globalContext.getEndpointId();
        MgcpConnection[] connections = context.get(MgcpEndpointParameter.UNREGISTERED_CONNECTIONS, MgcpConnection[].class);

        AudioSplitter splitter = globalContext.getSplitter();
        OOBSplitter oobSplitter = globalContext.getOobSplitter();
        for (MgcpConnection connection : connections) {
            unregisterConnection(endpointId, connection, splitter, oobSplitter);
        }
    }
    
    private void unregisterConnection(EndpointIdentifier endpointId, MgcpConnection connection, AudioSplitter splitter, OOBSplitter oobSplitter) {
        AudioComponent component = connection.getAudioComponent();
        OOBComponent oobComponent = connection.getOutOfBandComponent();

        if (connection.isLocal()) {
            splitter.releaseInsideComponent(component);
            if (log.isTraceEnabled()) {
                String connectionIdHex = connection.getHexIdentifier();
                int componentId = component.getComponentId();
                log.trace("Endpoint " + endpointId + " unregistered local connection " + connectionIdHex + " inside component " + componentId + " from the in-band splitter.");
            }
            
            oobSplitter.releaseInsideComponent(oobComponent);
            if (log.isTraceEnabled()) {
                String connectionIdHex = connection.getHexIdentifier();
                int componentId = oobComponent.getComponentId();
                log.trace("Endpoint " + endpointId + " unregistered local connection " + connectionIdHex + " inside component " + componentId + " from the out-of-band splitter.");
            }
        } else {
            splitter.releaseOutsideComponent(component);
            if (log.isTraceEnabled()) {
                String connectionIdHex = connection.getHexIdentifier();
                int componentId = component.getComponentId();
                log.trace("Endpoint " + endpointId + " unregistered remote connection " + connectionIdHex + " outside component " + componentId + " from the out-of-band splitter.");
            }
            
            oobSplitter.releaseOutsideComponent(oobComponent);
            if (log.isTraceEnabled()) {
                String connectionIdHex = connection.getHexIdentifier();
                int componentId = oobComponent.getComponentId();
                log.trace("Endpoint " + endpointId + " unregistered remote connection " + connectionIdHex + " outside component " + componentId + " from the out-of-band  splitter.");
            }
        }
    }

}
