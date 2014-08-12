package org.mobicents.media.server.impl.rtp;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates statistics of an RTP channel
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpStatistics {

	private final long ssrc;
	
	/*
	 * RTP statistics
	 */
	private volatile long received;
	private volatile long transmitted;
	private int sequenceNumber;
	
	private long lastPacketReceived;
	
	/*
	 * RTCP statistics
	 */
	/**
	 * the most current estimate for the number of senders in the session
	 */
	private int senders;
	
	/**
	 * List of SSRC that are senders
	 */
	private final List<Long> sendersList;
	
	public RtpStatistics() {
		this.ssrc = System.currentTimeMillis();
		
		// RTP statistics
		this.received = 0;
		this.transmitted = 0;
		this.sequenceNumber = 0;
		this.lastPacketReceived = 0;
		
		// RTCP statistics
		this.senders = 0;
		this.sendersList = new ArrayList<Long>();
	}
	
	public long getSsrc() {
		return ssrc;
	}
	
	/*
	 * RTP Statistics
	 */
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
	
	/*
	 * RTCP Statistics
	 */
	public int getSenders() {
		return this.senders;
	}
	
	public boolean isSender(long ssrc) {
		synchronized (this.sendersList) {
			return this.sendersList.contains(Long.valueOf(ssrc));
		}
	}
	
	public void addSender(long ssrc) {
		synchronized (this.sendersList) {
			if(!this.sendersList.contains(Long.valueOf(ssrc))) {
				this.sendersList.add(Long.valueOf(ssrc));
				this.senders++;
			}
		}
	}

	public void removeSender(long ssrc) {
		synchronized (this.sendersList) {
			if (this.sendersList.remove(Long.valueOf(ssrc))) {
				this.senders--;
			}
		}
	}
	
	public void reset() {
		// TODO finish reset for RTCP statistics
		this.received = 0;
		this.transmitted = 0;
	}
}
