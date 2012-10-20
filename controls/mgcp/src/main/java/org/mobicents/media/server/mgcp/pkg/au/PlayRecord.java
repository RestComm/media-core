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

package org.mobicents.media.server.mgcp.pkg.au;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Collection;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.mgcp.controller.signal.Event;
import org.mobicents.media.server.mgcp.controller.signal.NotifyImmediately;
import org.mobicents.media.server.mgcp.controller.signal.Signal;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.recorder.Recorder;
import org.mobicents.media.server.spi.recorder.RecorderEvent;
import org.mobicents.media.server.spi.recorder.RecorderListener;
import org.mobicents.media.server.utils.Text;

/**
 * Implements play announcement signal.
 * 
 * Plays a prompt and collects DTMF digits entered by a user.  If no
 * digits are entered or an invalid digit pattern is entered, the
 * user may be reprompted and given another chance to enter a correct
 * pattern of digits.  The following digits are supported:  0-9, *,
 * #, A, B, C, D.  By default PlayCollect does not play an initial
 * prompt, makes only one attempt to collect digits, and therefore
 * functions as a simple Collect operation.  Various special purpose
 * keys, key sequences, and key sets can be defined for use during
 * the PlayCollect operation.
 * 
 * 
 * @author oifa yulian
 */
public class PlayRecord extends Signal {
    
    private Event oc = new Event(new Text("oc"));
    private Event of = new Event(new Text("of"));
    
    private volatile boolean isActive;
    
    private Player player;
    private Recorder recorder;
    private DtmfDetector dtmfDetector;
    
    private Options options; 
    private EventBuffer buffer = new EventBuffer();
    
    private RecordingHandler recordingHandler;
    private DtmfHandler dtmfHandler;
    private PromptHandler promptHandler;
    
    private volatile boolean isPromptActive;
    private Text[] prompt=new Text[10];
    private Text[] deletePersistentAudio=new Text[100];
    private int promptLength=0,promptIndex=0,deletePersistentAudioLength=0;
    private int numberOfAttempts=1;
    
    private boolean isCompleted;
    private final static Logger logger = Logger.getLogger(PlayRecord.class);
    private int segCount = 0;
    
    private PlayerMode playerMode=PlayerMode.PROMPT;
    private Text eventContent;
    
    public PlayRecord(String name) {
        super(name);
        oc.add(new NotifyImmediately("N"));
        of.add(new NotifyImmediately("N"));
        
        
        //create recorder listener
        recordingHandler = new RecordingHandler(this);
        dtmfHandler = new DtmfHandler(this);
        promptHandler = new PromptHandler(this);
    }
    
    @Override
    public void execute() {
    	if(getEndpoint().getActiveConnectionsCount()==0)
    	{
    		oc.fire(this, new Text("rc=327"));
    		this.complete();
    		return;
    	}
    	
    	promptLength=0;
    	promptIndex=0;
    	segCount = 0;
    	
    	isCompleted=false;
    	//set flag active signal
        this.isActive = true;
        //get options of the request
        options = new Options(getTrigger().getParams());        
        
        //check if has delete persistent audio , if yes verify all files and delete
    	if(options.hasDeletePresistentAudio())
    	{
    		deletePersistentAudioLength=options.getDeletePersistentAudio().size();    		
    		deletePersistentAudio = options.getDeletePersistentAudio().toArray(deletePersistentAudio);
    		File f;
    		for(int i=0;i<deletePersistentAudioLength;i++)
    		{
    			try
    			{
    				f = new File(deletePersistentAudio[i].toString());    				
    				if(!f.exists())
    				{
    		    		oc.fire(this, new Text("rc=320 ri=" + deletePersistentAudio[i].toString()));        		
    		    		isCompleted=true;
    		    		complete();
    		    		return;
    				}	    				    				
    			}
    			catch(Exception ex)
    			{    
    				//should not occure
    				logger.error("OPERATION FAILURE", ex);
    			}
    		}
    		
    		for(int i=0;i<deletePersistentAudioLength;i++)
    		{
    			f = new File(deletePersistentAudio[i].toString());
    			f.delete();
				
    		}
    		oc.fire(this, new Text("rc=100"));        		
    		isCompleted=true;
    		complete();
    		return;
    	}    	
    	
    	playerMode=PlayerMode.PROMPT;
    	
    	if(options.getNumberOfAttempts()>1)
        	this.numberOfAttempts=options.getNumberOfAttempts();
        else
        	this.numberOfAttempts=1;
    	
        //start digits collect phase
        logger.info(String.format("(%s) Prepare digit collect phase", getEndpoint().getLocalName()));
        //Initializes resources for DTMF detection
        //at this stage DTMF detector started but local buffer is not assigned 
        //yet as listener
        prepareCollectPhase(options);
        
        //if initial prompt has been specified then start with prompt phase
        if (options.hasPrompt()) {
            logger.info(String.format("(%s) Start prompt phase", getEndpoint().getLocalName()));

            this.isPromptActive = true;
            startPromptPhase(options.getPrompt());
            
            return;
        }                
        
        //flush DTMF detector buffer and start collect phase
        logger.info(String.format("(%s) Start collect phase", getEndpoint().getLocalName()));
        
        flushBuffer();        
        //now all buffered digits must be inside local buffer
        startCollectPhase();
        
        logger.info(String.format("(%s) Start record phase", getEndpoint().getLocalName()));
        startRecordPhase(options);
    }

