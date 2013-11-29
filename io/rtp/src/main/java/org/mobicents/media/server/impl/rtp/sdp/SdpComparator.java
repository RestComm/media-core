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

import java.util.Collection;

import org.mobicents.media.server.utils.Text;

/**
 * Compares two SDPs.
 *
 * @author kulikov
 */
public class SdpComparator {
    protected final static Text AUDIO = new Text("audio");
    protected final static Text VIDEO = new Text("video");
    protected final static Text APPLICATION = new Text("application");

    private RTPFormats audio = new RTPFormats();
    private RTPFormats video = new RTPFormats();
    private RTPFormats application = new RTPFormats();

    @Deprecated
    public void negotiate(SessionDescription sdp, RTPFormats audio, RTPFormats video) {
        this.audio.clean();
        this.video.clean();

        Collection<MediaDescriptorField> mds = sdp.getMedia();
        for (MediaDescriptorField md : mds) {
            if (md.getMediaType().equals(AUDIO)) {
            	if(audio!=null)
            		md.getFormats().intersection(audio, this.audio);
            } else {
            	if(video!=null)
            		md.getFormats().intersection(video, this.video);
            }
        }
    }
    
    /**
     * Negotiates the audio formats to be used in the call.
     * @param sdp The session description
     * @param formats The available formats
     * @return The supported formats. If no formats are supported the returned list will be empty.
     */
    public RTPFormats negotiateAudio(SessionDescription sdp, RTPFormats formats) {
    	this.audio.clean();
    	MediaDescriptorField descriptor = sdp.getAudioDescriptor();
    	descriptor.getFormats().intersection(formats, this.audio);
    	return this.audio;
    }

    /**
     * Negotiates the video formats to be used in the call.
     * @param sdp The session description
     * @param formats The available formats
     * @return The supported formats. If no formats are supported the returned list will be empty.
     */
    public RTPFormats negotiateVideo(SessionDescription sdp, RTPFormats formats) {
    	this.video.clean();
    	MediaDescriptorField descriptor = sdp.getVideoDescriptor();
    	descriptor.getFormats().intersection(formats, this.video);
    	return this.video;
    }

    /**
     * Negotiates the application formats to be used in the call.
     * @param sdp The session description
     * @param formats The available formats
     * @return The supported formats. If no formats are supported the returned list will be empty.
     */
    public RTPFormats negotiateApplication(SessionDescription sdp, RTPFormats formats) {
    	this.application.clean();
    	MediaDescriptorField descriptor = sdp.getApplicationDescriptor();
    	descriptor.getFormats().intersection(formats, this.application);
    	return this.application;
    }

    public void compare(SessionDescription sdp1, SessionDescription sdp2) {
        audio.clean();
        video.clean();
        application.clean();

        Collection<MediaDescriptorField> mds1 = sdp1.getMedia();
        Collection<MediaDescriptorField> mds2 = sdp2.getMedia();

        for (MediaDescriptorField md1 : mds1) {
            for (MediaDescriptorField md2 : mds2) {
                if (md1.getMediaType().equals(md2.getMediaType())) {
                    compare(md1, md2);
                }
            }
        }
    }

    private void compare(MediaDescriptorField md1, MediaDescriptorField md2) {
        RTPFormats collector = null;
        if (md1.getMediaType().equals(AUDIO)) {
            collector = audio;
        } else if (md1.getMediaType().equals(VIDEO)) {
            collector = video;
        } else if (md1.getMediaType().equals(APPLICATION)) {
        	collector = application;
        } else {
        	throw new NullPointerException("Unrecognized collector for media type "+ md1.getMediaType());
        }
        md1.getFormats().intersection(md2.getFormats(), collector);
    }
    
    private RTPFormats compareAudio(SessionDescription sdp1, SessionDescription sdp2) {
    	this.audio.clean();
    	RTPFormats formats1 = sdp1.getAudioDescriptor().getFormats();
    	RTPFormats formats2 = sdp2.getAudioDescriptor().getFormats();
    	formats1.intersection(formats2, this.audio);
    	return this.audio;
    }
    
    private RTPFormats compareVideo(SessionDescription sdp1, SessionDescription sdp2) {
    	this.video.clean();
    	RTPFormats formats1 = sdp1.getVideoDescriptor().getFormats();
    	RTPFormats formats2 = sdp2.getVideoDescriptor().getFormats();
    	formats1.intersection(formats2, this.video);
    	return this.video;
    }
    
    private RTPFormats compareApplication(SessionDescription sdp1, SessionDescription sdp2) {
    	this.application.clean();
    	RTPFormats formats1 = sdp1.getApplicationDescriptor().getFormats();
    	RTPFormats formats2 = sdp2.getApplicationDescriptor().getFormats();
    	formats1.intersection(formats2, this.application);
    	return this.application;
    }
    
    public RTPFormats getAudio() {
        return audio;
    }

    public RTPFormats getVideo() {
        return video;
    }

    public RTPFormats getApplication() {
		return application;
	}

}
