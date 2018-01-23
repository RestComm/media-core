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

import java.util.Arrays;

import org.restcomm.media.spi.dsp.Codec;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;
/**
 * 
 * @author oifa yulian
 * 
 */
public class Encoder implements Codec {

    private final static Format ilbc = FormatFactory.createAudioFormat("ilbc", 8000, 16, 1);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private short[] signal = new short[160];
    
    //encoder state
    private EncoderState encoderState=new EncoderState();

    //encoder bits
    private EncoderBits encoderBits=new EncoderBits();
    
    //cb update index data
    CbUpdateIndexData updateIndexData=new CbUpdateIndexData();
    
    //cb search data
    CbSearchData searchData=new CbSearchData();
    
    //other variables
    private int i,j,k,n;
    private int tempL,temp,temp2,temp3,temp4,tempShift;
    private int tempIndex1,tempIndex2,tempIndex3,tempIndex4,tempIndex5,tempIndex6,tempIndex7,tempIndex8,tempIndex9;
    private int memlGotten, nFor, nBack;
    private int index, b2, minValue, sum;
    private int subCount, subFrame, stage;
    private int en1, en2, foundFreqs;
    private int codedEner, targetEner;
    
    //max used 256
    private int[] tempLMemory=new int[256];
    
    private short diff, startPos;
	private short scale,scale2, max, baseSize, range;
	private short tempS,tempS2,nBits,alphaExp;
	private short xHi,xLow,xMid,x,yHi,yLow,yMid,y;
	private short b1Hi,b1Low;
	private short sInd, eInd;
	private boolean isStable;
	
	private short[] weightdenum=new short[66];
	private short[] dataVec=new short[250];
	private short[] memVec=new short[155];
	private short[] residual=new short[160];
		
	//max used 1335
    private short[] tempMemory=new short[1350];        
    
