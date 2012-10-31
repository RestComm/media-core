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

/**
 * Start time:13:36:13 2008-11-22<br>
 * Project: mobicents-media-server-controllers<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 *         </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
package org.mobicents.protocols.mgcp.utils;

import java.net.InetAddress;


/**
 * Start time:13:36:13 2008-11-22<br>
 * Project: mobicents-media-server-controllers<br>
 * Simple packet representation, has byte[] raw data and remote port/address
 * fields
 * 
 * @author <a href="mailto:baranowb@gmail.com">baranowb</a>
 */
public class PacketRepresentation {	

	private byte[] rawData = null;
	private int remotePort = -1;
	private InetAddress remoteAddress = null;
	private int length = 0;

	private PacketRepresentationFactory prFactory = null;

	protected PacketRepresentation(int byteArrSize, PacketRepresentationFactory prFactory) {
		rawData = new byte[byteArrSize];
		this.prFactory = prFactory;
	}

	public byte[] getRawData() {
		return rawData;
	}

	public int getLength() {
		return this.length;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public InetAddress getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(InetAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public void release() {
		this.prFactory.deallocate(this);
	}

}
