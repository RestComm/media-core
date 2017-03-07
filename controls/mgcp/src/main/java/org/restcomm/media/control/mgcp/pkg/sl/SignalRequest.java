/*
 * Telestax, Open Source Cloud Communications
 * Copyright 2013, Telestax, Inc. and individual contributors
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
package org.restcomm.media.control.mgcp.pkg.sl;

import org.apache.log4j.Logger;
import org.restcomm.media.ComponentType;
import org.restcomm.media.control.mgcp.controller.signal.Event;
import org.restcomm.media.control.mgcp.controller.signal.NotifyImmediately;
import org.restcomm.media.control.mgcp.controller.signal.Signal;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Task;
import org.restcomm.media.spi.MediaType;
import org.restcomm.media.spi.dtmf.DtmfGenerator;
import org.restcomm.media.spi.dtmf.DtmfGeneratorEvent;
import org.restcomm.media.spi.dtmf.DtmfGeneratorListener;
import org.restcomm.media.spi.utils.Text;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author Yulian Oifa
 */
public class SignalRequest extends Signal implements DtmfGeneratorListener 
{
        private static final Logger logger = Logger.getLogger(SignalRequest.class);
        
        private DtmfGenerator generator;
        
        private Event oc;         // Operation Completed
        private Event of;               // Operation Failed
  
        private ArrayBlockingQueue<DtmfSignal> signals;
        DtmfSignal currentSignal;
        
        private Semaphore terminateSemaphore=new Semaphore(1);
        private Heartbeat heartbeat;
        
        private PriorityQueueScheduler scheduler;
        
        public SignalRequest(final String name) 
        {
        	super(name);
        	oc = new Event(new Text("oc"));
        	oc.add(new NotifyImmediately("N"));
        	of = new Event(new Text("of"));
        	of.add(new NotifyImmediately("N"));
    
        	signals = null;        	
        }

        @Override 
        public void execute()
        {
            try 
            {
            	final Text parameters = getTrigger().getParams();
                signals = Options.parse(parameters);
            } 
            catch(final IllegalArgumentException exception) 
            {
            	logger.error("There was an error parsing the signal list.", exception);
            	of.fire(this, new Text("rc=538"));
            	return;
            }
                
            // If there are no signals to generate we are done.
            if(signals.size() == 0) 
            {
            	oc.fire(this, new Text("rc=100"));
            	return;
            }
                
            this.scheduler=getEndpoint().getScheduler();
            heartbeat=new Heartbeat(this);
            
            // Lets send the signals.
            generator = getDtmfGenerator();
        	generator.addListener(this);
        	
        	currentSignal = signals.poll();
            generator.setOOBDigit(currentSignal.getDigit());
            generator.setToneDuration(currentSignal.getDuration());
            generator.activate();
        }

        @Override 
        public boolean doAccept(final Text event) 
        {
            if(!oc.isActive() && oc.matches(event)) 
              	return true;
            if(!of.isActive() && of.matches(event)) 
               	return true;
          
            return false;
        }

        @Override 
        public void cancel() 
        {            
        	terminate();
        }
        
        private void terminate() 
        {
            try
            {
                terminateSemaphore.acquire();
            }
            catch(InterruptedException e)
            {
                    
            }
         
            if(generator!=null)
            {
            	generator.removeListener(this);
            	generator.deactivate();
            	generator=null;
            }
            
            if(signals!=null)
            {
            	Options.recycle(signals);            
            	signals=null;
            }
            
            if(this.heartbeat!=null)
            {
                    this.heartbeat.disable();
                    this.heartbeat=null;
            }
            
            terminateSemaphore.release();
        }
                
        private DtmfGenerator getDtmfGenerator() 
        {
        	return (DtmfGenerator)getEndpoint().getResource(MediaType.AUDIO,ComponentType.DTMF_GENERATOR);
        }

        public void process(DtmfGeneratorEvent event) 
        {         
        	generator.deactivate();
        	heartbeat.activate();
            getEndpoint().getScheduler().submitHeatbeat(heartbeat);
        }
        
        private class Heartbeat extends Task 
        {
        	private AtomicBoolean active;            
            private Signal signal;
            
            public Heartbeat(Signal signal) 
            {
                super();
                active=new AtomicBoolean(false);
                this.signal=signal;
            }
            
            public int getQueueNumber()
            {
                    return PriorityQueueScheduler.HEARTBEAT_QUEUE;
            }     
            
            public void disable()
            {
                    this.active.set(false);         
            }
            
            public void activate()
            {
                    this.active.set(true);          
            }
            
            @Override
            public long perform() 
            {      
            	generator.deactivate();
            	currentSignal = signals.poll();
                if(currentSignal!=null)
                {
                    generator.setOOBDigit(currentSignal.getDigit());
                    generator.setToneDuration(currentSignal.getDuration());
                    generator.activate();
                }
                else
                {
                	terminate();
                	oc.fire(signal, new Text("rc=100"));
                }
                
                return 0;     
            }
        }
}