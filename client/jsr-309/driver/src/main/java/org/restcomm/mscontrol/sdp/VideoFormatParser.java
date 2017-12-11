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
package org.restcomm.mscontrol.sdp;

/**
 *
 * @author kulikov
 * @author amit bhayani
 */
public class VideoFormatParser implements FormatParser {

    public Format getFormat(int payload) {
        return AVPROFILE.getVideoFormat(payload);
    }

    public boolean parse(String rtpmap, int[] payloads, Format[] formats, int count) {
        int pos = rtpmap.indexOf(':');
        int pos1 = rtpmap.indexOf(' ', pos);
        
        //decode payload
        int payload = Integer.parseInt(rtpmap.substring(pos + 1, pos1));
        
        int index = 0;
        while (index < count) {
            if (payloads[index] == payload) break;
            index++;
        }
        
        //decoding encoding name
        pos = rtpmap.indexOf('/', pos1);
        String encoding = rtpmap.substring(pos1 + 1, pos);
        
        //decoding clock rate
        double clockRate = Double.parseDouble(rtpmap.substring(pos + 1, rtpmap.length()));
        formats[index] = new VideoFormat(encoding, 25, (int)clockRate);
        payloads[index] = payload;
        
        return index == count;        
    }

    public void write(StringBuffer buff, int p, Format f) {
        VideoFormat fmt = (VideoFormat)f;
        String encName = fmt.getEncoding().toLowerCase();
        buff.append("a=rtpmap:");
        buff.append(p);
        buff.append(" ");
        buff.append(encName);
        buff.append("/");
        buff.append(fmt.getClockRate());
        if (f.equals(AVProfile.H263)) {
            buff.append("\na=fmtp:" + p + " QCIF=2 CIF=3 MaxBR=1960");
        }
        
    }

}
