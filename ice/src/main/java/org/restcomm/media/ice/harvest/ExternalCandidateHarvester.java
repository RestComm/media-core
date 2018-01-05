/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.ice.harvest;

import java.net.InetAddress;
import java.nio.channels.Selector;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.ice.CandidateType;
import org.restcomm.media.ice.FoundationsRegistry;
import org.restcomm.media.ice.HostCandidate;
import org.restcomm.media.ice.IceComponent;
import org.restcomm.media.ice.IceMediaStream;
import org.restcomm.media.ice.LocalCandidateWrapper;
import org.restcomm.media.ice.ServerReflexiveCandidate;
import org.restcomm.media.network.deprecated.RtpPortManager;

/**
 * Gathers SRFLX candidates for the public address on which Media Server is installed.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ExternalCandidateHarvester implements CandidateHarvester {
	
	Logger logger = LogManager.getLogger(ExternalCandidateHarvester.class);

	private final FoundationsRegistry foundations;

	private final InetAddress externalAddress;
	
	public ExternalCandidateHarvester(final FoundationsRegistry foundations, final InetAddress externalAddress) {
		super();
		this.foundations = foundations;
		this.externalAddress = externalAddress;
	}
	
	@Override
	public void harvest(RtpPortManager portManager, IceMediaStream mediaStream, Selector selector) throws HarvestException {
		harvest(mediaStream.getRtpComponent());
		if(mediaStream.supportsRtcp() && !mediaStream.isRtcpMux()) {
			harvest(mediaStream.getRtcpComponent());
		}
	}
	
	private void harvest(IceComponent component) {
		// Gather SRFLX candidate for each host candidate of the component
		List<LocalCandidateWrapper> rtpCandidates = component.getLocalCandidates();

		for (LocalCandidateWrapper candidateWrapper : rtpCandidates) {
			// Create one reflexive candidate for each host candidate
			if(candidateWrapper.getCandidate() instanceof HostCandidate) {
				HostCandidate hostCandidate = (HostCandidate) candidateWrapper.getCandidate();
				ServerReflexiveCandidate srflxCandidate = new ServerReflexiveCandidate(component, externalAddress, hostCandidate.getPort(), hostCandidate);
				this.foundations.assignFoundation(srflxCandidate);
				component.addLocalCandidate(new LocalCandidateWrapper(srflxCandidate, candidateWrapper.getChannel()));
			}
		}
	}

	@Override
	public CandidateType getCandidateType() {
		return CandidateType.SRFLX;
	}

}
