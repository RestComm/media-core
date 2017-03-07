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

package org.restcomm.media.codec.gsm;

import org.restcomm.media.spi.dsp.Codec;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 * 
 * @author amit bhayani
 * @kulikov
 */
public class Decoder implements Codec {

    private final static Format gsm = FormatFactory.createAudioFormat("gsm", 8000);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private short k_temp,temp,temp1,temp2,temp3,mant,msr=0,nc,bc,mc,xmaxc,exp,nr,nrp=40,brp,drpp,sri,itest;
    
    private int i,j,k,l,framesCount;
    
    private short[] xmc=new short[13];
    private short[] xmp=new short[13];    
    private short[] erp=new short[40];
    private short[] drp=new short[160];
    private short[] result=new short[160];
    private short[] LARc=new short[9];
    private short[] LARp=new short[9];
    private short[] LARpp=new short[9];
    private short[] rp=new short[9];
    private short[] v=new short[9];
    private short[] LARpprev={0,0,0,0,0,0,0,0,0};
    
    private static final int BUFFER_SIZE = 320;
    byte[] resdata;
    
    public Decoder() 
    {
    	for(i=0;i<120;i++)
    		drp[i]=0;
    }

    public Frame process(Frame frame) {
    	byte[] data=frame.getData();    	
    	if(data.length==0)
    	{
    		//dummy frame received , sending blank data
    		Frame res = Memory.allocate(320);
        	
        	//load data into frame
        	data=res.getData();    	
        	for (i = 0; i < 320; i++) 
        		data[i]=0;        		           
            
            res.setOffset(0);
            res.setLength(320);
            res.setTimestamp(frame.getTimestamp());
            res.setDuration(frame.getDuration());
            res.setSequenceNumber(frame.getSequenceNumber());
            res.setEOM(frame.isEOM());
            res.setFormat(linear);
            return res;
    	}
    	
    	if(data.length%33!=0)
    		throw new IllegalArgumentException("invalid frame size expected 33,received " + data.length);
    	
    	framesCount=data.length/33;
    	Frame res = Memory.allocate(320*framesCount);
    	resdata=res.getData();
    	for(l=0;l<(data.length/33);l++)
    	{
    		k_temp=(short)(l*33);
    		if(((data[k_temp]>>4) & 0xF) != 0xD)
    			throw new IllegalArgumentException("not gsm fr frame,expected 0xD received " + Integer.toHexString(data[k_temp]>>4) +  " FRAME SIZE:" + data.length);
    	    		
    		//lets load LARC array
    		//LARC[1] - 4 bits from byte 0 and 2 bits from byte 1
    		LARc[1]=(short)(((data[k_temp]<<2)&0x3C) | ((data[k_temp+1]>>6) & 0x3));
    		//LARC[2] - 6 bits from byte 1 
    		LARc[2]=(short)(data[k_temp+1]&0x3F);
    		//LARC[3] - 5 bits from byte 2
    		LARc[3]=(short)((data[k_temp+2]>>3) & 0x1F);
    		//LARC[4] - 3 bits from byte 2 and 2 bits from byte 3
    		LARc[4]=(short)(((data[k_temp+2]<<2)&0x1C) | ((data[k_temp+3]>>6) & 0x3));
    		//LARC[5] -4 bits from byte 3
    		LARc[5]=(short)((data[k_temp+3]>>2) & 0xF);
    		//LARC[6] - 2 bits from byte 3 and 2 bits from byte 4
    		LARc[6]=(short)(((data[k_temp+3]<<2)&0xC) | ((data[k_temp+4]>>6) & 0x3));
    		//LARC[7] - 3 bits from byte 4
    		LARc[7]=(short)((data[k_temp+4]>>3) & 0x7);    	
    		//LARC[8] - 3 bits from byte 4
    		LARc[8]=(short)(data[k_temp+4] & 0x7);    	
    	    	
    		LARDecoding();
    	
    		k_temp+=5;
    		//lets handle 4 subframes
    		for(k=0;k<4;k++)
    		{   
    			//taking 7 bits for nc
    			nc=(short)((data[k_temp]>>1)&0x7F);
    		
    			//taking one bit from byte 1 and 1 bit from byte 2 for bc
    			bc=(short)(((data[k_temp++]<<1) & 0x2) | ((data[k_temp]>>7)&0x1));
    		
    			//taking 2 bits of mc
    			mc=(short)((data[k_temp]>>5) & 0X3);
    		
    			//taking 5 bits from byte 2 and 1 bit from byte 3 for xmaxc
    			xmaxc=(short)(((data[k_temp++]<<1) & 0x3E) | ((data[k_temp]>>7)&0x1));
    		
    			//loading xmc array
    			xmc[0]=(short)((data[k_temp]>>4) & 0x7);
    			xmc[1]=(short)((data[k_temp]>>1) & 0x7);
    			xmc[2]=(short)(((data[k_temp++]<<2)& 0x4) | ((data[k_temp]>>6)&0x3));
    		
    			xmc[3]=(short)((data[k_temp]>>3) & 0x7);
    			xmc[4]=(short)(data[k_temp++] & 0x7);
    		
    			xmc[5]=(short)((data[k_temp]>>5) & 0x7);
    			xmc[6]=(short)((data[k_temp]>>2) & 0x7);
    			xmc[7]=(short)(((data[k_temp++]<<1)& 0x6) | ((data[k_temp]>>7)&0x1));
    		
    			xmc[8]=(short)((data[k_temp]>>4) & 0x7);
    			xmc[9]=(short)((data[k_temp]>>1) & 0x7);
    			xmc[10]=(short)(((data[k_temp++]<<2)& 0x4) | ((data[k_temp]>>6)&0x3));
    		
    			xmc[11]=(short)((data[k_temp]>>3) & 0x7);
    			xmc[12]=(short)(data[k_temp++] & 0x7);    
    		
    			computeExpAndMant();
    			ACPMInverseQuantization();
    			RPEPositioning();
    			longTermSynthesisFiltering(k);
    		}
    	
    		//drp has data now
    		shortTermFiltering(result);
    		deemphasisFilter(result);
    		upscale(result);
    		downscale(result);
    	
    		//switch LARpp arrays
    		for(i=1;i<9;i++)
    		{
    			LARpprev[i]=LARpp[i];
    			LARpp[i]=0;
    		}            	
    	
    		
    		k_temp=(short)(l*320);
    		//load data into frame    		    
    		for (i = 0; i < 160; i++) 
    		{
    			resdata[k_temp+i*2]=(byte)(result[i]& 0xFF);
    			resdata[k_temp+i*2 + 1]=(byte)((result[i]>>8) & 0xFF);            
    		}
    	}
    	
        res.setOffset(0);
        res.setLength(320*framesCount);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(linear);
        return res;
    }
    
