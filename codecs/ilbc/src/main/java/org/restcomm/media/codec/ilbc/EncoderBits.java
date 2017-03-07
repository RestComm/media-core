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
public class EncoderBits
{
	private short[] LSF=new short[6];
	private short[] cbIndex=new short[15];
	private short[] gainIndex=new short[15];
	private short[] idxVec=new short[58];
    private short idxForMax;
    private boolean stateFirst;
    private short firstBits;
    private short startIdx;    
	
	public EncoderBits()
	{			 		
	}
	
	public short[] getLSF()
	{
		return this.LSF;
	}
	
	public short[] getCbIndex()
	{
		return this.cbIndex;
	}
	
	public short[] getGainIndex()
	{
		return this.gainIndex;
	}
	
	public short[] getIdxVec()
	{
		return this.idxVec;
	}
	
	public short getIdxForMax()
	{
		return this.idxForMax;
	}
	
	public void setIdxForMax(short value)
	{
		this.idxForMax=value;
	}
	
	public boolean getStateFirst()
	{
		return this.stateFirst;
	}
	
	public void setStateFirst(boolean value)
	{
		this.stateFirst=value;
	}
	
	public short getFirstBits()
	{
		return this.firstBits;
	}
	
	public void setFirstBits(short value)
	{
		this.firstBits=value;
	}
	
	public short getStartIdx()
	{
		return this.startIdx;
	}
	
	public void setStartIdx(short value)
	{
		this.startIdx=value;
	}
}
