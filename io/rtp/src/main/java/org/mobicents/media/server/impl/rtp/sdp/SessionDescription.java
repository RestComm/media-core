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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.mobicents.media.server.utils.Text;

/**
 * Session Descriptor.
 *
 * @author kulikov
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @deprecated use new /io/sdp library
 */
public class SessionDescription {
	
    private Text version;
    private OriginField origin;
    private Text session;
    private ConnectionField connection;
    private TimeField time;
    
    /* MEDIA */
    protected final static Text RTPMAP = new Text("a=rtpmap");
    protected final static Text FMTP = new Text("a=fmtp");
    protected final static Text AUDIO = new Text("audio");
    protected final static Text VIDEO = new Text("video");

    private ArrayList<MediaDescriptorField> mds = new ArrayList<MediaDescriptorField>(3);
    private MediaDescriptorField md;

    private MediaDescriptorField audioDescriptor;
    private MediaDescriptorField videoDescriptor;
    private MediaDescriptorField applicationDescriptor;
    
    /* ICE */
    protected static final Text ICE_UFRAG = new Text("a=ice-ufrag");
    protected static final Text ICE_PWD = new Text("a=ice-pwd");
    protected static final Text ICE_CANDIDATE = CandidateField.CANDIDATE_FIELD;
    
    private boolean ice = false;
    
    /* WebRTC */
    protected final static Text WEBRTC_FINGERPRINT = new Text("a=fingerprint");

    private Text fingerprint;
    
    public SessionDescription() {
    	this.origin = new OriginField();
    	this.connection = new ConnectionField();
    	this.time = new TimeField();
	}

    /**
     * Reads descriptor from binary data
     * @param data the binary data
     * @throws ParseException
     */
    public void parse(byte[] data) throws ParseException {
        Text text = new Text();
        text.strain(data, 0, data.length);
        init(text);
    }

    public void init(Text text) throws ParseException {
        //clean previous data
        reset();
        
        while (text.hasMoreLines()) {
            Text line = text.nextLine();
            if (line.length() == 0) continue;
            switch (line.charAt(0)) {
                case 'v':
                    Iterator<Text> it = line.split('=').iterator();
                    it.next();

                    version = it.next();
                    version.trim();
                    break;
                case 'o':
                    origin.strain(line);
                    break;
                case 's':
                    it = line.split('=').iterator();
                    it.next();
                    
                    session = it.next();
                    session.trim();
                    break;
                case 'c':
                    if (md == null) {
                        connection.strain(line);
                    } else {
                        md.setConnection(line);
                    }
                    break;
                case 't':
                    time.strain(line);
                    break;
                case 'm':
                	
                    md = new MediaDescriptorField();
                    mds.add(md);
                    md.setDescriptor(line);
                    
                    MediaType mediaType = MediaType.fromDescription(md.getMediaType());
                    if (mediaType != null) {
                    	switch (mediaType) {
    					case AUDIO:
    						this.audioDescriptor = md;
    						break;
    					case VIDEO:
    						this.videoDescriptor = md;
    						break;
    					case APPLICATION:
    						this.applicationDescriptor = md;
    						break;
    					default:
    						break;
    					}
                    }
                    break;
                case 'a':
                	if (md != null) {
                		md.addAttribute(line);
                	} else {
                    	// Identify ICE usage
                    	if(!this.ice) {
                    		this.ice = isIceAttribute(line);
                    	}
                    	
                        if (line.startsWith(WEBRTC_FINGERPRINT)) {
                        	addFingerprintAttribute(line);
                        	break;
                        }
                	}
                	break;
            }
        }
    }
    
    /**
     * Gets version attribute.
     *
     * @return the value of version attribute
     */
    public Text getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = new Text(version);
    }
    
    /**
     * Gets the origin attribute
     *
     * @return origin attribute
     */
    public OriginField getOrigin() {
        return origin;
    }

    public void setOrigin(String name, String sessionID, String sessionVersion,
            String netType, String addressType, String address) {
            this.origin = new OriginField(name, sessionID, sessionVersion,
                    netType, addressType, address);
    }
    
    /**
     * Gets session identifier.
     *
     * @return session identifier value.
     */
    public String getSession() {
        return session.toString();
    }

    /**
     * Gets connection attribute.
     *
     * @return the connection attribute.
     */
    public ConnectionField getConnection() {
        return connection;
    }

    /**
     * Gets the time attribute.
     *
     * @return time attribute.
     */
    public TimeField getTime() {
        return time;
    }
    
    public Text getFingerprint() {
		return fingerprint;
	}
    
    public Text getFingerprint(MediaType mediaType) {
    	if(MediaType.AUDIO.equals(mediaType)) {
    		return getFingerprint(this.audioDescriptor);
    	}
    	return this.fingerprint;
    }
    
    private Text getFingerprint(MediaDescriptorField mediaStream) {
    	Text value = mediaStream.getWebRTCFingerprint();
    	if(value != null && value.length() > 0) {
    		return value;
    	}
    	return this.fingerprint;
    	
    }

    /**
     * Gets the media attributes.
     *
     * @return collection of media attributes.
     */
    public Collection<MediaDescriptorField> getMedia() {
        return mds;
    }

    /**
     * Gets the description of audio stream.
     *
     * @return the descriptor object.
     */
    public MediaDescriptorField getAudioDescriptor() {
        return this.audioDescriptor;
    }

    /**
     * Gets the description of video stream.
     *
     * @return the descriptor object.
     */
    public MediaDescriptorField getVideoDescriptor() {
        return this.videoDescriptor;
    }
    
    /**
     * Gets the description of application stream
     * @return the description object
     */
    public MediaDescriptorField getApplicationDescriptor() {
		return applicationDescriptor;
	}
    
    public boolean hasAudioDescriptor() {
    	return this.audioDescriptor != null;
    }
    
    public boolean hasVideoDescriptor() {
    	return this.videoDescriptor != null;
    }
    
    public boolean hasApplicationDescriptor() {
    	return this.applicationDescriptor != null;
    }
    
    private boolean isIceAttribute(Text line) {
    	return line.startsWith(ICE_UFRAG) || line.startsWith(ICE_PWD) || line.startsWith(ICE_CANDIDATE);
    }
    
    public boolean isIce() {
		return ice;
	}

    public boolean isAudioIce() {
    	return this.ice || this.getAudioDescriptor().isIce();
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
    	// Remove line type 'a=fingerprint:'
    	Text fingerprint = (Text) attribute.subSequence(WEBRTC_FINGERPRINT.length(), attribute.length());
    	this.fingerprint = fingerprint;
    }
    
    public void reset() {
    	this.version = null;
        this.session = null;
        // TODO 
    	this.origin.reset();
    	this.connection.reset();
    	this.time.reset();
        this.mds.clear();
        this.md = null;
        this.audioDescriptor = null;
        this.videoDescriptor = null;
        this.applicationDescriptor = null;
        this.fingerprint = null;
        this.ice = false;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("v=%s\n", version.toString()));
        builder.append(origin.toString());
        return builder.toString();
    }
    
}
