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
package org.mobicents.media.server.impl.resource.mediaplayer.audio.wav;

import org.mobicents.media.Format;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.mobicents.media.Buffer;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.format.UnsupportedFormatException;
import org.mobicents.media.server.impl.resource.mediaplayer.Track;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 *
 * @author kulikov
 */
public class WavTrackImpl implements Track {

    /** audio stream */
    private transient AudioInputStream stream = null;
    private AudioFormat format;
    private int period = 20;
    private int frameSize;
    private boolean eom;
    private long timestamp;
    private long duration;
    
    private boolean first = true;
    private SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss,SSS");
    public WavTrackImpl(URL url) throws UnsupportedAudioFileException, IOException, UnsupportedFormatException {
        stream = AudioSystem.getAudioInputStream(url);
        duration = (long)(stream.getFrameLength()/stream.getFormat().getFrameRate() * 1000);
        format = getFormat(stream);
        if (format == null) {
            throw new UnsupportedFormatException(format);
        }

        frameSize = (int) (period * format.getChannels() * format.getSampleSizeInBits() *
                format.getSampleRate() / 8000);
    }

    public void setPeriod(int period) {
        this.period = period;
        frameSize = (int) (period * format.getChannels() * format.getSampleSizeInBits() *
                format.getSampleRate() / 8000);
    }

    public int getPeriod() {
        return period;
    }

    public long getMediaTime() {
        return timestamp;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setMediaTime(long timestamp) {
        this.timestamp = timestamp;
        try {
            long offset = frameSize * (timestamp / period);
            byte[] skip = new byte[(int)offset];
            stream.read(skip);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private AudioFormat getFormat(AudioInputStream stream) {
        Encoding encoding = stream.getFormat().getEncoding();
        if (encoding == Encoding.ALAW) {
            return (AudioFormat) AVProfile.PCMA;
        } else if (encoding == Encoding.ULAW) {
            return (AudioFormat) AVProfile.PCMU;
        } else if (encoding == Encoding.PCM_SIGNED) {
            int sampleSize = stream.getFormat().getSampleSizeInBits();
            int sampleRate = (int) stream.getFormat().getSampleRate();
            int channels = stream.getFormat().getChannels();
            return new AudioFormat(AudioFormat.LINEAR, sampleRate, sampleSize, channels,
                    AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
        }
        return null;
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
    private int readPacket(byte[] packet, int offset, int psize) throws IOException {
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
            e.printStackTrace();
        }
        return length;
    }

    private void padding(byte[] data, int count) {
        int offset = data.length - count;
        for (int i = 0; i < count; i++) {
            data[i + offset] = 0;
        }
    }
    
    public void process(Buffer buffer) throws IOException {
        if (first) {
            first = false;
            System.out.println("start:" + fmt.format(new Date()));
        }
        byte[] data = buffer.getData();
        if (data == null) {
            data = new byte[frameSize];
        }
        buffer.setData(data);
        
        int len = readPacket(data, 0, frameSize);
        if (len == 0) {
            eom = true;
            System.out.println("stop: " + fmt.format(new Date()));
        }

        if (len < frameSize) {
            padding(data, frameSize - len);
            eom = true;
        }

        buffer.setOffset(0);
        buffer.setLength(frameSize);
        buffer.setEOM(eom);
        buffer.setDuration(period);
        
        //update timestamp
        timestamp += period;
    }

    public void close() {
        try {
            stream.close();
        } catch (Exception e) {
        }
    }

    public Format getFormat() {
        return format;
    }
}
