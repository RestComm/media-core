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
package org.mobicents.media.server.announcement;

import java.util.ArrayList;
import org.mobicents.media.Component;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.BaseEndpointImpl;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.MediaPlayerImpl;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Implements announcement access point.
 * 
 * @author kulikov
 */
public class AnnouncementEndpoint extends BaseEndpointImpl {

	private final static AudioFormat LINEAR = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static Formats formats = new Formats();
    
    private MediaPlayerImpl mediaPlayer;
    //private AudioAnnouncement audioAnnouncement;
    private AudioMixer audioMixer;

    private ArrayList<MediaSource> components = new ArrayList();
    
    private DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    static {
        formats.add(LINEAR);
    }
    
    public AnnouncementEndpoint(String name) {
        super(name,BaseEndpointImpl.ENDPOINT_NORMAL);
        
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");
        
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder");
    }
    
    public AnnouncementEndpoint(String name,int endpointType) {
        super(name,endpointType);
        
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");
        
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder");
    }

    @Override
    public MediaSink getSink(MediaType media) {
        return null;
    }

    @Override
    public MediaSource getSource(MediaType media) {
        switch (media) {
            case AUDIO:
                return audioMixer.getOutput();
            default:
                return null;
        }
    }

    @Override
    public void unblock() {
    }

    @Override
    public void block() {
    }

    @Override
    public void start() throws ResourceUnavailableException {
        //construct media player
        mediaPlayer = new MediaPlayerImpl("", getScheduler());

        //construct audio part
        //audioAnnouncement = new AudioAnnouncement(getScheduler());
        audioMixer = new AudioMixer(getScheduler());
        
        try {
        	MediaSink sink = audioMixer.newInput();
            
            PipeImpl p = new PipeImpl();
            p.connect(sink);
            p.connect(mediaPlayer.getMediaSource(MediaType.AUDIO));
            
            components.add(mediaPlayer.getMediaSource(MediaType.AUDIO));
            
            sink.setDsp(dspFactory.newProcessor());
            sink.setFormats(formats);
            sink.start();
            
            //audioAnnouncement.add();
        } catch (Exception e) {
            throw new ResourceUnavailableException(e);
        }

        //its not clear why its needed here
        //sine = new Sine(getScheduler());
        //sine.setAmplitude((short)(Short.MAX_VALUE / 2));
        //sine.setFrequency(50);
        super.start();
    }

    public Component getResource(MediaType mediaType, Class intf) {
        switch (mediaType) {
            case AUDIO:
            	for (int i = 0; i < components.size(); i++) {
                    if (components.get(i).getInterface(intf) != null) {
                        return components.get(i);
                    }
                }
                return null;
            default:
                return null;
        }
    }
}
