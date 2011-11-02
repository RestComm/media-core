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
public class TestSignal2 extends Signal {
    
    private EventName event;
    
    private Logger logger = Logger.getLogger(TestSignal2.class);
    
    public TestSignal2(String name) {
        super(name);
    }
    
    @Override
    public void execute() {
        logger.info("Execute: sending event " + event);
        sendEvent(event);
        this.complete();
    }

    @Override
    public boolean doAccept(RequestedEvent event) {
        this.event = event.getEventName();
        return true;
    }

    @Override
    public void cancel() {
    }

}
