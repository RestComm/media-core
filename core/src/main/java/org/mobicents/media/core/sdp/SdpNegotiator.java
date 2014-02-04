package org.mobicents.media.core.sdp;

import gov.nist.javax.sdp.fields.AttributeField;

import java.util.List;
import java.util.Vector;

import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpConstants;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Candidate;
import org.ice4j.ice.CandidateType;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.RemoteCandidate;
import org.ice4j.ice.sdp.CandidateAttribute;
import org.ice4j.ice.sdp.IceSdpUtils;
import org.mobicents.media.core.MediaTypes;
import org.mobicents.media.core.ice.IceAgent;
import org.mobicents.media.server.impl.rtp.sdp.CandidateField;
import org.mobicents.media.server.utils.Text;

/**
 * Negotiates Session Description contents, including ICE candidates.
 * 
 * @author Henrique Rosa
 * 
 */
public class SdpNegotiator {

	private static final SdpFactory FACTORY = SdpFactory.getInstance();

	private final SessionDescription offer;
	private SessionDescription answer;
	private IceAgent iceAgent;

	public SdpNegotiator(String sdp) throws SdpParseException {
		this(sdp, null);
	}

	public SdpNegotiator(String sdp, IceAgent iceAgent)
			throws SdpParseException {
		this.offer = FACTORY.createSessionDescription(sdp);
		this.iceAgent = iceAgent;
	}

	public IceAgent getIceAgent() {
		return iceAgent;
	}

	public void setIceAgent(IceAgent iceAgent) {
		this.iceAgent = iceAgent;
	}

	public void setIceRemoteCandidates(String streamName) {
//		if (this.iceAgent != null) {
//			IceMediaStream iceStream = this.iceAgent.getStream(streamName);
//			if (iceStream != null) {
//				MediaDescription mediaStream = getMediaDescription(this.offer,
//						streamName);
//				if (mediaStream != null) {
//					// Set remote ice user and password
//					iceStream.setRemotePassword(getIcePwd(mediaStream,
//							this.offer));
//					iceStream.setRemoteUfrag(getIceUfrag(mediaStream,
//							this.offer));
//
//					// set remote candidates
//					@SuppressWarnings("unchecked")
//					Vector<AttributeField> attributes = mediaStream
//							.getAttributes(false);
//					if (attributes != null) {
//						for (AttributeField attribute : attributes) {
//							if (attribute.equals(CandidateAttribute.NAME)) {
//								addRemoteIceCandidate(attribute, iceStream);
//							}
//						}
//					}
//
//					// set default RTP candidate
//					setDefaultIceRemoteCandidate(mediaStream);
//				}
//			}
//		}
	}

	public void setDefaultIceRemoteCandidate(MediaDescription mediaDescription) {
//		try {
//			String address = getConnectionAddress(mediaDescription, this.offer);
//			int port = mediaDescription.getMedia().getMediaPort();
//			TransportAddress rtpAddress = new TransportAddress(address, port,
//					Transport.UDP);
//			// TODO retrieve RTCP address - hrosa
//
//			IceMediaStream audioStream = this.iceAgent
//					.getStream(MediaTypes.AUDIO.lowerName());
//			Component rtpComponent = audioStream.getComponent(Component.RTP);
//			RemoteCandidate remoteCandidate = rtpComponent
//					.findRemoteCandidate(rtpAddress);
//			rtpComponent.setDefaultRemoteCandidate(remoteCandidate);
//			// TODO set RTCP default component
//		} catch (SdpParseException e) {
//			// does not happen
//		}

	}

	/**
	 * Gets the <code>ice-pwd</code> attribute from a Media Description.
	 * 
	 * @param mediaDescription
	 *            The media description to fetch the attribute from
	 * @param sessionDescription
	 *            The session description that owns the media description
	 * @return The <code>ice-pwd</code> from the media description. If it does
	 *         not exist, gets the attributes from the session description.<br>
	 *         If the attribute is not found, returns null.
	 */
	private String getIcePwd(MediaDescription mediaDescription,
			SessionDescription sessionDescription) {
		return findAttribute("ice-pwd", mediaDescription, sessionDescription);
	}

