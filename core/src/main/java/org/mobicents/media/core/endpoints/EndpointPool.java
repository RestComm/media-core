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

import org.mobicents.media.core.pooling.ConcurrentResourcePool;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointType;

/**
 * Concurrent pool implementation responsible for managing {@link Endpoint} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EndpointPool {

    private static final EndpointPool INSTANCE = new EndpointPool();

    private final ConcurrentResourcePool<Endpoint> conferenceEndpoints = new ConcurrentResourcePool<Endpoint>();
    private final ConcurrentResourcePool<Endpoint> bridgeEndpoints = new ConcurrentResourcePool<Endpoint>();
    private final ConcurrentResourcePool<Endpoint> ivrEndpoints = new ConcurrentResourcePool<Endpoint>();

    EndpointPool() {
        super();
    }

    public static EndpointPool getInstance() {
        return INSTANCE;
    }

    public Endpoint poll(EndpointType endpointType) {
        Endpoint endpoint;

        // Poll endpoint from correct pool
        switch (endpointType) {
            case CONFERENCE:
                endpoint = conferenceEndpoints.poll();
                break;

            case BRIDGE:
                endpoint = bridgeEndpoints.poll();
                break;

            case IVR:
                endpoint = ivrEndpoints.poll();
                break;
            default:
                throw new IllegalArgumentException("Unsupported endpoint type: " + endpointType.name());
        }

        // Produce new endpoint of requested type in case the pool was empty
        if (endpoint == null) {
            endpoint = EndpointFactory.createEndpoint(endpointType);
        }
        return endpoint;
    }
    
    public void offer(Endpoint endpoint) {
        EndpointType endpointType = endpoint.getType();
        
        switch (endpointType) {
            case CONFERENCE:
                this.conferenceEndpoints.offer(endpoint);
                break;
                
            case BRIDGE:
                this.bridgeEndpoints.offer(endpoint);
                break;
                
            case IVR:
                this.ivrEndpoints.offer(endpoint);
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported endpoint type: " + endpointType.name());
        }
    }
    
    public int countEndpoints(EndpointType endpointType) {
        switch (endpointType) {
            case CONFERENCE:
                return this.conferenceEndpoints.size();
                
            case BRIDGE:
                return this.bridgeEndpoints.size();
                
            case IVR:
                return this.ivrEndpoints.size();
                
            default:
                return 0;
        }
    }

}
