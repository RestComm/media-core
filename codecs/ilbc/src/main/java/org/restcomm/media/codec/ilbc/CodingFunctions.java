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

/**
 * 
 * @author oifa yulian 
 */

public class CodingFunctions 
{
	private static final short[] emptyArray=new short[150];
	
	private static final short bestIndexMin=(short)-21299;
	private static final short bestIndexMax=(short)21299;
	private static final int bestIndexMinI=-21299;
	private static final int bestIndexMaxI=21299;
	
	public static void hpInput(EncoderState encoderState,short[] data,int startIndex,int length)
    {
    	short[] ba=Constants.HP_IN_COEFICIENTS;
    	short[] y=encoderState.getHpiMemY();
    	short[] x=encoderState.getHpiMemX();
    	
    	int currIndex=startIndex+length;
    	int current;
    	int i;
    	
    	for (i=startIndex; i<currIndex; i++) 
    	{    	   
    		current  = y[1]*ba[3];
    		current += y[3]*ba[4];
    		current = current>>15;
    		current += y[0]*ba[3];
    		current += y[2]*ba[4];
    		current = current<<1;
    	    
    		current += data[i]*ba[0];
    		current += x[0]*ba[1];
    		current += x[1]*ba[2];

    	    x[1] = x[0];
    	    x[0] = data[i];

    	    if(current>268431359)    	    	
    	    	data[i] = Short.MAX_VALUE;
    	    else if(current< -268439552)
    	    	data[i] = Short.MIN_VALUE;
   	    	else    	    		
   	    		data[i] = (short)((current + 4096)>>13);

    	    y[2] = y[0];
    	    y[3] = y[1];

    	    if (current>268435455)
    	    	current = Integer.MAX_VALUE;
    	    else if (current<-268435456)
    	    	current = Integer.MIN_VALUE;
    	    else
    	    	current = current<<3;    	    

    	    y[0] = (short)(current >> 16);
    	    y[1] = (short)((current - (y[0]<<16))>>1);    	    
        }
    }
	