	/**
	 * Gets the <code>ice-ufrag</code> attribute from a Media Description.
	 * 
	 * @param mediaDescription
	 *            The media description to fetch the attribute from
	 * @param sessionDescription
	 *            The session description that owns the media description
	 * @return The <code>ice-ufrag</code> from the media description. If it does
	 *         not exist, gets the attributes from the session description.<br>
	 *         If the attribute is not found, returns null.
	 */
	private String getIceUfrag(MediaDescription mediaDescription,
			SessionDescription sessionDescription) {
		return findAttribute("ice-ufrag", mediaDescription, sessionDescription);
	}

	private String findAttribute(String attributeName,
			MediaDescription mediaDescription,
			SessionDescription sessionDescription) {
		try {
			String attribute = mediaDescription.getAttribute(attributeName);
			if (attribute == null || attribute.isEmpty()) {
				attribute = sessionDescription.getAttribute(attributeName);
			}
			return attribute;
		} catch (SdpParseException e) {
			// Does not happen
			return null;
		}
	}

	private String getConnectionAddress(MediaDescription mediaDescription,
			SessionDescription sessionDescription) {
		try {
			String address = mediaDescription.getConnection().getAddress();
			if (address == null) {
				address = sessionDescription.getConnection().getAddress();
			}
			return address;
		} catch (SdpParseException e) {
			return null;
		}
	}

	/**
	 * Adds a remote ICE candidate to an ICE Media Stream.
	 * 
	 * @param candidateField
	 *            The SDP attribute representing the remote candidate
	 * @param iceStream
	 *            The media stream of an ICE agent
	 */
	private RemoteCandidate addRemoteIceCandidate(
			AttributeField candidateField, IceMediaStream iceStream) {
		String candidateLine;
		try {
			// Parse the candidate string from the SDP offer
			candidateLine = candidateField.getValue();
			CandidateField candidate = new CandidateField(new Text(
					candidateLine));

			// Get the ICE component associated with the remote candidate (RTP
			// or RTPC)
			Component component = iceStream.getComponent(candidate
					.getComponentId().toInteger());
			if (component != null) {
				// Check if there is a related address property
				RemoteCandidate relatedCandidate = null;
				if (candidate.hasRelatedAddress()) {
					Text relatedAddress = candidate.getRelatedAddress();
					int relatedPort = candidate.getRelatedPort().toInteger();
					TransportAddress raddr = new TransportAddress(
							relatedAddress.toString(), relatedPort,
							Transport.UDP);
					relatedCandidate = component.findRemoteCandidate(raddr);
				}

				// Associate the remote candidate with the ice media stream
				TransportAddress transportAddress = new TransportAddress(
						candidate.getAddress().toString(), candidate.getPort()
								.toInteger(), Transport.parse(candidate
								.getProtocol().toString()));
				RemoteCandidate remoteCandidate = new RemoteCandidate(
						transportAddress, component,
						CandidateType.parse(candidate.getType().toString()),
						candidate.getFoundation().toString(), candidate
								.getWeight().toLong(), relatedCandidate);
				return remoteCandidate;
			}
		} catch (SdpParseException e) {
			// does not happen
		}
		return null;
	}

