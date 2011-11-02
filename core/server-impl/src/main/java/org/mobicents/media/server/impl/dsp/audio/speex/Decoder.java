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
package org.mobicents.media.server.impl.dsp.audio.speex;

import java.io.StreamCorruptedException;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.spi.dsp.Codec;
import org.xiph.speex.SpeexDecoder;

/**
 * Implements Speex narrow band, 8kHz decompressor.
 * 
 * @author Amit Bhayani
 * @author Oleg Kulikov
 */
public class Decoder implements Codec {

    private final static int MODE_NB = 0;
    private final static boolean ENHANCED = false;
    private final static int SAMPLE_RATE = 8000;
    private final static int CHANNELS = 1;
    private SpeexDecoder decoder = new SpeexDecoder();

    public Decoder() {
        decoder.init(MODE_NB, SAMPLE_RATE, CHANNELS, ENHANCED);
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#getSupportedFormat().
     */
    public Format getSupportedInputFormat() {
        return Codec.SPEEX;
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#getSupportedFormat().
     */
    public Format getSupportedOutputFormat() {
        return Codec.LINEAR_AUDIO;
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#process(Buffer).
     */
    public void process(Buffer buffer) {
        byte[] data = buffer.getData();
        byte[] temp = new byte[320];
        int len = process(data, 0, data.length, temp);
        byte[] res = new byte[len];
        System.arraycopy(temp, 0, res, 0, len);
        buffer.setData(res);
        buffer.setOffset(0);
        buffer.setLength(len);
        buffer.setFormat(Codec.LINEAR_AUDIO);
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
