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

package org.mobicents.media.server.ctrl.mgcp.pkg.innertest;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import org.apache.log4j.Logger;
import org.mobicents.media.server.ctrl.mgcp.signal.Signal;

/**
 * Test signal used for inner testing.
 * 
 * @author kulikov
 */
public class TestSignal extends Signal implements Runnable {
    
    private EventName event;
    private volatile boolean started = false;
    
    private Logger logger = Logger.getLogger(TestSignal.class);
    
    public TestSignal(String name) {
        super(name);
    }
    
    @Override
    public void execute() {
    }

    @Override
    public boolean doAccept(RequestedEvent event) {
        boolean accepted = event.toString().equals("T/test1");
        logger.info("Request for accept " + event + ", result=" + accepted);
        
        if (accepted) {
            this.event = event.getEventName();
            //this event triggers execution
            this.started = true;
            new Thread(this).start();
        }
        
        return accepted;
    }

    @Override
    public void cancel() {
        started = false;
    }

    @SuppressWarnings("static-access")
    public void run() {
        int count = 3;
        while (started && count > 0) {
            logger.info("Sending " + event);
            this.sendEvent(event);            
            try {
                Thread.currentThread().sleep(1000);
            } catch (Exception e) {
            }
            count--;
        }
    }

}
