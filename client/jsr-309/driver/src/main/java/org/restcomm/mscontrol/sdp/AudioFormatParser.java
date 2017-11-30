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
public class AudioFormatParser implements FormatParser {
    
    public Format getFormat(int payload) {
        return AVPROFILE.getAudioFormat(payload);
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
        pos1 = rtpmap.indexOf('/', pos + 1);
        double clockRate = pos1 > 0 ? 
            Double.parseDouble(rtpmap.substring(pos + 1, pos1)) : 
            Double.parseDouble(rtpmap.substring(pos + 1, rtpmap.length()));
        
        int channels = pos1 > 0 ?
            Integer.parseInt(rtpmap.substring(pos1 + 1, rtpmap.length())) : 1;
        
        if (encoding.equalsIgnoreCase("pcmu")) {
            formats[index] = new AudioFormat(AudioFormat.ULAW, clockRate, 8, channels);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("pcma")) {
            formats[index] = new AudioFormat(AudioFormat.ALAW, clockRate, 8, channels);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("telephone-event")) {
            formats[index] = new AudioFormat("telephone-event", clockRate, AudioFormat.NOT_SPECIFIED, AudioFormat.NOT_SPECIFIED);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("g729")) {
            formats[index] = new AudioFormat(AudioFormat.G729, clockRate, AudioFormat.NOT_SPECIFIED, channels);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("gsm")) {
            formats[index] = new AudioFormat(AudioFormat.GSM, clockRate, AudioFormat.NOT_SPECIFIED, channels);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("l16")){
            formats[index] = new AudioFormat(AudioFormat.LINEAR, clockRate, 16, channels, 
                    AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("AMR")){
            formats[index] = new AudioFormat(AudioFormat.AMR, clockRate, AudioFormat.NOT_SPECIFIED, channels);
            payloads[index] = payload;
        } else {
            formats[index] = new AudioFormat(encoding, clockRate, AudioFormat.NOT_SPECIFIED, channels);
            payloads[index] = payload;
        }
        return index == count;
    }

    public void write(StringBuffer buff, int p, Format f) {
        AudioFormat fmt = (AudioFormat)f;
        String encName = f.getEncoding();
        buff.append("a=rtpmap:");
        buff.append(p);
        buff.append(" ");

        if (encName.equalsIgnoreCase("alaw")) {
            buff.append("PCMA");
        } else if (encName.equalsIgnoreCase("ulaw")) {
            buff.append("PCMU");
        } else if (encName.equalsIgnoreCase("linear")) {
            buff.append("L" + fmt.getSampleSizeInBits());
        } else {
            buff.append(encName);
        } 
        
        double sr = fmt.getSampleRate();
        if (sr > 0) {
            buff.append("/");

            if ((sr - (int) sr) < 1E-6) {
                buff.append((int) sr);
            } else {
                buff.append(sr);
            }
        }
        if (fmt.getChannels() > 1) {
            buff.append("/" + fmt.getChannels());
        }
        
        if (f.equals(AVProfile.DTMF)) {
            buff.append("\na=fmtp:" + p + " 0-15");
        }
    }

}