	public static void hpOutput(short[] signal,short[] ba,short[] y,short[] x,short len)
    {
		int i;
		int temp,temp2,tempShift;
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
	
	public static void lpcEncode(EncoderState encoderState,EncoderBits encoderBits,short[] synthDenum,int synthDenumIndex,short[] weightDenum,int weightDenumIndex,short[] data,int startIndex)
    {
		//GC items : lsf,lsfDeq
		short[] lsf=new short[20];
        short[] lsfDeq=new short[20];                
    	
        simpleLpcAnalysis(encoderState,lsf,0,data,startIndex);    	
        simpleLsfQ(encoderBits,lsfDeq, 0, lsf, 0);        
        lsfCheck(lsfDeq , 0, 10);
        simpleInterpolateLsf(encoderState,synthDenum, synthDenumIndex, weightDenum, weightDenumIndex, lsf, 0, lsfDeq , 0, 10);    	    	
    }
    
	public static short[] simpleLpcAnalysis(EncoderState encoderState,short[] lsf,int lsfIndex,short[] data,int startIndex)
    {
    	  short[] A=new short[11];
    	  short[] windowedData=new short[240];
    	  short[] rc=new short[10];
    	  int[] R=new int[11];

    	  short[] lpcBuffer=encoderState.getLpcBuffer();    	  
    	  System.arraycopy(data, startIndex, lpcBuffer, 300-encoderState.SIZE, encoderState.SIZE);    	  

    	  int j=0;
    	  
    	  BasicFunctions.multWithRightShift(windowedData,0, lpcBuffer,60, Constants.LPC_ASYM_WIN, 0, 240, 15);    		  
		  autoCorrelation(windowedData,0, 240, 10, R, 0);
		  windowMultiply(R,0,R,0,Constants.LPC_LAG_WIN, 11);    		      		      	    		    		  
	      if (!levinsonDurbin(R, 0, A, 0, rc, 0, 10)) 
		  {
			  A[0]=4096;
			  for(j=1;j<11;j++)
				  A[j]=0;    			  
		  }

		  BasicFunctions.expand(A,0,A,0,Constants.LPC_CHIRP_SYNT_DENUM,11);    		      		      		  
	      poly2Lsf(lsf,lsfIndex, A,0);
	      
    	  System.arraycopy(lpcBuffer, encoderState.SIZE, lpcBuffer, 0, 300-encoderState.SIZE);
    	  return A;
    }
    
	public static void autoCorrelation(short[] input,int inputIndex,int inputLength, int order, int[] result,int resultIndex)
    {
		int i,j,nBits,scale,currIndex1,currIndex2;
        short max=0,tempS;
        
    	if (order < 0)
            order = inputLength;

    	for(i=0;i<inputLength;i++)
    	{
    		tempS=BasicFunctions.abs(input[inputIndex++]);
    		if(tempS>max)
    			max=tempS;
    	}

    	inputIndex-=inputLength;    	
        if (max == 0)        
        	scale = 0;
        else
        {        	
            nBits = BasicFunctions.getSize(inputLength);
            tempS = BasicFunctions.norm(max*max);
                        
            if (tempS > nBits)
            	scale = 0;
            else
            	scale = (short)(nBits - tempS);                        
        }
             
        for (i = 0; i < order + 1; i++)
        {
        	result[resultIndex] = 0;
        	currIndex1=inputIndex;
        	currIndex2=inputIndex+i;
            for (j = inputLength - i; j > 0; j--)
            	result[resultIndex] += ((input[currIndex1++]*input[currIndex2++])>>scale);
            
    		resultIndex++;
        } 
    }

	public static void windowMultiply(int[] output,int outputIndex,int[] input,int inputIndex,int[] window,int length)
    {
		  int i;
		  short xHi,xLow,yHi,yLow;
    	  int nBits = BasicFunctions.norm(input[inputIndex]);
    	  BasicFunctions.bitShiftLeft(input, inputIndex, input, inputIndex,length, nBits);    	  		      	  
	    	  
    	  for (i = 0; i < length; i++) 
    	  {
    	    xHi = (short)(input[inputIndex]>>16);
    	    yHi = (short)(window[i]>>16);

    	    xLow = (short)((input[inputIndex++] - (xHi<<16))>>1);
    	    yLow = (short)((window[i] - (yHi<<16))>>1);

    	    output[outputIndex]=(xHi*yHi)<<1;
    	    output[outputIndex]+=(xHi*yLow)>>14;
    	    output[outputIndex++]+=(xLow*yHi)>>14;    	        	    
    	  }    	      	  		
    	  
    	  outputIndex-=length;
    	  BasicFunctions.bitShiftRight(output, outputIndex, output, outputIndex,length, nBits);    	  
    }
    
	public static boolean levinsonDurbin(int[] R,int rIndex,short[] A,int aIndex,short[] K,int kIndex,int order)
    {
    	short[] rHi=new short[21];
    	short[] rLow=new short[21];
    	short[] aHi=new short[21];
    	short[] aLow=new short[21];
    	short[] aUpdHi=new short[21];
    	short[] aUpdLow=new short[21];
    	
        int nBits = BasicFunctions.norm(R[rIndex]);
        int alphaExp;
        int temp,temp2,temp3;
        int i,j;
        short tempS,tempS2;
        short yHi,yLow,xHi,xLow;
        
        int currIndex=rIndex+order;
        for (i = order; i >= 0; i--)
        {
            temp = R[currIndex--]<<nBits;
            rHi[i] = (short)(temp>>16);
            rLow[i] = (short)((temp - (rHi[i]<<16))>>1);
        }

        temp2 = rHi[1]<<16; 
        temp3 = rLow[1]<<1;
        temp2 += temp3;
        
        if(temp2>0)
        	temp3=temp2;
        else
        	temp3=-temp2;                        
        
        temp = BasicFunctions.div(temp3, rHi[0], rLow[0]);        
        if (temp2 > 0)
            temp = -temp;                
        
        xHi = (short)(temp>>16);
        xLow = (short)((temp - (xHi<<16))>>1);

        K[kIndex++] = xHi;
        temp = temp>>4;
    	       	
        aHi[1] = (short)(temp>>16);
        aLow[1] = (short)((temp - (aHi[1]<<16))>>1);
        
        temp=(xHi*xLow) >> 14;
    	temp+=xHi*xHi;
    	temp<<=1;    	
    	
        if(temp<0)
        	temp=0-temp;
                
        temp = Integer.MAX_VALUE - temp;
        
        tempS = (short)(temp>>16);
        tempS2 = (short)((temp - (tempS<<16))>>1);

        temp=rHi[0]*tempS;
        temp+=(rHi[0]*tempS2) >> 15;
        temp+=(rLow[0]*tempS) >> 15;
        temp <<= 1;

        alphaExp = BasicFunctions.norm(temp);
        temp = temp<<alphaExp;
        yHi = (short)(temp>>16);
        yLow = (short)((temp - (yHi<<16))>>1);
        
        for (i = 2; i <= order; i++)
        {
            temp = 0;
            currIndex=i-1;
            for (j = 1; j < i; j++)
            {
                temp2=(rHi[j]*aLow[currIndex]) >> 15;
            	temp2+=(rLow[j]*aHi[currIndex]) >> 15;
        		temp+=temp2<<1;        		
            	temp+=(rHi[j]*aHi[currIndex]) << 1;
            	currIndex--;
            }

            temp = temp<<4;
            temp+=rHi[i]<<16;
            temp += rLow[i]<<1;            
            
            temp2 = Math.abs(temp);   
            temp3 = BasicFunctions.div(temp2, yHi, yLow);            
            
            if (temp > 0)
                temp3 = -temp3;
            
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
               
            xHi = (short)(temp3>>16);
            xLow = (short)((temp3 - (xHi<<16))>>1);

            K[kIndex++] = xHi;
            
            if (BasicFunctions.abs(xHi) > 32750)
            	return false; // Unstable filter
            
            currIndex=i-1;
            currIndex=i-1;
            for (j = 1; j < i; j++)
            {
                temp=aHi[j]<<16;
            	temp += aLow[j]<<1;
                temp2=(xLow*aHi[currIndex]) >> 15;
            	temp2+=(xHi*aLow[currIndex]) >> 15;
            	temp2+=xHi*aHi[currIndex];            	
                temp += temp2 << 1;
                
                aUpdHi[j] = (short)(temp>>16);
                aUpdLow[j] = (short)((temp - (aUpdHi[j]<<16))>>1);
                currIndex--;
            }

            temp3 = temp3>>4;
            
            aUpdHi[i] = (short)(temp3>>16);
            aUpdLow[i] = (short)((temp3 - (aUpdHi[i]<<16))>>1);

            temp = (((xHi*xLow) >> 14) + xHi*xHi) << 1;
            if(temp<0)
            	temp = 0-temp;
            
            temp = Integer.MAX_VALUE - temp;            
            tempS = (short)(temp>>16);
            tempS2 = (short)((temp - (tempS<<16))>>1);
            
            temp2=(yHi*tempS2) >> 15;
            temp2+=(yLow*tempS) >> 15;        	
        	temp2+=yHi*tempS;
            temp=temp2<<1;
            
            nBits = BasicFunctions.norm(temp);
            temp = temp<<nBits;            
            
            yHi = (short)(temp>>16);
            yLow = (short)((temp - (yHi<<16))>>1);
            alphaExp = (short)(alphaExp + nBits);
            
            for (j = 1; j <= i; j++)
            {
            	aHi[j] = aUpdHi[j];
                aLow[j] = aUpdLow[j];                
            }                                   
        }

        A[aIndex++] = 4096;        
        for (i = 1; i <= order; i++)
        {
            temp=aHi[i]<<16;
        	temp +=aLow[i]<<1;
            temp<<=1;
            temp+=32768;
            A[aIndex++] = (short)(temp>>16);
        }
        
        return true;
    }   
	
	public static void simpleInterpolateLsf(EncoderState encoderState,short[] synthdenum,int synthDenumIndex,short[] weightDenum,int weightDenumIndex,short[] lsf,int lsfIndex,short[] lsfDeq,int lsfDeqIndex,int length)
    {
    	short[] lsfOld=encoderState.getLsfOld();
    	short[] lsfDeqOld=encoderState.getLsfDeqOld();
    	
    	short[] lp=new short[11];
    	
    	int step=length + 1;    	
    	int index = 0;
    	int i;
    	
	    for (i = 0; i < encoderState.SUBFRAMES; i++) 
	    {
	    	lsfInterpolate2PolyEnc(lp, (short)0 ,lsfDeqOld, 0, lsfDeq, lsfDeqIndex , Constants.LSF_WEIGHT_20MS[i], length);    	    	
	    	System.arraycopy(lp, 0, synthdenum, synthDenumIndex + index, step);    	      

	    	lsfInterpolate2PolyEnc(lp, (short)0 , lsfOld, 0, lsf, lsfIndex, Constants.LSF_WEIGHT_20MS[i], length);
	    	BasicFunctions.expand(weightDenum,weightDenumIndex + index,lp,0,Constants.LPC_CHIRP_WEIGHT_DENUM,step);    	      

	    	index += step;
	    }

	    System.arraycopy(lsf, lsfIndex, lsfOld, 0, length);
	    System.arraycopy(lsfDeq, lsfDeqIndex, lsfDeqOld, 0, length);  	   
    }
	
    public static void lsfInterpolate2PolyEnc(short[] a, short aIndex, short[] lsf1, int lsf1Index, short[] lsf2, int lsf2Index, short coef, int length)
    {
		//GC items : lsfTemp
		short[] lsfTemp=new short[10];    	
    	interpolate(lsfTemp, 0, lsf1, lsf1Index, lsf2, lsf2Index, coef, length);
    	lsf2Poly(a, aIndex, lsfTemp, 0);
    }
    
	public static void interpolate(short[] out,int outIndex,short[] in1,int in1Index,short[] in2,int in2Index,short coef,int length)
    {
		int k;		
    	short tempS = (short)(16384 - coef);
    	for (k = 0; k < length; k++) 
    	    out[outIndex++] = (short) ((coef*in1[in1Index++] + tempS*in2[in2Index++]+8192)>>14);    	
    }
     
	public static void lsf2Poly(short[] a,int aIndex,short[] lsf,int lsfIndex)
    {
		int k;
		
		//GC items : f1,f2
		int[] f1=new int[6];
    	int[] f2=new int[6];
    	
    	short[] lsp=new short[10];    	    
    	
    	lsf2Lsp(lsf, lsfIndex, lsp, 0, 10);    	    	           	
    	
    	getLspPoly(lsp, 0, f1, 0);
    	getLspPoly(lsp, 1, f2, 0);

    	for (k=5; k>0; k--)
    	{
    		f1[k]+=f1[k-1];
    		f2[k]-=f2[k-1];    		
    	}

    	a[aIndex]=4096;
    	
    	int aStartIndex=aIndex+1;
    	int aEndIndex=aIndex+10;
    	
    	int currIndex=1;
    	for (k=5; k>0; k--)
    	{
    		a[aStartIndex++] = (short)(((f1[currIndex] + f2[currIndex])+4096)>>13);
    		a[aEndIndex--] = (short)(((f1[currIndex] - f2[currIndex])+4096)>>13);
    		currIndex++;    		
    	}
    }
    
	public static void lsf2Lsp(short[] lsf,int lsfIndex,short[] lsp,int lspIndex,int count)
    {
    	int j;
    	short tempS,tempS2;
    	
    	for(j=0; j<count; j++)
    	{
    		tempS = (short)((lsf[lsfIndex++]*20861)>>15);
    	    tempS2 = (short)(tempS>>8);
    	    tempS = (short)(tempS & 0x00ff);

    	    if (tempS2>63 || tempS2<0)
    	    	tempS2 = 63;    

    	    lsp[lspIndex++] = (short)(((Constants.COS_DERIVATIVE[tempS2]*tempS)>>12) + Constants.COS[tempS2]);
    	}
    }
    
    public static void getLspPoly(short[] lsp,int lspIndex,int[] f,int fIndex)
    {
    	int j,k;
    	short xHi,xLow;
    	
    	f[fIndex++]=16777216;
    	f[fIndex++] = lsp[lspIndex]*(-1024);
    	lspIndex+=2;

    	for(k=2; k<=5; k++)
    	{
    		f[fIndex]=f[fIndex-2];    		

    	    for(j=k; j>1; j--)
    	    {
    	    	xHi = (short)(f[fIndex-1]>>16);
    	    	xLow = (short)((f[fIndex-1]-(xHi<<16))>>1);

    	    	f[fIndex] += f[fIndex-2];    	    	
    	    	f[fIndex--] -= ((xHi*lsp[lspIndex])<<2) + (((xLow*lsp[lspIndex])>>15)<<2);         	    	    	    	
    	    }
    	    
    	    f[fIndex] -= lsp[lspIndex]<< 10;
    	    
    	    fIndex+=k;
    	    lspIndex+=2;    	        	        	   
    	}
    }
    
    public static void poly2Lsf(short[] lsf,int lsfIndex,short[] A,int aIndex)
    {
    	//GC : lsp
    	short[] lsp=new short[10];
    	poly2Lsp(A, aIndex, lsp, 0);    	
    	lspToLsf(lsp, 0,lsf, lsfIndex, 10);    	        	
    }
    
    public static void poly2Lsp(short[] A,int aIndex,short[] lsp,int lspIndex)
    {
    	short[] f1=new short[6]; 
    	short[] f2=new short[6];
    	
    	short xMid,xLow,xHi,yMid,yLow,yHi,x,y;
    	int temp;
    	int i,j;
    	int nBits;
    	
    	int aLowIndex=aIndex + 1;
    	int aHighIndex=aIndex + 10;
    	
    	f1[0]=1024;
    	f2[0]=1024;
    	
    	for (i = 0; i < 5; i++) 
    	{
    		f1[i+1] = (short)(((A[aLowIndex] + A[aHighIndex])>>2) - f1[i]);    	    
    	    f2[i+1] = (short)(((A[aLowIndex] - A[aHighIndex])>>2) + f2[i]);        	        	
        	
    	    aLowIndex++;
        	aHighIndex--;        	
    	}

    	short[] current=f1;
    	int currIndex = lspIndex;
    	int foundFreqs = 0;

    	xLow = Constants.COS_GRID[0];
    	yLow = chebushev(xLow, current, 0);
    	
    	for (j = 1; j < Constants.COS_GRID.length && foundFreqs < 10; j++) 
    	{
    	    xHi = xLow;
    	    yHi = yLow;
    	    xLow = Constants.COS_GRID[j];
    	    yLow = chebushev(xLow, current, 0);    	    
    	    
    	    if (yLow*yHi <= 0) 
    	    {
    	    	for (i = 0; i < 4; i++) 
    	    	{
    	    		xMid = (short)((xLow>>1) + (xHi>>1));
    	    		yMid = chebushev(xMid, current, 0);
    	    		
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

    	    	x = (short)(xHi - xLow);
    	    	y = (short)(yHi - yLow);
    	    	
    	    	if (y == 0)
    	    		lsp[currIndex++] = xLow;
    	      	else 
    	      	{
    	      		temp = y;
    	      		y = BasicFunctions.abs(y);
    	      		nBits = (short)(BasicFunctions.norm(y)-16);
    	      		y = (short)(y<<nBits);
    	      		
    	      		if (y != 0)
    	      	        y=(short)(536838144 / y);
    	      	    else
    	      	    	y=(short)Integer.MAX_VALUE;    	      	    

    	      		y = (short)(((x*y)>>(19-nBits))&0xFFFF);

    	      		if (temp < 0)
    	      			y = (short)-y;    	        
    	        
    	      		lsp[currIndex++] = (short)(xLow-(((yLow*y)>>10)&0xFFFF));
    	      	}

    	    	foundFreqs++;
    	    	if (foundFreqs<10) 
    	    	{
    	    		xLow = lsp[currIndex-1];
    	    		if((foundFreqs%2)==0)
    	    			current=f1;
    	    		else
    	    			current=f2;    	        

    	    		yLow = chebushev(xLow, current, 0);
    	    	}
    	    }
    	}

    	if (foundFreqs < 10)   
    		System.arraycopy(Constants.LSP_MEAN, 0, lsp, lspIndex, 10);    	
    }
    
    public static short chebushev(short value,short[] coefs,int coefsIndex)
    {   
    	int n,b2;
    	short b1Hi,b1Low;
    	int temp,temp2;
    	
    	b2 = 0x1000000;
    	temp = value<<10;
    	coefsIndex++;
    	temp += coefs[coefsIndex++]<<14;
    	
    	for (n = 2; n < 5; n++) 
    	{
    	    temp2 = temp;

    	    b1Hi = (short)(temp>>16);
    	    b1Low = (short)((temp-(b1Hi<<16))>>1);    	    
    	    
    	    temp=(((b1Low*value)>>15) + (b1Hi*value))<<2;    		
    	    temp -= b2;
    	    temp += coefs[coefsIndex++]<<14;

    	    b2 = temp2;    	        	    
    	}

    	b1Hi = (short)(temp>>16);
    	b1Low = (short)((temp-(b1Hi<<16))>>1);

    	temp=(((b1Low*value)>>15)<<1) + ((b1Hi*value)<<1);    	
    	temp -= b2;
    	temp += coefs[coefsIndex]<<13;
    	
    	if (temp>33553408) 
    	  	return Short.MAX_VALUE;
    	else if (temp<-33554432)
    		return Short.MIN_VALUE;
    	else
    		return (short)(temp>>10);    	  
    }
    
    public static void lspToLsf(short[] lsp, int lspIndex,short[] lsf,int lsfIndex,int coefsNumber)
    {
    	int i;
      	int j = 63;
      	int currValue;
      	
    	int currLspIndex=lspIndex+coefsNumber-1;  
    	int currLsfIndex=lsfIndex+coefsNumber-1;    	
    	    	
    	for(i=coefsNumber-1; i>=0; i--)
    	{
    		while(Constants.COS[j]<lsp[currLspIndex] && j>0)
    			j--;
    	    
    		currValue = (j<<9)+((Constants.ACOS_DERIVATIVE[j]*(lsp[currLspIndex--]-Constants.COS[j]))>>11);
    	    lsf[currLsfIndex--] = (short)((currValue*25736)>>15);    	        		    	        	        	   
    	  }
    }
    
    public static void simpleLsfQ(EncoderBits encoderBits,short[] lsfdeq,int lsfdeqIndex,short[] lsfArray,int lsfArrrayIndex)
    {
    	splitVq(lsfdeq, lsfdeqIndex,encoderBits.getLSF(), 0, lsfArray, lsfArrrayIndex);    	    
    }
    
    public static void splitVq(short[] qX,int qXIndex,short[] lsf,int lsfIndex,short[] X,int xIndex)
    {
    	vq3(qX, qXIndex, lsf, lsfIndex, Constants.LSF_INDEX_CB[0], X, xIndex, Constants.LSF_SIZE_CB[0]);
    	vq3(qX, qXIndex + Constants.LSF_DIM_CB[0], lsf, lsfIndex+1, Constants.LSF_INDEX_CB[1], X, xIndex + Constants.LSF_DIM_CB[0], Constants.LSF_SIZE_CB[1]);    	
    	vq4(qX, qXIndex + Constants.LSF_DIM_CB[0] + Constants.LSF_DIM_CB[1], lsf, lsfIndex+2, Constants.LSF_INDEX_CB[2], X, xIndex + Constants.LSF_DIM_CB[0] + Constants.LSF_DIM_CB[1], Constants.LSF_SIZE_CB[2]);
    }       

    public static void vq3(short[] qX,int qXIndex,short[] lsf,int lsfIndex,int cbIndex,short[] X,int xIndex,int cbSize)
    {
    	int minValue = Integer.MAX_VALUE;
    	int i,j,temp; 
    	short tempS;
    	int currIndex=0;
    	
    	for (j = 0; j < cbSize; j++) 
    	{
    		tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
    	    temp = tempS*tempS;
    	    tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
    	    temp+=tempS*tempS;
	    	tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
	    	temp+=tempS*tempS;	    		    	    	        	  

	    	xIndex-=3;
    	    if (temp < minValue) 
    	    {
    	    	minValue = temp;
    	    	currIndex=j;    	    	    	    	
    	    }    	        	   
    	}

    	cbIndex-=3*cbSize;
    	lsf[lsfIndex] = (short)currIndex;
    	currIndex*=3;
    	currIndex+=cbIndex;
    	for (i = 0; i < 3; i++)
    	    qX[qXIndex++] = Constants.LSF_CB[currIndex++];    	      	      	
    }
    
    public static void vq4(short[] qX,int qXIndex,short[] lsf,int lsfIndex,int cbIndex,short[] X,int xIndex,int cbSize)
    {
    	int minValue = Integer.MAX_VALUE;
    	int i,j,temp; 
    	short tempS;
    	
    	int currIndex=0;
    	for (j = 0; j < cbSize; j++) 
    	{
    		tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
    	    temp = tempS*tempS;
    	    for (i = 1; i < 4; i++) 
    	    {
    	    	tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
    	    	temp += tempS*tempS;
    	    }

    	    xIndex-=4;
    	    if (temp < minValue) 
    	    {
    	    	minValue = temp;
    	    	currIndex=j;
    	    }    	        	   
    	}

    	cbIndex-=4*cbSize;
    	lsf[lsfIndex] = (short)currIndex;
      	currIndex*=4;
      	currIndex+=cbIndex;
      	for (i = 0; i < 4; i++)
      	    qX[qXIndex++] = Constants.LSF_CB[currIndex++];      	
    }
    
    public static void lsfCheck(short[] lsf,int lsfIndex,int lsfSize)
    {
    	int n,k;
    	int currIndex1,currIndex2;
    	for (n=0;n<2;n++) 
    	{
    		for (k=0;k<lsfSize-1;k++) 
	    	{
	    		currIndex1=lsfIndex + k;
	    		currIndex2=currIndex1+1;
	    		
	    		if ((lsf[currIndex2]-lsf[currIndex1])<Constants.EPS) 
	    		{
	    			if (lsf[currIndex2]<lsf[currIndex1]) 
	    			{
	    				lsf[currIndex2]= (short)(lsf[currIndex1]+Constants.HALF_EPS);
	    				lsf[currIndex1]= (short)(lsf[currIndex2]-Constants.HALF_EPS);
	    			} 
	    			else 
	    			{
	    				lsf[currIndex1]-=Constants.HALF_EPS;
	    				lsf[currIndex2]+=Constants.HALF_EPS;
	    			}
	    		}

	    		if (lsf[currIndex1]<Constants.MIN_LSF) 
	    			lsf[currIndex1]=Constants.MIN_LSF;
	    		
	    		if (lsf[currIndex1]>Constants.MAX_LSF) 
	    			lsf[currIndex1]=Constants.MAX_LSF;    	    			    	    		
	    	}
    	}
    }
    
	public static short gainDequant(short index, short maxIn, short stage)
    {
    	if(maxIn<0)
    		maxIn=(short)(0-maxIn);
    	
    	if(maxIn<1638)
    		maxIn=1638;    	    	    	

    	return (short)((maxIn*Constants.GAIN[stage][index] +8192)>>14);
    }        
        
	public static short gainQuant(short gain,short maxIn,short stage,short[] index,int indexIndex)
    {
		int scale,cbIndex,n,temp,temp2,nBits;
    	short[] cb;    	
    	
    	if(maxIn>1638)
    		scale = maxIn;
    	else
    		scale = 1638;

    	cb = Constants.GAIN[stage];
    	temp = gain<<14;

    	cbIndex=(32>>stage) >> 1;
    	nBits = (short)cbIndex;
    	for (n=4-stage;n>0;n--) 
    	{
    		nBits>>=1;
    	    if (temp > scale*cb[cbIndex]) 
    	    	cbIndex+=nBits;
    	    else 
    	    	cbIndex-=nBits;    	        	   
    	}

    	temp2=scale*cb[cbIndex];
    	if (temp>temp2) 
    	{
    	    if ((scale*cb[cbIndex+1]-temp)<(temp-temp2))
    	    	cbIndex++;    	    
    	}
    	else if ((temp-scale*cb[cbIndex-1])<=(temp2-temp)) 
    	   	cbIndex--;    	    
    	
    	temp=(32>>stage) - 1;
    	if(cbIndex>temp)
    		cbIndex=temp;
    	
    	index[indexIndex]=(short)cbIndex;    	
    	return (short)((scale*cb[cbIndex]+8192)>>14); 
    }    
	
	public static void cbMemEnergyAugmentation(short[] interpSamples, int interpSamplesIndex, short[] cbMem, int cbMemIndex, short scale, short baseSize, short[] energy, int energyIndex, short[] energyShifts, int energyShiftsIndex)    	    
    {
		energyIndex = energyIndex + baseSize-20;
    	energyShiftsIndex = energyShiftsIndex + baseSize-20;
    	cbMemIndex = cbMemIndex + 147;    	

    	int en1 = BasicFunctions.scaleRight(cbMem,cbMemIndex-19,cbMem,cbMemIndex-19,15,scale);    		
    	int currIndex=cbMemIndex - 20;
    	int currValue;
    	int n;
    	
    	for (n=20; n<=39; n++) 
    	{
    	    en1 += (cbMem[currIndex]*cbMem[currIndex])>>scale;
    		currIndex--;
    		currValue = en1;

    	    /* interpolation */
    		currValue += BasicFunctions.scaleRight(interpSamples,interpSamplesIndex,interpSamples,interpSamplesIndex,4,scale);        	    	
    	    interpSamplesIndex += 4;

    	    /* Compute energy for the remaining samples */
    	    currValue += BasicFunctions.scaleRight(cbMem,cbMemIndex - n,cbMem,cbMemIndex - n,40-n,scale);    	    	

    	    /* Normalize the energy and store the number of shifts */
    	    energyShifts[energyShiftsIndex] = BasicFunctions.norm(currValue);
    	    currValue = currValue<<energyShifts[energyShiftsIndex++];
    	    
    	    energy[energyIndex++] = (short)(currValue>>16);    	    
    	}
    }
	
	public static void cbMemEnergy(short range,short[] cb,int cbIndex,short[] filteredCB,int filteredCbIndex, short length, short targetLength, short[] energy, int energyIndex, short[] energyShifts, int energyShiftsIndex, short scale, short baseSize)
    {
		int currValue = BasicFunctions.scaleRight(cb, cbIndex + length - targetLength, cb, cbIndex + length - targetLength, targetLength, scale);
    	energyShifts[energyShiftsIndex] = (short)BasicFunctions.norm(currValue);
    	energy[energyIndex] = (short)((currValue<<energyShifts[energyShiftsIndex])>>16);
    	energyCalc(currValue, range, cb, cbIndex + length - targetLength - 1, cb, cbIndex + length - 1, energy, energyIndex, energyShifts, energyShiftsIndex, scale, (short)0);
    	
    	currValue = BasicFunctions.scaleRight(filteredCB, filteredCbIndex + length - targetLength, filteredCB, filteredCbIndex + length - targetLength, targetLength, scale);    	
    	energyShifts[baseSize + energyShiftsIndex]=BasicFunctions.norm(currValue);    	
    	energy[baseSize + energyIndex] = (short)((currValue<<energyShifts[baseSize + energyShiftsIndex])>>16);
    	energyCalc(currValue, range, filteredCB, filteredCbIndex + length - targetLength - 1, filteredCB, filteredCbIndex + length - 1, energy, energyIndex, energyShifts, energyShiftsIndex, scale, baseSize);    	    		    
    }        
	
	public static void cbSearch(EncoderState encoderState,EncoderBits encoderBits,CbSearchData searchData,CbUpdateIndexData updateIndexData,short[] inTarget,int inTargetIndex,short[] decResidual,int decResidualIndex,int length,int vectorLength,short[] weightDenum,int weightDenumindex, int blockNumber,int cbIndexIndex,int gainIndexIndex)
    {
		//GC items : gains,cbBuf,energyShifts,targetVec,cbVectors,codedVec,interpSamples,interSamplesFilt,energy,augVec
		
    	short[] cbIndex=encoderBits.getCbIndex();
    	short[] gainIndex=encoderBits.getGainIndex();
    	
    	/* Stack based */
    	short[] gains=new short[4];
    	short[] cbBuf=new short[161];   
    	short[] energyShifts=new short[256];
    	short[] targetVec=new short[50];
    	short[] cbVectors=new short[147];
    	short[] codedVec=new short[40];
    	short[] interpSamples=new short[80];
    	short[] interSamplesFilt=new short[80];
    	short[] energy=new short[256];    	
    	short[] augVec=new short[256];
    	
    	short[] inverseEnergy=energy;
    	short[] inverseEnergyShifts=energyShifts;
    	short[] buf=cbBuf;//+10 length 151
    	short[] target=targetVec;//+10 length 40    	

    	int[] cDot=new int[128];
    	int[] crit=new int[128];

    	short[] pp;
    	int ppIndex,sInd,eInd,targetEner,codedEner,gainResult;
    	int stage,i,j,nBits;
    	short scale,scale2;
    	short range,tempS,tempS2;
    	
    	short baseSize=(short)(length-vectorLength+1);
    	if (vectorLength==40)
    	    baseSize=(short)(length-19);    	  
    	
    	int numberOfZeroes=length-Constants.FILTER_RANGE[blockNumber];
    	BasicFunctions.filterAR(decResidual, decResidualIndex + numberOfZeroes, buf, 10+numberOfZeroes, weightDenum, weightDenumindex, 11, Constants.FILTER_RANGE[blockNumber]);    	
    	System.arraycopy(cbBuf, length, targetVec, 0, 10);
    	
    	BasicFunctions.filterAR(inTarget, inTargetIndex, target, 10, weightDenum, weightDenumindex, 11, vectorLength);    	
    	System.arraycopy(target, 10, codedVec, 0, vectorLength);  	

    	int currIndex=10;    	
    	tempS=0;
    	for(i=0;i<length;i++)
    	{
    		if(buf[currIndex]>0 && buf[currIndex]>tempS)
    			tempS=buf[currIndex];
    		else if((0-buf[currIndex])>tempS)
    			tempS=(short)(0-buf[currIndex]);
    		
    		currIndex++;
    	}
    	
    	currIndex=10;
    	tempS2=0;
    	for(i=0;i<vectorLength;i++)
    	{
    		if(target[currIndex]>0 && target[currIndex]>tempS2)
    			tempS2=target[currIndex];
    		else if((0-target[currIndex])>tempS2)
    			tempS2=(short)(0-target[currIndex]);
    		
    		currIndex++;
    	}    	 

    	if ((tempS>0)&&(tempS2>0)) 
    	{
    		if(tempS2>tempS)
    			tempS = tempS2;
    		
    	    scale = BasicFunctions.getSize(tempS*tempS);
    	} 
    	else 
    	    scale = 30;
    	
    	scale = (short)(scale - 25);
    	if(scale<0)
    		scale=0;    	

    	scale2=scale;
    	targetEner = BasicFunctions.scaleRight(target,10,target,10,vectorLength, scale2);    	 
    	
    	filteredCBVecs(cbVectors, 0, buf, 10, length,Constants.FILTER_RANGE[blockNumber]);
    	
    	range = Constants.SEARCH_RANGE[blockNumber][0];    	
    	if(vectorLength == 40) 
    	{
    		interpolateSamples(interpSamples,0,buf,10,length);
    		interpolateSamples(interSamplesFilt,0,cbVectors,0,length);    	    
    	    
    		cbMemEnergyAugmentation(interpSamples, 0, buf, 10, scale2, (short)20, energy, 0, energyShifts, 0);    	    
    		cbMemEnergyAugmentation(interSamplesFilt, 0, cbVectors, 0, scale2, (short)(baseSize+20), energy, 0, energyShifts, 0);    	    

    		cbMemEnergy(range, buf, 10, cbVectors, 0, (short)length, (short)vectorLength, energy, 20, energyShifts, 20, scale2, baseSize);    	        	    	
    	}
    	else 
    		cbMemEnergy(range, buf, 10, cbVectors, 0, (short)length, (short)vectorLength, energy, 0, energyShifts, 0, scale2, baseSize);
    	
    	energyInverse(energy,0,baseSize*2);
    	
    	gains[0] = 16384;
    	for (stage=0; stage<3; stage++) 
    	{
    	    range = Constants.SEARCH_RANGE[blockNumber][stage];

    	    /* initialize search measures */
    	    updateIndexData.setCritMax(0);
    	    updateIndexData.setShTotMax((short)-100);
    	    updateIndexData.setBestIndex((short)0);
    	    updateIndexData.setBestGain((short)0);    	    

    	    /* Calculate all the cross correlations (augmented part of CB) */
    	    if (vectorLength==40) 
    	    {
    	    	augmentCbCorr(target, 10, buf, 10+length, interpSamples, 0, cDot, 0,20, 39, scale2);    	    	    	    	
    			currIndex=20;
    	    } 
    	    else 
    	    	currIndex=0;
    	    
    	    crossCorrelation(cDot, currIndex, target, 10, buf, 10 + length - vectorLength, (short)vectorLength, range, scale2, (short)-1);    	        	    
        	
    	    if (vectorLength==40)
    	      range=(short)(Constants.SEARCH_RANGE[blockNumber][stage]+20);
    	    else
    	      range=Constants.SEARCH_RANGE[blockNumber][stage];    	    

    	    cbSearchCore(searchData,cDot, 0, range, (short)stage, inverseEnergy, 0, inverseEnergyShifts, 0, crit, 0);
    	    updateBestIndex(updateIndexData,searchData.getCritNew(),searchData.getCritNewSh(),searchData.getIndexNew(),cDot[searchData.getIndexNew()],inverseEnergy[searchData.getIndexNew()],inverseEnergyShifts[searchData.getIndexNew()]);    	        	    
    	    
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
    	    			augmentCbCorr(target, 10, cbVectors, length, interSamplesFilt, 0, cDot, 0, sInd+20, 39, scale2);
    	    		else
    	    			augmentCbCorr(target, 10, cbVectors, length, interSamplesFilt, 0, cDot, 0, sInd+20, eInd+20, scale2);
    	    		
    	    		i=20;
    	    	}

    	    	if(20-sInd>0)
    	    		currIndex=20-sInd;
    	    	else
    	    		currIndex=0;
    	    	
    	    	crossCorrelation(cDot, currIndex, target, 10, cbVectors, length - 20 - i, (short)vectorLength, (short)(eInd-i+1), scale2, (short)-1);    	    	
    	    } 
    	    else 
    	    	crossCorrelation(cDot, 0, target, 10, cbVectors, length - vectorLength - sInd, (short)vectorLength, (short)(eInd-sInd+1), scale2, (short)-1);    	    	
    	    
    	    cbSearchCore(searchData,cDot, 0, (short)(eInd-sInd+1), (short)stage, inverseEnergy, baseSize+sInd, inverseEnergyShifts, baseSize+sInd, crit, 0);    	    
    	    updateBestIndex(updateIndexData,searchData.getCritNew(),searchData.getCritNewSh(),(short)(searchData.getIndexNew()+baseSize+sInd),cDot[searchData.getIndexNew()],inverseEnergy[searchData.getIndexNew()+baseSize+sInd],inverseEnergyShifts[searchData.getIndexNew()+baseSize+sInd]);    	    	

    	    cbIndex[cbIndexIndex+stage] = updateIndexData.getBestIndex();
    	    if(gains[stage]>0)    	    	
    	    	updateIndexData.setBestGain(gainQuant(updateIndexData.getBestGain(), gains[stage], (short)stage, gainIndex, gainIndexIndex+stage));
    	    else
    	    	updateIndexData.setBestGain(gainQuant(updateIndexData.getBestGain(), (short)(0-gains[stage]), (short)stage, gainIndex, gainIndexIndex+stage));

    	    if(vectorLength == 80-encoderState.STATE_SHORT_LEN) 
    	    {
    	    	if(cbIndex[cbIndexIndex+stage]<baseSize)
    	    	{
    	    		pp=buf;
    	    		ppIndex = 10+length-vectorLength-cbIndex[cbIndexIndex+stage];
    	    	}
    	    	else
    	    	{
    	    		pp=cbVectors;
    	    		ppIndex = length-vectorLength- cbIndex[cbIndexIndex+stage]+baseSize;
    	    	}
    	    	} 
    	    else 
    	    { 
    	    	if (cbIndex[cbIndexIndex+stage]<baseSize) 
    	    	{    	    		
    	    		if (cbIndex[cbIndexIndex+stage]>=20) 
    	    		{
    	    			cbIndex[cbIndexIndex+stage]-=20;
    	    			pp=buf;
    	    			ppIndex = 10+length-vectorLength-cbIndex[cbIndexIndex+stage];
    	    		} 
    	    		else 
    	    		{
    	    			cbIndex[cbIndexIndex+stage]+=(baseSize-20);
    	    			createAugmentVector((short)(cbIndex[cbIndexIndex+stage]-baseSize+40),buf, 10+length, augVec, 0);
    	    			pp=augVec;
    	    			ppIndex = 0;
    	    		}
    	    	} 
    	    	else 
    	    	{
    	    		if ((cbIndex[cbIndexIndex+stage] - baseSize) >= 20) 
    	    		{
    	    			cbIndex[cbIndexIndex+stage]-=20;
    	    			pp=cbVectors;
        	    		ppIndex = length-vectorLength- cbIndex[cbIndexIndex+stage]+baseSize;
    	    		} 
    	    		else 
    	    		{
    	    			cbIndex[cbIndexIndex+stage]+=(baseSize-20);
    	    			createAugmentVector((short)(cbIndex[cbIndexIndex+stage]-2*baseSize+40),cbVectors, length, augVec, 0);
    	    			pp=augVec;
    	    			ppIndex = 0;
    	    		}
    	    	}
    	    }

    	    BasicFunctions.addAffineVectorToVector(target, 10, pp, ppIndex, (short)(0-updateIndexData.getBestGain()), 8192, (short)14, vectorLength);   	    
    	    gains[stage+1] = updateIndexData.getBestGain();    	        	        	   
        }
    	
    	currIndex=10;
    	for (i=0;i<vectorLength;i++)
            codedVec[i]-=target[currIndex++];
    	
        codedEner = BasicFunctions.scaleRight(codedVec,0,codedVec,0,vectorLength, scale2);
        
        j=gainIndex[gainIndexIndex];

    	tempS = BasicFunctions.norm(codedEner);
    	tempS2 = BasicFunctions.norm(targetEner);

    	if(tempS < tempS2)
    		nBits = (short)(16 - tempS);
    	else
    		nBits = (short)(16 - tempS2);    	  

    	if(nBits<0)
    		targetEner = (targetEner<<(0-nBits))*((gains[1]*gains[1])>>14);
    	else
    		targetEner = (targetEner>>nBits)*((gains[1]*gains[1])>>14);
    	
    	gainResult = ((gains[1]-1)<<1);

    	if(nBits<0)
    		tempS=(short)(codedEner<<(-nBits));
    	else
    		tempS=(short)(codedEner>>nBits);
    	
    	for (i=gainIndex[gainIndexIndex];i<32;i++) 
    	{
    	    if ((tempS*Constants.GAIN_SQ5_SQ[i] - targetEner) < 0 && Constants.GAIN_SQ5[j] < gainResult) 
    	    	j=i;      	    	        	    
    	}
    	
    	gainIndex[gainIndexIndex]=(short)j;    	
    }
	
