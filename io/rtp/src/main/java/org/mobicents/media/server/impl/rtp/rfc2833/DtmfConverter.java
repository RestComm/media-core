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

import org.mobicents.media.server.impl.rtp.JitterBuffer;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 *
 * @author Oifa Yulian
 */
public class DtmfConverter {
   
    private final static AudioFormat LINEAR_AUDIO = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static double dt = 1.0 / LINEAR_AUDIO.getSampleRate();

    
    private ArrayList<Frame> frameBuffer=new ArrayList(5);
    private Frame currFrame;
    private int toneLength=0;
    
    private final static short A = Short.MAX_VALUE / 2;
    private long time = 0;
    private byte currTone=(byte)0xFF;
    
    byte[] data = new byte[4];
    byte[] tempData = new byte[4];
    
    private RtpClock clock;    
    private JitterBuffer jitterBuffer;
    
    public final static String[] TONE = new String[] {
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#", "A", "B", "C", "D"
    };
    
    private static final Logger logger = Logger.getLogger(DtmfConverter.class);
    
    public DtmfConverter(JitterBuffer jitterBuffer)
    {
    	this.jitterBuffer=jitterBuffer;
    }
    
    public void setClock(RtpClock clock) {
        this.clock = clock;
    }
    
    public void push(RtpPacket event) {
    	//obtain payload        
        event.getPyalod(data, 0);
        
        //check that same tone
        if(data.length==0)
        	return;
        
        if(frameBuffer.size()>0)
        {
        	if(currTone!=data[0])
        	{
        		//different tone detected
        		while(frameBuffer.size()>0)
        		{
        			currFrame=frameBuffer.remove(0);
        			currFrame.recycle();
        			toneLength=0;
        		}        		
        	}
        	
        	for(int i=0;i<frameBuffer.size();i++)
        		if(frameBuffer.get(i).getSequenceNumber()==event.getSeqNumber())
        			return;        		
        }
        
        toneLength++;
        currTone=data[0];        
        currFrame = Memory.allocate(320);
    	
    	//update time
        currFrame.setSequenceNumber(event.getSeqNumber());
    	
        currFrame.setOffset(0);
        currFrame.setLength(320);
        currFrame.setFormat(LINEAR_AUDIO);
        currFrame.setDuration(20);
    	
        //not storing too much data , 100ms(5 frames) is enough for single tone
    	frameBuffer.add(currFrame);
    	
        if(frameBuffer.size()>5)
        {
        	currFrame=frameBuffer.remove(0);
			currFrame.recycle();
        }
        
        //check here if its end of event
        boolean endOfEvent=false;
        if(data.length>1)
        	endOfEvent=(data[1] & 0X80)!=0;
        
        if(!endOfEvent)
        	//not time to send event yet
        	return;
        
        if(frameBuffer.size()<3)               
        {
        	while(frameBuffer.size()>0)
    		{
    			currFrame=frameBuffer.remove(0);
    			currFrame.recycle();
    		} 
        	
        	toneLength=0;
        	return;
        }
        
        //lets send signal inband
        logger.info("Convert: " + TONE[data[0]]);
        
        int offset=0;
        time=(toneLength-frameBuffer.size())*20;
        while(frameBuffer.size()>0)
        {
        	currFrame=frameBuffer.remove(0);
        	//allocate memory for the frame
        	
        	//copy data
            System.arraycopy(DtmfTonesData.buffer[data[0]], offset, currFrame.getData(), 0, 320);
            
            //since rtp packets arrives with same timestamps , need to add small number , otherwise will be discarded by pipe
            currFrame.setTimestamp(clock.convertToAbsoluteTime(event.getTimestamp()) + time);
            offset+=320;
        	time+=20;
        	jitterBuffer.pushFrame(currFrame);        	        	
        }
        
        toneLength=0;
    }
}
