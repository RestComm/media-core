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
public class DecoderState 
{
	public short DECODER_MODE=20;
	public short SIZE=160;
	public short SUBFRAMES=4;
	public short NASUB=2;
	public short BYTES=38;
	public short WORDS=19;
	public short LPC_N=1;
	public short STATE_SHORT_LEN=57;	
	
	private short[] synthMem=new short[10];
	private short[] lsfDeqOld=new short[10];
	private short[] prevLpc=new short[11];
	private short[] prevResidual=new short[240];
	private short[] oldSyntDenum=new short[66];
	private short[] enhancementBuffer=new short[643];
	private short[] enhancementPeriod=new short[8];
	private short[] hpiMemX=new short[2];
	private short[] hpiMemY=new short[4];
	
	private int lastTag,consPliCount,prevEnchPl,useEnhancer;
	private short perSquare,prevScale,prevPli,prevLag,seed;				
	
	public DecoderState()
	{			 		
	}
	
	public void setMode(int mode)
	{
		if(mode==20)
		{
			DECODER_MODE=20;
			SIZE=160;
			SUBFRAMES=4;
			NASUB=2;
			BYTES=38;
			WORDS=19;
			LPC_N=1;
			STATE_SHORT_LEN=57;
		}
		else
		{
			DECODER_MODE=20;
			SIZE=240;
			SUBFRAMES=6;
			NASUB=4;
			BYTES=50;
			WORDS=25;
			LPC_N=2;
			STATE_SHORT_LEN=58;
		}
	}
	
	public short[] getSynthMem()
	{
		return this.synthMem;
	}		
	
	public short[] getLsfDeqOld()
	{
		return this.lsfDeqOld;
	}
	
	public short[] getPrevLpc()
	{
		return this.prevLpc;
	}
	
	public short[] getPrevResidual()
	{
		return this.prevResidual;
	}
	
	public short[] getOldSyntDenum()
	{
		return this.oldSyntDenum;
	}	
	
	public short[] getEnhancementBuffer()
	{
		return this.enhancementBuffer;
	}
	
	public short[] getEnhancementPeriod()
	{
		return this.enhancementPeriod;
	}
	
	public short[] getHpiMemX()
	{
		return this.hpiMemX;
	}
	
	public short[] getHpiMemY()
	{
		return this.hpiMemY;
	}
	
	public int getLastTag()
	{
		return this.lastTag;
	}
	
	public void setLastTag(int lastTag)
	{
		this.lastTag=lastTag;
	}
	
	public int getConsPliCount()
	{
		return this.consPliCount;
	}
	
	public void setConsPliCount(int consPliCount)
	{
		this.consPliCount=consPliCount;
	}
	
	public int getPrevEnchPl()
	{
		return this.prevEnchPl;
	}
	
	public void setPrevEnchPl(int prevEnchPl)
	{
		this.prevEnchPl=prevEnchPl;
	}
	
	public int getUseEnhancer()
	{
		return this.useEnhancer;
	}
	
	public void setUseEnhancer(int useEnhancer)
	{
		this.useEnhancer=useEnhancer;
	}
	
	public short getPerSquare()
	{
		return this.perSquare;
	}
	
	public void setPerSquare(short perSquare)
	{
		this.perSquare=perSquare;
	}
	
	public short getPrevScale()
	{
		return this.prevScale;
	}
	
	public void setPrevScale(short prevScale)
	{
		this.prevScale=prevScale;
	}
	
	public short getPrevPli()
	{
		return this.prevPli;
	}
	
	public void setPrevPli(short prevPli)
	{
		this.prevPli=prevPli;
	}
	
	public short getPrevLag()
	{
		return this.prevLag;
	}
	
	public void setPrevLag(short prevLag)
	{
		this.prevLag=prevLag;
	}
	
	public short getSeed()
	{
		return this.seed;
	}
	
	public void setSeed(short seed)
	{
		this.seed=seed;
	}	
}
