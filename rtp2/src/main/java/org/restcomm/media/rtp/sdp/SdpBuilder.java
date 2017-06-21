/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.rtp.sdp;

import java.net.InetSocketAddress;

import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.sdp.MediaProfile;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.attributes.ConnectionModeAttribute;
import org.restcomm.media.sdp.attributes.FormatParameterAttribute;
import org.restcomm.media.sdp.attributes.PacketTimeAttribute;
import org.restcomm.media.sdp.attributes.RtpMapAttribute;
import org.restcomm.media.sdp.attributes.SsrcAttribute;
import org.restcomm.media.sdp.fields.ConnectionField;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.fields.OriginField;
import org.restcomm.media.sdp.fields.SessionNameField;
import org.restcomm.media.sdp.fields.TimingField;
import org.restcomm.media.sdp.fields.VersionField;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.format.AudioFormat;

/**
 * Produces SDP offers and answers.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SdpBuilder {

    static final String ORIGINATOR_NAME = "RestComm Media Server";
    static final short VERSION = 0;
    static final String NET_TYPE = "IN";
    static final String ADDRESS_TYPE = "IP4";
    static final int PACKETIZATION_TIME = 20;
    static final int NULL_PORT = 0;

    public SessionDescription buildSessionDescription(boolean offer, String cname, String localAddress, String externalAddress, RtpSession... sessions) {
        final String sessionId = String.valueOf(System.currentTimeMillis());
        final String originAddress = resolveOrigin(localAddress, externalAddress);

        // Session Description
        SessionDescription sdp = new SessionDescription();
        sdp.setVersion(new VersionField());
        sdp.setOrigin(new OriginField(sessionId, originAddress));
        sdp.setSessionName(new SessionNameField(ORIGINATOR_NAME));
        sdp.setConnection(new ConnectionField(NET_TYPE, ADDRESS_TYPE, originAddress));
        sdp.setTiming(new TimingField());

        // Media Description
        // TODO check support for ICE
        
        for (RtpSession session : sessions) {
            MediaDescriptionField md = buildMediaDescription(cname, externalAddress, session, offer);
            md.setSession(sdp);
            sdp.addMediaDescription(md);
            
            // TODO ICE Control
            // if(md.containsIce()) {
            // // Fix session-level attribute
            // sd.getConnection().setAddress(md.getConnection().getAddress());
            // ice = true;
            // }
        }
        
        // TODO Session-level ICE
        // if(ice) {
        // sd.setIceLite(new IceLiteAttribute());
        // }

        return sdp;
    }

    private MediaDescriptionField buildMediaDescription(String cname, String externalAddress, RtpSession session, boolean offer) {
        final String mediaType = session.getMediaType().name().toLowerCase();
        final ConnectionMode mode = session.getMode();
        final long ssrc = session.getSsrc();
        final InetSocketAddress rtpAddress = (InetSocketAddress) session.getRtpAddress();

        MediaDescriptionField md = new MediaDescriptionField();

        md.setMedia(mediaType);
        md.setPort(rtpAddress.getPort());
        md.setProtocol(MediaProfile.RTP_AVP.getProfile());
        md.setConnection(new ConnectionField(NET_TYPE, ADDRESS_TYPE, resolveOrigin(rtpAddress.getHostString(), externalAddress)));
        md.setPtime(new PacketTimeAttribute(PACKETIZATION_TIME));
        // TODO set RTCP details
        // TODO set RTCP-MUX details
        // TODO set ICE details

        // Media Formats
        final RTPFormat[] formats = session.getSupportedFormats().toArray();
        for (RTPFormat format : formats) {
            // Fixes #61 - SDP offer should offer only 101 telephone-event
            if (offer && AVProfile.isDtmf(format) && !AVProfile.isDefaultDtmf(format)) {
                continue;
            }

            RtpMapAttribute rtpMap = new RtpMapAttribute();
            rtpMap.setPayloadType(format.getID());
            rtpMap.setCodec(format.getFormat().getName().toString());
            rtpMap.setClockRate(format.getClockRate());

            switch (session.getMediaType()) {
                case AUDIO:
                    AudioFormat audioFormat = (AudioFormat) format.getFormat();

                    if (audioFormat.getChannels() > 1) {
                        rtpMap.setCodecParams(audioFormat.getChannels());
                    }

                    if (audioFormat.getOptions() != null) {
                        FormatParameterAttribute formatParameter = new FormatParameterAttribute(format.getID(), audioFormat.getOptions().toString());
                        rtpMap.setParameters(formatParameter);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Media type " + mediaType + " not supported.");
            }

            md.addPayloadType(format.getID());
            md.addFormat(rtpMap);
        }

        // TODO Add DTLS attributes

        md.setConnectionMode(new ConnectionModeAttribute(mode.description()));
        SsrcAttribute ssrcAttribute = new SsrcAttribute(String.valueOf(ssrc));
        ssrcAttribute.addAttribute(SsrcAttribute.CNAME, cname);
        md.setSsrc(ssrcAttribute);

        return md;
    }
    
    public void rejectMediaField(SessionDescription answer, MediaDescriptionField media) {
        MediaDescriptionField rejected = new MediaDescriptionField();
        rejected.setMedia(media.getMedia());
        rejected.setPort(NULL_PORT);
        rejected.setProtocol(media.getProtocol());
        rejected.setPayloadTypes(media.getPayloadTypes());
        
        rejected.setSession(answer);
        answer.addMediaDescription(rejected);
    }

    private String resolveOrigin(String localAddress, String externalAddress) {
        if (externalAddress == null || externalAddress.isEmpty()) {
            return localAddress;
        }
        return externalAddress;
    }

}
