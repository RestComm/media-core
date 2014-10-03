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
import org.mobicents.media.server.impl.rtp.sdp.SessionDescription;
import org.mobicents.media.server.utils.Text;

/**
 *
 * @author yulian oifa
 */
public class WebRTCSdpTemplate extends SdpTemplate {

	private Text localFingerprint;
	
    public WebRTCSdpTemplate(SessionDescription sessionDescription) {
    	super(sessionDescription);
    	this.localFingerprint = new Text();
    }
    
    @Override
    protected String getMediaProfile() {
    	return MediaDescriptorField.RTP_SAVP_PROFILE;
    }

    
    public void setLocalFingerprint(Text localFingerprint) {
		this.localFingerprint = localFingerprint;
	}
    
    /**
     * 
     * add any WebRTCSpecific attributes to the SDP offer
     * 
     */
    protected String getExtendedAudioAttributes() {
    	if(this.localFingerprint != null && this.localFingerprint.length() > 0) {
    		return "a=fingerprint:"+this.localFingerprint;
    	}
    	return "";
    }
}
