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

package org.restcomm.media.control.mgcp.endpoint.mixer;

import org.apache.log4j.Logger;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointEvent;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointFsm;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointParameter;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointState;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointTransitionContext;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * Registers a connection in the in-band and out-of-band mixers of the endpoint.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>REGISTERED_CONNECTION</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>n/a</li>
 * </ul>
 * </p>
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RegisterConnectionInMixerAction
        extends AnonymousAction<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext> {
    
    private static final Logger log = Logger.getLogger(RegisterConnectionInMixerAction.class);

    @Override
    public void execute(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event, MgcpEndpointTransitionContext context, MgcpEndpointFsm stateMachine) {
        MgcpConnection connection = context.get(MgcpEndpointParameter.REGISTERED_CONNECTION, MgcpConnection.class);
        MgcpMixerEndpointContext globalContext = (MgcpMixerEndpointContext) stateMachine.getContext();

        // Register connection to in-band mixer
        AudioComponent component = connection.getAudioComponent();
        AudioMixer mixer = globalContext.getMixer();
        mixer.addComponent(component);
        
        if(log.isTraceEnabled()) {
            EndpointIdentifier endpointId = globalContext.getEndpointId();
            String connectionIdHex = connection.getHexIdentifier();
            int componentId = component.getComponentId();
            log.trace("Endpoint " + endpointId + " registered connection " + connectionIdHex + " component " + componentId + " in the in-band mixer.");
        }

        // Register connection to out-of-band mixer
        OOBComponent oobComponent = connection.getOutOfBandComponent();
        OOBMixer oobMixer = globalContext.getOobMixer();
        oobMixer.addComponent(oobComponent);
        
        if(log.isTraceEnabled()) {
            EndpointIdentifier endpointId = globalContext.getEndpointId();
            String connectionIdHex = connection.getHexIdentifier();
            int componentId = oobComponent.getComponentId();
            log.trace("Endpoint " + endpointId + " registered connection " + connectionIdHex + " component " + componentId + " in the out-of-band mixer.");
        }
    }

}
