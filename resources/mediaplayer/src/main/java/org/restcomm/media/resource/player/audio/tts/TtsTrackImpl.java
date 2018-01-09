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

package org.restcomm.media.resource.player.audio.tts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import javax.sound.sampled.AudioInputStream;

import com.sun.speech.freetts.Voice;

import org.restcomm.media.resource.player.Track;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author kulikov
 * @author amit bhayani
 * @author baranowb
 */
public class TtsTrackImpl implements Track {

    /** audio stream */
    private transient AudioInputStream stream = null;
    private AudioFormat format = FormatFactory.createAudioFormat("linear", 8000, 16,1);

    private int period = 20;
    private int frameSize;
    private boolean eom;
    private boolean isReady = false;
    private Vector<byte[]> outputList;
    private Voice voice;
    private long duration;
    private long timestamp;
    private VoicesCache voiceCache;
    
    private static final Logger logger = LogManager.getLogger(TtsTrackImpl.class);
    
    public TtsTrackImpl(URL url, String voiceName,VoicesCache vc) throws IOException {
    	this.voiceCache = vc;

        isReady = false;
        URLConnection connection = url.openConnection();

        frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);

        // creating voice
        //VoiceManager voiceManager = VoiceManager.getInstance();
      //voice = voiceManager.getVoice(voiceName);
        voice = voiceCache.allocateVoice(voiceName);
        //this.voice.allocate();


        // creating speech buffer for writting
        TTSAudioBuffer audioBuffer = new TTSAudioBuffer();

        // assign buffer to speech engine and start generation
        // produced media data will be stored in the audio buffer

