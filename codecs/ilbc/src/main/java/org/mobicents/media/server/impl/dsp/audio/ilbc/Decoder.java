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

/**
 * 
 * @author oifa yulian
 */
public class Decoder implements Codec {
	/*static 
	{
		try 
		{
			System.load("/home/azureuser/webrtcnew/depot_tools/trunk/third_party/webrtc/modules/audio_coding/codecs/ilbc/libilbc.so");
			System.out.println("Loaded library hp_input");
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
	    }
	}*/
	
	
    private final static Format ilbc = FormatFactory.createAudioFormat("ilbc", 8000, 16, 1);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private short[] decResidual=new short[240];
    private short[] plcResidual=new short[250];
    private short[] syntDenum=new short[66];
    private short[] output=new short[240];
    private short[] plcLpc=new short[11];
    private short[] signal=new short[25];
    private short[] lsfDeq=new short[20];
    private short[] weightDenum=new short[66];
    
    private int i, mode , len,temp;
    private short orderPlusOne,memlGotten, nFor, nBack, diff, startPos,subCount, subFrame,baseSize;
    
    private EncoderBits encoderBits=new EncoderBits();   
    private DecoderState decoderState=new DecoderState();    
    private CorrData corrData=new CorrData();
    
    /*public native short[] unpackBits(short[] data);
    
    public native short[] hpOutput(short[] ba,short[] x,short[] y,short[] signal,short length);
    
    public native short[] updateDecIndex(short[] index);
    
    public native short[] simpleLsfDeq(short[] lsfDeq,short[] index,short lpc_n);
    
    public native short[] interpolateLsf(short[] syntDenum,short[] weightDenum,short[] lsfDeq,short[] lsfDeqOld,short mode,short length);
    
    public native int xCorrCoef(short[] target,short targetIndex,short[] regressor,short regressorIndex,short subl,short searchLen,short offset,short step);
    
    public native short[] decodeResidual(short[] memVec,short[] reverseDecresidual,short[] idxVec,short[] cb_index,short[] gain_index,short[] decresidual,short[] syntdenum,short state_short_len,short nsub,short state_first,short startIdx,short idxForMax);
    
    public native short[] decode(short[] data,short[] lsfDeq,short[] output,short[] syntDenum);*/

    public Frame process(Frame frame) {
    	
    	byte[] inputData = frame.getData();    	    	
    	if(inputData.length==50)
    		mode=30;
    	else if(inputData.length==38)
    		mode=20;
    	else
    		throw new IllegalArgumentException("INVALID FRAME SIZE");
    	
    	decoderState.setMode(mode);
    	temp=inputData.length/2;
    	
    	for (i = 0; i < temp; i++) {
    		signal[i] = ((short) ((inputData[i*2] << 8) | (inputData[i*2 + 1] & 0xFF)));
        }
    	
    	CodingFunctions.unpackBits(encoderBits,signal,mode);
    	
    	if (encoderBits.getStartIdx()<1)
	      mode = 0;
	    if (decoderState.DECODER_MODE==20 && encoderBits.getStartIdx()>3)
	      mode = 0;
	    if (decoderState.DECODER_MODE==30 && encoderBits.getStartIdx()>5)
	      mode = 0;	    

	    if (mode>0) 
	    { 	
	    	CodingFunctions.updateDecIndex(encoderBits);	    		    		    	
	    	CodingFunctions.simpleLsfDeq(lsfDeq, 0, encoderBits.getLSF(), 0, decoderState.LPC_N);	 	    
	    	CodingFunctions.lsfCheck(lsfDeq, 0, 10);	    	
	    	CodingFunctions.decoderInterpolateLsf(decoderState,syntDenum, 0, weightDenum, 0, lsfDeq, 0, (short)10);	 	    
	    	CodingFunctions.decodeResidual(decoderState, encoderBits, decResidual, 0, syntDenum, 0);	    		 	   	
	    	CodingFunctions.doThePlc(decoderState,plcResidual, 0, plcLpc, 0, (short)0, decResidual, 0, syntDenum, 11*(decoderState.SUBFRAMES-1), (short)(decoderState.getLastTag()));
	    	System.arraycopy(plcResidual, 0, decResidual, 0, decoderState.SIZE);	    	
	   }
	   else 
       {
    	    CodingFunctions.doThePlc(decoderState,plcResidual, 0, plcLpc, 0, (short)1,decResidual, 0, syntDenum, 0, (short)(decoderState.getLastTag()));
    	    System.arraycopy(plcResidual, 0, decResidual, 0, decoderState.SIZE);
    	   
    	    orderPlusOne = 11;
    	    for (i = 0; i < decoderState.SUBFRAMES; i++)
    	    	System.arraycopy(plcLpc, 0, syntDenum, i*orderPlusOne, orderPlusOne);    	          	   
       }

       if (mode==20)
           decoderState.setLastTag(CodingFunctions.xCorrCoef(decResidual, decoderState.SIZE-60, decResidual, decoderState.SIZE-80, (short)60, (short)80, (short)20, (short)-1));  		 	
       else
  	    	decoderState.setLastTag(CodingFunctions.xCorrCoef(decResidual, decoderState.SIZE-80, decResidual, decoderState.SIZE-100, (short)80, (short)100, (short)20, (short)-1));  		
  	   
  	   System.arraycopy(decResidual, 0, plcResidual, 10, decoderState.SIZE);
  	   System.arraycopy(decoderState.getSynthMem(), 0, plcResidual, 0, 10);
  	   
  	   for (i=0; i < decoderState.SUBFRAMES; i++)
  	     BasicFunctions.filterAR(plcResidual,10+40*i,plcResidual,10+40*i,syntDenum,11*i,11,40);  	        	   

  	   System.arraycopy(plcResidual, decoderState.SIZE, decoderState.getSynthMem(), 0, 10);
	   System.arraycopy(plcResidual, 10, output, 0, decoderState.SIZE);
	   CodingFunctions.hpOutput(output,Constants.HP_OUT_COEFICIENTS,decoderState.getHpiMemY(),decoderState.getHpiMemX(),decoderState.SIZE);
	   System.arraycopy(syntDenum, 0, decoderState.getOldSyntDenum(), 0, decoderState.SUBFRAMES*11);              
       
       if (mode==0)
    	   decoderState.setPrevEnchPl(1);
       else
    	   decoderState.setPrevEnchPl(0);
       
       Frame res;
       if(decoderState.DECODER_MODE==20)
       {
    	   res = Memory.allocate(320);
    	   inputData=res.getData();
    	   
    	   for (i = 0; i < 160; i++) 
           {
        	   inputData[i*2 + 1] =  (byte)((output[i]>>8)&0xFF);
        	   inputData[i*2] = (byte)(output[i] & 0xFF);    		   
           }
       }
       else
       {
    	   res = Memory.allocate(480);
    	   inputData=res.getData();
    	   
    	   for (i = 0; i < 240; i++) 
           {
        	   inputData[i*2 + 1] =  (byte)((output[i]>>8)&0xFF);
        	   inputData[i*2] = (byte)(output[i] & 0xFF);    		   
           }
       }                     
            
       res.setOffset(0);
       res.setLength(res.getData().length);
       res.setTimestamp(frame.getTimestamp());
       res.setDuration(frame.getDuration());
       res.setSequenceNumber(frame.getSequenceNumber());
       res.setEOM(frame.isEOM());
       res.setFormat(linear);
       return res;
    }
    
    public Format getSupportedInputFormat() {
        return ilbc;
    }

    public Format getSupportedOutputFormat() {
        return linear;
    }               
}