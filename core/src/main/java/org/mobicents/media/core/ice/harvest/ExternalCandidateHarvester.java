package org.mobicents.media.core.ice.harvest;

import java.net.InetAddress;
import java.nio.channels.Selector;
import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.media.core.ice.CandidateType;
import org.mobicents.media.core.ice.FoundationsRegistry;
import org.mobicents.media.core.ice.HostCandidate;
import org.mobicents.media.core.ice.IceComponent;
import org.mobicents.media.core.ice.IceMediaStream;
import org.mobicents.media.core.ice.LocalCandidateWrapper;
import org.mobicents.media.core.ice.ServerReflexiveCandidate;
import org.mobicents.media.server.io.network.PortManager;

/**
 * Gathers SRFLX candidates for the public address on which Media Server is installed.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ExternalCandidateHarvester implements CandidateHarvester {
	
	Logger logger = Logger.getLogger(ExternalCandidateHarvester.class);

	private final FoundationsRegistry foundations;

	private final InetAddress externalAddress;
	
	public ExternalCandidateHarvester(final FoundationsRegistry foundations, final InetAddress externalAddress) {
		super();
		this.foundations = foundations;
		this.externalAddress = externalAddress;
	}
	
	public void harvest(PortManager portManager, IceMediaStream mediaStream, Selector selector) throws HarvestException {
		// Gather SRFLX candidate for each host candidate of the RTP component
		IceComponent rtpComponent = mediaStream.getRtpComponent();
		List<LocalCandidateWrapper> rtpCandidates = mediaStream.getRtpComponent().getLocalCandidates();

		for (LocalCandidateWrapper candidateWrapper : rtpCandidates) {
			// Create one reflexive candidate for each host candidate
			if(candidateWrapper.getCandidate() instanceof HostCandidate) {
				HostCandidate hostCandidate = (HostCandidate) candidateWrapper.getCandidate();
				ServerReflexiveCandidate srflxCandidate = new ServerReflexiveCandidate(rtpComponent, externalAddress, hostCandidate.getPort(), hostCandidate);
				this.foundations.assignFoundation(srflxCandidate);
				rtpComponent.addLocalCandidate(new LocalCandidateWrapper(srflxCandidate, candidateWrapper.getChannel()));
				
				// TODO add srflx candidate for rtcp if supported - hrosa
			}
		}
	}

	public CandidateType getCandidateType() {
		return CandidateType.SRFLX;
	}

}
