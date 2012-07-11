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

package org.mobicents.media.server.mgcp.pkg.trunk;

import org.apache.log4j.Logger;
import org.mobicents.media.server.mgcp.controller.signal.Event;
import org.mobicents.media.server.mgcp.controller.signal.NotifyImmediately;
import org.mobicents.media.server.mgcp.controller.signal.Signal;

import org.mobicents.media.server.impl.resource.phone.PhoneSignalGenerator;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetector;

import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;

import org.mobicents.media.server.spi.listener.TooManyListenersException;

import org.mobicents.media.server.spi.tone.ToneEvent;
import org.mobicents.media.server.spi.tone.ToneDetectorListener;
import org.mobicents.media.server.utils.Text;

import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Implements continuity test with co2 generated and co1 received.
 * 
 * @author Oifa Yulian
 */
public class Continuity2 extends Signal implements ToneDetectorListener {    
	private Event of = new Event(new Text("of"));
	private Event oc = new Event(new Text("oc"));
	private Event co1 = new Event(new Text("c01"));
	private Event co2 = new Event(new Text("c02"));
	
    private volatile Options options;
    
    private PhoneSignalGenerator phoneGenerator;
    private PhoneSignalDetector phoneDetector;
    
    private final static Logger logger = Logger.getLogger(Continuity2.class);    
    private Heartbeat heartbeat;
    
    public static final Text[] toneOptions={new Text("c01"),new Text("c02")};
    public static final int[] toneValues={2010,1780};
    
    public Continuity2(String name) {
        super(name);                   
        of.add(new NotifyImmediately("N"));
        oc.add(new NotifyImmediately("N"));
        co1.add(new NotifyImmediately("N"));
        co2.add(new NotifyImmediately("N"));
    }
    
    @Override
    public void execute() {
    	//get access to input and output
    	phoneGenerator = this.getPhoneGenerator();
        phoneDetector = this.getPhoneDetector();
        
        //check result
        if (phoneGenerator == null || phoneDetector ==null) {
            of.fire(this, new Text("Endpoint is not ss7 endpoint"));
            complete();
            return;
        }
        
        //get options of the request
        options = new Options(getTrigger().getParams());        
                
        if(options.isDeactivation())
        {
        	//deactivate pipe
        	phoneDetector.removeListener(this);
        	phoneGenerator.stop();
        	phoneDetector.stop();
        }
        else
        {
        	heartbeat=new Heartbeat(getEndpoint().getScheduler(),this);
        	prepareToneReceiving();
        }        	       
        
        //signal does not have anything else , only looping ss7 channel
    }
    
    @Override
    public boolean doAccept(Text event) {
    	if (!of.isActive() && of.matches(event)) {
            return true;
        }
        
        if (!oc.isActive() && oc.matches(event)) {
            return true;
        }
        
        if (!co1.isActive() && co1.matches(event)) {
            return true;
        }
        
        if (!co2.isActive() && co2.matches(event)) {
            return true;
        }
        
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        
        if(phoneDetector!=null)
    	{
    		phoneDetector.removeListener(this);
    		phoneDetector.stop();    		
    	}
    	
    	if(phoneGenerator!=null)
    		phoneGenerator.stop();
    	
    	if(heartbeat!=null)
    		heartbeat.disable();
        
        oc.reset();
        of.reset();
        co1.reset();
        co2.reset();
    }
    
    @Override
    public void cancel() {    
    	//deactivate pipe
    	if(phoneDetector!=null)
    	{
    		phoneDetector.removeListener(this);
    		phoneDetector.stop();    		
    	}
    	
    	if(phoneGenerator!=null)
    		phoneGenerator.stop();
    	
    	if(heartbeat!=null)
    		heartbeat.disable();
    }
    
    private PhoneSignalGenerator getPhoneGenerator() {
    	return (PhoneSignalGenerator) getEndpoint().getResource(MediaType.AUDIO, PhoneSignalGenerator.class); 
    }
    
    private PhoneSignalDetector getPhoneDetector() {
    	return (PhoneSignalDetector) getEndpoint().getResource(MediaType.AUDIO, PhoneSignalDetector.class); 
    }
    
    private void prepareToneReceiving()
    {
    	phoneGenerator.setFrequency(new int[] {toneValues[1]});
		phoneGenerator.start();
		
    	phoneDetector.setFrequency(toneValues);
    	phoneDetector.start();
    	
    	//set ttl to 2 seconds
    	heartbeat.setTtl((int)(40));
    	heartbeat.activate();
		getEndpoint().getScheduler().submitHeatbeat(heartbeat);
		
    	try
    	{
    		//set itself as listener
    		phoneDetector.addListener(this);
    	}
    	catch(Exception ex)
    	{
    		
    	}
    }    
    
    public void process(ToneEvent event) {
    	phoneDetector.removeListener(this);
    	phoneDetector.stop();
    	phoneGenerator.stop();
    	heartbeat.disable();
    	
    	//tone detected    	
    	if(event.getFrequency()==toneValues[0])
    	{
    		logger.info(String.format("(%s) Detected tone co1", getEndpoint().getLocalName()));
    		co1.fire(this, new Text(""));
    	}
    	else
    	{
    		logger.info(String.format("(%s) Detected tone co2", getEndpoint().getLocalName()));
    		co2.fire(this, new Text(""));
    	}
    }
    
    private class Heartbeat extends Task {
    	private AtomicInteger ttl;
    	private AtomicBoolean active;
    	
    	private Scheduler scheduler;
    	private Signal signal;
    	
    	public Heartbeat(Scheduler scheduler,Signal signal) {
        	super(scheduler);
        	
        	ttl=new AtomicInteger(-1);
        	active=new AtomicBoolean(false);
            this.scheduler=scheduler;
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
        	
        	if(ttlValue!=0)
        	{
        		if(ttlValue>0)
        			ttl.set(ttlValue-1);
        		
        		scheduler.submitHeatbeat(this);
        		return 0;
        	}
        	
        	logger.info(String.format("(%s) Timeout expired waiting for tone", getEndpoint().getLocalName()));
        	phoneDetector.stop();
        	
        	oc.fire(signal, new Text("t/co1"));  
        	complete();
        	this.disable();
        	return 0;
        }
    }
}
