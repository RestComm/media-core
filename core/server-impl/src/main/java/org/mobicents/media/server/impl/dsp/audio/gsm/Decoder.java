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

import org.apache.log4j.Logger;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.SignalingProcessor;
import org.tritonus.lowlevel.gsm.InvalidGSMFrameException;

/**
 * 
 * @author amit bhayani
 * 
 */
public class Decoder implements Codec {

    private transient Logger logger = Logger.getLogger(Decoder.class);
    private org.tritonus.lowlevel.gsm.GSMDecoder decoder = new org.tritonus.lowlevel.gsm.GSMDecoder();
    private static final int BUFFER_SIZE = 320;

    public Decoder() {
    }

    public void process(Buffer buffer) {
        byte[] data = buffer.getData();
        byte[] res = process(data);
        buffer.setData(res);
        buffer.setOffset(0);
        buffer.setLength(res.length);
        buffer.setFormat(Codec.LINEAR_AUDIO);
    }
    
    private byte[] m_abBuffer;

    /**
     * Perform compression.
     * 
     * @param input
     *            media
     * @return compressed media.
     */
    public byte[] process(byte[] media) {

        m_abBuffer = new byte[BUFFER_SIZE];

        try {
            decoder.decode(media, 0, m_abBuffer, 0, false);
        } catch (InvalidGSMFrameException e) {
            e.printStackTrace();
        }
        return m_abBuffer;
    }

    public Format getSupportedInputFormat() {
        return Codec.GSM;
    }

    public Format getSupportedOutputFormat() {
        return Codec.LINEAR_AUDIO;
    }

    public void setProc(SignalingProcessor processor) {
        // TODO Auto-generated method stub
    }
}
