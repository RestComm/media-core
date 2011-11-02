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
package org.mobicents.media.server.impl.resource.mediaplayer.audio;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import org.apache.log4j.Logger;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.server.Utils;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.resource.mediaplayer.Track;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.gsm.GsmTrackImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.mpeg.AMRTrackImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.tts.TtsTrackImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.tts.VoicesCache;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.wav.WavTrackImpl;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.CodecFactory;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.resource.TTSEngine;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 * @author baranowb
 * @author Oleg Kulikov
 */
public class AudioPlayerImpl extends AbstractSource implements Player, TTSEngine {

    private final static AudioFormat LINEAR_AUDIO = new AudioFormat(AudioFormat.LINEAR, 8000, 16, 1,
            AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
    private final static ArrayList<String> mediaTypes = new ArrayList();
    

    static {
        mediaTypes.add("audio");
    }
    /** supported formats definition */
    private final static Format[] FORMATS = new Format[]{AVProfile.L16_MONO, AVProfile.L16_STEREO, AVProfile.PCMA,
        AVProfile.PCMU, AVProfile.SPEEX, AVProfile.GSM, LINEAR_AUDIO
    };
    private Track track;
    private Codec codec;
    private String audioMediaDirectory;
    private VoicesCache voicesCache;
    //private final static ArrayList<CodecFactory> codecFactories = new ArrayList();
    private final static CodecFactory[] codecFactories;
    
    private Logger logger = Logger.getLogger(AudioPlayerImpl.class);

    static {
        codecFactories = new CodecFactory[]{
                    new org.mobicents.media.server.impl.dsp.audio.g711.alaw.DecoderFactory(), 
                    new org.mobicents.media.server.impl.dsp.audio.g711.alaw.EncoderFactory(), 
                    new org.mobicents.media.server.impl.dsp.audio.g711.ulaw.DecoderFactory(), 
                    new org.mobicents.media.server.impl.dsp.audio.g711.ulaw.EncoderFactory(), 
                    new org.mobicents.media.server.impl.dsp.audio.gsm.DecoderFactory(), 
                    new org.mobicents.media.server.impl.dsp.audio.gsm.EncoderFactory(), 
                    new org.mobicents.media.server.impl.dsp.audio.speex.DecoderFactory(), 
                    new org.mobicents.media.server.impl.dsp.audio.speex.EncoderFactory(), 
                    new org.mobicents.media.server.impl.dsp.audio.g729.DecoderFactory(), 
                    new org.mobicents.media.server.impl.dsp.audio.g729.EncoderFactory()
                };

    }
    
    private long maxDuration;
    private long initialDelay;
    
    private String voiceName = "kevin";
    private int volume;

    //This is a hack for JSR-309
    private long drift;
    private long s;
    private boolean first = true;
    
    private Listeners<PlayerListener> listeners = new Listeners();
    
    private Codec selectCodec(Format f) {
        for (CodecFactory factory : codecFactories) {
            if (factory.getSupportedInputFormat().matches(f) && factory.getSupportedOutputFormat().matches(format)) {
                return factory.getCodec();
            }
        }
        return null;
    }

    /**
     * Creates new instance of the Audio player.
     * 
     * @param name
     *            the name of the AudioPlayer to be created.
     * @param timer
     *            source of synchronization.
     * @param audioMediaDirectory 
     */
    public AudioPlayerImpl(String name, String audioMediaDirectory, VoicesCache vc) {
        super(name);
        this.audioMediaDirectory = audioMediaDirectory;
        this.voicesCache = vc;

    }

    @Override
    public long getDuration() {
        return track.getDuration();
    }

    public void setMaxDuration(long maxDuration) {
        this.maxDuration = maxDuration;
    }
    
    @Override
    public void setMediaTime(long timestamp) {
        track.setMediaTime(timestamp);
    }

    @Override
    public long getMediaTime() {
        return track.getMediaTime();
    }

    public void setInitialDelay(long delay) {
        this.initialDelay = delay;
    }
    
    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.resource.AudioPlayer#setURL(java.lang.String)
     */
    public void setURL(String passedURI) throws IOException, ResourceUnavailableException {
        // let's disallow to assign file is player is not connected
        if (!this.isConnected()) {
            throw new IllegalStateException("Component should be connected");
        }

        // now using extension we have to determne the suitable stream parser
        int pos = passedURI.lastIndexOf('.');

        // extension is not specified?
        if (pos == -1) {
            throw new IOException("Unknow file type: " + passedURI);
        }

        String ext = passedURI.substring(pos + 1).toLowerCase();
        // creating required extension
        try {
            URL targetURL = Utils.getAbsoluteURL(this.audioMediaDirectory, passedURI);
            //check scheme, if its file, we should try to create dirs
            if (ext.matches(Extension.WAV)) {
                track = new WavTrackImpl(targetURL);
            } else if (ext.matches(Extension.GSM)) {
                track = new GsmTrackImpl(targetURL);
            } else if (ext.matches(Extension.TXT)) {
                track = new TtsTrackImpl(targetURL, voiceName, this.voicesCache);
            } else if (ext.matches(Extension.MOV) || ext.matches(Extension.MP4) || ext.matches(Extension.THREE_GP)) {
                track = new AMRTrackImpl(targetURL);
            } else {
                throw new ResourceUnavailableException("Unknown extension: " + passedURI);
            }

        } catch (Exception e) {

            throw new ResourceUnavailableException(e);
        }


        // checking format of the specified file and trying to understand
        // do we need transcoding
        Format fileFormat = track.getFormat();
        codec = null;
        if (!fileFormat.matches(this.getFormat())) {
            // we need transcode. let's see if this possible
            codec = this.selectCodec(fileFormat);
            if (codec == null) {
                // transcoding is not possible with existing codecs
                throw new ResourceUnavailableException("Format is not supported: " + fileFormat);
            }
        }
    }

    @Override
    public void start() {
        if (track == null) {
            throw new IllegalStateException("The media source is not specified");
        }
        
        first = true;
        s = System.currentTimeMillis();
        super.start();
        listeners.dispatch(new AudioPlayerEvent(this, AudioPlayerEvent.START));
    }

    @Override
    public void stop() {
        super.stop();
        if (track != null) {
            track.close();
            track = null;
        }
        this.maxDuration = 0;
    }

    /**
     * Sends notification that signal is completed.
     * 
     */
    @Override
    protected void completed() {
        super.completed();
        this.maxDuration = 0;
        listeners.dispatch(new AudioPlayerEvent(this, AudioPlayerEvent.STOP));
    }
    
    @Override
    public void evolve(Buffer buffer, long timestamp) {
        try {
            if (first) {
                first = false;
                
                drift = Math.abs(System.currentTimeMillis() - s -100);
                if (this.maxDuration > 0) {
                    this.maxDuration = Math.max(800, maxDuration - drift);
                } else {
                    this.maxDuration = Math.max(800, getDuration() - drift);
                }
                logger.info("==== drift=" + drift + " , dur=" + maxDuration);
            }
            
            //generate initial silence
            if (timestamp < initialDelay) {
                buffer.setData(new byte[320]);
                buffer.setOffset(0);
                buffer.setLength(320);
                buffer.setTimeStamp(timestamp);
                buffer.setFormat(this.format);
                buffer.setDuration(20);
                return;
            }
            
            track.process(buffer);
            buffer.setTimeStamp(timestamp);

            if (codec != null && !buffer.isEOM()) {
                codec.process(buffer);
            }

            if (buffer.isEOM()) {
                logger.info("End of file reached");
            }
            //set End of Media if max duration exceeded
            if (maxDuration > 0 && track.getMediaTime() > maxDuration) {
                buffer.setEOM(true);
                logger.info("Max duration exceeded");
            }
            
            if (buffer.isEOM()) {
                track.close();
            }
        } catch (IOException e) {
            track.close();
            listeners.dispatch(new AudioPlayerEvent(this, AudioPlayerEvent.FAILED));
            buffer.setDuration(0);
        }
    }

    public Format[] getFormats() {
        return FORMATS;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    public void setText(String text) {
        track = new TtsTrackImpl(text, voiceName, this.voicesCache);
        Format fileFormat = track.getFormat();
        codec = null;
        if (!fileFormat.matches(this.getFormat())) {
            // we need transcode. let's see if this possible
            codec = this.selectCodec(fileFormat);
            if (codec == null) {
                // transcoding is not possible with existing codecs
                //throw new ResourceUnavailableException("Format is not supported: " + fileFormat);
            }
        }
    }

    @Override
    public <T> T getInterface(Class<T> interfaceType) {
        if (interfaceType.equals(Player.class)) {
            return (T) this;
        }
        if (interfaceType.equals(TTSEngine.class)) {
            return (T) this;
        } else {
            return null;
        }
    }

    public void addListener(PlayerListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    public void removeListener(PlayerListener listener) {
        listeners.remove(listener);
    }
}
