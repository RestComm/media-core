package org.mobicents.media.core.ice.events;

import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.mobicents.media.core.ice.CandidatePair;
import org.mobicents.media.core.ice.IceAgent;
import org.mobicents.media.core.ice.IceComponent;

public class CandidatePairSelectedEvent {

	private final IceAgent source;
	private final CandidatePair candidatePair;
	private final String streamName;

	public CandidatePairSelectedEvent(IceAgent source, String stringName,
			CandidatePair candidatePair) {
		super();
		this.source = source;
		this.streamName = stringName;
		this.candidatePair = candidatePair;
	}

	public IceAgent getSource() {
		return source;
	}

	public String getStreamName() {
		return streamName;
	}

	public CandidatePair getCandidatePair() {
		return candidatePair;
	}

	public int getComponentId() {
		return this.candidatePair.getComponentId();
	}

	public SelectionKey getSelectionKey() {
		return candidatePair.getSelectionKey();
	}

	public DatagramChannel getUdpChannel() {
		return (DatagramChannel) getSelectionKey().channel();
	}

	public boolean isRtcp() {
		return getComponentId() == IceComponent.RTCP_ID;
	}

}
