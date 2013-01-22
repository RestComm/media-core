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

package org.mobicents.media.server.impl.rtp.rfc2833;

import org.mobicents.media.server.component.oob.OOBInput;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 *
 * @author yulian oifa
 */
public class DtmfInput extends AbstractSource {
   
	private final static AudioFormat dtmf = FormatFactory.createAudioFormat("telephone-event", 8000);
	private long period = 20000000L;
    private int packetSize = 4;
    
    private ArrayList<Frame> frameBuffer=new ArrayList(5);
    private Frame currFrame;
    
    private byte currTone=(byte)0xFF;
    private long latestTime=0;
    private int latestSeq=0;
    
    private boolean hasEndOfEvent;
    private long endTime=0;
    private int endSeq=0;
    
    private short eventDuration=0;
    byte[] data = new byte[4];
    
    private RtpClock clock;    
    
    private OOBInput input;
	
    private static final Logger logger = Logger.getLogger(DtmfInput.class);
    
    public DtmfInput(Scheduler scheduler,RtpClock clock)
    {
    	super("dtmfconverter", scheduler,scheduler.INPUT_QUEUE);
    	
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
        event.getPyalod(data, 0);
        
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
        
        //lets update sync data , allowing same tone come after 160ms from previous tone , not including end of tone
        if(currTone==data[0])
        {
        	if(hasEndOfEvent)
        	{
        		if(event.getSeqNumber()<=endSeq || event.getTimestamp()<=endTime)
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
        				latestTime=event.getTimestamp();
        			}
        		
        			return;
        		}
        	
        		if(event.getTimestamp()<(latestTime+160) && event.getTimestamp()>(latestTime-160))        		
        		{
        			if(event.getTimestamp()>latestTime)
        			{
        				latestSeq=event.getSeqNumber();
        				latestTime=event.getTimestamp();
        			}
        		
        			return;
        		}
        	}
        }        
        
        hasEndOfEvent=false;
    	endTime=0;
    	endSeq=0;
        latestSeq=event.getSeqNumber();
        latestTime=event.getTimestamp();
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
    
    public void reset() 
    {
    	hasEndOfEvent=false;
    	endTime=0;
    	endSeq=0;
    	latestSeq=0;
		latestTime=0;
		currTone=(byte)0xFF;		
    }
}
