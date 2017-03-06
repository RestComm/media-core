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
public class EncoderState 
{
	public static final short ENCODER_MODE=20;
	public static final short SIZE=160;
	public static final short SUBFRAMES=4;
	public static final short NASUB=2;
	public static final short BYTES=38;
	public static final short WORDS=19;
	public static final short LPC_N=1;
	public static final short STATE_SHORT_LEN=57;	
	
	private short[] anaMem=new short[10];
	private short[] lsfOld=new short[10];
	private short[] lsfDeqOld=new short[10];
	private short[] lpcBuffer=new short[300];
	private short[] hpiMemX=new short[2];
	private short[] hpiMemY=new short[4];
	
	public EncoderState()
	{
		System.arraycopy(Constants.LSF_MEAN, 0, lsfOld, 0, Constants.LSF_MEAN.length);
		System.arraycopy(Constants.LSF_MEAN, 0, lsfDeqOld, 0, Constants.LSF_MEAN.length);	 		 
	}
	
	public short[] getAnaMem()
	{
		return this.anaMem;
	}
	
	public short[] getLsfOld()
	{
		return this.lsfOld;
	}
	
	public short[] getLsfDeqOld()
	{
		return this.lsfDeqOld;
	}
	
	public short[] getLpcBuffer()
	{
		return this.lpcBuffer;
	}
	
	public short[] getHpiMemX()
	{
		return this.hpiMemX;
	}
	
	public short[] getHpiMemY()
	{
		return this.hpiMemY;
	}
}
