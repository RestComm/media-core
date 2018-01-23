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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.codec.opus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.spi.dsp.Codec;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 * Implements Opus decoder.
 * 
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 * 
 */
public class Decoder implements Codec {

    private final static Logger log = LogManager.getLogger(Encoder.class);

    private final static Format opus = FormatFactory.createAudioFormat("opus", 48000, 8, 2);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private long decoderAddress;
    
    private final int OPUS_SAMPLE_RATE = 8000;

    public Decoder() {
        decoderAddress = OpusJni.createDecoderNative(OPUS_SAMPLE_RATE, 1);
    }
    
    @Override
    protected void finalize() throws Throwable {
        if (decoderAddress != 0) OpusJni.releaseDecoderNative(decoderAddress);
        super.finalize();
    }

    @Override
    public Format getSupportedInputFormat() {
        return opus;
    }

    @Override
    public Format getSupportedOutputFormat() {
        return linear;
    }

    @Override
    public Frame process(Frame frame) {
    	
        short[] decodedData = OpusJni.decodeNative(decoderAddress, frame.getData());
        byte[] output = new byte[2 * decodedData.length];
        ByteBuffer.wrap(output).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(decodedData);
    	
        Frame res = Memory.allocate(output.length);
        System.arraycopy(output, 0, res.getData(), 0, output.length);
        
        res.setOffset(0);
        res.setLength(output.length);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(linear);
        res.setHeader(frame.getHeader());
        return res;
    }
}
