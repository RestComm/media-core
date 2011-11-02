package org.mobicents.media.server.impl.rtcp;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpBye extends RtcpCommonHeader {

	private long[] ssrcs = new long[31];

	protected RtcpBye() {

	}

	public RtcpBye(boolean padding) {
		super(padding, RtcpCommonHeader.RTCP_BYE);
	}

	protected int decode(byte[] rawData, int offSet) {

		int tmp = offSet;
		offSet = super.decode(rawData, offSet);

		for (int i = 0; i < this.count; i++) {
			this.ssrcs[i] |= rawData[offSet++] & 0xFF;
			this.ssrcs[i] <<= 8;
			this.ssrcs[i] |= rawData[offSet++] & 0xFF;
			this.ssrcs[i] <<= 8;
			this.ssrcs[i] |= rawData[offSet++] & 0xFF;
			this.ssrcs[i] <<= 8;
			this.ssrcs[i] |= rawData[offSet++] & 0xFF;
		}

		// Do we acre for optional part?

		return offSet;
	}

	protected int encode(byte[] rawData, int offSet) {

		int startPosition = offSet;

		offSet = super.encode(rawData, offSet);

		for (int i = 0; i < this.count; i++) {
			long ssrc = ssrcs[i];

			rawData[offSet++] = ((byte) ((ssrc & 0xFF000000) >> 24));
			rawData[offSet++] = ((byte) ((ssrc & 0x00FF0000) >> 16));
			rawData[offSet++] = ((byte) ((ssrc & 0x0000FF00) >> 8));
			rawData[offSet++] = ((byte) ((ssrc & 0x000000FF)));
		}
		
		/* Reduce 4 octest of header and length is in terms 32bits word */
		this.length = (offSet - startPosition - 4) / 4;

		rawData[startPosition + 2] = ((byte) ((this.length & 0xFF00) >> 8));
		rawData[startPosition + 3] = ((byte) (this.length & 0x00FF));
		
		return offSet;
	}

	public void addSsrc(long ssrc) {
		this.ssrcs[this.count++] = ssrc;
	}

	public long[] getSsrcs() {
		return ssrcs;
	}

}
