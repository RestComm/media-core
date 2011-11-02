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

import java.util.ArrayList;
import java.util.Collection;
import org.jboss.util.id.UID;
import org.mobicents.media.Component;
import org.mobicents.media.Format;
import org.mobicents.media.server.ConnectionImpl;
import org.mobicents.media.server.Utils;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.NotificationListener;
import org.mobicents.media.server.spi.events.NotifyEvent;

/**
 *
 * @author kulikov
 */
public abstract class BaseComponent implements Component {

    //big num, just in case, we can have a lot of them, dtmfs...
    private final static int LISTENER_TAB_SIZE = 70;
    
    //unique identifier of the component
    private String id = null;
    
    //the name of the component. 
    //name of the component might be same accros many components of same type
    private String name = null;
    
    //Endpoint to which this component belongs
    private Endpoint endpoint;
    //Connection to which component belongs
    private Connection connection;
    
    //Format used assigned for media conversation. 
    //The value of this field is negotiated during connecting procedure between any two components
    protected Format format;
    
    //The list for registered event listeners
    private final NotificationListener[] listeners = new NotificationListener[LISTENER_TAB_SIZE];

    /**
     * Creates new instance of the component.
     * 
     * @param name the name of component.
     */
    public BaseComponent(String name) {
        this.name = name;
        //generate identifier
        this.id = (new UID()).toString();
    }

    public Format getFormat() {
        return format;
    }

    public String getId() {
        return this.id;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#getName(). 
     */
    public String getName() {
        return name;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#getEndpoint(). 
     */
    public Endpoint getEndpoint() {
        return this.endpoint;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#setEndpoint(Endpoint). 
     */
    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#getConnection(). 
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#setConnection(). 
     */
    public void setConnection(Connection connection) {
        //diable all listeners if this component was associated with connection
        //and it is disconnected.
        if (this.connection != null && connection == null) {
            for (int index = 0; index < listeners.length; index++) {
                listeners[index] = null;
            }
        }
        this.connection = connection;
    }

    /**
     * Delivers specified event to registered listeners
     * 
     * @param evt the event to deliver
     */
    protected void sendEvent(NotifyEvent evt) {
        for (NotificationListener listener : listeners) {
            if (listener != null) {
                try {
                    listener.update(evt);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#addListener(NotificationListener). 
     */
    public void addListener(NotificationListener listener) {
        Utils.addObject(listeners, listener);
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#removeListener(NotificationListener). 
     */
    public void removeListener(NotificationListener listener) {
        Utils.removeObject(listeners, listener);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append(getName());
        buffer.append(" (endpoint=");

        if (getEndpoint() != null) {
            buffer.append(getEndpoint().getLocalName());
        } else {
            buffer.append("unknown");
        }

        if (getConnection() != null) {
            buffer.append(", connection=");
            buffer.append(((ConnectionImpl) getConnection()).getIndex());
        }
        buffer.append(")");

        return buffer.toString();
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.Component#reserStats();
     */
    public void resetStats() {
    }

    protected Collection<Format> subset(Format[] set1, Format[] set2) {
        ArrayList<Format> subset = new ArrayList();
        for (int i = 0; i < set1.length; i++) {
            for (int j = 0; j < set2.length; j++) {
                if (set1[i].matches(set2[j])) {
                    String e1 = set1[i].getEncoding();
                    String e2 = set2[j].getEncoding();

                    if (!e1.equalsIgnoreCase("ANY")) {
                        subset.add(set1[i]);
                        continue;
                    }

                    if (!e2.equalsIgnoreCase("ANY")) {
                        subset.add(set2[j]);
                        continue;
                    }

                    if (e1.equalsIgnoreCase("ANY") && e2.equalsIgnoreCase("ANY") && !subset.contains(Format.ANY)) {
                        subset.add(set2[j]);
                        continue;
                    }
                }
            }
        }
        return subset;
    }

    /* (non-Javadoc)
     * @see org.mobicents.media.Component#getInterface(java.lang.Class)
     */
    public <T> T getInterface(Class<T> interfaceType) {
        // TODO Auto-generated method stub
        return null;
    }
}
