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

package org.mobicents.media.server.impl.rtcp;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpSdes extends RtcpCommonHeader {

	/**
	 * SDES
	 */
	private RtcpSdesChunk[] sdesChunks = new RtcpSdesChunk[31];

	protected RtcpSdes() {

	}

	public RtcpSdes(boolean padding) {
		super(padding, RtcpCommonHeader.RTCP_SDES);
	}

	protected int decode(byte[] rawData, int offSet) {
		int tmp = offSet;
		offSet = super.decode(rawData, offSet);

		int tmpCount = 0;
		while ((offSet - tmp) < this.length) {
			RtcpSdesChunk rtcpSdesChunk = new RtcpSdesChunk();
			offSet = rtcpSdesChunk.decode(rawData, offSet);

			sdesChunks[tmpCount++] = rtcpSdesChunk;
		}
		return offSet;
	}

	protected int encode(byte[] rawData, int offSet) {
		int startPosition = offSet;

		offSet = super.encode(rawData, offSet);
		for (RtcpSdesChunk rtcpSdesChunk : sdesChunks) {
			if (rtcpSdesChunk != null) {
				offSet = rtcpSdesChunk.encode(rawData, offSet);
			} else {
				break;
			}
		}

		/* Reduce 4 octest of header and length is in terms 32bits word */
		this.length = (offSet - startPosition - 4) / 4;

		rawData[startPosition + 2] = ((byte) ((this.length & 0xFF00) >> 8));
		rawData[startPosition + 3] = ((byte) (this.length & 0x00FF));

		return offSet;
	}

	public void addRtcpSdesChunk(RtcpSdesChunk rtcpSdesChunk) {
		this.sdesChunks[this.count++] = rtcpSdesChunk;
	}

	public RtcpSdesChunk[] getSdesChunks() {
		return sdesChunks;
	}

}
