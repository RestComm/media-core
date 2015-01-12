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
package org.mobicents.media.server.impl.rtp.sdp;

import java.util.List;

import org.mobicents.media.io.ice.IceCandidate;
import org.mobicents.media.io.ice.LocalCandidateWrapper;
import org.mobicents.media.server.impl.rtp.channels.AudioChannel;
import org.mobicents.media.server.impl.rtp.channels.MediaChannel;
import org.mobicents.media.server.io.sdp.MediaProfile;
import org.mobicents.media.server.io.sdp.SessionDescription;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.attributes.FormatParameterAttribute;
import org.mobicents.media.server.io.sdp.attributes.PacketTimeAttribute;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.mobicents.media.server.io.sdp.attributes.SsrcAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.SetupAttribute;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.OriginField;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;
import org.mobicents.media.server.io.sdp.fields.TimingField;
import org.mobicents.media.server.io.sdp.fields.VersionField;
import org.mobicents.media.server.io.sdp.ice.attributes.CandidateAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceLiteAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpMuxAttribute;
import org.mobicents.media.server.spi.format.AudioFormat;

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
	 * @param localAddress
	 *            The local address of the media server.
	 * @param externalAddress
	 *            The public address of the media server.
	 * @param channels
	 *            The media channels to be included in the session description.
	 * @return The Session Description object.
	 */
	public static SessionDescription buildSdp(String localAddress, String externalAddress, MediaChannel... channels) {
		// Session-level fields
		SessionDescription sd = new SessionDescription();
		sd.setVersion(new VersionField((short) 0));
		String originAddress = (externalAddress == null || externalAddress.isEmpty()) ? localAddress : externalAddress;
		sd.setOrigin(new OriginField("-", String.valueOf(System.currentTimeMillis()), "1", "IN", "IP4", originAddress));
		sd.setSessionName(new SessionNameField("Mobicents Media Server"));
		sd.setConnection(new ConnectionField("IN", "IP4", localAddress));
		sd.setTiming(new TimingField(0, 0));
		
		// Media Descriptions
		boolean ice = false;
		for (MediaChannel channel : channels) {
			MediaDescriptionField md = buildMediaDescription(channel);
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
		rejected.setProtocol(media.containsDtls() ? MediaProfile.RTP_SAVPF : MediaProfile.RTP_AVP);
		
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
	public static MediaDescriptionField buildMediaDescription(MediaChannel channel) {
		MediaDescriptionField md = new MediaDescriptionField();
		
		md.setMedia(channel.getMediaType());
		md.setPort(channel.getRtpPort());
		md.setProtocol(channel.isDtlsEnabled() ? MediaProfile.RTP_SAVPF : MediaProfile.RTP_AVP);
		md.setConnection(new ConnectionField("IN", "IP4", channel.getRtpAddress()));
		md.setPtime(new PacketTimeAttribute(20));
		md.setRtcp(new RtcpAttribute(channel.getRtcpPort(), "IN", "IP4", channel.getRtcpAddress()));
		if (channel.isRtcpMux()) {
			md.setRtcpMux(new RtcpMuxAttribute());
		}
		
		// ICE attributes
		if (channel.isIceEnabled()) {
			md.setIceUfrag(new IceUfragAttribute(channel.getIceUfrag()));
			md.setIcePwd(new IcePwdAttribute(channel.getIcePwd()));

			List<LocalCandidateWrapper> rtpCandidates = channel.getRtpCandidates();
			if(!rtpCandidates.isEmpty()) {
				// Fix connection address based on default candidate
				IceCandidate defaultCandidate = channel.getDefaultRtpCandidate().getCandidate();
				md.getConnection().setAddress(defaultCandidate.getHostString());
				md.setPort(defaultCandidate.getPort());
				
				// Fix RTCP if rtcp-mux is used
				if(channel.isRtcpMux()) {
					md.getRtcp().setAddress(defaultCandidate.getHostString());
					md.getRtcp().setPort(defaultCandidate.getPort());
				}
				
				// Add candidates list for ICE negotiation
				for (LocalCandidateWrapper candidate : rtpCandidates) {
					md.addCandidate(processCandidate(candidate.getCandidate()));
				}
			}
			
			if (!channel.isRtcpMux()) {
				List<LocalCandidateWrapper> rtcpCandidates = channel.getRtcpCandidates();
				
				if(!rtcpCandidates.isEmpty()) {
					// Fix RTCP based on default RTCP candidate
					IceCandidate defaultCandidate = channel.getDefaultRtcpCandidate().getCandidate();
					md.getRtcp().setAddress(defaultCandidate.getHostString());
					md.getRtcp().setPort(defaultCandidate.getPort());
					
					// Add candidates list for ICE negotiation
					for (LocalCandidateWrapper candidate : rtcpCandidates) {
						md.addCandidate(processCandidate(candidate.getCandidate()));
					}
				}
			}
		}

		// Media formats
		RTPFormats negotiatedFormats = channel.getFormats();
		negotiatedFormats.rewind();
		while (negotiatedFormats.hasMore()) {
			RTPFormat f = negotiatedFormats.next();
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
			md.setSetup(new SetupAttribute(SetupAttribute.PASSIVE));
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

}
