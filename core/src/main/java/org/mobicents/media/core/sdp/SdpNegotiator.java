package org.mobicents.media.core.sdp;

import java.util.List;
import java.util.Vector;

import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpConstants;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.Candidate;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.sdp.CandidateAttribute;
import org.ice4j.ice.sdp.IceSdpUtils;

/**
 * Negotiates Session Description contents, including ICE candidates.
 * 
 * @author Henrique Rosa
 * 
 */
public class SdpNegotiator {

	public static void updateSDP(SessionDescription sdp, Agent agent)
			throws SdpException {
		SdpFactory sdpFactory = SdpFactory.getInstance();

		/*
		 * ICE-options
		 */
		StringBuilder iceOptionsBuilder = new StringBuilder();

		if (agent.isTrickling()) {
			iceOptionsBuilder.append(IceSdpUtils.ICE_OPTION_TRICKLE)
					.append(" ");
		}

		// TODO add other ice-options as necessary - hrosa

		String iceOptions = iceOptionsBuilder.toString().trim();
		if (!iceOptions.isEmpty()) {
			Attribute iceOptionsAttribute = sdpFactory.createAttribute(
					IceSdpUtils.ICE_OPTIONS, iceOptions);
			sdp.getAttributes(true).add(iceOptionsAttribute);
		}

		/*
		 * Origin
		 */
		TransportAddress defaultAddress = agent.getStreams().get(0)
				.getComponent(Component.RTP).getDefaultCandidate()
				.getTransportAddress();

		String addressFamily;
		if (defaultAddress.isIPv6()) {
			addressFamily = Connection.IP6;
		} else {
			addressFamily = Connection.IP4;
		}

		Origin origin = sdp.getOrigin();
		if (origin == null || "user".equals(origin.getUsername())) {
			// By default, jain-sdp creates a default origin that has "user" as
			// the user name so we use this to detect it.
			origin = sdpFactory.createOrigin(defaultAddress.getHostName(), 0,
					0, "IN", addressFamily, defaultAddress.getHostAddress());
		} else {
			// if an origin existed, we just make sure it has the right address
			// now and are careful not to touch anything else.
			origin.setAddress(defaultAddress.getHostAddress());
			origin.setAddressType(addressFamily);
		}
		sdp.setOrigin(origin);

		/*
		 * Media Lines
		 */
		List<IceMediaStream> streams = agent.getStreams();
		Vector<MediaDescription> sessionMedia = sdp.getMediaDescriptions(true);
		Vector<MediaDescription> mediaVector = new Vector<MediaDescription>(
				sessionMedia.size());

		for (IceMediaStream stream : streams) {
			// Find corresponding SDP media line
			MediaDescription media = null;
			for (MediaDescription md : sessionMedia) {
				if (md.getMedia().getMediaType().equals(stream.getName())) {
					media = md;
					break;
				}
			}

			// If media line already exists then update it with candidates
			// Otherwise create new media line
			if (media == null) {
				media = sdpFactory.createMediaDescription(stream.getName(), 0,
						1, SdpConstants.RTP_AVP, new int[] { 0 });
			}

			// Setup media line
			// Set mid-s
			media.setAttribute(IceSdpUtils.MID, stream.getName());

			// Add candidates
			Component firstComponent = null;
			for (Component component : stream.getComponents()) {
				// if this is the first component, remember it so that we
				// can
				// later use it for default candidates.
				if (firstComponent == null) {
					firstComponent = component;
				}

				for (Candidate<?> candidate : component.getLocalCandidates()) {
					media.addAttribute(new CandidateAttribute(candidate));
				}
			}

			// Set connection
			media.getMedia().setMediaPort(defaultAddress.getPort());
			media.setConnection(sdpFactory.createConnection("IN",
					defaultAddress.getHostAddress(), addressFamily));

			// now check if the RTCP port for the default candidate is
			// different than RTP.port+1, in which case we need to
			// mention it.
			Component rtcpComponent = stream.getComponent(Component.RTCP);
			if (rtcpComponent != null) {
				TransportAddress defaultRtcpCandidate = rtcpComponent
						.getDefaultCandidate().getTransportAddress();

				if (defaultRtcpCandidate.getPort() != defaultAddress.getPort() + 1) {
					media.setAttribute("rtcp",
							Integer.toString(defaultRtcpCandidate.getPort()));
				}
			}

			mediaVector.add(media);
		}

		// Add lines that are already defined on the media description but not
		// on the ICE agent
		// Most probably the media lines that were rejected (m=video 0,
		// m=application 0, etc)
		for (MediaDescription media : sessionMedia) {
			boolean found = false;
			for (IceMediaStream stream : streams) {
				if (media.getMedia().getMediaType().equals(stream.getName())) {
					found = true;
					break;
				}
				if (!found) {
					mediaVector.add(media);
				}
			}
		}

		// Override session media
		sdp.setMediaDescriptions(mediaVector);

		/*
		 * ICE credentials
		 */
		IceSdpUtils.setIceCredentials(sdp, agent.getLocalUfrag(),
				agent.getLocalPassword());
	}

	public static String answer(Agent localAgent, String sdp)
			throws SdpException {
		SdpFactory factory = SdpFactory.getInstance();
		SessionDescription sessionDescription = factory
				.createSessionDescription(sdp);
		SdpNegotiator.updateSDP(sessionDescription, localAgent);
		return sessionDescription.toString();
	}
}
