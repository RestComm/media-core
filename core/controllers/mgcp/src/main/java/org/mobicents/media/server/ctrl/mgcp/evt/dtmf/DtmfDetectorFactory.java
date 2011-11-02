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
package org.mobicents.media.server.ctrl.mgcp.evt.dtmf;

import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import org.mobicents.media.server.ctrl.mgcp.evt.DetectorFactory;
import org.mobicents.media.server.ctrl.mgcp.evt.EventDetector;
import org.mobicents.media.server.ctrl.mgcp.evt.MgcpPackage;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.events.NotifyEvent;
import org.mobicents.media.server.spi.resource.DtmfDetector;

/**
 *
 * @author kulikov
 */
public class DtmfDetectorFactory implements DetectorFactory {

    private String name;
    private MgcpPackage mgcpPackage;

    private int eventID;
    
    public String getEventName() {
        return this.name;
    }

    public MgcpPackage getPackage() {
        return this.mgcpPackage;
    }

    public void setEventName(String eventName) {
        this.name = eventName;
    }

    public void setPackage(MgcpPackage mgcpPackage) {
        this.mgcpPackage = mgcpPackage;
    }

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }
    
    public EventDetector getInstance(String params, RequestedAction[] actions,
            Class interface1, MediaType type) {
        return new MgcpDtmfEvent(this.mgcpPackage, name, eventID, params, actions, DtmfDetector.class);
    }

    private class MgcpDtmfEvent extends EventDetector {

        public MgcpDtmfEvent(MgcpPackage mgcpPackage, String eventName, int eventID, String params,
            RequestedAction[] actions, Class interface1) {
            super(mgcpPackage, eventName, eventID, params, actions);
            this.setDetectorInterface(interface1);
            this.setMediaType(MediaType.AUDIO);
        }
        
        @Override
        public void start() {
            super.start();
            ((DtmfDetector) component).start();
        }
        
        @Override
        public void performAction(NotifyEvent event, RequestedAction action) {
            System.out.println("Notify " + this.getEventName().getEventIdentifier());
            getRequest().sendNotify(this.getEventName());
            //((DtmfDetector) component).stop();
        }
    }
}
