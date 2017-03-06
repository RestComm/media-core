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
package org.restcomm.media.rtp.sdp;

import org.mobicents.media.server.spi.format.AudioFormat;
import org.restcomm.media.ice.IceCandidate;
import org.restcomm.media.ice.IceComponent;
import org.restcomm.media.rtp.channels.AudioChannel;
import org.restcomm.media.rtp.channels.MediaChannel;
import org.restcomm.media.sdp.MediaProfile;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.attributes.ConnectionModeAttribute;
import org.restcomm.media.sdp.attributes.FormatParameterAttribute;
import org.restcomm.media.sdp.attributes.PacketTimeAttribute;
import org.restcomm.media.sdp.attributes.RtpMapAttribute;
import org.restcomm.media.sdp.attributes.SsrcAttribute;
import org.restcomm.media.sdp.dtls.attributes.FingerprintAttribute;
import org.restcomm.media.sdp.dtls.attributes.SetupAttribute;
import org.restcomm.media.sdp.fields.ConnectionField;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.fields.OriginField;
import org.restcomm.media.sdp.fields.SessionNameField;
import org.restcomm.media.sdp.fields.TimingField;
import org.restcomm.media.sdp.fields.VersionField;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.ice.attributes.CandidateAttribute;
import org.restcomm.media.sdp.ice.attributes.IceLiteAttribute;
import org.restcomm.media.sdp.ice.attributes.IcePwdAttribute;
import org.restcomm.media.sdp.ice.attributes.IceUfragAttribute;
import org.restcomm.media.sdp.rtcp.attributes.RtcpAttribute;
import org.restcomm.media.sdp.rtcp.attributes.RtcpMuxAttribute;

