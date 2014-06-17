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
import org.mobicents.media.server.impl.rtp.sdp.SdpComparator;
import org.mobicents.media.server.impl.rtp.sdp.SessionDescription;
import org.mobicents.media.server.spi.format.AudioFormat;

/**
 * 
 * @author yulian oifa
 * @author Henrique Rosa
 */
public class SdpTemplate {
	
	private SdpComparator sdpComparator;
	private SessionDescription sessionDescription;

    private String template;
    
    private String bindAddress = "";
    private String connectionAddress = "";
    private String networkType = "";
    private String addressType = "";
    private int audioPort = 0;
    private int videoPort = 0;
    private int applicationPort = 0;
	/*
	 * Supported formats to build the final template.
	 * 
	 * If the format list for a given media is null, then the corresponding
	 * media line (m=) WILL NOT be part of the SDP template.
	 * 
	 * If the format list for a given media is empty, then a corresponding media
	 * line WILL be part of the SDP template with port=0, indicating that the
	 * media type will be rejected.
	 * 
	 * This behavior is important because the number of m= lines must be equal
	 * between SDP offer and answer.
	 */
    private RTPFormats offeredAudioFormats;
    private RTPFormats offeredVideoFormats;
    private RTPFormats offeredApplicationFormats;
    private RTPFormats supportedAudioFormats;
    private RTPFormats supportedVideoFormats;
    private RTPFormats supportedApplicationFormats;
    
    private RTPFormats negotiatedAudioFormats;
    private RTPFormats negotiatedVideoFormats;
    private RTPFormats negotiatedApplicationFormats;
    
    private String headerTemplate = "";
    private String audioTemplate = "";
    private String videoTemplate = "";
    private String applicationTemplate = "";
    
    @Deprecated
    private boolean isAudioSupported;
    @Deprecated
    private boolean isVideoSupported;
    
    
    public SdpTemplate(SessionDescription sessionDescription) {
    	this.sdpComparator = new SdpComparator();
    	this.sessionDescription = sessionDescription;
    	
    	setOfferedAudioFormats(sessionDescription);
    	setOfferedVideoFormats(sessionDescription);
    	setOfferedApplicationFormats(sessionDescription);
    }

    @Deprecated
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
    
    public String build() {
    	StringBuilder builder = new StringBuilder();
    	
    	// Write session-level descriptor
    	this.headerTemplate = writeHeader();
    	builder.append(String.format(this.headerTemplate, System.currentTimeMillis(), this.bindAddress, this.networkType, this.addressType, this.connectionAddress));
    	    	
    	// Write audio descriptor
    	if(this.offeredAudioFormats != null) {
    		this.audioTemplate = writeAudioDescriptor();
    		builder.append(String.format(this.audioTemplate, isAudioSupported() ? this.audioPort : 0));
    	}
    	// Write video descriptor
    	if(this.offeredVideoFormats != null) {
    		this.videoTemplate = writeVideoDescriptor();
    		builder.append(String.format(this.videoTemplate, isVideoSupported() ? this.videoPort : 0));
    	}
    	// write application descriptor
    	if(this.offeredApplicationFormats != null) {
    		this.applicationTemplate = writeApplicationDescriptor();
    		builder.append(String.format(this.applicationTemplate, isApplicationSupported() ? this.applicationPort : 0));
    	}
    	return builder.toString();
    }
    
    protected String getMediaProfile() {
    	return MediaDescriptorField.RTP_AVP_PROFILE;
    }
    
    protected String getApplicationProfile() {
    	if(this.sessionDescription != null) {
    		if(this.sessionDescription.getApplicationDescriptor() != null) {
    			return this.sessionDescription.getApplicationDescriptor().getProfile().toString();
    		}
    	}
    	return "";
    }
    
