package org.mobicents.media.server.impl.rtcp;

import java.util.Date;

import org.apache.commons.net.ntp.TimeStamp;
import org.mobicents.media.server.impl.rtp.RtpStatistics;

/**
 * Factory for building RTCP packets
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpPacketFactory {

	/**
	 * Builds a packet containing an RTCP Sender Report.
	 * 
	 * @param statistics
	 *            The statistics of the RTP session
	 * @return The RTCP packet
	 */
	private static RtcpPacket buildSenderReport(RtpStatistics statistics) {
		/*
		 *         0                   1                   2                   3
         *         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * header |V=2|P|    RC   |   PT=SR=200   |             length            |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                         SSRC of sender                        |
         *        +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         * sender |              NTP timestamp, most significant word             |
         * info   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |             NTP timestamp, least significant word             |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                         RTP timestamp                         |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                     sender's packet count                     |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                      sender's octet count                     |
         *        +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         * report |                 SSRC_1 (SSRC of first source)                 |
         * block  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *   1    | fraction lost |       cumulative number of packets lost       |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |           extended highest sequence number received           |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                      interarrival jitter                      |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                         last SR (LSR)                         |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *        |                   delay since last SR (DLSR)                  |
         *        +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         * report |                 SSRC_2 (SSRC of second source)                |
         * block  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *   2    :                               ...                             :
         *        +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
         *        |                  profile-specific extensions                  |
         *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
		 */
		
		// FIXME validate padding!!!
		boolean padding = false;
		long ssrc = statistics.getSsrc();
		TimeStamp ntpTs = new TimeStamp(new Date(statistics.getCurrentTime()));
		long ntpSec = ntpTs.getSeconds();
		long ntpFrac = ntpTs.getFraction();
		long rtpTs = statistics.getRtpTime();
		long psent = statistics.getRtpPacketsSent();
		long osent = statistics.getRtpOctetsReceived();
		
		RtcpSenderReport senderReport = new RtcpSenderReport(padding, ssrc, ntpSec, ntpFrac, rtpTs, psent, osent);
		return null;
	}

	/**
	 * Builds a packet containing an RTCP Receiver Report
	 * 
	 * @param statistics
	 *            The statistics of the RTP session
	 * @return The RTCP packet
	 */
	private static RtcpPacket buildReceiverReport(RtpStatistics statistics) {
		// TODO implement buildSenderReport
		return null;
	}

	/**
	 * Builds a packet containing an RTCP Report.
	 * 
	 * RTP receivers provide reception quality feedback using RTCP report
	 * packets which may take one of two forms depending upon whether or not the
	 * receiver is also a sender. The only difference between the sender report
	 * (SR) and receiver report (RR) forms, besides the packet type code, is
	 * that the sender report includes a 20-byte sender information section for
	 * use by active senders. The SR is issued if a site has sent any data
	 * packets during the interval since issuing the last report or the previous
	 * one, otherwise the RR is issued.
	 * 
	 * Both the SR and RR forms include zero or more reception report blocks,
	 * one for each of the synchronization sources from which this receiver has
	 * received RTP data packets since the last report. Reports are not issued
	 * for contributing sources listed in the CSRC list. Each reception report
	 * block provides statistics about the data received from the particular
	 * source indicated in that block.
	 * 
	 * Since a maximum of 31 reception report blocks will fit in an SR or RR
	 * packet, additional RR packets SHOULD be stacked after the initial SR or
	 * RR packet as needed to contain the reception reports for all sources
	 * heard during the interval since the last report. If there are too many
	 * sources to fit all the necessary RR packets into one compound RTCP packet
	 * without exceeding the MTU of the network path, then only the subset that
	 * will fit into one MTU SHOULD be included in each interval. The subsets
	 * SHOULD be selected round-robin across multiple intervals so that all
	 * sources are reported.
	 * 
	 * @param statistics
	 *            The statistics of the RTP session
	 * @return The RTCP packet containing the RTCP Report (SS or RR).
	 */
	public static RtcpPacket buildReport(RtpStatistics statistics) {
		if (statistics.hasSent()) {
			return buildSenderReport(statistics);
		} else {
			return buildReceiverReport(statistics);
		}
	}

	/**
	 * Builds a packet containing an RTCP BYE message.
	 * 
	 * @param statistics
	 *            The statistics of the RTP session
	 * @return The RTCP packet
	 */
	public static RtcpPacket buildBye(RtpStatistics statistics) {
		// TODO implement buildSenderReport
		return null;
	}
}
