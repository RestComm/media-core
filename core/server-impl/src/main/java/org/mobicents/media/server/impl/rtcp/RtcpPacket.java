package org.mobicents.media.server.impl.rtcp;

import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpPacket implements Serializable {
	
	private static final Logger logger = Logger.getLogger(RtcpPacket.class);

	private RtcpSenderReport rtcpSenderReport = null;
	private RtcpReceptionReport rtcpReceptionReport = null;
	private RtcpSdes rtcpSdes = null;
	private RtcpBye rtcpBye = null;
	private RtcpAppDefined rtcpAppDefined = null;
	
	private int noOfPackets = 0;
	private int packetSize = 0;

	public RtcpPacket() {

	}

	public RtcpPacket(RtcpSenderReport rtcpSenderReport, RtcpReceptionReport rtcpReceptionReport, RtcpSdes rtcpSdes,
			RtcpBye rtcpBye, RtcpAppDefined rtcpAppDefined) {
		this.rtcpSenderReport = rtcpSenderReport;
		this.rtcpReceptionReport = rtcpReceptionReport;
		this.rtcpSdes = rtcpSdes;
		this.rtcpBye = rtcpBye;
		this.rtcpAppDefined = rtcpAppDefined;
	}

	public int decode(byte[] rawData, int offSet) {
		this.packetSize = rawData.length - offSet;
		while (offSet < rawData.length) {
			int type = rawData[offSet + 1] & 0x000000FF;
			switch (type) {
			case RtcpCommonHeader.RTCP_SR:
				noOfPackets++;
				this.rtcpSenderReport = new RtcpSenderReport();
				offSet = this.rtcpSenderReport.decode(rawData, offSet);
				break;
			case RtcpCommonHeader.RTCP_RR:
				noOfPackets++;
				this.rtcpReceptionReport = new RtcpReceptionReport();
				offSet = this.rtcpReceptionReport.decode(rawData, offSet);
				break;
			case RtcpCommonHeader.RTCP_SDES:
				noOfPackets++;
				this.rtcpSdes = new RtcpSdes();
				offSet = this.rtcpSdes.decode(rawData, offSet);
				break;
			case RtcpCommonHeader.RTCP_APP:
				noOfPackets++;
				this.rtcpAppDefined = new RtcpAppDefined();
				offSet = this.rtcpAppDefined.decode(rawData, offSet);
				break;
			case RtcpCommonHeader.RTCP_BYE:
				noOfPackets++;
				this.rtcpBye = new RtcpBye();
				offSet = this.rtcpBye.decode(rawData, offSet);
				break;
			default:				
				logger.error("Received type = "+type+" RTCP Packet decoding falsed. offSet = "+offSet);
				offSet = rawData.length;
				break;
			}
		}

		return offSet;
	}

	public int encode(byte[] rawData, int offSet) {
		if (this.rtcpSenderReport != null) {
			noOfPackets++;
			offSet = this.rtcpSenderReport.encode(rawData, offSet);
		}
		if (this.rtcpReceptionReport != null) {
			noOfPackets++;
			offSet = this.rtcpReceptionReport.encode(rawData, offSet);
		}
		if (this.rtcpSdes != null) {
			noOfPackets++;
			offSet = this.rtcpSdes.encode(rawData, offSet);
		}
		if (this.rtcpAppDefined != null) {
			noOfPackets++;
			offSet = this.rtcpAppDefined.encode(rawData, offSet);
		}
		if (this.rtcpBye != null) {
			noOfPackets++;
			offSet = this.rtcpBye.encode(rawData, offSet);
		}
		return offSet;
	}

	public RtcpSenderReport getRtcpSenderReport() {
		return rtcpSenderReport;
	}

	public RtcpReceptionReport getRtcpReceptionReport() {
		return rtcpReceptionReport;
	}

	public RtcpSdes getRtcpSdes() {
		return rtcpSdes;
	}

	public RtcpBye getRtcpBye() {
		return rtcpBye;
	}

	public RtcpAppDefined getRtcpAppDefined() {
		return rtcpAppDefined;
	}

	public int getNoOfPackets() {
		return noOfPackets;
	}

	public int getPacketSize() {
		return packetSize;
	}
}
