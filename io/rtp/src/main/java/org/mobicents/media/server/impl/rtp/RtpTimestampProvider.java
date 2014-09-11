package org.mobicents.media.server.impl.rtp;

/**
 * Provides the current RTP timestamp.
 * 
 * @author Henrique Rosa
 * 
 */
public interface RtpTimestampProvider {

	long getRtpTimestamp();
	
	long getDtmfTimestamp();

}
