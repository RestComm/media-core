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

package org.mobicents.media.server.impl.dsp.audio.speex;

import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.xiph.speex.SpeexEncoder;

/**
 * Implements Speex narrow band, 8kHz compressor.
 * 
 * @author Amit Bhayani
 * @author Oleg Kulikov
 */
public class Encoder implements Codec {

    private final static Format speex = FormatFactory.createAudioFormat("speex", 8000);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private int MODE_NB = 0;
    private int mode = 0;
    private int QUALITY = 3;
    private int quality = 3;
    private final static int SAMPLE_RATE = 8000;
    private final static int CHANNELS = 1;
    private SpeexEncoder speexEncoder = new SpeexEncoder();
    private int limit, len;

    public Encoder() {
        speexEncoder.init(MODE_NB, QUALITY, SAMPLE_RATE, CHANNELS);
    }

    /**
     * Gets the mode of the codec.
     * 
     * @return integer identifier of the mode.
     */
    public int getMode() {
        return mode;
    }
    
    /**
     * Sets the mode of codec.
     * 
     * @param mode the new mode value
     */
    public void setMode(int mode) {
        this.mode = mode;
    }
    
    /**
     * Gets the quality value.
     * 
     * @return integer value in range 0..10 which shows the quality.
     */
    public int getQuality(){
        return quality;
    }
    
    /**
     * Modify quality value.
     * 
     * @param quality integer value in range 0..10 which shows the quality.
     */
    public void setQuality(int quality) {
        this.quality = quality;
    }
    
    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#getSupportedFormat().
     */
    public Format getSupportedInputFormat() {
        return linear;
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#getSupportedFormat().
     */
    public Format getSupportedOutputFormat() {
        return speex;
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#process(Buffer).
     */
    public Frame process(Frame frame) {
        Frame res = Memory.allocate(320);

        //fill remainder with zeros
        limit = Math.min(320, frame.getData().length);
        for (int i = frame.getLength(); i < limit; i++) {
            frame.getData()[i] = 0;
        }

        len = process(frame.getData(), 0, 320, res.getData());
        res.setOffset(0);
        res.setLength(len);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        return res;
    }
    
    /**
     * Perform compression.
     * 
     * @param media input media
     * @return compressed media.
     */
    public int process(byte[] media, int offset, int length, byte[] dest) {
        speexEncoder.processData(media, offset, length);
        int size = speexEncoder.getProcessedDataByteSize();
        //System.out.println(speexEncoder.getSampleRate());
        speexEncoder.getProcessedData(dest, 0);
        return size;
    }
}
