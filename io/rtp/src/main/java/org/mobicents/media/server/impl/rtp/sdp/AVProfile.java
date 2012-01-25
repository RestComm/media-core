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

import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.utils.Text;

/**
 *
 * @author kulikov
 */
public class AVProfile {

    public final static AudioFormat telephoneEvent = FormatFactory.createAudioFormat("telephone-event", 8000);
    static {
        telephoneEvent.setOptions(new Text("0-15"));
    }
    public final static RTPFormats audio = new RTPFormats();
    public final static RTPFormats video = new RTPFormats();
    
    private final static RTPFormat pcmu = new RTPFormat(0, FormatFactory.createAudioFormat("pcmu", 8000, 8, 1), 8000);
    private final static RTPFormat pcma = new RTPFormat(8, FormatFactory.createAudioFormat("pcma", 8000, 8, 1), 8000);
    private final static RTPFormat gsm = new RTPFormat(3, FormatFactory.createAudioFormat("gsm", 8000), 8000);
    private final static RTPFormat g729 = new RTPFormat(18, FormatFactory.createAudioFormat("g729", 8000), 8000);
    private final static RTPFormat dtmf = new RTPFormat(101, telephoneEvent, 8000);
    private final static RTPFormat linear = new RTPFormat(150, FormatFactory.createAudioFormat("linear", 8000, 16, 1), 8000);

    private final static RTPFormat H261 = new RTPFormat(45, FormatFactory.createVideoFormat("h261"));
    private final static RTPFormat H263 = new RTPFormat(34, FormatFactory.createVideoFormat("h263"));
    private final static RTPFormat MP4V_ES = new RTPFormat(96, FormatFactory.createVideoFormat("mp4v-es"));

    static {
        audio.add(pcma);
        audio.add(pcmu);
        audio.add(gsm);
        audio.add(g729);
        audio.add(dtmf);
    }

    static {
        video.add(H261);
        video.add(H263);
        video.add(MP4V_ES);
    }
    
    public static RTPFormat getFormat(int p) {    	
        RTPFormat res = audio.find(p);
        return res == null ? video.find(p) : res;
    }    
    
    public static RTPFormat getFormat(int p,Text mediaType) {
    	
    	RTPFormat res=null;
    	if(mediaType.equals(SessionDescription.AUDIO))
    		res = audio.find(p);    		
    	else if(mediaType.equals(SessionDescription.VIDEO))
    		res = video.find(p);    		
    	
    	return res;
    }
}
