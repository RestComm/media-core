package org.mobicents.media.server.impl.rtp;

/**
 * Encapsulates statistics of an RTP channel
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpStatistics {

	private volatile long received;
	private volatile long transmitted;
	private int sequenceNumber;
	
	private long lastPacketReceived;
	
	public RtpStatistics() {
		this.received = 0;
		this.transmitted = 0;
		this.sequenceNumber = 0;
		this.lastPacketReceived = 0;
	}
	
	public long getReceived() {
		return received;
	}

	public void incrementReceived() {
		this.received++;
	}
	
	public long getTransmitted() {
		return transmitted;
	}
	
	public void incrementTransmitted() {
		this.transmitted++;
	}
	
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	
	public int nextSequenceNumber() {
		this.sequenceNumber++;
		return this.sequenceNumber;
	}
	
	public long getLastPacketReceived() {
		return lastPacketReceived;
	}
	
	public void setLastPacketReceived(long lastPacketReceived) {
		this.lastPacketReceived = lastPacketReceived;
	}
}
