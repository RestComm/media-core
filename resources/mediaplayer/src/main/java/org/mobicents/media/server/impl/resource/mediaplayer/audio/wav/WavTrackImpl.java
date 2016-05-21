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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel; 

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
    
    /*Audio stream*/
    private AudioFormat format;
    private int period = 20;
    private int frameSize;
    private boolean eom;
    private long duration;
    private int totalRead = 0;
    private int sizeOfData;
    private boolean first = true;
    
    /*Buffer, Channel, Header */
    private final static int BUFFER_SIZE = 128;
    private ReadableByteChannel rbChannel;
    private ByteBuffer buff = ByteBuffer.allocate(BUFFER_SIZE);
    private byte[] header = new byte[46];
    private byte[] headerEnd = null;
    int buffPos = 0;
    
    /*Padding for different stream types.*/
    private final static byte PCM_PADDING_BYTE = 0;
    private final static byte ALAW_PADDING_BYTE = (byte) 0xD5;
    private final static byte ULAW_PADDING_BYTE = (byte) 0xFF;
    private final static byte[] factBytes = new byte[] { 0x66, 0x61, 0x63, 0x74 };
    private byte paddingByte = PCM_PADDING_BYTE;
    private static final Logger logger = Logger.getLogger(WavTrackImpl.class);
    
    
    public WavTrackImpl (URL url) throws IOException, UnsupportedAudioFileException {
        
        setRbChannel(url.openStream());
        getAudioFormat(getRbChannel());
       
    }
    
    /**
        Reads data from channel into buffer and transfers data to header[] array.
        bytesRead in this case (using bulk method) returns the amount which is available
        and not the number of actually read bytes. The buffer position must be set to some offset.
        The offset in the bulk method is the offset in the array where the data is stored.
        Before getting the data in the array, the whole channel is read (bytesReadFromChannel),
        to avoid BufferUnderflow. 
    */

    private void getAudioFormat(ReadableByteChannel rbc) throws IOException, UnsupportedAudioFileException {
        
        int bytesRead = 0;
        int bytesCount = 0;
        int byteIndex = 0;
        int bytesReadFromChannel=rbc.read(getBuff());
        
        setBuffPos(0);
        
        while (bytesCount < getHeader().length && getBuff().remaining() > 0) {
            
            bytesRead = rbc.read(getBuff().get(getHeader(), 0, getHeader().length));
            if (bytesRead == -1) {
                return;
            }
            bytesCount = bytesReadFromChannel - bytesRead; 

        }
        getBuff().order(ByteOrder.LITTLE_ENDIAN);

        // ckSize 16,17,18,19
        int ckSize = (getHeader(16) & 0xFF) | ((getHeader(17) & 0xFF) << 8) 
                     | ((getHeader(18) & 0xFF) << 16) | ((getHeader(19) & 0xFF) << 24);
        // format 20,21
        int formatValue = (getHeader(20) & 0xFF) | ((getHeader(21) & 0xFF) << 8);
        // channels 22,23
        int channels = (getHeader(22) & 0xFF) | ((getHeader(23) & 0xFF) << 8);
        // bits per sample 34,35
        int bitsPerSample = (getHeader(34) & 0xFF) | ((getHeader(35) & 0xFF) << 8);
        // sample rate 24,25,26,27
        int sampleRate = (getHeader(24) & 0xFF) | ((getHeader(25) & 0xFF) << 8) 
                         | ((getHeader(26) & 0xFF) << 16) | ((getHeader(27) & 0xFF) << 24);
        // size of data bytes 4,5,6,7
        sizeOfData = (getHeader(4) & 0xFF) | ((getHeader(5) & 0xFF) << 8) 
                     | ((getHeader(6) & 0xFF) << 16) | ((getHeader(7) & 0xFF) << 24);
        
        sizeOfData -= 12;
        sizeOfData -= ckSize;
        
        this.getFormat(formatValue,channels,bitsPerSample,sampleRate);
        
        /*Gets the last 10 bytes from the header 
        and transfers data to headerEnd[] array.*/
        
        setHeaderEnd(new byte[8 + ckSize - 16]);
        setBuffPos(getHeader().length - getHeaderEnd().length);
        bytesCount = 0;
       
        while (bytesCount < getHeaderEnd().length && getBuff().remaining()> 0) {
            
            bytesRead = rbc.read(getBuff().get(getHeaderEnd(), 0, getHeaderEnd().length));
            if (bytesRead == -1){
                return;
            }
            bytesCount = bytesReadFromChannel - bytesRead; 
        }
        
        byteIndex = getHeaderEnd().length - 4 - getFactBytes().length;
        boolean hasFact = true;
        for (int i = 0; i < getFactBytes().length; i++) {
            if (getFactBytes(i) != getHeaderEnd(byteIndex++)) {
                hasFact = false;
                break;
            }
        }

        if (hasFact) {
            // skip fact chunk
            sizeOfData -= 12;
            setHeaderEnd(new byte[12]);
            setBuffPos(getHeader().length);
            bytesCount = 0;
            
            while (bytesCount < getHeaderEnd().length && getBuff().remaining() > 0) {

                bytesRead = rbc.read(getBuff().get(getHeaderEnd(), 0, getHeaderEnd().length));
                if (bytesRead == -1) {
                    return;
                }
                bytesCount = bytesReadFromChannel - bytesRead;                
            }
        }
        // don't close the channel
    }
    
    private void getFormat(int formatValue, int channels, int bitsPerSample, int sampleRate) throws UnsupportedAudioFileException {
        
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
        
        if (format != null) {
            frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);
            duration = sizeOfData * period * 1000000L / frameSize;
        } else {
            throw new UnsupportedAudioFileException();
        }
    }
    
    private void skip(long timestamp) {
        try {
            long offset = frameSize * (timestamp / period / 1000000L);
            byte[] skip = new byte[(int) offset];
            int buffSize = getHeader().length + skip.length;
            int bytesRead = 0;
            
            if (getBuff().capacity()< skip.length){
                ByteBuffer newBuff = ByteBuffer.allocate(buffSize); 
                newBuff.put((ByteBuffer) getBuff().flip()); 
                setBuff(newBuff);
            } 
 
            setBuffPos(getHeader().length);
            while (bytesRead < skip.length && getBuff().remaining() > 0) {

                int len = getRbChannel().read(ByteBuffer.wrap(skip, 0, skip.length));
                if (len == -1) {
                    return;
                }

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
     * @return the number of actually read bytes.
     * @throws java.io.IOException
     */
    private int readPacket(byte[] packet, int offset, int psize) throws IOException {
        
        int length = 0;
        offset += 46;
        int packetSize = psize + offset;
        
        if (getBuff().capacity()< psize){
            ByteBuffer newBuff = ByteBuffer.allocate(packetSize); 
            newBuff.put((ByteBuffer) getBuff().flip()); 
            setBuff(newBuff);
        }
        
        try {
            setBuffPos(offset);
            while (length < psize && getBuff().remaining() > 0) {

                int len = getRbChannel().read(ByteBuffer.wrap(packet,0,psize));
                if (len == -1){
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
    
    /* Getters, Setters, Overridable methods*/

    public int getFrameSize() {
        return frameSize;
    }
    
    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
    
    public static byte[] getFactBytes() {
        return factBytes;
    }
    
    public static byte getFactBytes(int i) {
        return factBytes[i];
    }
    
    public byte[] getHeader() {
        return header;
    }
    public byte getHeader(int i) {
        return header[i];
    }

    public byte[] getHeaderEnd() {
        return headerEnd;
    }
    
    public byte getHeaderEnd(int i) {
        return headerEnd[i];
    }
    
    public void setHeader(byte[] header) {
        this.header = header;
    }

    public void setHeaderEnd(byte[] headerEnd) {
        this.headerEnd = headerEnd;
    }

    private ReadableByteChannel getRbChannel()  {
        return rbChannel;
    }

    private void setRbChannel(InputStream inStream) {
        this.rbChannel = Channels.newChannel(inStream);
    }
    
    public ByteBuffer getBuff() {
        return buff;
    }

    public void setBuff(ByteBuffer buff) {
        this.buff = buff;
    }
    
    public void setBuffPos(int buffPos) {
        this.buffPos = buffPos;
        this.getBuff().position(buffPos);
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

    @Override
    public Frame process(long timestamp) throws IOException {
        if (first) {
            if (timestamp > 0) {
                skip(timestamp);
            }
            first = false;
        }

        Frame frame = Memory.allocate(getFrameSize());
        byte[] data = frame.getData();
        if (data == null) {
            data = new byte[getFrameSize()];
        }

        int len = readPacket(data, 0, getFrameSize());
        totalRead += len;
        if (len == 0) {
            eom = true;
        }

        if (len < getFrameSize()) {
            padding(data, getFrameSize() - len);
            eom = true;
        }

//      will not generate empty packet next time
        if (totalRead >= sizeOfData) {
            eom = true;
        }
        
        frame.setOffset(0);
        frame.setLength(getFrameSize());
        frame.setEOM(eom);
        frame.setDuration(getPeriod() * 1000000L);
        frame.setFormat(format);

        return frame;
    }

    @Override
    public void close() {
        try {
            getRbChannel().close();
        } catch (Exception e) {
            logger.error("Could not close .wav track properly.", e);
        }
    }
    
}
