package org.mobicents.media.server.impl.rtcp;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpReceptionReport extends RtcpCommonHeader {

	/* receiver generating this report */
	private long ssrc;

	private RtcpReceptionReportItem[] rtcpReceptionReports = new RtcpReceptionReportItem[31]; /* variable-length list */

	protected RtcpReceptionReport() {
	}

	public RtcpReceptionReport(boolean padding, long ssrc) {
		super(padding, RtcpCommonHeader.RTCP_RR);
		this.ssrc = ssrc;
	}

	protected int decode(byte[] rawData, int offSet) {

		int tmp = offSet;

		offSet = super.decode(rawData, offSet);

		this.ssrc |= rawData[offSet++] & 0xFF;
		this.ssrc <<= 8;
		this.ssrc |= rawData[offSet++] & 0xFF;
		this.ssrc <<= 8;
		this.ssrc |= rawData[offSet++] & 0xFF;
		this.ssrc <<= 8;
		this.ssrc |= rawData[offSet++] & 0xFF;

		int tmpCount = 0;
		while ((offSet - tmp) < this.length) {
			RtcpReceptionReportItem rtcpReceptionReportItem = new RtcpReceptionReportItem();
			offSet = rtcpReceptionReportItem.decode(rawData, offSet);

			rtcpReceptionReports[tmpCount++] = rtcpReceptionReportItem;
		}

		return offSet;
	}

	protected int encode(byte[] rawData, int offSet) {
		int startPosition = offSet;

		offSet = super.encode(rawData, offSet);

		rawData[offSet++] = ((byte) ((this.ssrc & 0xFF000000) >> 24));
		rawData[offSet++] = ((byte) ((this.ssrc & 0x00FF0000) >> 16));
		rawData[offSet++] = ((byte) ((this.ssrc & 0x0000FF00) >> 8));
		rawData[offSet++] = ((byte) ((this.ssrc & 0x000000FF)));

		for (RtcpReceptionReportItem rtcpReceptionReportItem : rtcpReceptionReports) {
			if (rtcpReceptionReportItem != null) {
				offSet = rtcpReceptionReportItem.encode(rawData, offSet);
			} else {
				break;
			}
		}

		/* Reduce 4 octets of header and length is in terms 32bits word */
		this.length = (offSet - startPosition - 4) / 4;

		rawData[startPosition + 2] = ((byte) ((this.length & 0xFF00) >> 8));
		rawData[startPosition + 3] = ((byte) (this.length & 0x00FF));

		return offSet;
	}

	public long getSsrc() {
		return ssrc;
	}

	public RtcpReceptionReportItem[] getRtcpReceptionReports() {
		return rtcpReceptionReports;
	}

	public void addRtcpReceptionReportItem(RtcpReceptionReportItem rtcpReceptionReportItem) {
		this.rtcpReceptionReports[this.count++] = rtcpReceptionReportItem;
	}
}