/**
 * Factory that produces SDP offers and answers.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SdpFactory {
	
    /**
     * Builds a Session Description object to be sent to a remote peer.
     * 
     * @param offer if the SDP is for an answer or answer.
     * @param localAddress The local address of the media server.
     * @param externalAddress The public address of the media server.
     * @param channels The media channels to be included in the session description.
     * @return The Session Description object.
     */
	public static SessionDescription buildSdp(boolean offer, String localAddress, String externalAddress, MediaChannel... channels) {
		// Session-level fields
		SessionDescription sd = new SessionDescription();
		sd.setVersion(new VersionField((short) 0));
		String originAddress = (externalAddress == null || externalAddress.isEmpty()) ? localAddress : externalAddress;
		sd.setOrigin(new OriginField("-", String.valueOf(System.currentTimeMillis()), "1", "IN", "IP4", originAddress));
		sd.setSessionName(new SessionNameField("Mobicents Media Server"));
        sd.setConnection(new ConnectionField("IN", "IP4", originAddress));
		sd.setTiming(new TimingField(0, 0));
		
		// Media Descriptions
		boolean ice = false;
		for (MediaChannel channel : channels) {
			MediaDescriptionField md = buildMediaDescription(channel, offer);
			md.setSession(sd);
			sd.addMediaDescription(md);
			
			if(md.containsIce()) {
				// Fix session-level attribute
				sd.getConnection().setAddress(md.getConnection().getAddress());
				ice = true;
			}
		}
		
		// Session-level ICE
		if(ice) {
			sd.setIceLite(new IceLiteAttribute());
		}
		return sd;
	}
	
	/**
	 * Rejects a media description from an SDP offer.
	 * 
	 * @param answer
	 *            The SDP answer to include the rejected media
	 * @param media
	 *            The offered media description to be rejected
	 */
	public static void rejectMediaField(SessionDescription answer, MediaDescriptionField media) {
		MediaDescriptionField rejected = new MediaDescriptionField();
		rejected.setMedia(media.getMedia());
		rejected.setPort(0);
		rejected.setProtocol(media.getProtocol());
		rejected.setPayloadTypes(media.getPayloadTypes());
		
		rejected.setSession(answer);
		answer.addMediaDescription(rejected);
	}
	
	/**
	 * Build an SDP description for a media channel.
	 * 
	 * @param channel
	 *            The channel to read information from
	 * @return The SDP media description
	 */
	public static MediaDescriptionField buildMediaDescription(MediaChannel channel, boolean offer) {
		MediaDescriptionField md = new MediaDescriptionField();
		
		md.setMedia(channel.getMediaType());
		md.setPort(channel.getRtpPort());
		MediaProfile profile = channel.isDtlsEnabled() ? MediaProfile.RTP_SAVPF : MediaProfile.RTP_AVP;
		md.setProtocol(profile.getProfile());
        final String externalAddress = channel.getExternalAddress() == null || channel.getExternalAddress().isEmpty() ? null : channel.getExternalAddress();
        md.setConnection(new ConnectionField("IN", "IP4", externalAddress != null ? externalAddress : channel.getRtpAddress()));
		md.setPtime(new PacketTimeAttribute(20));
        md.setRtcp(new RtcpAttribute(channel.getRtcpPort(), "IN", "IP4", externalAddress != null ? externalAddress : channel.getRtcpAddress()));
		if (channel.isRtcpMux()) {
			md.setRtcpMux(new RtcpMuxAttribute());
		}
		
		// ICE attributes
		if (channel.isIceEnabled()) {
			md.setIceUfrag(new IceUfragAttribute(channel.getIceUfrag()));
			md.setIcePwd(new IcePwdAttribute(channel.getIcePwd()));
			
			// Fix connection address based on default (only) candidate
            md.getConnection().setAddress(externalAddress != null ? externalAddress : channel.getRtpAddress());
			md.setPort(channel.getRtpPort());
			
			// Fix RTCP if rtcp-mux is used
			if(channel.isRtcpMux()) {
                md.getRtcp().setAddress(externalAddress != null ? externalAddress : channel.getRtpAddress());
			    md.getRtcp().setPort(channel.getRtpPort());
			}
			
			// Add HOST candidate
			md.addCandidate(processHostCandidate(channel, IceComponent.RTP_ID));
			if(!channel.isRtcpMux()) {
			    md.addCandidate(processHostCandidate(channel, IceComponent.RTCP_ID));
			}
			
			if(channel.getExternalAddress() != null && !channel.getExternalAddress().isEmpty()) {
			    // Add SRFLX candidate
			    md.addCandidate(processSrflxCandidate(channel, IceComponent.RTP_ID));
			    if(!channel.isRtcpMux()) {
			        md.addCandidate(processSrflxCandidate(channel, IceComponent.RTCP_ID));
			    }
			}
			
//			List<LocalCandidateWrapper> rtpCandidates = channel.getRtpCandidates();
//			if(!rtpCandidates.isEmpty()) {
//				// Fix connection address based on default candidate
//				IceCandidate defaultCandidate = channel.getDefaultRtpCandidate().getCandidate();
//				md.getConnection().setAddress(defaultCandidate.getHostString());
//				md.setPort(defaultCandidate.getPort());
//				
//				// Fix RTCP if rtcp-mux is used
//				if(channel.isRtcpMux()) {
//					md.getRtcp().setAddress(defaultCandidate.getHostString());
//					md.getRtcp().setPort(defaultCandidate.getPort());
//				}
//				
//				// Add candidates list for ICE negotiation
//				for (LocalCandidateWrapper candidate : rtpCandidates) {
//					md.addCandidate(processCandidate(candidate.getCandidate()));
//				}
//			}
			
//			if (!channel.isRtcpMux()) {
//				List<LocalCandidateWrapper> rtcpCandidates = channel.getRtcpCandidates();
//				
//				if(!rtcpCandidates.isEmpty()) {
//					// Fix RTCP based on default RTCP candidate
//					IceCandidate defaultCandidate = channel.getDefaultRtcpCandidate().getCandidate();
//					md.getRtcp().setAddress(defaultCandidate.getHostString());
//					md.getRtcp().setPort(defaultCandidate.getPort());
//					
//					// Add candidates list for ICE negotiation
//					for (LocalCandidateWrapper candidate : rtcpCandidates) {
//						md.addCandidate(processCandidate(candidate.getCandidate()));
//					}
//				}
//			}
		}

		// Media formats
		RTPFormat[] negotiatedFormats = channel.getFormats().toArray();
		for (int index = 0; index < negotiatedFormats.length; index++) {
			RTPFormat f = negotiatedFormats[index];
			// Fixes #61 - MMS SDP offer should offer only 101 telephone-event
			if(offer && AVProfile.isDtmf(f) && !AVProfile.isDefaultDtmf(f)) {
			    continue;
			}
			RtpMapAttribute rtpMap = new RtpMapAttribute();
			rtpMap.setPayloadType(f.getID());
			rtpMap.setCodec(f.getFormat().getName().toString());
			rtpMap.setClockRate(f.getClockRate());
			
			switch (channel.getMediaType()) {
			case AudioChannel.MEDIA_TYPE:
				AudioFormat audioFormat = (AudioFormat) f.getFormat();

				if (audioFormat.getChannels() > 1) {
					rtpMap.setCodecParams(audioFormat.getChannels());
				}
				
				if (audioFormat.getOptions() != null) {
					rtpMap.setParameters(new FormatParameterAttribute(f.getID(), audioFormat.getOptions().toString()));
				}
				break;

			default:
				throw new IllegalArgumentException("Media type " + channel.getMediaType() + " not supported.");
			}
			
			md.addPayloadType(f.getID());
			md.addFormat(rtpMap);
		}

		// DTLS attributes
		if (channel.isDtlsEnabled()) {
			md.setSetup(new SetupAttribute(offer ? SetupAttribute.ACTPASS : SetupAttribute.PASSIVE));
			String fingerprint = channel.getDtlsFingerprint();
			int whitespace = fingerprint.indexOf(" ");
			String fingerprintHash = fingerprint.substring(0, whitespace);
			String fingerprintValue = fingerprint.substring(whitespace + 1);
			md.setFingerprint(new FingerprintAttribute(fingerprintHash, fingerprintValue));
		}
		
		md.setConnectionMode(new ConnectionModeAttribute(ConnectionModeAttribute.SENDRECV));
		SsrcAttribute ssrcAttribute = new SsrcAttribute(Long.toString(channel.getSsrc()));
		ssrcAttribute.addAttribute("cname", channel.getCname());
		md.setSsrc(ssrcAttribute);
		
		return md;
	}
	
	private static CandidateAttribute processCandidate(IceCandidate candidate) {
		CandidateAttribute candidateSdp = new CandidateAttribute();
		candidateSdp.setFoundation(candidate.getFoundation());
		candidateSdp.setComponentId(candidate.getComponentId());
		candidateSdp.setProtocol(candidate.getProtocol().getDescription());
		candidateSdp.setPriority(candidate.getPriority());
		candidateSdp.setAddress(candidate.getHostString());
		candidateSdp.setPort(candidate.getPort());
		String candidateType = candidate.getType().getDescription();
		candidateSdp.setCandidateType(candidateType);
		if (CandidateAttribute.TYP_HOST != candidateType) {
			candidateSdp.setRelatedAddress(candidate.getBase().getHostString());
			candidateSdp.setRelatedPort(candidate.getBase().getPort());
		}
		candidateSdp.setGeneration(0);
		return candidateSdp;
	}
	
	private static CandidateAttribute processHostCandidate(MediaChannel candidate, short componentId) {
	       CandidateAttribute candidateSdp = new CandidateAttribute();
	        candidateSdp.setFoundation("11111111");
	        candidateSdp.setComponentId(componentId);
	        candidateSdp.setProtocol("udp");
	        candidateSdp.setPriority(1L);
	        switch (componentId) {
                case IceComponent.RTP_ID:
                    candidateSdp.setAddress(candidate.getRtpAddress());
                    candidateSdp.setPort(candidate.getRtpPort());
                    break;
                    
                case IceComponent.RTCP_ID:
                    candidateSdp.setAddress(candidate.getRtcpAddress());
                    candidateSdp.setPort(candidate.getRtcpPort());
                    break;

                default:
                    break;
            }
	        candidateSdp.setCandidateType(CandidateAttribute.TYP_HOST);
	        candidateSdp.setGeneration(0);
	        return candidateSdp;
	}

	private static CandidateAttribute processSrflxCandidate(MediaChannel candidate, short componentId) {
	    CandidateAttribute candidateSdp = processHostCandidate(candidate, componentId);
	    candidateSdp.setCandidateType(CandidateAttribute.TYP_SRFLX);
        candidateSdp.setAddress(candidate.getExternalAddress());
        
        switch (componentId) {
            case IceComponent.RTP_ID:
                candidateSdp.setRelatedAddress(candidate.getRtpAddress());
                candidateSdp.setRelatedPort(candidate.getRtpPort());
                break;
                
            case IceComponent.RTCP_ID:
                candidateSdp.setRelatedPort(candidate.getRtcpPort());
                candidateSdp.setRelatedPort(candidate.getRtcpPort());
                break;

            default:
                break;
        }
	    
	    return candidateSdp;
	}

}