    @Override
    public boolean doAccept(Text event) {
        if (!oc.isActive() && oc.matches(event)) {
            return true;
        }
        if (!of.isActive() && of.matches(event)) {
            return true;
        }
        
        return false;
    }

    @Override
    public void cancel() {
        //disable signal activity and terminate
        this.isActive = false;
        this.terminate();
    }

    /**
     * Starts the prompt phase.
     * 
     * @param options requested options.
     */
    private void startPromptPhase(Collection<Text> promptList) {
        player = this.getPlayer();        
        try {
            //assign listener
            player.addListener(promptHandler);
            promptLength=promptList.size();
            prompt = promptList.toArray(prompt);            
            player.setURL(prompt[0].toString());
            
          //specify URL to play
//          player.setURL(options.getPrompt().toString());
            
            //start playback
            player.activate();
        } catch (TooManyListenersException e) {
            of.fire(this, new Text("Too many listeners"));
            logger.error("OPERATION FAILURE", e);
        } 
        catch (MalformedURLException e) {
        	logger.info("Received URL in invalid format , firing of");
        	of.fire(this, new Text(e.getMessage()));
            return;
        } 
        catch (ResourceUnavailableException e) {
        	logger.info("Received URL can not be found , firing of");
        	of.fire(this, new Text(e.getMessage()));
            return;
        }   
    }

    /**
     * Terminates prompt phase if it was started or do nothing otherwise.
     */
    private void terminatePrompt() {
    	//jump to end of segments
    	if (promptLength>0) {
        	promptIndex=promptLength-1;
        }
    	
        if (player != null) {            
            player.deactivate();
            player.removeListener(promptHandler);
            player=null;
        }
    }
    
    /**
     * Starts record phase.
     * 
     * @param options requested options
     */
    private void startRecordPhase(Options options) {
        recorder = getRecorder();
        
        //assign record duration
        recorder.setMaxRecordTime(options.getRecordDuration());
        //post speech timer
        recorder.setPostSpeechTimer(options.getPostSpeechTimer());
        if(options.getPreSpeechTimer()>0)
        	recorder.setPreSpeechTimer(options.getPreSpeechTimer());
        else
        	recorder.setPreSpeechTimer(options.getPostSpeechTimer());
        
        try {
        	recorder.addListener(recordingHandler);
            
            recorder.setRecordFile(options.getRecordID().toString(), !options.isOverride());
            recorder.activate();
        } 
        catch (TooManyListenersException e) {
            of.fire(this, new Text("Too many listeners"));
            logger.error("OPERATION FAILURE", e);
        } 
        catch (IOException e) {
        	logger.info("Received Recording URL can not be found , firing of");
            of.fire(this, new Text(e.getMessage()));
        }
    }
    
    
    /**
     * Terminates prompt phase if it was started or do nothing otherwise.
     */
    private void terminateRecordPhase() {
        if (recorder != null) {            
            recorder.deactivate();
            recorder.removeListener(recordingHandler);
            recorder=null;
        }
    }
    
    /**
     * Prepares resources for DTMF collection phase.
     * 
     * @param options 
     */
    private void prepareCollectPhase(Options options) {
        //obtain detector instance
        dtmfDetector = this.getDetector();
        
        //DTMF detector was buffering digits and now it can contain
        //digits in the buffer
        //Clean detector's buffer if requested
        if (options.isClearDigits()) {
            dtmfDetector.clearDigits();
        }
        
        buffer.reset();        
        buffer.setListener(dtmfHandler);        
        
        //assign requested parameters
        buffer.setPatterns(options.getDigitPattern());
        buffer.setCount(options.getDigitsNumber());                    
    }
    
