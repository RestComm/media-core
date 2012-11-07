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
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.utils.Text;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class PlayCollect extends Signal {
    
    private Event oc = new Event(new Text("oc"));
    private Event of = new Event(new Text("of"));
    
    
    private Player player;
    private DtmfDetector dtmfDetector;
    
    private Options options;
    private EventBuffer buffer = new EventBuffer();
    
    private PromptHandler promptHandler;
    private DtmfHandler dtmfHandler;
    
    private volatile boolean isPromptActive;
    private Text[] prompt=new Text[10];
    private int promptLength=0,promptIndex=0;
    
    private final static Logger logger = Logger.getLogger(PlayCollect.class);
    
    private long firstDigitTimer=0;
    private long nextDigitTimer=0;
    private long maxDuration=0;
    private int numberOfAttempts=1;
    
    private Heartbeat heartbeat;
    private int segCount = 0;
        
    private PlayerMode playerMode=PlayerMode.PROMPT;
    private Text eventContent;
    
    private Scheduler scheduler;
    
    private Boolean dtmfListenerAdded=false;
    private Boolean playerListenerAdded=false;
    
    public PlayCollect(String name) {
        super(name);
        oc.add(new NotifyImmediately("N"));
        oc.add(new InteruptPrompt("S", player));
        of.add(new NotifyImmediately("N"));
        
        dtmfHandler = new DtmfHandler(this);
        promptHandler = new PromptHandler(this);                
    }
    
    @Override
    public void execute() {
    	if(getEndpoint().getActiveConnectionsCount()==0)
    	{
    		oc.fire(this, new Text("rc=326"));
    		this.complete();
    		return;
    	}
    	
    	playerMode=PlayerMode.PROMPT;
    	promptLength=0;
    	promptIndex=0;
    	segCount = 0;
    	this.scheduler=getEndpoint().getScheduler();
    	heartbeat=new Heartbeat(this);
    	
        //get options of the request
        options = new Options(getTrigger().getParams());

        logger.info(String.format("(%s) Prepare digit collect phase", getEndpoint().getLocalName()));
        //Initializes resources for DTMF detection
        //at this stage DTMF detector started but local buffer is not assigned 
        //yet as listener
        prepareCollectPhase(options);
        
        if(options.getFirstDigitTimer()>0)
        	this.firstDigitTimer=options.getFirstDigitTimer();
        else
        	this.firstDigitTimer=0;
        
        if(options.getInterDigitTimer()>0)
        	this.nextDigitTimer=options.getInterDigitTimer();
        else
        	this.nextDigitTimer=0;
                
        if(options.getMaxDuration()>0)
        	this.maxDuration=options.getMaxDuration();
        else
        	this.maxDuration=0;
        
        if(options.getNumberOfAttempts()>1)
        	this.numberOfAttempts=options.getNumberOfAttempts();
        else
        	this.numberOfAttempts=1;
        
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
        	if(!playerListenerAdded)
        	{
        		player.addListener(promptHandler);
        		playerListenerAdded=true;
        	}
        	
            promptLength=promptList.size();
            prompt = promptList.toArray(prompt);            
            player.setURL(prompt[0].toString());
            
            //specify URL to play
            //player.setURL(options.getPrompt().toString());
            
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
            playerListenerAdded=false;
            player=null;
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
        
        //clear local buffer
        buffer.reset();
        buffer.setListener(dtmfHandler);
        
        //assign requested parameters
        buffer.setPatterns(options.getDigitPattern());
        if(options.getMaxDigitsNumber()>0)
        	buffer.setCount(options.getMaxDigitsNumber());
        else
        	buffer.setCount(options.getDigitsNumber());               
    }
    
    /**
     * Flushes DTMF buffer content to local buffer
     */
    private void flushBuffer() {        
        try {
        	//attach local buffer to DTMF detector
            //but do not flush
        	if(!dtmfListenerAdded)
        	{
        		dtmfDetector.addListener(buffer);
        		dtmfListenerAdded=true;
        	}
        	
            dtmfDetector.flushBuffer();            
        } catch (TooManyListenersException e) {
            of.fire(this, new Text("Too many listeners for DTMF detector"));
            logger.error("OPERATION FAILURE", e);
        }        
    }
    
    private void startCollectPhase() {
    	if(this.firstDigitTimer>0 || this.maxDuration>0)
    	{
    		if(this.firstDigitTimer>0)
    			heartbeat.setTtl((int)(this.firstDigitTimer/100000000L));
    		else
    			heartbeat.setTtl(-1);
    		
    		if(this.maxDuration>0)
    			heartbeat.setOverallTtl((int)(this.maxDuration/100000000L));
    		else
    			heartbeat.setOverallTtl(-1);
    		
    		heartbeat.activate();
    		getEndpoint().getScheduler().submitHeatbeat(heartbeat);
    	}    	
    	
    	buffer.activate();        
        buffer.flush();
    }
    
    /**
     * Terminates digit collect phase.
     */
    private void terminateCollectPhase() {
        if (dtmfDetector != null) {
            dtmfDetector.removeListener(buffer);
            dtmfListenerAdded=false;
            
           //dtmfDetector.clearDigits();
            
            buffer.passivate();
            buffer.clear();      
            dtmfDetector=null;
        }
    }
    
    /**
     * Terminates any activity. 
     */
    private void terminate() {
    	this.isPromptActive=false;
    	this.terminatePrompt();
        this.terminateCollectPhase();
        
        if(this.heartbeat!=null)
    		this.heartbeat.disable();
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
    	if(this.heartbeat!=null)
    		this.heartbeat.disable();
    	
        this.isPromptActive = false;
        this.terminate();
    }

    private Player getPlayer() {
    	Endpoint endpoint = getEndpoint();
        return (Player) getEndpoint().getResource(MediaType.AUDIO, ComponentType.PLAYER);
    }

    private DtmfDetector getDetector() {
    	Endpoint endpoint = getEndpoint();
        return (DtmfDetector) getEndpoint().getResource(MediaType.AUDIO, ComponentType.DTMF_DETECTOR);
    }    
    
    @Override
    public void reset() {
        super.reset();
        //disable signal activity and terminate
        
        this.isPromptActive = false;
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
    
    private void decreaseNa()
    {
    	numberOfAttempts--;
    	if (options.hasReprompt()) {
			buffer.passivate();
			isPromptActive = true;
			startPromptPhase(options.getReprompt());	        
			heartbeat.disable();			
		}
    	else if (options.hasPrompt()) {
			buffer.passivate();
			isPromptActive = true;
			startPromptPhase(options.getPrompt());
			heartbeat.disable();			
		} else
			startCollectPhase();		
    }
    
    /**
     * Handler for prompt phase.
     */
    private class PromptHandler implements PlayerListener {

        private PlayCollect signal;
        
        /**
         * Creates new handler instance.
         * 
         * @param signal the play record signal instance
         */
        protected PromptHandler(PlayCollect signal) {
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
                        
                				logger.info(String.format("(%s) Prompt phase terminated, start collect phase", getEndpoint().getLocalName()));
                				startCollectPhase();
                			}
                			break;
                		case SUCCESS:
                			oc.fire(signal, eventContent);
                			reset();
                    		complete();
                			break;
                		case FAILURE:
                			if(numberOfAttempts==1)
                			{
                				oc.fire(signal, eventContent);
                				reset();
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
     * Handler for digit collect phase.
     * 
     */
    private class DtmfHandler implements BufferListener {

        private PlayCollect signal;

        /**
         * Constructor for this handler.
         * @param signal 
         */
        public DtmfHandler(PlayCollect signal) {
            this.signal = signal;
        }
        
        /**
         * (Non Java-doc.)
         * 
         * @see BufferListener#patternMatches(int, java.lang.String) 
         */
        public void patternMatches(int index, String s) {
            logger.info(String.format("(%s) Collect phase: pattern has been detected", getEndpoint().getLocalName()));
            String naContent="";
    		if(options.getNumberOfAttempts()>1)
    			naContent=" na=" + options.getNumberOfAttempts();
    		
    		if(options.hasSuccessAnnouncement())
			{
				eventContent=new Text("rc=100 dc=" + s + " pi=" + index + naContent);
				playerMode=PlayerMode.SUCCESS;
				startPromptPhase(options.getSuccessAnnouncement());				
			}
			else
			{
				oc.fire(signal, new Text("rc=100 dc=" + s + " pi=" + index + naContent));
				reset();
	            complete();
			}
        }
        
        /**
         * (Non Java-doc.)
         * 
         * @see BufferListener#countMatches(java.lang.String) 
         */
        public void countMatches(String s) {
            logger.info(String.format("(%s) Collect phase: max number of digits has been detected", getEndpoint().getLocalName()));
            String naContent="";
    		if(options.getNumberOfAttempts()>1)
    			naContent=" na=" + options.getNumberOfAttempts();
    		
    		if(options.hasSuccessAnnouncement())
			{
				eventContent=new Text("rc=100 dc=" + s + naContent);
				playerMode=PlayerMode.SUCCESS;
				startPromptPhase(options.getSuccessAnnouncement());				
			}
			else
			{
				oc.fire(signal, new Text("rc=100 dc=" + s + naContent));
	            reset();
	            complete();	            				
			}            
        }

        /**
         * (Non Java-doc.)
         * 
         * @see BufferListener#tone(java.lang.String)  
         */
        public boolean tone(String s) {
        	if(options.getMaxDigitsNumber()>0 && s.charAt(0)==options.getEndInputKey() && buffer.length()>=options.getDigitsNumber())
        	{
        		String naContent="";
        		if(options.getNumberOfAttempts()>1)
        			naContent=" na=" + options.getNumberOfAttempts();
        		
        		 logger.info(String.format("(%s) End Input Tone '%s' has been detected", getEndpoint().getLocalName(), s));
                 //end input key still not included in sequence
        		 if(options.hasSuccessAnnouncement())
        		 {
        			 if(options.isIncludeEndInputKey())
        				 eventContent=new Text("rc=100 dc=" + buffer.getSequence() + s + naContent);
        			 else
        				 eventContent=new Text("rc=100 dc=" + buffer.getSequence() + naContent);
        			 
     				 playerMode=PlayerMode.SUCCESS;
     				 startPromptPhase(options.getSuccessAnnouncement()); 
        		 }
        		 else
        		 {
        			 if(options.isIncludeEndInputKey())
        				 oc.fire(signal, new Text("rc=100 dc=" + buffer.getSequence() + s + naContent));        			
        			 else
        				 oc.fire(signal, new Text("rc=100 dc=" + buffer.getSequence() + naContent));
        			 
        			 heartbeat.disable();
            		 reset();
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
            
            if(nextDigitTimer>0)
        	{
        		heartbeat.setTtl((int)(nextDigitTimer/100000000L));
        		if(!heartbeat.isActive())
        		{
        			heartbeat.activate();
        			getEndpoint().getScheduler().submitHeatbeat(heartbeat);        			
        		}
        	}
        	else if(maxDuration==0)
        		heartbeat.disable();  
            
            return true;
        }
        
    }
    
    private class Heartbeat extends Task {
    	private AtomicInteger ttl;
    	private AtomicInteger overallTtl;
    	private AtomicBoolean active;
    	
    	private Signal signal;
    	
        public Heartbeat(Signal signal) {
        	super();
        	
        	ttl=new AtomicInteger(-1);
        	overallTtl=new AtomicInteger(-1);
        	active=new AtomicBoolean(false);
            this.signal=signal;
        }
        
        public int getQueueNumber()
        {
        	return scheduler.HEARTBEAT_QUEUE;
        }     
        
        public void setTtl(int value)
        {
        	ttl.set(value);        	        
        }
        
        public void setOverallTtl(int value)
        {
        	overallTtl.set(value);        	        
        }
        
        public void disable()
        {
        	this.active.set(false);        	
        }
        
        public void activate()
        {
        	this.active.set(true);  	
        }
        
        public boolean isActive()
        {
        	return this.active.get();
        }

        @Override
        public long perform() {        	
        	if(!active.get())
        		return 0;
        	
        	int ttlValue=ttl.get();
        	int overallTtlValue=overallTtl.get();
        	if(ttlValue!=0 && overallTtlValue!=0)
        	{
        		if(ttlValue>0)
        			ttl.set(ttlValue-1);
        		
        		if(overallTtlValue>0)
        			overallTtl.set(overallTtlValue-1);
        		
        		scheduler.submitHeatbeat(this);
        		return 0;
        	}
            
        	logger.info(String.format("(%s) Timeout expired waiting for dtmf", getEndpoint().getLocalName()));
        	if(numberOfAttempts==1)
        	{
        		String naContent="";
        		if(options.getNumberOfAttempts()>1)
        			naContent=" na=" + options.getNumberOfAttempts();
        			
        		if(ttlValue==0) {
        			int length=buffer.getSequence().length();
        			if(options.getDigitsNumber()>0 && length>=options.getDigitsNumber())
        			{
        				if(options.hasSuccessAnnouncement())
        				{
        					eventContent=new Text("rc=100 dc=" + buffer.getSequence() + naContent);
        					playerMode=PlayerMode.SUCCESS;
        					startPromptPhase(options.getSuccessAnnouncement());
        				}
        				else
        				{
        					oc.fire(signal, new Text("rc=100 dc=" + buffer.getSequence() + naContent));
        					reset();
        		            complete();
        				}        					
        			}    				
        			else if(length>0)
        			{
        				if(options.hasNoDigitsReprompt())
        				{
        					eventContent=new Text("rc=326 dc=" + buffer.getSequence() + naContent);
        					playerMode=PlayerMode.FAILURE;
        					startPromptPhase(options.getNoDigitsReprompt());
        				}
        				else if(options.hasFailureAnnouncement())
        				{
        					eventContent=new Text("rc=326 dc=" + buffer.getSequence() + naContent);
        					playerMode=PlayerMode.FAILURE;
        					startPromptPhase(options.getFailureAnnouncement());
        				}
        				else
        				{
        					oc.fire(signal, new Text("rc=326 dc=" + buffer.getSequence() + naContent));
        					reset();
        		            complete();
        				}        				
        			}
        			else
        			{
        				if(options.hasNoDigitsReprompt())
        				{
        					eventContent=new Text("rc=326" + naContent);
        					playerMode=PlayerMode.FAILURE;
        					startPromptPhase(options.getNoDigitsReprompt());
        				}
        				else if(options.hasFailureAnnouncement())
        				{
        					eventContent=new Text("rc=326" + naContent);
        					playerMode=PlayerMode.FAILURE;
        					startPromptPhase(options.getFailureAnnouncement());
        				}
        				else
        				{
        					oc.fire(signal, new Text("rc=326" + naContent));
        					reset();
        		            complete();
        				}        					
        			}        				
        		}
        		else
        		{
        			if(options.hasNoDigitsReprompt())
    				{
    					eventContent=new Text("rc=330" + naContent);
    					playerMode=PlayerMode.FAILURE;
    					startPromptPhase(options.getNoDigitsReprompt());
    				}
    				else if(options.hasFailureAnnouncement())
    				{
    					eventContent=new Text("rc=330" + naContent);
    					playerMode=PlayerMode.FAILURE;
    					startPromptPhase(options.getFailureAnnouncement());
    				}
    				else
    				{
    					oc.fire(signal, new Text("rc=330" + naContent));
    					reset();
    		            complete();
    				}
        		}        		        		        	
        	}
        	else
        	{
        		buffer.reset();
        		decreaseNa();
        	}
        	
        	return 0;        	
        }
	}
}
