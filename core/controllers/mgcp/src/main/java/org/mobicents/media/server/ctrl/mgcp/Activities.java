/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.mobicents.media.server.ctrl.mgcp;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import javolution.util.FastMap;

/**
 *
 * @author kulikov
 */
public class Activities {
    private MgcpController controller;
    
    protected FastMap<String, Call> calls = new FastMap();    
    protected FastMap<String, EndpointActivity> endpoints = new FastMap();
    
    public Activities(MgcpController controller) {
        this.controller = controller;
    }
    
    public Call createCall(String callID) {
        return new Call(callID, this);
    }
    
    public Call getCall(String callID) throws UnknownActivityException {
        if (calls.containsKey(callID)) {
            return calls.get(callID);
        }
        throw new UnknownActivityException("call= " + callID);
    }
    
    public EndpointActivity getEndpointActivity(EndpointIdentifier endpointID) {
        if (endpoints.containsKey(endpointID.getLocalEndpointName())) {
            return endpoints.get(endpointID.getLocalEndpointName());
        }        
        return new EndpointActivity(endpointID, this);
    }

    public ConnectionActivity createConnectionActivity(String callID, EndpointIdentifier endpointID) throws UnknownActivityException {
        if (!calls.containsKey(callID)) {
            throw new UnknownActivityException("Call: " + callID);
        }
        
        getEndpointActivity(endpointID);
        
        return new ConnectionActivity(this, callID, endpointID);        
    }
}
