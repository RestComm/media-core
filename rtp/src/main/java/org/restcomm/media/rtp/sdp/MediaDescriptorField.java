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

package org.restcomm.media.rtp.sdp;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.mobicents.media.server.spi.format.ApplicationFormat;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.EncodingName;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.VideoFormat;
import org.mobicents.media.server.utils.Text;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;

/**
 * Media descriptor attribute.
 *
 * @author kulikov
 * 
 * @deprecated use new /io/sdp library
 */
public class MediaDescriptorField {
    private Text mediaType;
    private int port;
    private Text profile;
    private ConnectionField connection;
    private boolean rtcpMux = false;
    private boolean ice = false;
    private Text iceUfrag;
    private Text icePwd;
    private List<CandidateField> candidates = new ArrayList<CandidateField>();

    private RTPFormats formats = new RTPFormats(15);

    // optional SDP attribute used for WebRTC session encryption
	private Text webRTCFingerprint;

    // legacy unencrypted RTP media profile
    public final static String RTP_AVP_PROFILE = "RTP/AVP";

    // legacy encrypted SRTP media profile 
    public final static String RTP_SAVP_PROFILE = "RTP/SAVP";

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
        if (attribute.startsWith(SessionDescription.RTPMAP)) {
            addRtpMapAttribute(attribute);
            return;
        }

        if (attribute.startsWith(SessionDescription.FMTP)) {
        	addFmtAttribute(attribute);
            return;
        }
        
        if (attribute.startsWith(SessionDescription.WEBRTC_FINGERPRINT)) {
        	addFingerprintAttribute(attribute);
        	return;
        }
        
        if(attribute.startsWith(SessionDescription.ICE_UFRAG)) {
        	this.ice = true;
        	addIceUfragAttribute(attribute);
        	return;
        }

        if(attribute.startsWith(SessionDescription.ICE_PWD)) {
        	this.ice = true;
        	addIcePwdAttribute(attribute);
        	return;
        }
        
        if(attribute.startsWith(CandidateField.CANDIDATE_FIELD)) {
        	this.ice = true;
        	addCandidate(attribute);
        	return;
        }

