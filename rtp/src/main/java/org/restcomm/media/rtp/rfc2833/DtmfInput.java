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

package org.restcomm.media.rtp.rfc2833;

import java.util.ArrayList;

import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.server.component.AbstractSource;
import org.restcomm.media.server.component.oob.OOBInput;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 *
 * @author yulian oifa
 */
public class DtmfInput extends AbstractSource {
   
	private static final long serialVersionUID = 1648858097848867435L;

	private final static AudioFormat dtmf = FormatFactory.createAudioFormat("telephone-event", 8000);
	private long period = 20000000L;
    private int packetSize = 4;
    
    private ArrayList<Frame> frameBuffer=new ArrayList<Frame>(5);
    private Frame currFrame;
    
    private byte currTone=(byte)0xFF;
    private int latestDuration=0;
    private int latestSeq=0;
    
    private boolean hasEndOfEvent;
    private long endTime=0;
    private int endSeq=0;
    
    private int eventDuration=0;
    byte[] data = new byte[4];
    
    boolean endOfEvent;
    
    private RtpClock clock;    
    
    private OOBInput input;
	
    public DtmfInput(PriorityQueueScheduler scheduler,RtpClock clock)
    {
    	super("dtmfconverter", scheduler, PriorityQueueScheduler.INPUT_QUEUE);
    	
    	this.clock=clock;
    	this.clock.setClockRate(8000);
    	
    	input=new OOBInput(2);
        this.connect(input);       
    }
    
    public OOBInput getOOBInput()
    {
    	return this.input;
    }
    
    public void setClock(RtpClock clock) 
    {
        this.clock = clock;
        this.clock.setClockRate(8000);        
    }
    
    public void write(RtpPacket event) 
    {
    	//obtain payload        
        event.getPayload(data, 0);
        
        if(data.length==0)
        	return;
    	
    	boolean endOfEvent=false;
        if(data.length>1)
            endOfEvent=(data[1] & 0X80)!=0;
        
       //lets ignore end of event packets
        if(endOfEvent)
        {
        	hasEndOfEvent=true;
        	endTime=event.getTimestamp();
        	endSeq=event.getSeqNumber();
        	return;                                       
        }
        
        eventDuration=(data[2]<<8) | (data[3] & 0xFF);    	
    	
        //lets update sync data , allowing same tone come after 160ms from previous tone , not including end of tone
        if(currTone==data[0])
        {
        	if(hasEndOfEvent)
        	{
        		if((event.getSeqNumber()<=endSeq && event.getSeqNumber()>(endSeq-8)))
        			//out of order , belongs to same event 
        			//if comes after end of event then its new one
        			return;
        	}
        	else
        	{
        		if((event.getSeqNumber()<(latestSeq+8)) && event.getSeqNumber()>(latestSeq-8))
        		{
        			if(event.getSeqNumber()>latestSeq)
        			{
        				latestSeq=event.getSeqNumber();
        				latestDuration=eventDuration;
        			}
        		
        			return;
        		}
        	
        		if(eventDuration<(latestDuration+1280) && eventDuration>(latestDuration-1280))        		
        		{
        			if(eventDuration>latestDuration)
        			{
        				latestSeq=event.getSeqNumber();
        				latestDuration=eventDuration;
        			}
        		
        			return;
        		}
        	}
        }
        
        hasEndOfEvent=false;
    	endTime=0;
    	endSeq=0;
        latestSeq=event.getSeqNumber();
        latestDuration=eventDuration;
        currTone=data[0];
        for(int i=0;i<7;i++)
        {
        	currFrame = Memory.allocate(packetSize);
        	byte[] newData=currFrame.getData();
        	newData[0]=data[0];
        	newData[1]=(byte)(0x3F & data[1]);
        	eventDuration=(short)(160*i);
        	newData[2]=(byte)((eventDuration>>8) & 0xFF);
        	newData[3]=(byte)(eventDuration & 0xFF);        	
        	currFrame.setSequenceNumber(event.getSeqNumber()+i);
        	currFrame.setOffset(0);
            currFrame.setLength(packetSize);
            currFrame.setFormat(dtmf);
            currFrame.setDuration(period);
            currFrame.setTimestamp(clock.convertToAbsoluteTime(event.getTimestamp() + 20*i));
            frameBuffer.add(currFrame);                       
        }
        
        for(int i=7;i<10;i++)
        {
        	currFrame = Memory.allocate(packetSize);
        	byte[] newData=currFrame.getData();
        	newData[0]=data[0];
        	newData[1]=(byte)(0x80 | data[1]);
        	eventDuration=(short)(160*i);
        	newData[2]=(byte)((eventDuration>>8) & 0xFF);
        	newData[3]=(byte)(eventDuration & 0xFF);
        	currFrame.setSequenceNumber(event.getSeqNumber()+i);
        	currFrame.setOffset(0);
            currFrame.setLength(packetSize);
            currFrame.setFormat(dtmf);
            currFrame.setDuration(period);
            currFrame.setTimestamp(clock.convertToAbsoluteTime(event.getTimestamp() + 20*i));
            frameBuffer.add(currFrame);                       
        }
        
        wakeup();
    }
    
    @Override
    public Frame evolve(long timestamp) 
    {
    	if (frameBuffer.size()==0)
    		return null;    	
    	
    	return frameBuffer.remove(0);    	
    }
    
    @Override
    public void reset() 
    {
    	hasEndOfEvent=false;
    	endTime=0;
    	endSeq=0;
    	latestSeq=0;
    	latestDuration=0;
		currTone=(byte)0xFF;		
    }
}
