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

package org.restcomm.media.codec.g711.ulaw;

import org.restcomm.media.spi.dsp.Codec;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 * Implements G.711 U-law compressor.
 * 
 * @author Yulian Oifa
 */
public class Encoder implements Codec {
    private final static Format ulaw = FormatFactory.createAudioFormat("pcmu", 8000, 8, 1);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private int i,j,count;
    
    public Format getSupportedInputFormat() {
        return linear;
    }

    public Format getSupportedOutputFormat() {
        return ulaw;
    }

    /**
     * (Non Java-doc)
     * 
     * @see org.mobicents.media.server.impl.jmf.dsp.Codec#process(Buffer).
     */
    public Frame process(Frame frame) {
    	count = frame.getLength() / 2;
        Frame res = Memory.allocate(count);
        
        byte[] data=frame.getData();
        byte[] resData=res.getData();
        
        for (i = 0,j = 0; i < count; i++,j++)
        	resData[i]=EncoderData.muLawCompressTable[data[j+1] & 0xff][data[j++] & 0xff];        	
        
        res.setOffset(0);
        res.setLength(count);
        res.setFormat(ulaw);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setEOM(frame.isEOM());
        res.setSequenceNumber(frame.getSequenceNumber());

        return res;               
    }
}