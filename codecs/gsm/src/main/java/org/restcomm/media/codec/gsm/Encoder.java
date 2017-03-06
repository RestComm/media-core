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
 * @author oifa yulian
 * 
 */
public class Encoder implements Codec {

    private final static Format gsm = FormatFactory.createAudioFormat("gsm", 8000);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private short z1=0,mp=0,k_temp,temp,temp1,temp2,temp3,s1,msp,lsp,smax,dmax,scalauto,di,sav,scal,nc,bc,R,S,bp,mc,xmax,exp,itest,xmaxc,mant;
    private short[] r=new short[9];
    private short[] rp=new short[9];
    private short[] LAR=new short[9];
    private short[] LARc=new short[9];
    private short[] LARp=new short[9];
    private short[] LARpprev={0,0,0,0,0,0,0,0,0};
    private short[] LARpp=new short[9];
    private short[] ACF=new short[9];
    private short[] K=new short[9];
    private short[] P=new short[9];    
    private short[] u={0,0,0,0,0,0,0,0};
    
    private int L_z2=0,L_s2,L_temp,L_max,L_result,L_power;  
    private int i,j,k,em;
    private int[] L_ACF=new int[9];    
    
    private short[] signal = new short[160];
    private short[] dp=new short[120];
    private short[] dpp=new short[40];
    private short[] e=new short[40];
    private short[] ep=new short[40];
    private short[] x=new short[40];
    private short[] xm=new short[13];
    private short[] xmc=new short[13];
    private short[] xmp=new short[13];    
    private short[] wt=new short[50];
    
    public Encoder()
    {
    	for(i=0;i<120;i++)
    		dp[i]=0;
    }
    
    public Frame process(Frame frame) {
        // encode into short values
        byte[] data = frame.getData();

        // int k = 0;
        for (i = 0; i < 160; i++) {
            signal[i] = ((short) ((data[i*2 + 1] << 8) | (data[i*2] & 0xFF)));
        }

        Frame res = Memory.allocate(33);
        data = res.getData();
        
        //preprocessing
        downscale(signal);
        offsetCompensation(signal);
        preemphasis(signal);
        
        //lpc analysis
        autocorrelation(signal);
        computeReflection();
        transformToLAR();
        quantizationAndCoding();        
        
        //short term analysis
        LARDecoding();
        shortTermFiltering(signal);

        //switch LARpp arrays
        for(i=1;i<9;i++)
        {
        	LARpprev[i]=LARpp[i];
        	LARpp[i]=0;
        }
                
        //write 5 LARC bytes
        //0XD0 + 4 bits of LARC[1]
        data[0]  = (byte) ( 0xD0 | ((LARc[1] >> 2) & 0xF));
        //2 bits of LARC[1] + 6 bits of LARC[2]
        data[1]  = (byte) ((LARc[1] << 6) | (LARc[2] & 0x3F));
        //5 bits of LARC[3] + 3 bits of LARC[4]
        data[2]  = (byte) ((LARc[3] << 3) | ((LARc[4] >> 2) & 0x7));
        //2 bits of LARC[4] + 4 bits of LARC[5] + 2 bits of LARC[6]
        data[3]  = (byte) ((LARc[4] << 6) | ((LARc[5] & 0xF) << 2) | ((LARc[6] >> 2) & 0x3));
        //2 bits of LARC[6] + 3 bits of LARC[7] + 3 bits of LARC[8]
        data[4]  = (byte) ((LARc[6] << 6) | ((LARc[7] & 0x7) << 3) | (LARc[8] & 0x7));
	
        k_temp=5;
        for(k=0;k<4;k++)
        {
        	//long term
        	calculateLTPParams(signal,k);
        	longTermAnalysis(signal,k);
        	
        	//rpe encoding
        	weightingFilter();
        	gridSelection();
        	ACPMQuantization();
        	ACPMInverseQuantization();
        	RPEPositioning();
        	
        	//updating dp
        	updatedp();
        	
        	//write frame 7 bytes
        	//7 bits of NC + one bits of BC
        	data[k_temp++]  = (byte) ((nc << 1) | ((bc >> 1) & 0x1));
        	//1 bit of BC + 2 bits of MC + 5 bits of XMAXC
        	data[k_temp++]  = (byte) ((bc << 7) | ((mc & 0x3) << 5) | ((xmaxc >> 1) & 0x1F));
        	//1 bit of XMAXC + 3 bits of XMC[0] + 3 bits of XMC[1] + 1 bit of XMC[2]
        	data[k_temp++]  = (byte) ((xmaxc << 7) | ((xmc[0] & 0x7) << 4) | ((xmc[1] & 0x7) << 1) | ((xmc[2] >> 2) & 0x1));
        	//2 bits of XMC[2] + 3 bits of XMC[3] + 3 bits of XMC[4]
        	data[k_temp++]  = (byte) ((xmc[2] << 6) | ((xmc[3] & 0x7) << 3) | (xmc[4] & 0x7));
        	//3 bits of XMC[5] + 3 bits of XMC[6] + 2 bits of XMC[7]
        	data[k_temp++]  = (byte) ((xmc[5] << 5)	| ((xmc[6] & 0x7) << 2) | ((xmc[7] >> 1) & 0x3));
        	//1 bit of XMC[7] + 3 bits of XMC[8] + 3 bits of XMC[9] + 1 bit of XMC[10]
        	data[k_temp++]  = (byte) ((xmc[7] << 7)	| ((xmc[8] & 0x7) << 4) | ((xmc[9] & 0x7) << 1) | ((xmc[10] >> 2) & 0x1));
        	//2 bits of XMC[10]+ 3 bits of XMC[11] + 3 bits of XMC[12]
        	data[k_temp++]  = (byte) ((xmc[10] << 6)	| ((xmc[11] & 0x7) << 3) | (xmc[12] & 0x7)); 
        }
                
        //encoder.encode(signal, res.getData());
        
        res.setOffset(0);
        res.setLength(33);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(gsm);
        
        return res;
    }    

