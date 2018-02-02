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
public class BasicFunctions {	
	
	public static short abs(short var1)
    {
    	if(var1==Short.MIN_VALUE)
    		return Short.MAX_VALUE;
    	else if(var1<0)
    		return (short)-var1;
    	
    	return (short)var1;
    }
	
	public static short getSize(int var1) throws ArithmeticException
    {
		short count;
		if ((0xFFFF0000 & var1)!=0)
			count = 16;
		else
			count = 0;
		
		if ((0x0000FF00 & (var1 >> count))!=0) count += 8;
		if ((0x000000F0 & (var1 >> count))!=0) count += 4;
		if ((0x0000000C & (var1 >> count))!=0) count += 2;
		if ((0x00000002 & (var1 >> count))!=0) count += 1;
		if ((0x00000001 & (var1 >> count))!=0) count += 1;

		return count;
    }
	
	public static short norm(int var1) throws ArithmeticException
    {
		short count=0;
		int currValue=var1;  
		if (currValue <= 0) currValue ^= 0xFFFFFFFF;

		if ((0xFFFF8000 & currValue)==0) 
			count = 16;
		else 
			count = 0;
		  
		if ((0xFF800000 & (currValue << count))==0) count += 8;
		if ((0xF8000000 & (currValue << count))==0) count += 4;
		if ((0xE0000000 & (currValue << count))==0) count += 2;
		if ((0xC0000000 & (currValue << count))==0) count += 1;

		return count;		    	
    }
	
	public static int div(int num, short hi,short low)
	{
	    short temp1, temp2, numHi, numLow;
	    int temp,tempL,tempShift,approx;
	    
	    if(hi!=0)
	    	approx=0x1FFFFFFF/hi;
	    else
	    	approx=Integer.MAX_VALUE;
	    	   
	    tempL=hi*approx;
	    tempL=tempL << 1;
	    temp=tempL;
	    tempL=low*approx;
	    tempL=tempL >> 15;
	    tempL=tempL<<1;
	    temp +=  tempL;
	    temp = Integer.MAX_VALUE - temp;
	    temp1 = (short)(temp>>16);
	    tempShift=temp1<<16;
	    temp2 = (short)((temp - tempShift)>>1);
	    
	    tempL=(temp1*approx);
	    temp=tempL;
	    tempL=temp2*approx;
	    tempL=tempL>> 15;
		temp+=tempL;
	    temp = temp << 1;
	    temp1 = (short)(temp>>16);
	    tempShift=temp1<<16;
	    temp2 = (short)((temp - tempShift)>>1);

	    numHi = (short)(num>>16);
	    tempShift=numHi<<16;
	    numLow = (short)((num - tempShift)>>1);

	    tempL=numHi*temp1;
	    temp=tempL;
	    tempL=numHi*temp2;
	    tempL=tempL>> 15;
	    temp+=tempL;
	    tempL=numLow*temp1;
	    tempL=(tempL) >> 15;
	    temp+= tempL;
	    temp = temp<<3;
	    return temp;
	}
		
	//MaxAbsValue
	public static short getMaxAbsValue(short[] input,int inputIndex,int length)
	{
		short currValue=0;
		int i = inputIndex;
		int total = inputIndex + length;
		for(;i<total;i++)
			if(input[i]>0 && input[i]>currValue)
				currValue=input[i];
			else if(input[i]<0 && (0-input[i])>currValue)
				currValue=(short)(0-input[i]);
		
		return currValue;
	}
	
	//DotProductWithScale
	public static int scaleRight(short[] input1,int input1Index,short[] input2,int input2Index,int length,int rightShifts)
	{		
		int sum=0;
		for(int n=0;n<length;n++)
			sum+=((input1[input1Index++]*input2[input2Index++])>>rightShifts);
		
		return sum;
	}
	
	public static int scaleLeft(short[] input1,int input1Index,short[] input2,int input2Index,int length,int leftShifts)
	{		
		int sum=0;
		for(int n=0;n<length;n++)
			sum+=((input1[input1Index++]*input2[input2Index++])<<leftShifts);
		
		return sum;
	}
	
	//ScaleVectorWithSat
	public static void scaleVector(short[] input, int inputIndex, short[] output, int outputIndex, short gain,int length,int rightShifts)
	{
		int i;
	    int temp;
	    
	    for (i = 0; i < length; i++)
	    {
	    	temp = (input[inputIndex++]*gain)>>rightShifts;
	    	if(temp>Short.MAX_VALUE)
	    		output[outputIndex++] = Short.MAX_VALUE;
	    	else if(temp<Short.MIN_VALUE)
	    		output[outputIndex++] = Short.MIN_VALUE;
	    	else
	    		output[outputIndex++]=(short)temp;
	    }
	}
	
	//ElementwiseVectorMult
	public static void multWithRightShift(short[] output,int outputIndex,short[] input1,int input1Index,short[] input2,int input2Index,int length,int rightShifts)
	{		
		for(int n=0;n<length;n++)
			output[outputIndex++]=(short)((input1[input1Index++]*input2[input2Index++])>>rightShifts);		
	}
	
