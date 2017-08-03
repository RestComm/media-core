/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.mscontrol.sdp;

/**
 *
 * @author kulikov
 */
public class Origin {
    private String name;
    private String sessionID;
    private String sessionVersion;
    private String networkType;
    private String addressType;
    private String address;

    public Origin(String o) {
        int pos1 = o.indexOf(61);
        int pos2 = o.indexOf(32, pos1);
        name = o.substring(pos1 + 1, pos2);
        
        pos1 = o.indexOf(32, pos2 + 1);
        sessionID = o.substring(pos2 + 1, pos1);
        
        pos2 = o.indexOf(32, pos1 + 1);
        sessionVersion = o.substring(pos1 + 1, pos2);
        
        pos1 = o.indexOf(32, pos2 + 1);
        networkType = o.substring(pos2 + 1, pos1);
        
        pos2 = o.indexOf(32, pos1 + 1);
        addressType = o.substring(pos1 + 1, pos2);
        
        address = o.substring(pos2 + 1, o.length());
    }
    
    public Origin(String name, String sessionID, String sessionVersion, String networkType, String addressType, String address) {
        this.name = name;
        this.sessionID = sessionID;
        this.sessionVersion = sessionVersion;
        this.networkType = networkType;
        this.addressType = addressType;
        this.address = address;
    }

    
    public String getAddress() {
        return address;
    }

    public String getAddressType() {
        return addressType;
    }

    public String getName() {
        return name;
    }

    public String getNetworkType() {
        return networkType;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getSessionVersion() {
        return sessionVersion;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public void setSessionVersion(String sessionVersion) {
        this.sessionVersion = sessionVersion;
    }
    
    @Override
    public String toString() {
        return "o=" + name + " " + sessionID + " " + sessionVersion + " " +
                networkType + " " + addressType + " " + address;
    }
}