    public Format getSupportedInputFormat() {
        return linear;
    }

    public Format getSupportedOutputFormat() {
        return gsm;
    }
    
    //5.2.1 downscale
    private void downscale(short[] data)
    {
    	for(i=0;i<160;i++)
    		data[i]=(short)((data[i]>>3)<<2);
    }

    //5.2.2 offset compensation
    private void offsetCompensation(short[] data)
    {
    	for(i=0;i<160;i++)
    	{
    		s1=BasicFunctions.sub(data[i],z1);
    		z1=data[i];
    		L_s2=s1;
    		L_s2<<=15;
    		msp=(short)(L_z2 >> 15);
    		lsp=(short)BasicFunctions.L_sub(L_z2,msp<<15);
    		L_s2=BasicFunctions.L_add(L_s2,BasicFunctions.mult_r(lsp,(short)32735));
    		L_z2=BasicFunctions.L_add(BasicFunctions.L_mult(msp,(short)32735)>>1,L_s2);
    		data[i]=(short)(BasicFunctions.L_add(L_z2,16384)>>15);    		
    	}
    }
    
    //5.2.3 offset compensation
    private void preemphasis(short[] data)
    {
    	for(i=0;i<160;i++)
    	{
    		temp=data[i];
    		data[i]=BasicFunctions.add(data[i],BasicFunctions.mult_r(mp,(short)-28180));
    		mp=temp;
    	}
    }
    
