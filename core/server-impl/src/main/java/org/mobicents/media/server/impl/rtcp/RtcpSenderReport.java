package org.mobicents.media.server.impl.rtcp;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpSenderReport extends RtcpCommonHeader {

	/**
	 * sender report (SR)
	 */

	/* sender generating this report */
	private long ssrc;

	/* NTP timestamp */
	private long ntpSec;

	private long ntpFrac;

	/* RTP timestamp */
	private long rtpTs;

	/* packets sent */
	private long psent;

	/* octets sent */
	private long osent;

	private RtcpReceptionReportItem[] rtcpReceptionReports = new RtcpReceptionReportItem[31]; /* variable-length list */

	protected RtcpSenderReport() {

	}

	public RtcpSenderReport(boolean padding, long ssrc, long ntpSec, long ntpFrac, long rtpTs, long psent,
			long osent) {
		super(padding, RtcpCommonHeader.RTCP_SR);
		this.ssrc = ssrc;
		this.ntpSec = ntpSec;
		this.ntpFrac = ntpFrac;
		this.rtpTs = rtpTs;
		this.psent = psent;
		this.osent = osent;
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

		this.ntpSec |= rawData[offSet++] & 0xFF;
		this.ntpSec <<= 8;
		this.ntpSec |= rawData[offSet++] & 0xFF;
		this.ntpSec <<= 8;
		this.ntpSec |= rawData[offSet++] & 0xFF;
		this.ntpSec <<= 8;
		this.ntpSec |= rawData[offSet++] & 0xFF;

		this.ntpFrac |= rawData[offSet++] & 0xFF;
		this.ntpFrac <<= 8;
		this.ntpFrac |= rawData[offSet++] & 0xFF;
		this.ntpFrac <<= 8;
		this.ntpFrac |= rawData[offSet++] & 0xFF;
		this.ntpFrac <<= 8;
		this.ntpFrac |= rawData[offSet++] & 0xFF;

		this.rtpTs |= rawData[offSet++] & 0xFF;
		this.rtpTs <<= 8;
		this.rtpTs |= rawData[offSet++] & 0xFF;
		this.rtpTs <<= 8;
		this.rtpTs |= rawData[offSet++] & 0xFF;
		this.rtpTs <<= 8;
		this.rtpTs |= rawData[offSet++] & 0xFF;

		this.psent |= rawData[offSet++] & 0xFF;
		this.psent <<= 8;
		this.psent |= rawData[offSet++] & 0xFF;
		this.psent <<= 8;
		this.psent |= rawData[offSet++] & 0xFF;
		this.psent <<= 8;
		this.psent |= rawData[offSet++] & 0xFF;

		this.osent |= rawData[offSet++] & 0xFF;
		this.osent <<= 8;
		this.osent |= rawData[offSet++] & 0xFF;
		this.osent <<= 8;
		this.osent |= rawData[offSet++] & 0xFF;
		this.osent <<= 8;
		this.osent |= rawData[offSet++] & 0xFF;

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

		rawData[offSet++] = ((byte) ((this.ntpSec & 0xFF000000) >> 24));
		rawData[offSet++] = ((byte) ((this.ntpSec & 0x00FF0000) >> 16));
		rawData[offSet++] = ((byte) ((this.ntpSec & 0x0000FF00) >> 8));
		rawData[offSet++] = ((byte) ((this.ntpSec & 0x000000FF)));

		rawData[offSet++] = ((byte) ((this.ntpFrac & 0xFF000000) >> 24));
		rawData[offSet++] = ((byte) ((this.ntpFrac & 0x00FF0000) >> 16));
		rawData[offSet++] = ((byte) ((this.ntpFrac & 0x0000FF00) >> 8));
		rawData[offSet++] = ((byte) ((this.ntpFrac & 0x000000FF)));

		rawData[offSet++] = ((byte) ((this.rtpTs & 0xFF000000) >> 24));
		rawData[offSet++] = ((byte) ((this.rtpTs & 0x00FF0000) >> 16));
		rawData[offSet++] = ((byte) ((this.rtpTs & 0x0000FF00) >> 8));
		rawData[offSet++] = ((byte) ((this.rtpTs & 0x000000FF)));

		rawData[offSet++] = ((byte) ((this.psent & 0xFF000000) >> 24));
		rawData[offSet++] = ((byte) ((this.psent & 0x00FF0000) >> 16));
		rawData[offSet++] = ((byte) ((this.psent & 0x0000FF00) >> 8));
		rawData[offSet++] = ((byte) ((this.psent & 0x000000FF)));

		rawData[offSet++] = ((byte) ((this.osent & 0xFF000000) >> 24));
		rawData[offSet++] = ((byte) ((this.osent & 0x00FF0000) >> 16));
		rawData[offSet++] = ((byte) ((this.osent & 0x0000FF00) >> 8));
		rawData[offSet++] = ((byte) ((this.osent & 0x000000FF)));

		for (RtcpReceptionReportItem rtcpReceptionReportItem : rtcpReceptionReports) {
			if (rtcpReceptionReportItem != null) {
				offSet = rtcpReceptionReportItem.encode(rawData, offSet);
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

	public long getSsrc() {
		return ssrc;
	}

	public long getNtpSec() {
		return ntpSec;
	}

	public long getNtpFrac() {
		return ntpFrac;
	}

	public long getRtpTs() {
		return rtpTs;
	}

	public long getPsent() {
		return psent;
	}

	public long getOsent() {
		return osent;
	}

	public RtcpReceptionReportItem[] getRtcpReceptionReports() {
		return rtcpReceptionReports;
	}

	public void addRtcpReceptionReportItem(RtcpReceptionReportItem rtcpReceptionReportItem) {
		this.rtcpReceptionReports[this.count++] = rtcpReceptionReportItem;
	}

}