	public static void cbSearchCore(CbSearchData searchData,int[] cDot, int cDotIndex, short range, short stage, short[] inverseEnergy, int inverseEnergyIndex, short[] inverseEnergyShift, int inverseEnergyShiftIndex, int[] crit, int critIndex)
    {        
		int n,current,nBits,maxCrit;
		short max,tempS;
		
    	if (stage==0) 
    	{
    		for (n=0;n<range;n++) 
    	    {
    	    	if(cDot[cDotIndex]<0)
    	    		cDot[cDotIndex]=0;
    	    	
    	    	cDotIndex++;
    	    }
    		cDotIndex-=range;
    	}

    	current=0;
    	for(n=0;n<range;n++)
    	{
    		if(cDot[cDotIndex]>0 && cDot[cDotIndex]>current)
    			current=cDot[cDotIndex];
    		else if((0-cDot[cDotIndex])>current)
    			current=0-cDot[cDotIndex];
    		
    		cDotIndex++;
    	}
    	
    	cDotIndex-=range;
    	nBits=BasicFunctions.norm(current);
    	max=Short.MIN_VALUE;

    	for (n=0;n<range;n++) 
    	{
    	    tempS = (short)((cDot[cDotIndex++]<<nBits)>>16);    	    
    	    crit[critIndex]=((tempS*tempS)>>16)*inverseEnergy[inverseEnergyIndex++];
    	    
    	    if (crit[critIndex]!=0 && inverseEnergyShift[inverseEnergyShiftIndex]>max)
    	    	max = inverseEnergyShift[inverseEnergyShiftIndex];    	    	
    	    
    	    inverseEnergyShiftIndex++;
    	    critIndex++;
    	}
    	
    	if (max==Short.MIN_VALUE)
    	    max = 0;    	  

    	critIndex-=range;
    	inverseEnergyShiftIndex-=range;
    	for (n=0;n<range;n++) 
    	{
    	    if(max-inverseEnergyShift[inverseEnergyShiftIndex]>16)
    	    	crit[critIndex]=crit[critIndex]>>16;    		
    		else
    		{
    			tempS = (short)(max-inverseEnergyShift[inverseEnergyShiftIndex]);    		
    			if(tempS<0)
    				crit[critIndex]<<=-tempS;
    			else
    				crit[critIndex]>>=tempS;
    		}
    	    
    	    inverseEnergyShiftIndex++;
    	    critIndex++;
    	}

    	critIndex-=range;
    	maxCrit=crit[critIndex];
    	critIndex++;
    	searchData.setIndexNew((short)0);
    	for(n=1;n<range;n++)
    	{
    		if(crit[critIndex]>maxCrit)
    		{
    			maxCrit=crit[critIndex];
    			searchData.setIndexNew((short)n);    			
    		}
    		
    		critIndex++;
    	}
    	
    	searchData.setCritNew(maxCrit);
    	searchData.setCritNewSh((short)(32 - 2*nBits + max));    	
    }
	
