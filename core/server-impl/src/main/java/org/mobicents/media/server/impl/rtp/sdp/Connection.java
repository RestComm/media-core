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
package org.mobicents.media.server.impl.rtp.sdp;

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
