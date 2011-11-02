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
package org.mobicents.media.server.impl.rtp;

import net.java.stun4j.StunAddress;
import net.java.stun4j.client.NetworkConfigurationDiscoveryProcess;
import net.java.stun4j.client.StunDiscoveryReport;

/**
 *
 * @author kulikov
 */
public class TestStun {

    public static void main(String[] args) throws Exception {
        StunAddress localStunAddress = new StunAddress("192.168.1.2", 8000);
        StunAddress serverStunAddress = new StunAddress("stun.ekiga.net", 3478);

        NetworkConfigurationDiscoveryProcess addressDiscovery =
                new NetworkConfigurationDiscoveryProcess(
                localStunAddress, serverStunAddress);
        addressDiscovery.start();

        StunDiscoveryReport report = addressDiscovery.determineAddress();
        if (report.getPublicAddress() != null) {
            String publicAddressFromStun = report.getPublicAddress().getSocketAddress().getAddress().getHostAddress();
            System.out.println("Public address: " + publicAddressFromStun);
        // TODO set a timer to retry the binding and provide a
        // callback to update the global ip address and port
        } else {
            System.out.println("Stun discovery failed to find a valid public ip address, disabling stun !");
        }
        System.out.println("Stun report = " + report);
        addressDiscovery.shutDown();
    }
}
