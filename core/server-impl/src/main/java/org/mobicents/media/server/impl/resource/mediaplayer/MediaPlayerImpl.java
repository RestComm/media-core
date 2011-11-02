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
package org.mobicents.media.server.impl.resource.mediaplayer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.BaseComponent;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.video.VideoPlayerFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.video.VideoPlayerImpl;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.MultimediaSource;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * @author baranowb
 * @author kulikov
 */
public class MediaPlayerImpl extends BaseComponent implements MultimediaSource {

    //engines for generating media 
    private HashMap<MediaType, MediaSource> engines = new HashMap();
    
    //list of supported media types
    private static ArrayList<MediaType> mediaTypes = new ArrayList();
    static {
        mediaTypes.add(MediaType.AUDIO);
        mediaTypes.add(MediaType.VIDEO);
    }
    
    /**
     * Creates new instance of MediaPlayer
     * 
     * @param name the name of media player component
     * @param videoPlayerFactory 
     * @param audioPlayerFactory 
     * @throws ResourceUnavailableException 
     */
    public MediaPlayerImpl(String name, AudioPlayerFactory audioPlayerFactory, VideoPlayerFactory videoPlayerFactory) throws ResourceUnavailableException {
        super(name);        
        //create audio processing engine
        engines.put(MediaType.AUDIO, audioPlayerFactory.newInstance(getEndpoint()));
        engines.put(MediaType.VIDEO, videoPlayerFactory.newInstance(getEndpoint()));
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
        return engines.get(media);
    }

}
