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

package org.mobicents.media.core.endpoints;

import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.core.endpoints.impl.BridgeEndpoint;
import org.mobicents.media.core.endpoints.impl.ConferenceEndpoint;
import org.mobicents.media.core.endpoints.impl.IvrEndpoint;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointType;

/**
 * Factory that produces {@link Endpoint} objects by type.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EndpointFactory {

    private static final AtomicInteger ID_GENERATOR_CONFERENCE = new AtomicInteger(0);
    private static final AtomicInteger ID_GENERATOR_BRIDGE = new AtomicInteger(0);
    private static final AtomicInteger ID_GENERATOR_IVR = new AtomicInteger(0);
    
    private EndpointFactory() {
        super();
    }

    public static Endpoint createEndpoint(EndpointType type) {
        switch (type) {
            case CONFERENCE:
                return createConferenceEndpoint();

            case BRIDGE:
                return createBridgeEndpoint();

            case IVR:
                return createIvrEndpoint();

            default:
                throw new IllegalArgumentException("Unsupported endpoint type " + type.name());
        }
    }

    private static Endpoint createConferenceEndpoint() {
        return new ConferenceEndpoint("mobicents/cnf/" + ID_GENERATOR_CONFERENCE.incrementAndGet());
    }

    private static Endpoint createBridgeEndpoint() {
        return new BridgeEndpoint("mobicents/bridge/" + ID_GENERATOR_BRIDGE.incrementAndGet());
    }

    private static Endpoint createIvrEndpoint() {
        return new IvrEndpoint("mobicents/ivr/" + ID_GENERATOR_IVR.incrementAndGet());
    }

}
