/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.component;

import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.component.oob.OOBInput;
import org.mobicents.media.server.component.oob.OOBOutput;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * Contains the media and out-of-band components of a connection.
 * 
 * @author Henrique Rosa (henrique.rosa@gmail.com)
 *
 */
public class CompoundComponent {

    private final int componentId;
    private final InbandComponent inbandComponent;
    private final OOBComponent ooBComponent;

    /**
     * Create a compound component with both inband and out-of-band components
     * 
     * @param componentId The identifier of the component. Must be unique in the session (like the SSRC, for example)
     * @param inbandComponent The inband component
     * @param ooBComponent The out-of-band component
     */
    public CompoundComponent(int componentId, InbandComponent inbandComponent, OOBComponent ooBComponent) {
        this.componentId = componentId;
        this.inbandComponent = inbandComponent;
        this.ooBComponent = ooBComponent;
    }

    public int getComponentId() {
        return componentId;
    }

    public InbandComponent getInbandComponent() {
        return inbandComponent;
    }

    public void addInbandInput(InbandInput input) {
        this.inbandComponent.addInput(input);
    }

    public void addInbandOutput(InbandOutput output) {
        this.inbandComponent.addOutput(output);
    }

    public OOBComponent getOOBComponent() {
        return ooBComponent;
    }

    public void addOOBInput(OOBInput input) {
        this.ooBComponent.addInput(input);
    }

    public void addOOBOutput(OOBOutput output) {
        this.ooBComponent.addOutput(output);
    }

    public void updateMode(ConnectionMode connectionMode) {
        switch (connectionMode) {
            case SEND_ONLY:
                inbandComponent.updateMode(false, true);
                ooBComponent.updateMode(false, true);
                break;
            case RECV_ONLY:
                inbandComponent.updateMode(true, false);
                ooBComponent.updateMode(true, false);
                break;
            case INACTIVE:
                inbandComponent.updateMode(false, false);
                ooBComponent.updateMode(false, false);
                break;
            case SEND_RECV:
            case CONFERENCE:
                inbandComponent.updateMode(true, true);
                ooBComponent.updateMode(true, true);
                break;
            case NETWORK_LOOPBACK:
                inbandComponent.updateMode(false, false);
                ooBComponent.updateMode(false, false);
                break;
            default:
                break;
        }
    }

}
