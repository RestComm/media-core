package org.mobicents.media.io.ice.sdp;

import gov.nist.javax.sdp.fields.AttributeField;
import gov.nist.javax.sdp.fields.ConnectionField;

import java.util.Vector;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.apache.log4j.Logger;
import org.mobicents.media.io.ice.IceAgent;
import org.mobicents.media.io.ice.IceCandidate;
import org.mobicents.media.io.ice.IceComponent;
import org.mobicents.media.io.ice.IceMediaStream;
import org.mobicents.media.io.ice.LocalCandidateWrapper;
import org.mobicents.media.io.ice.sdp.attributes.CandidateAttribute;
import org.mobicents.media.io.ice.sdp.attributes.IceLiteAttribute;
import org.mobicents.media.io.ice.sdp.attributes.IcePwdAttribute;
import org.mobicents.media.io.ice.sdp.attributes.IceUfragAttribute;
import org.mobicents.media.io.ice.sdp.attributes.RtcpAttribute;
import org.mobicents.media.io.ice.sdp.attributes.RtcpMuxAttribute;

/**
 * Provides methods to update SDP descriptions with information provided by an
 * ICE Agent.
 * 
 * @author Henrique Rosa
 * 
 */
public class IceSdpNegotiator {
	
	private static final Logger logger = Logger.getLogger(IceSdpNegotiator.class);
	
	@SuppressWarnings("unchecked")
	public static boolean isRtcpMux(String sdp) {
		try {
			SdpFactory factory = SdpFactory.getInstance();
			SessionDescription sessionDescription = factory.createSessionDescription(sdp);
			
			Vector<MediaDescription> mediaDescriptions = sessionDescription.getMediaDescriptions(false);
			if(mediaDescriptions != null) {
				for (MediaDescription mediaDescription : mediaDescriptions) {
					String mediaType = mediaDescription.getMedia().getMediaType();
					if("audio".equals(mediaType)) {
						Vector<AttributeField> attributes = mediaDescription.getAttributes(false);
						if(attributes != null) {
							for (AttributeField attribute : attributes) {
								if("rtcp-mux".equals(attribute.getName())) {
									return true;
								}
							}
						}
					}
				}
			}
			return false;
		} catch (SdpException e) {
			logger.error("RTCP-MUX will be deactivated because SDP could not be parsed: "+ e.getMessage(), e);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public static SessionDescription updateAnswer(String sdp, IceAgent agent) throws SdpException {
		SdpFactory factory = SdpFactory.getInstance();
		SessionDescription sessionDescription = factory.createSessionDescription(sdp);

		// Add user fragment and password from ICE agent
		Vector<AttributeField> attributes = sessionDescription.getAttributes(true);
		attributes.add(new IceUfragAttribute(agent.getUfrag()));
		attributes.add(new IcePwdAttribute(agent.getPassword()));

		// If the ICE agent is lite, sdp must include a 'a=ice-lite' attribute
		if (agent.isLite()) {
			attributes.add(new IceLiteAttribute());
		}
		sessionDescription.setAttributes(attributes);

		// Update the media streams of the SDP description with the information
		// hold by the ICE agent
		Vector<MediaDescription> mediaStreams = sessionDescription.getMediaDescriptions(true);
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
				throw new SdpException("RTP component does not have a default local candidate selected.");
			}

			IceCandidate defaultRtpCandidate = rtpComponent.getDefaultLocalCandidate().getCandidate();
			String defaultAddress = defaultRtpCandidate.getAddress().getHostAddress();
			int defaultPort = defaultRtpCandidate.getPort();
			String addressType = defaultRtpCandidate.isIPv4() ? "IP4" : "IP6";

			// SdpTemplate only uses global connection 'c=' line
			Connection mediaConnection = mediaStream.getConnection();
			if (mediaConnection == null) {
				mediaConnection = new ConnectionField();
				mediaConnection.setAddressType(addressType);
				mediaConnection.setNetworkType("IN");
				mediaConnection.setAddress(defaultAddress);
				mediaStream.setConnection(mediaConnection);
			} else {
				mediaConnection.setAddress(defaultAddress);
			}
			mediaStream.getMedia().setMediaPort(defaultPort);

			/*
			 * If the agent is using RTCP, it must encode the RTCP candidate
			 * using the 'a=rtcp' attribute. In case RTP and RTCP are multiplexed
			 * in the same channel, then we must signal it using the 'a=rtcp-mux'
			 * attribute. 
			 * 
			 * Otherwise, the agent must signal that using 'b=RS:0' and 'b=RR:0'
			 */
			if (iceStream.supportsRtcp()) {
				if(iceStream.isRtcpMux()) {
					mediaStream.addAttribute(new RtcpMuxAttribute());
					// Add a=rtcp attribute for legacy compatibility
					RtcpAttribute rtcpAttribute = new RtcpAttribute(defaultRtpCandidate, "IN");
					mediaStream.addAttribute(rtcpAttribute);
				} else {
					IceComponent rtcpComponent = iceStream.getRtcpComponent();
					IceCandidate defaultRtcpCandidate = rtcpComponent.getDefaultLocalCandidate().getCandidate();
					RtcpAttribute rtcpAttribute = new RtcpAttribute(defaultRtcpCandidate, "IN");
					mediaStream.addAttribute(rtcpAttribute);
				}
				
				// XXX Bandwidth - Default values for G711 RTP/AVP profile
//				mediaStream.setBandwidth("AS", 64);
//				mediaStream.setBandwidth("RS", 800);
//				mediaStream.setBandwidth("RR", 2400);
			} else {
				// XXX RS and RR attributes not supported on firefox atm
				// Reduce available RTCP bandwidth to zero
				mediaStream.setBandwidth("RS", 0);
				mediaStream.setBandwidth("RR", 0);
			}
		}
		return sessionDescription;
	}

	private static void addCandidates(IceComponent component, MediaDescription media) throws SdpException {
		for (LocalCandidateWrapper candidate : component.getLocalCandidates()) {
			addCandidate(media, candidate);
		}
	}

	private static void addCandidate(MediaDescription media, LocalCandidateWrapper candidate) throws SdpException {
		CandidateAttribute candidateAttribute = new CandidateAttribute(candidate.getCandidate());
		media.addAttribute(candidateAttribute);
	}

}
