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

package org.restcomm.media.resource.player.audio.wav;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.resource.player.Track;
import org.restcomm.media.resource.player.audio.RemoteStreamProvider;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 *
 * @author Oifa Yulian
 */
public class WavTrackImpl implements Track {

    /** audio stream */
    private InputStream inStream;
    private AudioFormat format;
    private int period = 20;
    private int frameSize;
    private boolean eom;
    private long duration;
    private int totalRead = 0;
    private int sizeOfData;

    private boolean first = true;

    private static final Logger logger = LogManager.getLogger(WavTrackImpl.class);

    // Padding for different stream types.
    private final static byte PCM_PADDING_BYTE = 0;
    private final static byte ALAW_PADDING_BYTE = (byte) 0xD5;
    private final static byte ULAW_PADDING_BYTE = (byte) 0xFF;

    private final static byte[] factBytes = new byte[] { 0x66, 0x61, 0x63, 0x74 };
    private byte paddingByte = PCM_PADDING_BYTE;

    public WavTrackImpl(URL url, RemoteStreamProvider streamProvider) throws UnsupportedAudioFileException, IOException {
        inStream = streamProvider.getStream(url);

        getFormat(inStream);
        if (format == null) {
            throw new UnsupportedAudioFileException();
        }
    }

    public void setPeriod(int period) {
        this.period = period;
        frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);
    }

    public int getPeriod() {
        return period;
    }

    @Override
    public long getMediaTime() {
        return 0;// timestamp * 1000000L;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public void setMediaTime(long timestamp) {
        // this.timestamp = timestamp/1000000L;
        // try {
        // long offset = frameSize * (timestamp / period);
        // byte[] skip = new byte[(int)offset];
        // stream.read(skip);
        // } catch (IOException e) {
        // }
    }

    private void skip(long timestamp) {
        try {
            long offset = frameSize * (timestamp / period / 1000000L);
            byte[] skip = new byte[(int) offset];
            int bytesRead = 0;
            while (bytesRead < skip.length && inStream.available() > 0) {
                int len = inStream.read(skip, bytesRead, skip.length - bytesRead);
                if (len == -1)
                    return;

                totalRead += len;
                bytesRead += len;
            }

        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void getFormat(InputStream stream) throws IOException {
        byte[] header = new byte[36];
        byte[] headerEnd = null;
        int bytesRead = 0;
        while (bytesRead < 36 && stream.available() > 0) {
            int len = stream.read(header, bytesRead, 36 - bytesRead);
            if (len == -1) {
                return;
            }
            bytesRead += len;
        }

        // ckSize 16,17,18,19
        int ckSize = (header[16] & 0xFF) | ((header[17] & 0xFF) << 8) | ((header[18] & 0xFF) << 16)
                | ((header[19] & 0xFF) << 24);
        // format 20,21
        int formatValue = (header[20] & 0xFF) | ((header[21] & 0xFF) << 8);
        // channels 22,23
        int channels = (header[22] & 0xFF) | ((header[23] & 0xFF) << 8);
        // bits per sample 34,35
        int bitsPerSample = (header[34] & 0xFF) | ((header[35] & 0xFF) << 8);
        // sample rate 24,25,26,27
        int sampleRate = (header[24] & 0xFF) | ((header[25] & 0xFF) << 8) | ((header[26] & 0xFF) << 16)
                | ((header[27] & 0xFF) << 24);
        // size of data bytes 4,5,6,7
        sizeOfData = (header[4] & 0xFF) | ((header[5] & 0xFF) << 8) | ((header[6] & 0xFF) << 16) | ((header[7] & 0xFF) << 24);
        sizeOfData -= 12;
        sizeOfData -= ckSize;

        int extraHeaderSize = 0;

        format = null;
        switch (formatValue) {
            case 1:
                // PCM
                format = FormatFactory.createAudioFormat("linear", sampleRate, bitsPerSample, channels);
                break;
            case 6:
                // ALAW
                format = FormatFactory.createAudioFormat("pcma", sampleRate, bitsPerSample, channels);
                paddingByte = ALAW_PADDING_BYTE;
                break;
            case 7:
                // ULAW
                format = FormatFactory.createAudioFormat("pcmu", sampleRate, bitsPerSample, channels);
                paddingByte = ULAW_PADDING_BYTE;
                break;
        }

        headerEnd = new byte[8 + ckSize - 16];
        bytesRead = 0;
        extraHeaderSize = headerEnd.length;
        while (bytesRead < extraHeaderSize && stream.available() > 0) {
            int len = stream.read(headerEnd, bytesRead, extraHeaderSize - bytesRead);
            if (len == -1) {
                return;
            }
            bytesRead += len;
        }

        int byteIndex = headerEnd.length - 4 - factBytes.length;
        boolean hasFact = true;
        for (int i = 0; i < factBytes.length; i++) {
            if (factBytes[i] != headerEnd[byteIndex++]) {
                hasFact = false;
                break;
            }
        }

        if (hasFact) {
            // skip fact chunk
            sizeOfData -= 12;
            headerEnd = new byte[12];
            bytesRead = 0;
            while (bytesRead < 12 && stream.available() > 0) {
                int len = stream.read(headerEnd, bytesRead, 12 - bytesRead);
                if (len == -1) {
                    return;
                }
                bytesRead += len;
            }
        }

        if (format != null) {
            frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);
            duration = sizeOfData * period * 1000000L / frameSize;
        }
    }

    /**
     * Reads packet from currently opened stream.
     * 
     * @param packet the packet to read
     * @param offset the offset from which new data will be inserted
     * @return the number of actualy read bytes.
     * @throws java.io.IOException
     */
    private int readPacket(byte[] packet, int offset, int psize) throws IOException {
        int length = 0;
        try {
            while (length < psize) {
                int len = inStream.read(packet, offset + length, psize - length);
                if (len == -1) {
                    return length;
                }
                length += len;
            }
            return length;
        } catch (Exception e) {
            logger.error(e);
        }
        return length;
    }

    private void padding(byte[] data, int count) {
        int offset = data.length - count;
        for (int i = 0; i < count; i++) {
            data[i + offset] = paddingByte;
        }
    }

    @Override
    public Frame process(long timestamp) throws IOException {
        if (first) {
            if (timestamp > 0) {
                skip(timestamp);
            }
            first = false;
        }

        Frame frame = Memory.allocate(frameSize);
        byte[] data = frame.getData();
        if (data == null) {
            data = new byte[frameSize];
        }

        int len = readPacket(data, 0, frameSize);
        totalRead += len;
        if (len == 0) {
            eom = true;
        }

        if (len < frameSize) {
            padding(data, frameSize - len);
            eom = true;
        }

        // will not generate empty packet next time
        if (totalRead >= sizeOfData) {
            eom = true;
        }

        frame.setOffset(0);
        frame.setLength(frameSize);
        frame.setEOM(eom);
        frame.setDuration(period * 1000000L);
        frame.setFormat(format);

        return frame;
    }

    @Override
    public void close() {
        try {
            inStream.close();
        } catch (Exception e) {
            logger.error("Could not close .wav track properly.", e);
        }
    }

    @Override
    public Format getFormat() {
        return format;
    }
}
