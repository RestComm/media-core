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

package org.mobicents.media.server.component.audio;

import java.util.Iterator;
import org.mobicents.media.server.scheduler.IntConcurrentLinkedList;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 * Implements compound components used by mixer and splitter.
 * 
 * @author Yulian Oifa
 */
public class CompoundComponent {
	//the format of the output stream.
    private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 8;    

    private IntConcurrentLinkedList<CompoundInput> inputs = new IntConcurrentLinkedList();
	private IntConcurrentLinkedList<CompoundOutput> outputs = new IntConcurrentLinkedList();
    
	protected Boolean shouldRead=false;
	protected Boolean shouldWrite=false;
	
	//samples storage
	private int[] data;
	
	private byte[] dataArray;
	private Frame frame;
	
	int i,k;
	boolean first;
	
	private int componentId;
    /**
     * Creates new instance with default name.
     */
    public CompoundComponent(int componentId) {
    	this.packetSize=packetSize;
    	this.componentId=componentId;
    	data=new int[packetSize/2];
    }

    public int getComponentId()
    {
    	return componentId;
    }
    
    public void updateMode(Boolean shouldRead,Boolean shouldWrite)
    {
    	this.shouldRead=shouldRead;
    	this.shouldWrite=shouldWrite;
    }
    
    public void addInput(CompoundInput input) {
    	inputs.offer(input,input.getInputId());
    }

    public void addOutput(CompoundOutput output) {
    	outputs.offer(output,output.getOutputId());
    }
    
    public void remove(CompoundInput input)
    {
    	inputs.remove(input.getInputId());
    }
    
    public void remove(CompoundOutput output)
    {
    	outputs.remove(output.getOutputId());
    }
    
    public void perform()
    {
    	first=true;    	
    	Iterator<CompoundInput> activeInputs=inputs.iterator();
        while(activeInputs.hasNext())
        {
        	CompoundInput input=activeInputs.next();
        	frame=input.poll();
        	if(frame!=null)
        	{
        		dataArray=frame.getData();
        		if(first)
        		{
        			k=0;
        			for(i=0;i<dataArray.length;i+=2)
        				data[k++]=(short) (((dataArray[i + 1]) << 8) | (dataArray[i] & 0xff));
        			
        			first=false;
        		}
        		else
        		{
        			k=0;
        			for(i=0;i<dataArray.length;i+=2)
        				data[k++]+=(short) (((dataArray[i + 1]) << 8) | (dataArray[i] & 0xff));
        		}
        		
        		frame.recycle();
        	}        	   	   
        }
    }
    
    public int[] getData()
    {
    	if(!this.shouldRead)
    		return null;
    	
    	if(first)
    		return null;
        
        return data;
    }
    
    public void offer(int[] data)
    {
    	if(!this.shouldWrite)
    		return;
    	
    	Frame frame=Memory.allocate(packetSize);
    	dataArray=frame.getData();
    	
    	k=0;
    	for(i=0;i<data.length;)
    	{
    		dataArray[k++]=(byte) (data[i]);
    		dataArray[k++]=(byte) (data[i++] >> 8);
    	}
    	
    	frame.setOffset(0);
        frame.setLength(packetSize);
        frame.setDuration(period);
        frame.setFormat(format);
        
        Iterator<CompoundOutput> activeOutputs=outputs.iterator();
    	while(activeOutputs.hasNext())
        {
        	CompoundOutput output=activeOutputs.next();
        	if(!activeOutputs.hasNext())
        		output.offer(frame);
        	else
        		output.offer(frame.clone());        		        	
        	
        	output.wakeup();
        }
    }
}
