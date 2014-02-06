package org.mobicents.media.core.ice.sdp;

import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.mobicents.media.core.ice.IceAgent;
import org.mobicents.media.core.ice.IceComponent;
import org.mobicents.media.core.ice.IceMediaStream;
import org.mobicents.media.core.ice.candidate.IceCandidate;
import org.mobicents.media.core.ice.candidate.LocalCandidateWrapper;
import org.mobicents.media.core.ice.sdp.attributes.CandidateAttribute;
import org.mobicents.media.core.ice.sdp.attributes.RtcpAttribute;

public class IceSdpNegotiator {

	public SessionDescription negotiate(String sdp, IceAgent agent)
			throws SdpException {
		SdpFactory factory = SdpFactory.getInstance();
		SessionDescription sessionDescription = factory
				.createSessionDescription(sdp);

		// Add user fragment and password from ICE agent
		sessionDescription.setAttribute("ice-ufrag", agent.getUfrag());
		sessionDescription.setAttribute("ice-pwd", agent.getPassword());

		// If the ICE agent is lite, sdp must include a 'a=ice-lite' attribute
		if (agent.isLite()) {
			sessionDescription.setAttribute("ice-lite", "");
		}

		// Update the media streams of the SDP description with the information
		// hold by the ICE agent
		Vector<MediaDescription> mediaStreams = sessionDescription
				.getMediaDescriptions(true);
		for (MediaDescription mediaStream : mediaStreams) {
			String streamName = mediaStream.getMedia().getMediaType();
			IceMediaStream iceStream = agent.getMediaStream(streamName);

			if (iceStream == null) {
				// Proceed to next media stream if ICE agent has no information
				// about current stream
				// TODO When video is implemented, it should throw exception
				continue;
			}

			// Add the ICE Candidates to the Session Description
			IceComponent rtpComponent = iceStream.getRtpComponent();
			addCandidates(rtpComponent, mediaStream);
			if (iceStream.supportsRtcp()) {
				addCandidates(iceStream.getRtcpComponent(), mediaStream);
			}

			/*
			 * The default candidates are added to the SDP as the default
			 * destination for media.
			 * 
			 * This is done by placing the IP address and port of the RTP
			 * candidate into the "c=" and "m=" lines.
			 */
			if (!rtpComponent.isDefaultLocalCandidateSelected()) {
				throw new SdpException(
						"RTP component does not have a default local candidate selected.");
			}

			IceCandidate defaultCandidate = rtpComponent
					.getDefaultLocalCandidate().getCandidate();
			String defaultAddress = defaultCandidate.getAddress()
					.getHostAddress();
			int defaultPort = defaultCandidate.getPort();

			mediaStream.getConnection().setAddress(defaultAddress);
			mediaStream.getMedia().setMediaPort(defaultPort);

			/*
			 * If the agent is using RTCP, it must encode the RTCP candidate
			 * using the 'a=rtcp' attribute.
			 * 
			 * Otherwise, the agent must signal that using 'b=RS:0' and 'b=RR:0'
			 */
			if (iceStream.supportsRtcp()) {
				RtcpAttribute rtcpAttribute = new RtcpAttribute(
						defaultCandidate, "IN");
				mediaStream.addAttribute(rtcpAttribute);
			} else {
				mediaStream.setBandwidth("RS", 0);
				mediaStream.setBandwidth("RR", 0);
			}
		}
		return sessionDescription;
	}

	private void addCandidates(IceComponent component, MediaDescription media)
			throws SdpException {
		for (LocalCandidateWrapper candidate : component.getLocalCandidates()) {
			addCandidate(media, candidate);
		}
	}

	private void addCandidate(MediaDescription media,
			LocalCandidateWrapper candidate) throws SdpException {
		CandidateAttribute candidateAttribute = new CandidateAttribute(
				candidate.getCandidate());
		media.addAttribute(candidateAttribute);
	}

}
