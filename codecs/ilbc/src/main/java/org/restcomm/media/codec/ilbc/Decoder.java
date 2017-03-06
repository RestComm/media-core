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

package org.restcomm.media.codec.ilbc;

import org.restcomm.media.spi.dsp.Codec;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 * 
 * @author oifa yulian
 */
public class Decoder implements Codec {

    private final static Format ilbc = FormatFactory.createAudioFormat("ilbc", 8000, 16, 1);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private short[] decResidual=new short[240];
    private short[] plcResidual=new short[250];
    private short[] syntDenum=new short[66];
    private short[] output=new short[240];
    private short[] plcLpc=new short[11];
    private short[] signal=new short[25];
    private int i, j, k, n, s, mode , pos, cbPos, len, tempShift,crossCorr, energy,shifts,newCrit, maxCrit;
    private int measure, maxMeasure, ind;
	private int tempIndex1,tempIndex2,tempIndex3,tempIndex4,tempIndex5,tempIndex6,tempIndex7;
    private int temp,temp2;
    
    //max used 12
    private int[] tempLMemory=new int[100];
    
    private short tempS,tempS2,xHi,xLow,orderPlusOne,max,lag;
    private short memlGotten, nFor, nBack, diff, startPos;
    private short subCount, subFrame,baseSize;
    private short maxLag,crossCorrScale, energyScale,crossCorrSqMod, crossCorrSqModMax,crossCorrMod, energyMod, energyModMax,totScale, totScaleMax,scaleDiff;
    private short pick,crossSquareMax, crossSquare;
    private short shift1, shift2, shift3, shiftMax , scale1, scale2 , scale3 ,nom;
    private short corrLen,useGain,totGain,maxPerSquare,denom,pitchFact,useLag,randLag;
    //encoder bits
    private EncoderBits encoderBits=new EncoderBits();
    
    //decoder state
    private DecoderState decoderState=new DecoderState();
    
    //correlation data
    private CorrData corrData=new CorrData(),tempCorrData=new CorrData();
    
    //max used 327
    private short[] tempMemory=new short[350];
    
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
    	
    	unpackBits(signal,mode);
    	    	
    	/* Check for bit errors */
	    if (encoderBits.getStartIdx()<1)
	      mode = 0;
	    if (decoderState.DECODER_MODE==20 && encoderBits.getStartIdx()>3)
	      mode = 0;
	    if (decoderState.DECODER_MODE==30 && encoderBits.getStartIdx()>5)
	      mode = 0;	    

	    if (mode>0) 
	    { 	/* No bit errors was detected, continue decoding */
	    	/* Stack based */
	    	short[] lsfDeq=tempMemory; //+0 length 20 
	    	short[] weightDenum=tempMemory; //+20 length 66
	      
	    	/* adjust index */
	    	updateDecIndex();

	    	/* decode the lsf */
	    	simpleLsfDeq(lsfDeq, 0, encoderBits.getLSF(), 0);	    	
	    	
	    	lsfCheck(lsfDeq, 0, 10);
	    	
	    	decoderInterpolateLsp(syntDenum, 0, weightDenum, 20, lsfDeq, 0, (short)10);

	    	/* Decode the residual using the cb and gain indexes */
	    	decodeResidual(decResidual, 0, syntDenum, 0);	    	

	    	/* preparing the plc for a future loss! */
	    	doThePlc(plcResidual, 0, plcLpc, 0, (short)0, decResidual, 0, syntDenum, 11*(decoderState.SUBFRAMES-1), (short)(decoderState.getLastTag()));

	    	/* Use the output from doThePLC */
	    	System.arraycopy(plcResidual, 0, decResidual, 0, decoderState.SIZE);	    	
	   }

       if (mode == 0) 
       {
    	    doThePlc(plcResidual, 0, plcLpc, 0, (short)1,decResidual, 0, syntDenum, 0, (short)(decoderState.getLastTag()));

    	    System.arraycopy(plcResidual, 0, decResidual, 0, decoderState.SIZE);
    	    
    	    orderPlusOne = 11;

    	    for (i = 0; i < decoderState.SUBFRAMES; i++)
    	    	System.arraycopy(plcLpc, 0, syntDenum, i*orderPlusOne, orderPlusOne);    	          	   
       }

       /* Find last lag (since the enhancer is not called to give this info) */
  	   lag = 20;
  	   if (mode==20)
  		   lag = (short)(xCorrCoef(decResidual, decoderState.SIZE-60, decResidual, decoderState.SIZE-60-lag, (short)60, (short)80, lag, (short)-1));
  	   else 
  		   lag = (short)(xCorrCoef(decResidual, decoderState.SIZE-80, decResidual, decoderState.SIZE-80-lag, (short)80, (short)100, lag, (short)-1));  		   
  	   
  	   /* Store lag (it is needed if next packet is lost) */
  	   decoderState.setLastTag((int)lag);

  	   /* copy data and run synthesis filter */
  	   System.arraycopy(decResidual, 0, plcResidual, 10, decoderState.SIZE);
  	   
  	   /* Set up the filter state */
  	   System.arraycopy(decoderState.getSynthMem(), 0, plcResidual, 0, 10);
  	   
  	   for (i=0; i < decoderState.SUBFRAMES; i++)
  		   BasicFunctions.filterAR(plcResidual,10+40*i,plcResidual,10+40*i,syntDenum,11*i,11,40);  	        	   

  	   /* Save the filter state */
  	   System.arraycopy(plcResidual, decoderState.SIZE, decoderState.getSynthMem(), 0, 10);
	   
	   System.arraycopy(plcResidual, 10, output, 0, decoderState.SIZE);
	   
	   /* High pass filter the signal (with upscaling a factor 2 and saturation) */
	   hpOutput(output,Constants.HP_OUT_COEFICIENTS,decoderState.getHpiMemY(),decoderState.getHpiMemX(),decoderState.SIZE);
       
       System.arraycopy(syntDenum, 0, decoderState.getOldSyntDenum(), 0, decoderState.SUBFRAMES*11);
       
       decoderState.setPrevEnchPl(0);
       
       if (mode==0) { /* PLC was used */
    	   decoderState.setPrevEnchPl(1);
       }
       
