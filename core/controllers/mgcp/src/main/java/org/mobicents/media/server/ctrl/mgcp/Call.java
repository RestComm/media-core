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

import java.io.Serializable;


import java.util.Collection;
import javolution.util.FastMap;

/**
 * Represents MGCP Call.
 * @author baranowb
 * @author kulikov
 */
public class Call implements Serializable {

    private String id;
    private MgcpController controller;    
    
    private Activities parent;
    protected FastMap<String, ConnectionActivity> connections = new FastMap<String, ConnectionActivity>();

    protected Call(String id, Activities parent) {
        this.id = id;
        this.parent = parent;
        parent.calls.put(id, this);
    }
    
    public String getID() {
        return id;
    }

    public ConnectionActivity getConnectionActivity(String ID) throws UnknownActivityException {
        if (!connections.containsKey(ID)) {
            throw new UnknownActivityException("Connection: " + ID);
        }
        return connections.get(ID);
    }

    protected MgcpController getController() {
        return this.controller;
    }
    
    public void terminate() {
        ConnectionActivity[] list = new ConnectionActivity[connections.size()];
        connections.values().toArray(list);
        
        for (ConnectionActivity a : list) {
            a.terminate();
        }
        parent.calls.remove(this.id);
    }
}