    /**
     * Terminates digit collect phase.
     */
    private void terminateCollectPhase() {
        if (dtmfDetector != null) {
            dtmfDetector.removeListener(buffer);            
            
           //dtmfDetector.clearDigits();
            
            buffer.passivate();
            buffer.clear();        
            dtmfDetector=null;
        }
    }
    
    /**
     * Flushes DTMF buffer content to local buffer
     */
    private void flushBuffer() {        
        try {
            //attach local buffer to DTMF detector
            //but do not flush
            dtmfDetector.addListener(buffer);
            dtmfDetector.flushBuffer();
        } catch (TooManyListenersException e) {
            of.fire(this, new Text("Too many listeners for DTMF detector"));
            logger.error("OPERATION FAILURE", e);
        }        
    }
    
    private void startCollectPhase() {
    	buffer.activate();                
        buffer.flush();
    }
    
    /**
     * Terminates any activity. 
     */
    private void terminate() {
    	this.isPromptActive = false;
        this.terminatePrompt();
        this.terminateRecordPhase();
        this.terminateCollectPhase();
    }
    
    private void decreaseNa()
    {
    	numberOfAttempts--;
    	if (options.hasReprompt()) {
			buffer.passivate();
			isPromptActive = true;
			startPromptPhase(options.getReprompt());	        
							}
    	else if (options.hasPrompt()) {
			buffer.passivate();
			isPromptActive = true;
			startPromptPhase(options.getPrompt());					
		} else
			startCollectPhase();		
    }
    
    private Player getPlayer() {
    	Endpoint endpoint = getEndpoint();
        return (Player) getEndpoint().getResource(MediaType.AUDIO, ComponentType.PLAYER);
    }

    private DtmfDetector getDetector() {
    	Endpoint endpoint = getEndpoint();
        return (DtmfDetector) getEndpoint().getResource(MediaType.AUDIO, ComponentType.DTMF_DETECTOR);
    }  

    private Recorder getRecorder() {
    	Endpoint endpoint = getEndpoint();
        return (Recorder) getEndpoint().getResource(MediaType.AUDIO, ComponentType.RECORDER);
    }    
    
    @Override
    public void reset() {
        super.reset();

        this.terminate();
        
        oc.reset();
        of.reset();
    }

    private void next(long delay) {
    	segCount++;
    	promptIndex++;
        try {
        	String url = prompt[promptIndex].toString(); 
        	logger.info(String.format("(%s) Processing player next with url - %s", getEndpoint().getLocalName(), url));
            player.setURL(url);
            player.setInitialDelay(delay * 1000000L);
            //start playback
            player.start();
        } 
        catch (MalformedURLException e) {
        	logger.info("Received URL in invalid format , firing of");
        	of.fire(this, new Text(e.getMessage()));            
            return;
        } 
        catch (ResourceUnavailableException e) {
        	logger.info("Received URL can not be found , firing of");
        	of.fire(this, new Text(e.getMessage()));
            return;
        }   
    }
    
    private void prev(long delay) {
    	segCount++;
    	promptIndex--;
        try {
        	String url = prompt[promptIndex].toString(); 
        	logger.info(String.format("(%s) Processing player prev with url - %s", getEndpoint().getLocalName(), url));
            player.setURL(url);
            player.setInitialDelay(delay * 1000000L);
            //start playback
            player.start();
        } 
        catch (MalformedURLException e) {
        	logger.info("Received URL in invalid format , firing of");
        	of.fire(this, new Text(e.getMessage()));            
            return;
        } 
        catch (ResourceUnavailableException e) {
        	logger.info("Received URL can not be found , firing of");
        	of.fire(this, new Text(e.getMessage()));
            return;
        }   
    }
    
    private void curr(long delay) {
    	segCount++;
    	try {
        	String url = prompt[promptIndex].toString(); 
        	logger.info(String.format("(%s) Processing player curr with url - %s", getEndpoint().getLocalName(), url));
            player.setURL(url);
            player.setInitialDelay(delay * 1000000L);
            //start playback
            player.start();
        } 
    	catch (MalformedURLException e) {
        	logger.info("Received URL in invalid format , firing of");
        	of.fire(this, new Text(e.getMessage()));            
            return;
        } 
        catch (ResourceUnavailableException e) {
        	logger.info("Received URL can not be found , firing of");
        	of.fire(this, new Text(e.getMessage()));
            return;
        }   
    }
    
