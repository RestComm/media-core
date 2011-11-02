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
public class RtcpSdesChunk {

	private long ssrc;

	private RtcpSdesItem[] rtcpSdesItems = new RtcpSdesItem[9];

	private int itemCount = 0;

	public RtcpSdesChunk(long ssrc) {
		this.ssrc = ssrc;
	}

	protected RtcpSdesChunk() {

	}

	protected int decode(byte[] rawData, int offSet) {

		this.ssrc |= rawData[offSet++] & 0xFF;
		this.ssrc <<= 8;
		this.ssrc |= rawData[offSet++] & 0xFF;
		this.ssrc <<= 8;
		this.ssrc |= rawData[offSet++] & 0xFF;
		this.ssrc <<= 8;
		this.ssrc |= rawData[offSet++] & 0xFF;

		while (true) {
			RtcpSdesItem sdesItem = new RtcpSdesItem();
			offSet = sdesItem.decode(rawData, offSet);
			rtcpSdesItems[itemCount++] = sdesItem;

			if (RtcpSdesItem.RTCP_SDES_END == sdesItem.getType()) {
				break;
			}
		}

		return offSet;
	}

	public void addRtcpSdesItem(RtcpSdesItem rtcpSdesItem) {
		this.rtcpSdesItems[itemCount++] = rtcpSdesItem;
	}

	public long getSsrc() {
		return ssrc;
	}

	public RtcpSdesItem[] getRtcpSdesItems() {
		return rtcpSdesItems;
	}

	public int getItemCount() {
		return itemCount;
	}

	protected int encode(byte[] rawData, int offSet) {

		int temp = offSet;

		rawData[offSet++] = ((byte) ((this.ssrc & 0xFF000000) >> 24));
		rawData[offSet++] = ((byte) ((this.ssrc & 0x00FF0000) >> 16));
		rawData[offSet++] = ((byte) ((this.ssrc & 0x0000FF00) >> 8));
		rawData[offSet++] = ((byte) ((this.ssrc & 0x000000FF)));

		for (RtcpSdesItem rtcpSdesItem : rtcpSdesItems) {
			if (rtcpSdesItem != null) {
				offSet = rtcpSdesItem.encode(rawData, offSet);
			} else {
				break;
			}
		}

		// This is End
		rawData[offSet++] = 0x00;

		int remainder = (offSet - temp) % 4;
		if (remainder != 0) {
			int pad = 4 - remainder;
			for (int i = 0; i < pad; i++) {
				rawData[offSet++] = 0x00;
			}
		}

		return offSet;
	}
}
