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

package org.restcomm.media.resource.player.audio;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.ComponentType;
import org.restcomm.media.component.AbstractSource;
import org.restcomm.media.component.audio.AudioInput;
import org.restcomm.media.resource.player.Track;
import org.restcomm.media.resource.player.audio.gsm.GsmTrackImpl;
import org.restcomm.media.resource.player.audio.mpeg.AMRTrackImpl;
import org.restcomm.media.resource.player.audio.tone.ToneTrackImpl;
import org.restcomm.media.resource.player.audio.tts.TtsTrackImpl;
import org.restcomm.media.resource.player.audio.wav.WavTrackImpl;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.ResourceUnavailableException;
import org.restcomm.media.spi.dsp.Processor;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.listener.Listeners;
import org.restcomm.media.spi.listener.TooManyListenersException;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.player.Player;
import org.restcomm.media.spi.player.PlayerListener;
import org.restcomm.media.spi.pooling.PooledObject;
import org.restcomm.media.spi.resource.TTSEngine;

/**
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class AudioPlayerImpl extends AbstractSource implements Player, TTSEngine, PooledObject {

    private static final long serialVersionUID = 8321615909592642344L;

    private final static Logger log = LogManager.getLogger(AudioPlayerImpl.class);

    // define natively supported formats
    private final static AudioFormat LINEAR = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static long period = 20000000L;
    private final static int packetSize = (int) (period / 1000000) * LINEAR.getSampleRate() / 1000 * LINEAR.getSampleSize() / 8;

    // Media Components
    private Processor dsp;
    private final AudioInput input;

    // audio track
    private Track track;
    private int volume;
    private String voiceName = "kevin";

    // Listeners
    private final Listeners<PlayerListener> listeners;

    private final RemoteStreamProvider remoteStreamProvider;

    /**
     * Creates new instance of the Audio player.
     * 
     * @param name the name of the AudioPlayer to be created.
     * @param scheduler EDF job scheduler
     * @param vc the TTS voice cache.
     */
    public AudioPlayerImpl(String name, PriorityQueueScheduler scheduler, RemoteStreamProvider remoteStreamProvider) {
        super(name, scheduler, PriorityQueueScheduler.INPUT_QUEUE);
        this.input = new AudioInput(ComponentType.PLAYER.getType(), packetSize);
        this.listeners = new Listeners<PlayerListener>();
        this.connect(this.input);
        this.remoteStreamProvider = remoteStreamProvider;
    }

    public AudioInput getAudioInput() {
        return this.input;
    }

    /**
     * Assigns the digital signaling processor of this component. The DSP allows to get more output formats.
     *
     * @param dsp the dsp instance
     */
    public void setDsp(Processor dsp) {
        this.dsp = dsp;
    }

    /**
     * Gets the digital signaling processor associated with this media source
     *
     * @return DSP instance.
     */
    public Processor getDsp() {
        return this.dsp;
    }

    @Override
    public void setURL(String passedURI) throws ResourceUnavailableException, MalformedURLException {
        // close previous track if was opened
        if (this.track != null) {
            track.close();
            track = null;
        }

        // let's disallow to assign file is player is not connected
        if (!this.isConnected()) {
            throw new IllegalStateException("Component should be connected");
        }
        URL targetURL;
        // now using extension we have to determine the suitable stream parser
        int pos = passedURI.lastIndexOf('.');

        // extension is not specified?
        if (pos == -1) {
            throw new MalformedURLException("Unknow file type: " + passedURI);
        }

        String ext = passedURI.substring(pos + 1).toLowerCase();
        targetURL = new URL(passedURI);

        // creating required extension
        try {
            // check scheme, if its file, we should try to create dirs
            if (ext.matches(Extension.WAV)) {
                track = new WavTrackImpl(targetURL, remoteStreamProvider);
            } else if (ext.matches(Extension.GSM)) {
                track = new GsmTrackImpl(targetURL);
            } else if (ext.matches(Extension.TONE)) {
                track = new ToneTrackImpl(targetURL);
            } else if (ext.matches(Extension.TXT)) {
                track = new TtsTrackImpl(targetURL, voiceName, null);
            } else if (ext.matches(Extension.MOV) || ext.matches(Extension.MP4) || ext.matches(Extension.THREE_GP)) {
                track = new AMRTrackImpl(targetURL);
            } else {
                throw new ResourceUnavailableException("Unknown extension: " + passedURI);
            }
        } catch (Exception e) {
            throw new ResourceUnavailableException(e);
        }

        // update duration
        this.duration = track.getDuration();
    }

    @Override
    public void activate() {
        if (track == null) {
            throw new IllegalStateException("The media source is not specified");
        }
        start();

        listeners.dispatch(new AudioPlayerEvent(this, AudioPlayerEvent.START));
    }

    @Override
    public void deactivate() {
        stop();
        if (track != null) {
            track.close();
            track = null;
        }
    }

    @Override
    protected void stopped() {
        listeners.dispatch(new AudioPlayerEvent(this, AudioPlayerEvent.STOP));
    }

    @Override
    protected void completed() {
        super.completed();
        listeners.dispatch(new AudioPlayerEvent(this, AudioPlayerEvent.STOP));
    }

    @Override
    public Frame evolve(long timestamp) {
        try {
            // Null check is necessary when a DTMF detector stops the announcement earlier causing the player
            // to stop and the track to null
            if (track == null) {
                return null;
            }

            Frame frame = track.process(timestamp);
            if (frame == null)
                return null;

            frame.setTimestamp(timestamp);

            if (frame.isEOM()) {
                if(log.isInfoEnabled()) {
                    log.info("End of file reached");
                }
            }

            // do the transcoding job
            if (dsp != null) {
                try {
                    frame = dsp.process(frame, frame.getFormat(), LINEAR);
                } catch (Exception e) {
                    // transcoding error , print error and try to move to next frame
                    log.error(e);
                }
            }

            if (frame.isEOM() && track != null) {
                track.close();
            }
            return frame;
        } catch (IOException e) {
            log.error(e);
            if (track != null) {
                track.close();
            }
        }
        return null;
    }

    @Override
    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    @Override
    public String getVoiceName() {
        return voiceName;
    }

    @Override
    public void setVolume(int volume) {
        this.volume = volume;
    }

    @Override
    public int getVolume() {
        return volume;
    }

    @Override
    public void setText(String text) {
        track = new TtsTrackImpl(text, voiceName, null);
    }

    @Override
    public void addListener(PlayerListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    @Override
    public void removeListener(PlayerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void clearAllListeners() {
        listeners.clear();
    }

    @Override
    public void checkIn() {
        clearAllListeners();
        track = null;
    }

    @Override
    public void checkOut() {
        // TODO Auto-generated method stub

    }
}