    private void first(long delay) {
    	segCount++;
    	promptIndex=0;
        try {
        	String url = prompt[promptIndex].toString(); 
        	logger.info(String.format("(%s) Processing player first with url - %s", getEndpoint().getLocalName(), url));
            player.setURL(url);
            player.setInitialDelay(delay * 1000000L);
            //start playback
            player.start();
        } 
        catch (MalformedURLException e) {
        	logger.info("Received URL in invalid format , firing of");
        	of.fire(this, new Text(e.getMessage()));            
            return;
        } 
        catch (ResourceUnavailableException e) {
        	logger.info("Received URL can not be found , firing of");
        	of.fire(this, new Text(e.getMessage()));
            return;
        }   
    }
    
    private void last(long delay) {
    	segCount++;
    	promptIndex=promptLength-1;
        try {
        	String url = prompt[promptIndex].toString(); 
        	logger.info(String.format("(%s) Processing player last with url - %s", getEndpoint().getLocalName(), url));
            player.setURL(url);
            player.setInitialDelay(delay * 1000000L);
            //start playback
            player.start();
        } 
        catch (MalformedURLException e) {
        	logger.info("Received URL in invalid format , firing of");
        	of.fire(this, new Text(e.getMessage()));            
            return;
        } 
        catch (ResourceUnavailableException e) {
        	logger.info("Received URL can not be found , firing of");
        	of.fire(this, new Text(e.getMessage()));
            return;
        }   
    }
    
    /**
     * Handler for prompt phase.
     */
    private class PromptHandler implements PlayerListener {

        private PlayRecord signal;
        
        /**
         * Creates new handler instance.
         * 
         * @param signal the play record signal instance
         */
        protected PromptHandler(PlayRecord signal) {
            this.signal = signal;
        }
        
        public void process(PlayerEvent event) {
            switch (event.getID()) {
                case PlayerEvent.START :
                	if (segCount == 0) {
                		flushBuffer();
                	}
                    break;
                case PlayerEvent.STOP :
                	if (promptIndex<promptLength-1) {
                        next(options.getInterval());
                        return;
                    }           
                	
                	switch(playerMode)
                	{
                		case PROMPT:
                			//start collect phase when prompted has finished
                			if (isPromptActive) {
                				isPromptActive = false;
                        
                				logger.info(String.format("(%s) Prompt phase terminated, start collect/record phase", getEndpoint().getLocalName()));
                				startCollectPhase();
                        
                				//should not start record phase if completed by collect
                				if(!isCompleted)
                					startRecordPhase(options);
                			}
                			break;
                		case SUCCESS:
                			oc.fire(signal, eventContent);
                			reset();
                			isCompleted=true;
                    		complete();
                			break;
                		case FAILURE:
                			if(numberOfAttempts==1)
                			{
                				oc.fire(signal, eventContent);                				
                				reset();
                				isCompleted=true;
                        		complete();
                			}
                			else
                				decreaseNa();
                			
                			break;
                	}
            		break;
                case PlayerEvent.FAILED :
                    of.fire(signal, null);
                    complete();
                    break;
            }
        }
        
    }
    
    /**
     * Handler for recorder events
     */
    private class RecordingHandler implements RecorderListener {
        
        private PlayRecord signal;
        
        protected RecordingHandler(PlayRecord signal) {
            this.signal = signal;
        }
        
        public void process(RecorderEvent event) {
            switch (event.getID()) {
                case RecorderEvent.STOP :
                    switch (event.getQualifier()) {
                        case RecorderEvent.MAX_DURATION_EXCEEDED :
                        	if(numberOfAttempts==1)                        	
                        		oc.fire(signal, new Text("rc=328"));                            
                        	else if(options.hasFailureAnnouncement())
                			{
                				eventContent=new Text("rc=328");
                				playerMode=PlayerMode.FAILURE;
                				startPromptPhase(options.getFailureAnnouncement());
                			}
            				else
            					oc.fire(signal, new Text("rc=328"));
                            break;
                        case RecorderEvent.NO_SPEECH :
                        	if(numberOfAttempts==1)
                        		oc.fire(signal, new Text("rc=327"));
                        	else if(options.hasNoSpeechReprompt())
            				{
            					eventContent=new Text("rc=327");
            					playerMode=PlayerMode.FAILURE;
            					startPromptPhase(options.getNoSpeechReprompt());
            				}
            				else if(options.hasFailureAnnouncement())
                			{
                				eventContent=new Text("rc=327");
                				playerMode=PlayerMode.FAILURE;
                				startPromptPhase(options.getFailureAnnouncement());
                			}
            				else
            					oc.fire(signal, new Text("rc=327"));
                            break;
                    }
                    break;
            }
        }
        
    }
    