    public Frame process(Frame frame) {    	
    	byte[] data = frame.getData();    	    	
    	
    	temp=10;
    	for (i = 0; i < 160; i++) {
    		dataVec[temp++] = ((short) ((data[i*2 + 1] << 8) | (data[i*2] & 0xFF)));
        }
    	     
    	/* xLow pass filtering of input signal and scale down the residual (*0.5) */
    	hpInput(dataVec,10,160);
    	
    	/* LPC of hp filtered input data */
    	lpcEncode(memVec, 4, weightdenum, 0, dataVec, 10);    	

    	/* Set up state */
    	System.arraycopy(encoderState.getAnaMem(), 0, dataVec, 0, 10);    	    

    	/* inverse filter to get residual */
    	for (i=0; i<encoderState.SUBFRAMES; i++ )
    	    BasicFunctions.filterMA(dataVec, 10+i*40, residual, i*40,memVec, 4 + i*11,11, 40);        	
    	
    	/* Copy the state for next frame */
    	System.arraycopy(dataVec, 160, encoderState.getAnaMem(), 0, 10);    	

    	/* find state location */
    	encoderBits.setStartIdx(frameClassify(residual));
    	
    	/* check if state should be in first or last part of the two subframes */
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

    	/* Scale to maximum 25 bits so that the MAC won't cause overflow */
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

    	/* scalar quantization of state */
    	stateSearch(residual, startPos, memVec, 4 + (encoderBits.getStartIdx()-1)*11, weightdenum, (encoderBits.getStartIdx()-1)*11);    	
    	stateConstruct(memVec, 4 + (encoderBits.getStartIdx()-1)*11, residual , startPos);

    	/* predictive quantization in state */
    	if (encoderBits.getStateFirst()) 
    	{ /* put adaptive part in the end */
    	   /* setup memory */
    	   for(i=4;i<151-encoderState.STATE_SHORT_LEN;i++)
    		   memVec[i]=0;
    	   
    	   System.arraycopy(residual, startPos, memVec, 151-encoderState.STATE_SHORT_LEN, encoderState.STATE_SHORT_LEN);    	   

    	   /* encode subframes */
       	   cbSearch(residual, startPos+encoderState.STATE_SHORT_LEN, memVec, 66, 85, diff, weightdenum, encoderBits.getStartIdx()*11, 0,0,0);
    	   
    	   /* construct decoded vector */
    	   cbConstruct(residual,startPos+encoderState.STATE_SHORT_LEN,memVec,66,(short)85,diff,0,0);    	       	   
    	}
    	else 
    	{ 
    		/* put adaptive part in the beginning */    		
    	   /* create reversed vectors for prediction */
    	   BasicFunctions.reverseCopy(dataVec, diff+9, residual, encoderBits.getStartIdx()*40-40, diff);
    	   
    	   /* setup memory */
    	   BasicFunctions.reverseCopy(memVec, 150, residual, startPos, encoderState.STATE_SHORT_LEN);
    	   
    	   for(i=4;i<151-encoderState.STATE_SHORT_LEN;i++)
    		   memVec[i]=0;
    	   
    	   /* encode subframes */
    	   cbSearch(dataVec, 10, memVec, 66, 85, diff, weightdenum, (encoderBits.getStartIdx()-1)*11, 0, 0, 0);
    	       	   
    	   cbConstruct(dataVec,10,memVec,66, (short)85, diff, 0, 0);

    	   /* get decoded residual from reversed vector */
    	   BasicFunctions.reverseCopy(residual, startPos-1, dataVec, 10, diff);    	   
    	}

    	/* forward prediction of subframes */
    	nFor = encoderState.SUBFRAMES-encoderBits.getStartIdx()-1;

    	/* counter for predicted subframes */
    	subCount=1;

    	if( nFor > 0 )
    	{
    		/* setup memory */
    		for(i=4;i<71;i++)
     		   memVec[i]=0;
    	    
    		System.arraycopy(residual, (encoderBits.getStartIdx()-1)*40, memVec, 71, 80);  	    
    		
    	    /* loop over subframes to encode */
    	    for (subFrame = 0; subFrame < nFor; subFrame++)
    	    {    	    	
    	      /* encode subframe */
    	      cbSearch(residual,(encoderBits.getStartIdx()+1+subFrame)*40,memVec,4,147,40,weightdenum,(encoderBits.getStartIdx()+1+subFrame)*11, subCount, subCount*3, subCount*3);    	      

    	      /* construct decoded vector */
    	      cbConstruct(residual, (encoderBits.getStartIdx()+1+subFrame)*40, memVec, 4, (short)147, (short)40, subCount*3, subCount*3);    	      

    	      /* update memory */
    	      temp=4;
  	    	  for(i=44;i<151;i++)
  	    		  memVec[temp++]=memVec[i];
  	    	
    	      System.arraycopy(residual, (encoderBits.getStartIdx()+1+subFrame)*40, memVec, 111, 40);    	      

    	      subCount++;
    	    }
    	}
    	
    	/* backward prediction of subframes */
    	nBack = encoderBits.getStartIdx()-1;

    	if( nBack > 0 )
    	{
    		/* create reverse order vectors (The decresidual does not need to be copied since it is contained in the same vector as the residual) */
    		BasicFunctions.reverseCopy(dataVec, 10 + nBack*40-1, residual, 0, nBack*40);     	   

    		/* setup memory */    		
    	    memlGotten = 40*(encoderState.SUBFRAMES+1-encoderBits.getStartIdx());
    	    if( memlGotten > 147 )
    	    	memlGotten=147;    

    	    BasicFunctions.reverseCopy(memVec, 150, residual, nBack*40,memlGotten);
    	    for(i=4;i<151-memlGotten;i++)
    	    	memVec[i]=0;    	        	        	    
        	
    	    /* loop over subframes to encode */
    	    for (subFrame = 0; subFrame < nBack; subFrame++)
    	    {
    	    	/* encode subframe */
    	    	cbSearch(dataVec, 10 + subFrame*40, memVec, 4, 147, 40, weightdenum, (encoderBits.getStartIdx()-2-subFrame)*11, subCount, subCount*3, subCount*3);

    	    	/* construct decoded vector */
    	    	cbConstruct(dataVec, 10 + subFrame*40, memVec, 4, (short)147, (short)40, subCount*3, subCount*3);    	      

    	    	/* update memory */
    	    	temp=4;
    	    	for(i=44;i<151;i++)
    	    		memVec[temp++]=memVec[i];
    	    	
    	    	System.arraycopy(dataVec, 10 + subFrame*40, memVec, 111, 40);    	    	
    	    	subCount++;
    	    }

    	    /* get decoded residual from reversed vector */
    	    BasicFunctions.reverseCopy(residual, 40*nBack-1, dataVec, 10, 40*nBack);    	    
    	}
    	/* end encoding part */

    	/* Packetize the parameters into the frame */
    	
    	/* adjust index */
    	short[] index=encoderBits.getCbIndex();
    	for (k=4;k<6;k++) 
    	{
    	    /* Readjust the second and third codebook index so that it is packetized into 7 bits (before it was put in lag-wise the same
    	       way as for the first codebook which uses 8 bits) 
    	    */
    	    if (index[k]>=108 && index[k]<172)
    	      index[k]-=64;
    	    else if (index[k]>=236)
    	      index[k]-=128;
    	    else
    	    {
    	    	/* ERROR */
    	    }
    	}

    	System.out.println("************************************************************");
    	System.out.println("PACKED DATA");
    	System.out.println("************************************************************");
    	System.out.println("START IDX:" + encoderBits.getStartIdx() + ",STATE FIRST:" + encoderBits.getStateFirst());
    	System.out.print("LSF:");
    	for(i=0;i<encoderBits.getLSF().length;i++)
    		System.out.print(encoderBits.getLSF()[i] + ",");
    	
    	System.out.println();
    	
    	System.out.print("IDX VEC:");
    	for(i=0;i<encoderBits.getIdxVec().length;i++)
    		System.out.print(encoderBits.getIdxVec()[i] + ",");
    	
    	System.out.println();
    	System.out.print("CB INDEX:");
    	for(i=0;i<encoderBits.getCbIndex().length;i++)
    		System.out.print(encoderBits.getCbIndex()[i] + ",");
    	
    	System.out.println();
    	
    	System.out.print("GAIN INDEX:");
    	for(i=0;i<encoderBits.getGainIndex().length;i++)
    		System.out.print(encoderBits.getGainIndex()[i] + ",");
    	
    	System.out.println();
    	    	
    	System.out.println("STATE FIRST:" + encoderBits.getStateFirst());
    	System.out.println("START IDX:" + encoderBits.getStartIdx());
    	System.out.println("IDX FOR MAX:" + encoderBits.getIdxForMax());
    	
    	Frame res = Memory.allocate(38);
    	packBits(res.getData());       	    	
    	
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
    
    private void hpInput(short[] data,int startIndex,int length)
    {
    	short[] ba=Constants.HP_IN_COEFICIENTS;
    	short[] y=encoderState.getHpiMemY();
    	short[] x=encoderState.getHpiMemX();
    	
    	temp3=startIndex+length;
    	for (i=startIndex; i<temp3; i++) 
    	{    	   
    		temp  = y[1]*ba[3];     /* (-a[1])*y[i-1] (low part) */
    	    temp += y[3]*ba[4];     /* (-a[2])*y[i-2] (low part) */
    	    temp = temp>>15;
    	    temp += y[0]*ba[3];     /* (-a[1])*y[i-1] (high part) */
    	    temp += y[2]*ba[4];     /* (-a[2])*y[i-2] (high part) */
    	    temp = temp<<1;
    	    
    	    temp += data[i]*ba[0];   /* b[0]*x[0] */
    	    temp += x[0]*ba[1];   /* b[1]*x[i-1] */
    	    temp += x[1]*ba[2];   /* b[2]*x[i-2] */

    	    /* Update state (input part) */
    	    x[1] = x[0];
    	    x[0] = data[i];

    	    /* Rounding in Q(12+1), i.e. add 2^12 */
    	    temp2 = temp + 4096;

    	    /* Saturate (to 2^28) so that the HP filtered signal does not overflow */
    	    if(temp2>268435455)
    	    	temp2=268435455;
    	    else if(temp2<-268435456)
    	    	temp2=-268435456;
    	    
    	    /* Convert back to Q0 and multiply with 0.5 */
    	    data[i] = (short)(temp2>>13);

    	    /* Update state (filtered part) */
    	    y[2] = y[0];
    	    y[3] = y[1];

    	    /* upshift temp by 3 with saturation */
    	    if (temp>268435455)
    	      temp = Integer.MAX_VALUE;
    	    else if (temp<-268435456)
    	      temp = Integer.MIN_VALUE;
    	    else
    	      temp = temp<<3;    	    

    	    y[0] = (short)(temp >> 16);
    	    tempShift=y[0]<<16;
    	    y[1] = (short)((temp - tempShift)>>1);    	    
        }
    }
    
    private void lpcEncode(short[] synthDenum,int synthDenumIndex,short[] weightDenum,int weightDenumIndex,short[] data,int startIndex)
    {
    	short[] lsf=tempMemory; //+ 0 length 20
        short[] lsfDeq=tempMemory; //+ 20 length 20                
    	
        /* Calculate LSF's from the input speech */
        simpleLpcAnalysis(lsf,0,data,startIndex);    	
        
        /* Quantize the LSF's */
        simpleLsfQ(lsfDeq, 20, lsf, 0);        
        
        /* Stableize the LSF's if needed */
        lsfCheck(lsfDeq , 20, 10);
        
        /* Calculate the synthesis and weighting filter coefficients from the optimal LSF and the dequantized LSF */
    	simpleInterpolateLsf(synthDenum, synthDenumIndex, weightDenum, weightDenumIndex, lsf, 0, lsfDeq , 20, 10);    	    	
    }
    
    private void simpleLpcAnalysis(short[] lsf,int lsfIndex,short[] data,int startIndex)
    {
    	  short[] A=tempMemory; //+ 40 length 11
    	  short[] windowedData=tempMemory; //+ 51 length 240
    	  short[] rc=tempMemory; //+ 291 length 10
    	  int[] R=tempLMemory; //+ 0 length 11

    	  short[] lpcBuffer=encoderState.getLpcBuffer();    	  
    	  System.arraycopy(data, startIndex, lpcBuffer, 300-encoderState.SIZE, encoderState.SIZE);    	  

    	  /* No lookahead, last window is asymmetric */
    	  for (k = 0; k < encoderState.LPC_N; k++) 
    	  {
    		  if (k < encoderState.LPC_N - 1)     			  
    		  {
    			  /* Hanning table kLpcWin[] is in Q15-domain so the output is right-shifted 15 */
    			  BasicFunctions.multWithRightShift(windowedData,51, lpcBuffer,0, Constants.LPC_WIN, 0, 240, 15);    			                	    	    	
    		  }
    		  else 
    		  {
    			  /* Hanning table kLpcAsymWin[] is in Q15-domain so the output is right-shifted 15 */
    			  BasicFunctions.multWithRightShift(windowedData,51, lpcBuffer,60, Constants.LPC_ASYM_WIN, 0, 240, 15);        		          		  
    		  }

    		  /* Compute autocorrelation */
    		  autoCorrelation(windowedData,51, 240, 10, R, 0);
    		      		  
    		  /* Window autocorrelation vector */
    		  windowMultiply(R,0,R,0,Constants.LPC_LAG_WIN, 11);    		      		      	    		    		  
    	    	    	    	
    		  /* Calculate the A coefficients from the Autocorrelation using Levinson Durbin algorithm */
    		  isStable=levinsonDurbin(R, 0, A, 40, rc, 291, 10);
    		      		  
    		  /*
    	       	Set the filter to {1.0, 0.0, 0.0,...} if filter from Levinson Durbin algorithm is unstable
    	       	This should basically never happen...
    		  */
    		  if (!isStable) 
    		  {
    			  A[40]=4096;
    			  for(j=41;j<51;j++)
    				  A[j]=0;    			  
    		  }

    		  /* Bandwidth expand the filter coefficients */
    		  BasicFunctions.expand(A,40,A,40,Constants.LPC_CHIRP_SYNT_DENUM,11);    		      		      		  
    	          		  
    		  /* Convert from A to LSF representation */
    		  poly2Lsf(lsf,lsfIndex + 10*k, A,40);    		      		      		 
    	  }

    	  System.arraycopy(lpcBuffer, encoderState.SIZE, lpcBuffer, 0, 300-encoderState.SIZE);    	  
    }
    
    private void autoCorrelation(short[] input,int inputIndex,int inputLength, int order, int[] result,int resultIndex)
    {
    	if (order < 0)
            order = inputLength;

        // Find the max. sample
    	max=0;
    	tempIndex2=inputIndex;
    	for(i=0;i<inputLength;i++)
    	{
    		tempS=BasicFunctions.abs(input[tempIndex2++]);
    		if(tempS>max)
    			max=tempS;
    	}

    	// In order to avoid overflow when computing the sum we should scale the samples so that
        // (in_vector_length * smax * smax) will not overflow.
    	
        if (max == 0)        
        	scale = 0;
        else
        {        	
            nBits = BasicFunctions.getSize(inputLength); // # of bits in the sum loop
            tempS2 = BasicFunctions.norm(max*max); // # of bits to normalize max
                        
            if (tempS2 > nBits)
            	scale = 0;
            else
            	scale = (short)(nBits - tempS2);                        
        }
             
        // Perform the actual correlation calculation
        for (i = 0; i < order + 1; i++)
        {
            sum = 0;
            tempIndex2=inputIndex;
            tempIndex3=inputIndex+i;
            for (j = inputLength - i; j > 0; j--)
            {
            	tempShift=input[tempIndex2++]*input[tempIndex3++];            	
                sum += (tempShift>>scale);
            }
            
    		result[resultIndex++] = sum;
        }                  
    }

    private void windowMultiply(int[] output,int outputIndex,int[] input,int inputIndex,int[] window,int length)
    {
    	  nBits = BasicFunctions.norm(input[inputIndex]);
    	  BasicFunctions.bitShiftLeft(input, inputIndex, input, inputIndex,length, nBits);    	  		      	  
	    	  
    	  tempIndex2=outputIndex;
    	  /* The double precision numbers use a special representation:
    	   * w32 = hi<<16 + lo<<1
    	   */    	  
    	  for (i = 0; i < length; i++) {
    	    /* Extract higher bytes */
    	    xHi = (short)(input[inputIndex]>>16);
    	    yHi = (short)(window[i]>>16);

    	    /* Extract lower bytes, defined as (w32 - hi<<16)>>1 */
    	    tempShift=xHi<<16;
    	    xLow = (short)((input[inputIndex++] - tempShift)>>1);
    	    tempShift=yHi<<16;
    	    yLow = (short)((window[i] - tempShift)>>1);

    	    /* Calculate z by a 32 bit multiplication using both low and high from x and y */
    	    tempShift=(xHi*yHi)<<1;
    	    output[tempIndex2]=tempShift;
    	    tempShift=(xHi*yLow)>>14;
    	    output[tempIndex2]+=tempShift;
    	    tempShift=(xLow*yHi)>>14;    	    
    	    output[tempIndex2++]+=tempShift;    	        	    
    	  }    	      	  		  
		  
    	  BasicFunctions.bitShiftRight(output, outputIndex, output, outputIndex,length, nBits);    	  
    }
    

    private boolean levinsonDurbin(int[] R,int rIndex,short[] A,int aIndex,short[] K,int kIndex,int order)
    {
    	short[] rHi=tempMemory; //+ 301 length 21
    	short[] rLow=tempMemory; //+ 322 length 21
    	short[] aHi=tempMemory; //+ 343 length 21
    	short[] aLow=tempMemory; //+ 364 length 21
    	short[] aUpdHi=tempMemory; //+ 385 length 21
    	short[] aUpdLow=tempMemory; //+ 406 length 21
    	
        nBits = BasicFunctions.norm(R[rIndex]);
        tempIndex1=rIndex+order;
        tempIndex2=301+order;
        tempIndex3=322+order;
        for (i = order; i >= 0; i--)
        {
            temp = R[tempIndex1--]<<nBits;
            // Put R in hi and low format
            rHi[tempIndex2] = (short)(temp>>16);
            tempShift=rHi[tempIndex2--]<<16;
            rLow[tempIndex3--] = (short)((temp - tempShift)>>1);
        }

        // K = A[1] = -R[1] / R[0]

        temp2 = rHi[302]<<16; 
        temp3 = rLow[323]<<1; // R[1] in Q31
        temp2 += temp3;
        
        if(temp2>0)
        	temp3=temp2;
        else
        	temp3=-temp2;                        
        
        temp = BasicFunctions.div(temp3, rHi[301], rLow[322]); // abs(R[1])/R[0] in Q31        
        // Put back the sign on R[1]        
        if (temp2 > 0)
            temp = -temp;                
        
        // Put K in hi and low format
        xHi = (short)(temp>>16);
        tempShift=xHi<<16;
        xLow = (short)((temp - tempShift)>>1);

        // Store first reflection coefficient
        K[kIndex++] = xHi;
        
        temp = temp>>4; // A[1] in Q27
    	       	
        // Put A[1] in hi and low format
        aHi[344] = (short)(temp>>16);
        tempShift=aHi[344]<<16;
        aLow[365] = (short)((temp - tempShift)>>1);
        // Alpha = R[0] * (1-K^2)

        tempShift=(xHi*xLow) >> 14;
    	temp=tempShift;
    	tempShift=xHi*xHi;
    	temp+=tempShift;
    	temp<<=1;//temp = k^2 in Q31    	
    	
        if(temp<0)
        	temp=0-temp;
                
        temp = Integer.MAX_VALUE - temp; // temp = (1 - K[0]*K[0]) in Q31
        
        // Store temp = 1 - K[0]*K[0] on hi and low format
        tempS = (short)(temp>>16);
        tempShift=tempS<<16;
        tempS2 = (short)((temp - tempShift)>>1);

        // Calculate Alpha in Q31
        tempShift=rHi[301]*tempS;
        temp=tempShift;
        tempShift=(rHi[301]*tempS2) >> 15;
        temp+=tempShift;
        tempShift=(rLow[322]*tempS) >> 15;
        temp+=tempShift;
        temp <<= 1;

        // Normalize Alpha and put it in hi and low format        
        alphaExp = BasicFunctions.norm(temp);
        temp = temp<<alphaExp;
        yHi = (short)(temp>>16);
        tempShift=yHi<<16;
        yLow = (short)((temp - tempShift)>>1);
        
        // Perform the iterative calculations in the Levinson-Durbin algorithm
        for (i = 2; i <= order; i++)
        {
            /*                    ----
             temp =  R[i] + > R[j]*A[i-j]
             /
             ----
             j=1..i-1
             */

            temp = 0;
            tempIndex1=302;
            tempIndex3=323; 
            tempIndex4=363+i;
            tempIndex5=342+i;
            for (j = 1; j < i; j++)
            {
                // temp is in Q31
            	tempShift=(rHi[tempIndex1]*aLow[tempIndex4--]) >> 15;
            	temp2=tempShift;
            	tempShift=(rLow[tempIndex3++]*aHi[tempIndex5]) >> 15;
        		temp2+=tempShift;
        		temp+=temp2<<1;        		
            	tempShift=(rHi[tempIndex1++]*aHi[tempIndex5--]) << 1;            	
            	temp+=tempShift;            	            
            }

            temp = temp<<4;
            tempShift=rHi[tempIndex1]<<16;
            temp+=tempShift;
            tempShift=rLow[tempIndex3]<<1;
            temp += tempShift;            
            
            // K = -temp / Alpha            
            temp2 = Math.abs(temp); // abs(temp)            
            
            temp3 = BasicFunctions.div(temp2, yHi, yLow); // abs(temp)/Alpha            
            
            // Put the sign of temp back again
            if (temp > 0)
                temp3 = -temp3;
            
            // Use the Alpha shifts from earlier to de-normalize
            nBits = BasicFunctions.norm(temp3);
            
            if (alphaExp <= nBits || temp3 == 0)
                temp3 = temp3<<alphaExp;
            else
            {
                if (temp3 > 0)
                    temp3 = Integer.MAX_VALUE;
                else
                    temp3 = Integer.MIN_VALUE;                
            }
               
            // Put K on hi and low format
            xHi = (short)(temp3>>16);
            tempShift=xHi<<16;
            xLow = (short)((temp3 - tempShift)>>1);

            // Store Reflection coefficient in Q15
            K[kIndex++] = xHi;
            
            // Test for unstable filter.
            // If unstable return 0 and let the user decide what to do in that case
           
            if (BasicFunctions.abs(xHi) > 32750)
            	return false; // Unstable filter
            
            /*
             Compute updated LPC coefficient: Anew[i]
             Anew[j]= A[j] + K*A[i-j]   for j=1..i-1
             Anew[i]= K
             */            
            
            //set indexes to 1
            tempIndex4=363+i;
            tempIndex5=342+i;
            tempIndex6=386;
            tempIndex7=407;
            tempIndex8=344;
            tempIndex9=365;
            	
            for (j = 1; j < i; j++)
            {
                // temp = A[j] in Q27
            	tempShift=aHi[tempIndex8++]<<16;
            	temp=tempShift;
            	tempShift=aLow[tempIndex9++]<<1;
                temp += tempShift;
                // temp += K*A[i-j] in Q27
                tempShift=(xLow*aHi[tempIndex5]) >> 15;
            	temp2=tempShift;
            	tempShift=(xHi*aLow[tempIndex4--]) >> 15;
            	temp2+=tempShift;
            	tempShift=xHi*aHi[tempIndex5--];
            	temp2+=tempShift;            	
                temp += temp2 << 1;
                
                // Put Anew in hi and low format                
                aUpdHi[tempIndex6] = (short)(temp>>16);
                tempShift=aUpdHi[tempIndex6++]<<16;
                aUpdLow[tempIndex7++] = (short)((temp - tempShift)>>1);
            }

            // temp3 = K in Q27 (Convert from Q31 to Q27)
            temp3 = temp3>>4;
            
            
            // Store Anew in hi and low format
            aUpdHi[tempIndex6] = (short)(temp3>>16);
            tempShift=aUpdHi[tempIndex6]<<16;
            aUpdLow[tempIndex7] = (short)((temp3 - tempShift)>>1);

            // Alpha = Alpha * (1-K^2)
            tempShift=(xHi*xLow) >> 14;
            tempShift+=xHi*xHi;
            temp = tempShift << 1; // K*K in Q31
            if(temp<0)
            	temp = 0-temp; // Guard against <0
            
            temp = Integer.MAX_VALUE - temp; // 1 - K*K  in Q31            
            
            // Convert 1- K^2 in hi and low format
            tempS = (short)(temp>>16);
            tempShift=tempS<<16;
            tempS2 = (short)((temp - tempShift)>>1);
            
            // Calculate Alpha = Alpha * (1-K^2) in Q31
            tempShift=(yHi*tempS2) >> 15;
            temp2=tempShift;
            tempShift=(yLow*tempS) >> 15;
        	temp2+=tempShift;        	
        	tempShift=yHi*tempS;
            temp2+=tempShift;
            temp=temp2<<1;
            
            // Normalize Alpha and store it on hi and low format

            nBits = BasicFunctions.norm(temp);
            temp = temp<<nBits;            
            
            yHi = (short)(temp>>16);
            tempShift=yHi<<16;
            yLow = (short)((temp - tempShift)>>1);

            // Update the total normalization of Alpha
            alphaExp = (short)(alphaExp + nBits);
            
            // Update A[]
            tempIndex4=365;
            tempIndex5=344;
            tempIndex6=386;
            tempIndex7=407;            
            for (j = 1; j <= i; j++)
            {
            	aHi[tempIndex5++] = aUpdHi[tempIndex6++];
                aLow[tempIndex4++] = aUpdLow[tempIndex7++];                
            }                                   
        }

        /*
         Set A[0] to 1.0 and store the A[i] i=1...order in Q12
         (Convert from Q27 and use rounding)
         */
        tempIndex1=aIndex;
        A[tempIndex1++] = 4096;      
        
        tempIndex5=344;
        tempIndex4=365;
        for (i = 1; i <= order; i++)
        {
            // temp in Q27
        	tempShift=aHi[tempIndex5++]<<16;
        	temp=tempShift;
        	tempShift=aLow[tempIndex4++]<<1;
            temp +=  tempShift;
            temp<<=1;
            temp+=32768;
            // Round and store upper word            
            A[tempIndex1++] = (short)(temp>>16);
        }
        
        return true; // Stable filters
    }

    private void poly2Lsf(short[] lsf,int lsfIndex,short[] A,int aIndex)
    {
    	short[] lsp=tempMemory; //+ 301 length 10
    	poly2Lsp(A, aIndex, lsp, 301);    	
    	lspToLsf(lsp, 301,lsf, lsfIndex, 10);    	        	
    }
    
    private void poly2Lsp(short[] A,int aIndex,short[] lsp,int lspIndex)
    {
    	/* f[0][] represents f1 and f[1][] represents f2 */
    	short[] f=tempMemory; //+311 length 12 (2*6) 
    	
    	/*
    	  Calculate the two polynomials f1(z) and f2(z)
    	  (the sum and the diff polynomial)
    	  f1[0] = f2[0] = 1.0;
    	  f1[i+1] = a[i+1] + a[10-i] - f1[i];
    	  f2[i+1] = a[i+1] - a[10-i] - f1[i];
    	*/

    	tempIndex1 = aIndex + 1;
    	tempIndex2 = aIndex+10;
    	
    	tempIndex3=311;
    	tempIndex4=317;    	  
    	f[tempIndex3]=1024;
    	f[tempIndex4]=1024;
    	
    	for (i = 0; i < 5; i++) 
    	{
    		tempShift=A[tempIndex1] + A[tempIndex2];     		
    		tempShift>>=2;    	
    	    f[tempIndex3+1] = (short)(tempShift - f[tempIndex3]);
    	    
    	    tempShift=A[tempIndex1] - A[tempIndex2];     		
    	    tempShift>>=2;
        	f[tempIndex4+1] = (short)(tempShift + f[tempIndex4]);        	        	
        	
        	tempIndex1++;
        	tempIndex2--;
        	tempIndex3++;
        	tempIndex4++;
    	}

    	/*
    	  find the LSPs using the Chebychev pol. evaluation
    	*/

    	tempIndex1 = 311; /* selector between f1 and f2, start with f1 */
    	tempIndex2 = lspIndex;
    	foundFreqs = 0;

    	xLow = Constants.COS_GRID[0];
    	yLow = chebushev(xLow, f, 311);
    	/*
    	   Iterate until all the 10 LSP's have been found or
    	   all the grid points have been tried. If the 10 LSP's can
    	   not be found, set the LSP vector to previous LSP
    	*/
    	for (j = 1; j < Constants.COS_GRID.length && foundFreqs < 10; j++) 
    	{
    	    xHi = xLow;
    	    yHi = yLow;
    	    xLow = Constants.COS_GRID[j];
    	    yLow = chebushev(xLow, f, tempIndex1);    	    
    	    
    	    if (yLow*yHi <= 0) 
    	    {
    	    	/* Run 4 times to reduce the interval */
    	    	for (i = 0; i < 4; i++) 
    	    	{
    	    		/* xmid =(xLow + xHi)/2 */    	    		
    	    		xMid = (short)((xLow>>1) + (xHi>>1));
    	    		yMid = chebushev(xMid, f, tempIndex1);
    	    		
    	    		if (yLow*yMid <= 0) 
    	    		{
    	    			yHi = yMid;
    	    			xHi = xMid;
    	    		} 
    	    		else 
    	    		{
    	    			yLow = yMid;
    	    			xLow = xMid;
    	    		}
    	    	}

    	    	/*
    	        	Calculater xint by linear interpolation:
    	        	xint = xLow - yLow*(xHi-xLow)/(yHi-yLow);
    	    	*/

    	    	x = (short)(xHi - xLow);
    	    	y = (short)(yHi - yLow);
    	    	
    	    	if (y == 0)
    	    		lsp[tempIndex2++] = xLow;
    	      	else 
    	      	{
    	      		temp2 = y;
    	      		y = BasicFunctions.abs(y);
    	      		nBits = (short)(BasicFunctions.norm(y)-16);
    	      		y = (short)(y<<nBits);
    	      		
    	      		if (y != 0)
    	      	        y=(short)(536838144 / y);
    	      	    else
    	      	    	y=(short)Integer.MAX_VALUE;    	      	    

    	      		tempShift=x*y;
    	      		temp= tempShift>>(19-nBits);    	      		

    	      		/* y=(xHi-xLow)/(yHi-yLow) */
    	      		y = (short)(temp&0xFFFF);

    	      		if (temp2 < 0)
    	      			y = (short)-y;    	        
    	        
    	      		/* temp = yLow*(xHi-xLow)/(yHi-yLow) */
    	      		temp = (yLow*y)>>10;    	      		
    	      		
    	      		lsp[tempIndex2++] = (short)(xLow-temp&0xFFFF);
    	      	}

    	    	/* Store the calculated lsp */
    	    	foundFreqs++;

    	    	/* if needed, set xLow and yLow for next recursion */
    	    	if (foundFreqs<10) 
    	    	{
    	    		xLow = lsp[tempIndex2-1];
    	    		/* Swap between f1 and f2 (f[0][] and f[1][]) */
    	    		if(tempIndex1==311)
    	    			tempIndex1=317;
    	    		else
    	    			tempIndex1=311;    	        

    	    		yLow = chebushev(xLow, f, tempIndex1);
    	    	}
    	    }
    	}

    	/* Check if M roots found, if not then use the old LSP */
    	if (foundFreqs < 10)   
    		System.arraycopy(Constants.LSP_MEAN, 0, lsp, lspIndex, 10);    	
    }
    
    private void lspToLsf(short[] lsp, int lspIndex,short[] lsf,int lsfIndex,int coefsNumber)
    {
      	/* set the index to maximum index value in COS */
    	j = 63;

    	/*
    	  Start with the highest LSP and then work the way down
    	  For each LSP the lsf is calculated by first order approximation
    	  of the acos(x) function
    	*/
    	tempIndex1=lspIndex+coefsNumber-1;  
    	tempIndex2=lsfIndex+coefsNumber-1;    	
    	    	
    	for(i=coefsNumber-1; i>=0; i--)
    	{
    	    /*
    	       locate value in the table, which is just above lsp[i],
    	       basically an approximation to acos(x)
    	    */
    		while(Constants.COS[j]<lsp[tempIndex1] && j>0)
    			j--;
    	    
    		/* Calculate diff, which is used in the linear approximation of acos(x) */
    	    diff = (short)(lsp[tempIndex1--]-Constants.COS[j]);

    	    /*
    	       The linear approximation of acos(lsp[i]) :
    	       acos(lsp[i])= k*512 + (WebRtcIlbcfix_kAcosDerivative[ind]*offset >> 11)
    	    */

    	    /* tempS (linear offset) in Q16 */
    	    tempShift=Constants.ACOS_DERIVATIVE[j]*diff;
    	    tempS = (short)(tempShift>>11);

    	    /* freq in Q16 */
    	    tempShift=j<<9;
    	    tempS2 = (short)(tempShift+tempS);

    	    /* lsf = freq*2*pi */
    	    tempShift=tempS2*25736;
    	    lsf[tempIndex2--] = (short)(tempShift>>15);    	        		    	        	        	   
    	  }
    }
    
    private short chebushev(short value,short[] coefs,int coefsIndex)
    {    	    
    	b2 = 0x1000000; /* b2 = 1.0 (Q23) */
    	/* Calculate b1 = 2*x + f[1] */
    	temp3 = value<<10;
    	coefsIndex++;
    	temp3 += coefs[coefsIndex++]<<14;
    	
    	for (n = 2; n < 5; n++) 
    	{
    	    temp4 = temp3;

    	    /* Split b1 (in temp3) into a high and low part */
    	    b1Hi = (short)(temp3>>16);
    	    tempShift=b1Hi<<16;
    	    b1Low = (short)((temp3-tempShift)>>1);    	    
    	    
    	    /* Calculate 2*x*b1-b2+f[i] */
    	    tempShift=(b1Low*value)>>15;
    		temp3=tempShift;
    		tempShift=b1Hi*value;
    		temp3+=tempShift;
    	    temp3 <<=2;

    	    temp3 -= b2;
    	    temp3 += coefs[coefsIndex++]<<14;

    	    /* Update b2 for next round */
    	    b2 = temp4;    	        	    
    	}

    	/* Split b1 (in temp3) into a high and low part */
    	b1Hi = (short)(temp3>>16);
    	tempShift=b1Hi<<16;
    	b1Low = (short)((temp3-tempShift)>>1);

    	/* temp3 = x*b1 - b2 + f[i]/2 */
    	tempShift=(b1Low*value)>>15;
    	temp3=tempShift<<1;
    	tempShift=(b1Hi*value)<<1;
    	temp3+=tempShift;
    	
    	temp3 -= b2;
    	temp3 += coefs[coefsIndex]<<13;
    	
    	/* Handle overflows and set to maximum or minimum WebRtc_Word16 instead */
    	if (temp3>33553408) 
    	  	return Short.MAX_VALUE;
    	else if (temp3<-33554432)
    		return Short.MIN_VALUE;
    	else
    		return (short)(temp3>>10);    	  
    }

    private void simpleLsfQ(short[] lsfdeq,int lsfdeqIndex,short[] lsfArray,int lsfArrrayIndex)
    {
    	/* Quantize first LSF with memoryless split VQ */
    	splitVq(lsfdeq, lsfdeqIndex,encoderBits.getLSF(), 0, lsfArray, lsfArrrayIndex);
    	
    	if (encoderState.LPC_N==2) 
    	{
    	    /* Quantize second LSF with memoryless split VQ */
    		splitVq(lsfdeq, lsfdeqIndex + 10, encoderBits.getLSF(), 3, lsfArray, lsfArrrayIndex + 10);    	    
    	}
    }
    
    private void splitVq(short[] qX,int qXIndex,short[] lsf,int lsfIndex,short[] X,int xIndex)
    {
    	/* Quantize X with the 3 vectror quantization tables */
    	vq3(qX, qXIndex, lsf, lsfIndex, Constants.LSF_INDEX_CB[0], X, xIndex, Constants.LSF_SIZE_CB[0]);

    	vq3(qX, qXIndex + Constants.LSF_DIM_CB[0], lsf, lsfIndex+1, Constants.LSF_INDEX_CB[1], X, xIndex + Constants.LSF_DIM_CB[0], Constants.LSF_SIZE_CB[1]);    	
    	
    	vq4(qX, qXIndex + Constants.LSF_DIM_CB[0] + Constants.LSF_DIM_CB[1], lsf, lsfIndex+2, Constants.LSF_INDEX_CB[2], X, xIndex + Constants.LSF_DIM_CB[0] + Constants.LSF_DIM_CB[1], Constants.LSF_SIZE_CB[2]);
    }
    
    private void vq3(short[] qX,int qXIndex,short[] lsf,int lsfIndex,int cbIndex,short[] X,int xIndex,int cbSize)
    {
    	minValue = Integer.MAX_VALUE; /* start value */

    	tempIndex2=cbIndex;
    	tempIndex5=0;
    	/* Find the codebook with the lowest square distance */
    	for (j = 0; j < cbSize; j++) 
    	{
    		tempIndex3=xIndex;
    		tempS = (short)(X[tempIndex3++] - Constants.LSF_CB[tempIndex2++]);
    	    temp = tempS*tempS;
    	    tempS = (short)(X[tempIndex3++] - Constants.LSF_CB[tempIndex2++]);
    	    tempShift=tempS*tempS;
	    	temp += tempShift;	    	
	    	tempS = (short)(X[tempIndex3++] - Constants.LSF_CB[tempIndex2++]);
	    	tempShift=tempS*tempS;
	    	temp += tempShift;	    	    	        	   

    	    if (temp < minValue) 
    	    {
    	    	minValue = temp;
    	    	tempIndex5=j;    	    	    	    	
    	    }    	        	   
    	}

    	tempIndex1=qXIndex;
    	
    	/* Store the quantized codebook and the index */
    	lsf[lsfIndex] = (short)tempIndex5;
    	tempIndex5*=3;
    	tempIndex5+=cbIndex;
    	for (i = 0; i < 3; i++)
    	    qX[tempIndex1++] = Constants.LSF_CB[tempIndex5++];    	      	      	
    }
    
    private void vq4(short[] qX,int qXIndex,short[] lsf,int lsfIndex,int cbIndex,short[] X,int xIndex,int cbSize)
    {
    	minValue = Integer.MAX_VALUE; /* start value */

    	tempIndex2=cbIndex;
    	tempIndex5=0;
    	/* Find the codebook with the lowest square distance */
    	for (j = 0; j < cbSize; j++) 
    	{
    		tempIndex3=xIndex;
    		tempS = (short)(X[tempIndex3++] - Constants.LSF_CB[tempIndex2++]);
    	    temp = tempS*tempS;
    	    for (i = 1; i < 4; i++) 
    	    {
    	    	tempS = (short)(X[tempIndex3++] - Constants.LSF_CB[tempIndex2++]);
    	    	temp += tempS*tempS;
    	    }

    	    if (temp < minValue) 
    	    {
    	    	minValue = temp;
    	    	tempIndex5=j;
    	    }    	        	   
    	}

    	tempIndex1=qXIndex;    	

    	/* Store the quantized codebook and the index */
      	lsf[lsfIndex] = (short)tempIndex5;
      	tempIndex5*=4;
      	tempIndex5+=cbIndex;
      	for (i = 0; i < 4; i++)
      	    qX[tempIndex1++] = Constants.LSF_CB[tempIndex5++];    	
    }

    private void lsfCheck(short[] lsf,int lsfIndex,int lsfSize)
    {
    	/* LSF separation check*/
    	for (n=0;n<2;n++) 
    	{
    		/* Run through a 2 times */
    	    for (j=0;j<encoderState.LPC_N;j++) 
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

    private void simpleInterpolateLsf(short[] synthdenum,int synthDenumIndex,short[] weightDenum,int weightDenumIndex,short[] lsf,int lsfIndex,short[] lsfDeq,int lsfDeqIndex,int length)
    {
    	short[] lsfOld=encoderState.getLsfOld();
    	short[] lsfDeqOld=encoderState.getLsfDeqOld();
    	
    	short[] lp=tempMemory; //+40 length 11
    	
    	tempIndex1=lsfIndex+length;
    	tempIndex2=lsfDeqIndex+length;
    	
    	temp=length + 1;    	
    	if (encoderState.ENCODER_MODE==30) 
    	{
    	    /* subframe 1: Interpolation between old and first set of
    	       lsf coefficients */

    	    /* Calculate Analysis/Syntehsis filter from quantized LSF */
    		lsfInterpolate2PolyEnc(lp, (short)40 , lsfDeqOld, 0, lsfDeq, lsfDeqIndex, Constants.LSF_WEIGHT_30MS[0], length);    		
    		System.arraycopy(lp, 40, synthdenum, synthDenumIndex, temp);    	    

    	    /* Calculate Weighting filter from quantized LSF */
    	    lsfInterpolate2PolyEnc(lp, (short)40, lsfOld, 0, lsf, lsfIndex ,Constants.LSF_WEIGHT_30MS[0], length);
    	    BasicFunctions.expand(weightDenum,weightDenumIndex,lp,40,Constants.LPC_CHIRP_WEIGHT_DENUM,temp);
    	    
    	    /* subframe 2 to 6: Interpolation between first and second
    	       set of lsf coefficients */

    	    index = temp;
    	    for (i = 1; i < encoderState.SUBFRAMES; i++) 
    	    {
    	    	/* Calculate Analysis/Syntehsis filter from quantized LSF */
    	    	lsfInterpolate2PolyEnc(lp, (short)40 ,lsfDeq, lsfDeqIndex, lsfDeq, tempIndex2 ,Constants.LSF_WEIGHT_30MS[i], length);
    	    	System.arraycopy(lp, 40, synthdenum, synthDenumIndex + index, temp);    	      

    	    	/* Calculate Weighting filter from quantized LSF */
    	    	lsfInterpolate2PolyEnc(lp, (short)40, lsf, lsfIndex, lsf, tempIndex1 ,Constants.LSF_WEIGHT_30MS[i], length);
    	    	BasicFunctions.expand(weightDenum,weightDenumIndex + index,lp,40,Constants.LPC_CHIRP_WEIGHT_DENUM,temp);   	    	

    	    	index += temp;
    	    }

    	    /* update memory */
    	    System.arraycopy(lsf, tempIndex1, lsfOld, 0, length);
    	    System.arraycopy(lsfDeq, tempIndex2, lsfDeqOld, 0, length);
    	}
    	else 
    	{ 
    		/* encoderState.ENCODER_MODE==20 */
    		index = 0;
    	    for (i = 0; i < encoderState.SUBFRAMES; i++) 
    	    {
    	    	/* Calculate Analysis/Syntehsis filter from quantized LSF */
    	    	lsfInterpolate2PolyEnc(lp, (short)40 ,lsfDeqOld, 0, lsfDeq, lsfDeqIndex , Constants.LSF_WEIGHT_20MS[i], length);    	    	
    	    	System.arraycopy(lp, 40, synthdenum, synthDenumIndex + index, temp);    	      

    	    	/* Calculate Weighting filter from quantized LSF */
    	    	lsfInterpolate2PolyEnc(lp, (short)40 , lsfOld, 0, lsf, lsfIndex, Constants.LSF_WEIGHT_20MS[i], length);
    	    	BasicFunctions.expand(weightDenum,weightDenumIndex + index,lp,40,Constants.LPC_CHIRP_WEIGHT_DENUM,temp);    	      

    	    	index += temp;
    	    }

    	    /* update memory */
    	    System.arraycopy(lsf, lsfIndex, lsfOld, 0, length);
    	    System.arraycopy(lsfDeq, lsfDeqIndex, lsfDeqOld, 0, length);  	    
    	}
    }
    
    
    private void lsfInterpolate2PolyEnc(short[] a, short aIndex, short[] lsf1, int lsf1Index, short[] lsf2, int lsf2Index, short coef, int length)
    {
    	/* Stack based */
    	short[] lsfTemp=tempMemory; //+ 51 length 10    	
    	
    	/* interpolate LSF */
    	interpolate(lsfTemp, 51, lsf1, lsf1Index, lsf2, lsf2Index, coef, length);
    	
    	/* Compute the filter coefficients from the LSF */
    	lsf2Poly(a, aIndex, lsfTemp, 51);
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

    private short frameClassify(short[] residual)
    {
    	int[] ssqEn=tempLMemory; //+ 0 length 5
    	
    	/*
    	  Calculate the energy of each of the 80 sample blocks
    	  in the draft the 4 first and last samples are windowed with 1/5...4/5
    	  and 4/5...1/5 respectively. To simplify for the fixpoint we have changed
    	  this to 0 0 1 1 and 1 1 0 0
    	*/

    	max=0;    	
    	for(n=0;n<encoderState.SIZE;n++)
    	{
    		tempS=residual[n];
    		if(tempS<0)
    			tempS=(short)(0-tempS);
    		
    		if(tempS>max)
    			max=tempS;    		
    	}
    	
    	/* Scale to maximum 24 bits so that it won't overflow for 76 samples */
    	scale=(short)(BasicFunctions.getSize(max*max)-24);
    	if(scale<0)
    		scale=0;    	

    	/* Calculate energies */
    	tempIndex1=2;
    	tempIndex2=0;    	
    	for (n=encoderState.SUBFRAMES-1; n>0; n--) 
    	{
    		ssqEn[tempIndex2++] = BasicFunctions.scaleRight(residual,tempIndex1, residual,tempIndex1, 76, scale);
    	    tempIndex1 += 40;    	    
    	}

    	/* Scale to maximum 20 bits in order to allow for the 11 bit window */
    	tempIndex2=0;
    	temp2=ssqEn[tempIndex2++];	
    	for(n=1;n<encoderState.SUBFRAMES-1;n++)
    	{
    		if(ssqEn[tempIndex2]>temp2)
    			temp2=ssqEn[tempIndex2];
    		
    		tempIndex2++;
    	}
    	
    	scale = (short)(BasicFunctions.getSize(temp2) - 20);
    	if(scale<0)
    		scale = 0;

    	/* Window each 80 block with the ssqEn_winTbl window to give higher probability for
    	   the blocks in the middle
    	*/
    	tempIndex2=0;    	
    	if (encoderState.ENCODER_MODE==20)
    		tempIndex1=1;    	    
    	else
    		tempIndex1=0;
    	
    	
    	for (n=encoderState.SUBFRAMES-1; n>0; n--) 
    	{
    		ssqEn[tempIndex2]=(ssqEn[tempIndex2]>>scale)*Constants.ENG_START_SEQUENCE[tempIndex1++];
    	    tempIndex2++;    	    
    	}

    	/* Extract the best choise of start state */
    	tempIndex1=0;
    	tempIndex2=0;
    	temp2=ssqEn[tempIndex2++];	
    	for(n=1;n<encoderState.SUBFRAMES-1;n++)
    	{
    		if(ssqEn[tempIndex2]>temp2)
    		{
    			tempIndex1=tempIndex2;
    			temp2=ssqEn[tempIndex2];    			
    		}
    		
    		tempIndex2++;
    	}
    	
    	return (short)(tempIndex1 + 1);
    }

    private void stateSearch(short[] residual,int residualIndex,short[] syntDenum,int syntIndex,short[] weightDenum,int weightIndex)
    {
    	/* Stack based */
    	short[] numerator=tempMemory; //+ 0 length 11
        short[] residualLongVec=tempMemory; //+ 11 length 126    	  
    	short[] sampleMa=tempMemory; //+ 137 length 116
    	short[] residualLong=tempMemory; //+ 21 length 116 
    	short[] sampleAr=tempMemory; //+ 21 length 116

    	/* Scale to maximum 12 bits to avoid saturation in circular convolution filter */
    	tempIndex1=residualIndex;
    	max=0;    	
    	for(n=0;n<encoderState.STATE_SHORT_LEN;n++)
    	{
    		tempS=residual[tempIndex1++];
    		if(tempS<0)
    			tempS=(short)(0-tempS);
    		
    		if(tempS>max)
    			max=tempS;    		
    	}
    	
    	tempS = (short)(BasicFunctions.getSize(max)-12);
    	if(tempS<0)
    		tempS = 0;
    	      	
    	/* Set up the filter coefficients for the circular convolution */
    	tempIndex1=syntIndex+10;
    	for (i=0; i<11; i++)
    	   numerator[i] = (short)(syntDenum[tempIndex1--]>>tempS);    	

    	/* Copy the residual to a temporary buffer that we can filter
    	* and set the remaining samples to zero.
    	*/    	
    	System.arraycopy(residual, residualIndex, residualLong, 21, encoderState.STATE_SHORT_LEN);
    	tempIndex1=21+encoderState.STATE_SHORT_LEN;
    	for(i=0;i<encoderState.STATE_SHORT_LEN;i++)
    		residualLong[tempIndex1++]=0;    	  

    	/* Run the Zero-Pole filter (Ciurcular convolution) */
    	tempIndex1=11;
    	for(i=0;i<10;i++)
    		residualLongVec[tempIndex1++]=0;
    	
    	BasicFunctions.filterMA(residualLong,21,sampleMa,137,numerator,0,11,encoderState.STATE_SHORT_LEN+10);
    	
    	tempIndex1=137 + encoderState.STATE_SHORT_LEN + 10;
    	for(i=0;i<encoderState.STATE_SHORT_LEN-10;i++)
    		sampleMa[tempIndex1++]=0;    	

    	BasicFunctions.filterAR(sampleMa,137,sampleAr,21,syntDenum,syntIndex,11,2*encoderState.STATE_SHORT_LEN);
    	
    	tempIndex1=21;
    	tempIndex2=21+encoderState.STATE_SHORT_LEN;
    	for(i=0;i<encoderState.STATE_SHORT_LEN;i++)
    	    sampleAr[tempIndex1++] += sampleAr[tempIndex2++];    	  

    	/* Find maximum absolute value in the vector */
    	max=0;    	
    	tempIndex1=21;
    	for(n=0;n<encoderState.STATE_SHORT_LEN;n++)
    	{
    		tempS2=sampleAr[tempIndex1++];
    		if(tempS2<0)
    			tempS2=(short)(0-tempS2);
    		
    		if(tempS2>max)
    			max=tempS2;    		
    	}    	
    	
    	/* Find the best index */
    	if ((max<<tempS)<23170)
    	    temp2=(max*max)<<(2+2*tempS);
    	else
    		temp2=Integer.MAX_VALUE;    	  

    	index=0;
    	for (i=0;i<63;i++) 
    	{
    	    if (temp2>=Constants.CHOOSE_FRG_QUANT[i])
    	    	index=i+1;
    	    else
    	    	i=63;    	    
    	}
    	
    	encoderBits.setIdxForMax((short)index);    	

    	/* Rescale the vector before quantization */
    	scale=Constants.SCALE[index];

    	if (index<27) /* scale table is in Q16, fout[] is in Q(-1) and we want the result to be in Q11 */
    	    nBits=4;
    	else /* scale table is in Q21, fout[] is in Q(-1) and we want the result to be in Q11 */
    		nBits=9;    	  

    	/* Set up vectors for AbsQuant and rescale it with the scale factor */
    	BasicFunctions.scaleVector(sampleAr, 21, sampleAr, 21, scale, encoderState.STATE_SHORT_LEN, nBits-tempS);
    	
    	/* Quantize the values in fout[] */
    	absQuant(sampleAr, 21, weightDenum, weightIndex);
    	
    	
    }
    
    private void absQuant(short[] in,int inIndex,short[] weightDenum, int weightDenumIndex)
    {
    	/* Stack based */
    	short[] quantLen=tempMemory; //+253 length 2
    	short[] syntOutBuf=tempMemory; //+255 length 68
    	short[] inWeightedVec=tempMemory; //+323 length 68
    	short[] inWeighted=tempMemory; //+333 length 58    	

    	/* Initialize the buffers */
    	tempIndex1=255;
    	for(i=0;i<68;i++)
    		syntOutBuf[tempIndex1++]=0;
    	
    	tempIndex1 = 265;
    	/* Start with zero state */
    	tempIndex2=323;
    	for(i=0;i<10;i++)
    		inWeightedVec[tempIndex2++]=0;    	

    	/* Perform the quantization loop in two sections of length quantLen[i],
    	where the perceptual weighting filter is updated at the subframe
    	border */
    	
    	if (encoderBits.getStateFirst()) 
    	{
    	    quantLen[253]=40;
    	    quantLen[254]=(short)(encoderState.STATE_SHORT_LEN-40);
    	} 
    	else 
    	{
    	    quantLen[253]=(short)(encoderState.STATE_SHORT_LEN-40);
    	    quantLen[254]=40;
    	}

    	/* Calculate the weighted residual, switch perceptual weighting
    	filter at the subframe border */
    	BasicFunctions.filterAR(in, inIndex, inWeighted, 333, weightDenum, weightDenumIndex, 11, quantLen[253]);
    	BasicFunctions.filterAR(in, inIndex+quantLen[253], inWeighted, 333+quantLen[253], weightDenum, weightDenumIndex+11, 11, quantLen[254]);    	

    	absQUantLoop(syntOutBuf, tempIndex1, inWeighted, 333,weightDenum, weightDenumIndex, quantLen, 253);
    }
    
    private void absQUantLoop(short[] syntOut, int syntOutIndex, short[] inWeighted, int inWeightedIndex, short[] weightDenum, int weightDenumIndex, short[] quantLen, int quantLenIndex)
    {
    	short[] idxVec=encoderBits.getIdxVec();    	
    	int startIndex=0;
    	for(i=0;i<2;i++) 
    	{
    		tempIndex3=quantLenIndex+i;
    		
        	for(j=0;j<quantLen[tempIndex3];j++)
    	    {

    	    	/* Filter to get the predicted value */
    	    	BasicFunctions.filterAR(syntOut, syntOutIndex, syntOut, syntOutIndex, weightDenum, weightDenumIndex, 11, 1);    	    	

    	    	/* the quantizer */
    	    	temp = inWeighted[inWeightedIndex] - syntOut[syntOutIndex];
    	    	temp2 = temp<<2;
    	    	
    	    	if (temp2 > Short.MAX_VALUE)
    	    		temp2 = Short.MAX_VALUE;
    	    	else if (temp2 < Short.MIN_VALUE)
    	    		temp2 = Short.MIN_VALUE;    	      

    	    	/* Quantize the state */
    	    	if (temp< -7577) {
    	    		/* To prevent negative overflow */
    	    		index=0;
    	      	} else if (temp>8151) {
    	      		/* To prevent positive overflow */
    	      		index=7;
    	      	} 
    	      	else 
    	      	{
    	      		/* Find the best quantization index
    	           	(state_sq3Tbl is in Q13 and toQ is in Q11)
    	         	*/
    	      		if (temp2 <= Constants.STATE_SQ3[0]) 
    	      		    index = 0;    	      		    
    	      		else 
    	      		{
    	      			k = 0;
    	      		    while(temp2 > Constants.STATE_SQ3[k] && k<Constants.STATE_SQ3.length-1)
    	      		    	k++;

    	      		    tempShift=Constants.STATE_SQ3[k] + Constants.STATE_SQ3[k - 1] + 1;
    	      		    if (temp2 > (tempShift>>1)) 
    	      		    	index = k;
    	      		    else 
    	      		    	index = k - 1;    	      		    
    	      		}    	      		
    	      	}
    	    	
    	    	/* Store selected index */
    	    	idxVec[startIndex + j] = (short)index;

    	    	/* Compute decoded sample and update of the prediction filter */
    	    	tempShift=Constants.STATE_SQ3[index] + 2;
    	    	tempS = (short)(tempShift >> 2);

    	    	syntOut[syntOutIndex++] = (short)(tempS + inWeighted[inWeightedIndex++] - temp);    	    	
    	    }
    	    
        	startIndex+=quantLen[tempIndex3];
        	/* Update perceptual weighting filter at subframe border */        	
    	    weightDenumIndex += 11;
    	}
    }

    private void stateConstruct(short[] syntDenum,int syntDenumIndex,short[] outFix,int outFixIndex)    
    {
    	/* Stack based */
    	short[] numerator=tempMemory; //+0 length 11
    	short[] sampleValVec=tempMemory; //+11 length 126
    	short[] sampleMaVec=tempMemory; //+137 length 126
    	short[] sampleVal=tempMemory; //+21 length 116
    	short[] sampleMa=tempMemory; //+147 length 116;
    	short[] sampleAr=tempMemory; //+21 length 116;    	  
    	
    	short[] idxVec=encoderBits.getIdxVec();
    	/* initialization of coefficients */
    	tempIndex1=0;
    	tempIndex2=syntDenumIndex+10;
    	for (k=0; k<11; k++)
    	    numerator[tempIndex1++] = syntDenum[tempIndex2--];
    	
    	/* decoding of the maximum value */
    	max = Constants.FRQ_QUANT_MOD[encoderBits.getIdxForMax()];

    	/* decoding of the sample values */
    	tempIndex1=21;
    	tempIndex2 = encoderState.STATE_SHORT_LEN-1;

    	if (encoderBits.getIdxForMax()<37) 
    	{
    	    for(k=0; k<encoderState.STATE_SHORT_LEN; k++)
    	    {
    	      /*the shifting is due to the Q13 in sq4_fixQ13[i], also the adding of 2097152 (= 0.5 << 22)
    	      max is in Q8 and result is in Q(-1) */
    	      sampleVal[tempIndex1++]=(short) ((max*Constants.STATE_SQ3[idxVec[tempIndex2--]]+2097152) >> 22);    	      
    	    }
    	} 
    	else if (encoderBits.getIdxForMax()<59) 
    	{
    	    for(k=0; k<encoderState.STATE_SHORT_LEN; k++)
    	    {
    	      /*the shifting is due to the Q13 in sq4_fixQ13[i], also the adding of 262144 (= 0.5 << 19)
    	        max is in Q5 and result is in Q(-1) */
    	      sampleVal[tempIndex1++]=(short) ((max*Constants.STATE_SQ3[idxVec[tempIndex2--]]+262144) >> 19);    	      
    	    }
    	} 
    	else 
    	{
    	    for(k=0; k<encoderState.STATE_SHORT_LEN; k++)
    	    {
    	      /*the shifting is due to the Q13 in sq4_fixQ13[i], also the adding of 65536 (= 0.5 << 17)
    	        max is in Q3 and result is in Q(-1) */
    	      sampleVal[tempIndex1++]=(short) ((max*Constants.STATE_SQ3[idxVec[tempIndex2--]]+65536) >> 17);    	      
    	    }
    	}

    	/* Set the rest of the data to zero */
    	tempIndex1=21+encoderState.STATE_SHORT_LEN;
    	for(i=0;i<encoderState.STATE_SHORT_LEN;i++)
    		sampleVal[tempIndex1++]=0;    	

    	/* circular convolution with all-pass filter */

    	/* Set the state to zero */
    	tempIndex1=11;
    	for(i=0;i<10;i++)
    		sampleValVec[tempIndex1++]=0;    	

    	/* Run MA filter + AR filter */
    	BasicFunctions.filterMA(sampleVal, 21, sampleMa, 147, numerator, 0, 11, 11+encoderState.STATE_SHORT_LEN);    	
    	
    	tempIndex1=157+encoderState.STATE_SHORT_LEN;
    	for(i=0;i<encoderState.STATE_SHORT_LEN-10;i++)
    		sampleMa[tempIndex1++]=0;
    	
    	BasicFunctions.filterAR(sampleMa, 147, sampleAr, 21, syntDenum, syntDenumIndex, 11, 2*encoderState.STATE_SHORT_LEN);
    	
    	tempIndex1=21+encoderState.STATE_SHORT_LEN-1;
    	tempIndex2=21+2*encoderState.STATE_SHORT_LEN-1;
    	tempIndex3=outFixIndex;    	
    	for(k=0;k<encoderState.STATE_SHORT_LEN;k++)
    		outFix[tempIndex3++]=(short)(sampleAr[tempIndex1--]+sampleAr[tempIndex2--]);    	
    }

    private void cbSearch(short[] inTarget,int inTargetIndex,short[] decResidual,int decResidualIndex,int length,int vectorLength,short[] weightDenum,int weightDenumindex, int blockNumber,int cbIndexIndex,int gainIndexIndex)
    {
    	short[] cbIndex=encoderBits.getCbIndex();
    	short[] gainIndex=encoderBits.getGainIndex();
    	
    	/* Stack based */
    	short[] gains=tempMemory;//+0 length 4
    	short[] cbBuf=tempMemory;//+4 length 161   
    	short[] energyShifts=tempMemory;//+165 length 256
    	short[] targetVec=tempMemory;//+421 length 50
    	short[] cbVectors=tempMemory;//+471 length 147
    	short[] codedVec=tempMemory;//+618 length 40
    	short[] interpSamples=tempMemory;//+658 length 80
    	short[] interSamplesFilt=tempMemory;//+738 length 80
    	short[] energy=tempMemory;//+818 length 256    	
    	short[] augVec=tempMemory;//+1074 length 256
    	
    	short[] inverseEnergy=tempMemory;//+818 length 256
    	short[] inverseEnergyShifts=tempMemory;//+165 length 256
    	short[] buf=tempMemory;//+14 length 151
    	short[] target=tempMemory;//+431 length 40    	

    	int[] cDot=tempLMemory;//+0 length 128
    	int[] crit=tempLMemory;//+128 length 128
    	
    	Arrays.fill(tempLMemory,0,256,0);
    	Arrays.fill(tempMemory,0,1330,(short)0);
    	
    	/* Determine size of codebook sections */
    	baseSize=(short)(length-vectorLength+1);
    	if (vectorLength==40)
    	    baseSize=(short)(length-19);    	  

    	/* weighting of the CB memory */
    	temp=length-Constants.FILTER_RANGE[blockNumber];
    	
    	BasicFunctions.filterAR(decResidual, decResidualIndex + temp, buf, 14+temp, weightDenum, weightDenumindex, 11, Constants.FILTER_RANGE[blockNumber]);
    	
    	/* weighting of the target vector */
    	System.arraycopy(buf, 4 + length, target, 421, 10);
    	BasicFunctions.filterAR(inTarget, inTargetIndex, target, 431, weightDenum, weightDenumindex, 11, vectorLength);    	

    	/* Store target, towards the end codedVec is calculated as
    	the initial target minus the remaining target */
    	System.arraycopy(target, 431, codedVec, 618, vectorLength);  	

    	/* Find the highest absolute value to calculate proper
    	vector scale factor (so that it uses 12 bits) */
    	tempIndex1=14;    	
    	tempS=0;
    	for(i=0;i<length;i++)
    	{
    		if(buf[tempIndex1]>0 && buf[tempIndex1]>tempS)
    			tempS=buf[tempIndex1];
    		else if((0-buf[tempIndex1])>tempS)
    			tempS=(short)(0-buf[tempIndex1]);
    		
    		tempIndex1++;
    	}
    	
    	tempIndex1=431;
    	tempS2=0;
    	for(i=0;i<vectorLength;i++)
    	{
    		if(target[tempIndex1]>0 && target[tempIndex1]>tempS2)
    			tempS2=target[tempIndex1];
    		else if((0-target[tempIndex1])>tempS2)
    			tempS2=(short)(0-target[tempIndex1]);
    		
    		tempIndex1++;
    	}    	 

    	if ((tempS>0)&&(tempS2>0)) 
    	{
    		if(tempS2>tempS)
    			tempS = tempS2;
    		
    	    scale = BasicFunctions.getSize(tempS*tempS);
    	} 
    	else 
    	{
    	    /* tempS or tempS2 is negative (maximum was -32768) */
    	    scale = 30;
    	}

    	/* Scale to so that a mul-add 40 times does not overflow */
    	scale = (short)(scale - 25);
    	if(scale<0)
    		scale=0;    	

    	scale2=scale;
    	/* Compute energy of the original target */
    	targetEner = BasicFunctions.scaleRight(target,431,target,431,vectorLength, scale2);    	 
    	
    	/* Prepare search over one more codebook section. This section
    	is created by filtering the original buffer with a filter. */
    	filteredCBVecs(cbVectors, 471, buf, 14, length,Constants.FILTER_RANGE[blockNumber]);
    	    	
    	range = Constants.SEARCH_RANGE[blockNumber][0];    	
    	
    	if(vectorLength == 40) 
    	{
    	    /* Create the interpolated samples and store them for use in all stages */
    		/* First section, non-filtered half of the cb */
    		interpolateSamples(interpSamples,658,buf,14,length);    	    

    		/* Second section, filtered half of the cb */
    		interpolateSamples(interSamplesFilt,738,cbVectors,471,length);    	    
    	    
    		/* Compute the CB vectors' energies for the first cb section (non-filtered) */
    	    cbMemEnergyAugmentation(interpSamples, 658, buf, 14, scale2, (short)20, energy, 818, energyShifts, 165);    	    
    	    
    	    /* Compute the CB vectors' energies for the second cb section (filtered cb) */
    	    cbMemEnergyAugmentation(interSamplesFilt, 738, cbVectors, 471, scale2, (short)(baseSize+20), energy, 818, energyShifts, 165);    	    

    	    /* Compute the CB vectors' energies and store them in the vector
    	     * energyW16. Also the corresponding shift values are stored. The
    	     * energy values are used in all three stages. */    	    
    	    cbMemEnergy(range, buf, 14, cbVectors, 471, (short)length, (short)vectorLength, energy, 838, energyShifts, 185, scale2, baseSize);    	        	    	
    	} 
    	else 
    	{
    		/* Compute the CB vectors' energies and store them in the vector
    	     * energyW16. Also the corresponding shift values are stored. The
    	     * energy values are used in all three stages. */
    		cbMemEnergy(range, buf, 14, cbVectors, 471, (short)length, (short)vectorLength, energy, 818, energyShifts, 165, scale2, baseSize);

    	    /* Set the energy positions 58-63 and 122-127 to zero
    	    (otherwise they are uninitialized) */
    	   //do not set since cleared array 	    
    	}    	
    	
    	/* Calculate Inverse Energy (energyW16 is already normalized
    	and will contain the inverse energy in Q29 after this call */    	
    	energyInverse(energy,818,baseSize*2);
    	
    	
    	/* The gain value computed in the previous stage is used
    	* as an upper limit to what the next stage gain value
    	* is allowed to be. In stage 0, 16384 (1.0 in Q14) is used as
    	* the upper limit. */
    	gains[0] = 16384;

    	for (stage=0; stage<3; stage++) 
    	{

    	    /* Set up memories */
    	    range = Constants.SEARCH_RANGE[blockNumber][stage];

    	    /* initialize search measures */
    	    updateIndexData.setCritMax(0);
    	    updateIndexData.setShTotMax((short)-100);
    	    updateIndexData.setBestIndex((short)0);
    	    updateIndexData.setBestGain((short)0);    	    

    	    /* loop over lags 40+ in the first codebook section, full search */
    	    tempIndex2 = 14 + length - vectorLength;    	    
    	    
    	    /* Calculate all the cross correlations (augmented part of CB) */
    	    if (vectorLength==40) 
    	    {
    	    	augmentCbCorr(target, 431, buf, 14+length, interpSamples, 658, cDot, 0,20, 39, scale2);    	    	    	    	
    			    			
    	    	tempIndex1=20;
    	    } 
    	    else 
    	    	tempIndex1=0;
    	    
    	    /* Calculate all the cross correlations (main part of CB) */
    	    crossCorrelation(cDot, tempIndex1, target, 431, buf, tempIndex2, (short)vectorLength, range, scale2, (short)-1);    	        	    
        	
    	    /* Adjust the search range for the augmented vectors */
    	    if (vectorLength==40)
    	      range=(short)(Constants.SEARCH_RANGE[blockNumber][stage]+20);
    	    else
    	      range=Constants.SEARCH_RANGE[blockNumber][stage];    	    

    	    /* Search for best index in this part of the vector */
    	    cbSearchCore(cDot, 0, range, (short)stage, inverseEnergy, 818, inverseEnergyShifts, 165, crit, 128);    	        	    
    	    
    	    /* Update the global best index and the corresponding gain */
    	    updateBestIndex(searchData.getCritNew(),searchData.getCritNewSh(),searchData.getIndexNew(),cDot[searchData.getIndexNew()],inverseEnergy[818+searchData.getIndexNew()],inverseEnergyShifts[165+searchData.getIndexNew()]);    	        	    
    	    
    	    sInd=(short)(updateIndexData.getBestIndex()-17);
    	    eInd=(short)(sInd+34);
    	    
    	    if (sInd<0) 
    	    {
    	      eInd-=sInd;
    	      sInd=0;
    	    }
    	    
    	    if (eInd>=range) 
    	    {
    	      eInd=(short)(range-1);
    	      sInd=(short)(eInd-34);
    	    }

    	    range = Constants.SEARCH_RANGE[blockNumber][stage];

    	    if (vectorLength==40) 
    	    {
    	    	i=sInd;
    	    	
    	    	if (sInd<20) 
    	    	{    	    		    	    	
    	    		if(eInd+20>39)
    	    			augmentCbCorr(target, 431, cbVectors, 471+length, interSamplesFilt, 738, cDot, 0, sInd+20, 39, scale2);
    	    		else
    	    			augmentCbCorr(target, 431, cbVectors, 471+length, interSamplesFilt, 738, cDot, 0, sInd+20, eInd+20, scale2);
    	    		
    	    		i=20;
    	    	}

    	    	if(20-sInd>0)
    	    		tempIndex1=20-sInd;
    	    	else
    	    		tempIndex1=0;
    	    	
    	    	tempIndex2 = 451 + length - i;

    	    	/* Calculate the cross correlations (main part of the filtered CB) */
    	    	crossCorrelation(cDot, tempIndex1, target, 431, cbVectors, tempIndex2, (short)vectorLength, (short)(eInd-i+1), scale2, (short)-1);    	    	
    	    } 
    	    else 
    	    {
    	    	tempIndex1 = 0;
    	    	tempIndex2 = 471 + length - vectorLength - sInd;

    	    	/* Calculate the cross correlations (main part of the filtered CB) */
    	    	crossCorrelation(cDot, tempIndex1, target, 431, cbVectors, tempIndex2, (short)vectorLength, (short)(eInd-sInd+1), scale2, (short)-1);    	    	
    	    }
    	    
    	    /* Search for best index in this part of the vector */
    	    cbSearchCore(cDot, 0, (short)(eInd-sInd+1), (short)stage, inverseEnergy, 818+baseSize+sInd, inverseEnergyShifts, 165+baseSize+sInd, crit, 128);
    	    
    	    /* Update the global best index and the corresponding gain */
    	    updateBestIndex(searchData.getCritNew(),searchData.getCritNewSh(),(short)(searchData.getIndexNew()+baseSize+sInd),cDot[searchData.getIndexNew()],inverseEnergy[818+searchData.getIndexNew()+baseSize+sInd],inverseEnergyShifts[165+searchData.getIndexNew()+baseSize+sInd]);    	    	

    	    cbIndex[cbIndexIndex+stage] = updateIndexData.getBestIndex();
    	    
    	    if(gains[stage]>0)    	    	
    	    	updateIndexData.setBestGain(gainQuant(updateIndexData.getBestGain(), gains[stage], (short)stage, gainIndex, gainIndexIndex+stage));
    	    else
    	    	updateIndexData.setBestGain(gainQuant(updateIndexData.getBestGain(), (short)(0-gains[stage]), (short)stage, gainIndex, gainIndexIndex+stage));
    	    
    	    /* Extract the best (according to measure) codebook vector
    	       Also adjust the index, so that the augmented vectors are last.
    	       Above these vectors were first...
    	    */    	    
    	    
    	    if(vectorLength == 80-encoderState.STATE_SHORT_LEN) 
    	    {
    	    	if(cbIndex[cbIndexIndex+stage]<baseSize)
    	    		tempIndex3 = 14+length-vectorLength-cbIndex[cbIndexIndex+stage];
    	    	else
    	    		tempIndex3 = 471+length-vectorLength- cbIndex[cbIndexIndex+stage]+baseSize;    	      
    	    } 
    	    else 
    	    {
    	    	if (cbIndex[cbIndexIndex+stage]<baseSize) 
    	    	{    	    		
    	    		if (cbIndex[cbIndexIndex+stage]>=20) 
    	    		{
    	    			/* Adjust index and extract vector */
    	    			cbIndex[cbIndexIndex+stage]-=20;
    	    			tempIndex3 = 14+length-vectorLength-cbIndex[cbIndexIndex+stage];
    	    		} 
    	    		else 
    	    		{
    	    			/* Adjust index and extract vector */
    	    			cbIndex[cbIndexIndex+stage]+=(baseSize-20);

    	    			createAugmentVector((short)(cbIndex[cbIndexIndex+stage]-baseSize+40),buf, 14+length, augVec, 1074);
    	    			tempIndex3 = 1074;
    	    		}
    	    	} 
    	    	else 
    	    	{
    	    		if ((cbIndex[cbIndexIndex+stage] - baseSize) >= 20) 
    	    		{
    	    			/* Adjust index and extract vector */
    	    			cbIndex[cbIndexIndex+stage]-=20;
    	    			tempIndex3 = 471+length-vectorLength- cbIndex[cbIndexIndex+stage]+baseSize;
    	    		} 
    	    		else 
    	    		{
    	    			/* Adjust index and extract vector */
    	    			cbIndex[cbIndexIndex+stage]+=(baseSize-20);
    	    			createAugmentVector((short)(cbIndex[cbIndexIndex+stage]-2*baseSize+40),cbVectors, 471+length, augVec, 1074);    	    			
    	    			tempIndex3 = 1074;
    	    		}
    	    	}
    	    }

    	    /* Subtract the best codebook vector, according
    	    to measure, from the target vector */
    	    BasicFunctions.addAffineVectorToVector(target, 431, buf, tempIndex3, (short)(0-updateIndexData.getBestGain()), 8192, (short)14, vectorLength);    	    

    	    /* record quantized gain */    	    
    	    gains[stage+1] = updateIndexData.getBestGain();    	        	        	   
        } /* end of Main Loop. for (stage=0;... */    	    	
    	
    	tempIndex1=618;
    	tempIndex2=431;
    	
    	/* Calculte the coded vector (original target - what's left) */
        for (i=0;i<vectorLength;i++)
            codedVec[tempIndex1++]-=target[tempIndex2++];
    	
        /* Gain adjustment for energy matching */
        codedEner = BasicFunctions.scaleRight(codedVec,618,codedVec,618,vectorLength, scale2);
        
        j=gainIndex[gainIndexIndex+0];

    	tempS = BasicFunctions.norm(codedEner);
    	tempS2 = BasicFunctions.norm(targetEner);

    	if(tempS < temp2)
    		nBits = (short)(16 - tempS);
    	else
    		nBits = (short)(16 - tempS2);    	  

    	tempShift=(gains[1]*gains[1])>>14;
    	if(nBits<0)
    		targetEner = (targetEner<<(0-nBits))*tempShift;
    	else
    		targetEner = (targetEner>>nBits)*tempShift;
    	
    	temp = ((gains[1]-1)<<1);

    	/* Pointer to the table that contains gain_sq5TblFIX * gain_sq5TblFIX in Q14 */
    	j=gainIndex[gainIndexIndex];
    	
    	if(nBits<0)
    		tempS=(short)(codedEner<<(-nBits));
    	else
    		tempS=(short)(codedEner>>nBits);
    	
    	/* targetEner and codedEner are in Q(-2*scale) */
    	for (i=gainIndex[gainIndexIndex];i<32;i++) 
    	{
    	    /* Change the index if (codedEnergy*gainTbl[i]*gainTbl[i])<(targetEn*gain[0]*gain[0]) AND gainTbl[i] < 2*gain[0] */
    	    temp2 = tempS*Constants.GAIN_SQ5_SQ[i];
    	    temp2 = temp2 - targetEner;
    	    
    	    if (temp2 < 0 && Constants.GAIN_SQ5[j] < temp) 
    	    	j=i;      	    	        	    
    	}
    	gainIndex[gainIndexIndex]=(short)j;    	    	 	     	   
    }    

    private void filteredCBVecs(short[] cbVectors, int cbVectorsIndex, short[] cbMem, int cbMemIndex, int length,int samples)
    {
    	tempIndex7=cbMemIndex+length;
    	for(n=0;n<4;n++)
    		cbMem[tempIndex7++]=0;
    	
    	tempIndex7=cbMemIndex-4;
    	for(n=0;n<4;n++)
    		cbMem[tempIndex7++]=0;
    	
    	tempIndex7=cbVectorsIndex;
    	for(n=0;n<length-samples;n++)
    		cbVectors[tempIndex7++]=0;    	

    	/* Filter to obtain the filtered CB memory */
    	BasicFunctions.filterMA(cbMem, cbMemIndex+4+length-samples, cbVectors, cbVectorsIndex + length - samples, Constants.CB_FILTERS_REV, 0, 8, samples);    	
    }
    
    private void energyInverse(short[] energy,int energyIndex,int length)
    {
    	/* Set the minimum energy value to 16384 to avoid overflow */
    	/* Calculate inverse energy in Q29 */
    	tempIndex7=energyIndex;
    	for (n=0; n<length; n++)
    		if(energy[tempIndex7]<16384)
    			energy[tempIndex7++]=16384;
    	
    	tempIndex7=energyIndex;
    	for (n=0; n<length; n++)
    	{
    		if(energy[tempIndex7]==0)
    			energy[tempIndex7]=Short.MAX_VALUE;
    		else
    			energy[tempIndex7]=(short)(0x1FFFFFFF/energy[tempIndex7]);    		
    		
    		tempIndex7++;
    	}
    }
    
    private void interpolateSamples(short[] interpSamples,int interpSamplesIndex,short[] cbMem,int cbMemIndex,int length)
    {
    	/* Calculate the 20 vectors of interpolated samples (4 samples each) that are used in the codebooks for lag 20 to 39 */
    	for (n=0; n<20; n++) 
    	{
    	    tempIndex6 = cbMemIndex+length-4;
    	    tempIndex7 = cbMemIndex+length-n-24;
    	    
    	    tempShift=(Constants.ALPHA[3]*cbMem[tempIndex6++])>>15;
    		temp=tempShift;
    		tempShift=(Constants.ALPHA[0]*cbMem[tempIndex7++])>>15;
    		temp+=tempShift;
    	    interpSamples[interpSamplesIndex++] = (short)temp;
    	    tempShift=(Constants.ALPHA[2]*cbMem[tempIndex6++])>>15;
    		temp=tempShift;
    		tempShift=(Constants.ALPHA[1]*cbMem[tempIndex7++])>>15;
    		temp+=tempShift;
  	      	interpSamples[interpSamplesIndex++] = (short)temp;
  	      	tempShift=(Constants.ALPHA[1]*cbMem[tempIndex6++])>>15;
  			temp=tempShift;
  			tempShift=(Constants.ALPHA[2]*cbMem[tempIndex7++])>>15;
  			temp+=tempShift;
  	      	interpSamples[interpSamplesIndex++] = (short)temp;
  	      	tempShift=(Constants.ALPHA[0]*cbMem[tempIndex6++])>>15;
  			temp=tempShift;
  			tempShift=(Constants.ALPHA[3]*cbMem[tempIndex7++])>>15;
  			temp+=tempShift;
  	      	interpSamples[interpSamplesIndex++] = (short)temp;    	        	      	
    	}
    }    
    
    private void cbMemEnergyAugmentation(short[] interpSamples, int interpSamplesIndex, short[] cbMem, int cbMemIndex, short scale, short baseSize, short[] energy, int energyIndex, short[] energyShifts, int energyShiftsIndex)    	    
    {
    	energyIndex = energyIndex + baseSize-20;
    	energyShiftsIndex = energyShiftsIndex + baseSize-20;
    	cbMemIndex = cbMemIndex + 147;    	

    	/* Compute the energy for the first (low-5) noninterpolated samples */
    	en1 = BasicFunctions.scaleRight(cbMem,cbMemIndex-19,cbMem,cbMemIndex-19,15,scale);    		
    	tempIndex7 = cbMemIndex - 20;

    	for (n=20; n<=39; n++) 
    	{
    	    /* Update the energy recursively to save complexity */
    		en1 += (cbMem[tempIndex7]*cbMem[tempIndex7])>>scale;
    	    tempIndex7--;
    	    temp2 = en1;

    	    /* interpolation */
    	    temp2 += BasicFunctions.scaleRight(interpSamples,interpSamplesIndex,interpSamples,interpSamplesIndex,4,scale);        	    	
    	    interpSamplesIndex += 4;

    	    /* Compute energy for the remaining samples */
    	    temp2 += BasicFunctions.scaleRight(cbMem,cbMemIndex - n,cbMem,cbMemIndex - n,40-n,scale);    	    	

    	    /* Normalize the energy and store the number of shifts */
    	    energyShifts[energyShiftsIndex] = BasicFunctions.norm(temp2);
    	    temp2 = temp2<<energyShifts[energyShiftsIndex++];
    	    
    	    energy[energyIndex++] = (short)(temp2>>16);    	    
    	}
    }
        
    private void cbMemEnergy(short range,short[] cb,int cbIndex,short[] filteredCB,int filteredCbIndex, short length, short targetLength, short[] energy, int energyIndex, short[] energyShifts, int energyShiftsIndex, short scale, short baseSize)
    {
    	/* Compute the energy and store it in a vector. Also the
    	* corresponding shift values are stored. The energy values
    	* are reused in all three stages. */

    	/* Calculate the energy in the first block of 'lTarget' sampels. */
    	temp2 = BasicFunctions.scaleRight(cb, cbIndex + length - targetLength, cb, cbIndex + length - targetLength, targetLength, scale);    	

    	/* Normalize the energy and store the number of shifts */
    	energyShifts[energyShiftsIndex] = (short)BasicFunctions.norm(temp2);
    	temp = temp2<<energyShifts[energyShiftsIndex];
    	energy[energyIndex] = (short)(temp>>16);

    	/* Compute the energy of the rest of the cb memory
    	* by step wise adding and subtracting the next
    	* sample and the last sample respectively. */
    	energyCalc(temp2, range, cb, cbIndex + length - targetLength - 1, cb, cbIndex + length - 1, energy, energyIndex, energyShifts, energyShiftsIndex, scale, (short)0);
    	
    	/* Next, precompute the energy values for the filtered cb section */
    	temp2 = BasicFunctions.scaleRight(filteredCB, filteredCbIndex + length - targetLength, filteredCB, filteredCbIndex + length - targetLength, targetLength, scale);    	

    	/* Normalize the energy and store the number of shifts */
    	energyShifts[baseSize + energyShiftsIndex]=BasicFunctions.norm(temp2);    	
    	temp = temp2<<energyShifts[baseSize + energyShiftsIndex];
    	energy[baseSize + energyIndex] = (short)(temp>>16);

    	energyCalc(temp2, range, filteredCB, filteredCbIndex + length - targetLength - 1, filteredCB, filteredCbIndex + length - 1, energy, energyIndex, energyShifts, energyShiftsIndex, scale, baseSize);    	    		    
    }
        
    private void energyCalc(int energy, short range, short[] ppi, int ppiIndex, short[] ppo, int ppoIndex, short[] energyArray, int energyArrayIndex, short[] energyShifts, int energyShiftsIndex, short scale, short baseSize)
    {
    	energyShiftsIndex += 1 + baseSize;    	
    	energyArrayIndex += 1 + baseSize;    	

    	for(n=0;n<range-1;n++) 
    	{
    	    /* Calculate next energy by a +/- operation on the edge samples */
    	    energy += ((ppi[ppiIndex] * ppi[ppiIndex])-(ppo[ppoIndex] * ppo[ppoIndex]))>>scale;
    	    if(energy<0)
    	    	energy=0;    	    

    	    ppiIndex--;
    	    ppoIndex--;

    	    /* Normalize the energy into a WebRtc_Word16 and store the number of shifts */
    	    energyShifts[energyShiftsIndex] = BasicFunctions.norm(energy);

    	    energyArray[energyArrayIndex++] = (short)((energy<<energyShifts[energyShiftsIndex])>>16);
    	    energyShiftsIndex++;
    	}
    }
    
    private void augmentCbCorr(short[] target, int targetIndex, short[] buf, int bufIndex, short[] interpSamples, int interpSamplesIndex, int[] cDot,int cDotIndex,int low,int high,int scale)
    {
    	/* Calculate the correlation between the target and the
    	interpolated codebook. The correlation is calculated in
    	3 sections with the interpolated part in the middle */    	
    	for (n=low; n<=high; n++) 
    	{
    	    /* Compute dot product for the first (n-4) samples */
    		cDot[cDotIndex] = BasicFunctions.scaleRight(target, targetIndex, buf, bufIndex-n, n-4, scale);    	    

    	    /* Compute dot product on the interpolated samples */
    		cDot[cDotIndex]+=BasicFunctions.scaleRight(target, targetIndex+n-4, interpSamples, interpSamplesIndex, 4, scale);    	    
    	    interpSamplesIndex += 4;

    	    /* Compute dot product for the remaining samples */
    	    cDot[cDotIndex]+=BasicFunctions.scaleRight(target, targetIndex+n, buf, bufIndex-n, 40-n, scale);
    	    cDotIndex++;
    	}
    }
   
    private void crossCorrelation(int[] crossCorrelation,int crossCorrelationIndex,short[] seq1, int seq1Index,short[] seq2, int seq2Index,short dimSeq,short dimCrossCorrelation,short rightShifts,short stepSeq2)
    {
    	tempIndex7 = crossCorrelationIndex;
    	for (i = 0; i < dimCrossCorrelation; i++)
        {
            // Set the pointer to the static vector, set the pointer to the sliding vector
            // and initialize cross_correlation            
            tempIndex5 = seq2Index + stepSeq2 * i;
            tempIndex6 = seq1Index;            
            crossCorrelation[tempIndex7]=0;
            
            // Perform the cross correlation
            for (j = 0; j < dimSeq; j++)
            	crossCorrelation[tempIndex7] += (seq1[tempIndex6++]*seq2[tempIndex5++])>>rightShifts;
            
            tempIndex7++;
        }    
    }
    
    private void createAugmentVector(short index,short[] buf,int bufIndex,short[] cbVec,int cbVecIndex)
    {
    	short[] cbVecTmp=tempMemory;//+1330 length 4    	
    	
    	tempIndex7=cbVecIndex+index-4;    	

    	/* copy the first noninterpolated part */    	
    	System.arraycopy(buf, bufIndex-index, cbVec, cbVecIndex, index);    	

    	/* perform cbVec[ilow+k] = ((ppi[k]*alphaTbl[k])>>15) + ((ppo[k]*alphaTbl[3-k])>>15);
    	   for k = 0..3
    	*/
    	BasicFunctions.multWithRightShift(cbVec, tempIndex7, buf, bufIndex-index-4, Constants.ALPHA, 0, 4, 15);
    	BasicFunctions.reverseMultiplyRight(cbVecTmp, 1330, buf, bufIndex-4, Constants.ALPHA, 3, 4, 15);
    	BasicFunctions.addWithRightShift(cbVec, tempIndex7, cbVec, tempIndex7, cbVecTmp, 1330, 4, 0);    	

    	/* copy the second noninterpolated part */
    	System.arraycopy(buf, bufIndex-index, cbVec, cbVecIndex + index, 40-index);    	
    }
   
    private short gainQuant(short gain,short maxIn,short stage,short[] index,int indexIndex)
    {
    	short[] cb;    	
    	
    	/* ensure a lower bound (0.1) on the scaling factor */
    	if(maxIn>1638)
    		scale = maxIn;
    	else
    		scale = 1638;

    	/* select the quantization table and calculate the length of the table and the number of steps in the binary search that are needed */
    	cb = Constants.GAIN[stage];
    	
    	/* Multiply the gain with 2^14 to make the comparison easier and with higher precision */
    	temp = gain<<14;

    	/* Do a binary search, starting in the middle of the CB
    	   loc - defines the current position in the table
    	   noMoves - defines the number of steps to move in the CB in order
    	   to get next CB location
    	*/
    	tempIndex7=(32>>stage) >> 1;
    	nBits = (short)tempIndex7;
    	for (n=4-stage;n>0;n--) 
    	{
    		nBits>>=1;
    	    /* Move up if gain is larger, otherwise move down in table */
    	    if (0 > scale*cb[tempIndex7] - temp) 
    	    	tempIndex7+=nBits;    	     
    	    else 
    	    	tempIndex7-=nBits;
    	        	    
    	}

    	/* Check which value is the closest one: loc-1, loc or loc+1 */
    	temp2=scale*cb[tempIndex7];
    	if (temp>temp2) 
    	{
    	    /* Check against value above loc */
    	    if ((scale*cb[tempIndex7+1]-temp)<(temp-temp2))
    	    	tempIndex7+=1;    	    
    	} 
    	else 
    	{
    	    /* Check against value below loc */
    	    if ((temp-scale*cb[tempIndex7-1])<=(temp2-temp))
    	    	tempIndex7-=1;    	    
    	}

    	/* Guard against getting outside the table. The calculation above can give a location which is one above the maximum value (in very rare cases) */
    	temp=(32>>stage) - 1;
    	if(tempIndex7>temp)
    		tempIndex7=temp;
    	
    	index[indexIndex]=(short)tempIndex7;
    	
    	/* Calculate the quantized gain value (in Q14) */
    	/* return the quantized value */
    	return (short)((scale*cb[tempIndex7]+8192)>>14); 
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
    
    private void updateBestIndex(int critNew,short critNewSh,short indexNew,int cDotNew,short inverseEnergyNew,short energyShiftNew)
    {
    	/* Normalize the new and old Criteria to the same domain */
    	if (critNewSh>updateIndexData.getShTotMax()) 
    	{
    		if(31<critNewSh-updateIndexData.getShTotMax())
    			tempS=31;
    		else
    			tempS=(short)(critNewSh-updateIndexData.getShTotMax());
    	    
    	    tempS2=0;
    	} 
    	else 
    	{
    		if(31<updateIndexData.getShTotMax()-critNewSh)
    			tempS2=31;
    		else
    			tempS2=(short)(updateIndexData.getShTotMax()-critNewSh);
    		
    		tempS=0;    	    
    	}

    	/* Compare the two criterias. If the new one is better,
    	   calculate the gain and store this index as the new best one
    	*/

    	if ((critNew>>tempS2) > (updateIndexData.getCritMax()>>tempS)) 
    	{
    		tempS = (short)(16 - BasicFunctions.norm(cDotNew));

    	    /* Calculate the gain in Q14
    	       Compensate for inverseEnergyshift in Q29 and that the energy
    	       value was stored in a WebRtc_Word16 (shifted down 16 steps)
    	       => 29-14+16 = 31 */

    		tempS2=(short)(31-energyShiftNew-tempS);
    		if(tempS2>31)
        	    tempS2=31;    	    

    		if(tempS<0)
    			tempShift=(cDotNew<<(-tempS));
    		else
    			tempShift=(cDotNew>>(tempS));
    		
    		temp = ((tempShift*inverseEnergyNew)>>tempS2);
    			
    	    /* Check if criteria satisfies Gain criteria (max 1.3)
    	       if it is larger set the gain to 1.3
    	       (slightly different from FLP version)
    	    */
    	    if (temp>21299) 
    	    	updateIndexData.setBestGain((short)21299);    	      
    	    else if (temp<-21299) 
    	    	updateIndexData.setBestGain((short)-21299); 
    	    else 
    	    	updateIndexData.setBestGain((short)temp);

    	    updateIndexData.setCritMax(critNew);
    	    updateIndexData.setShTotMax(critNewSh);
    	    updateIndexData.setBestIndex(indexNew);    	    
    	}
    }
    
    private void cbSearchCore(int[] cDot, int cDotIndex, short range, short stage, short[] inverseEnergy, int inverseEnergyIndex, short[] inverseEnergyShift, int inverseEnergyShiftIndex, int[] crit, int critIndex)
    {        
    	/* Don't allow negative values for stage 0 */
    	if (stage==0) 
    	{
    		tempIndex7=cDotIndex;    	    
    	    for (n=0;n<range;n++) 
    	    {
    	    	if(cDot[tempIndex7]<0)
    	    		cDot[tempIndex7]=0;
    	    	
    	    	tempIndex7++;
    	    }
    	}

    	/* Normalize cDot to WebRtc_Word16, calculate the square of cDot and store the upper WebRtc_Word16 */
    	tempIndex7=cDotIndex;
    	temp=0;
    	for(n=0;n<range;n++)
    	{
    		if(cDot[tempIndex7]>0 && cDot[tempIndex7]>temp)
    			temp=cDot[tempIndex7];
    		else if((0-cDot[tempIndex7])>temp)
    			temp=0-cDot[tempIndex7];
    		tempIndex7++;
    	}
    	
    	nBits=BasicFunctions.norm(temp);
    	tempIndex7=cDotIndex;  
    	tempIndex6=critIndex;
    	tempIndex5=inverseEnergyShiftIndex;    	
    	max=Short.MIN_VALUE;

    	for (n=0;n<range;n++) 
    	{
    	    /* Calculate cDot*cDot and put the result in a WebRtc_Word16 */
    		tempShift=cDot[tempIndex7++]<<nBits;
    	    tempS = (short)(tempShift>>16);
    	    
    	    /* Calculate the criteria (cDot*cDot/energy) */
    	    tempShift=(tempS*tempS)>>16;
    	    crit[tempIndex6]=tempShift*inverseEnergy[inverseEnergyIndex++];
    	    
    	    /* Extract the maximum shift value under the constraint
    	       that the criteria is not zero */
    	    if (crit[tempIndex6]!=0 && inverseEnergyShift[tempIndex5]>max)
    	    	max = inverseEnergyShift[tempIndex5];    	    	
    	    
    	    tempIndex5++;
    	    tempIndex6++;
    	}
    	
    	/* If no max shifts still at initialization value, set shift to zero */
    	if (max==Short.MIN_VALUE)
    	    max = 0;    	  

    	/* Modify the criterias, so that all of them use the same Q domain */
    	tempIndex6=critIndex;
    	tempIndex5=inverseEnergyShiftIndex;
    	for (n=0;n<range;n++) 
    	{
    	    /* Guarantee that the shift value is less than 16
    	       in order to simplify for DSP's (and guard against >31) */
    		if(16<max-inverseEnergyShift[tempIndex5])
    			tempS = 16;
    		else
    			tempS = (short)(max-inverseEnergyShift[tempIndex5]);
    		
    	    if(tempS<0)
    	    	crit[tempIndex6]=crit[tempIndex6]<<(-tempS);    	    	
    	    else
    	    	crit[tempIndex6]=crit[tempIndex6]>>(tempS);
    	    
    	    tempIndex5++;
    	    tempIndex6++;
    	        	    
    	}

    	/* Find the index of the best value */
    	tempIndex6=critIndex+1;
    	temp=crit[critIndex];
    	searchData.setIndexNew((short)0);
    	for(n=1;n<range;n++)
    	{
    		if(crit[tempIndex6]>temp)
    		{
    			temp=crit[tempIndex6];
    			searchData.setIndexNew((short)n);    			
    		}
    		
    		tempIndex6++;
    	}
    	
    	searchData.setCritNew(crit[critIndex + searchData.getIndexNew()]);    	

    	/* Calculate total shifts of this criteria */
    	searchData.setCritNewSh((short)(32 - 2*nBits + max));    	
    }
    
    private void cbConstruct(short[] decVector,int decVectorIndex,short[] mem,int memIndex,short length,short vectorLength,int cbIndexIndex,int gainIndexIndex)
    {
    	short[] cbIndex=encoderBits.getCbIndex();
    	short[] gainIndex=encoderBits.getGainIndex();
    	
    	/* Stack based */
    	short gain[]=tempMemory;//+0 length 3
    	short cbVec0[]=tempMemory;//+3 length 40
    	short cbVec1[]=tempMemory;//+43 length 40
    	short cbVec2[]=tempMemory;//+83 length 40
    	
    	/* gain de-quantization */
    	gain[0] = gainDequant(gainIndex[gainIndexIndex], (short)16384, (short)0);
    	gain[1] = gainDequant(gainIndex[gainIndexIndex+1], (short)gain[0], (short)1);
    	gain[2] = gainDequant(gainIndex[gainIndexIndex+2], (short)gain[1], (short)2);

    	/* codebook vector construction and construction of total vector */

    	for(i=0;i<40;i++)
    	{
    		cbVec0[3+i]=0;
    		cbVec1[43+i]=0;
    		cbVec2[83+i]=0;
    	}
    	/* Stack based */
    	getCbVec(cbVec0, 3, mem, memIndex, cbIndex[cbIndexIndex], length, vectorLength);
    	getCbVec(cbVec1, 43, mem, memIndex, cbIndex[cbIndexIndex+1], length, vectorLength);
    	getCbVec(cbVec2, 83, mem, memIndex, cbIndex[cbIndexIndex+2], length, vectorLength);
    	    	
		tempIndex5=3;
    	tempIndex6=43;
    	tempIndex7=83;
    	for(i=0;i<vectorLength;i++)
    	{
    		temp=gain[0]*cbVec0[tempIndex5++];
    		temp+=gain[1]*cbVec1[tempIndex6++];
    		temp+=gain[2]*cbVec2[tempIndex7++];
    		    		
    		tempShift=(temp+8192)>>14;
    		decVector[decVectorIndex++]=(short)tempShift;
    	}    	
    }
    
    private void getCbVec(short[] cbVec,int cbVecIndex,short[] mem,int memIndex,short index,int length,int vectorLength)
    {
    	/* Stack based */    	  
    	short tempbuff2[]=tempMemory;//+123 length 45    	        	

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
    	    	BasicFunctions.filterMA(mem, memIndex+length-vectorLength-1, tempbuff2, 123, Constants.CB_FILTERS_REV, 0, 8, vectorLength+5);    	    	

    	    	createAugmentVector((short)((vectorLength<<1)-20+index-baseSize-length-1),tempbuff2,168,cbVec,cbVecIndex);    	    	
    	     }
    	 }
    }

    private void packBits(byte[] result)
    {    	    
    	short[] lsf=encoderBits.getLSF();
    	short[] cbIndex=encoderBits.getCbIndex();
    	short[] gainIndex=encoderBits.getGainIndex();
    	short[] idxVec=encoderBits.getIdxVec();
    	
    	//********************************************************
    	//filling class 1 bits
    	//********************************************************
    	
    	//6 bits of lsf0 + 2 bits of lsf1
    	result[0]=(byte)((lsf[0]<<2) | ((lsf[1]>>5) & 0x3));
    	//5 bits of lsf1 + 3 bits of lsf2
    	result[1]=(byte)((lsf[1] & 0x1F)<<3 | ((lsf[2]>>4) & 0x7));
    	//4 bits of lsf2
    	result[2]=(byte)((lsf[2] & 0xF)<<4);    	
    	    	
    	if (encoderState.ENCODER_MODE==20) 
    	{    		
    		//2 bits for startidx , one bit for isStateFirst
    		if(encoderBits.getStateFirst())
    			result[2]|=(encoderBits.getStartIdx()&0x3)<<2 | 0x2;
    		else
    			result[2]|=(encoderBits.getStartIdx()&0x3)<<2;

    		//one bit for idxForMax
    		result[2]|=(encoderBits.getIdxForMax()>>5) & 0x1;
    		
    		//5 bits for idxForMax and 3 bits cbIndex[0]
    		result[3]=(byte)(((encoderBits.getIdxForMax() & 0x1F)<<3) | ((cbIndex[0]>>4) & 0x7));
    	    
    		//3 bits cbIndex[0] 2 bits gainIndex[0] 1 bit gainIndex[1] 2 bits cbIndex[3]
    		result[4]=(byte)(((cbIndex[0] & 0xE)<<4) | (gainIndex[0] & 0x18) | ((gainIndex[1] & 0x8)>>1) | ((cbIndex[3]>>6) & 0x3));
    		
    		//5 bits cbIndex[3] 1 bit gainIndex[3] 1 bit gainIndex[4] 1 bit gainIndex[6]
    		result[5]=(byte)(((cbIndex[3] & 0x3E)<<2) | ((gainIndex[3]>>2) & 0x4)  | ((gainIndex[4]>>2) & 0x2) | ((gainIndex[6]>>4) & 0x1));
    		
    		tempIndex7=6;
    	} 
    	else 
    	{ /* mode==30 */
    		//4 bits lsf[3]
    		result[2]|=(lsf[3]>>2) & 0xF;
    		
    		//2 bits lsf[3] 6 bits lsf[4]
    		result[3]=(byte)(((lsf[3] & 0x3)<<6) | ((lsf[4]>>1) & 0x3F));
    		
    		//1 bit lsf[4] 7 bits lsf[5]
    		result[4]=(byte)(((lsf[4] & 0x1)<<7) | (lsf[5] & 0x7F));
    		
    		//3 bits startIdx one bit isStateFirst 4 bits idxForMax
    		if(encoderBits.getStateFirst())
    			result[5]=(byte)((encoderBits.getStartIdx()<<5) | 0x10 | (encoderBits.getIdxForMax()>>2) & 0xF); 
    		else
    			result[5]=(byte)((encoderBits.getStartIdx()<<5) | (encoderBits.getIdxForMax()>>2) & 0xF);
    		
    		//2 bits idxForMax 4 bits cbIndex[0] 1 bit gainIndex[0] 1 bit gainIndex[1]
    		result[6]=(byte)(((encoderBits.getIdxForMax() & 0x3)<<6) | ((cbIndex[0]&0x78)>>1) | ((gainIndex[0]&0x10)>>3) | ((gainIndex[1]&0x80)>>3)); 
    		
    	    //6 bits cbIndex[3] 1 bit gainIndex[3] 1 bit gainIndex[4]
    	    result[7]=(byte)(cbIndex[3]&0xFC | ((gainIndex[3] & 0x10)>>3) | ((gainIndex[4] & 0x80)>>3));
    	    
    	    tempIndex7=8;
    	}
    	
    	//********************************************************
    	//filling class 2 bits
    	//********************************************************
    	
    	/* 4:th to 6:th WebRtc_Word16 for 20 ms case
    	5:th to 7:th WebRtc_Word16 for 30 ms case */
    	tempIndex6=0;
    	for (k=0; k<7; k++) 
    	{
    		result[tempIndex7]=0;
    		//take bit 3 for each 
    	    for (i=7; i>=0; i--) 
    	    	result[tempIndex7] |= (((idxVec[tempIndex6++] & 0x4)>>2)<<i);
    	    
    	    tempIndex7++;    	    
    	}
    	
    	result[tempIndex7] = (byte)((idxVec[tempIndex6++] & 0x4)<<5);
    	
    	if (encoderState.ENCODER_MODE==20) 
    	{
    	    /* 7:th WebRtc_Word16 */    		
    		result[tempIndex7] |= (gainIndex[1] & 0x4)<<4;
    		result[tempIndex7] |= (gainIndex[3] & 0xC)<<2;
    		result[tempIndex7] |= (gainIndex[4] & 0x4)<<1;
    		result[tempIndex7] |= (gainIndex[6] & 0x8)>>1;
    		result[tempIndex7] |= (gainIndex[7] & 0xC)>>2;
    	}
    	else
    	{
    		/* mode==30 */
    	    /* 8:th WebRtc_Word16 */
    		result[tempIndex7] |= (idxVec[tempIndex6++] & 0x4)<<4;
    	        	    
    		//add 6 more bits , cbIndex[0] - 2 , gainIndex[0] - 1,gainIndex[1] - 2,
    		result[tempIndex7] |= (cbIndex[0] & 0x6)<<3;   /* Bit 10..11 */
    		result[tempIndex7] |= (gainIndex[0] & 0x8);   /* Bit 12  */
    		result[tempIndex7] |= (gainIndex[1] & 0x4);   /* Bit 13  */
    		result[tempIndex7] |= (cbIndex[3] & 0x2);    /* Bit 14  */
    		result[tempIndex7] |= (cbIndex[6] & 0x80)>>7;   /* Bit 15  */
    	        		
    	    /* 9:th WebRtc_Word16 */
    		tempIndex7++;
    		result[tempIndex7] = (byte)((cbIndex[6] & 0x7E)<<1 | (cbIndex[9] & 0xC0)>>6);
    		
    		tempIndex7++;
    		result[tempIndex7] = (byte)((cbIndex[9] & 0x3E)<<2 | (cbIndex[12] & 0xE0)>>5);
    		    		    	    
    	    /* 10:th WebRtc_Word16 */
    		tempIndex7++;
    		result[tempIndex7] = (byte)((cbIndex[12] & 0x1E)<<3 | (gainIndex[3] & 0xC) | (gainIndex[4] & 0x6)>>1);
    		
    		tempIndex7++;
    		result[tempIndex7] = (byte)((gainIndex[6] & 0x18)<<3 | (gainIndex[7] & 0xC)<<2 | (gainIndex[9] & 0x10)>>1 | (gainIndex[10] & 0x8)>>1 | (gainIndex[12] & 0x10)>>3 | (gainIndex[13] & 0x8)>>3);    	    
    	}
    	
    	//********************************************************
    	//filling class 2 bits
    	//********************************************************
    	
    	/*  8:th to 14:th WebRtc_Word16 for 20 ms case
    	    11:th to 17:th WebRtc_Word16 for 30 ms case */
    	tempIndex6=0;
    	tempIndex7++;
    	for (k=0; k<14; k++) 
    	{
    		result[tempIndex7]=0;
    		//take bit 1,2 for each 
    	    for (i=6; i>=0; i-=2) 
    	    	result[tempIndex7] |= ((idxVec[tempIndex6++] & 0x3)<<i);
    	    
    	    tempIndex7++;
    	}
    	
    	if (encoderState.ENCODER_MODE==20) 
    	{
    	    /* 15:th WebRtc_Word16 */
    		result[tempIndex7++] =(byte)((idxVec[56]& 0x3)<<6 | (cbIndex[0] & 0x1)<<5 | (cbIndex[1] & 0x7C)>>2);
    		result[tempIndex7++] =(byte)((cbIndex[1] & 0x3)<<6 | (cbIndex[2] & 0x7E)>>1);
    			    	    
    	    /* 16:th WebRtc_Word16 */
    	    result[tempIndex7++] =(byte)((cbIndex[2] & 0x1)<<7 | (gainIndex[0] & 0x7)<<4 | (gainIndex[1] & 0x3)<<2 | (gainIndex[2] & 0x6)>>1);
    	    result[tempIndex7++] =(byte)((gainIndex[2] & 0x1)<<7 | (cbIndex[3] & 0x1)<<6 | (cbIndex[4] & 0x7E)>>1);  
    	    
    	    /* 17:th WebRtc_Word16 */
    	    result[tempIndex7++] = (byte)((cbIndex[4] & 0x1)<<7 | (cbIndex[5] & 0x7F));
    	    result[tempIndex7++] = (byte)(cbIndex[6] & 0xFF);
    	    
    	    /* 18:th WebRtc_Word16 */
    	    result[tempIndex7++] = (byte)(cbIndex[7] & 0xFF);
    	    result[tempIndex7++] = (byte)(cbIndex[8] & 0xFF);
    	    
    	    /* 19:th WebRtc_Word16 */
    	    result[tempIndex7++] = (byte)((gainIndex[3] & 0x3)<<6 | (gainIndex[4] & 0x3)<<4 | (gainIndex[5] & 0x7)<<1 | (gainIndex[6] & 0x4)>>2);
    	    result[tempIndex7++] = (byte)((gainIndex[6] & 0x3)<<6 | (gainIndex[7] & 0x3)<<4 | (gainIndex[8] & 0x7)<<1);     	    	    	    
    	} 
    	else 
    	{ /* mode==30 */
    	    /* 18:th WebRtc_Word16 */
    		result[tempIndex7++] = (byte)((idxVec[56] & 0x3)<<6 | (idxVec[57] & 0x3)<<4 | (cbIndex[0] & 0x1)<<3 | (cbIndex[1] & 0x70)>>4);
    		result[tempIndex7++] = (byte)((cbIndex[1] & 0xF)<<4 | (cbIndex[2] & 0x78)>>3); 
    		
    		/* 19:th WebRtc_Word16 */
    		result[tempIndex7++] = (byte)((cbIndex[2] & 0x7)<<5 | (gainIndex[0] & 0x7)<<2 | (gainIndex[1] & 0x3));
    		result[tempIndex7++] = (byte)((gainIndex[2] & 0x7)<<7 | (cbIndex[3] & 0x1)<<4 | (cbIndex[4] & 0x78)>>3);
    		
    		/* 20:th WebRtc_Word16 */
    		result[tempIndex7++] = (byte)((cbIndex[4] & 0x7)<<5 | (cbIndex[5] & 0x7C)>>2);
    		result[tempIndex7++] = (byte)((cbIndex[5] & 0x3)<<6 | (cbIndex[6] & 0x1)<<1 | (cbIndex[7] & 0xF8)>>3); 
    	    
    		/* 21:st WebRtc_Word16 */
    		result[tempIndex7++] = (byte)((cbIndex[7] & 0x7)<<5 | (cbIndex[8] & 0xF8)>>3);
    		result[tempIndex7++] = (byte)((cbIndex[8] & 0x7)<<5 | (cbIndex[9] & 0x1)<<4 | (cbIndex[10] & 0xF0)>>4);  
    		
    	    /* 22:nd WebRtc_Word16 */
    		result[tempIndex7++] = (byte)((cbIndex[10] & 0xF)<<4 | (cbIndex[11] & 0xF0)>>4);
    		result[tempIndex7++] = (byte)((cbIndex[11] & 0xF)<<4 | (cbIndex[12] & 0x1)<<3 | (cbIndex[13] & 0xE0)>>5);  
    	    
    		/* 23:rd WebRtc_Word16 */
    		result[tempIndex7++] = (byte)((cbIndex[13] & 0x1F)<<3 | (cbIndex[14] & 0xE0)>>5);  
    		result[tempIndex7++] = (byte)((cbIndex[14] & 0x1F)<<3 | (gainIndex[3] & 0x3)<<1 | (gainIndex[4] & 0x1));
    	    
    		/* 24:rd WebRtc_Word16 */
    		result[tempIndex7++] = (byte)((gainIndex[5] & 0x7)<<5 | (gainIndex[6] & 0x7)<<2 | (gainIndex[7] & 0x3)); 
    		result[tempIndex7++] = (byte)((gainIndex[8] & 0x7)<<5 | (gainIndex[9] & 0xF)<<1 | (gainIndex[10] & 0x4)>>2); 
    			
    	    /* 25:rd WebRtc_Word16 */
    		result[tempIndex7++] = (byte)((gainIndex[10] & 0x3)<<6 | (gainIndex[11] & 0x7)<<3 | (gainIndex[12] & 0xE)>>1); 
    		result[tempIndex7++] = (byte)((gainIndex[12] & 0x1)<<7 | (gainIndex[13] & 0x7)<<4 | (gainIndex[14] & 0x7)<<1);     		
    	}
    	/* Last bit is automatically zero */
    }
}