	public static void multWithLeftShift(short[] output,int outputIndex,short[] input1,int input1Index,short[] input2,int input2Index,int length,int leftShifts)
	{		
		for(int n=0;n<length;n++)
			output[outputIndex++]=(short)((input1[input1Index++]*input2[input2Index++])<<leftShifts);		
	}
	
	//ReverseOrderMultArrayElements
	public static void reverseMultiplyRight(short[] out,int outIndex,short[] in,int inIndex,short[] win,int winIndex,int length,int rightShifts)            
	{
		for (int n = 0; n < length; n++)
	    	out[outIndex++]= (short)((in[inIndex++] * win[winIndex--]) >> rightShifts);	    
	}
	
	public static void reverseMultiplyLeft(short[] out,int outIndex,short[] in,int inIndex,short[] win,int winIndex,int length,int leftShifts)            
	{
		for (int n = 0; n < length; n++)
	    	out[outIndex++]= (short)((in[inIndex++] * win[winIndex--]) << leftShifts);
	}
	
	//VectorBitShiftW32
	public static void bitShiftRight(int[] output,int outputIndex,int[] input,int inputIndex,int length,int rightShifts)
	{
		for(int n=0;n<length;n++)
			output[outputIndex++]=input[inputIndex++]>>rightShifts;		
	}
	
	public static void bitShiftLeft(int[] output,int outputIndex,int[] input,int inputIndex,int length,int leftShifts)
	{
		for(int n=0;n<length;n++)
			output[outputIndex++]=input[inputIndex++]<<leftShifts;		
	}	
	
	//AddVectorsAndShift
	public static void addWithRightShift(short[] out, int outIndex, short[] in1, int in1Index, short[] in2, int in2Index,int length,int rightShifts)
	{
		for (int i = length; i > 0; i--)
	        out[outIndex++] = (short)((in1[in1Index++] + in2[in2Index++]) >> rightShifts);	    
	}
	
	public static void addWithLeftShift(short[] out, int outIndex, short[] in1, int in1Index, short[] in2, int in2Index,int length,int leftShifts)
	{
		for (int i = length; i > 0; i--)
	        out[outIndex++] = (short)((in1[in1Index++] + in2[in2Index++]) << leftShifts);	    
	}
	
	//AddAffineVectorToVector
	public static void addAffineVectorToVector(short[] out, int outIndex, short[] in, int inIndex, short gain, int addConstant, short rightShifts, int length)
	{
		for (int n = 0; n < length; n++)
	    	out[outIndex++] += (short)((in[inIndex++]*gain + addConstant) >> rightShifts);	    
	}
	
	//BwExpand
	public static void expand(short[] output,int outputIndex,short[] input,int inputIndex,short[] coeficient,int length)
	{
		int temp;
		output[outputIndex++] = input[inputIndex++];		
		for (int i = 1; i < length; i++) 
		{
		    /* out[i] = coef[i] * in[i] with rounding.
		       in[] and out[] are in Q12 and coef[] is in Q15
		    */
		    output[outputIndex++] = (short)((coeficient[i]*input[inputIndex++]+16384)>>15);
		}
	}
	
	//WebRtcSpl_MemCpyReversedOrder
	public static void reverseCopy(short[] dest,int destIndex,short[] source,int sourceIndex,int length)
	{
		while(length>0)
		{
			dest[destIndex--]=source[sourceIndex++];
			length--;
		}	    
	}
	
	//FilterMAFastQ12
	public static void filterMA(short[] input,int inputIndex,short[] output,int outputIndex,short[] B,int bIndex,int bLength,int length)
	{
		int o;
	    int i, j;
	    int temp1,temp2;
	    for (i = 0; i < length; i++)
	    {
	        temp2=inputIndex+i;
	        temp1=bIndex;
	        o = 0;

	        for (j = 0; j < bLength; j++)
	            o += B[temp1++]*input[temp2--];	        

	        // If output is higher than 32768, saturate it. Same with negative side
	        // 2^27 = 134217728, which corresponds to 32768 in Q12

	        // Saturate the output
	        if(o>134215679)
	        	o=134215679;
	        else if(o<-134217728)
	        	o=-134217728;	        	        

	        output[outputIndex++] = (short)((o + 2048) >> 12);
	    }
	}
	
	//FilterARFastQ12
	public static void filterAR(short[] input,int inputIndex,short[] output,int outputIndex,short[] coefs,int coefsIndex,int coefsLength,int length)
	{
		int i, j;
		int temp2,temp3;
		int result=0,sum=0;
		for (i = 0; i < length; i++) 
		{		   
			result=0;
			sum=0;
			temp2=outputIndex + i - coefsLength + 1;
			temp3=coefsIndex + coefsLength - 1;
			for (j = coefsLength - 1; j > 0; j--)
		      sum += coefs[temp3--] * output[temp2++];		    

			result = coefs[coefsIndex] * input[inputIndex++];
			result -= sum;

		    // Saturate and store the output.
		    if(result>134215679)
		    	result=134215679;
		    else if(result<-134217728)
		    	result=-134217728;
		    
		    output[temp2] = (short)((result + 2048) >> 12);
		}
	}	
}
