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

package org.mobicents.media.server.impl.rtp.rfc2833;

import java.util.ArrayList;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.Server;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.server.impl.resource.dtmf.DtmfEventImpl;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.CodecFactory;

/**
 *
 * @author kulikov
 */
public class DtmfConverter {
    
    private final static AudioFormat LINEAR_AUDIO = new AudioFormat(
            AudioFormat.LINEAR, 8000, 16, 1,
            AudioFormat.LITTLE_ENDIAN,
            AudioFormat.SIGNED);
    
    private double dt = 1 / LINEAR_AUDIO.getSampleRate();
    private short A = Short.MAX_VALUE / 2;
    private int volume = 0;
    private int f1,  f2;
    private double time = 0;
    private long rtpTime = 0;
    
    private RtpClock clock;    

    public final static String[] TONE = new String[] {
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#", "A",
        "B", "C", "D"
    };
    
    public final static String[][] events = new String[][]{
        {"1", "2", "3", "A"},
        {"4", "5", "6", "B"},
        {"7", "8", "9", "C"},
        {"*", "0", "#", "D"}
    };
    
    private int[] lowFreq = new int[]{697, 770, 852, 941};
    private int[] highFreq = new int[]{1209, 1336, 1477, 1633};
    
    private Codec codec;

    private final static ArrayList<CodecFactory> codecFactories = new ArrayList();
    static {
        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.g711.alaw.DecoderFactory());
        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.g711.alaw.EncoderFactory());

        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.g711.ulaw.DecoderFactory());
        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.g711.ulaw.EncoderFactory());

        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.gsm.DecoderFactory());
        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.gsm.EncoderFactory());

        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.speex.DecoderFactory());
        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.speex.EncoderFactory());

        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.g729.DecoderFactory());
        codecFactories.add(new org.mobicents.media.server.impl.dsp.audio.g729.EncoderFactory());
    }
    
    //This is the time when last RTP event arrives
    private long timestamp;
    
    public void setClock(RtpClock clock) {
        this.clock = clock;
    }
    
    private short getValue(double t) {
        return (short) (A * (Math.sin(2 * Math.PI * f1 * t) + Math.sin(2 * Math.PI * f2 * t)));
    }
    
    public void setPreffered(Format fmt) {
        codec = selectCodec(fmt);
    }

    private Codec selectCodec(Format f) {
        for (CodecFactory factory : codecFactories) {
            if (factory.getSupportedOutputFormat().matches(f) &&
                    factory.getSupportedInputFormat().matches(Codec.LINEAR_AUDIO)) {
                return factory.getCodec();
            }
        }
        return null;
    }
    
    public void process(RtpPacket packet,  Buffer buffer) {
        long now = Server.scheduler.getTimestamp();
        
        //the first packet has marker set but this packet may be lost
        //so if time between two packets excceds 1 second we count is as first
        if (packet.getMarker() == true || (now - timestamp) > 1000) {
            time = 0;
            rtpTime = 0;
        } 
        
        //remember when this packet was processed
        timestamp = now;
        
        byte[] data = packet.getPayload();
        
        String digit = TONE[data[0]];
        volume = data[1] & 0x3F;
        
        int duration = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
        rtpTime = duration;
        
        int d = (int)(clock.getTime(rtpTime) - time * 1000);
        
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (events[i][j].equalsIgnoreCase(digit)) {
                    f1 = lowFreq[i];
                    f2 = highFreq[j];
                }
            }
        }
        
        int k = 0;
        int frameSize = 160;

        data = new byte[2 * frameSize];
        for (int i = 0; i < frameSize; i++) {
            short v = getValue(time + dt * i);
            data[k++] = (byte) v;
            data[k++] = (byte) (v >> 8);
        }
        
        buffer.setHeader(new DtmfEventImpl(null, digit, volume));
        buffer.setData(data);
        buffer.setOffset(0);
        buffer.setLength(320);
        buffer.setFormat(LINEAR_AUDIO);
        buffer.setDuration(d);
        time += ((double) d) / 1000.0;
        if (codec != null) {
            codec.process(buffer);
        }
    }
    
}
