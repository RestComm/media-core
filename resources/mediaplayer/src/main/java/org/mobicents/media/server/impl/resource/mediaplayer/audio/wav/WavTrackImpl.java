/*
 * Copyright (C) 2016 TeleStax, Inc..
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.mobicents.media.server.impl.resource.mediaplayer.audio.wav;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.Selector;
import java.nio.channels.ReadableByteChannel; 
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.resource.mediaplayer.Track;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
/**
 *
 * @author apollo
 */
public class WavTrackImpl implements Track{
    
    /*audio stream*/
    private AudioFormat format;
    private int period = 20;
    private int frameSize;
    private boolean eom;
    private long duration;
    private int totalRead = 0;
    private int sizeOfData;
    private boolean first = true;
    
    private ReadableByteChannel rbChannel;
    private ByteBuffer buff;
   
    private final static int BUFFER_SIZE = 128;
    
    // Padding for different stream types.
    private final static byte PCM_PADDING_BYTE = 0;
    private final static byte ALAW_PADDING_BYTE = (byte) 0xD5;
    private final static byte ULAW_PADDING_BYTE = (byte) 0xFF;

    private final static byte[] factBytes = new byte[] { 0x66, 0x61, 0x63, 0x74 };
    private byte paddingByte = PCM_PADDING_BYTE;
    private static final Logger logger = Logger.getLogger(WavTrackImpl.class);
    
    public WavTrackImpl (URL url) throws IOException, UnsupportedAudioFileException {
        
        rbChannel=Channels.newChannel(url.openStream());
        buff = ByteBuffer.allocate(BUFFER_SIZE); 
        
        getAudioFormat(rbChannel);
        if (format == null) {
            throw new UnsupportedAudioFileException();
        }
       
    }

    private void getAudioFormat(ReadableByteChannel rbc) throws IOException {
        byte[] header = new byte[36];
        byte[] headerEnd = null;
       
        int bytesReadFromChannel = 0 ;
        int bytesRead = 0;
        int count = 0;
        
        bytesReadFromChannel=rbc.read(buff);
        buff.clear();
        
        while (count < 36 && buff.remaining() > 0) {
            //count=rbc.read(buffer);
            header[count]=buff.get(count);
            count++;
        }
        buff.order(ByteOrder.LITTLE_ENDIAN);

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
        extraHeaderSize = headerEnd.length;
        int bytesReadExtra = 0;
        
        int headerSize=header.length;
        int buffLimit = headerSize + extraHeaderSize;
        buff.limit(buffLimit);
        buff.position(headerSize);

        while (bytesReadExtra < extraHeaderSize && buff.remaining()> 0) {
            
            int len = rbc.read(buff.get(headerEnd));
            if (len == -1) {
                return;
            }
            bytesReadExtra += len;
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
            buffLimit += 12;
            buff.limit(buffLimit);
            buff.position(buffLimit-12);
            
            while (bytesRead < 12 && buff.remaining() > 0) {

                int len = rbc.read(buff.get(headerEnd));
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
        
        // don't close the channel
    }
    
    public void setPeriod(int period) {
        this.period = period;
        frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);
    }

    public int getPeriod() {
        return period;
    }
    
    @Override
    public Format getFormat() {
        return format;
    }

    @Override
    public long getMediaTime() {
        return 0;
    }

    @Override
    public void setMediaTime(long timestamp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long getDuration() {
        return duration;
    }
    
    private void skip(long timestamp) {
        try {
            long offset = frameSize * (timestamp / period / 1000000L);
            byte[] skip = new byte[(int) offset];
            int bytesRead = 0;
            
            int header = 46;
            int buffSize = skip.length + header;
            
            if (buff.capacity()< skip.length){
                ByteBuffer newBuff = ByteBuffer.allocate(buffSize); 
                newBuff.put((ByteBuffer) buff.flip()); 
                buff = newBuff;
            }   

            while (bytesRead < skip.length && buff.remaining() > 0) {
                buff.position(header);
                buff.limit(buffSize);
                int len = rbChannel.read(ByteBuffer.wrap(skip));
                if (len == -1)
                    return;

                totalRead += len;
                bytesRead += len;
            }

        } catch (IOException e) {
            logger.error(e);
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
        offset += 46;
        int packetSize = psize + offset;
        
        if (buff.capacity()< psize){
            ByteBuffer newBuff = ByteBuffer.allocate(packetSize); 
            newBuff.put((ByteBuffer) buff.flip()); 
            buff = newBuff;
        }
        
        try {
            while (length < psize && buff.remaining() > 0) {
                
                buff.position(offset);
                buff.limit(packetSize);
                int len = rbChannel.read(ByteBuffer.wrap(packet));
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

//      will not generate empty packet next time
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
            rbChannel.close();
        } catch (Exception e) {
            logger.error("Could not close .wav track properly.", e);
        }
    }
    
}
