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

package org.restcomm.media.spi;

/**
 * Enumeration of possible modes for an MGCP connection.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum ConnectionMode {
    
    INACTIVE("inactive"), 
    SEND_ONLY("sendonly"),
    RECV_ONLY("recvonly"),
    SEND_RECV("sendrecv"),
    CONFERENCE("confrnce"),
    NETWORK_LOOPBACK("netwloop"),
    LOOPBACK("loopback"),
    CONTINUITY_TEST("conttest"),
    NETWORK_CONTINUITY_TEST("netwtest");
    
    private final String description;
    
    private ConnectionMode(String description) {
        this.description = description;
    }
    
    public String description() {
        return this.description;
    }
    
    public static final ConnectionMode fromDescription(String description) {
        if(description != null && !description.isEmpty()) {
            for (ConnectionMode mode : values()) {
                if(mode.description.equalsIgnoreCase(description)) {
                    return mode;
                }
            }
        }
        throw new IllegalArgumentException("Unknown connection mode: " + description);
    }
}
