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

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

/**
 * Used as Util to convert some MGCP values to Media Server SPI.
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class MgcpUtils {

	public org.mobicents.media.server.spi.ConnectionMode getMode(ConnectionMode mode) {
		switch (mode.getConnectionModeValue()) {
		case ConnectionMode.RECVONLY:
			return org.mobicents.media.server.spi.ConnectionMode.RECV_ONLY;
		case ConnectionMode.SENDONLY:
			return org.mobicents.media.server.spi.ConnectionMode.SEND_ONLY;
		case ConnectionMode.SENDRECV:
			return org.mobicents.media.server.spi.ConnectionMode.SEND_RECV;
		case ConnectionMode.INACTIVE:
			return org.mobicents.media.server.spi.ConnectionMode.INACTIVE;
		default:
			return null;
		}
	}

	public ConnectionMode getMode(org.mobicents.media.server.spi.ConnectionMode mode) {
		switch (mode) {
		case INACTIVE:
			return ConnectionMode.Inactive;
		case SEND_ONLY:
			return ConnectionMode.SendOnly;
		case RECV_ONLY:
			return ConnectionMode.RecvOnly;
		case SEND_RECV:
			return ConnectionMode.SendRecv;
		default:
			return null;
		}
	}
}
