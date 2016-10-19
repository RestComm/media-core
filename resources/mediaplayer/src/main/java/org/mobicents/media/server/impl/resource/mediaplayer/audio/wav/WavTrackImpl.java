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
import org.mobicents.media.server.impl.resource.mediaplayer.audio.RemoteStreamProvider;
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
    private InputStream inStream;
    private int period = 20;
    private boolean eom;
    private long duration;
    private int sizeOfData;
    private boolean first = true;
    protected int frameSize;
    protected int totalRead = 0;
    
    /*Buffer, Channel, Header */
    private final static int BUFFER_SIZE = 1024;
    private ReadableByteChannel rbChannel;
    private int bytesReadFromChannel;
    private ByteBuffer buff = ByteBuffer.allocate(BUFFER_SIZE);
    private int headerSize = 46 ; // assuming  we have cannonical format(mono pcm)
    private byte[] header = new byte[headerSize];
    private byte[] headerEnd = null;
    private int buffPos = 0;
    
    /*Padding for different stream types.*/
    private final static byte PCM_PADDING_BYTE = 0;
    private final static byte ALAW_PADDING_BYTE = (byte) 0xD5;
    private final static byte ULAW_PADDING_BYTE = (byte) 0xFF;
    private final static byte[] factBytes = new byte[] { 0x66, 0x61, 0x63, 0x74 };
    private byte paddingByte = PCM_PADDING_BYTE;
    private static final Logger logger = Logger.getLogger(WavTrackImpl.class);
    
    
    public WavTrackImpl (URL url,  RemoteStreamProvider streamProvider) throws IOException, UnsupportedAudioFileException {
        
        inStream = streamProvider.getStream(url);
        rbChannel = Channels.newChannel(inStream);
        getAudioFormat(rbChannel);
       
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
        
        int byteIndex = 0;
        
        // Initial buffer read, set position to 0 and fill the header
        bytesReadFromChannel=rbc.read(buff);
        buff.position(0);
        buff.get(header, 0, header.length);
        buff.order(ByteOrder.LITTLE_ENDIAN);

        // Determines the Subchunk size, value for pcm is 18
        // ckSize bytes: 16,17,18,19
        int ckSize = (header[16] & 0xFF) | ((header[17] & 0xFF) << 8) 
                     | ((header[18] & 0xFF) << 16) | ((header[19] & 0xFF) << 24);
        
        // Format bytes:20,21
        // Value for pcm(mono) is 1. 
        // Values other than 1 indicate some form of compression.
        int formatValue = (header[20] & 0xFF) | ((header[21] & 0xFF) << 8);
        
        // Number of channels bytes: 22,23
        // Mono=1, Stereo=2.
        int channels = (header[22] & 0xFF) | ((header[23] & 0xFF) << 8);
        
        // Bits per sample bytes: 34,35
        // Value for pcm is 16 bit/sample
        int bitsPerSample = (header[34] & 0xFF) | ((header[35] & 0xFF) << 8);
        
        // Sample rate bytes: 24,25,26,27
        // Labeled also as nSamplesPerSec(F).   
        // Value for pcm(mono) is 8000 and stereo 44100
        int sampleRate = (header[24] & 0xFF) | ((header[25] & 0xFF) << 8) 
                         | ((header[26] & 0xFF) << 16) | ((header[27] & 0xFF) << 24);
        
        // chunkSize bytes: 4,5,6,7
        // This is the size of the entire file in bytes minus 8 bytes for the
        // two fields not included in this count:ChunkIDand ChunkSize.
        sizeOfData = (header[4] & 0xFF) | ((header[5] & 0xFF) << 8) 
                     | ((header[6] & 0xFF) << 16) | ((header[7] & 0xFF) << 24);
        
        // SubChunk2Size bytes: 40,41,42,43
        // M*Nc*Ns (Nc = channels, Ns = total number of blocks, Nc= block samples)
        int sampleData = (header[40] & 0xFF) | ((header[41] & 0xFF) << 8) 
                     | ((header[42] & 0xFF) << 16) | ((header[43] & 0xFF) << 24);
        
        sizeOfData -= (bytesReadFromChannel-8);
        
        this.getFormat(formatValue,channels,bitsPerSample,sampleRate);
        
        /*Gets the last 10 bytes from the header 
        and transfers data to headerEnd[] array.*/
        
        headerEnd = new byte[8 + ckSize - 16];
        buff.position(header.length - headerEnd.length);
        buff.get(headerEnd, 0, headerEnd.length);
        
        byteIndex = headerEnd.length - 4 - factBytes.length;
        boolean hasFact = true;
        for (int i = 0; i < factBytes.length; i++) {
            if (factBytes[i] != headerEnd[byteIndex++]) {
                hasFact = false;
                break;
            }
        }

        // All compressed non-PCM formats have fact chunk.
        // Fuct chunk is 12 bytes long.

        if (hasFact) {
            // skip fact chunk
            sizeOfData -= 12;
            headerEnd = new byte[12];
            buff.position(header.length);
            buff.get(headerEnd, 0, headerEnd.length);
           
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
            int buffSize = header.length + skip.length;
                        
            if (buff.capacity()< skip.length){
                ByteBuffer newBuff = ByteBuffer.allocate(buffSize); 
                newBuff.put((ByteBuffer) buff.flip()); 
                buff = newBuff;
                
            } 

            buff.position(header.length);
            int bytesRead = rbChannel.read(buff);
            
            while (bytesRead < skip.length && buff.remaining() > 0) {

                int len = rbChannel.read(ByteBuffer.wrap(skip, bytesRead , skip.length - bytesRead));
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
    public int readPacket(byte[] packet, int offset, int psize) throws IOException {
        
        int length = 0;
        int len=0;
       
        int packetSize = psize + offset + headerSize;
        
        if (buff.capacity()< psize){
            ByteBuffer newBuff = ByteBuffer.allocate(packetSize); 
            newBuff.put((ByteBuffer) buff.flip()); 
            buff = newBuff;
        }
        
        try {
            buff.position(offset);
            len = rbChannel.read(ByteBuffer.wrap(packet,offset,psize));
            if (len == -1){
                return length;
            }
            length += len;
            
            
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
    
    /* Overridable methods*/
    
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

        Frame frame = Memory.allocate(frameSize);
        byte[] data = frame.getData();
        if (data == null) {
            data = new byte[frameSize];
        }

        int len = readPacket(data, 0, frameSize);
        totalRead = totalRead + len;
        
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
