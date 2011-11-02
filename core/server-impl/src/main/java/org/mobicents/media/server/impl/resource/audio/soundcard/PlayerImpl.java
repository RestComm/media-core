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
package org.mobicents.media.server.impl.resource.audio.soundcard;

import java.io.IOException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFormat.Encoding;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.events.NotifyEvent;
import org.mobicents.media.server.spi.rtp.AVProfile;
import org.xiph.speex.spi.SpeexEncoding;

/**
 * PlayerImpl is to play audio to hardware
 * 
 * @author amit bhayani
 * 
 */
public class PlayerImpl extends AbstractSink {

//    private static final Logger logger = Logger.getLogger(PlayerImpl.class);
    private final static Format[] FORMATS = new Format[]{
        AVProfile.L16_MONO,
        AVProfile.L16_STEREO,
        Codec.LINEAR_AUDIO
    };
    private final static Encoding GSM_ENCODING = new Encoding("GSM0610");
    private volatile boolean first = true;
    boolean bigEndian = false;
    private SourceDataLine sourceDataLine = null;
    private boolean isAcceptable = false;
    private Format fmt;
    private javax.sound.sampled.AudioFormat audioFormat = null;

    public PlayerImpl(String name) {
        super(name);
    }

    private javax.sound.sampled.AudioFormat.Encoding getEncoding(String encodingName) {
        if (encodingName.equalsIgnoreCase(AudioFormat.ALAW)) {
            return javax.sound.sampled.AudioFormat.Encoding.ALAW;
        } else if (encodingName.equalsIgnoreCase(AudioFormat.ULAW)) {
            return javax.sound.sampled.AudioFormat.Encoding.ULAW;
        } else if (encodingName.equalsIgnoreCase(AudioFormat.SPEEX)) {
            return SpeexEncoding.SPEEX;
        } else if (encodingName.equalsIgnoreCase(AudioFormat.GSM)) {
            return GSM_ENCODING;
        } else {
            return javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
        }
    }

    @Override
    public void onMediaTransfer(Buffer buffer) throws IOException {

        if (first) {
            first = false;

            AudioFormat fmt = (AudioFormat) buffer.getFormat();

            float sampleRate = (float) fmt.getSampleRate();
            int sampleSizeInBits = fmt.getSampleSizeInBits();
            int channels = fmt.getChannels();
            int frameSize = (fmt.getFrameSizeInBits() / 8);
            float frameRate = (float) fmt.getFrameRate();
            boolean bigEndian = fmt.getEndian() == 1;
            Encoding encoding = getEncoding(fmt.getEncoding());

            frameSize = (channels == AudioSystem.NOT_SPECIFIED || sampleSizeInBits == AudioSystem.NOT_SPECIFIED) ? AudioSystem.NOT_SPECIFIED
                    : ((sampleSizeInBits + 7) / 8) * channels;

            audioFormat = new javax.sound.sampled.AudioFormat(encoding, sampleRate, sampleSizeInBits, channels,
                    frameSize, sampleRate, bigEndian);

            // FIXME : Need a configuration to select the specific hardware
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

            // TODO : Should getting the SourceDataLine go in start() In which case its configurable to know the Formats
            // beforehand.
            try {
                sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();

            } catch (LineUnavailableException e) {
                logger.error(e);
                this.failed(NotifyEvent.RX_FAILED, e);
                this.stop();

            } catch (IllegalArgumentException e) {
                logger.error(e);
                this.failed(NotifyEvent.RX_FAILED, e);
                this.stop();

            }
        }

        // FIXME : write() will block till all bytes are written. Need async operation here.
        byte[] data = buffer.getData();
        try {
            sourceDataLine.write(data, buffer.getOffset(), buffer.getLength());
        } catch (IllegalArgumentException e) {
            logger.error(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.error(e);
        }

    }

    public Format[] getFormats() {
        return FORMATS;
    }

    public boolean isAcceptable(Format format) {

        if (fmt != null && fmt.matches(format)) {
            return isAcceptable;
        }
        for (int i = 0; i < FORMATS.length; i++) {
            if (FORMATS[i].matches(format)) {
                fmt = format;
                isAcceptable = true;
                return isAcceptable;
            }
        }
        isAcceptable = false;
        return isAcceptable;
    }

    @Override
    public void stop() {
        super.stop();
        first = false;
        if (sourceDataLine != null) {
            sourceDataLine.drain();
            sourceDataLine.close();
        }
    }

    @Override
    public void start() {
        super.start();
        first = true;
    }
}