	public static void cbConstruct(EncoderBits encoderBits,short[] decVector,int decVectorIndex,short[] mem,int memIndex,short length,short vectorLength,int cbIndexIndex,int gainIndexIndex)
    {
		//GC items : gain,cbVec0,cbVec1,cbVec2
		
		short[] cbIndex=encoderBits.getCbIndex();
    	short[] gainIndex=encoderBits.getGainIndex();
    	
    	short gain[]=new short[3];
    	short cbVec0[]=new short[40];
    	short cbVec1[]=new short[40];
    	short cbVec2[]=new short[40];
    	
    	int i;
    	
    	gain[0] = gainDequant(gainIndex[gainIndexIndex], (short)16384, (short)0);
    	gain[1] = gainDequant(gainIndex[gainIndexIndex+1], (short)gain[0], (short)1);
    	gain[2] = gainDequant(gainIndex[gainIndexIndex+2], (short)gain[1], (short)2);

    	System.arraycopy(emptyArray, 0, cbVec0, 0, 40);
    	System.arraycopy(emptyArray, 0, cbVec1, 0, 40);
    	System.arraycopy(emptyArray, 0, cbVec2, 0, 40);
    	
    	getCbVec(cbVec0, 0, mem, memIndex, cbIndex[cbIndexIndex], length, vectorLength);
    	getCbVec(cbVec1, 0, mem, memIndex, cbIndex[cbIndexIndex+1], length, vectorLength);
    	getCbVec(cbVec2, 0, mem, memIndex, cbIndex[cbIndexIndex+2], length, vectorLength);
    	    	
		for(i=0;i<vectorLength;i++)
    		decVector[decVectorIndex++]=(short)((gain[0]*cbVec0[i] + gain[1]*cbVec1[i] + gain[2]*cbVec2[i] + 8192)>>14);
    }
	
