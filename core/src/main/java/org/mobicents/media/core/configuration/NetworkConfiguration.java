/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.core.configuration;

/**
 * Network configuration of the Media Server.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NetworkConfiguration {
    
    public static final String BIND_ADDRESS = "127.0.0.1";
    public static final String EXTERNAL_ADDRESS = "";
    public static final String NETWORK = "127.0.0.1";
    public static final String SUBNET = "255.255.255.255";
    public static final boolean SBC = false;

    private String bindAddress;
    private String externalAddress;
    private String network;
    private String subnet;
    private boolean sbc;

    public NetworkConfiguration() {
        this.bindAddress = BIND_ADDRESS;
        this.externalAddress = EXTERNAL_ADDRESS;
        this.network = NETWORK;
        this.subnet = SUBNET;
        this.sbc = SBC;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        if (bindAddress == null || bindAddress.isEmpty()) {
            throw new IllegalArgumentException("BindAddress cannot be empty.");
        }
        this.bindAddress = bindAddress;
    }

    public String getExternalAddress() {
        return externalAddress;
    }

    public void setExternalAddress(String externalAddress) {
        if (externalAddress == null || externalAddress.isEmpty()) {
            throw new IllegalArgumentException("ExternalAddress cannot be empty.");
        }
        this.externalAddress = externalAddress;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        if (network == null || network.isEmpty()) {
            throw new IllegalArgumentException("Network cannot be empty.");
        }
        this.network = network;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        if (subnet == null || subnet.isEmpty()) {
            throw new IllegalArgumentException("Subnet cannot be empty.");
        }
        this.subnet = subnet;
    }

    public boolean isSbc() {
        return sbc;
    }

    public void setSbc(boolean sbc) {
        this.sbc = sbc;
    }

}
