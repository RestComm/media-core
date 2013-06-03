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

package org.mobicents.media.server.impl.dsp.audio.ilbc;

import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

import java.util.Arrays;
/**
 * 
 * @author oifa yulian
 * 
 */
public class Encoder implements Codec {
	private final static Format ilbc = FormatFactory.createAudioFormat("ilbc", 8000, 16, 1);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private short[] signal = new short[160];
    
    private EncoderState encoderState=new EncoderState();
    private EncoderBits encoderBits=new EncoderBits();    
    private CbUpdateIndexData updateIndexData=new CbUpdateIndexData();
    private CbSearchData searchData=new CbSearchData();
    
    private int n,temp,tempIndex1;
    private int memlGotten, nFor, nBack,index,subCount, subFrame, stage,en1, en2;    
    private short diff, startPos,scale, max,tempS;
	
	private short[] weightdenum=new short[66];
	private short[] dataVec=new short[250];
	private short[] memVec=new short[155];
	private short[] residual=new short[160];

    public Frame process(Frame frame) 
    {    	
    	byte[] data = frame.getData();    	    	
    	
    	temp=10;
    	for (n = 0; n < 160; n++)
    		dataVec[temp++] = ((short) ((data[n*2 + 1] << 8) | (data[n*2] & 0xFF)));
        
    	CodingFunctions.hpInput(encoderState,dataVec,10,160);    	
    	CodingFunctions.lpcEncode(encoderState,encoderBits,memVec, 4, weightdenum, 0, dataVec, 10);    	
    	System.arraycopy(encoderState.getAnaMem(), 0, dataVec, 0, 10);    	    

    	for (n=0; n<encoderState.SUBFRAMES; n++)
    	    BasicFunctions.filterMA(dataVec, 10+n*40, residual, n*40,memVec, 4 + n*11,11, 40);        	
    	
    	System.arraycopy(dataVec, 160, encoderState.getAnaMem(), 0, 10);    	
    	encoderBits.setStartIdx(CodingFunctions.frameClassify(residual));
    	index = (encoderBits.getStartIdx()-1)*40;
    	max=0;    	
    	tempIndex1=index;
    	for(n=0;n<80;n++)
    	{
    		tempS=residual[tempIndex1++];
    		if(tempS<0)
    			tempS=(short)(0-tempS);
    		
    		if(tempS>max)
    			max=tempS;    		
    	}
    	scale=BasicFunctions.getSize(max*max);

    	scale = (short)(scale - 25);
    	if(scale < 0)
    	   scale = 0;    

    	diff = (short)(80 - encoderState.STATE_SHORT_LEN);
    	
    	en1=BasicFunctions.scaleRight(residual,index,residual,index,encoderState.STATE_SHORT_LEN,scale);
    	index += diff;
    	en2=BasicFunctions.scaleRight(residual,index,residual,index,encoderState.STATE_SHORT_LEN,scale);
    	
    	if (en1 > en2) 
    	{
    		encoderBits.setStateFirst(true);
    		startPos = (short)((encoderBits.getStartIdx()-1)*40);
    	} 
    	else 
    	{
    		encoderBits.setStateFirst(false);
    		startPos = (short)((encoderBits.getStartIdx()-1)*40 + diff);
    	}

    	CodingFunctions.stateSearch(encoderState, encoderBits, residual, startPos, memVec, 4 + (encoderBits.getStartIdx()-1)*11, weightdenum, (encoderBits.getStartIdx()-1)*11);	    
	    CodingFunctions.stateConstruct(encoderBits,memVec, 4 + (encoderBits.getStartIdx()-1)*11, residual , startPos,encoderState.STATE_SHORT_LEN);
	    if (encoderBits.getStateFirst()) 
    	{ 
    		for(n=4;n<151-encoderState.STATE_SHORT_LEN;n++)
    		   memVec[n]=0;
    	   
    	   System.arraycopy(residual, startPos, memVec, 151-encoderState.STATE_SHORT_LEN, encoderState.STATE_SHORT_LEN);
    	   
    	   CodingFunctions.cbSearch(encoderState,encoderBits,searchData,updateIndexData,residual, startPos+encoderState.STATE_SHORT_LEN, memVec, 66, 85, diff, weightdenum, encoderBits.getStartIdx()*11, 0,0,0);    	   
    	   CodingFunctions.cbConstruct(encoderBits,residual,startPos+encoderState.STATE_SHORT_LEN,memVec,66,(short)85,diff,0,0);   	                  	  
    	}
    	else 
    	{ 
    	   BasicFunctions.reverseCopy(dataVec, diff+9, residual, encoderBits.getStartIdx()*40-40, diff);
    	   BasicFunctions.reverseCopy(memVec, 150, residual, startPos, encoderState.STATE_SHORT_LEN);
    	   
    	   for(n=4;n<151-encoderState.STATE_SHORT_LEN;n++)
    		   memVec[n]=0;
    	   
    	   CodingFunctions.cbSearch(encoderState,encoderBits,searchData,updateIndexData,dataVec, 10, memVec, 66, 85, diff, weightdenum, (encoderBits.getStartIdx()-1)*11, 0, 0, 0);       		    	   
    	   CodingFunctions.cbConstruct(encoderBits,dataVec,10,memVec,66, (short)85, diff, 0, 0);   	       
	    	
    	   BasicFunctions.reverseCopy(residual, startPos-1, dataVec, 10, diff);    	   
    	}

    	nFor = encoderState.SUBFRAMES-encoderBits.getStartIdx()-1;
    	subCount=1;

    	if( nFor > 0 )
    	{
    		for(n=4;n<71;n++)
     		   memVec[n]=0;
    	    
    		System.arraycopy(residual, (encoderBits.getStartIdx()-1)*40, memVec, 71, 80);  	    

    		for (subFrame = 0; subFrame < nFor; subFrame++)
    	    {    	    	
    	       CodingFunctions.cbSearch(encoderState,encoderBits,searchData,updateIndexData,residual,(encoderBits.getStartIdx()+1+subFrame)*40,memVec,4,147,40,weightdenum,(encoderBits.getStartIdx()+1+subFrame)*11, subCount, subCount*3, subCount*3);    	          	      
    	       CodingFunctions.cbConstruct(encoderBits,residual, (encoderBits.getStartIdx()+1+subFrame)*40, memVec, 4, (short)147, (short)40, subCount*3, subCount*3);
      	       
    	       temp=4;
      	       for(n=44;n<151;n++)
  	    		  memVec[temp++]=memVec[n];
  	    	
    	      System.arraycopy(residual, (encoderBits.getStartIdx()+1+subFrame)*40, memVec, 111, 40);    	      
    	      subCount++;
    	    }
    	}
    	
    	nBack = encoderBits.getStartIdx()-1;
    	if( nBack > 0 )
    	{
    		BasicFunctions.reverseCopy(dataVec, 10 + nBack*40-1, residual, 0, nBack*40);     	   
    	    memlGotten = 40*(encoderState.SUBFRAMES+1-encoderBits.getStartIdx());
    	    if( memlGotten > 147 )
    	    	memlGotten=147;    

    	    BasicFunctions.reverseCopy(memVec, 150, residual, nBack*40,memlGotten);
    	    for(n=4;n<151-memlGotten;n++)
    	    	memVec[n]=0;    	        	        	    
        	
    	    for (subFrame = 0; subFrame < nBack; subFrame++)
    	    {
    	    	CodingFunctions.cbSearch(encoderState,encoderBits,searchData,updateIndexData,dataVec, 10 + subFrame*40, memVec, 4, 147, 40, weightdenum, (encoderBits.getStartIdx()-2-subFrame)*11, subCount, subCount*3, subCount*3);
    	    	CodingFunctions.cbConstruct(encoderBits,dataVec, 10 + subFrame*40, memVec, 4, (short)147, (short)40, subCount*3, subCount*3);    	      
    	   	    temp=4;
    	    	for(n=44;n<151;n++)
    	    		memVec[temp++]=memVec[n];
    	    	
    	    	System.arraycopy(dataVec, 10 + subFrame*40, memVec, 111, 40);    	    	
    	    	subCount++;
    	    }

    	    BasicFunctions.reverseCopy(residual, 40*nBack-1, dataVec, 10, 40*nBack);    	    
    	}
    	
    	short[] index=encoderBits.getCbIndex();
    	for (n=4;n<6;n++) 
    	{
    	
    		if (index[n]>=108 && index[n]<172)
    	      index[n]-=64;
    	    else if (index[n]>=236)
    	      index[n]-=128;
    	    else
    	    {
    	    	/* ERROR */
    	    }
    	}

    	Frame res = Memory.allocate(38);
    	CodingFunctions.packBits(encoderState,encoderBits,res.getData());       	    	
    	res.setOffset(0);
        res.setLength(38);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(ilbc);
        return res;
    }

    public Format getSupportedInputFormat() {
        return linear;
    }

    public Format getSupportedOutputFormat() {
        return ilbc;
    }                
}