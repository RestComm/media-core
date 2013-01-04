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
 * Implements compound oob splitter , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 */
public class OOBSplitter {
    //scheduler for mixer job scheduling
    private Scheduler scheduler;
    
    //The pools of components
    private IntConcurrentLinkedList<OOBComponent> insideComponents = new IntConcurrentLinkedList();
    private IntConcurrentLinkedList<OOBComponent> outsideComponents = new IntConcurrentLinkedList();
        
    private InsideMixTask insideMixer;
    private OutsideMixTask outsideMixer;
    private volatile boolean started = false;

    protected long mixCount = 0;
    
    public OOBSplitter(Scheduler scheduler) {
        this.scheduler = scheduler;
        
        insideMixer = new InsideMixTask();        
        outsideMixer = new OutsideMixTask();
    }

    public void addInsideComponent(OOBComponent component)
    {
    	insideComponents.offer(component,component.getComponentId());    	
    }
    
    public void addOutsideComponent(OOBComponent component)
    {
    	outsideComponents.offer(component,component.getComponentId());    	
    }
    
    /**
     * Releases inside component
     *
     * @param component
     */
    public void releaseInsideComponent(OOBComponent component) {
    	insideComponents.remove(component.getComponentId());        
    }
    
    /**
     * Releases outside component
     *
     * @param component
     */
    public void releaseOutsideComponent(OOBComponent component) {
    	outsideComponents.remove(component.getComponentId());        
    }
    
    public void start() {
    	mixCount = 0;
    	started = true;
    	scheduler.submit(insideMixer,scheduler.MIXER_MIX_QUEUE);
    	scheduler.submit(outsideMixer,scheduler.MIXER_MIX_QUEUE);
    }    
    
    public void stop() {
    	started = false;
    	insideMixer.cancel();
    	outsideMixer.cancel();
    }

    private class InsideMixTask extends Task {
    	private Frame current;
        
        public InsideMixTask() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {
            //summarize all
        	current=null;
            Iterator<OOBComponent> activeComponents=insideComponents.iterator();
            while(activeComponents.hasNext())
            {
            	OOBComponent component=activeComponents.next();
            	component.perform();
            	current=component.getData();
            	if(current!=null)
            		break;            	
            }

            if(current==null)
            {
            	scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
                mixCount++;            
                return 0;            
            }
            
            //get data for each component
            activeComponents=outsideComponents.iterator();
            while(activeComponents.hasNext())
            {
            	OOBComponent component=activeComponents.next();
            	if(!activeComponents.hasNext())
            		component.offer(current);
            	else
            		component.offer(current.clone());            		
            }
            
            scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
            mixCount++;            
            
            return 0;            
        }
    }
    
    private class OutsideMixTask extends Task {
    	private Frame current;
        
        public OutsideMixTask() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {
            //summarize all
            current=null;
            Iterator<OOBComponent> activeComponents=outsideComponents.iterator();
            while(activeComponents.hasNext())
            {
            	OOBComponent component=activeComponents.next();
            	component.perform();
            	current=component.getData();
            	if(current!=null)
            		break;            	
            }

            if(current==null)
            {
            	scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
                mixCount++;            
                return 0;            
            }
            
            //get data for each component
            activeComponents=insideComponents.iterator();
            while(activeComponents.hasNext())
            {
            	OOBComponent component=activeComponents.next();
            	if(!activeComponents.hasNext())
            		component.offer(current);
            	else
            		component.offer(current.clone());
            }
            
            scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
            mixCount++;            
            
            return 0;
        }
    }
}