    public Format getSupportedInputFormat() {
        return gsm;
    }

    public Format getSupportedOutputFormat() {
        return linear;
    }
    
    //5.2.8 Decoding of coded LAR
    private void LARDecoding() 
    {
    	for(i=1;i<9;i++)
    	{
    		temp=(short)(BasicFunctions.add(LARc[i],BasicFunctions.MIC[i-1])<<10);
    		temp2=(short)(BasicFunctions.B[i-1]<<1);
    		temp=BasicFunctions.sub(temp,temp2);
    		temp=BasicFunctions.mult_r(BasicFunctions.INVA[i-1],temp);
    		LARpp[i]=BasicFunctions.add(temp,temp);
    	}
    }
    
    //5.2.15 compute exponent and mantissa 
    private void computeExpAndMant()
    {
    	exp=0;
    	if(xmaxc>15)
    		exp=BasicFunctions.sub((short)(xmaxc>>3),(short)1);
    	
    	mant=BasicFunctions.sub(xmaxc,(short)(exp<<3));
    	
    	//normalize mantissa
    	if(mant==0)
    	{
    		exp=-4;
    		mant=15;
    	}
    	else
    	{
    		itest=0;
    		for(i=0;i<3;i++)
    		{
    			if(mant>7)
    				itest=1;
    			
    			if(itest==0)
    			{
    				mant=BasicFunctions.add((short)(mant<<1),(short)1);
    				exp=BasicFunctions.sub(exp,(short)1);
    			}
    		}    	    		
    	}
    	
    	mant=BasicFunctions.sub(mant,(short)8);
    }
    
    //5.2.16 ACPM Inverse Quantization
    private void ACPMInverseQuantization() 
    {
    	temp1=BasicFunctions.FAC[mant];
    	temp2=BasicFunctions.sub((short)6,exp);
    	temp3=(short)(1<<BasicFunctions.sub(temp2,(short)1));
    	
    	for(i=0;i<13;i++)
    	{
    		temp=BasicFunctions.sub((short)(xmc[i]<<1),(short)7);
    		temp=(short)(temp<<12);
    		temp=BasicFunctions.mult_r(temp1,temp);
    		temp=BasicFunctions.add(temp,temp3);
    		xmp[i]=(short)(temp>>temp2);
    	}
    }
    
    //5.2.17 RPE grip positioning
    private void RPEPositioning() 
    {
    	for(i=0;i<40;i++)
    		erp[i]=0;
    	
    	for(i=0;i<13;i++)
    		erp[mc+3*i]=xmp[i];
    }
    