    //5.2.4 auto correlation
    private void autocorrelation(short[] data) 
    {    	
    	smax=0;
    	for(i=0;i<160;i++)
    	{
    		temp=BasicFunctions.abs(data[i]);
    		if(temp>smax)
    			smax=temp;
    	}
    	
    	if(smax==0)
    		scalauto=0;
    	else
    		scalauto=BasicFunctions.sub((short)4,BasicFunctions.norm(smax<<16));    		    	
    	
    	if(scalauto>0)
    	{
    		temp=(short)(16384>>(scalauto-1));
    		for(int i=0;i<160;i++)
    			data[i]=BasicFunctions.mult_r(data[i],temp);    		    		
    	}
    	
    	for(i=0;i<9;i++)
    	{
    		L_ACF[i]=0;
    		for(j=i;j<160;j++)
    			L_ACF[i]=BasicFunctions.L_add(L_ACF[i],BasicFunctions.L_mult(data[j],data[j-i]));
    	}
    	
    	if(scalauto>0)
    	{
    		for(i=0;i<160;i++)
    			data[i]=(short)(data[i]<<scalauto);    		    		
    	}    	
    }
    
    //5.2.5 reflection coeficients
    private void computeReflection() 
    {
    	if(L_ACF[0]==0)
    	{
    		for(i=1;i<9;i++)
    			r[i]=0;
    		
    		return;
    	}
    	
    	temp=BasicFunctions.norm(L_ACF[0]);    	
    	
    	for(i=0;i<9;i++)
    		ACF[i]=(short)((L_ACF[i]<<temp)>>16);
    		
    	for(i=1;i<8;i++)
    		K[9-i]=ACF[i];
    	
    	for(i=0;i<9;i++)
    		P[i]=ACF[i];
    	
    	for(i=1;i<9;i++)
    	{
    		if(P[0]<BasicFunctions.abs(P[1]))
    		{
    			for(j=i;j<9;j++)
    				r[j]=0;
    			
    			return;
    		}
    		
    		r[i]=BasicFunctions.div(BasicFunctions.abs(P[1]),P[0]);
    		if(P[1]>0)
    			r[i]=BasicFunctions.sub((short)0,r[i]);
    		
    		if(i==8)
    			return;
    		
    		P[0]=BasicFunctions.add(P[0],BasicFunctions.mult_r(P[1],r[i]));
    		for(j=1;j<=8-i;j++)
    		{
    			P[j]=BasicFunctions.add(P[j+1],BasicFunctions.mult_r(K[9-j],r[i]));
    			K[9-j]=BasicFunctions.add(K[9-j],BasicFunctions.mult_r(K[j+1],r[i]));
    		}
    	}
    }
    
    //5.2.6 transformation of reflection coeficients to Log.-Area Ratios
    private void transformToLAR() 
    {    	
    	for(i=1;i<9;i++)
    	{
    		temp=BasicFunctions.abs(r[i]);
    		if(temp<22118)
    			temp=(short)(temp>>1);
    		else if(temp<31130)
    			temp=BasicFunctions.sub(temp,(short)11059);
    		else
    			temp=(short)(BasicFunctions.sub(temp,(short)26112)<<2);
    		
    		LAR[i]=temp;
    		if(r[i]<0)
    			LAR[i]=BasicFunctions.sub((short)0,LAR[i]);
    	}
    }
    
