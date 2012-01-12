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

package org.mobicents.media.server.impl.rtp.rfc2833;

import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 *
 * @author kulikov
 */
public class DtmfConverter {
   
    private final static AudioFormat LINEAR_AUDIO = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static double dt = 1.0 / LINEAR_AUDIO.getSampleRate();
    
    //one second buffer for tones
    private final static byte[][] buffer = new byte[16][16000];
    
    private final static short A = Short.MAX_VALUE / 2;
    private double time = 0;
    
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
    
    private final static int[] lowFreq = new int[]{697, 770, 852, 941};
    private final static int[] highFreq = new int[]{1209, 1336, 1477, 1633};
    
    static {
        init();
    }
    
    //This is the absolute time when last RTP event arrived
    private long timestamp;
    
    //flag indicating the begining of the dtmf tone
    private boolean start;
    
    private static void init() {
        for (int i = 0; i < TONE.length; i++) {
            int f = 0; int F = 0;
            for (int k = 0; k < 4; k++) {
                for (int l = 0; l < 4; l++) {
                    if (events[k][l].equals(TONE[i])) {
                        f = lowFreq[k];
                        F = highFreq[l];
                        break;
                    }
                }
                
                if (f > 0 && F > 0) {
                    break;
                }
            }
            
            int q = 0;
            double t = 0;
            int len = buffer[i].length / 2;
            for (int p = 0; p < len; p++) {                
                short v = getValue(t, f, F);
                
                buffer[i][q++] = (byte)v;
                buffer[i][q++] = (byte)(v >> 8);
                
                t+= dt;
            }
        }
    }
    
    public void setClock(RtpClock clock) {
        this.clock = clock;
    }
    
    private static short getValue(double t, int f1, int f2) {
        return (short) (A * (Math.sin(2 * Math.PI * f1 * t) + Math.sin(2 * Math.PI * f2 * t)));
    }
    
    public Frame process(RtpPacket event) {
    	System.out.println("Converting...");
        //check is this begining of the tone
        long now = System.nanoTime();        
        start = event.getMarker() || (now - timestamp) > 1000000000L;
        //remember this time
        timestamp = now;
        
        
        //rewind time parker in case of tone beging 
        if (start) time = 0;
        
        //obtain payload
        byte[] data = new byte[5];
        event.getPyalod(data, 0);
        
        //get the total duration of the tone in milliseconds
        long duration = clock.convertToAbsoluteTime(((data[2] & 0xff) << 8) | (data[3] & 0xff));
        //get position in buffer and length
        int offset = (int)(time * 16);
        int len = (int)((duration - time) * 16);
        
        if (len == 0) {
            return null;
        }
        
        //allocate memory for the frame
        Frame frame = Memory.allocate(320);
        //copy data
        System.arraycopy(buffer[data[0]], offset, frame.getData(), 0, len);

        //update time
        time = duration;
        frame.setOffset(0);
        frame.setLength(320);
        frame.setFormat(LINEAR_AUDIO);
        frame.setHeader(TONE[data[0]]);
        frame.setDuration(20);
        
        System.out.println("Convert: " + TONE[data[0]]);
        return frame;        
    }
    
}
