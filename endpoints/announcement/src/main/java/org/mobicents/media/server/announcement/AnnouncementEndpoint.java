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

	private MediaPlayerImpl mediaPlayer;
    private AudioMixer audioMixer;
    private PipeImpl pipe;
    
    private ArrayList<MediaSource> components = new ArrayList();
    
    public AnnouncementEndpoint(String name) {
        super(name,BaseEndpointImpl.ENDPOINT_NORMAL);                
    }
    
    public AnnouncementEndpoint(String name,int endpointType) {
        super(name,endpointType);
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
        audioMixer = new AudioMixer(getScheduler());
        
        try {
        	MediaSink sink = audioMixer.newInput();
            MediaSource source=mediaPlayer.getMediaSource(MediaType.AUDIO);
            PipeImpl p = new PipeImpl();
            p.connect(sink);
            p.connect(source);
            
            components.add(source);            
            mediaPlayer.setDsp(getDspFactory().newProcessor(),MediaType.AUDIO);                
            sink.start();
        } catch (Exception e) {
            throw new ResourceUnavailableException(e);
        }

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
