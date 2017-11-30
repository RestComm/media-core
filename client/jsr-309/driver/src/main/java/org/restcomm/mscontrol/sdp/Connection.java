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
public class Connection {
    private String networkType;
    private String addressType;
    private String address;

    public Connection(String s) {
        int pos1 = s.indexOf('=');
        int pos2 = s.indexOf(' ', pos1);
        networkType = s.substring(pos1 + 1, pos2);
        
        pos1 = s.indexOf(' ', pos2 + 1);
        addressType = s.substring(pos2 + 1, pos1);
                
        address = s.substring(pos1 + 1, s.length());
    }
    
    public Connection(String networkType, String addressType, String address) {
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

    public String getNetworkType() {
        return networkType;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    @Override
    public String toString() {
        return "c=" + networkType + " " + addressType + " " + address;
    }
}