	public static void stateSearch(EncoderState encoderState,EncoderBits encoderBits,short[] residual,int residualIndex,short[] syntDenum,int syntIndex,short[] weightDenum,int weightIndex)
    {
		//GC items : numerator,residualLongVec,sampleMa
		
		short[] numerator=new short[11];
        short[] residualLongVec=new short[126];    	  
    	short[] sampleMa=new short[116];
    	short[] residualLong=residualLongVec; //+ 10 length 116 
    	short[] sampleAr=residualLongVec; //+ 10 length 116

    	int nBits,n,currIndex,temp;
    	short max=0,tempS,tempS2;
    	
    	for(n=0;n<encoderState.STATE_SHORT_LEN;n++)
    	{
    		tempS=residual[residualIndex++];
    		if(tempS<0)
    			tempS=(short)(0-tempS);
    		
    		if(tempS>max)
    			max=tempS;    		
    	}
    	
    	tempS = (short)(BasicFunctions.getSize(max)-12);
    	if(tempS<0)
    		tempS = 0;
    	      	
    	currIndex=syntIndex+10;
    	for (n=0; n<11; n++)
    	   numerator[n] = (short)(syntDenum[currIndex--]>>tempS);
    	
    	residualIndex-=encoderState.STATE_SHORT_LEN;
    	System.arraycopy(residual, residualIndex, residualLong, 10, encoderState.STATE_SHORT_LEN);
    	System.arraycopy(emptyArray, 0, residualLong, 10+encoderState.STATE_SHORT_LEN, encoderState.STATE_SHORT_LEN);
    	System.arraycopy(emptyArray, 0, residualLongVec, 0, 10);
    	BasicFunctions.filterMA(residualLong,10,sampleMa,0,numerator,0,11,encoderState.STATE_SHORT_LEN+10);
    	
    	System.arraycopy(emptyArray, 0, sampleMa, encoderState.STATE_SHORT_LEN + 10, encoderState.STATE_SHORT_LEN-10);
    	BasicFunctions.filterAR(sampleMa,0,sampleAr,10,syntDenum,syntIndex,11,2*encoderState.STATE_SHORT_LEN);
    	
    	int arIndex=10;
    	int arIndex2=10+encoderState.STATE_SHORT_LEN;
    	for(n=0;n<encoderState.STATE_SHORT_LEN;n++)
    	    sampleAr[arIndex++] += sampleAr[arIndex2++];    	  

    	max=0;    	
    	arIndex=10;
    	for(n=0;n<encoderState.STATE_SHORT_LEN;n++)
    	{
    		tempS2=sampleAr[arIndex++];
    		if(tempS2<0)
    			tempS2=(short)(0-tempS2);
    		
    		if(tempS2>max)
    			max=tempS2;    		
    	}    	
    	
    	/* Find the best index */
    	if ((max<<tempS)<23170)
    		temp=(max*max)<<(2+2*tempS);
    	else
    		temp=Integer.MAX_VALUE;    	  

    	currIndex=0;
    	for (n=0;n<63;n++) 
    	{
    	    if (temp>=Constants.CHOOSE_FRG_QUANT[n])
    	    	currIndex=n+1;
    	    else
    	    	n=63;
    	}
    	
    	encoderBits.setIdxForMax((short)currIndex);    	
    	if (currIndex<27)
    	    nBits=4;
    	else
    		nBits=9;    	  

    	BasicFunctions.scaleVector(sampleAr, 10, sampleAr, 10, Constants.SCALE[currIndex], encoderState.STATE_SHORT_LEN, nBits-tempS);
    	absQuant(encoderBits,sampleAr, 10, weightDenum, weightIndex);    	    	
    }
	
	public static void stateConstruct(EncoderBits encoderBits,short[] syntDenum,int syntDenumIndex,short[] outFix,int outFixIndex,int stateLen)    
    {
		//GC items : numerator,sampleValVec,sampleMaVec
		
		short[] numerator=new short[11];
    	short[] sampleValVec=new short[126];
    	short[] sampleMaVec=new short[126];
    	short[] sampleVal=sampleValVec; //+10 length 116
    	short[] sampleMa=sampleMaVec; //+10 length 116;
    	short[] sampleAr=sampleValVec; //+10 length 116;    	  
    	
    	short[] idxVec=encoderBits.getIdxVec();
    	
    	int coef,bitShift,currIndex,currIndex2;
    	int k;
    	
    	currIndex=syntDenumIndex+10;    	
    	for (k=0; k<11; k++)
    	    numerator[k] = syntDenum[currIndex--];
    	
    	int max = Constants.FRQ_QUANT_MOD[encoderBits.getIdxForMax()];
    	if (encoderBits.getIdxForMax()<37) 
    	{
    		coef=2097152;
    		bitShift=22;    		    	        	   
    	} 
    	else if (encoderBits.getIdxForMax()<59) 
    	{
    		coef=262144;
    		bitShift=19;    	        	   
    	} 
    	else 
    	{
    		coef=65536;
    		bitShift=17;    	        	   
    	}

    	currIndex=10;
    	currIndex2 = stateLen-1;
    	for(k=0; k<stateLen; k++)
  	      sampleVal[currIndex++]=(short) ((max*Constants.STATE_SQ3[idxVec[currIndex2--]]+coef) >> bitShift);
    	
    	System.arraycopy(emptyArray, 0, sampleVal, 10+stateLen, stateLen);
    	System.arraycopy(emptyArray, 0, sampleValVec, 0, 10);    	
    	BasicFunctions.filterMA(sampleVal, 10, sampleMa, 10, numerator, 0, 11, 11+stateLen);    	
    	System.arraycopy(emptyArray, 0, sampleMa, 20+stateLen, stateLen-10);    	
    	BasicFunctions.filterAR(sampleMa, 10, sampleAr, 10, syntDenum, syntDenumIndex, 11, 2*stateLen);
    	
    	currIndex=10+stateLen-1;
    	currIndex2=10+2*stateLen-1;
    	for(k=0;k<stateLen;k++)
    		outFix[outFixIndex++]=(short)(sampleAr[currIndex--]+sampleAr[currIndex2--]);    	
    }
	
	public static void filteredCBVecs(short[] cbVectors, int cbVectorsIndex, short[] cbMem, int cbMemIndex, int length,int samples)
    {
		int n;
		
		System.arraycopy(emptyArray, 0, cbMem, cbMemIndex+length, 4);
		System.arraycopy(emptyArray, 0, cbMem, cbMemIndex-4, 4);
		System.arraycopy(emptyArray, 0, cbVectors, cbVectorsIndex, length-samples);
		
    	BasicFunctions.filterMA(cbMem, cbMemIndex+4+length-samples, cbVectors, cbVectorsIndex+length-samples, Constants.CB_FILTERS_REV, 0, 8, samples);    	
    }
    
    public static void crossCorrelation(int[] crossCorrelation,int crossCorrelationIndex,short[] seq1, int seq1Index,short[] seq2, int seq2Index,short dimSeq,short dimCrossCorrelation,short rightShifts,short stepSeq2)
    {
		int i,j;
		
    	for (i = 0; i < dimCrossCorrelation; i++)
        {
            crossCorrelation[crossCorrelationIndex]=0;
            
            for (j = 0; j < dimSeq; j++)
            	crossCorrelation[crossCorrelationIndex] += (seq1[seq1Index++]*seq2[seq2Index++])>>rightShifts;
            
            seq1Index-=dimSeq;
            seq2Index=seq2Index+stepSeq2-dimSeq;
            crossCorrelationIndex++;
        }
    }
       
	public static void updateBestIndex(CbUpdateIndexData updateIndexData,int critNew,short critNewSh,short indexNew,int cDotNew,short inverseEnergyNew,short energyShiftNew)
    {
		int shOld,shNew;
		int gain;
		
		short tempShort,tempScale;
		
		int current=critNewSh-updateIndexData.getShTotMax();		
		if(current>31)
		{
			shOld=31;
			shNew=0;
		}
		else if(current>0)
		{
			shOld=current;
			shNew=0;
		}
		else if(current>-31)
		{			
			shNew=0-current;
			shOld=0;
		}
		else
		{
			shNew=31;
			shOld=0;
		}
		
    	if ((critNew>>shNew) > (updateIndexData.getCritMax()>>shOld)) 
    	{
    		tempShort = (short)(16 - BasicFunctions.norm(cDotNew));
    		tempScale=(short)(31-energyShiftNew-tempShort);
    		if(tempScale>31)
    			tempScale=31;    	    

    		if(tempShort<0)
    			gain = ((cDotNew<<(-tempShort))*inverseEnergyNew)>>tempScale;    			
    		else
    			gain = ((cDotNew>>tempShort)*inverseEnergyNew)>>tempScale;    		    		
    			
    	    if (gain>bestIndexMaxI)
    	    	updateIndexData.setBestGain(bestIndexMax);    	      
    	    else if (gain<bestIndexMinI) 
    	    	updateIndexData.setBestGain(bestIndexMin); 
    	    else 
    	    	updateIndexData.setBestGain((short)gain);

    	    updateIndexData.setCritMax(critNew);
    	    updateIndexData.setShTotMax(critNewSh);
    	    updateIndexData.setBestIndex(indexNew);    	    
    	}
    }
	
	public static void getCbVec(short[] cbVec,int cbVecIndex,short[] mem,int memIndex,short index,int length,int vectorLength)
    {
		//GC items : tempBuffer
		
		short tempBuffer[]=new short[45];
    	int baseSize=(short)(length-vectorLength+1);
    	int k;
    	
    	if (vectorLength==40)
    	    baseSize+=vectorLength>>1;    	  

    	if (index<length-vectorLength+1) 
    	{
    	    k=index+vectorLength;
    	    System.arraycopy(mem, memIndex+length-k, cbVec, cbVecIndex, vectorLength);    	    
    	} 
    	else if (index < baseSize) 
    	{
    	    k=2*(index-(length-vectorLength+1))+vectorLength;
    	    createAugmentVector((short)(k>>1),mem,memIndex+length,cbVec,cbVecIndex);    	    
    	}
    	else 
    	{
    	    if (index-baseSize<length-vectorLength+1) 
    	    {
    	    	System.arraycopy(emptyArray, 0, mem, memIndex-4, 4);
    	    	System.arraycopy(emptyArray, 0, mem, memIndex+length, 4);
    	    	BasicFunctions.filterMA(mem, memIndex+length-(index-baseSize+vectorLength)+4, cbVec, cbVecIndex, Constants.CB_FILTERS_REV, 0, 8, vectorLength);    	    	
    	    }
    	    else 
    	    {
    	    	System.arraycopy(emptyArray, 0, mem, memIndex+length, 4);    	    	
    	    	BasicFunctions.filterMA(mem, memIndex+length-vectorLength-1, tempBuffer, 0, Constants.CB_FILTERS_REV, 0, 8, vectorLength+5);
    	    	createAugmentVector((short)((vectorLength<<1)-20+index-baseSize-length-1),tempBuffer,45,cbVec,cbVecIndex);    	    	
    	    }
    	}
    }
	
	public static void createAugmentVector(short index,short[] buf,int bufIndex,short[] cbVec,int cbVecIndex)
    {
		//GC items : cbVecTemp
		
		short[] cbVecTmp=new short[4];    	
    	int currIndex=cbVecIndex+index-4;
    	
    	System.arraycopy(buf, bufIndex-index, cbVec, cbVecIndex, index);
    	BasicFunctions.multWithRightShift(cbVec, currIndex, buf, bufIndex-index-4, Constants.ALPHA, 0, 4, 15);
    	BasicFunctions.reverseMultiplyRight(cbVecTmp, 0, buf, bufIndex-4, Constants.ALPHA, 3, 4, 15);
    	BasicFunctions.addWithRightShift(cbVec, currIndex, cbVec, currIndex, cbVecTmp, 0, 4, 0);    	
    	System.arraycopy(buf, bufIndex-index, cbVec, cbVecIndex + index, 40-index);    	
    }
	
	public static void energyInverse(short[] energy,int energyIndex,int length)
    {
		int n;
    	for (n=0; n<length; n++)
    	{
    		if(energy[energyIndex]<16384)
    			energy[energyIndex++]=Short.MAX_VALUE;	
    		else
    		{
    			energy[energyIndex]=(short)(0x1FFFFFFF/energy[energyIndex]);
    			energyIndex++;
    		}    		
    	}
    }
	
	public static void energyCalc(int energy, short range, short[] ppi, int ppiIndex, short[] ppo, int ppoIndex, short[] energyArray, int energyArrayIndex, short[] energyShifts, int energyShiftsIndex, short scale, short baseSize)
    {
		int n;
		
    	energyShiftsIndex += 1 + baseSize;    	
    	energyArrayIndex += 1 + baseSize;    	

    	for(n=0;n<range-1;n++) 
    	{
    	    energy += ((ppi[ppiIndex] * ppi[ppiIndex])-(ppo[ppoIndex] * ppo[ppoIndex]))>>scale;
    	    if(energy<0)
    	    	energy=0;    	    

    	    ppiIndex--;
    	    ppoIndex--;

    	    energyShifts[energyShiftsIndex] = BasicFunctions.norm(energy);
    	    energyArray[energyArrayIndex++] = (short)((energy<<energyShifts[energyShiftsIndex])>>16);
    	    energyShiftsIndex++;
    	}
    }
	