    //5.3.2 Long term synthesis filtering
    private void longTermSynthesisFiltering(int subframe)
    {
    	nr=nc;
    	if(nc<40)
    		nr=nrp;
    	else if(nc>120)
    		nr=nrp;
    	
    	nrp=nr;
    	
    	brp=BasicFunctions.QLB[bc];
    	for(i=0;i<40;i++)
    	{
    		drpp=BasicFunctions.mult_r(brp,drp[i-nr+120]);
    		drp[i+120]=BasicFunctions.add(erp[i],drpp);
    	}
    	
    	for(i=0;i<119;i++)
    	{
    		drp[i]=drp[i+40];    		
    	}
    	
    	temp=(short)(subframe*40);
    	for(i=0;i<40;i++)
    	{
    		result[temp]=drp[i];
    		temp++;
    	}		
    }    
    
    //5.2.9 Short term filtering
    private void shortTermFiltering(short[] data)
    {
    	//*************************************
    	//from 0 to 12
    	//*************************************
    	
    	//5.2.9.1 - Interpolation of LARpp to get LARp
    	for(i=1;i<9;i++)
		{
			LARp[i]=BasicFunctions.add((short)(LARpprev[i]>>2),(short)(LARpp[i]>>2));
			LARp[i]=BasicFunctions.add(LARp[i],(short)(LARpprev[i]>>1));
		}
    	
    	//5.2.9.2 - Computation of rp from interpolated LARp
    	computeRP();    	    	
    	
    	//5.3.4 - short term synthesis filtering
    	shortTermSynthesisFiltering(data,0,12);
    	
    	//*************************************
    	//from 13 to 26
    	//*************************************
    	
    	//5.2.9.1 - Interpolation of LARpp to get LARp
    	for(i=1;i<9;i++)
			LARp[i]=BasicFunctions.add((short)(LARpprev[i]>>1),(short)(LARpp[i]>>1));
		
    	//5.2.9.2 - Computation of rp from interpolated LARp
    	computeRP();    	    	
    	
    	//5.3.4 - short term synthesis filtering
    	shortTermSynthesisFiltering(data,13,26);
    	
    	//*************************************
    	//from 27 to 39
    	//*************************************    	
    	
    	//5.2.9.1 - Interpolation of LARpp to get LARp
    	for(i=1;i<9;i++)
    	{
			LARp[i]=BasicFunctions.add((short)(LARpprev[i]>>2),(short)(LARpp[i]>>2));
			LARp[i]=BasicFunctions.add(LARp[i],(short)(LARpp[i]>>1));
		}
    	
    	//5.2.9.2 - Computation of rp from interpolated LARp
    	computeRP();    	    	
    	
    	//5.3.4 - short term synthesis filtering
    	shortTermSynthesisFiltering(data,27,39);
    	
    	//*************************************
    	//from 40 to 159
    	//*************************************
    	//5.2.9.1 - Interpolation of LARpp to get LARp
    	for(i=1;i<9;i++)
			LARp[i]=LARpp[i];		
    	
    	//5.2.9.2 - Computation of rp from interpolated LARp
    	computeRP();    
    	
    	//5.3.4 - short term synthesis filtering
    	shortTermSynthesisFiltering(data,40,159);
    }
    
    private void computeRP()
    {
    	for(i=1;i<9;i++)
    	{
    		temp=BasicFunctions.abs(LARp[i]);
    		if(temp<11059)
    			temp=(short)(temp<<1);
    		else if(temp<20070)
    			temp=BasicFunctions.add(temp,(short)11059);
    		else
    			temp=BasicFunctions.add((short)(temp>>2),(short)26112);
    		
    		rp[i]=temp;
    		if(LARp[i]<0)
    			rp[i]=BasicFunctions.sub((short)0,rp[i]);
    	}
    }
    
    //5.3.4 short term synthesis filtering
    private void shortTermSynthesisFiltering(short[] data,int startIndex,int endIndex)
    {
    	for(j=startIndex;j<=endIndex;j++)
    	{
    		sri=data[j];
    		for(i=1;i<9;i++)
    		{
    			sri=BasicFunctions.sub(sri,BasicFunctions.mult_r(rp[9-i],v[8-i]));
    			v[9-i]=BasicFunctions.add(v[8-i],BasicFunctions.mult_r(rp[9-i],sri));
    		}   
    		
    		data[j]=sri;
    		v[0]=sri;
    	}    	
    }
    
    //5.3.5 deempasis filter
    private void deemphasisFilter(short[] data)
    {
    	for(i=0;i<160;i++)
    	{
    		temp=BasicFunctions.add(data[i],BasicFunctions.mult_r(msr,(short)28180));
    		msr=temp;
    		data[i]=msr;
    	}    	
    }
    
    //5.3.6 upscale
    private void upscale(short[] data)
    {
    	for(i=0;i<160;i++)
    		data[i]=BasicFunctions.add(data[i],data[i]);    	
    }
    
    //5.3.7 truncation
    private void downscale(short[] data)
    {
    	for(i=0;i<160;i++)
    	{
    		data[i]=(short)(data[i]>>3);
    		data[i]=(short)(data[i]<<3);
    	}
    }
}
