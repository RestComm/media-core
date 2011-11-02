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
package org.mobicents.media.server.spi;
/**
 *
 * @author amit bhayani
 */
public enum ConnectionState {

    NULL(0, "NULL"), IDLE(1, "IDLE"), HALF_OPEN(2, "HALF_OPEN"), OPEN(3, "OPEN"), CLOSED(4, "CLOSED");

    private int code;
    private String stateName;
    
    private ConnectionState(int code, String stateName) {
        this.stateName = stateName;
        this.code = code;
    }
    
    public static ConnectionState getInstance(String name) {
        if (name.equalsIgnoreCase("NULL")) {
            return NULL;
        } else if(name.equalsIgnoreCase("IDLE")){
            return IDLE;
        } else if(name.equalsIgnoreCase("HALF_OPEN")){
            return HALF_OPEN;
        } else if(name.equalsIgnoreCase("OPEN")){
            return OPEN;
        } else if(name.equalsIgnoreCase("CLOSED")){
            return CLOSED;
        } else{
        	throw new IllegalArgumentException("There is no media type for: "+name);
        }
    }    
    
    @Override
    public String toString() {
        return stateName;
    }
    
    public int getCode() {
        return code;
    }
}