	public static void augmentCbCorr(short[] target, int targetIndex, short[] buf, int bufIndex, short[] interpSamples, int interpSamplesIndex, int[] cDot,int cDotIndex,int low,int high,int scale)
    {
		int n;
    	for (n=low; n<=high; n++) 
    	{
    	    cDot[cDotIndex] = BasicFunctions.scaleRight(target, targetIndex, buf, bufIndex-n, n-4, scale);    	    
    		
    	    cDot[cDotIndex]+=BasicFunctions.scaleRight(target, targetIndex+n-4, interpSamples, interpSamplesIndex, 4, scale);    	    
    	    interpSamplesIndex += 4;

    	    cDot[cDotIndex]+=BasicFunctions.scaleRight(target, targetIndex+n, buf, bufIndex-n, 40-n, scale);
    	    cDotIndex++;
    	}
    }
	
	public static void absQuant(EncoderBits encoderBits,short[] in,int inIndex,short[] weightDenum, int weightDenumIndex)
    {
    	short[] quantLen=new short[2];
    	short[] syntOutBuf=new short[68];
    	short[] inWeightedVec=new short[68];
    	short[] inWeighted=inWeightedVec; //+10 length 58    	

    	System.arraycopy(emptyArray, 0, syntOutBuf, 0, 68);    	
    	System.arraycopy(emptyArray, 0, inWeightedVec, 0, 10);
    	
    	if (encoderBits.getStateFirst()) 
    	{
    	    quantLen[0]=40;
    	    quantLen[1]=(short)(EncoderState.STATE_SHORT_LEN-40);
    	} 
    	else 
    	{
    	    quantLen[0]=(short)(EncoderState.STATE_SHORT_LEN-40);
    	    quantLen[1]=40;
    	}

    	BasicFunctions.filterAR(in, inIndex, inWeighted, 10, weightDenum, weightDenumIndex, 11, quantLen[0]);
    	BasicFunctions.filterAR(in, inIndex+quantLen[0], inWeighted, 10+quantLen[0], weightDenum, weightDenumIndex+11, 11, quantLen[1]);    	

    	absQUantLoop(encoderBits,syntOutBuf, 10, inWeighted, 10,weightDenum, weightDenumIndex, quantLen, 0);
    }  
	
