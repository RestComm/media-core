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

package org.restcomm.media.codec.g729;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.restcomm.media.spi.dsp.Codec;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

public class Encoder implements Codec {

    private final static Format g729 = FormatFactory.createAudioFormat("g729", 8000);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    int frame = 0;
    CodLD8K encoder = new CodLD8K();
    PreProc preProc = new PreProc();
    CircularBuffer circularBuffer = new CircularBuffer(32000);
    int prm[] = new int[LD8KConstants.PRM_SIZE];
    short serial[] = new short[LD8KConstants.SERIAL_SIZE];

    /* For Debugging Only */
    FileInputStream testData = null;
    FileOutputStream outdbg = null;

    public Encoder() {
        preProc.init_pre_process();
        encoder.init_coder_ld8k();
        try {
            // testData = new FileInputStream("speech-java.bit.itu");
            // testData = new FileInputStream("french.in");
            // outdbg = new FileOutputStream("/home/vralev/speech-dbg.bit");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Format getSupportedInputFormat() {
        return linear;
    }

    public Format getSupportedOutputFormat() {
        return g729;
    }

    public Frame process(Frame frame) {
        Frame res = null;
        byte[] data = frame.getData();

        circularBuffer.addData(data);

        int frameSize = 2 * LD8KConstants.L_FRAME;
        byte[] speechWindow = circularBuffer.getData(2 * frameSize);
        byte[] resultingBytes = null;

        if (speechWindow == null) {
            resultingBytes = new byte[0]; // No data available right now, send
        // empty buffer
        } else {
            // Process two frames = 20ms
            byte[] one = new byte[frameSize];
            byte[] two = new byte[frameSize];
            for (int q = 0; q < frameSize; q++) {
                one[q] = speechWindow[q];
                two[q] = speechWindow[q + frameSize];
            }
            one = process(one);
            two = process(two);

            if (one.length != two.length) {
                throw new RuntimeException(
                        "The two frames are not equal in size!");
            }
            res = Memory.allocate(one.length + two.length);
            res.setLength(one.length + two.length);
            byte[] resultBytes = res.getData();
            for (int q = 0; q < one.length; q++) {
            	resultBytes[q] = one[q];            	
            	resultBytes[q + one.length] = two[q];
            }
        }
        res.setOffset(0);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(g729);
        return res;
    }

    /**
     * Perform compression.
     * 
     * @param input
     *            media
     * @return compressed media.
     */
    public byte[] process(byte[] media) {
        frame++;

        float[] new_speech = new float[media.length];
        short[] shortMedia = Util.byteArrayToShortArray(media);
        for (int i = 0; i < LD8KConstants.L_FRAME; i++) {
            new_speech[i] = (float) shortMedia[i];
        }
        preProc.pre_process(new_speech, LD8KConstants.L_FRAME);

        encoder.loadSpeech(new_speech);
        encoder.coder_ld8k(prm, 0);

        //byte[] a = new byte[10];
        Bits.prm2bits_ld8k(prm, serial);
        // return a;
        return Bits.toRealBits(serial);
    }

    /* These methods are just for debugging */
    public void processTestDecoderWithFileITUEncoded(Frame buffer) {
        /*
        byte[] data = (byte[]) buffer.getData();
        
        int offset = buffer.getOffset();
        int length = buffer.getLength();
        
        byte[] media = new byte[length - offset];
        System.arraycopy(data, 0, media, 0, media.length);
        
        int frameSize = 160;
        byte[] speechWindow160 = new byte[2 * frameSize];
        try {
        int r = testData.read(speechWindow160);
        outdbg.write(speechWindow160);
        outdbg.flush();
        if (r != speechWindow160.length) {
        logger.info("Diferent frame size" + r);
        }
        } catch (IOException e) {
        e.printStackTrace();
        }
        byte[] res = null;
        if (speechWindow160 == null) {
        res = new byte[0];
        } else {
        byte[] one = new byte[frameSize];
        byte[] two = new byte[frameSize];
        for (int q = 0; q < frameSize; q++) {
        one[q] = speechWindow160[q];
        two[q] = speechWindow160[q + frameSize];
        }
        one = process(one);
        two = process(two);
        if (one.length != two.length) {
        logger.error("UNEQUAL SIZE OF G729 FRAMES!!!");
        }
        res = new byte[one.length + two.length];
        for (int q = 0; q < one.length; q++) {
        res[q] = one[q];
        res[q + one.length] = two[q];
        }
        logger.debug("Incoming:\n" + DebugUtils.debugArray(Util.byteArrayToShortArray(speechWindow160)));
        logger.debug("Outgoing:\n" + DebugUtils.debugArray(res));
        }
        
        buffer.setData(res);
        buffer.setOffset(0);
        buffer.setLength(res.length);
         */
    }

    public void processTestFileWithoutDecoding(Frame buffer) {
        byte[] res = null;
        try {
            byte[] data = new byte[20];
            byte[] tmp = new byte[82 * 2];
            testData.read(tmp);
            short[] sdata = Util.byteArrayToShortArray(tmp);
            byte[] bits = Bits.toRealBits(sdata);
            System.arraycopy(bits, 0, data, 0, 10);
            testData.read(tmp);
            sdata = Util.byteArrayToShortArray(tmp);
            bits = Bits.toRealBits(sdata);
            System.arraycopy(bits, 0, data, 10, 10);
            res = data;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
