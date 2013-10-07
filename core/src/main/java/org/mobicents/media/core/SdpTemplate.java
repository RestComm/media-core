/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.media.core;

import org.mobicents.media.server.impl.rtp.sdp.MediaDescriptorField;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.spi.format.AudioFormat;

/**
 *
 * @author yulian oifa
 */
public class SdpTemplate {

    private String template;

    private boolean isAudioSupported;
    private boolean isVideoSupported;

    public SdpTemplate(RTPFormats audio, RTPFormats video) {
        StringBuilder builder = new StringBuilder();

        //prepare header part
        writeHeader(builder);

        //prepare media descriptors if required

        if (audio != null && !audio.isEmpty()) {
            this.isAudioSupported = true;
            this.writeAudioDescriptor(builder, audio);
        }

        if (video != null && !video.isEmpty()) {
            this.isVideoSupported = true;
            this.writeVideoDescriptor(builder, video);
        }

        template = builder.toString();
    }

    private void writeHeader(StringBuilder builder) {
        builder.append("v=0\n");
        builder.append("o=- %s 1 IN IP4 %s\n");
        builder.append("s=Mobicents Media Server \n");
        builder.append("c=%s %s %s\n");
        builder.append("t=0 0\n");
    }

    protected String getMediaProfile() {
    	return MediaDescriptorField.RTP_AVP_PROFILE;
    }
    
    private void writeAudioDescriptor(StringBuilder builder, RTPFormats formats) {
        builder.append("m=audio %s ");
        builder.append(getMediaProfile());
        builder.append(" ");
        builder.append(payloads(formats));
        builder.append("\n");        
        formats.rewind();
        while (formats.hasMore()) {
            RTPFormat f = formats.next();
            String rtpmap = null;
            AudioFormat fmt = (AudioFormat) f.getFormat();

            if (fmt.getChannels() == 1) {
                rtpmap = String.format("a=rtpmap:%d %s/%d\n", f.getID(), fmt.getName(), f.getClockRate());
            } else {
                rtpmap = String.format("a=rtpmap:%d %s/%d/%d\n", f.getID(), fmt.getName(), f.getClockRate(), fmt.getChannels());
            }

            builder.append(rtpmap);

            if (f.getFormat().getOptions() != null) {
                builder.append(String.format("a=fmtp:%d %s\n", f.getID(), f.getFormat().getOptions()));
            }
            
            if(f.getFormat().shouldSendPTime())
            	builder.append("a=ptime:20\n");
            
            builder.append(getSdpSessionSetupAttribute());
        };
        builder.append(getExtendedAudioAttributes());
    }

    /**
     * 
     * Intended for subclasses of SdpTemplate
     * 
     * @return any additional attributes for the audio SDP part 
     */
    protected String getExtendedAudioAttributes() {
		return "";
	}

	/**
     * 
     * Mobicents Media Server is typically installed on a server with IP address, 
     * which is reachable by remote UAs that may be behind NAT
     * Remote UAs are expected to be in setup:active mode according to multiple related RFCs:
     * http://tools.ietf.org/html/rfc4145#section-4
     * http://tools.ietf.org/html/rfc6135#section-4.2.2
     * http://tools.ietf.org/html/rfc5763#section-5
     * The Media Server being in passive mode automagically solves NAT without the UA using ICE (STUN, TURN)
     * However this approach can be problematic for the use of early media
     * http://tools.ietf.org/html/rfc5763#section-6.2
     * 
     * @return the "a=setup" attribute value that the Media Server will use  
     */
    private String getSdpSessionSetupAttribute() {
    	return "a=setup:passive\n";
    }
    
    
    /**
     * 
     * TODO: Video support is work in progress.
     * 
     * @param builder
     * @param formats
     */
    private void writeVideoDescriptor(StringBuilder builder, RTPFormats formats) {
        builder.append("m=video %s RTP/AVP ");
        builder.append(payloads(formats));
        builder.append("\n");
        
        formats.rewind();
        while(formats.hasMore()) {
            RTPFormat f = formats.next();
            builder.append(String.format("a=rtpmap:%d %s/%d\n", f.getID(), f.getFormat().getName(), f.getClockRate()));
            if (f.getFormat().getOptions() != null) {
                builder.append(String.format("a=fmtp: %d %s\n", f.getID(), f.getFormat().getOptions().toString()));
            }
        }
    }

    /**
     * List of payloads.
     *
     * @param formats the RTP format objects.
     * @return the string which with payload numbers
     */
    private String payloads(RTPFormats formats) {
        StringBuilder builder = new StringBuilder();        
        formats.rewind();
        while (formats.hasMore()) {
            RTPFormat f = formats.next();
            builder.append(f.getID());
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    public String getSDP(String bindAddress, String netwType, String addressType, String address, int audioPort, int videoPort) {
        if (this.isAudioSupported && !this.isVideoSupported) {
            return String.format(template, System.currentTimeMillis(), bindAddress, netwType, addressType, address, audioPort);
        } else if (!this.isAudioSupported && this.isVideoSupported) {
            return String.format(template, System.currentTimeMillis(), bindAddress, netwType, addressType, address, videoPort);
        } else if (this.isAudioSupported && this.isVideoSupported) {
            return String.format(template, System.currentTimeMillis(), bindAddress, netwType, addressType, address, audioPort, videoPort);
        }
        return String.format(template, System.currentTimeMillis(), bindAddress, netwType, addressType, address);
    }
}
