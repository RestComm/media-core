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

package org.mobicents.media.server.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.ConcurrentLinkedList;
import org.mobicents.media.server.scheduler.IntConcurrentLinkedList;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.component.DtmfClamp;

/**
 * Splitts the media on to the different streams
 * @author kulikov
 */
public class Splitter {
	private final static int POOL_SIZE = 5;

	//scheduler
    private Scheduler scheduler;
    
    //Input stream
    private final Input input;

    //The pool of output streams
    private ConcurrentLinkedList<Output> pool = new ConcurrentLinkedList();

    //Active output streams
    private IntConcurrentLinkedList<Output> outputs = new IntConcurrentLinkedList();

    private Boolean dtmfClampActive=false;    
    private DtmfClamp dtmfClamp;
    
    protected long splitCount = 0;
    
    //stores last id used for output
    private AtomicInteger currentKey=new AtomicInteger(1);
    
    /**
     * Creates new Splitter.
     */
    public Splitter(Scheduler scheduler) {
    	this.scheduler=scheduler;
    	input = new Input();
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.offer(new Output(scheduler,currentKey.getAndIncrement()));
        }
        
        this.dtmfClamp=new DtmfClamp();
    }

    /**
     * Set dtmf clamp value
     * 
     * @param value should or should not dtmf clamp should be activated on input
     */
    public void setDtmfClamp(Boolean value)
    {
    	this.dtmfClampActive=value;
    	this.dtmfClamp.recycle();    	    	   
    }    

    /**
     * Gets input stream.
     *
     * @return the input stream as media sink
     */
    public MediaSink getInput() {
        return input;
    }

    /**
     * Creates new output stream.
     *
     * @return the new output stream as media source
     */
    public MediaSource newOutput() {
    	if(pool.isEmpty())
    		pool.offer(new Output(scheduler,currentKey.getAndIncrement()));
    	
        Output output = pool.poll();
        outputs.offer(output,output.outputId);
        return output;
    }


    /**
     * Releases unused output stream
     *
     * @param output the output stream previously created
     */
    public void release(MediaSource output) {
        ((Output)output).recycle();
    }

    public String report() {
        StringBuilder builder = new StringBuilder();
        builder.append("Splitter{\n");
        
        builder.append("     input:  (");
        builder.append(input.report());
        builder.append(")\n");
        
        Iterator<Output> activeOutputs=outputs.iterator();
        while(activeOutputs.hasNext()) {
            builder.append("     output: (");
            builder.append(activeOutputs.next().report());
            builder.append(")\n");
        }
        
        builder.append("}");
        return builder.toString();
    }
    
    /**
     * Implements input stream
     */
    private class Input extends AbstractSink {
    	Frame currFrame;
    	/**
         * Creates new stream.
         */
        public Input() {
        	super("splitter.input");
        }

        @Override
        public void stop()
        {
        	super.stop();
        	dtmfClamp.recycle();    
        	//System.out.println("SPLIT COUNT:" + splitCount);        	
        	splitCount=0;
        }
        
        @Override
        public void onMediaTransfer(Frame frame) throws IOException 
        {
        	currFrame=frame;
        	if(dtmfClampActive)
        		currFrame=dtmfClamp.process(currFrame);
        	
        	if(currFrame!=null)
        	{
        		splitCount++;
        		//clone frame and queue to each active output
        		Iterator<Output> activeOutputs=outputs.iterator();
                while(activeOutputs.hasNext())
                {
                	Output output=activeOutputs.next();
                	if (output.buffer.size() > Output.limit) 
                		output.buffer.poll().recycle();
                	
                	output.buffer.offer((Frame)currFrame.clone());
                    output.wakeup();
                }
                
                //recycle original frame
                currFrame.recycle();                
        	}
        }        
    }    
    
    /**
     * Implements output stream.
     *
     */
    private class Output extends AbstractSource {
        //buffer limit
        private final static int limit = 1;
        private int outputId;
        
        //transmission buffer
        private ConcurrentLinkedList<Frame> buffer = new ConcurrentLinkedList();

        /**
         * Creates new output stream.
         */
        public Output(Scheduler scheduler,int outputId) {
            super("splitter.output", scheduler,scheduler.SPLITTER_OUTPUT_QUEUE);
            this.outputId=outputId;
        }

        public int getOutputId()
        {
        	return outputId;
        }
        
        @Override
        public Frame evolve(long timestamp) {
            return buffer.poll();
        }        

        /**
         * Recycles output stream
         */
        protected void recycle() {
        	while(buffer.size()>0)
        		buffer.poll().recycle();
                    	
            outputs.remove(this.outputId);
            pool.offer(this);
        }
    }
}
