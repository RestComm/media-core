/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.restcomm.media.control.mgcp.controller;

import org.mobicents.media.server.utils.Text;

/**
 * Notified entity MGCP parameter.
 * 
 * @author kulikov
 */
public class NotifiedEntity {
    //fully qualified name
    private Text fqn = new Text(new byte[100], 0, 100);
    
    //the name partitions
    private Text localName = new Text();        
    private Text domainName = new Text();
    
    //domain name partitions
    private Text hostName = new Text();
    private Text portNumber = new Text();
    
    private Text[] notifiedEntity = new Text[]{localName, domainName};
    private Text[] domain = new Text[]{hostName, portNumber};
    
    private int port;
    
    /**
     * Modifies value of this object.
     * 
     * @param text the new value;
     */
    public void setValue(Text text) {        
        text.duplicate(fqn);
        fqn.divide('@', notifiedEntity);
        domainName.divide(':', domain);
        port = portNumber.toInteger();
    }
    
    /**
     * Gets the value of this object.
     * 
     * @return the fully qualified name of this entity.
     */
    public Text getValue() {
        return fqn;
    }
    
    /**
     * Gets the local name partition of the name.
     * 
     * @return Text object wrapping local name part.
     */
    public Text getLocalName() {
        return localName;
    }
    
    /**
     * Gets the domain name partition of the name.
     * 
     * @return Text object wrapping domain name part.
     */
    public Text getHostName() {
        return hostName;
    }
    
    /**
     * Gets the port number partition of the name.
     * 
     * @return port number.
     */
    public int getPort() {
        return port;
    }
}
