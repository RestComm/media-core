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
package org.mobicents.media.server.impl.dsp.audio.gsm;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.SignalingProcessor;

/**
 * 
 * @author amit bhayani
 * 
 */
public class Encoder implements Codec {

    private org.tritonus.lowlevel.gsm.Encoder encoder = new org.tritonus.lowlevel.gsm.Encoder();
    private short[] signal = new short[160];
    private byte[] res = new byte[33];

    public void process(Buffer buffer) {
        if (buffer.getLength() != 320) {
            buffer.setFlags(Buffer.FLAG_BUF_UNDERFLOWN);
            return;
        }

        // encode into short values

        byte[] data = buffer.getData();

        // int k = 0;
        for (int i = 0; i < 160; i++) {
            // signal[i] = (short) ((data[k++] << 8) & (data[k++]));
            signal[i] = bytesToShort16(data, i * 2, false);
        }

        res = new byte[33];
        encoder.encode(signal, res);
        buffer.setData(res);
        buffer.setOffset(0);
        buffer.setLength(res.length);
        //buffer.setData(res);
        buffer.setFormat(Codec.GSM);
    }

    private short bytesToShort16(byte[] buffer, int byteOffset, boolean bigEndian) {
        return bigEndian ? ((short) ((buffer[byteOffset] << 8) | (buffer[byteOffset + 1] & 0xFF)))
                : ((short) ((buffer[byteOffset + 1] << 8) | (buffer[byteOffset] & 0xFF)));
    }

    public Format getSupportedInputFormat() {
        return Codec.LINEAR_AUDIO;
    }

    public Format getSupportedOutputFormat() {
        return Codec.GSM;
    }

    public void setProc(SignalingProcessor processor) {
        // TODO Auto-generated method stub
    }
}
