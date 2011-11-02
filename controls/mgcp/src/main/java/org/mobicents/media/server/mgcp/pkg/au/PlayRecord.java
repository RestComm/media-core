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

import org.apache.log4j.Logger;
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
 * @author kulikov
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
    private boolean isCompleted;
    private final static Logger logger = Logger.getLogger(PlayRecord.class);
    
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
    	
    	isCompleted=false;
    	//set flag active signal
        this.isActive = true;
        //get options of the request
        options = new Options(getTrigger().getParams());        
        
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
            startPromptPhase(options);
            
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
    private void startPromptPhase(Options options) {
        player = this.getPlayer();        
        try {
            //assign listener
            player.addListener(promptHandler);
            
            //specify URL to play
            player.setURL(options.getPrompt().toString());
            
            //start playback
            player.start();
        } catch (TooManyListenersException e) {
            of.fire(this, new Text("Too many listeners"));
        } catch (Exception e) {
            of.fire(this, new Text(e.getMessage()));
        }
    }

    /**
     * Terminates prompt phase if it was started or do nothing otherwise.
     */
    private void terminatePrompt() {
        if (player != null) {
            player.stop();
            player.removeListener(promptHandler);
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
        
        try {
        	recorder.addListener(recordingHandler);
            
            recorder.setRecordFile(options.getRecordID().toString(), !options.isOverride());
            recorder.start();
        } catch (Exception e) {
            of.fire(this, new Text(e.getMessage()));
        }
    }
    
    
    /**
     * Terminates prompt phase if it was started or do nothing otherwise.
     */
    private void terminateRecordPhase() {
        if (recorder != null) {
            recorder.stop();
            recorder.removeListener(recordingHandler);
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
        this.terminatePrompt();
        this.terminateRecordPhase();
        this.terminateCollectPhase();
    }
    
    private Player getPlayer() {
        if (getTrigger().getConnectionID() == null) {
            Endpoint endpoint = getEndpoint();
            return (Player) getEndpoint().getResource(MediaType.AUDIO, Player.class); 
        }
        
        String connectionID = getTrigger().getConnectionID().toString();
        Connection connection = getConnection(connectionID);
        
        if (connection == null) {
        	   return null;
        }
        
        return null;
    }

    
    private Recorder getRecorder() {
        if (getTrigger().getConnectionID() == null) {
            Endpoint endpoint = getEndpoint();
            return (Recorder) getEndpoint().getResource(MediaType.AUDIO, Recorder.class); 
        }
        
        String connectionID = getTrigger().getConnectionID().toString();
        Connection connection = getConnection(connectionID);
        
        if (connection == null) {
            return null;
        }
        
        return null;
    }
    
    private DtmfDetector getDetector() {
        if (getTrigger().getConnectionID() == null) {
            Endpoint endpoint = getEndpoint();
            return (DtmfDetector) getEndpoint().getResource(MediaType.AUDIO, DtmfDetector.class); 
        }
        
        String connectionID = getTrigger().getConnectionID().toString();
        Connection connection = getConnection(connectionID);
        
        if (connection == null) {
            return null;
        }
        
        return null;
    }
    
    @Override
    public void reset() {
        super.reset();

        this.terminate();
        
        oc.reset();
        of.reset();
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
                    flushBuffer();
                    break;
                case PlayerEvent.STOP :
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
                            oc.fire(signal, new Text("rc=328"));                            
                            break;
                        case RecorderEvent.NO_SPEECH :
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
        		oc.fire(signal, new Text("rc=100 dc=" + s + " pi=" + index));        		
        		reset();
        		isCompleted=true;
        		complete();        	
        }
        
        /**
         * (Non Java-doc.)
         * 
         * @see BufferListener#countMatches(java.lang.String) 
         */
        public void countMatches(String s) {
        		oc.fire(signal, new Text("rc=100 dc=" + s));        		
        		reset();
        		isCompleted=true;
        		complete();        	
        }

        /**
         * (Non Java-doc.)
         * 
         * @see BufferListener#tone(java.lang.String)  
         */
        public void tone(String s) {
        	logger.info(String.format("(%s) Tone '%s' has been detected", getEndpoint().getLocalName(), s));
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
                } else {
                    logger.info(String.format("(%s) Tone '%s' has been detected: collected", getEndpoint().getLocalName(), s));
                }
            }            
        }
        
    }
}