        this.voice.setAudioPlayer(audioBuffer);
        this.voice.speak(connection.getInputStream());
        audioBuffer.flip();


    }

    public TtsTrackImpl(String text, String voiceName,VoicesCache vc) {
    	this.voiceCache = vc;
        isReady = false;
        // creating voice
        //VoiceManager voiceManager = VoiceManager.getInstance();
        //voice = voiceManager.getVoice(voiceName);
        voice = voiceCache.allocateVoice(voiceName);

        //voice.allocate();
        // creating speech buffer for writting
        TTSAudioBuffer audioBuffer = new TTSAudioBuffer();

        // assign buffer to speech engine and start generation
        // produced media data will be stored in the audio buffer
        voice.setAudioPlayer(audioBuffer);
        voice.speak(text);


        audioBuffer.flip();
        frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);


    }

    public void setPeriod(int period) {
        this.period = period;
        frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);
    }

    public int getPeriod() {
        return period;
    }

    public long getMediaTime() {
        return timestamp;
    }

    public void setMediaTime(long timestamp) {
        this.timestamp = timestamp;
        try {
            stream.reset();
            long offset = frameSize * (timestamp / period);
            stream.skip(offset);
        } catch (IOException e) {
        	logger.error(e);
        }
    }

    public long getDuration() {
        return duration;
    }

    /**
     * Reads packet from currently opened stream.
     * 
     * @param packet
     *            the packet to read
     * @param offset
     *            the offset from which new data will be inserted
     * @return the number of actualy read bytes.
     * @throws java.io.IOException
     */
    private int readPacket(byte[] packet, int offset, int psize)
            throws IOException {
        int length = 0;
        try {
            while (length < psize) {
                int len = stream.read(packet, offset + length, psize - length);
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
            data[i + offset] = 0;
        }
    }

    private void switchEndian(byte[] b, int off, int readCount) {

        for (int i = off; i < (off + readCount); i += 2) {
            byte temp;
            temp = b[i];
            b[i] = b[i + 1];
            b[i + 1] = temp;
        }

    }

    public Frame process(long timestamp) throws IOException {
        if (!isReady) {
            return null;
        }

        Frame frame = Memory.allocate(frameSize);
        byte[] data = frame.getData();

        int len = readPacket(data, 0, frameSize);
        if (len == 0) {
            eom = true;
        }

        // switchEndian(data, 0, data.length);

        if (len < frameSize) {
            padding(data, frameSize - len);
            eom = true;
        }

        frame.setOffset(0);
        frame.setLength(frameSize);
        frame.setEOM(eom);
        frame.setDuration(20);
        return frame;
    }

    public void close() {
        try {
            //voice.deallocate();

            voiceCache.releaseVoice(this.voice);
            this.voice = null;
            stream.close();
        } catch (Exception e) {
        }
    }

    private class TTSAudioBuffer implements
            com.sun.speech.freetts.audio.AudioPlayer {

        private javax.sound.sampled.AudioFormat fmt;
        private float volume;
        private byte[] localBuff;
        private int curIndex = 0;
        private int totalBytes = 0;

        public TTSAudioBuffer() {
            outputList = new Vector<byte[]>();
        }

        public void setAudioFormat(javax.sound.sampled.AudioFormat fmt) {
            this.fmt = fmt;
        }

        public javax.sound.sampled.AudioFormat getAudioFormat() {
            return fmt;
        }

        public void pause() {
        }

        public void resume() {
        }

        public void reset() {
            curIndex = 0;
            localBuff = null;
            isReady = false;
        }

        public boolean drain() {
            return true;
        }

        public void begin(int size) {
            localBuff = new byte[size];
            curIndex = 0;
        }

        public boolean end() {
            outputList.add(localBuff);

            totalBytes += localBuff.length;

            System.out.println("end() called totalBytes = " + totalBytes);

            isReady = true;
            return true;
        }

        public void cancel() {
            //System.out.println("cancel() called");
        }

        public void close() {
            //System.out.println("Close() called");
        }

        public void flip() {
            //System.out.println("flip() called");

            byte[] rawData = null;

            if (outputList.size() == 1) {
                rawData = outputList.firstElement();
            } else {
                int offSet = 0;
                rawData = new byte[totalBytes];
                for (byte[] byteArr : outputList) {
                    System.arraycopy(byteArr, 0, rawData, offSet, byteArr.length);
                    offSet += byteArr.length;
                }
            }

            //System.out.println("Format = " + fmt);

            // If its BigEndian lets convert it to little-endian first
            if (fmt.isBigEndian()) {
                switchEndian(rawData, 0, rawData.length);
                fmt = new javax.sound.sampled.AudioFormat(fmt.getEncoding(),
                        fmt.getSampleRate(), fmt.getSampleSizeInBits(), fmt.getChannels(), fmt.getFrameSize(), fmt.getFrameRate(), false);
                //System.out.println("Converted Format to little-endian = " + fmt);
            }

            // duration = (long)(totalBytes/320 * 20);
            duration = (long) (totalBytes * 1000 / (fmt.getSampleSizeInBits() / 8 * fmt.getSampleRate()));
            

            if (fmt.getSampleRate() != 8000) {
                double originalFrequency = fmt.getSampleRate();
                double targetFrequency = 8000;
                
                //discrete step, between samples
                double targetDX = 1/targetFrequency;
                double originalDX = 1/originalFrequency;
                
                //number of bytes per sample
                int byteCount = fmt.getSampleSizeInBits() / 8;
                
                //total number of samples
                int originalSampleCount = totalBytes/byteCount;
       
                //converting raw data to array of samples
                int[] originalSignal = new int[originalSampleCount];
                int j = 0;
                //create original signal.
                for (int i = 0; i < originalSignal.length; i++) {
                    originalSignal[i] = (rawData[j++] & 0xff) | (rawData[j++] << 8);
                }

                //determine the length of the resamples signal
                double ratio = fmt.getSampleRate() / 8000;
                int count = (int) (originalSignal.length / ratio);

                //creating array for resamples signal
                int[] resampledSignal = new int[count];

                for(int k = 0;k<count;k++)
                {
                	double xk = (double)targetDX * k;
                	//count how many full dx we have, its index of p, p+1 will be over xk in time domain
                	int i = (int) ( xk/originalDX); //round up down, always
                	double tang = (originalSignal[i+1] - originalSignal[i])/(originalDX); //determine tang
                	//add orignial sig + difference in xk in time domain
                	double resampled = originalSignal[i] + (xk - i*originalDX)* tang;
                	resampledSignal[k] = (int) resampled;
        
                }
      
                
            
                rawData = new byte[resampledSignal.length * 2];

                j = 0;
                for (int i = 0; i < resampledSignal.length; i++) {
                    rawData[j++] = (byte) (resampledSignal[i]);
                    rawData[j++] = (byte) (resampledSignal[i] >> 8);
                }
                
                javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(
                        fmt.getEncoding(), 8000, fmt.getSampleSizeInBits(), fmt.getChannels(), fmt.getFrameSize(), 8000, fmt.isBigEndian());

               // System.out.println("Traget Format = " + targetFormat);
            }

            InputStream is = new ByteArrayInputStream(rawData);
            stream = new AudioInputStream(is, fmt, rawData.length / fmt.getFrameSize());

        }

        public float getVolume() {
            return volume;
        }

        public void setVolume(float volume) {
            this.volume = volume;
        }

        public long getTime() {
            return 0;
        }

        public void resetTime() {
        }

        public void startFirstSampleTimer() {
        }

        public boolean write(byte[] buff) {
            return write(buff, 0, buff.length);
        }

        public boolean write(byte[] buff, int off, int len) {
            System.arraycopy(buff, off, localBuff, curIndex, len);
            curIndex += len;
            return true;
        }

        public void showMetrics() {
        }
    }

    public Format getFormat() {
        return format;
    }
   
}