    //5.2.7 Quantization and coding of the LAR
    private void quantizationAndCoding() 
    {
    	for(i=1;i<9;i++)
    	{
    		temp=BasicFunctions.mult(BasicFunctions.A[i-1],LAR[i]);
    		temp=BasicFunctions.add(temp,BasicFunctions.B[i-1]);
    		temp=BasicFunctions.add(temp,(short)256);
    		LARc[i]=(short)(temp>>9);
    		
    		if(LARc[i]>BasicFunctions.MAC[i-1])
        		LARc[i]=BasicFunctions.MAC[i-1];
        	else if(LARc[i]<BasicFunctions.MIC[i-1])
        		LARc[i]=BasicFunctions.MIC[i-1];
        	
        	LARc[i]=BasicFunctions.sub(LARc[i],BasicFunctions.MIC[i-1]);
    	}    	    	
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
    
    //5.2.9-10 Short term filtering
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
    	
    	//5.2.10 - short term analysis filtering
    	shortTermAnalysis(data,0,12);
    	
    	//*************************************
    	//from 13 to 26
    	//*************************************
    	
    	//5.2.9.1 - Interpolation of LARpp to get LARp
    	for(i=1;i<9;i++)
			LARp[i]=BasicFunctions.add((short)(LARpprev[i]>>1),(short)(LARpp[i]>>1));
		
    	//5.2.9.2 - Computation of rp from interpolated LARp
    	computeRP();
    	
    	//5.2.10 - short term analysis filtering
    	shortTermAnalysis(data,13,26);
    	
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
    	
    	//5.2.10 - short term analysis filtering
    	shortTermAnalysis(data,27,39);
    	
    	//*************************************
    	//from 40 to 159
    	//*************************************
    	//5.2.9.1 - Interpolation of LARpp to get LARp
    	for(i=1;i<9;i++)
			LARp[i]=LARpp[i];		
    	
    	//5.2.9.2 - Computation of rp from interpolated LARp
    	computeRP();
    	
    	//5.2.10 - short term analysis filtering
    	shortTermAnalysis(data,40,159);
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
    
    private void shortTermAnalysis(short[] data,int startIndex,int endIndex)
    {
    	for(j=startIndex;j<=endIndex;j++)
    	{
    		di=data[j];
    		sav=di;
    		for(i=1;i<9;i++)
    		{
    			temp=BasicFunctions.add(u[i-1],BasicFunctions.mult_r(rp[i],di));
    			di=BasicFunctions.add(di,BasicFunctions.mult_r(rp[i],u[i-1]));
    			u[i-1]=sav;
    			sav=temp;
    		}
    		
    		data[j]=di;
    	}
    }
    
    //5.2.11 Calculation of LTP Parameters
    private void calculateLTPParams(short[] data,int step)
    {
    	//search for optimum scaling
    	temp2=(short)(40*(step+1));
    	dmax=0;
    	for(i=40*step;i<temp2;i++)
    	{
    		temp=BasicFunctions.abs(data[i]);
    		if(temp>dmax)
    			dmax=temp;
    	}
    	
    	temp=0;
    	if(dmax==0)
    		scal=0;
    	else
    		temp=BasicFunctions.norm(dmax<<16);
    	
    	if(temp>6)
    		scal=0;
    	else
    		scal=BasicFunctions.sub((short)6,temp);
    	
    	//initializing working array
    	temp2=(short)(40*step);
    	for(i=0;i<40;i++)
    	{
    		wt[i]=(short)(data[temp2]>>scal);
    		temp2+=1;
    	}    	
    	
    	//searching for maximum cross-correlation
    	L_max=0;
    	nc=40;
    	
    	for(j=40;j<=120;j++)
    	{
    		L_result=0;    		
    		for(i=0;i<40;i++)
        	{
    			L_temp=BasicFunctions.L_mult(wt[i],dp[i-j+120]);
    			L_result=BasicFunctions.L_add(L_temp,L_result);
        	}
    		
    		if(L_result>L_max)
    		{
    			nc=(short)j;
    			L_max=L_result;
    		}
    	}
    	
    	//Rescaling L_max
    	L_max=L_max>>BasicFunctions.sub((short)6,scal);
    	
    	//initializing working array
    	for(i=0;i<40;i++)
    		wt[i]=(short)(dp[i-nc+120]>>3);
    	
    	//compute a power
    	L_power=0;
    	for(i=0;i<40;i++)
    	{
    		L_temp=BasicFunctions.L_mult(wt[i],wt[i]);
    		L_power=BasicFunctions.L_add(L_temp,L_power);
    	}
    	
    	//normalization of L_max and L_power
    	if(L_max<=0)
    	{
    		bc=0;
    		return;
    	}
    	
    	if(L_max>=L_power)
    	{
    		bc=3;
    		return;
    	}
    	
    	temp=BasicFunctions.norm(L_power);
    	R=(short)((L_max<<temp)>>16);
    	S=(short)((L_power<<temp)>>16);
    
    	//coding of ltp gain
    	for(bc=0;bc<3;bc++)
    		if(R<=BasicFunctions.mult(S,BasicFunctions.DLB[bc]))
    			return;
    }
    
    //5.2.12 Long Term analysis filtering
    private void longTermAnalysis(short[] data,int step)
    {
    	bp=BasicFunctions.QLB[bc];
    	
    	temp2=(short)(40*step);
    	for(i=0;i<40;i++)
    	{
    		dpp[i]=BasicFunctions.mult_r(bp,dp[i-nc+120]);
    		e[i]=BasicFunctions.sub(data[temp2],dpp[i]);
    		temp2+=1;
    	}
    }
    
    //5.2.13 Weighting filter
    private void weightingFilter()
    {
    	for(i=0;i<5;i++)
    		wt[i]=0;
    	
    	for(;i<45;i++)
    		wt[i]=e[i-5];
    	
    	for(;i<50;i++)
    		wt[i]=0;
    	
    	for(i=0;i<40;i++)
    	{
    		L_result=8192;
    		
    		for(j=0;j<11;j++)
    		{
    			L_temp=BasicFunctions.L_mult(wt[i+j],BasicFunctions.H[j]);
    			L_result=BasicFunctions.L_add(L_result,L_temp);
    		}
    		
    		L_result=BasicFunctions.L_add(L_result,L_result);
    		L_result=BasicFunctions.L_add(L_result,L_result);
    		x[i]=(short)(L_result>>16);    		
    	}
    }
    
    //5.2.14 Grid selection
    private void gridSelection()
    {
    	em=0;
    	mc=0;
    	
    	for(j=0;j<4;j++)
    	{
    		L_result=0;
    		for(i=0;i<13;i++)
    		{
    			temp=(short)(x[j+(3*i)]>>2);
    			L_temp=BasicFunctions.L_mult(temp,temp);
    			L_result=BasicFunctions.L_add(L_temp,L_result);
    		}
    		
    		if(L_result>em)
    		{
    			mc=(short)j;
    			em=L_result;
    		}
    	}
    	
    	for(i=0;i<13;i++)
    		xm[i]=x[mc+3*i];
    }
    
    //5.2.15 ACPM Quantization of selected RPE sequence
    private void ACPMQuantization() 
    {
    	//find maximum of xm
    	xmax=0;
    	for(i=0;i<13;i++)
    	{
    		temp=BasicFunctions.abs(xm[i]);
    		if(temp>xmax)
    			xmax=temp;
    	}
    	
    	//quantization and coding of xmax to get xmaxc 
    	exp=0;
    	temp=(short)(xmax>>9);
    	itest=0;
    	for(i=0;i<6;i++)
    	{
    		if(temp<=0)
    			itest=1;
    		
    		temp=(short)(temp>>1);
    		if(itest==0)
    			exp=BasicFunctions.add(exp,(short)1);
    	}    	
    	
    	temp=BasicFunctions.add(exp,(short)5);
    	xmaxc=BasicFunctions.add((short)(xmax>>temp),(short)(exp<<3));
    	
    	//compute exponent and mantissa
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
    	
    	//direct computation of xmc
    	temp1=BasicFunctions.sub((short)6,exp);
    	temp2=BasicFunctions.NRFAC[mant];
    	
    	for(i=0;i<13;i++)
    	{
    		temp=(short)(xm[i]<<temp1);
    		temp=BasicFunctions.mult(temp,temp2);
    		xmc[i]=BasicFunctions.add((short)(temp>>12),(short)4);
    	}
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
    		ep[i]=0;
    	
    	for(i=0;i<13;i++)
    		ep[mc+3*i]=xmp[i];
    }
    
    //5.2.18 Update short term residual signal
    private void updatedp() 
    {
    	for(i=0;i<80;i++)
    		dp[i]=dp[i+40];
    	
    	temp=0;
    	for(;i<120;i++,temp++)
    		dp[i]=BasicFunctions.add(ep[temp],dpp[temp]);
    }
}