	private MediaDescription getMediaDescription(SessionDescription sdp,
			String streamName) {
		try {
			@SuppressWarnings("unchecked")
			Vector<MediaDescription> descriptions = this.offer
					.getMediaDescriptions(true);
			for (MediaDescription description : descriptions) {
				if (description.getMedia().getMediaType().equals(streamName)) {
					return description;
				}
			}
		} catch (SdpException e) {
			// Does not happen
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private SessionDescription negotiate(SessionDescription sdp)
			throws SdpException {
//		// If the call is not using ICE, this answer is final
//		if (this.iceAgent == null) {
//			return sdp;
//		}
//
//		/*
//		 * ICE-options
//		 */
//		StringBuilder iceOptionsBuilder = new StringBuilder();
//
//		if (this.iceAgent.isTrickling()) {
//			iceOptionsBuilder.append(IceSdpUtils.ICE_OPTION_TRICKLE)
//					.append(" ");
//		}
//
//		String iceOptions = iceOptionsBuilder.toString().trim();
//		if (!iceOptions.isEmpty()) {
//			Attribute iceOptionsAttribute = FACTORY.createAttribute(
//					IceSdpUtils.ICE_OPTIONS, iceOptions);
//			sdp.getAttributes(true).add(iceOptionsAttribute);
//		}
//
//		/*
//		 * Origin
//		 */
//		TransportAddress defaultAddress = this.iceAgent.getStreams().get(0)
//				.getComponent(Component.RTP).getDefaultCandidate()
//				.getTransportAddress();
//
//		String addressFamily;
//		if (defaultAddress.isIPv6()) {
//			addressFamily = Connection.IP6;
//		} else {
//			addressFamily = Connection.IP4;
//		}
//
//		Origin origin = sdp.getOrigin();
//		if (origin == null || "user".equals(origin.getUsername())) {
//			// By default, jain-sdp creates a default origin that has "user" as
//			// the user name so we use this to detect it.
//			origin = FACTORY.createOrigin(defaultAddress.getHostName(), 0, 0,
//					"IN", addressFamily, defaultAddress.getHostAddress());
//		} else {
//			// if an origin existed, we just make sure it has the right address
//			// now and are careful not to touch anything else.
//			origin.setAddress(defaultAddress.getHostAddress());
//			origin.setAddressType(addressFamily);
//		}
//		sdp.setOrigin(origin);
//
//		/*
//		 * Media Lines
//		 */
//		List<IceMediaStream> streams = this.iceAgent.getStreams();
//		Vector<MediaDescription> sessionMedia = sdp.getMediaDescriptions(true);
//		Vector<MediaDescription> mediaVector = new Vector<MediaDescription>(
//				sessionMedia.size());
//
//		for (IceMediaStream stream : streams) {
//			// Find corresponding SDP media line
//			MediaDescription media = null;
//			for (MediaDescription md : sessionMedia) {
//				if (md.getMedia().getMediaType().equals(stream.getName())) {
//					media = md;
//					break;
//				}
//			}
//
//			// If media line already exists then update it with candidates
//			// Otherwise create new media line
//			if (media == null) {
//				media = FACTORY.createMediaDescription(stream.getName(), 0, 1,
//						SdpConstants.RTP_AVP, new int[] { 0 });
//			}
//
//			// Setup media line
//			// Set mid-s
//			media.setAttribute(IceSdpUtils.MID, stream.getName());
//
//			// Add candidates
//			Component firstComponent = null;
//			for (Component component : stream.getComponents()) {
//				// if this is the first component, remember it so that we
//				// can
//				// later use it for default candidates.
//				if (firstComponent == null) {
//					firstComponent = component;
//				}
//
//				for (Candidate<?> candidate : component.getLocalCandidates()) {
//					media.addAttribute(new CandidateAttribute(candidate));
//				}
//			}
//
//			// Set connection
//			media.getMedia().setMediaPort(defaultAddress.getPort());
//			media.setConnection(FACTORY.createConnection("IN",
//					defaultAddress.getHostAddress(), addressFamily));
//
//			// now check if the RTCP port for the default candidate is
//			// different than RTP.port+1, in which case we need to
//			// mention it.
//			Component rtcpComponent = stream.getComponent(Component.RTCP);
//			if (rtcpComponent != null) {
//				TransportAddress defaultRtcpCandidate = rtcpComponent
//						.getDefaultCandidate().getTransportAddress();
//
//				if (defaultRtcpCandidate.getPort() != defaultAddress.getPort() + 1) {
//					media.setAttribute("rtcp",
//							Integer.toString(defaultRtcpCandidate.getPort()));
//				}
//			}
//
//			mediaVector.add(media);
//		}
//
//		// Add lines that are already defined on the media description but not
//		// on the ICE agent
//		// Most probably the media lines that were rejected (m=video 0,
//		// m=application 0, etc)
//		for (MediaDescription media : sessionMedia) {
//			boolean found = false;
//			for (IceMediaStream stream : streams) {
//				if (media.getMedia().getMediaType().equals(stream.getName())) {
//					found = true;
//					break;
//				}
//				if (!found) {
//					mediaVector.add(media);
//				}
//			}
//		}
//
//		// Override session media
//		sdp.setMediaDescriptions(mediaVector);
//
//		/*
//		 * ICE credentials
//		 */
//		IceSdpUtils.setIceCredentials(sdp, this.iceAgent.getLocalUfrag(),
//				this.iceAgent.getLocalPassword());
//		return sdp;
		return null;
	}

	public String answer(String sdp) throws SdpException {
		SessionDescription answerProposal = FACTORY
				.createSessionDescription(sdp);
		this.answer = negotiate(answerProposal);
		return this.answer.toString();
	}
}
