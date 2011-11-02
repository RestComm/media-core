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
package org.mobicents.media.server.impl.dsp.audio.g729;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.log4j.Logger;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.SignalingProcessor;

public class Encoder implements Codec {

    int frame = 0;
    CodLD8K encoder = new CodLD8K();
    PreProc preProc = new PreProc();
    CircularBuffer circularBuffer = new CircularBuffer(32000);
    int prm[] = new int[LD8KConstants.PRM_SIZE];
    short serial[] = new short[LD8KConstants.SERIAL_SIZE];
    private transient Logger logger = Logger.getLogger(Encoder.class);

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
        return Codec.LINEAR_AUDIO;
    }

    public Format getSupportedOutputFormat() {
        return Codec.G729;
    }

    public void process(Buffer buffer) {
        byte[] data = buffer.getData();

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
            resultingBytes = new byte[one.length + two.length];
            for (int q = 0; q < one.length; q++) {
                resultingBytes[q] = one[q];
                resultingBytes[q + one.length] = two[q];
            }
        }
        buffer.setData(resultingBytes);
        buffer.setOffset(0);
        buffer.setLength(resultingBytes.length);
        buffer.setFormat(Codec.G729);
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
    public void processTestDecoderWithFileITUEncoded(Buffer buffer) {
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

    public void processTestFileWithoutDecoding(Buffer buffer) {
        byte[] res = null;
        try {
            byte[] data = new byte[20];
            byte[] tmp = new byte[82 * 2];
            testData.read(tmp);
            short[] sdata = Util.byteArrayToShortArray(tmp);
            byte[] bits = Bits.toRealBits(sdata);
            for (int q = 0; q < 10; q++) {
                data[q] = bits[q];
            }
            testData.read(tmp);
            sdata = Util.byteArrayToShortArray(tmp);
            bits = Bits.toRealBits(sdata);
            for (int q = 0; q < 10; q++) {
                data[10 + q] = bits[q];
            }
            res = data;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setProc(SignalingProcessor processor) {
    }
}
