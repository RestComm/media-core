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