        if(attribute.startsWith(RtcpMuxField.RTCP_MUX_FIELD)) {
        	this.rtcpMux = true;
        	return;
        }
    }
    
    /**
     * Parses an ice-ufrag attribute and registers it in the media session.<br>
     * Example: <code>a=ice-ufrag:apy5ZU+12zHYMHGq</code>
     * @param attribute
     */
    private void addIceUfragAttribute(Text attribute) {
		// Copy and trim the attribute
		Text attr = new Text();
		attribute.copy(attr);
		attr.trim();
		
		// Extract the value from the attribute
		Iterator<Text> it = attribute.split(':').iterator();
    	Text token = it.next();
        token = it.next();
        token.trim();
        this.iceUfrag = token;
    }

    /**
     * Parses an ice-pwd attribute and registers it in the media session.<br>
     * Example: <code>a=ice-pwd:BsDycZCv0plnGR+su8E+3kGJ</code>
     * @param attribute
     */
    private void addIcePwdAttribute(Text attribute) {
    	// Copy and trim the attribute
    	Text attr = new Text();
    	attribute.copy(attr);
    	attr.trim();
    	
    	// Extract the value from the attribute
    	Iterator<Text> it = attribute.split(':').iterator();
    	Text token = it.next();
    	token = it.next();
    	token.trim();
    	this.icePwd = token;
    }
    
    /**
     * Parses a candidate field for ICE and register it on internal list.
     * @param attribute
     */
	private void addCandidate(Text attribute) {
		// Copy and trim the attribute
		Text attr = new Text();
		attribute.copy(attr);
		attr.trim();
		
		// Parse the candidate field and add it to list of candidates
		CandidateField candidateField = new CandidateField(attr);
		this.candidates.add(candidateField);
		// Candidates must be listed by weight in descending order
		Collections.sort(this.candidates, Collections.reverseOrder());
	}

	/**
	 * Register a new RTP MAP attribute.
	 * <p>Example: <code>a=rtpmap:126 telephone-event/8000</code></p>
	 * 
	 * @param attribute
	 *            The attribute line to be registered.
	 * @throws IllegalArgumentException
	 *             If the attribute is not a valid RTP MAP line.
	 */
    private void addRtpMapAttribute(Text attribute) throws IllegalArgumentException {
    	if (!attribute.startsWith(SessionDescription.RTPMAP)) {
    		throw new IllegalArgumentException("Not a valid RTP MAP attribute"+attribute);
    	}

    	Iterator<Text> it = attribute.split(':').iterator();
    	Text token = it.next();
    	
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
    }
    
	/**
	 * Register a new FMT attribute.<br>
	 * Example: <code>a=fmtp:111 minptime=10</code>
	 * 
	 * @param attribute
	 *            The attribute line to be registered.
	 * @throws IllegalArgumentException
	 *             If the attribute is not a valid FMT line.
	 */
    private void addFmtAttribute(Text attribute) throws IllegalArgumentException {
    	if (!attribute.startsWith(SessionDescription.FMTP)) {
    		throw new IllegalArgumentException("Not a valid FMT attribute"+attribute);
    	}
    	
    	Iterator<Text> it = attribute.split(':').iterator();
    	Text token = it.next();
        
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
    }
    
	/**
	 * Register a new fingerprint attribute for WebRTC calls.<br>
	 * Example: <code>a=fingerprint:sha-256 E5:52:E5:88:CC:B6:7A:D7:8E:...</code>
	 * 
	 * @param attribute
	 *            The attribute line to be registered.
	 * @throws IllegalArgumentException
	 *             If the attribute is not a valid FMT line.
	 */
    private void addFingerprintAttribute(Text attribute) throws IllegalArgumentException {
    	if (!attribute.startsWith(SessionDescription.WEBRTC_FINGERPRINT)) {
    		throw new IllegalArgumentException("Not a valid fingerprint attribute"+attribute);
    	}

    	// Remove line type 'a=fingerprint:'
    	Text fingerprint = (Text) attribute.subSequence(SessionDescription.WEBRTC_FINGERPRINT.length(),attribute.length());
    	setWebRTCFingerprint(fingerprint);
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
        Collections.sort(this.candidates);
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
	 * Indicates whether the media channel supports RTCP-MUX or not.
	 * 
	 * @return whether rtcp-mux is supported
	 */
    public boolean isRtcpMux() {
		return rtcpMux;
	}

    /**
     * Creates or updates format using payload number and text format description.
     *
     * @param payload the payload number of the format.
     * @param description text description of the format
     * @return format object
     */
    private RTPFormat createFormat(int payload, Text description) {
		MediaType mtype = MediaType.fromDescription(mediaType);
		switch (mtype) {
		case AUDIO:
			return createAudioFormat(payload, description);
		case VIDEO:
			return createVideoFormat(payload, description);
		case APPLICATION:
			return createApplicationFormat(payload, description);
		default:
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
     * Creates or updates application format using payload number and text format description.
     *
     * @param payload the payload number of the format.
     * @param description text description of the format
     * @return format object
     */
    private RTPFormat createApplicationFormat(int payload, Text description) {
        Iterator<Text> it = description.split('/').iterator();

        //encoding name
        Text token = it.next();
        token.trim();
        EncodingName name = new EncodingName(token);

        //clock rate
        token = it.next();
        token.trim();

        RTPFormat rtpFormat = getFormat(payload);
        if (rtpFormat == null) {
            formats.add(new RTPFormat(payload, FormatFactory.createApplicationFormat(name)));
        } else {
            ((ApplicationFormat)rtpFormat.getFormat()).setName(name);
        }
        return rtpFormat;
    }

    /**
     * 
     * @return true if the media profile requires encryption
     */
	public boolean isWebRTCProfile() {
		return getProfile().toString().equals(RTP_SAVP_PROFILE) || getProfile().toString().equals(RTP_SAVPF_PROFILE);
	}

	public Text getWebRTCFingerprint() {
		return webRTCFingerprint;
	}

	public void setWebRTCFingerprint(Text webRTCFingerprint) {
		this.webRTCFingerprint = webRTCFingerprint;
	}
	
    public boolean isIce() {
    	return this.ice;
    }
	
	public Text getIceUfrag() {
		return iceUfrag;
	}
	
	public Text getIcePwd() {
		return icePwd;
	}

	public List<CandidateField> getCandidates() {
		return candidates;
	}
	
	public CandidateField getMostRelevantCandidate() {
		if(this.candidates == null || this.candidates.isEmpty()) {
			return null;
		}
		return this.candidates.get(0);
	}
	
}
