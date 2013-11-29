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

/**
 *
 * @author yulian oifa
 */
public class WebRTCSdpTemplate extends SdpTemplate {

    public WebRTCSdpTemplate(SessionDescription sessionDescription) {
    	super(sessionDescription);
    }
    
    @Override
    protected String getMediaProfile() {
    	return MediaDescriptorField.RTP_SAVPF_PROFILE;
    }

    
    /**
     * 
     * add any WebRTCSpecific attributes to the SDP offer
     * 
     */
    protected String getExtendedAudioAttributes() {
    	// TODO: Calculate the actual fingerprint of the server certificate
    	return "a=fingerprint:sha-256 28:D5:4A:00:0E:4A:53:F9:DC:57:67:17:49:BC:E2:85:24:A3:52:70:99:76:48:B8:72:11:BB:DF:14:A7:4D:3B";
    }

}
