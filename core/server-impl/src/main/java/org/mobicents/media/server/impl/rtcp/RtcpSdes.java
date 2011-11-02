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