    public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}
    
    public void setConnectionAddress(String connectionAddress) {
		this.connectionAddress = connectionAddress;
	}
    
    public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}
    
    public void setAddressType(String addressType) {
		this.addressType = addressType;
	}
    
    public void setSupportedAudioFormats(RTPFormats supported) {
    	this.supportedAudioFormats = supported;
    	
    	if(this.offeredAudioFormats != null && !offeredAudioFormats.isEmpty()) {
    		this.negotiatedAudioFormats = this.sdpComparator.negotiateAudio(this.sessionDescription, this.supportedAudioFormats);
    	} else {
    		this.negotiatedAudioFormats = null;
    	}
    }
    
    public void setSupportedApplicationFormats(RTPFormats formats) {
		this.supportedApplicationFormats = formats;
		if (isApplicationSupported()) {
			this.negotiatedApplicationFormats = this.sdpComparator.negotiateApplication(this.sessionDescription, this.supportedApplicationFormats);
		} else {
			this.negotiatedApplicationFormats = null;
		}
	}
    
    public void setSupportedVideoFormats(RTPFormats supportedVideoFormats) {
		this.supportedVideoFormats = supportedVideoFormats;
		if (isVideoSupported()) {
			this.negotiatedVideoFormats = this.sdpComparator.negotiateVideo(this.sessionDescription, this.supportedVideoFormats);
		} else {
			this.negotiatedVideoFormats = null;
		}
	}
    
    private void setOfferedAudioFormats(final SessionDescription sdp) {
    	MediaDescriptorField audioDescriptor = sdp.getAudioDescriptor();
    	if(audioDescriptor != null) {
    		this.offeredAudioFormats = audioDescriptor.getFormats();
    	} else {
    		this.offeredAudioFormats = null;
    	}
    }
    
    private void setOfferedVideoFormats(final SessionDescription sdp) {
    	MediaDescriptorField videoDescriptor = sdp.getVideoDescriptor();
    	if(videoDescriptor != null) {
    		this.offeredVideoFormats = videoDescriptor.getFormats();
    	} else {
    		this.offeredVideoFormats = null;
    	}
    }
    
    private void setOfferedApplicationFormats(final SessionDescription sdp) {
    	MediaDescriptorField applicationDescriptor = sdp.getApplicationDescriptor();
    	if(applicationDescriptor != null) {
    		this.offeredApplicationFormats = applicationDescriptor.getFormats();
    	} else {
    		this.offeredApplicationFormats = null;
    	}
    }
    
    public RTPFormats getNegotiatedAudioFormats() {
		return negotiatedAudioFormats;
	}
    
    public RTPFormats getNegotiatedVideoFormats() {
		return negotiatedVideoFormats;
	}
    
    public RTPFormats getNegotiatedApplicationFormats() {
		return negotiatedApplicationFormats;
	}
    
    public boolean isAudioSupported() {
    	return this.negotiatedAudioFormats != null && !this.negotiatedAudioFormats.isEmpty();
    }
    
    public boolean isApplicationSupported() {
    	return this.negotiatedApplicationFormats != null && !this.negotiatedApplicationFormats.isEmpty();
    }
    
    public boolean isVideoSupported() {
    	return this.negotiatedVideoFormats != null && !this.negotiatedVideoFormats.isEmpty();
    }
    
	public void setAudioPort(int audioPort) {
		this.audioPort = audioPort;
	}
	
    public void setVideoPort(int videoPort) {
		this.videoPort = videoPort;
	}
    
    public void setApplicationPort(int applicationPort) {
		this.applicationPort = applicationPort;
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
     * Intended for subclasses of SdpTemplate
     * 
     * @return any additional attributes for the video SDP part 
     */
    protected String getExtendedVideoAttributes() {
		return "";
	}
    
    /**
     * 
     * Intended for subclasses of SdpTemplate
     * 
     * @return any additional attributes for the application SDP part 
     */
    protected String getExtendedApplicationAttributes() {
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

    @Deprecated
    private void writeHeader(StringBuilder builder) {
        builder.append("v=0\n");
        builder.append("o=- %s 1 IN IP4 %s\n");
        builder.append("s=Mobicents Media Server \n");
        builder.append("c=%s %s %s\n");
        builder.append("t=0 0\n");
    }
    
    private String writeHeader() {
    	StringBuilder builder = new StringBuilder();
        builder.append("v=0\n");
        builder.append("o=- %s 1 IN IP4 %s\n");
        builder.append("s=Mobicents Media Server \n");
        builder.append("c=%s %s %s\n");
        builder.append("t=0 0\n");
        return builder.toString();
    }

    @Deprecated
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
        builder.append(getExtendedAudioAttributes()).append("\n");
    }
    
    private String writeAudioDescriptor() {
    	if(this.offeredAudioFormats == null) {
    		throw new NullPointerException("Supported audio formats is null. Cannot write audio descriptor.");
    	}
    	
    	StringBuilder builder = new StringBuilder();
        builder.append("m=audio %s ").append(getMediaProfile()).append(" ");
        if(isAudioSupported()) {
        	builder.append(payloads(this.negotiatedAudioFormats)).append("\n");
            this.negotiatedAudioFormats.rewind();
            while (this.negotiatedAudioFormats.hasMore()) {
                RTPFormat f = this.negotiatedAudioFormats.next();
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
            builder.append(getExtendedAudioAttributes()).append("\n");
        } else {
        	builder.append(payloads(this.offeredAudioFormats)).append("\n");
        }
        return builder.toString();
    }

    @Deprecated
    private void writeVideoDescriptor(StringBuilder builder, RTPFormats formats) {
		builder.append("m=video %s ").append(getMediaProfile()).append(" ");
		// builder.append("100 116 117");
		builder.append(payloads(formats)).append("\n");

        formats.rewind();
        while(formats.hasMore()) {
            RTPFormat f = formats.next();
            builder.append(String.format("a=rtpmap:%d %s/%d\n", f.getID(), f.getFormat().getName(), f.getClockRate()));
            if (f.getFormat().getOptions() != null) {
                builder.append(String.format("a=fmtp: %d %s\n", f.getID(), f.getFormat().getOptions().toString()));
            }
            // TODO Finish implementing video description
        }
    }
    
    private String writeVideoDescriptor() {
    	if(this.offeredVideoFormats == null) {
    		throw new NullPointerException("Supported video formats is null. Cannot write video descriptor.");
    	}
    	
    	StringBuilder builder = new StringBuilder();
		builder.append("m=video %s ").append(getMediaProfile()).append(" ");
		if(isVideoSupported()) {
			builder.append(payloads(this.negotiatedVideoFormats)).append("\n");
			
	        this.negotiatedVideoFormats.rewind();
	        while(this.negotiatedVideoFormats.hasMore()) {
	            RTPFormat f = this.negotiatedVideoFormats.next();
	            builder.append(String.format("a=rtpmap:%d %s/%d\n", f.getID(), f.getFormat().getName(), f.getClockRate()));
	            if (f.getFormat().getOptions() != null) {
	                builder.append(String.format("a=fmtp: %d %s\n", f.getID(), f.getFormat().getOptions().toString()));
	            }
	            // TODO Finish implementing video description
	        }
	        builder.append(this.getExtendedVideoAttributes()).append("\n");
		} else {
			// TODO No video formats are received because video is still not implemented
			 builder.append(payloads(this.offeredVideoFormats)).append("\n");
//			builder.append("100 116 117").append("\n");
		}
        return builder.toString();
    }
    
    private String writeApplicationDescriptor() {
    	if(this.offeredApplicationFormats == null) {
    		throw new NullPointerException("Supported application formats is null. Cannot write application descriptor.");
    	}
    	
    	StringBuilder builder = new StringBuilder();
        builder.append("m=application %s ").append(getApplicationProfile()).append(" 0");
        return builder.toString();
    }
    
    @Deprecated
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
