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

package org.restcomm.media.resource.player.video.mpeg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 
 * @author amit bhayani
 *
 */
public class RTPSample {
	private int packetCount = 0;
	private int reserved;
	RTPLocalPacket[] rtpLocalPackets = null;
	byte[] extradata = null;
	
	/**
	 * Sample Period in MicroSeconds
	 */
	private int samplePeriod = 0;

	private int index = 0;

	public int getSamplePeriod() {
		return samplePeriod;
	}

	public void setSamplePeriod(int samplePeriod) {
		this.samplePeriod = samplePeriod;
	}

	public int getPacketCount() {
		return packetCount;
	}

	public void setPacketCount(int packetCount) {
		this.packetCount = packetCount;
		rtpLocalPackets = new RTPLocalPacket[packetCount];
	}

	public int getReserved() {
		return reserved;
	}

	public void setReserved(int reserved) {
		this.reserved = reserved;
	}

	public RTPLocalPacket[] getRtpLocalPackets() {
		return rtpLocalPackets;
	}

	public void addRtpLocalPackets(RTPLocalPacket rtpLocalPacket) {
		this.rtpLocalPackets[index++] = rtpLocalPacket;
	}

	public byte[] getExtradata() {
		return extradata;
	}

	public void setExtradata(byte[] extradata) {
		this.extradata = extradata;
	}

	public byte[] toByteArray(long ssrc) throws IOException {
		if (this.packetCount == 0) {
			return null;
		} else if (this.packetCount == 1) {
			return this.rtpLocalPackets[0].toByteArray(ssrc);
		} else {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			for (RTPLocalPacket rtpLocalPacket : this.rtpLocalPackets) {
				bout.write(rtpLocalPacket.toByteArray(ssrc));
			}
			return bout.toByteArray();
		}
	}

}
