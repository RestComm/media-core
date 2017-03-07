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
public class CbUpdateIndexData
{
	private int critMax;
	private short shTotMax; 
	private short bestIndex;
	private short bestGain;
	
	public CbUpdateIndexData()
	{			 		
	}	
	
	public int getCritMax()
	{
		return this.critMax;
	}
	
	public void setCritMax(int value)
	{
		this.critMax=value;
	}
	
	public short getShTotMax()
	{
		return this.shTotMax;
	}
	
	public void setShTotMax(short value)
	{
		this.shTotMax=value;
	}
	
	public short getBestIndex()
	{
		return this.bestIndex;
	}
	
	public void setBestIndex(short value)
	{
		this.bestIndex=value;
	}
	
	public short getBestGain()
	{
		return this.bestGain;
	}
	
	public void setBestGain(short value)
	{
		this.bestGain=value;
	}
}
