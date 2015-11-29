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

package org.mobicents.media.server.mgcp.naming;

import org.mobicents.media.server.spi.EndpointType;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EndpointNamingWizard {
    
    public static final String WILDCARD_ANY = "$";
    public static final String WILDCARD_ALL = "*";
    
    public static final String BRIDGE = "bridge";
    public static final String IVR = "ivr";
    public static final String CONFERENCE = "cnf";
    
    public static EndpointType getEndpointType(String endpointName) {
        if(endpointName.contains(BRIDGE)) {
            return EndpointType.BRIDGE;
        }
        
        if(endpointName.contains(IVR)) {
            return EndpointType.IVR;
        }
        
        if(endpointName.contains(CONFERENCE)) {
            return EndpointType.CONFERENCE;
        }
        
        return null;
    }

}
