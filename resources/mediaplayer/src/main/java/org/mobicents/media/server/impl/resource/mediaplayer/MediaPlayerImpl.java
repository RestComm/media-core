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

package org.mobicents.media.server.impl.resource.mediaplayer;

import java.util.ArrayList;
import java.util.Collection;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.BaseComponent;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.video.VideoPlayerImpl;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.MultimediaSource;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * @author baranowb
 * @author kulikov
 */
public class MediaPlayerImpl extends BaseComponent implements MultimediaSource {

    //list of supported media types
    private final static ArrayList<MediaType> mediaTypes = new ArrayList();
    static {
        mediaTypes.add(MediaType.AUDIO);
        mediaTypes.add(MediaType.VIDEO);
    }

    private AudioPlayerImpl audioPlayer;
    private VideoPlayerImpl videoPlayer;
    
    /**
     * Creates new instance of MediaPlayer
     * 
     * @param name the name of media player component
     * @param videoPlayerFactory 
     * @param audioPlayerFactory 
     * @throws ResourceUnavailableException 
     */
    public MediaPlayerImpl(String name, Scheduler scheduler) throws ResourceUnavailableException {
        super(name);        
        audioPlayer = new AudioPlayerImpl("", scheduler, null);
    }


	/**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.MultimediaSource#getMediaTypes() 
     */
    public Collection<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.MultimediaSource#getMediaSource() 
     */
    public MediaSource getMediaSource(MediaType media) {
        switch (media) {
            case AUDIO : return audioPlayer;
            case VIDEO : return videoPlayer;
            default : return null;
        }
    }

}
