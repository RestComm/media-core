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
 * 
 */
public class Encoder implements Codec {

    private final static Format ilbc = FormatFactory.createAudioFormat("ilbc", 8000, 16, 1);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private short[] signal = new short[160];
    
    private int i,j;    
    private int tempL,temp,temp2,temp3;
    private int nbits,scale;
    private int r[]=new int[11];    
    
    private short smax,tempS,hi,low,tempHi,tempLow;
    private short alpha,alphaLow,alphaHi;
    private short[] rc=new short[10];
    private short[] analysisState=new short[10];
    private short[] LSF=new short[10];
    private short[] LSFold=new short[10];
    private short[] LSFdeq=new short[10];
    private short[] LSFdeqOld=new short[10];
    private short[] lpcBuffer=new short[300];
    private short[] windowedData=new short[240];
    private short[] hpStateX=new short[2];
    private short[] hpStateY=new short[4];    
    private short[] a=new short[11];
    private short rHi[]=new short[11];
    private short rLow[]=new short[11];
    private short aHi[]=new short[11];
    private short aLow[]=new short[11];
    private short aNewHi[]=new short[11];
    private short aNewLow[]=new short[11];
    
    private boolean isStable;
    
    public Frame process(Frame frame) {
    	byte[] data = frame.getData();
    	
    	for (i = 0; i < 160; i++) {
            signal[i] = ((short) ((data[i*2 + 1] << 8) | (data[i*2] & 0xFF)));
        }
    	
    	hpInput(signal);
    	lpcAnalysis(signal);
    	
    	Frame res = Memory.allocate(320);
        System.arraycopy( frame.getData(), 0, res.getData(), 0, 320 );

        res.setOffset(0);
        res.setLength(320);
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
    
    private void hpInput(short[] data)
    {    	
    	for (i=0; i<data.length; i++) 
    	{
    	    temp  = hpStateY[1]*Constants.HP_IN_COEFICIENTS[3] + hpStateY[3]*Constants.HP_IN_COEFICIENTS[4];
    	    temp >>= 15;
    	    temp += hpStateY[0]*Constants.HP_IN_COEFICIENTS[3] + hpStateY[2]*Constants.HP_IN_COEFICIENTS[4];
    	    temp <<= 1;
    	    temp += data[i]*Constants.HP_IN_COEFICIENTS[0] + hpStateX[0]*Constants.HP_IN_COEFICIENTS[1] + hpStateX[1]*Constants.HP_IN_COEFICIENTS[2];

    	    hpStateX[1] = hpStateX[0];
    	    hpStateX[0] = data[i];

    	    temp2=temp + 4096;
    	    if(temp2>268435455)
    	    	temp2 =	268435455;
    	    else if(temp2<-268435456)
    	     	temp2 =	-268435456;

    	    data[i] = (short)(temp2>>13);

    	    hpStateY[2] = hpStateY[0];
    	    hpStateY[3] = hpStateY[1];

    	    if (temp>268435455)
    	      temp = Integer.MAX_VALUE;
    	    else if (temp<-268435456)
    	      temp = Integer.MIN_VALUE;
    	    else
    	      temp = temp<<2;    	    

    	    hpStateY[0] = (short)(temp >> 16);
    	    hpStateY[1] = (short)((temp - (((int)hpStateY[0])<<16))>>1);
    	}
    }
    
    private void lpcAnalysis(short[] data)
    {    	
    	temp=140;
    	for(i=0;i<data.length;i++,temp++)
    		lpcBuffer[temp]=data[i];
    	
    	
    	temp=60;
    	for(i=0;i<240;i++,temp++)
    		windowedData[i] = (short)((lpcBuffer[temp]*Constants.LPC_ASYM_WIN[i]) >> 15);    			
    	
    	smax = 0;
		for(i=0;i<windowedData.length;i++)
		{
			tempS=Constants.abs(windowedData[i]);
			if(smax<tempS)
				smax = tempS;			
		}
		
	    if (smax == 0)
	        scale = 0;
	    else
	    {
	        temp = Constants.norm(smax*smax);
	        if (temp > 8)
	            scale = 0;
	        else
	            scale = 8 - temp;	        
	    }

	    for (i = 0; i < windowedData.length; i++)
	    {
	    	r[i] = 0;
	    	temp=0;
	    	temp2=i;
	        	        
	        for (j = windowedData.length - i; j > 0; j--,temp++,temp2++)
	        	r[i] +=(windowedData[temp]*windowedData[temp2]) >> scale;	        			       
	    }

    	tempL = Constants.norm(r[0]);
    	for(i=0;i<r.length;i++)
    		r[i]=r[i]<<tempL;
    	    	
    	for (i = 0; i < 11; i++) {
    	    temp = (short)(r[i]>>16);
    	    temp2 = (short) (Constants.LPC_LAG_WIN[i]>>16);    	    
    	    r[i]=(((r[i] - temp<<16)>>1)*temp2)>>14;
    	    r[i]+=(temp*((Constants.LPC_LAG_WIN[i] - temp2<<16)>>1))>>14;
    	    r[i]+= (temp*temp2)<<1;
    	}
    	
    	for(i=0;i<r.length;i++)
    		r[i]=r[i]>>tempL;
    	
    	isStable=calculateACoefs();

        if (!isStable) {
        	a[0]=(short)4096;
        	for(i=1;i<a.length;i++)
        		a[i]=0;        	
        }

          /* Bandwidth expand the filter coefficients */
    	//WebRtcIlbcfix_BwExpand(A, A, (WebRtc_Word16*)WebRtcIlbcfix_kLpcChirpSyntDenum, LPC_FILTERORDER+1);

          /* Convert from A to LSF representation */
    	//WebRtcIlbcfix_Poly2Lsf(lsf + k*LPC_FILTERORDER, A);
        
    	temp=140;
    	for(i=0;i<160;i++,temp++)
    		lpcBuffer[i]=lpcBuffer[temp];        
    }	
    
    private boolean calculateACoefs()
    {
    	nbits = Constants.norm(r[0]);

        for (i = 0; i <= 10; i++)
        {
            tempL = r[i]<<nbits;
            rHi[i] = (short)(tempL>>16);
            rLow[i] = (short)((tempL- rHi[i]<<16)>>1);
        }

        temp2 = rHi[1]<<16 + rLow[1]<<1;
        if(temp2>0)
        	temp3 = temp2;
        else
        	temp3=-temp2;
        
        tempL = Constants.div(temp3, rHi[0], rLow[0]);
        
        if (temp2 > 0)
            tempL = -tempL;
        
        hi = (short)(tempL>>16);
        low = (short)((tempL- hi<<16)>>1);

        rc[0] = hi;

        tempL = tempL>>4;

        aHi[1] = (short)(tempL>>16);
        aLow[1] = (short)((tempL-hi<<16)>>1);

        tempL = (((hi*low) >> 14) + (hi*hi)<< 1);
        if(tempL<0)
        	tempL = -tempL;
        
        tempL = Integer.MAX_VALUE - tempL;

        tempHi = (short)(tempL>>16);
        tempLow = (short)((tempL-tempHi<<16)>>1);
        
        tempL = (rHi[0]*tempHi + (rHi[0]*tempLow) >> 15 + ((rLow[0]*tempHi) >> 15)) << 1;

        alpha = Constants.norm(tempL);
        tempL = tempL<<alpha;
        alphaHi = (short)(tempL>>16);
        alphaLow = (short)((tempL-tempHi<<16)>>1);
        
        for (i = 2; i <= 10; i++)
        {
            tempL = 0;

            for (j = 1; j < i; j++)
                tempL += ((rHi[j]*aHi[i-j])<<1 + (rHi[j]*aLow[i-j]) >> 15 + (rLow[j]*aHi[i-j]) >> 15) << 1;
            
            tempL = tempL<<4;
            tempL += rHi[i]<<16 + rLow[i]>>1;
            
            if(tempL>0)
            	temp2 = tempL;
            else
            	temp2=0-tempL;
            
            temp3=Constants.div(temp2,alphaHi,alphaLow);            

            if (tempL > 0)
                temp3 = -temp3;
            
            nbits=Constants.norm(temp3);
            if ((alpha <= nbits) || (temp3 == 0))
                temp3 = temp3<<alpha;
            else
            {
                if (temp3 > 0)
                    temp3 = Integer.MAX_VALUE;
                else
                    temp3 = Integer.MIN_VALUE;                
            }

            hi = (short)(temp3>>16);
            low = (short)((temp3- hi<<16)>>1);

            rc[i - 1] = hi;
            
            if (Constants.abs(hi) > 32750)
                return false;
            
            for (j = 1; j < i; j++)
            {
                tempL = aHi[j]<<16 + aLow[j]<<1;
                tempL += (hi*aHi[i-j] + (hi*aLow[i-j]) >> 15 + (low*aHi[i-j]) >> 15) << 1;

                // Put Anew in hi and low format
                aNewHi[j] = (short)(tempL>>16);
                aNewLow[j] = (short)((tempL-aNewHi[j]<<16)>>1);                                 
            }

            temp3 = temp3>>4;

            aNewHi[j] = (short)(temp3>>16);
            aNewLow[j] = (short)((temp3-aNewHi[j]<<16)>>1);

            tempL = (((hi*low) >> 14) + (hi*hi)<< 1);
            if(tempL<0)
            	tempL = -tempL;
            
            tempL = Integer.MAX_VALUE - tempL;
            
            tempHi = (short)(tempL>>16);
            tempLow = (short)((tempL-tempHi<<16)>>1);
            
            tempL = (alphaHi*tempHi + (alphaHi*tempLow) >> 15 + (alphaLow*tempLow) >> 15) << 1;
            nbits = Constants.norm(tempL);
            tempL = tempL<<nbits;

            alphaHi = (short)(tempL>>16);
            alphaLow = (short)((tempL-alphaHi<<16)>>1);
            
            alpha = (short)(alpha + nbits);

            for (j = 1; j <= i; j++)
            {
                aHi[j] = aNewHi[j];
                aLow[j] = aNewLow[j];
            }
        }

        a[0] = 4096;
        for (i = 1; i <= 10; i++)
        {
            tempL = aHi[i]<<16 + aLow[i]>>1;
            a[i] = (short)(((tempL<<1)+32768)>>16);
        }
        
        return true;
    }
}