    /**
     * Handler for digit collect phase.
     * 
     */
    private class DtmfHandler implements BufferListener {

        private PlayRecord signal;

        /**
         * Constructor for this handler.
         * @param signal 
         */
        public DtmfHandler(PlayRecord signal) {
            this.signal = signal;
        }
        
        /**
         * (Non Java-doc.)
         * 
         * @see BufferListener#patternMatches(int, java.lang.String) 
         */
        public void patternMatches(int index, String s) {
        	if(options.hasSuccessAnnouncement())
			{
				eventContent=new Text("rc=100 dc=" + s + " pi=" + index);
				playerMode=PlayerMode.SUCCESS;
				startPromptPhase(options.getSuccessAnnouncement());				
			}
			else
			{
				oc.fire(signal, new Text("rc=100 dc=" + s + " pi=" + index));
				reset();
				isCompleted=true;
	            complete();
			}        	        
        }
        
        /**
         * (Non Java-doc.)
         * 
         * @see BufferListener#countMatches(java.lang.String) 
         */
        public void countMatches(String s) {
        	if(options.hasSuccessAnnouncement())
			{
				eventContent=new Text("rc=100 dc=" + s);
				playerMode=PlayerMode.SUCCESS;
				startPromptPhase(options.getSuccessAnnouncement());				
			}
			else
			{
				oc.fire(signal, new Text("rc=100 dc=" + s));
	            reset();
        		isCompleted=true;
        		complete();             				
			}           		     
        }

        /**
         * (Non Java-doc.)
         * 
         * @see BufferListener#tone(java.lang.String)  
         */
        public boolean tone(String s) {     
        	if(options.getDigitsNumber()>0 && s.charAt(0)==options.getEndInputKey() && buffer.length()>=options.getDigitsNumber())
        	{
        		logger.info(String.format("(%s) End Input Tone '%s' has been detected", getEndpoint().getLocalName(), s));
                //end input key still not included in sequence
       		 	if(options.hasSuccessAnnouncement())
       		 	{
       		 		if(options.isIncludeEndInputKey())
       		 			eventContent=new Text("rc=100 dc=" + buffer.getSequence() + s);
       		 		else
       		 			eventContent=new Text("rc=100 dc=" + buffer.getSequence());
       			 
    				 playerMode=PlayerMode.SUCCESS;
    				 startPromptPhase(options.getSuccessAnnouncement()); 
       		 	}
       		 	else
       		 	{
       		 		if(options.isIncludeEndInputKey())
       		 			oc.fire(signal, new Text("rc=100 dc=" + buffer.getSequence() + s));        			
       		 		else
       		 			oc.fire(signal, new Text("rc=100 dc=" + buffer.getSequence()));
       			 
       		 		reset();
       		 		isCompleted=true;
       		 		complete();
       		 	}
       		         		 
       		 	return true;
        	}
        	
        	logger.info(String.format("(%s) Tone '%s' has been detected", getEndpoint().getLocalName(), s));
        	if(isPromptActive)
        	{
        		if(options.prevKeyValid() && options.getPrevKey()==s.charAt(0))
        		{
        			prev(options.getInterval());
        			return false;
        		}
        		else if(options.firstKeyValid() && options.getFirstKey()==s.charAt(0))
        		{
        			first(options.getInterval());
        			return false;
        		}
        		else if(options.currKeyValid() && options.getCurrKey()==s.charAt(0))
        		{
        			curr(options.getInterval());
        			return false;
        		}
        		else if(options.nextKeyValid() && options.getNextKey()==s.charAt(0))
        		{
        			first(options.getInterval());
        			return false;
        		}
        		else if(options.lastKeyValid() && options.getLastKey()==s.charAt(0))
        		{
        			curr(options.getInterval());
        			return false;
        		}
        	}
        	
            if (!options.isNonInterruptable()) {
                if (isPromptActive) {
                    logger.info(String.format("(%s) Tone '%s' has been detected: prompt phase interrupted", getEndpoint().getLocalName(), s));
                    terminatePrompt();                    
                } else {
                    logger.info(String.format("(%s) Tone '%s' has been detected: collected", getEndpoint().getLocalName(), s));
                }
            } else {
                if (isPromptActive) {
                    logger.info(String.format("(%s) Tone '%s' has been detected, waiting for prompt phase termination", getEndpoint().getLocalName(), s));
                    if (options.isClearDigits())
                    	return false;
                } else {
                    logger.info(String.format("(%s) Tone '%s' has been detected: collected", getEndpoint().getLocalName(), s));
                }
            }  
            
            return true;
        }
    }
}
