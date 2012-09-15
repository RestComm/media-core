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
package org.mobicents.javax.media.mscontrol.container;

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
