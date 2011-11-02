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

import java.io.StreamCorruptedException;

import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.xiph.speex.SpeexDecoder;

/**
 * Implements Speex narrow band, 8kHz decompressor.
 * 
 * @author Amit Bhayani
 * @author Oleg Kulikov
 */
public class Decoder implements Codec {

    private final static Format speex = FormatFactory.createAudioFormat("speex", 8000);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private final static int MODE_NB = 0;
    private final static boolean ENHANCED = false;
    private final static int SAMPLE_RATE = 8000;
    private final static int CHANNELS = 1;
    private SpeexDecoder decoder = new SpeexDecoder();
    private int len;

    public Decoder() {
        decoder.init(MODE_NB, SAMPLE_RATE, CHANNELS, ENHANCED);
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#getSupportedFormat().
     */
    public Format getSupportedInputFormat() {
        return speex;
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#getSupportedFormat().
     */
    public Format getSupportedOutputFormat() {
        return linear;
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#process(Buffer).
     */
    public Frame process(Frame frame) {
        Frame res = Memory.allocate(320);
        len = process(frame.getData(), 0, frame.getLength(), res.getData());

        res.setOffset(0);
        res.setLength(len);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        
        return res;
    }

    /**
     * Perform decompression.
     * 
     * @param media input compressed speech.
     * @return uncompressed speech.
     */
    private int process(byte[] media, int offset, int len, byte[] res) {
        try {
            decoder.processData(media, offset, len);
            int size = decoder.getProcessedDataByteSize();
            decoder.getProcessedData(res, 0);
            return size;
        } catch (StreamCorruptedException e) {
            return 0;
        }
    }
}
