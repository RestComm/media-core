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

package org.restcomm.media.resource.player.audio.tone;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.restcomm.media.resource.player.Track;
import org.restcomm.media.spi.dtmf.DtmfTonesData;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 *
 * @author oifa yulian
 */
public class ToneTrackImpl implements Track {
    private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);;
    private int period = 20;
    private int frameSize;
    private boolean eom;
//    private long timestamp;
    private long duration;
    
    private byte[] source;
    private boolean first = true;
    private SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss,SSS");

    private int position=0;
    public static final String extention=".tone";
    
    public ToneTrackImpl(URL url) throws UnsupportedAudioFileException, IOException {
    	if(!url.getHost().endsWith(extention))
    		throw new UnsupportedAudioFileException("Invalid extention");
    	
    	String toneName=url.getHost().substring(0,url.getHost().length()-extention.length());
    	if(toneName.length()>1)
    		throw new UnsupportedAudioFileException("Invalid tone");
    	
    	char currTone=toneName.charAt(0);
    	if(currTone>='0' && currTone<='9')
    		source=DtmfTonesData.buffer[currTone-'0'];
    	else if(currTone=='*')
    		source=DtmfTonesData.buffer[10];
    	else if(currTone=='#')
    		source=DtmfTonesData.buffer[11];
    	else if(currTone>='A' && currTone<='D')
    		source=DtmfTonesData.buffer[currTone-'A' + 12];
    	else if(currTone>='a' && currTone<='d')
    		source=DtmfTonesData.buffer[currTone-'a' + 12];
    	else
    		throw new UnsupportedAudioFileException("Invalid tone");
    	
    	position=0;
        
        //measure in nanoseconds
        frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);
        duration = source.length/frameSize*period*1000000L;                
    }

    public void setPeriod(int period) {
        this.period = period;
        frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);
    }

    public int getPeriod() {
        return period;
    }

    public long getMediaTime() {
        return 0;// timestamp * 1000000L;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setMediaTime(long timestamp) {
//        this.timestamp = timestamp/1000000L;
//        try {
//            long offset = frameSize * (timestamp / period);
//            byte[] skip = new byte[(int)offset];
//            stream.read(skip);
//        } catch (IOException e) {
//        }
    }
    
    private void skip(long timestamp) {
    	long offset = frameSize * (timestamp / period/ 1000000L);
        position+=offset;        
    }    

    /**
     * Reads packet from currently opened stream.
     * 
     * @param packet
     *            the packet to read
     * @param offset
     *            the offset from which new data will be inserted
     * @return the number of actualy read bytes.
     * @throws java.io.IOException
     */
    private int readPacket(byte[] packet, int offset, int psize) throws IOException {
        int length = 0;
        if(position+psize<source.length)
    	{
    		length=psize;
    		System.arraycopy(source, position, packet, offset, length);
    		position+=psize;
    	}
    	else if(position<source.length)
    	{
    		length=source.length-position;
    		System.arraycopy(source, position, packet, offset, length);
    		position+=length;
    	}
        
        return length;
    }

    private void padding(byte[] data, int count) {
        int offset = data.length - count;
        for (int i = 0; i < count; i++) {
            data[i + offset] = 0;
        }
    }
    
    public Frame process(long timestamp) throws IOException {
        if (first) {
            if (timestamp > 0) {
                skip(timestamp);
            }
            first = false;
        }
        
        Frame frame = Memory.allocate(frameSize);
        byte[] data =frame.getData();
        if (data == null) {
            data = new byte[frameSize];
        }
        
        int len = readPacket(data, 0, frameSize);
        if (len == 0) {
            eom = true;
        }

        if (len < frameSize) {
            padding(data, frameSize - len);
            eom = true;
        }

        frame.setOffset(0);
        frame.setLength(frameSize);
        frame.setEOM(eom);
        frame.setDuration(period* 1000000L);
        frame.setFormat(format);
        
        return frame;
    }

    public void close() {
    	position=source.length;
    }

    public Format getFormat() {
        return format;
    }
}
