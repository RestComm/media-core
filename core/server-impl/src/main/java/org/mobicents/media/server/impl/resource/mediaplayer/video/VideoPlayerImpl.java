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
package org.mobicents.media.server.impl.resource.mediaplayer.video;

import java.io.IOException;
import java.net.URL;

import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.server.Utils;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.resource.mediaplayer.Track;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.events.NotifyEvent;
import org.mobicents.media.server.spi.resource.Player;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 * @author baranowb
 * @author Oleg Kulikov
 */
public class VideoPlayerImpl extends AbstractSource implements Player {

    /** supported formats definition */
    private final static Format[] FORMATS = new Format[]{
        AVProfile.MPEG4_GENERIC, AVProfile.MP4V
    };
    
    private Track track;
    private String videoMediaDirectory;
    /**
     * Creates new instance of the Audio player.
     * 
     * @param name the name of the AudioPlayer to be created.
     * @param timer source of synchronization.
     * @param videoMediaDirectory 
     */
    public VideoPlayerImpl(String name, String videoMediaDirectory) {
        super(name);
        this.videoMediaDirectory = videoMediaDirectory;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.resource.AudioPlayer#setURL(java.lang.String) 
     */
    public void setURL(String passedURI) throws IOException, ResourceUnavailableException {
        //let's disallow to assign file is player is not connected
        if (!this.isConnected()) {
            throw new IllegalStateException("Component should be connected");
        }
        
        //now using extension we have to determine the suitable stream parser
        int pos = passedURI.lastIndexOf('.');

        //extension is not specified?
        if (pos == -1) {
            throw new IOException("Unknow file type: " + passedURI);
        }
        
        String ext = passedURI.substring(pos + 1).toLowerCase();        
        //creating required extension
        try{
        	URL targetURL = Utils.getAbsoluteURL(this.videoMediaDirectory, passedURI);
        	//check scheme, if its file, we should try to create dirs
        	
        	 //TODO: handle extension
            track = new MpegVideoTrackImpl(targetURL);
        }catch(Exception e )
       {
        	
    	   throw new ResourceUnavailableException(e);
       }
        
        //checking format of the specified file and trying to understand
        //do we need transcoding
        
        //TODO: check formats
        //Format fileFormat = track.getFormat();
    }

    public void setMaxDuration(long maxDuration) {
    }
    
    @Override
    public long getDuration() {
        return track.getDuration();
    }
    
    @Override
    public void setMediaTime(long timestamp) {
        track.setMediaTime(timestamp);
    }
    
    @Override
    public long getMediaTime() {
        return track.getMediaTime();
    }
    
    @Override
    public void start() {
        if (track == null) {
            throw new IllegalStateException("The media source is not specified");
        }
        super.start();
    }

    @Override
    public void stop() {
        if (track != null) {
            track.close();
            track = null;
        }
        super.stop();
    }
    
    @Override
    public void evolve(Buffer buffer, long timestamp) {
        try {
            track.process(buffer);
            buffer.setTimeStamp(timestamp);
            if (buffer.isEOM()) {
                track.close();
            }
        } catch (IOException e) {
            track.close();
            this.failed(NotifyEvent.TX_FAILED, e);
            buffer.setDuration(0);
        }
    }

    public Format[] getFormats() {
        return FORMATS;
    }
    @Override
	public <T> T getInterface(Class<T> interfaceType) {
		if(interfaceType.equals(Player.class))
		{
			return (T) this;
		}
		{
			return null;
		}
	}

    public void setInitialDelay(long delay) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
