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

package org.restcomm.media.control.mgcp.pkg.au;

import java.util.Collection;
import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.utils.Text;
import org.restcomm.media.server.concurrent.ConcurrentCyclicFIFO;

/**
 *
 * @author yulian oifa
 */
public class EventBuffer implements DtmfDetectorListener {
    //string representation of queue;
    private String sequence = "";
    
    //patterns for even detection
    private String[] patterns=new String[0];	
    
    //the number of events to detect.
    private int count;
    
    //buffer state listener
    private BufferListener listener;
    
    private volatile boolean isActive = false;
    private ConcurrentCyclicFIFO<DtmfEvent> queue = new ConcurrentCyclicFIFO<DtmfEvent>();
    
    private Logger logger = Logger.getLogger(EventBuffer.class);
    
    public void activate() {
    	this.isActive = true;
    }
    
    public void passivate() {
        this.isActive = false;
    }
    
    public int length() {
        return sequence.length();
    }
    
    public String getSequence() {
        return sequence;
    }
    
    public void flush() { 
        //do nothing if digit collect phase is not active
        if (!this.isActive) {
            return;
        }
        
        //process buffered events        
        while (queue.size()!=0) {
            this.process(queue.poll());
        }
    }
    
    public void reset() {
        sequence = "";
        count = -1;
        queue.clear();        
    }
    
    public void clear() {
        sequence = "";
        //why should clear a count
        //count = -1;
        queue.clear();        
    }
    
    public void setPatterns(Collection<Text> patterns) {
    	if(patterns==null)
    	{
    		this.patterns=new String[0];
    		return;
    	}
    	
    	this.patterns=new String[patterns.size()];
    	int i = 0;            
        for (Text pattern : patterns) {
        	this.patterns[i]=pattern.toString();
        	//replacing to comply with Megaco digitMap
        	this.patterns[i]=this.patterns[i].replace(".", "+");
        	this.patterns[i]=this.patterns[i].replace("x", "\\d");
        	this.patterns[i]=this.patterns[i].replace("*", "\\*");
        	i++;
        }
        
        this.sequence = "";
    }
    
    public void setCount(int count) {
        this.count = count;
        this.sequence = "";
    }
    
    public void setListener(BufferListener listener) {
        this.listener = listener;
    }    

    public void process(DtmfEvent event) {
    	logger.info("Receive " + event.getTone() + " tone");
    	
    	if(!listener.tone(event.getTone()))
    		return;
        
    	//process event immediately if collect phase is active
        if (this.isActive) {
            process(event.getTone());
        } else {
            //buffer tone if collect phase is not activated yet
            queue.offer(event);
        }                          
    }
    
    private void process(String tone) {
    	sequence += tone;
        boolean sequenceFound=false;
        
        //check pattern matching for the entire sequence
        for (int i = 0;i<patterns.length;i++) {
            if (sequence.matches(patterns[i]) || tone.matches(patterns[i]))
            {
            	listener.patternMatches(i, sequence);
            	
            	//count = -1;
            	sequence = "";
            	sequenceFound=true;
            	break;
            }                
        }
        
        //check the amount of detected event and notify listener
        //if limit reached.
        if (!sequenceFound && count > 0 && sequence.length() == count) {
        	listener.countMatches(sequence);
            sequence = "";
            //count = -1;
        }
    }
    
}
