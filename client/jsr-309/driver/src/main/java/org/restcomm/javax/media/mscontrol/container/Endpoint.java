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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.javax.media.mscontrol.container;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Represents server side endpoint
 * @author kulikov
 */
public class Endpoint {
    //local name of server endpoint    
    private EndpointIdentifier endpointID;
    private Semaphore semaphore = new Semaphore(0);
    private volatile boolean waitngConcreteName;
    
    public Endpoint(EndpointIdentifier endpointID) {
        this.endpointID = endpointID;
    }
    
    public boolean hasConcreteName() {
        return endpointID.getLocalEndpointName().indexOf("$") < 0;
    }
    
    public void expectingConcreteName() {
        this.waitngConcreteName = true;
    }
    
    public boolean concreteNameExpectedSoon() {
        return this.waitngConcreteName;
    }
    
    public void await() throws InterruptedException {
        semaphore.tryAcquire(10, TimeUnit.SECONDS);
    }
    
    public void setConcreteName(EndpointIdentifier endpointID) {
        this.endpointID = endpointID;
        this.waitngConcreteName = false;
        semaphore.release();
    }
    
    public EndpointIdentifier getIdentifier() {
        return this.endpointID;
    }
}
