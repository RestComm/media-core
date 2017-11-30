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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.sdp;

import java.util.ArrayList;
import java.util.Collection;
import javax.sdp.Attribute;
import javax.sdp.SdpFactory;


/**
 *
 * @author kulikov
 */
public class RTPVideoFormat extends VideoFormat implements RTPFormat {

    private final static SdpFactory sdpFactory = SdpFactory.getInstance();
    private int payloadType;

    public RTPVideoFormat(int payloadType, String encdingName) {
        super(encdingName);
        this.payloadType = payloadType;
    }
    
    public RTPVideoFormat(int payloadType, String encdingName, float fr) {
        super(encdingName, -1,  fr);
        this.payloadType = payloadType;
    }

    
    
    public RTPVideoFormat(int payloadType,String encoding, int maxDataLength, float frameRate) {
		super(encoding, maxDataLength,  frameRate);
		this.payloadType = payloadType;
	}

	public int getPayloadType() {
        return payloadType;
    }

    private String rtpmap() {
        String s = payloadType + " " + getEncoding();
        if (getFrameRate() > 0) {
            int fr = (int) getFrameRate();
            s += "/" + fr;
        }
        return s;
    }

    public static RTPVideoFormat parseFormat(String rtpmap) {
        RTPVideoFormat fmt = null;
        String tokens[] = rtpmap.toLowerCase().split(" ");

        // split params
        int p = Integer.parseInt(tokens[0]);
        tokens = tokens[1].split("/");

        String encodingName = tokens[0];        
        if (tokens.length > 1) {
            float fr = Float.parseFloat(tokens[1]);
            fmt = new RTPVideoFormat(p, encodingName, fr);
        } else {
            fmt = new RTPVideoFormat(p, encodingName);
        }
        return fmt;
    }

    public Collection<Attribute> encode() {
        ArrayList<Attribute> list = new ArrayList();
        if (this.getEncoding().equals(VideoFormat.H261)) {
            list.add(sdpFactory.createAttribute("rtpmap", rtpmap()));
        }
        return list;
    }
}
