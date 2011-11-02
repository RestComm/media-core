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
package org.mobicents.media.server.impl;

import org.mobicents.media.Component;
import org.mobicents.media.server.spi.events.NotifyEvent;

/**
 * Implementation for standard events.
 * 
 * @author kulikov
 */
public class NotifyEventImpl implements NotifyEvent {
    
    private BaseComponent component;
    private int eventID;
    private String desc;
    
    public NotifyEventImpl(BaseComponent component, int eventID) {
        this.component = component;
        this.eventID = eventID;
    }
    
    public NotifyEventImpl(BaseComponent component, int eventID, String desc) {
		super();
		this.component = component;
		this.eventID = eventID;
		this.desc = desc;
	}

	public int getEventID() {
        return eventID;
    }

    public Component getSource() {
        return component;
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NotifyEventImpl [component=" + component + ", desc=" + desc
				+ ", eventID=" + eventID + "]";
	}

}
