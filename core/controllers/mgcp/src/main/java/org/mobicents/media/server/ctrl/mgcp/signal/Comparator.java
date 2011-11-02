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
package org.mobicents.media.server.ctrl.mgcp.signal;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;

/**
 * Compares event/signal objects.
 * 
 * @author kulikov
 */
public class Comparator {
    /**
     * Gets the fully qualified name of the given event.
     * 
     * @param event the event
     * @return name as string.
     */
    private static String fqn(EventName event) {
        return String.format("%s/%s", event.getPackageName().toString(), event.getEventIdentifier().getName());
    }
    
    /**
     * Compares two events.
     * 
     * @param e1 first event
     * @param e2 second event
     * @return true if both has same fully qualified name.
     */
    public static boolean equals(EventName e1, EventName e2) {
        if (e1 == null || e2 == null) {
            return false;
        }
        
        if (e1 == e2) {
            return true;
        }
        
        return fqn(e1).equals(fqn(e2));
    }
    
    /**
     * Compares requested event with event name.
     * 
     * @param e1 requested event
     * @param e2 event name
     * @return true if requested even't name equals to given event name.
     */
    public static boolean matches(RequestedEvent e1, EventName e2) {
        if (e1 == null) {
            return false;
        }
        
        return equals(e1.getEventName(), e2);
    }
    
    /**
     * Compares given name with name of given event.
     * 
     * @param name the name
     * @param event the event
     * @return true if given name equals to even't fully qualified name.
     */
    public static boolean equals(String name, EventName event) {
        return name.equals(fqn(event));
    }
}
