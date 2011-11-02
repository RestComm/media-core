/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.rtp.sdp;

import org.mobicents.media.Format;
import org.mobicents.media.format.VideoFormat;
import org.mobicents.media.server.spi.rtp.AVProfile;

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