	public static void absQUantLoop(EncoderBits encoderBits,short[] syntOut, int syntOutIndex, short[] inWeighted, int inWeightedIndex, short[] weightDenum, int weightDenumIndex, short[] quantLen, int quantLenIndex)
    {
    	short[] idxVec=encoderBits.getIdxVec();    	
    	int startIndex=0,currIndex;
    	int i,j,k;
    	int temp,temp2;
    	
    	for(i=0;i<2;i++) 
    	{
    		currIndex=quantLenIndex+i;    		
        	for(j=0;j<quantLen[currIndex];j++)
    	    {
    	    	BasicFunctions.filterAR(syntOut, syntOutIndex, syntOut, syntOutIndex, weightDenum, weightDenumIndex, 11, 1);    	    	

    	    	temp = inWeighted[inWeightedIndex] - syntOut[syntOutIndex];
    	    	temp2 = temp<<2;
    	    	
    	    	if (temp2 > Short.MAX_VALUE)
    	    		temp2 = Short.MAX_VALUE;
    	    	else if (temp2 < Short.MIN_VALUE)
    	    		temp2 = Short.MIN_VALUE;    	      

    	    	if (temp< -7577) 
    	    	{
	      			idxVec[startIndex + j] = (short)0;
	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[0] + 2) >> 2) + inWeighted[inWeightedIndex++] - temp);
	      		}
    	      	else if (temp>8151) 
    	      	{
	      			idxVec[startIndex + j] = (short)7;
	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[7] + 2) >> 2) + inWeighted[inWeightedIndex++] - temp);
	      		}
    	      	else 
    	      	{
    	      		if (temp2 <= Constants.STATE_SQ3[0])
    	      		{
    	      			idxVec[startIndex + j] = (short)0;
    	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[0] + 2) >> 2) + inWeighted[inWeightedIndex++] - temp);
    	      		}
    	      		else 
    	      		{
    	      			k = 0;
    	      		    while(temp2 > Constants.STATE_SQ3[k] && k<Constants.STATE_SQ3.length-1)
    	      		    	k++;

    	      		    if (temp2 > ((Constants.STATE_SQ3[k] + Constants.STATE_SQ3[k - 1] + 1)>>1))
    	      		    {
        	      			idxVec[startIndex + j] = (short)k;
        	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[k] + 2) >> 2) + inWeighted[inWeightedIndex++] - temp);
        	      		}
        	      		else
        	      		{
        	      			idxVec[startIndex + j] = (short)(k-1);
        	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[k-1] + 2) >> 2) + inWeighted[inWeightedIndex++] - temp);
        	      		}        	      		   	      		   
    	      		}    	      		
    	      	}
    	    	
    	    	    	    	
    	    }
    	    
        	startIndex+=quantLen[currIndex];
        	/* Update perceptual weighting filter at subframe border */        	
    	    weightDenumIndex += 11;
    	}
    }
	
	public static void interpolateSamples(short[] interpSamples,int interpSamplesIndex,short[] cbMem,int cbMemIndex,int length)
    {
		int n,highIndex,lowIndex,temp;
    	for (n=0; n<20; n++) 
    	{
    		highIndex = cbMemIndex+length-4;
    	    lowIndex = cbMemIndex+length-n-24;
    	    
    	    interpSamples[interpSamplesIndex++] = (short)(((Constants.ALPHA[3]*cbMem[highIndex++])>>15) + ((Constants.ALPHA[0]*cbMem[lowIndex++])>>15));
    	    interpSamples[interpSamplesIndex++] = (short)(((Constants.ALPHA[2]*cbMem[highIndex++])>>15) + ((Constants.ALPHA[1]*cbMem[lowIndex++])>>15));
  	      	interpSamples[interpSamplesIndex++] = (short)(((Constants.ALPHA[1]*cbMem[highIndex++])>>15) + ((Constants.ALPHA[2]*cbMem[lowIndex++])>>15));
  	      	interpSamples[interpSamplesIndex++] = (short)(((Constants.ALPHA[0]*cbMem[highIndex++])>>15) + ((Constants.ALPHA[3]*cbMem[lowIndex++])>>15));  	      	    	        	      
    	}
    }
	
	public static short frameClassify(short[] residual)
    {
		//GC items:ssqEn
    	int[] ssqEn=new int[5];
    	
    	short max=0,tempS;
    	int n;
    	
    	for(n=0;n<EncoderState.SIZE;n++)
    	{
    		tempS=residual[n];
    		if(tempS<0)
    			tempS=(short)(0-tempS);
    		
    		if(tempS>max)
    			max=tempS;    		
    	}
    	
    	short scale=(short)(BasicFunctions.getSize(max*max)-24);
    	if(scale<0)
    		scale=0;    	

    	int ssqIndex=2;
    	int currIndex=0;    	
    	for (n=EncoderState.SUBFRAMES-1; n>0; n--) 
    	{
    		ssqEn[currIndex++] = BasicFunctions.scaleRight(residual,ssqIndex, residual,ssqIndex, 76, scale);
    		ssqIndex += 40;    	    
    	}

    	/* Scale to maximum 20 bits in order to allow for the 11 bit window */
    	ssqIndex=0;
    	int maxSSq=ssqEn[0];	
    	for(n=1;n<EncoderState.SUBFRAMES-1;n++)
    	{
    		if(ssqEn[n]>maxSSq)
    			maxSSq=ssqEn[n];    		
    	}
    	
    	scale = (short)(BasicFunctions.getSize(maxSSq) - 20);
    	if(scale<0)
    		scale = 0;

    	ssqIndex=0;    	
    	currIndex=1;    	    
    	
    	for (n=EncoderState.SUBFRAMES-1; n>0; n--) 
    	{
    		ssqEn[ssqIndex]=(ssqEn[ssqIndex]>>scale)*Constants.ENG_START_SEQUENCE[currIndex++];
    		ssqIndex++;    	    
    	}

    	/* Extract the best choise of start state */
    	currIndex=0;
    	maxSSq=ssqEn[0];	
    	for(n=1;n<EncoderState.SUBFRAMES-1;n++)
    	{
    		if(ssqEn[n]>maxSSq)
    		{
    			currIndex=n;
    			maxSSq=ssqEn[n];    			
    		}    		    		
    	}
    	
    	return (short)(currIndex + 1);
    }   
	
	public static void packBits(EncoderState encoderState,EncoderBits encoderBits,byte[] result)
    {   
		int resIndex,idxVecIndex;
		int i,k;
		
    	short[] lsf=encoderBits.getLSF();
    	short[] cbIndex=encoderBits.getCbIndex();
    	short[] gainIndex=encoderBits.getGainIndex();
    	short[] idxVec=encoderBits.getIdxVec();
    	
    	result[0]=(byte)((lsf[0]<<2) | ((lsf[1]>>5) & 0x3));
    	result[1]=(byte)((lsf[1] & 0x1F)<<3 | ((lsf[2]>>4) & 0x7));
    	result[2]=(byte)((lsf[2] & 0xF)<<4);    	
    	    	
    	if (encoderState.ENCODER_MODE==20) 
    	{    		
    		if(encoderBits.getStateFirst())
    			result[2]|=(encoderBits.getStartIdx()&0x3)<<2 | 0x2;
    		else
    			result[2]|=(encoderBits.getStartIdx()&0x3)<<2;

    		result[2]|=(encoderBits.getIdxForMax()>>5) & 0x1;    		
    		result[3]=(byte)(((encoderBits.getIdxForMax() & 0x1F)<<3) | ((cbIndex[0]>>4) & 0x7));
    	    result[4]=(byte)(((cbIndex[0] & 0xE)<<4) | (gainIndex[0] & 0x18) | ((gainIndex[1] & 0x8)>>1) | ((cbIndex[3]>>6) & 0x3));
    		result[5]=(byte)(((cbIndex[3] & 0x3E)<<2) | ((gainIndex[3]>>2) & 0x4)  | ((gainIndex[4]>>2) & 0x2) | ((gainIndex[6]>>4) & 0x1));
    		resIndex=6;
    	} 
    	else 
    	{ 
    		result[2]|=(lsf[3]>>2) & 0xF;
    		result[3]=(byte)(((lsf[3] & 0x3)<<6) | ((lsf[4]>>1) & 0x3F));
    		result[4]=(byte)(((lsf[4] & 0x1)<<7) | (lsf[5] & 0x7F));
    		if(encoderBits.getStateFirst())
    			result[5]=(byte)((encoderBits.getStartIdx()<<5) | 0x10 | (encoderBits.getIdxForMax()>>2) & 0xF); 
    		else
    			result[5]=(byte)((encoderBits.getStartIdx()<<5) | (encoderBits.getIdxForMax()>>2) & 0xF);
    		
    		result[6]=(byte)(((encoderBits.getIdxForMax() & 0x3)<<6) | ((cbIndex[0]&0x78)>>1) | ((gainIndex[0]&0x10)>>3) | ((gainIndex[1]&0x80)>>3)); 
    		result[7]=(byte)(cbIndex[3]&0xFC | ((gainIndex[3] & 0x10)>>3) | ((gainIndex[4] & 0x80)>>3));
    	    resIndex=8;
    	}
    	
    	idxVecIndex=0;
    	for (k=0; k<7; k++) 
    	{
    		result[resIndex]=0;
    	    for (i=7; i>=0; i--) 
    	    	result[resIndex] |= (((idxVec[idxVecIndex++] & 0x4)>>2)<<i);
    	    
    	    resIndex++;    	    
    	}
    	
    	result[resIndex] = (byte)((idxVec[idxVecIndex++] & 0x4)<<5);
    	
    	if (encoderState.ENCODER_MODE==20) 
    	{
    		result[resIndex] |= (gainIndex[1] & 0x4)<<4;
    		result[resIndex] |= (gainIndex[3] & 0xC)<<2;
    		result[resIndex] |= (gainIndex[4] & 0x4)<<1;
    		result[resIndex] |= (gainIndex[6] & 0x8)>>1;
    		result[resIndex] |= (gainIndex[7] & 0xC)>>2;
    	}
    	else
    	{
    		result[resIndex] |= (idxVec[idxVecIndex++] & 0x4)<<4;
    		result[resIndex] |= (cbIndex[0] & 0x6)<<3;
    		result[resIndex] |= (gainIndex[0] & 0x8);
    		result[resIndex] |= (gainIndex[1] & 0x4);
    		result[resIndex] |= (cbIndex[3] & 0x2);
    		result[resIndex] |= (cbIndex[6] & 0x80)>>7;

    		resIndex++;
    		result[resIndex] = (byte)((cbIndex[6] & 0x7E)<<1 | (cbIndex[9] & 0xC0)>>6);
    		
    		resIndex++;
    		result[resIndex] = (byte)((cbIndex[9] & 0x3E)<<2 | (cbIndex[12] & 0xE0)>>5);
    		    		    	    
    	    resIndex++;
    		result[resIndex] = (byte)((cbIndex[12] & 0x1E)<<3 | (gainIndex[3] & 0xC) | (gainIndex[4] & 0x6)>>1);
    		
    		resIndex++;
    		result[resIndex] = (byte)((gainIndex[6] & 0x18)<<3 | (gainIndex[7] & 0xC)<<2 | (gainIndex[9] & 0x10)>>1 | (gainIndex[10] & 0x8)>>1 | (gainIndex[12] & 0x10)>>3 | (gainIndex[13] & 0x8)>>3);    	    
    	}
    	
    	idxVecIndex=0;
    	resIndex++;
    	for (k=0; k<14; k++) 
    	{
    		result[resIndex]=0;
    		
    		for (i=6; i>=0; i-=2) 
    	    	result[resIndex] |= ((idxVec[idxVecIndex++] & 0x3)<<i);
    	    
    	    resIndex++;
    	}
    	
    	if (encoderState.ENCODER_MODE==20) 
    	{
    	    result[resIndex++] =(byte)((idxVec[56]& 0x3)<<6 | (cbIndex[0] & 0x1)<<5 | (cbIndex[1] & 0x7C)>>2);
    		result[resIndex++] =(byte)(((cbIndex[1]& 0x3)<<6) | ((cbIndex[2]>>1) & 0x3F));
    			    	    
    	    result[resIndex++] =(byte)((cbIndex[2] & 0x1)<<7 | (gainIndex[0] & 0x7)<<4 | (gainIndex[1] & 0x3)<<2 | (gainIndex[2] & 0x6)>>1);
    	    result[resIndex++] =(byte)((gainIndex[2] & 0x1)<<7 | (cbIndex[3] & 0x1)<<6 | (cbIndex[4] & 0x7E)>>1);  
    	    
    	    result[resIndex++] = (byte)((cbIndex[4] & 0x1)<<7 | (cbIndex[5] & 0x7F));
    	    result[resIndex++] = (byte)(cbIndex[6] & 0xFF);
    	    
    	    result[resIndex++] = (byte)(cbIndex[7] & 0xFF);
    	    result[resIndex++] = (byte)(cbIndex[8] & 0xFF);
    	    
    	    result[resIndex++] = (byte)(((gainIndex[3] & 0x3)<<6) | ((gainIndex[4] & 0x3)<<4) | ((gainIndex[5] & 0x7)<<1) | ((gainIndex[6] & 0x4)>>2));
    	    result[resIndex++] = (byte)(((gainIndex[6] & 0x3)<<6) | ((gainIndex[7] & 0x3)<<4) | ((gainIndex[8] & 0x7)<<1));     	    	    	    
    	} 
    	else 
    	{ 
    		result[resIndex++] = (byte)((idxVec[56] & 0x3)<<6 | (idxVec[57] & 0x3)<<4 | (cbIndex[0] & 0x1)<<3 | (cbIndex[1] & 0x70)>>4);
    		result[resIndex++] = (byte)((cbIndex[1] & 0xF)<<4 | (cbIndex[2] & 0x78)>>3); 
    		
    		result[resIndex++] = (byte)((cbIndex[2] & 0x7)<<5 | (gainIndex[0] & 0x7)<<2 | (gainIndex[1] & 0x3));
    		result[resIndex++] = (byte)((gainIndex[2] & 0x7)<<7 | (cbIndex[3] & 0x1)<<4 | (cbIndex[4] & 0x78)>>3);
    		
    		result[resIndex++] = (byte)((cbIndex[4] & 0x7)<<5 | (cbIndex[5] & 0x7C)>>2);
    		result[resIndex++] = (byte)((cbIndex[5] & 0x3)<<6 | (cbIndex[6] & 0x1)<<1 | (cbIndex[7] & 0xF8)>>3); 
    	    
    		result[resIndex++] = (byte)((cbIndex[7] & 0x7)<<5 | (cbIndex[8] & 0xF8)>>3);
    		result[resIndex++] = (byte)((cbIndex[8] & 0x7)<<5 | (cbIndex[9] & 0x1)<<4 | (cbIndex[10] & 0xF0)>>4);  
    		
    	    result[resIndex++] = (byte)((cbIndex[10] & 0xF)<<4 | (cbIndex[11] & 0xF0)>>4);
    		result[resIndex++] = (byte)((cbIndex[11] & 0xF)<<4 | (cbIndex[12] & 0x1)<<3 | (cbIndex[13] & 0xE0)>>5);  
    	    
    		result[resIndex++] = (byte)((cbIndex[13] & 0x1F)<<3 | (cbIndex[14] & 0xE0)>>5);  
    		result[resIndex++] = (byte)((cbIndex[14] & 0x1F)<<3 | (gainIndex[3] & 0x3)<<1 | (gainIndex[4] & 0x1));
    	    
    		result[resIndex++] = (byte)((gainIndex[5] & 0x7)<<5 | (gainIndex[6] & 0x7)<<2 | (gainIndex[7] & 0x3)); 
    		result[resIndex++] = (byte)((gainIndex[8] & 0x7)<<5 | (gainIndex[9] & 0xF)<<1 | (gainIndex[10] & 0x4)>>2); 
    			
    	    result[resIndex++] = (byte)((gainIndex[10] & 0x3)<<6 | (gainIndex[11] & 0x7)<<3 | (gainIndex[12] & 0xE)>>1); 
    		result[resIndex++] = (byte)((gainIndex[12] & 0x1)<<7 | (gainIndex[13] & 0x7)<<4 | (gainIndex[14] & 0x7)<<1);     		
    	}    	
    }
	
	public static void unpackBits(EncoderBits encoderBits,short[] data,int mode) 
    {
    	short[] lsf=encoderBits.getLSF();
    	short[] cbIndex=encoderBits.getCbIndex();
    	short[] gainIndex=encoderBits.getGainIndex();
    	short[] idxVec=encoderBits.getIdxVec();
    	
    	short tempIndex1=0,tempIndex2=0;
    	short tempS;
    	int i,k;
    	
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

	public static void updateDecIndex(EncoderBits encoderBits)
    {
    	short[] index=encoderBits.getCbIndex();    	  
    	int k;
    	
    	for (k=4;k<6;k++) 
    	{
    		if (index[k]>=44 && index[k]<108)
    			index[k]+=64;
    	    else if (index[k]>=108 && index[k]<128)
    	    	index[k]+=128;    	    
    	}
    }
	
	public static void simpleLsfDeq(short[] lsfDeq,int lsfDeqIndex,short[] index,int indexIndex,int lpcN)
    {
		int cbIndex=0;
		int i,j;
		
		for (i = 0; i < 3; i++) 
    	{			
			cbIndex=Constants.LSF_INDEX_CB[i];
    		for (j = 0; j < Constants.LSF_DIM_CB[i]; j++)
    			lsfDeq[lsfDeqIndex++] = Constants.LSF_CB[cbIndex + index[indexIndex]*Constants.LSF_DIM_CB[i] + j];    	    	
    		
    	    indexIndex++;    	    
    	}    	
    		    
    	if (lpcN>1) 
    	{
    		/* decode last LSF */
    		for (i = 0; i < 3; i++) 
        	{
    			cbIndex=Constants.LSF_INDEX_CB[i];
        	    for (j = 0; j < Constants.LSF_DIM_CB[i]; j++)
        	    	lsfDeq[lsfDeqIndex++] = Constants.LSF_CB[cbIndex + index[indexIndex]*Constants.LSF_DIM_CB[i] + j];        	    	
        	
        	    indexIndex++;        	    
    	    }
    	}
    }    

	public static void decoderInterpolateLsf(DecoderState decoderState,short[] syntDenum,int syntDenumIndex,short[] weightDenum,int weightDenumIndex,short[] lsfDeq,int lsfDeqIndex,short length)
    {
		//GC items:lp
    	short[] lp=new short[11];     	    
    	int len = length + 1;
    	short s;
    	
    	if (decoderState.DECODER_MODE==30) 
    	{
    	    lspInterpolate2PolyDec(lp, 0, decoderState.getLsfDeqOld(), 0, lsfDeq, lsfDeqIndex,Constants.LSF_WEIGHT_30MS[0], length);
	    	System.arraycopy(lp, 0, syntDenum, syntDenumIndex, len);
    	    BasicFunctions.expand(weightDenum, weightDenumIndex, lp, 0, Constants.LPC_CHIRP_SYNT_DENUM, len);

    	    for (s = 1; s < decoderState.SUBFRAMES; s++) 
    	    {
    	    	syntDenumIndex += len;
    	    	weightDenumIndex+= len;
    	    	lspInterpolate2PolyDec(lp, 0, lsfDeq, lsfDeqIndex, lsfDeq, lsfDeqIndex + length,Constants.LSF_WEIGHT_30MS[s], length);
    	    	System.arraycopy(lp, 0, syntDenum, syntDenumIndex, len);
    	    	BasicFunctions.expand(weightDenum,weightDenumIndex, lp, 0, Constants.LPC_CHIRP_SYNT_DENUM, len);    	    	
    	    }
    	}
    	else 
    	{ 	
    		for (s = 0; s < decoderState.SUBFRAMES; s++) 
    	    {
    	    	lspInterpolate2PolyDec(lp, 0, decoderState.getLsfDeqOld(), 0, lsfDeq, lsfDeqIndex,Constants.LSF_WEIGHT_20MS[s], length);
    	    	System.arraycopy(lp, 0, syntDenum, syntDenumIndex, len);
    	    	BasicFunctions.expand(weightDenum, weightDenumIndex, lp, 0, Constants.LPC_CHIRP_SYNT_DENUM, len);
    	    	syntDenumIndex += len;
    	    	weightDenumIndex+= len;
    	    }
    	}

    	if (decoderState.DECODER_MODE==30) 
    		System.arraycopy(lsfDeq, lsfDeqIndex + length, decoderState.getLsfDeqOld(), 0, length);
    	else
    		System.arraycopy(lsfDeq, lsfDeqIndex, decoderState.getLsfDeqOld(), 0, length);    	        	 
    }
	
	public static void lspInterpolate2PolyDec(short[] a,int aIndex, short[] lsf1,int lsf1Index, short[] lsf2,int lsf2Index, short coef,int length)
    {
		//GC items:lsf temp
    	short[] lsfTemp=new short[10];

    	interpolate(lsfTemp, 0, lsf1, lsf1Index, lsf2, lsf2Index, coef, length);
    	lsf2Poly(a,aIndex,lsfTemp,0);
    }
	
	public static void decodeResidual(DecoderState decoderState,EncoderBits encoderBits,short[] decResidual,int decResidualIndex,short[] syntDenum,int syntDenumIndex) 
    {
    	short[] reverseDecresidual = decoderState.getEnhancementBuffer();
    	short[] memVec = decoderState.getPrevResidual();

    	int i,subCount,subFrame;
    	short startPos,nBack,nFor,memlGotten;
    	short diff = (short)(80 - decoderState.STATE_SHORT_LEN);
    	
    	if (encoderBits.getStateFirst())
    		startPos = (short)((encoderBits.getStartIdx()-1)*40);
    	else
    		startPos = (short)((encoderBits.getStartIdx()-1)*40 + diff);    	  

    	stateConstruct(encoderBits,syntDenum,syntDenumIndex + (encoderBits.getStartIdx()-1)*11,decResidual,decResidualIndex + startPos,decoderState.STATE_SHORT_LEN);    	
    	
    	if(encoderBits.getStateFirst()) 
    	{ 
    		for(i=4;i<151-decoderState.STATE_SHORT_LEN;i++)
    			memVec[i]=0;
    		
    		System.arraycopy(decResidual, startPos, memVec, 151-decoderState.STATE_SHORT_LEN, decoderState.STATE_SHORT_LEN);
    	    cbConstruct(encoderBits,decResidual,decResidualIndex + startPos + decoderState.STATE_SHORT_LEN, memVec,66, (short)85, diff, 0, 0);
    	}
    	else 
    	{
    		memlGotten = decoderState.STATE_SHORT_LEN;
    	    BasicFunctions.reverseCopy(memVec, 150, decResidual, decResidualIndex + startPos, memlGotten);
    	    
    	    for(i=4;i<151-memlGotten;i++)
    			memVec[i]=0;    		

    	    cbConstruct(encoderBits,reverseDecresidual, 0, memVec, 66, (short)85, diff, 0, 0);
    	    BasicFunctions.reverseCopy(decResidual, decResidualIndex + startPos-1, reverseDecresidual, 0, diff);    	    
    	}

    	subCount=1;
    	nFor = (short)(decoderState.SUBFRAMES - encoderBits.getStartIdx() -1);
    	  
    	if(nFor > 0) 
    	{
    		for(i=4;i<71;i++)
    			memVec[i]=0;
    		
    	    System.arraycopy(decResidual, decResidualIndex + 40 * (encoderBits.getStartIdx()-1), memVec, 71, 80);    	    	
    	    
    	    for (subFrame=0; subFrame<nFor; subFrame++) 
    	    {
    	    	cbConstruct(encoderBits,decResidual, decResidualIndex + 40*(encoderBits.getStartIdx()+1+subFrame), memVec, 4, (short)147, (short)40, subCount*3, subCount*3);
        	    
    	    	for(i=4;i<111;i++)
    	    		memVec[i]=memVec[i+40];
    	    	
    	    	System.arraycopy(decResidual, decResidualIndex + 40 * (encoderBits.getStartIdx()+1+subFrame) , memVec, 111, 40);    	    	
    	    	subCount++;    	    	
    	    }
    	}

    	nBack = (short)(encoderBits.getStartIdx()-1);

    	if(nBack > 0)
    	{
    	    memlGotten = (short)(40*(decoderState.SUBFRAMES + 1 - encoderBits.getStartIdx()));
    	    if(memlGotten > 147)
    	    	memlGotten=147;    	    

    	    BasicFunctions.reverseCopy(memVec, 150, decResidual, decResidualIndex + 40 * (encoderBits.getStartIdx()-1), memlGotten);
    	    
    	    for(i=4;i<151-memlGotten;i++)
    	    	memVec[i]=0;
    	    
    	    for (subFrame=0; subFrame<nBack; subFrame++) 
    	    {
    	    	cbConstruct(encoderBits,reverseDecresidual, 40*subFrame, memVec, 4, (short)147, (short)40, subCount*3, subCount*3);
        	    
    	    	for(i=4;i<111;i++)
    	    		memVec[i]=memVec[i+40];
    	    	
    	    	System.arraycopy(reverseDecresidual, 40 * subFrame, memVec, 111, 40);
    	    	subCount++;
    	    }

    	    BasicFunctions.reverseCopy(decResidual, decResidualIndex+40*nBack-1, reverseDecresidual, 0, 40*nBack);    	    
    	}  	    	        	
    }   
	
	public static int xCorrCoef(short[] target,int targetIndex,short[] regressor,int regressorIndex,short subl,short searchLen,short offset,short step)
    {    	  
    	/* Initializations, to make sure that the first one is selected */
    	short max,energyScale,totScale,scaleDiff,crossCorrSqMod,energyMod,crossCorrMod,crossCorrScale;
    	short energyModMax=Short.MAX_VALUE;
    	short totScaleMax=-500;
    	short crossCorrSqModMax=0;
    	short maxLag=0;
    	
    	int pos=0;
    	int tempIndex1,tempIndex2,tempIndex3,tempIndex4,shifts,newCrit,maxCrit,k,energy,temp,crossCorr;
    	
    	if (step==1) 
    	{
    		max=BasicFunctions.getMaxAbsValue(regressor,regressorIndex,subl+searchLen-1);
    	    tempIndex1=regressorIndex;
    	    tempIndex2=regressorIndex + subl;    	    
    	}
    	else 
    	{ 
    		max=BasicFunctions.getMaxAbsValue(regressor,regressorIndex-searchLen,subl+searchLen-1);
    	    tempIndex1=regressorIndex-1;
    		tempIndex2=regressorIndex + subl-1;    	    
    	}

    	if (max>5000)
    		shifts=2;
    	else
    		shifts=0;    	  

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

    			//if(k<2)
    			//	System.out.println("SQ MOD:" + crossCorrSqMod + ",SCALE DIFF:" + scaleDiff);
        		
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
	
	public static void doThePlc(DecoderState decoderState,short[] plcResidual,int plcResidualIndex,short[] plcLpc,int plcLpcIndex,short pli,short[] decResidual,int decResidualIndex,short[] lpc,int lpcIndex,short inLag)
    {    	      	     
    	short[] randVec=new short[240];
    	short scale,scale1,scale2,totScale;
    	short shift1,shift2,shift3,shiftMax;
    	short useGain,totGain,maxPerSquare,pitchFact,useLag,randLag,pick,crossSquareMax, crossSquare,tempS,tempS2,lag,max,denom,nom,corrLen;
    	
    	int temp,temp2,tempShift,i,energy,j,ind,measure,maxMeasure;
    	CorrData tempCorrData=new CorrData(),corrData=new CorrData();
    	tempCorrData.setEnergy(0);
    	
    	if (pli == 1) 
    	{
    	    decoderState.setConsPliCount(decoderState.getConsPliCount()+1);
    	    if (decoderState.getPrevPli() != 1) 
    	    {
    	      max=BasicFunctions.getMaxAbsValue(decoderState.getPrevResidual(),0,decoderState.SIZE);
    	      scale = (short)((BasicFunctions.getSize(max)<<1) - 25);
    	      if (scale < 0)
    	        scale = 0;    	      

    	      decoderState.setPrevScale(scale);
    	      lag = (short)(inLag - 3);

    	      if(60 > decoderState.SIZE-inLag-3)
    	    	  corrLen=60;
    	      else
    	    	  corrLen=(short)(decoderState.SIZE-inLag-3);
    	      
    	      compCorr(corrData, decoderState.getPrevResidual(), 0, lag, decoderState.SIZE, corrLen, scale);

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
    	    	  compCorr(tempCorrData, decoderState.getPrevResidual(), 0, (short)j, decoderState.SIZE, corrLen, scale);

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
    	    		  lag = (short)j;
    	    		  crossSquareMax = crossSquare;
    	    		  corrData.setCorrelation(tempCorrData.getCorrelation());
    	    		  shiftMax = shift1;
    	    		  corrData.setEnergy(tempCorrData.getEnergy());
    	    	  }
    	      }

    	      temp2=BasicFunctions.scaleRight(decoderState.getPrevResidual(),decoderState.SIZE-corrLen,decoderState.getPrevResidual(),decoderState.SIZE-corrLen,corrLen, scale);
    	      
    	      if ((temp2>0)&&(tempCorrData.getEnergy()>0)) 
    	      {    	    	
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
    	    	      	    	  
    	    	  denom=(short)((tempS*tempS2)>>16);

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
    	        maxPerSquare = 0;    	      
    	  }    	  
    	  else 
    	  {
    		  lag = decoderState.getPrevLag();
    	      maxPerSquare = decoderState.getPerSquare();
    	  }

    	  useGain = 32767;
    	  
    	  if (decoderState.getConsPliCount()*decoderState.SIZE>320)
    		  useGain = 29491;
    	  else if (decoderState.getConsPliCount()*decoderState.SIZE>640)
    		  useGain = 22938;
    	  else if (decoderState.getConsPliCount()*decoderState.SIZE>960)
    		  useGain = 16384;
    	  else if (decoderState.getConsPliCount()*decoderState.SIZE>1280)
    		  useGain = 0;
    	  
    	  if (maxPerSquare>7868) 
    		  pitchFact = 32767;
    	  else if (maxPerSquare>839) 
    	  { 
    		  ind = 5;
    	      while ((maxPerSquare<Constants.PLC_PER_SQR[ind]) && (ind>0))
    	        ind--;
    	      
    	      temp = Constants.PLC_PITCH_FACT[ind];
    	      temp += ((Constants.PLC_PF_SLOPE[ind]*(maxPerSquare-Constants.PLC_PER_SQR[ind])) >> 11);

    	      if(temp>Short.MIN_VALUE)
    	    	  pitchFact=Short.MIN_VALUE;
    	      else
    	    	  pitchFact=(short)temp;    	      
    	  } 
    	  else 
    		  pitchFact = 0;
    	  
    	  useLag = lag;
    	  if (lag<80) 
    	      useLag = (short)(2*lag);
    	  
    	  energy = 0;
    	  for (i=0; i<decoderState.SIZE; i++) 
    	  {
    	      decoderState.setSeed((short)((decoderState.getSeed()*31821) + 13849));
    	      randLag = (short)(53 + (decoderState.getSeed() & 63));

    	      pick = (short)(i - randLag);
    	      if (pick < 0)
    	    	  randVec[i] = decoderState.getPrevResidual()[decoderState.SIZE+pick];
    	      else
    	    	  randVec[i] = decoderState.getPrevResidual()[pick];    	      

    	      pick = (short)(i - useLag);

    	      if (pick < 0)
    	        plcResidual[plcResidualIndex + i] = decoderState.getPrevResidual()[decoderState.SIZE+pick];
    	      else
    	    	  plcResidual[plcResidualIndex + i] = plcResidual[plcResidualIndex + pick];    	      

    	      if (i<80)
    	        totGain=useGain;
    	      else if (i<160)
    	        totGain=(short)((31130 * useGain) >> 15);
    	      else
    	        totGain=(short)((29491 * useGain) >> 15);    	      


    	      tempShift=pitchFact * plcResidual[plcResidualIndex + i];
    	      tempShift+=(32767-pitchFact)*randVec[i];
    	      tempShift+=16384;
    	      temp=(short)(tempShift>>15);
    	      plcResidual[plcResidualIndex + i] = (short)((totGain*temp)>>15);

    	      tempShift=plcResidual[plcResidualIndex + i] * plcResidual[plcResidualIndex + i];
    	      energy += (short)(tempShift>>(decoderState.getPrevScale()+1));
    	  }

    	  tempShift=decoderState.SIZE*900;
    	  if(decoderState.getPrevScale()+1>0)
    		  tempShift=tempShift>>(decoderState.getPrevScale()+1);
    	  else
    		  tempShift=tempShift<<(0-decoderState.getPrevScale()-1);
    	  
    	  if (energy < tempShift) 
    	  {
    	      energy = 0;
    	      for (i=0; i<decoderState.SIZE; i++)
    	        plcResidual[plcResidualIndex + i] = randVec[i];    	      
    	  }

    	  System.arraycopy(decoderState.getPrevLpc(), 0, plcLpc, plcLpcIndex, 10);    	  
    	  decoderState.setPrevLag(lag);
    	  decoderState.setPerSquare(maxPerSquare);    	  
       }
       else 
       {
     	  System.arraycopy(decResidual, decResidualIndex, plcResidual, plcResidualIndex, decoderState.SIZE);    	  
     	  System.arraycopy(lpc, lpcIndex, plcLpc, plcLpcIndex, 11);    	  
    	  decoderState.setConsPliCount(0);    	    
       }

       decoderState.setPrevPli(pli);
       System.arraycopy(plcLpc, plcLpcIndex, decoderState.getPrevLpc(), 0, 11);    	  
       System.arraycopy(plcResidual, plcResidualIndex, decoderState.getPrevResidual(), 0, decoderState.SIZE); 	       
    }    
	
	public static void compCorr(CorrData currData,short[] buffer,int bufferIndex,short lag,short bLen,short sRange,short scale)
    {
    	int currIndex=bLen-sRange-lag;
    	
    	if(scale>0)
    	{
    		currData.setCorrelation(BasicFunctions.scaleRight(buffer, bufferIndex + bLen - sRange, buffer, currIndex, sRange, scale));
    		currData.setEnergy(BasicFunctions.scaleRight(buffer, currIndex, buffer, currIndex, sRange, scale));    		
    	}
    	else
    	{
    		currData.setCorrelation(BasicFunctions.scaleLeft(buffer, bufferIndex + bLen - sRange, buffer, currIndex, sRange, (0-scale)));
    		currData.setEnergy(BasicFunctions.scaleLeft(buffer, currIndex, buffer, currIndex, sRange, scale));
    	}
    	
    	if (currData.getCorrelation() == 0) 
    	{
    		currData.setCorrelation(0);
    		currData.setEnergy(1);    	    
    	}
    }
}