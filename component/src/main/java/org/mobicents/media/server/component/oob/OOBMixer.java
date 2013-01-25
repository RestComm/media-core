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

package org.mobicents.media.server.component.oob;

import java.util.Iterator;

import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.IntConcurrentLinkedList;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 * Implements compound oob mixer , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 */
public class OOBMixer {
    //scheduler for mixer job scheduling
    private Scheduler scheduler;
    
    //The pool of components
    private IntConcurrentLinkedList<OOBComponent> components = new IntConcurrentLinkedList();
    
    Iterator<OOBComponent> activeComponents=components.iterator();
    
    private MixTask mixer;
    private volatile boolean started = false;

    public long mixCount = 0;
    
    //gain value
    private double gain = 1.0;
    
    public OOBMixer(Scheduler scheduler) {
        this.scheduler = scheduler;
        
        mixer = new MixTask();        
    }

    public void addComponent(OOBComponent component)
    {
    	components.offer(component,component.getComponentId());    	
    }
    
    /**
     * Releases unused input stream
     *
     * @param input the input stream previously created
     */
    public void release(OOBComponent component) {
    	components.remove(component.getComponentId());        
    }

    public void start() {
    	mixCount = 0;
    	started = true;
    	scheduler.submit(mixer,scheduler.MIXER_MIX_QUEUE);
    }    
    
    public void stop() {
    	started = false;
        mixer.cancel();        
    }

    private class MixTask extends Task {
    	int sourceComponent=0;
    	private Frame current;
        
        public MixTask() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {
        	//summarize all
        	components.resetIterator(activeComponents);
        	while(activeComponents.hasNext())
            {
            	OOBComponent component=activeComponents.next();
            	component.perform();
            	current=component.getData();
            	if(current!=null)
            	{
            		sourceComponent=component.getComponentId();
            		break;
            	}
            }
            
            if(current==null)
            {
            	scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
                mixCount++;            
                return 0;            
            }
            
            //get data for each component
            components.resetIterator(activeComponents);
        	while(activeComponents.hasNext())
            {        		
            	OOBComponent component=activeComponents.next();
            	if(component.getComponentId()!=sourceComponent)
            		component.offer(current.clone());            	
            }
            
            current.recycle();
            scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
            mixCount++;            
            return 0;            
        }
    }
}