       Frame res;
       if(decoderState.DECODER_MODE==20)
       {
    	   res = Memory.allocate(320);
    	   inputData=res.getData();
    	   
    	   for (i = 0; i < 160; i++) 
           {
        	   inputData[i*2] =  (byte)((output[i]>>8)&0xFF);
        	   inputData[i*2 + 1] = (byte)(output[i] & 0xFF);    		   
           }
       }
       else
       {
    	   res = Memory.allocate(480);
    	   inputData=res.getData();
    	   
    	   for (i = 0; i < 240; i++) 
           {
        	   inputData[i*2] =  (byte)((output[i]>>8)&0xFF);
        	   inputData[i*2 + 1] = (byte)(output[i] & 0xFF);    		   
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
    
    private void doThePlc(short[] plcResidual,int plcResidualIndex,short[] plcLpc,int plcLpcIndex,short pli,short[] decResidual,int decResidualIndex,short[] lpc,int lpcIndex,short inLag)
    {    	      	     	 
    	short[] randVec=tempMemory;//+86 length 240
    	tempCorrData.setEnergy(0);
    	
    	/* Packet Loss */
    	if (pli == 1) 
    	{
    	    decoderState.setConsPliCount(decoderState.getConsPliCount()+1);

    	    /* if previous frame not lost,
    	       determine pitch pred. gain */

    	    if (decoderState.getPrevPli() != 1) 
    	    {

    	      /* Maximum 60 samples are correlated, preserve as high accuracy
    	         as possible without getting overflow */
    	      max=BasicFunctions.getMaxAbsValue(decoderState.getPrevResidual(),0,decoderState.SIZE);
    	      scale3 = (short)((BasicFunctions.getSize(max)<<1) - 25);
    	      if (scale3 < 0)
    	        scale3 = 0;    	      

    	      /* Store scale for use when interpolating between the
    	       * concealment and the received packet */
    	      decoderState.setPrevScale(scale3);

    	      /* Search around the previous lag +/-3 to find the
    	         best pitch period */
    	      lag = (short)(inLag - 3);

    	      /* Guard against getting outside the frame */
    	      if(60 > decoderState.SIZE-inLag-3)
    	    	  corrLen=60;
    	      else
    	    	  corrLen=(short)(decoderState.SIZE-inLag-3);
    	      
    	      compCorr(corrData, decoderState.getPrevResidual(), 0, lag, decoderState.SIZE, corrLen, scale3);

    	      /* Normalize and store cross^2 and the number of shifts */
    	      shiftMax = (short)(BasicFunctions.getSize(Math.abs(corrData.getCorrelation()))-15);
    	      if(shiftMax>0)
    	      {
    	    	  tempShift=corrData.getCorrelation()>>shiftMax;
    	    	  tempShift=tempShift*tempShift;
    	    	  crossSquareMax=(short)(tempShift>>15);    	    	  
    	      }
    	      else
    	      {
    	    	  tempShift=corrData.getCorrelation()<<(0-shiftMax);
    	    	  tempShift=tempShift*tempShift;
    	    	  crossSquareMax=(short)(tempShift>>15);  
    	      }

    	      for (j=inLag-2;j<=inLag+3;j++) 
    	      {
    	    	  compCorr(tempCorrData, decoderState.getPrevResidual(), 0, (short)j, decoderState.SIZE, corrLen, scale3);

    	    	  /* Use the criteria (corr*corr)/energy to compare if
    	          this lag is better or not. To avoid the division,
    	          do a cross multiplication */
    	    	  shift1 = (short)(BasicFunctions.getSize(Math.abs(tempCorrData.getCorrelation())-15));
    	    	  if(shift1>0)
        	      {
        	    	  tempShift=tempCorrData.getCorrelation()>>shift1;
        	    	  tempShift=tempShift*tempShift;
        	    	  crossSquare=(short)(tempShift>>15);    	    	  
        	      }
        	      else
        	      {
        	    	  tempShift=tempCorrData.getCorrelation()<<(0-shift1);
        	    	  tempShift=tempShift*tempShift;
        	    	  crossSquare=(short)(tempShift>>15);  
        	      }    	    	  

    	    	  shift2 = (short)(BasicFunctions.getSize(corrData.getEnergy())-15);
    	    	  if(shift2>0)
    	    		  measure=(corrData.getEnergy()>>shift2)*crossSquare;
    	    	  else
    	    		  measure=(corrData.getEnergy()<<(0-shift2))*crossSquare;

    	    	  shift3 = (short)(BasicFunctions.getSize(tempCorrData.getEnergy())-15);
    	    	  if(shift3>0)
    	    		  maxMeasure=(tempCorrData.getEnergy()>>shift3)*crossSquareMax;
    	    	  else
    	    		  maxMeasure=(tempCorrData.getEnergy()<<(0-shift3))*crossSquareMax;
    	    	  
    	    	  /* Calculate shift value, so that the two measures can
    	          be put in the same Q domain */
    	    	  if(((shiftMax<<1)+shift3) > ((shift1<<1)+shift2)) 
    	    	  {
    	    		  tempShift=(shiftMax<<1);
    	    		  tempShift-=(shift1<<1);
    	    		  tempShift=tempShift+shift3-shift2;
    	    		  if(tempShift>31)
    	    			  tempS = 31;
    	    		  else
    	    			  tempS= (short)tempShift;
    	    		  
    	    		  tempS2 = 0;
    	    	  } 
    	    	  else 
    	    	  {
    	    		  tempS = 0;
    	    		  tempShift=(shift1<<1);
    	    		  tempShift-=(shiftMax<<1);
    	    		  tempShift=tempShift+shift2-shift3;
    	    		  if(tempShift>31)
    	    			  tempS2 = 31;
    	    		  else
    	    			  tempS2=(short)tempShift;
    	    	  }

    	    	  if ((measure>>tempS) > (maxMeasure>>tempS2)) 
    	    	  {
    	    		  /* New lag is better => record lag, measure and domain */
    	    		  lag = (short)j;
    	    		  crossSquareMax = crossSquare;
    	    		  corrData.setCorrelation(tempCorrData.getCorrelation());
    	    		  shiftMax = shift1;
    	    		  corrData.setEnergy(tempCorrData.getEnergy());
    	    	  }
    	      }

    	      /* Calculate the periodicity for the lag with the maximum correlation.
    	       * 
    	         Definition of the periodicity:
    	         abs(corr(vec1, vec2))/(sqrt(energy(vec1))*sqrt(energy(vec2)))

    	         Work in the Square domain to simplify the calculations
    	         max_perSquare is less than 1 (in Q15)
    	      */
    	      BasicFunctions.scaleRight(decoderState.getPrevResidual(),decoderState.SIZE-corrLen,decoderState.getPrevResidual(),decoderState.SIZE-corrLen,corrLen, scale3);
    	      
    	      if ((temp2>0)&&(tempCorrData.getEnergy()>0)) 
    	      {
    	    	  /* norm energies to WebRtc_Word16, compute the product of the energies and
    	          use the upper WebRtc_Word16 as the denominator */

    	    	  scale1=(short)(BasicFunctions.norm(temp2)-16);
    	    	  if(scale1>0)
    	    		  tempS=(short)(temp2<<scale1);
    	    	  else
    	    		  tempS=(short)(temp2>>(0-scale1));
    	    	  
    	    	  scale2=(short)(BasicFunctions.norm(corrData.getEnergy())-16);
    	    	  if(scale2>0)
    	    		  tempS2=(short)(corrData.getEnergy()<<scale2);
    	    	  else
    	    		  tempS2=(short)(corrData.getEnergy()>>(0-scale2));
    	    	      	    	  
    	    	  denom=(short)((tempS*tempS2)>>16); /* denom in Q(scale1+scale2-16) */

    	    	  /* Square the cross correlation and norm it such that max_perSquare
    	           will be in Q15 after the division */

    	    	  totScale = (short)(scale1+scale2-1);
    	    	  tempShift=(totScale>>1);
    	    	  if(tempShift>0)
    	    		  tempS = (short)(corrData.getCorrelation()<<tempShift);
    	    	  else
    	    		  tempS = (short)(corrData.getCorrelation()>>(0-tempShift));
    	    	  
    	    	  tempShift=totScale-tempShift;
    	    	  if(tempShift>0)
    	    		  tempS2 = (short)(corrData.getCorrelation()<<tempShift);
    	    	  else
    	    		  tempS2 = (short)(corrData.getCorrelation()>>(0-tempShift));
    	    	  
    	    	  nom = (short)(tempS*tempS2);
    	    	  maxPerSquare = (short)(nom/denom);

    	      } 
    	      else 
    	      {
    	        maxPerSquare = 0;
    	      }
    	  }    	  
    	  else 
    	  {
    		  /* previous frame lost, use recorded lag and gain */
    	      lag = decoderState.getPrevLag();
    	      maxPerSquare = decoderState.getPerSquare();
    	  }

    	  /* Attenuate signal and scale down pitch pred gain if
    	     several frames lost consecutively */

    	  useGain = 32767;   /* 1.0 in Q15 */
    	  
    	  if (decoderState.getConsPliCount()*decoderState.SIZE>320) {
    		  useGain = 29491;  /* 0.9 in Q15 */
    	  } else if (decoderState.getConsPliCount()*decoderState.SIZE>640) {
    		  useGain = 22938;  /* 0.7 in Q15 */
    	  } else if (decoderState.getConsPliCount()*decoderState.SIZE>960) {
    		  useGain = 16384;  /* 0.5 in Q15 */
    	  } else if (decoderState.getConsPliCount()*decoderState.SIZE>1280) {
    		  useGain = 0;   /* 0.0 in Q15 */
    	  }

    	  /* Compute mixing factor of picth repeatition and noise:
    	     for max_per>0.7 set periodicity to 1.0
    	     0.4<max_per<0.7 set periodicity to (maxper-0.4)/0.7-0.4)
    	     max_per<0.4 set periodicity to 0.0
    	  */

    	  if (maxPerSquare>7868) 
    	  { 
    		  /* periodicity > 0.7  (0.7^4=0.2401 in Q15) */
    	      pitchFact = 32767;
    	  } 
    	  else if (maxPerSquare>839) 
    	  { 
    		  /* 0.4 < periodicity < 0.7 (0.4^4=0.0256 in Q15) */
    	      /* find best index and interpolate from that */
    	      ind = 5;
    	      while ((maxPerSquare<Constants.PLC_PER_SQR[ind]) && (ind>0))
    	        ind--;
    	      
    	      /* pitch fact is approximated by first order */
    	      temp = Constants.PLC_PITCH_FACT[ind];
    	      temp += ((Constants.PLC_PF_SLOPE[ind]*(maxPerSquare-Constants.PLC_PER_SQR[ind])) >> 11);

    	      if(temp>Short.MIN_VALUE)
    	    	  pitchFact=Short.MIN_VALUE;
    	      else
    	    	  pitchFact=(short)temp;    	      
    	  } 
    	  else 
    	  { 
    		  /* periodicity < 0.4 */
    	      pitchFact = 0;
    	  }

    	  /* avoid repetition of same pitch cycle (buzzyness) */
    	  useLag = lag;
    	  if (lag<80) 
    	      useLag = (short)(2*lag);
    	  
    	  /* compute concealed residual */
    	  energy = 0;

    	  for (i=0; i<decoderState.SIZE; i++) 
    	  {
    	      /* noise component -  52 < randlagFIX < 117 */
    	      decoderState.setSeed((short)((decoderState.getSeed()*31821) + 13849));
    	      randLag = (short)(53 + (decoderState.getSeed() & 63));

    	      pick = (short)(i - randLag);

    	      if (pick < 0)
    	    	  randVec[86+i] = decoderState.getPrevResidual()[decoderState.SIZE+pick];
    	      else
    	    	  randVec[86+i] = decoderState.getPrevResidual()[pick];    	      

    	      /* pitch repeatition component */
    	      pick = (short)(i - useLag);

    	      if (pick < 0)
    	        plcResidual[plcResidualIndex + i] = decoderState.getPrevResidual()[decoderState.SIZE+pick];
    	      else
    	    	  plcResidual[plcResidualIndex + i] = plcResidual[plcResidualIndex + pick];    	      

    	      /* Attinuate total gain for each 10 ms */
    	      if (i<80)
    	        totGain=useGain;
    	      else if (i<160)
    	        totGain=(short)((31130 * useGain) >> 15); /* 0.95*use_gain */
    	      else
    	        totGain=(short)((29491 * useGain) >> 15); /* 0.9*use_gain */    	      


    	      /* mix noise and pitch repeatition */

    	      tempShift=pitchFact * plcResidual[plcResidualIndex + i];
    	      tempShift+=(32767-pitchFact)*randVec[86+i];
    	      tempShift+=16384;
    	      temp=(short)(tempShift>>15);
    	      plcResidual[plcResidualIndex + i] = (short)((totGain*temp)>>15);

    	      /* Shifting down the result one step extra to ensure that no overflow
    	         will occur */
    	      tempShift=plcResidual[plcResidualIndex + i] * plcResidual[plcResidualIndex + i];
    	      energy += (short)(tempShift>>(decoderState.getPrevScale()+1));
    	  }

    	  /* less than 30 dB, use only noise */
    	  tempShift=decoderState.SIZE*900;
    	  if(decoderState.getPrevScale()+1>0)
    		  tempShift=tempShift>>(decoderState.getPrevScale()+1);
    	  else
    		  tempShift=tempShift<<(0-decoderState.getPrevScale()-1);
    	  
    	  if (energy < tempShift) 
    	  {
    	      energy = 0;
    	      for (i=0; i<decoderState.SIZE; i++)
    	        plcResidual[plcResidualIndex + i] = randVec[86 + i];    	      
    	  }

    	  /* use the old LPC */
    	  System.arraycopy(decoderState.getPrevLpc(), 0, plcLpc, plcLpcIndex, 10);    	  

    	  /* Update state in case there are multiple frame losses */
    	  decoderState.setPrevLag(lag);
    	  decoderState.setPerSquare(maxPerSquare);    	  
       }
       else 
       {
    	  /* no packet loss, copy input */
     	  System.arraycopy(decResidual, decResidualIndex, plcResidual, plcResidualIndex, decoderState.SIZE);    	  
     	  System.arraycopy(lpc, lpcIndex, plcLpc, plcLpcIndex, 11);    	  
    	  decoderState.setConsPliCount(0);    	    
       }

       /* update state */
       decoderState.setPrevPli(pli);
       System.arraycopy(plcLpc, plcLpcIndex, decoderState.getPrevLpc(), 0, 11);    	  
       System.arraycopy(plcResidual, plcResidualIndex, decoderState.getPrevResidual(), 0, decoderState.SIZE); 	       
    }
    
    private void compCorr(CorrData currData,short[] buffer,int bufferIndex,short lag,short bLen,short sRange,short scale)
    {
    	tempIndex7=bLen-sRange-lag;
    	
    	/* Calculate correlation and energy */
    	if(scale>0)
    	{
    		currData.setCorrelation(BasicFunctions.scaleRight(buffer, bufferIndex + bLen - sRange, buffer, tempIndex7, sRange, scale));
    		currData.setEnergy(BasicFunctions.scaleRight(buffer, tempIndex7, buffer, tempIndex7, sRange, scale));    		
    	}
    	else
    	{
    		currData.setCorrelation(BasicFunctions.scaleLeft(buffer, bufferIndex + bLen - sRange, buffer, tempIndex7, sRange, (0-scale)));
    		currData.setEnergy(BasicFunctions.scaleLeft(buffer, tempIndex7, buffer, tempIndex7, sRange, scale));
    	}
    	
    	/* For zero energy set the energy to 0 in order to avoid potential
    	   problems for coming divisions */
    	if (currData.getCorrelation() == 0) 
    	{
    		currData.setCorrelation(0);
    		currData.setEnergy(1);    	    
    	}
    }
    
    private void hpOutput(short[] signal,short[] ba,short[] y,short[] x,short len)
    {
    	for (i=0; i<len; i++) 
    	{
    	    /*
    	      y[i] = b[0]*x[i] + b[1]*x[i-1] + b[2]*x[i-2]
    	      + (-a[1])*y[i-1] + (-a[2])*y[i-2];
    	    */
    	    temp  = y[1]*ba[3];     /* (-a[1])*y[i-1] (low part) */
    	    temp += y[3]*ba[4];     /* (-a[2])*y[i-2] (low part) */
    	    temp = temp>>15;
    	    temp += y[0]*ba[3];     /* (-a[1])*y[i-1] (high part) */
    	    temp += y[2]*ba[4];     /* (-a[2])*y[i-2] (high part) */
    	    temp = temp<<1;

    	    temp += signal[i]*ba[0];   /* b[0]*x[0] */
    	    temp += x[0]*ba[1];   /* b[1]*x[i-1] */
    	    temp += x[1]*ba[2];   /* b[2]*x[i-2] */

    	    /* Update state (input part) */
    	    x[1] = x[0];
    	    x[0] = signal[i];

    	    /* Rounding in Q(12-1), i.e. add 2^10 */
    	    temp2 = temp + 1024;

    	    /* Saturate (to 2^26) so that the HP filtered signal does not overflow */
    	    if(temp2>67108863)
    	    	temp2=67108863;
    	    else if(temp2<-67108864)
    	    	temp2=-67108864;
    	    
    	    /* Convert back to Q0 and multiply with 2 */
    	    signal[i] = (short)(temp2>>11);

    	    /* Update state (filtered part) */
    	    y[2] = y[0];
    	    y[3] = y[1];

    	    /* upshift tmpW32 by 3 with saturation */
    	    if (temp>268435455)
    	      temp = Integer.MAX_VALUE;
    	    else if (temp<-268435456)
    	      temp = Integer.MIN_VALUE;
    	    else
    	      temp = temp<<3;    	    

    	    y[0] = (short)(temp>>16);
    	    tempShift = y[0]<<16;
    	    y[1] = (short)((temp - tempShift)>>1);
   	    }
    }
    
    private int xCorrCoef(short[] target,int targetIndex,short[] regressor,int regressorIndex,short subl,short searchLen,short offset,short step)
    {    	  
    	/* Initializations, to make sure that the first one is selected */
    	crossCorrSqModMax=0;
    	energyModMax=Short.MAX_VALUE;
    	totScaleMax=-500;
    	maxLag=0;
    	pos=0;

    	/* Find scale value and start position */
    	if (step==1) 
    	{
    		max=BasicFunctions.getMaxAbsValue(regressor,regressorIndex,subl+searchLen-1);
    	    tempIndex1=regressorIndex;
    	    tempIndex2=regressorIndex + subl;    	    
    	}
    	else 
    	{ 
    		/* step==-1 */
    		max=BasicFunctions.getMaxAbsValue(regressor,regressorIndex-searchLen,subl+searchLen-1);
    	    tempIndex1=regressorIndex-1;
    		tempIndex2=regressorIndex + subl-1;    	    
    	}

    	/* Introduce a scale factor on the Energy in WebRtc_Word32 in
    	order to make sure that the calculation does not
    	overflow */

    	if (max>5000)
    		shifts=2;
    	else
    		shifts=0;    	  

    	/* Calculate the first energy, then do a +/- to get the other energies */
    	energy=BasicFunctions.scaleRight(regressor, regressorIndex, regressor, regressorIndex, subl, shifts);
    	
    	for (k=0;k<searchLen;k++) 
    	{
    		tempIndex3=targetIndex;
    		tempIndex4=regressorIndex + pos;
    	    
    		crossCorr=BasicFunctions.scaleRight(target, tempIndex3, regressor, tempIndex4, subl, shifts);    			  

    		if ((energy>0)&&(crossCorr>0)) 
    		{
    			/* Put cross correlation and energy on 16 bit word */
    			crossCorrScale=(short)(BasicFunctions.norm(crossCorr)-16);
    			
    			if(crossCorrScale>0)
    				crossCorrMod=(short)(crossCorr<<crossCorrScale);
    			else
    				crossCorrMod=(short)(crossCorr>>(0-crossCorrScale));
    			
    			energyScale=(short)(BasicFunctions.norm(energy)-16);
    			
    			if(energyScale>0)
    				energyMod=(short)(energy<<energyScale);
    			else
    				energyMod=(short)(energy>>(0-energyScale));
    			
    			/* Square cross correlation and store upper WebRtc_Word16 */
    			crossCorrSqMod=(short)((crossCorrMod * crossCorrMod) >> 16);

    			/* Calculate the total number of (dynamic) right shifts that have
    	        been performed on (crossCorr*crossCorr)/energy
    			*/
    			totScale=(short)(energyScale-(crossCorrScale<<1));

    			/* Calculate the shift difference in order to be able to compare the two
    	        (crossCorr*crossCorr)/energy in the same domain
    			*/
    			scaleDiff=(short)(totScale-totScaleMax);
    			if(scaleDiff>31)
    				scaleDiff=31;
    			else if(scaleDiff<-31)
    				scaleDiff=-31;

    			/* Compute the cross multiplication between the old best criteria
    	        and the new one to be able to compare them without using a
    	        division */

    			if (scaleDiff<0) 
    			{
    				newCrit = ((crossCorrSqMod*energyModMax)>>(-scaleDiff));
    			  	maxCrit = crossCorrSqModMax*energyMod;
    			} 
    			else 
    			{
    				newCrit = crossCorrSqMod*energyModMax;
    				maxCrit = ((crossCorrSqModMax*energyMod)>>scaleDiff);
    			}

    			/* Store the new lag value if the new criteria is larger
    	        than previous largest criteria */

    			if (newCrit > maxCrit) 
    			{
    				crossCorrSqModMax = crossCorrSqMod;
    				energyModMax = energyMod;
    				totScaleMax = totScale;
    				maxLag = (short)k;
    			}
    		}
    		  
    		pos+=step;

    		/* Do a +/- to get the next energy */
    		temp=regressor[tempIndex2]*regressor[tempIndex2] - regressor[tempIndex1]*regressor[tempIndex1];
    		temp>>=shifts;
    		energy += step*temp;
    		tempIndex1+=step;
    		tempIndex2+=step;
    	}

    	return(maxLag+offset);
    }
    
    private void decodeResidual(short[] decResidual,int decResidualIndex,short[] syntDenum,int syntDenumIndex) 
    {
    	short[] reverseDecresidual = decoderState.getEnhancementBuffer();
    	short[] memVec = decoderState.getPrevResidual();

    	diff = (short)(80 - decoderState.STATE_SHORT_LEN);

    	if (encoderBits.getStateFirst())
    		startPos = (short)((encoderBits.getStartIdx()-1)*40);
    	else
    		startPos = (short)((encoderBits.getStartIdx()-1)*40 + diff);    	  

    	/* decode scalar part of start state */
    	stateConstruct(syntDenum,syntDenumIndex + (encoderBits.getStartIdx()-1)*11,decResidual,decResidualIndex + startPos);    	

    	if(encoderBits.getStateFirst()) 
    	{ 
    		/* put adaptive part in the end */
    	    /* setup memory */
    		for(i=4;i<151-decoderState.STATE_SHORT_LEN;i++)
    			memVec[i]=0;
    		
    		System.arraycopy(decResidual, startPos, memVec, 151-decoderState.STATE_SHORT_LEN, decoderState.STATE_SHORT_LEN);

    	    /* construct decoded vector */
    	    cbConstruct(decResidual,decResidualIndex + startPos + decoderState.STATE_SHORT_LEN, memVec,66, (short)85, diff, 0, 0);
    	}
    	else 
    	{
    		/* put adaptive part in the beginning */
    	    /* create reversed vectors for prediction */
    		BasicFunctions.reverseCopy(reverseDecresidual, diff, decResidual, decResidualIndex + (encoderBits.getStartIdx()+1)*40-80,diff);
    	    
    	    /* setup memory */
    	    memlGotten = decoderState.STATE_SHORT_LEN;
    	    BasicFunctions.reverseCopy(memVec, 150, decResidual, decResidualIndex + startPos, memlGotten);
    	    
    	    for(i=4;i<151-memlGotten;i++)
    			memVec[i]=0;    		

    	    /* construct decoded vector */
    	    cbConstruct(reverseDecresidual, 0, memVec, 66, (short)85, diff, 0, 0);
    	    
    	    /* get decoded residual from reversed vector */
    	    BasicFunctions.reverseCopy(decResidual, decResidualIndex + startPos-1, reverseDecresidual, 0, diff);    	    
    	}

    	/* counter for predicted subframes */
    	subCount=1;

    	/* forward prediction of subframes */
    	nFor = (short)(decoderState.SUBFRAMES - encoderBits.getStartIdx() -1);
    	  
    	if(nFor > 0) 
    	{
    	    /* setup memory */
    		for(i=4;i<71;i++)
    			memVec[i]=0;
    		
    	    System.arraycopy(decResidual, decResidualIndex + 40 * (encoderBits.getStartIdx()-1), memVec, 71, 80);    	    	
    	    
    	    /* loop over subframes to encode */
    	    for (subFrame=0; subFrame<nFor; subFrame++) 
    	    {
    	    	/* construct decoded vector */
    	    	cbConstruct(decResidual, decResidualIndex + 40*(encoderBits.getStartIdx()+1+subFrame), memVec, 4, (short)147, (short)40, subCount*3, subCount*3);
        	    
    	    	/* update memory */
    	    	for(i=4;i<111;i++)
    	    		memVec[i]=memVec[i+40];
    	    	
    	    	System.arraycopy(reverseDecresidual, 40 * (encoderBits.getStartIdx()+1+subFrame) , memVec, 111, 40);    	    	

    	    	subCount++;
    	    }
    	}

    	/* backward prediction of subframes */
    	nBack = (short)(encoderBits.getStartIdx()-1);

    	if(nBack > 0)
    	{
    	    /* setup memory */
    	    memlGotten = (short)(40*(decoderState.SUBFRAMES + 1 - encoderBits.getStartIdx()));
    	    if(memlGotten > 147)
    	    	memlGotten=147;    	    

    	    BasicFunctions.reverseCopy(memVec, 150, decResidual, decResidualIndex + 40 * (encoderBits.getStartIdx()-1), memlGotten);
    	    
    	    for(i=4;i<151-memlGotten;i++)
    	    	memVec[i]=0;
    	    
    	    /* loop over subframes to decode */
    	    for (subFrame=0; subFrame<nBack; subFrame++) 
    	    {
    	    	/* construct decoded vector */
    	    	cbConstruct(reverseDecresidual, 40*subFrame, memVec, 4, (short)147, (short)40, subCount*3, subCount*3);
        	    
    	    	/* update memory */
    	    	for(i=4;i<111;i++)
    	    		memVec[i]=memVec[i+40];
    	    	
    	    	System.arraycopy(reverseDecresidual, 40 * subFrame, memVec, 111, 40);
    	    	
    	    	subCount++;
    	    }

    	    /* get decoded residual from reversed vector */
    	    BasicFunctions.reverseCopy(decResidual, decResidualIndex+40*nBack-1, reverseDecresidual, 0, 40*nBack);    	    
    	}    	    	        	
    }
    
    private void lsf2Poly(short[] a,int aIndex,short[] lsf,int lsfIndex)
    {
    	/* f[0][] and f[1][] corresponds to
        F1(z) and F2(z) respectivly */
    	int[] f=tempLMemory; //+ 0 length 12    	
    	tempIndex3=0;
    	tempIndex4=6;
    	
    	/* Stack based */
    	short[] lsp=tempMemory; //+ 61 length 10    	    
    	
    	/* Convert lsf to lsp */
    	lsf2Lsp(lsf, lsfIndex, lsp, 61, 10);    	    	           	
    	
    	/* Get F1(z) and F2(z) from the lsp */    	
    	getLspPoly(lsp, 61, f, 0);
    	getLspPoly(lsp, 62, f, 6);

    	/* for i = 5 down to 1
		Compute f1[i] += f1[i-1];
		and     f2[i] += f2[i-1];
    	*/
    	tempIndex3=5;
    	tempIndex4=11;
    	for (k=5; k>0; k--)
    	{
    		f[tempIndex3]+=f[tempIndex3-1];
    		f[tempIndex4]-=f[tempIndex4-1];
    		tempIndex3--;
    		tempIndex4--;
    		
    	}

    	/* Get the A(z) coefficients
		a[0] = 1.0
		for i = 1 to 5
			a[i] = (f1[i] + f2[i] + round)>>13;
		for i = 1 to 5
			a[11-i] = (f1[i] - f2[i] + round)>>13;
    	*/
    	a[aIndex]=4096;
    	
    	tempIndex5=aIndex+1;
    	tempIndex6=aIndex+10;
    	
    	tempIndex3=1;
    	tempIndex4=7;
    	
    	for (k=5; k>0; k--)
    	{
    		temp2 = f[tempIndex3] + f[tempIndex4];
    		a[tempIndex5++] = (short)((temp2+4096)>>13);
    		
    		temp2 = f[tempIndex3] - f[tempIndex4];
    		tempIndex3++;
    		tempIndex4++;
    		a[tempIndex6--] = (short)((temp2+4096)>>13);    		
    	}        	
    }
    
    private void getLspPoly(short[] lsp,int lspIndex,int[] f,int fIndex)
    {
    	tempIndex5=lspIndex;
    	tempIndex6=fIndex;
    	
    	/* f[0] = 1.0 (Q24) */
    	f[tempIndex6++]=16777216;
    	
    	f[tempIndex6++] = lsp[tempIndex5]*(-1024);
    	tempIndex5+=2;

    	for(k=2; k<=5; k++)
    	{
    		f[tempIndex6]=f[tempIndex6-2];    		

    	    for(j=k; j>1; j--)
    	    {
    	    	/* Compute f[j] = f[j] + tmp*f[j-1] + f[j-2]; */
    	    	xHi = (short)(f[tempIndex6-1]>>16);
    	    	tempShift=xHi<<16;
    	    	xLow = (short)((f[tempIndex6-1]-tempShift)>>1);

    	    	tempShift=(xHi*lsp[tempIndex5])<<2;
    	    	temp2=tempShift;
    	    	tempShift=((xLow*lsp[tempIndex5])>>15)<<2;
    	    	temp2 += tempShift;

    	    	f[tempIndex6] += f[tempIndex6-2];    	    	
    	    	f[tempIndex6--] -= temp2;         	    	    	    	
    	    }
    	    
    	    f[tempIndex6] -= lsp[tempIndex5]<< 10;
    	    
    	    tempIndex6+=k;
    	    tempIndex5+=2;    	        	        	   
    	}
    }
    
    private void interpolate(short[] out,int outIndex,short[] in1,int in1Index,short[] in2,int in2Index,short coef,int length)
    {
    	tempIndex3=outIndex;
    	tempIndex4=in1Index;
    	tempIndex5=in2Index;
    	
    	/*
    	 Performs the operation out[i] = in[i]*coef + (1-coef)*in2[i] (with rounding)
    	*/

    	tempS = (short)(16384 - coef); /* 16384 = 1.0 (Q14)*/
    	for (k = 0; k < length; k++) 
    	    out[tempIndex3++] = (short) ((coef*in1[tempIndex4++] + tempS*in2[tempIndex5++]+8192)>>14);    	
    }
    
    private void lsf2Lsp(short[] lsf,int lsfIndex,short[] lsp,int lspIndex,int count)
    {
    	tempIndex6=lspIndex;
    	for(j=0; j<count; j++)
    	{
    		tempS = (short)((lsf[lsfIndex++]*20861)>>15);
    	    /* 20861: 1.0/(2.0*PI) in Q17 */
    	    /*
    	       Upper 8 bits give the index k and
    	       Lower 8 bits give the difference, which needs
    	       to be approximated linearly
    	    */    		
    	    tempS2 = (short)(tempS>>8);
    	    tempS = (short)(tempS & 0x00ff);

    	    /* Guard against getting outside table */
    	    if (tempS2>63 || tempS2<0)
    	    	tempS2 = 63;    

    	    /* Calculate linear approximation */
    	    temp2 = Constants.COS_DERIVATIVE[tempS2]*tempS;
    	    temp2>>=12;
    	    
    	    lsp[lspIndex++] = (short)(temp2 + Constants.COS[tempS2]);
    	}
    }
    
    private void decoderInterpolateLsp(short[] syntDenum,int syntDenumIndex,short[] weightDenum,int weightDenumIndex,short[] lsfDeq,int lsfDeqIndex,short length)
    {
    	short[] lp=tempMemory;//+86 length 11 
    	    	
    	tempIndex2 = lsfDeqIndex + length;
    	len = length + 1;

    	if (decoderState.DECODER_MODE==30) 
    	{
    	    /* subframe 1: Interpolation between old and first LSF */
    	    lspInterpolate2PolyDec(lp, 86, decoderState.getLsfDeqOld(), 0, lsfDeq, lsfDeqIndex,Constants.LSF_WEIGHT_30MS[0], length);
	    	System.arraycopy(lp, 86, syntDenum, syntDenumIndex, len);
    	    BasicFunctions.expand(weightDenum, weightDenumIndex, lp, 86, Constants.LPC_CHIRP_SYNT_DENUM, len);

    	    /* subframes 2 to 6: interpolation between first and last LSF */
    	    tempIndex1 = len;
    	    for (s = 1; s < 6; s++) 
    	    {
    	    	lspInterpolate2PolyDec(lp, 86, lsfDeq, lsfDeqIndex, lsfDeq, tempIndex2,Constants.LSF_WEIGHT_30MS[i], length);
    	    	System.arraycopy(lp, 86, syntDenum, syntDenumIndex + tempIndex1, len);
    	    	BasicFunctions.expand(weightDenum,weightDenumIndex + tempIndex1, lp, 86, Constants.LPC_CHIRP_SYNT_DENUM, len);
    	    	tempIndex1 += len;
    	    }
    	}
    	else 
    	{ 	
    		/* iLBCdec_inst->mode=20 */
    	    /* subframes 1 to 4: interpolation between old and new LSF */
    		tempIndex1 = 0;
    	    for (s = 0; s < decoderState.SUBFRAMES; s++) 
    	    {
    	    	lspInterpolate2PolyDec(lp, 86, decoderState.getLsfDeqOld(), 0, lsfDeq, lsfDeqIndex,Constants.LSF_WEIGHT_20MS[i], length);
    	    	System.arraycopy(lp, 86, syntDenum, syntDenumIndex + tempIndex1, len);
    	    	BasicFunctions.expand(weightDenum, weightDenumIndex+tempIndex1, lp, 86, Constants.LPC_CHIRP_SYNT_DENUM, len);
    	    	tempIndex1 += len;
    	    }
    	}

    	/* update memory */
    	if (decoderState.DECODER_MODE==30) 
    		System.arraycopy(lsfDeq, tempIndex2, decoderState.getLsfDeqOld(), 0, length);
    	else
    		System.arraycopy(lsfDeq, lsfDeqIndex, decoderState.getLsfDeqOld(), 0, length);    	        	 
    }
    
    private void simpleLsfDeq(short[] lsfDeq,int lsfDeqIndex,short[] index,int indexIndex)
    {
    	for (i = 0; i < 3; i++) 
    	{
    		tempIndex2 = Constants.LSF_INDEX_CB[i];
    	    for (j = 0; j < Constants.LSF_DIM_CB[i]; j++)
    	    	lsfDeq[lsfDeqIndex++] = Constants.LSF_CB[tempIndex2 + (index[indexIndex]*Constants.LSF_DIM_CB[i]) + j];    	        	   	    		    	    	    	
    	    
    	    indexIndex++;
    	}    	
    		    
    	if (decoderState.LPC_N>1) 
    	{
    	    /* decode last LSF */
    		tempIndex3 = 3+indexIndex;
        	for (i = 0; i < 3; i++) 
        	{
        		tempIndex2 = Constants.LSF_INDEX_CB[i];
        	    for (j = 0; j < Constants.LSF_DIM_CB[i]; j++)
    	    	  lsfDeq[lsfDeqIndex++] = Constants.LSF_CB[tempIndex2 + index[indexIndex] * Constants.LSF_DIM_CB[i] + j];
    	      
        	    indexIndex++;
    	    }
    	}
    }
    
    private void lspInterpolate2PolyDec(short[] a,int aIndex, short[] lsf1,int lsf1Index, short[] lsf2,int lsf2Index, short coef,int length)
    {
    	short[] lsfTemp=tempMemory;//+97 length 10

    	/* interpolate LSF */
    	interpolate(lsfTemp, 97, lsf1, lsf1Index, lsf2, lsf2Index, coef, length);

    	/* Compute the filter coefficients from the LSF */
    	lsf2Poly(a,aIndex,lsfTemp,97);
    }
    
    private void lsfCheck(short[] lsf,int lsfIndex,int lsfSize)
    {
    	/* LSF separation check*/
    	for (n=0;n<2;n++) 
    	{
    		/* Run through a 2 times */
    	    for (j=0;j<decoderState.LPC_N;j++) 
    	    { /* Number of analyses per frame */
    	    	for (k=0;k<lsfSize-1;k++) 
    	    	{
    	    		tempIndex1=lsfIndex + j*lsfSize+k;
    	    		tempIndex2=tempIndex1+1;
    	    		
    	    		/* Seperate coefficients with a safety margin of 50 Hz */
    	    		if ((lsf[tempIndex2]-lsf[tempIndex1])<Constants.EPS) 
    	    		{
    	    			if (lsf[tempIndex2]<lsf[tempIndex1]) 
    	    			{
    	    				lsf[tempIndex2]= (short)(lsf[tempIndex1]+Constants.HALF_EPS);
    	    				lsf[tempIndex1]= (short)(lsf[tempIndex2]-Constants.HALF_EPS);
    	    			} 
    	    			else 
    	    			{
    	    				lsf[tempIndex1]-=Constants.HALF_EPS;
    	    				lsf[tempIndex2]+=Constants.HALF_EPS;
    	    			}
    	    		}

    	    		/* Limit minimum and maximum LSF */
    	    		if (lsf[tempIndex1]<Constants.MIN_LSF) 
    	    			lsf[tempIndex1]=Constants.MIN_LSF;
    	    		
    	    		if (lsf[tempIndex1]>Constants.MAX_LSF) 
    	    			lsf[tempIndex1]=Constants.MAX_LSF;    	    			    	    		
    	    	}
    	    }
    	}
    }
    
    private void updateDecIndex()
    {
    	short[] index=encoderBits.getCbIndex();    	  

    	  for (k=4;k<6;k++) 
    	  {
    	    /* Readjust the second and third codebook index for the first 40 sample
    	       so that they look the same as the first (in terms of lag)
    	    */
    	    if (index[k]>=44 && index[k]<108)
    	      index[k]+=64;
    	    else if (index[k]>=108 && index[k]<128)
    	      index[k]+=128;    	    
    	  }
    }
    
    private void unpackBits(short[] data,int mode) 
    {
    	short[] lsf=encoderBits.getLSF();
    	short[] cbIndex=encoderBits.getCbIndex();
    	short[] gainIndex=encoderBits.getGainIndex();
    	short[] idxVec=encoderBits.getIdxVec();
    	
    	tempIndex1=0;
    	/* First WebRtc_Word16 */
    	lsf[0]  = (short)((data[tempIndex1]>>10) & 0x3F);       /* Bit 0..5  */
    	lsf[1]  = (short)((data[tempIndex1]>>3)&0x7F);      /* Bit 6..12 */
    	lsf[2]  = (short)((data[tempIndex1]&0x7)<<4);      /* Bit 13..15 */
    	  
    	tempIndex1++;
    	/* Second WebRtc_Word16 */
    	lsf[2] |= (data[tempIndex1]>>12)&0xF;      /* Bit 0..3  */

    	if (mode==20) 
    	{
    		encoderBits.setStartIdx((short)((data[tempIndex1]>>10)&0x3));  /* Bit 4..5  */
    	    
    	    encoderBits.setStateFirst(false);
    	    if(((data[tempIndex1]>>9)&0x1)!=0)
    	    	encoderBits.setStateFirst(true);  /* Bit 6  */
    	    
    	    encoderBits.setIdxForMax((short)((data[tempIndex1]>>3)&0x3F));  /* Bit 7..12 */
    	    cbIndex[0] = (short)((data[tempIndex1]&0x7)<<4);  /* Bit 13..15 */
    	    tempIndex1++;
    	    /* Third WebRtc_Word16 */
    	    cbIndex[0] |= (data[tempIndex1]>>12)&0xE;  /* Bit 0..2  */
    	    gainIndex[0] = (short)((data[tempIndex1]>>8)&0x18);  /* Bit 3..4  */
    	    gainIndex[1] = (short)((data[tempIndex1]>>7)&0x8);  /* Bit 5  */
    	    cbIndex[3] = (short)((data[tempIndex1]>>2)&0xFE);  /* Bit 6..12 */
    	    gainIndex[3] = (short)((data[tempIndex1]<<2)&0x10);  /* Bit 13  */
    	    gainIndex[4] = (short)((data[tempIndex1]<<2)&0x8);  /* Bit 14  */
    	    gainIndex[6] = (short)((data[tempIndex1]<<4)&0x10);  /* Bit 15  */
    	} 
    	else 
    	{ /* mode==30 */
    	    lsf[3] = (short)((data[tempIndex1]>>6)&0x3F);  /* Bit 4..9  */
    	    lsf[4] = (short)((data[tempIndex1]<<1)&0x7E);  /* Bit 10..15 */
    	    tempIndex1++;
    	    /* Third WebRtc_Word16 */
    	    lsf[4] |= (data[tempIndex1]>>15)&0x1;  /* Bit 0  */
    	    lsf[5] = (short)((data[tempIndex1]>>8)&0x7F);  /* Bit 1..7  */
    	    encoderBits.setStartIdx((short)((data[tempIndex1]>>5)&0x7));  /* Bit 8..10 */
    	    
    	    encoderBits.setStateFirst(false);
    	    if((short)((data[tempIndex1]>>4)&0x1)!=0)
    	    	encoderBits.setStateFirst(true);/* Bit 11  */
    	      
    	    tempS=(short)((data[tempIndex1]<<2)&0x3C);/* Bit 12..15 */
    	    tempIndex1++;    	     
    	    
    	    /* 4:th WebRtc_Word16 */
    	    tempS |= (data[tempIndex1]>>14)&0x3;  /* Bit 0..1  */
    	    encoderBits.setIdxForMax(tempS);
    	    cbIndex[0] = (short)((data[tempIndex1]>>7)&0x78);  /* Bit 2..5  */
    	    gainIndex[0] = (short)((data[tempIndex1]>>5)&0x10);  /* Bit 6  */
    	    gainIndex[1] = (short)((data[tempIndex1]>>5)&0x8);  /* Bit 7  */
    	    cbIndex[3] = (short)((data[tempIndex1])&0xFC);  /* Bit 8..13 */
    	    gainIndex[3] = (short)((data[tempIndex1]<<3)&0x10);  /* Bit 14  */
    	    gainIndex[4] = (short)((data[tempIndex1]<<3)&0x8);  /* Bit 15  */
    	}
    	
    	/* Class 2 bits of ULP */
    	/* 4:th to 6:th WebRtc_Word16 for 20 ms case
    	   5:th to 7:th WebRtc_Word16 for 30 ms case */
    	tempIndex1++;
    	tempIndex2=0;
    	  
    	for (k=0; k<3; k++) 
    	{
    	   for (i=15; i>=0; i--) 
    		   idxVec[tempIndex2++] = (short)(((data[tempIndex1]>>i)<<2)&0x4);/* Bit 15-i  */    	      
    	    
    	   tempIndex1++;
    	}

    	if (mode==20) 
    	{
    	    /* 7:th WebRtc_Word16 */
    	    for (i=15; i>6; i--)
    	    	idxVec[tempIndex2++] = (short)(((data[tempIndex1]>>i)<<2)&0x4); /* Bit 15-i  */    	    
    	    
    	    gainIndex[1] |= (data[tempIndex1]>>4)&0x4; /* Bit 9  */
    	    gainIndex[3] |= (data[tempIndex1]>>2)&0xC; /* Bit 10..11 */
    	    gainIndex[4] |= (data[tempIndex1]>>1)&0x4; /* Bit 12  */
    	    gainIndex[6] |= (data[tempIndex1]<<1)&0x8; /* Bit 13  */
    	    gainIndex[7] = (short)((data[tempIndex1]<<2)&0xC); /* Bit 14..15 */

    	} 
    	else 
    	{   /* mode==30 */
    	    /* 8:th WebRtc_Word16 */
    	    for (i=15; i>5; i--)
    	    	idxVec[tempIndex2++] = (short)(((data[tempIndex1]>>i)<<2)&0x4);/* Bit 15-i  */    	      
    	    
    	    cbIndex[0] |= (data[tempIndex1]>>3)&0x6; /* Bit 10..11 */
    	    gainIndex[0] |= (data[tempIndex1])&0x8;  /* Bit 12  */
    	    gainIndex[1] |= (data[tempIndex1])&0x4;  /* Bit 13  */
    	    cbIndex[3] |= (data[tempIndex1])&0x2;  /* Bit 14  */
    	    cbIndex[6] = (short)((data[tempIndex1]<<7)&0x80); /* Bit 15  */
    	    tempIndex1++;
    	    /* 9:th WebRtc_Word16 */
    	    cbIndex[6] |= (data[tempIndex1]>>9)&0x7E; /* Bit 0..5  */
    	    cbIndex[9] = (short)((data[tempIndex1]>>2)&0xFE); /* Bit 6..12 */
    	    cbIndex[12] = (short)((data[tempIndex1]<<5)&0xE0); /* Bit 13..15 */
    	    tempIndex1++;
    	    /* 10:th WebRtc_Word16 */
    	    cbIndex[12] |= (data[tempIndex1]>>11)&0x1E;/* Bit 0..3 */
    	    gainIndex[3] |= (data[tempIndex1]>>8)&0xC; /* Bit 4..5  */
    	    gainIndex[4] |= (data[tempIndex1]>>7)&0x6; /* Bit 6..7  */
    	    gainIndex[6] = (short)((data[tempIndex1]>>3)&0x18); /* Bit 8..9  */
    	    gainIndex[7] = (short)((data[tempIndex1]>>2)&0xC); /* Bit 10..11 */
    	    gainIndex[9] = (short)((data[tempIndex1]<<1)&0x10); /* Bit 12  */
    	    gainIndex[10] = (short)((data[tempIndex1]<<1)&0x8); /* Bit 13  */
    	    gainIndex[12] = (short)((data[tempIndex1]<<3)&0x10); /* Bit 14  */
    	    gainIndex[13] = (short)((data[tempIndex1]<<3)&0x8); /* Bit 15  */
    	}
    	tempIndex1++;
    	  
    	/* Class 3 bits of ULP */
    	/* 8:th to 14:th WebRtc_Word16 for 20 ms case
    	   11:th to 17:th WebRtc_Word16 for 30 ms case */
    	tempIndex2=0;
    	for (k=0; k<7; k++) 
    	{
    	    for (i=14; i>=0; i-=2)
    	      idxVec[tempIndex2++] |= (data[tempIndex1]>>i)&0x3; /* Bit 15-i..14-i*/
    	      
    	    tempIndex1++;
    	}

    	if (mode==20) 
    	{
    	    /* 15:th WebRtc_Word16 */
    		idxVec[56] |= (data[tempIndex1]>>14)&0x3; /* Bit 0..1  */
    	    cbIndex[0] |= (data[tempIndex1]>>13)&0x1; /* Bit 2  */
    	    cbIndex[1] = (short)((data[tempIndex1]>>6)&0x7F); /* Bit 3..9  */
    	    cbIndex[2] = (short)((data[tempIndex1]<<1)&0x7E); /* Bit 10..15 */
    	    tempIndex1++;
    	    /* 16:th WebRtc_Word16 */
    	    cbIndex[2] |= (data[tempIndex1]>>15)&0x1; /* Bit 0  */
    	    gainIndex[0] |= (data[tempIndex1]>>12)&0x7; /* Bit 1..3  */
    	    gainIndex[1] |= (data[tempIndex1]>>10)&0x3; /* Bit 4..5  */
    	    gainIndex[2] = (short)((data[tempIndex1]>>7)&0x7); /* Bit 6..8  */
    	    cbIndex[3] |= (data[tempIndex1]>>6)&0x1; /* Bit 9  */
    	    cbIndex[4] = (short)((data[tempIndex1]<<1)&0x7E); /* Bit 10..15 */
    	    tempIndex1++;
    	    /* 17:th WebRtc_Word16 */
    	    cbIndex[4] |= (data[tempIndex1]>>15)&0x1; /* Bit 0  */
    	    cbIndex[5] = (short)((data[tempIndex1]>>8)&0x7F); /* Bit 1..7  */
    	    cbIndex[6] = (short)((data[tempIndex1])&0xFF); /* Bit 8..15 */
    	    tempIndex1++;
    	    /* 18:th WebRtc_Word16 */
    	    cbIndex[7] = (short)((data[tempIndex1]>>8) & 0xFF);  /* Bit 0..7  */
    	    cbIndex[8] = (short)(data[tempIndex1]&0xFF);  /* Bit 8..15 */
    	    tempIndex1++;
    	    /* 19:th WebRtc_Word16 */
    	    gainIndex[3] |= (data[tempIndex1]>>14)&0x3; /* Bit 0..1  */
    	    gainIndex[4] |= (data[tempIndex1]>>12)&0x3; /* Bit 2..3  */
    	    gainIndex[5] = (short)((data[tempIndex1]>>9)&0x7); /* Bit 4..6  */
    	    gainIndex[6] |= (data[tempIndex1]>>6)&0x7; /* Bit 7..9  */
    	    gainIndex[7] |= (data[tempIndex1]>>4)&0x3; /* Bit 10..11 */
    	    gainIndex[8] = (short)((data[tempIndex1]>>1)&0x7); /* Bit 12..14 */
    	} 
    	else 
    	{   /* mode==30 */
    	    /* 18:th WebRtc_Word16 */
    		idxVec[56] |= (data[tempIndex1]>>14)&0x3; /* Bit 0..1  */
    		idxVec[57] |= (data[tempIndex1]>>12)&0x3; /* Bit 2..3  */
    	    cbIndex[0] |= (data[tempIndex1]>>11)&1; /* Bit 4  */
    	    cbIndex[1] = (short)((data[tempIndex1]>>4)&0x7F); /* Bit 5..11 */
    	    cbIndex[2] = (short)((data[tempIndex1]<<3)&0x78); /* Bit 12..15 */
    	    tempIndex1++;
    	    /* 19:th WebRtc_Word16 */
    	    cbIndex[2] |= (data[tempIndex1]>>13)&0x7; /* Bit 0..2  */
    	    gainIndex[0] |= (data[tempIndex1]>>10)&0x7; /* Bit 3..5  */
    	    gainIndex[1] |= (data[tempIndex1]>>8)&0x3; /* Bit 6..7  */
    	    gainIndex[2] = (short)((data[tempIndex1]>>5)&0x7); /* Bit 8..10 */
    	    cbIndex[3] |= (data[tempIndex1]>>4)&0x1; /* Bit 11  */
    	    cbIndex[4] = (short)((data[tempIndex1]<<3)&0x78); /* Bit 12..15 */
    	    tempIndex1++;
    	    /* 20:th WebRtc_Word16 */
    	    cbIndex[4] |= (data[tempIndex1]>>13)&0x7; /* Bit 0..2  */
    	    cbIndex[5] = (short)((data[tempIndex1]>>6)&0x7F); /* Bit 3..9  */
    	    cbIndex[6] |= (data[tempIndex1]>>5)&0x1; /* Bit 10  */
    	    cbIndex[7] = (short)((data[tempIndex1]<<3)&0xF8); /* Bit 11..15 */
    	    tempIndex1++;
    	    /* 21:st WebRtc_Word16 */
    	    cbIndex[7] |= (data[tempIndex1]>>13)&0x7; /* Bit 0..2  */
    	    cbIndex[8] = (short)((data[tempIndex1]>>5)&0xFF); /* Bit 3..10 */
    	    cbIndex[9] |= (data[tempIndex1]>>4)&0x1; /* Bit 11  */
    	    cbIndex[10] = (short)((data[tempIndex1]<<4)&0xF0); /* Bit 12..15 */
    	    tempIndex1++;
    	    /* 22:nd WebRtc_Word16 */
    	    cbIndex[10] |= (data[tempIndex1]>>12)&0xF; /* Bit 0..3  */
    	    cbIndex[11] = (short)((data[tempIndex1]>>4)&0xFF); /* Bit 4..11 */
    	    cbIndex[12] |= (data[tempIndex1]>>3)&0x1; /* Bit 12  */
    	    cbIndex[13] = (short)((data[tempIndex1]<<5)&0xE0); /* Bit 13..15 */
    	    tempIndex1++;
    	    /* 23:rd WebRtc_Word16 */
    	    cbIndex[13] |= (data[tempIndex1]>>11)&0x1F;/* Bit 0..4  */
    	    cbIndex[14] = (short)((data[tempIndex1]>>3)&0xFF); /* Bit 5..12 */
    	    gainIndex[3] |= (data[tempIndex1]>>1)&0x3; /* Bit 13..14 */
    	    gainIndex[4] |= (data[tempIndex1]&0x1);  /* Bit 15  */
    	    tempIndex1++;
    	    /* 24:rd WebRtc_Word16 */
    	    gainIndex[5] = (short)((data[tempIndex1]>>13)&0x7); /* Bit 0..2  */
    	    gainIndex[6] |= (data[tempIndex1]>>10)&0x7; /* Bit 3..5  */
    	    gainIndex[7] |= (data[tempIndex1]>>8)&0x3; /* Bit 6..7  */
    	    gainIndex[8] = (short)((data[tempIndex1]>>5)&0x7); /* Bit 8..10 */
    	    gainIndex[9] |= (data[tempIndex1]>>1)&0xF; /* Bit 11..14 */
    	    gainIndex[10] |= (data[tempIndex1]<<2)&0x4; /* Bit 15  */
    	    tempIndex1++;
    	    /* 25:rd WebRtc_Word16 */
    	    gainIndex[10] |= (data[tempIndex1]>>14)&0x3; /* Bit 0..1  */
    	    gainIndex[11] = (short)((data[tempIndex1]>>11)&0x7); /* Bit 2..4  */
    	    gainIndex[12] |= (data[tempIndex1]>>7)&0xF; /* Bit 5..8  */
    	    gainIndex[13] |= (data[tempIndex1]>>4)&0x7; /* Bit 9..11 */
    	    gainIndex[14] = (short)((data[tempIndex1]>>1)&0x7); /* Bit 12..14 */
    	}    	      	  
    }
    
    private void cbConstruct(short[] decVector,int decVectorIndex,short[] mem,int memIndex,short length,short vectorLength,int cbIndexIndex,int gainIndexIndex)
    {
    	short[] cbIndex=encoderBits.getCbIndex();
    	short[] gainIndex=encoderBits.getGainIndex();
    	
    	/* Stack based */
    	short gain[]=tempMemory;//+86 length 3
    	short cbVec0[]=tempMemory;//+89 length 40
    	short cbVec1[]=tempMemory;//+129 length 40
    	short cbVec2[]=tempMemory;//+169 length 40
    	
    	/* gain de-quantization */
    	gain[86] = gainDequant(gainIndex[gainIndexIndex], (short)16384, (short)0);
    	gain[87] = gainDequant(gainIndex[gainIndexIndex+1], (short)gain[86], (short)1);
    	gain[88] = gainDequant(gainIndex[gainIndexIndex+2], (short)gain[87], (short)2);

    	/* codebook vector construction and construction of total vector */

    	for(i=0;i<40;i++)
    	{
    		cbVec0[89+i]=0;
    		cbVec1[129+i]=0;
    		cbVec2[169+i]=0;
    	}
    	/* Stack based */
    	getCbVec(cbVec0, 89, mem, memIndex, cbIndex[cbIndexIndex], length, vectorLength);
    	getCbVec(cbVec1, 129, mem, memIndex, cbIndex[cbIndexIndex+1], length, vectorLength);
    	getCbVec(cbVec2, 169, mem, memIndex, cbIndex[cbIndexIndex+2], length, vectorLength);
    	    	
    	tempIndex5=89;
    	tempIndex6=129;
    	tempIndex7=169;
    	for(i=0;i<vectorLength;i++)
    	{
    		temp=gain[86]*cbVec0[tempIndex5++];
    		temp+=gain[87]*cbVec1[tempIndex6++];
    		temp+=gain[88]*cbVec2[tempIndex7++];
    		    		
    		tempShift=(temp+8192)>>14;
    		decVector[decVectorIndex++]=(short)tempShift;
    	} 	
    }
    
    private short gainDequant(short index, short maxIn, short stage)
    {
    	/* obtain correct scale factor */
    	if(maxIn<0)
    		maxIn=(short)(0-maxIn);
    	
    	if(maxIn<1638)/* if lower than 0.1, set it to 0.1 */
    		maxIn=1638;    	    	    	

    	/* select the quantization table and return the decoded value */    	
    	return (short)((maxIn*Constants.GAIN[stage][index] +8192)>>14);
    }
    
    private void getCbVec(short[] cbVec,int cbVecIndex,short[] mem,int memIndex,short index,int length,int vectorLength)
    {
    	/* Stack based */    	  
    	short tempbuff2[]=tempMemory;//+209 length 45    	        	

    	/* Determine size of codebook sections */
    	baseSize=(short)(length-vectorLength+1);

    	if (vectorLength==40)
    	    baseSize+=vectorLength>>1;    	  

    	/* No filter -> First codebook section */

    	if (index<length-vectorLength+1) 
    	{
    	    /* first non-interpolated vectors */
    	    k=index+vectorLength;
    	    /* get vector */
    	    System.arraycopy(mem, memIndex+length-k, cbVec, cbVecIndex, vectorLength);    	    
    	} 
    	else if (index < baseSize) 
    	{
    	    /* Calculate lag */
    	    k=2*(index-(length-vectorLength+1))+vectorLength;
    	    createAugmentVector((short)(k>>1),mem,memIndex+length,cbVec,cbVecIndex);    	    
    	}
    	/* Higher codebbok section based on filtering */
    	else 
    	{
    	    /* first non-interpolated vectors */
    	    if (index-baseSize<length-vectorLength+1) 
    	    {
    	    	/* Set up filter memory, stuff zeros outside memory buffer */
    	    	tempIndex7=memIndex-4;
    	    	for(n=0;n<4;n++)
    	    		mem[tempIndex7++]=0;
    	    	
    	    	tempIndex7=memIndex+length;
    	    	for(n=0;n<4;n++)
    	    		mem[tempIndex7++]=0;

    	    	/* do filtering to get the codebook vector */
    	    	BasicFunctions.filterMA(mem, memIndex+length-(index-baseSize+vectorLength)+4, cbVec, cbVecIndex, Constants.CB_FILTERS_REV, 0, 8, vectorLength);    	    	
    	    }
    	    /* interpolated vectors */
    	    else 
    	    {
    	    	/* Stuff zeros outside memory buffer  */    	    	
    	    	tempIndex7=memIndex+length;
    	    	for(n=0;n<4;n++)
    	    		mem[tempIndex7++]=0;    	    	

    	    	/* do filtering */
    	    	BasicFunctions.filterMA(mem, memIndex+length-vectorLength-1, tempbuff2, 209, Constants.CB_FILTERS_REV, 0, 8, vectorLength+5);    	    	

    	    	createAugmentVector((short)((vectorLength<<1)-20+index-baseSize-length-1),tempbuff2,254,cbVec,cbVecIndex);    	    	
    	     }
    	 }
    }
    
    private void createAugmentVector(short index,short[] buf,int bufIndex,short[] cbVec,int cbVecIndex)
    {
    	short[] cbVecTmp=tempMemory;//+254 length 4    	
    	
    	tempIndex7=cbVecIndex+index-4;    	

    	/* copy the first noninterpolated part */    	
    	System.arraycopy(buf, bufIndex-index, cbVec, cbVecIndex, index);    	

    	/* perform cbVec[ilow+k] = ((ppi[k]*alphaTbl[k])>>15) + ((ppo[k]*alphaTbl[3-k])>>15);
    	   for k = 0..3
    	*/
    	BasicFunctions.multWithRightShift(cbVec, tempIndex7, buf, bufIndex-index-4, Constants.ALPHA, 0, 4, 15);
    	BasicFunctions.reverseMultiplyRight(cbVecTmp, 254, buf, bufIndex-4, Constants.ALPHA, 3, 4, 15);
    	BasicFunctions.addWithRightShift(cbVec, tempIndex7, cbVec, tempIndex7, cbVecTmp, 254, 4, 0);    	

    	/* copy the second noninterpolated part */
    	System.arraycopy(buf, bufIndex-index, cbVec, cbVecIndex + index, 40-index);    	
    }
    
    private void stateConstruct(short[] syntDenum,int syntDenumIndex,short[] outFix,int outFixIndex)    
    {
    	/* Stack based */
    	short[] numerator=tempMemory; //+86 length 11
    	short[] sampleValVec=tempMemory; //+97 length 126
    	short[] sampleMaVec=tempMemory; //+223 length 126
    	short[] sampleVal=tempMemory; //+107 length 116
    	short[] sampleMa=tempMemory; //+233 length 116;
    	short[] sampleAr=tempMemory; //+107 length 116;    	  
    	
    	short[] idxVec=encoderBits.getIdxVec();
    	/* initialization of coefficients */
    	tempIndex1=86;
    	tempIndex2=syntDenumIndex+10;
    	for (k=0; k<11; k++)
    	    numerator[tempIndex1++] = syntDenum[tempIndex2--];
    	
    	/* decoding of the maximum value */
    	max = Constants.FRQ_QUANT_MOD[encoderBits.getIdxForMax()];

    	/* decoding of the sample values */
    	tempIndex1=107;
    	tempIndex2 = decoderState.STATE_SHORT_LEN-1;

    	if (encoderBits.getIdxForMax()<37) 
    	{
    	    for(k=0; k<decoderState.STATE_SHORT_LEN; k++)
    	    {
    	      /*the shifting is due to the Q13 in sq4_fixQ13[i], also the adding of 2097152 (= 0.5 << 22)
    	      max is in Q8 and result is in Q(-1) */
    	      sampleVal[tempIndex1++]=(short) ((max*Constants.STATE_SQ3[idxVec[tempIndex2--]]+2097152) >> 22);    	      
    	    }
    	} 
    	else if (encoderBits.getIdxForMax()<59) 
    	{
    	    for(k=0; k<decoderState.STATE_SHORT_LEN; k++)
    	    {
    	      /*the shifting is due to the Q13 in sq4_fixQ13[i], also the adding of 262144 (= 0.5 << 19)
    	        max is in Q5 and result is in Q(-1) */
    	      sampleVal[tempIndex1++]=(short) ((max*Constants.STATE_SQ3[idxVec[tempIndex2--]]+262144) >> 19);    	      
    	    }
    	} 
    	else 
    	{
    	    for(k=0; k<decoderState.STATE_SHORT_LEN; k++)
    	    {
    	      /*the shifting is due to the Q13 in sq4_fixQ13[i], also the adding of 65536 (= 0.5 << 17)
    	        max is in Q3 and result is in Q(-1) */
    	      sampleVal[tempIndex1++]=(short) ((max*Constants.STATE_SQ3[idxVec[tempIndex2--]]+65536) >> 17);    	      
    	    }
    	}

    	/* Set the rest of the data to zero */
    	tempIndex1=107+decoderState.STATE_SHORT_LEN;
    	for(i=0;i<decoderState.STATE_SHORT_LEN;i++)
    		sampleVal[tempIndex1++]=0;    	

    	/* circular convolution with all-pass filter */

    	/* Set the state to zero */
    	tempIndex1=97;
    	for(i=0;i<10;i++)
    		sampleValVec[tempIndex1++]=0;    	

    	/* Run MA filter + AR filter */
    	BasicFunctions.filterMA(sampleVal, 107, sampleMa, 233, numerator, 86, 11, 11+decoderState.STATE_SHORT_LEN);    	
    	
    	tempIndex1=243+decoderState.STATE_SHORT_LEN;
    	for(i=0;i<decoderState.STATE_SHORT_LEN-10;i++)
    		sampleMa[tempIndex1++]=0;
    	
    	BasicFunctions.filterAR(sampleMa, 233, sampleAr, 107, syntDenum, syntDenumIndex, 11, 2*decoderState.STATE_SHORT_LEN);
    	
    	tempIndex1=107+decoderState.STATE_SHORT_LEN-1;
    	tempIndex2=107+2*decoderState.STATE_SHORT_LEN-1;
    	tempIndex3=outFixIndex;    	
    	for(k=0;k<decoderState.STATE_SHORT_LEN;k++)
    		outFix[tempIndex3++]=(short)(sampleAr[tempIndex1--]+sampleAr[tempIndex2--]);    	    	
    }
}
