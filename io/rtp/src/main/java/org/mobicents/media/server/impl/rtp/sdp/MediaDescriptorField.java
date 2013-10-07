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

package org.mobicents.media.server.impl.rtp.sdp;

import java.text.ParseException;
import java.util.Iterator;

import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.EncodingName;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.VideoFormat;
import org.mobicents.media.server.utils.Text;

/**
 * Media descriptor attribute.
 *
 * @author kulikov
 */
public class MediaDescriptorField {
    private Text mediaType;
    private int port;
    private Text profile;
    private ConnectionField connection;

    private RTPFormats formats = new RTPFormats(15);

    // optional SDP attribute used for WebRTC session encryption
	private Text webRTCFingerprint;    

    // legacy unencrypted RTP media profile
    public final static String RTP_AVP_PROFILE = "RTP/AVP";

    // WebRTC (DTLS SRTP encrypted) RTP media profile
    public final static String RTP_SAVPF_PROFILE = "RTP/SAVPF";

    /**
     * Reads values from specified text line
     * 
     * @param line the text description of the media.
     * @throws ParseException
     */
    protected void setDescriptor(Text line) throws ParseException {
        line.trim();
        try {
            //split using equal sign
            Iterator<Text> it = line.split('=').iterator();

            //skip first token (m)
            Text t = it.next();

            //select second token (media_type port profile formats)
            t = it.next();

            //split using white spaces
            it = t.split(' ').iterator();

            //media type
            mediaType = it.next();
            mediaType.trim();

            //port
            t = it.next();
            t.trim();
            port = t.toInteger();

            //profile
            profile = it.next();
            profile.trim();

            //formats
            while (it.hasNext()) {
                t = it.next();
                t.trim();

                RTPFormat fmt = AVProfile.getFormat(t.toInteger(),mediaType);
                if (fmt != null && !formats.contains(fmt.getFormat())) {
                    formats.add(fmt.clone());
                }
            }
        } catch (Exception e) {
            throw new ParseException("Could not parse media descriptor", 0);
        }
    }

    /**
     * Parses attribute.
     *
     * @param attribute the attribute to parse
     */
    protected void addAttribute(Text attribute) {
        Iterator<Text> it = attribute.split(':').iterator();
        Text token = it.next();

        if (token.equals(SessionDescription.RTPMAP)) {
            token = it.next();
            token.trim();

            //payload and format descriptor
            it = token.split(' ').iterator();

            //payload
            token = it.next();
            token.trim();

            int payload = token.toInteger();

            //format descriptor
            token = it.next();
            token.trim();

            createFormat(payload, token);
            return;
        }

        if (token.equals(SessionDescription.FMTP)) {
            token = it.next();
            token.trim();

            //payload and format descriptor
            it = token.split(' ').iterator();

            //payload
            token = it.next();
            token.trim();

            int payload = token.toInteger();

            //format descriptor
            token = it.next();
            token.trim();

            RTPFormat fmt = getFormat(payload);
            if (fmt != null) {
                //TODO : replace string with text
                fmt.getFormat().setOptions(token);
            }
            
            if (token.equals(SessionDescription.WEBRTC_FINGERPRINT)) {
                token = it.next();
                token.trim();
            	setWebRTCFingerprint(token);
            }

            return;
        }
    }

    /**
     * Gets the connection attribute of this description.
     * 
     * @return connection field
     */
    public ConnectionField getConnection() {
        return this.connection;
    }
    
    /**
     * Modify connection attribute.
     * 
     * @param line the text view of attribute.
     * @throws ParseException
     */
    protected void setConnection(Text line) throws ParseException {
        connection = new ConnectionField();
        connection.strain(line);
    }

    /**
     * Gets the media types
     *
     * @return media type value
     */
    public Text getMediaType() {
        return mediaType;
    }

    /**
     * Gets the port number.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the profile
     *
     * @return profile value
     */
    public Text getProfile() {
        return profile;
    }

    /**
     * Gets the list of formats offered.
     *
     * @return collection of formats.
     */
    public RTPFormats getFormats() {
        return formats;
    }

    /**
     * Searches format with specified payload number.
     *
     * @param payload payload number.
     * @return format with this payload number.
     */
    private RTPFormat getFormat(int payload) {
        return formats.find(payload);
    }

    /**
     * Creates or updates format using payload number and text format description.
     *
     * @param payload the payload number of the format.
     * @param description text description of the format
     * @return format object
     */
    private RTPFormat createFormat(int payload, Text description) {
        if (mediaType.equals(SessionDescription.AUDIO)) {
            return createAudioFormat(payload, description);
        } else if (mediaType.equals(SessionDescription.VIDEO)) {
            return createVideoFormat(payload, description);
        } else {
            return null;
        }
    }

    /**
     * Creates or updates audio format using payload number and text format description.
     *
     * @param payload the payload number of the format.
     * @param description text description of the format
     * @return format object
     */
    private RTPFormat createAudioFormat(int payload, Text description) {
        Iterator<Text> it = description.split('/').iterator();

        //encoding name
        Text token = it.next();
        token.trim();
        EncodingName name = new EncodingName(token);

        //clock rate
        //TODO : convert to sample rate
        token = it.next();
        token.trim();
        int clockRate = token.toInteger();

        //channels
        int channels = 1;
        if (it.hasNext()) {
            token = it.next();
            token.trim();
            channels = token.toInteger();
        }

        RTPFormat rtpFormat = getFormat(payload);
        if (rtpFormat == null) {
            formats.add(new RTPFormat(payload, FormatFactory.createAudioFormat(name, clockRate, -1, channels)));
        } else {
            //TODO: recreate format anyway. it is illegal to use clock rate as sample rate
            ((AudioFormat)rtpFormat.getFormat()).setName(name);
            ((AudioFormat)rtpFormat.getFormat()).setSampleRate(clockRate);
            ((AudioFormat)rtpFormat.getFormat()).setChannels(channels);
        }

        return rtpFormat;
    }

    /**
     * Creates or updates video format using payload number and text format description.
     *
     * @param payload the payload number of the format.
     * @param description text description of the format
     * @return format object
     */
    private RTPFormat createVideoFormat(int payload, Text description) {
        Iterator<Text> it = description.split('/').iterator();

        //encoding name
        Text token = it.next();
        token.trim();
        EncodingName name = new EncodingName(token);

        //clock rate
        //TODO : convert to frame rate
        token = it.next();
        token.trim();
        int clockRate = token.toInteger();

        RTPFormat rtpFormat = getFormat(payload);
        if (rtpFormat == null) {
            formats.add(new RTPFormat(payload, FormatFactory.createVideoFormat(name, clockRate)));
        } else {
            //TODO: recreate format anyway. it is illegal to use clock rate as frame rate
            ((VideoFormat)rtpFormat.getFormat()).setName(name);
            ((VideoFormat)rtpFormat.getFormat()).setFrameRate(clockRate);
        }

        return rtpFormat;
    }

    
    /**
     * 
     * @return true if the media profile requires encryption
     */
	public boolean isWebRTCProfile() {
		boolean isEcryptionRequired = getProfile().equals(RTP_SAVPF_PROFILE);
		return isEcryptionRequired;
	}

	public Text getWebRTCFingerprint() {
		return webRTCFingerprint;
	}

	public void setWebRTCFingerprint(Text webRTCFingerprint) {
		this.webRTCFingerprint = webRTCFingerprint;
	}

}
