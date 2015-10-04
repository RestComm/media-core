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

package org.mobicents.media.server.impl.resource.mediaplayer.audio;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.audio.AudioInput;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.resource.mediaplayer.Track;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.gsm.GsmTrackImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.mpeg.AMRTrackImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.tone.ToneTrackImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.tts.TtsTrackImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.wav.WavTrackImpl;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.spi.resource.TTSEngine;

/**
 * @author
 * @author yulian oifa
 */
public class AudioPlayerImpl extends AbstractSource implements Player, TTSEngine {

    private static final long serialVersionUID = 8321615909592642344L;

    //define natively supported formats
    private final static AudioFormat LINEAR = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * LINEAR.getSampleRate()/1000 * LINEAR.getSampleSize() / 8;    

    //digital signaling processor
    private Processor dsp;
    
    //audio track
    private Track track;

    private String voiceName = "kevin";
    private int volume;

    private Listeners<PlayerListener> listeners = new Listeners<PlayerListener>();

    private final static Logger logger = Logger.getLogger(AudioPlayerImpl.class);
    
    private AudioInput input;
    /**
     * Creates new instance of the Audio player.
     * 
     * @param name the name of the AudioPlayer to be created.
     * @param scheduler EDF job scheduler
     * @param vc  the TTS voice cache. 
     */
    public AudioPlayerImpl(String name, Scheduler scheduler) {
        super(name, scheduler,Scheduler.INPUT_QUEUE);
        
        this.input=new AudioInput(ComponentType.PLAYER.getType(),packetSize);
        this.connect(this.input);        
    }

    public AudioInput getAudioInput()
    {
    	return this.input;
    }
    
    /**
     * Assigns the digital signaling processor of this component.
     * The DSP allows to get more output formats.
     *
     * @param dsp the dsp instance
     */
    public void setDsp(Processor dsp) {
        //assign processor
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
    
    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.player.Player#setURL(java.lang.String)
     */
    public void setURL(String passedURI) throws ResourceUnavailableException, MalformedURLException {
    	//close previous track if was opened
    	if(this.track!=null)
    	{
    		track.close();
            track = null;
    	}
    	
        // let's disallow to assign file is player is not connected
        if (!this.isConnected()) {
            throw new IllegalStateException("Component should be connected");
        }
        URL targetURL;
     // now using extension we have to determne the suitable stream parser
    	int pos = passedURI.lastIndexOf('.');

    	// extension is not specified?
    	if (pos == -1) {
    		throw new MalformedURLException("Unknow file type: " + passedURI);
    	}

    	String ext = passedURI.substring(pos + 1).toLowerCase();
    	targetURL = new URL(passedURI);
    	
    	// creating required extension
    	try {
    		//check scheme, if its file, we should try to create dirs
    		if (ext.matches(Extension.WAV)) {       
    			track = new WavTrackImpl(targetURL);            	
    		} else if (ext.matches(Extension.GSM)) {
    			track = new GsmTrackImpl(targetURL);
    		} else if (ext.matches(Extension.TONE)) {
    			track = new ToneTrackImpl(targetURL);
    		} else if (ext.matches(Extension.TXT)) {
    			track = new TtsTrackImpl(targetURL, voiceName, null);
    		} else if (ext.matches(Extension.MOV) || ext.matches(Extension.MP4) || ext.matches(Extension.THREE_GP)) {
    			track = new AMRTrackImpl(targetURL);
    		} else {
    			if(getEndpoint()==null)
    				logger.info("unknown extension:" + passedURI);
    			else
    				logger.info("(" + getEndpoint().getLocalName() + ") unknown extension:" + passedURI);
    			
    			throw new ResourceUnavailableException("Unknown extension: " + passedURI);
    		}
    	} catch (Exception e) {        
    		if(getEndpoint()==null)
    			logger.error("error occured",e);
    		else
    			logger.error("(" + getEndpoint().getLocalName() + ") error occured",e);
    		
    		throw new ResourceUnavailableException(e);
    	}
    	
        //update duration
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
    /**
     * Sends notification that signal is completed.
     * 
     */
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
            if(frame==null)
            	return null;
            
            frame.setTimestamp(timestamp);

            if (frame.isEOM()) {
            	if(getEndpoint()==null)
            		logger.info("End of file reached");
            	else
            		logger.info("(" + getEndpoint().getLocalName() + ") End of file reached");            		
            }

            //do the transcoding job
        	if (dsp != null) {
        		try
        		{
        			frame = dsp.process(frame,frame.getFormat(),LINEAR);
        		}
        		catch(Exception e)
        		{
        			//transcoding error , print error and try to move to next frame
        			if(getEndpoint()==null)
        				logger.error(e);
        			else
        				logger.error("(" + getEndpoint().getLocalName() + ")",e);        			        		
        		}                	
        	}  
        	
        	if (frame.isEOM() && track != null) {
            	track.close();                
            }
            return frame;
        } catch (IOException e) {
        	if(getEndpoint()==null)
        		logger.error(e);
        	else        		
        		logger.error("(" + getEndpoint().getLocalName() + ")",e);
        	
            if (track != null) {
                track.close();
            }
        }
        return null;
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
        track = new TtsTrackImpl(text, voiceName, null);
    }    

    public void addListener(PlayerListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    public void removeListener(PlayerListener listener) {
    	listeners.remove(listener);
    }

    public void clearAllListeners() {
    	listeners.clear();
    }
    
    public void setMaxDuration(long duration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
