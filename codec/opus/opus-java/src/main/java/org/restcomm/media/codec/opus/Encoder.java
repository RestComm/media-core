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
 * Implements Opus encoder.
 * 
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 * 
 */
public class Encoder implements Codec {

    private final static Logger log = LogManager.getLogger(Encoder.class);

    private final static Format opus = FormatFactory.createAudioFormat("opus", 48000, 8, 2);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    
    private long encoderAddress;

    private final int OPUS_SAMPLE_RATE = 8000;
    private final int OPUS_BITRATE = 20000;

    public Encoder() {
        encoderAddress = OpusJni.createEncoderNative(OPUS_SAMPLE_RATE, 1, OpusJni.OPUS_APPLICATION_VOIP, OPUS_BITRATE);
    }
    
    @Override
    protected void finalize() throws Throwable {
        OpusJni.releaseEncoderNative(encoderAddress);
        super.finalize();
    }

    @Override
    public Format getSupportedInputFormat() {
        return linear;
    }

    @Override
    public Format getSupportedOutputFormat() {
        return opus;
    }

    @Override
    public Frame process(Frame frame) {
    	
        byte[] input = frame.getData();
        short[] inputData = new short[frame.getLength() / 2];
        ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(inputData);
        byte[] encodedData = OpusJni.encodeNative(encoderAddress, inputData);
    	
        Frame res = Memory.allocate(encodedData.length);
        System.arraycopy(encodedData, 0, res.getData(), 0, encodedData.length);
        
        res.setOffset(0);
        res.setLength(encodedData.length);
        res.setFormat(opus);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setEOM(frame.isEOM());
        res.setSequenceNumber(frame.getSequenceNumber());

        return res;
    }    
}
