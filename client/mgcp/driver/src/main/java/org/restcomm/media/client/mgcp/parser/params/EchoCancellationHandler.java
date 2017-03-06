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

package org.restcomm.media.client.mgcp.parser.params;

import java.text.ParseException;

import org.restcomm.media.client.mgcp.parser.StringFunctions;

import jain.protocol.ip.mgcp.message.parms.EchoCancellation;

public class EchoCancellationHandler
{
	public static EchoCancellation decode(byte[] value,int offset,int length) throws ParseException
	{
		if(length==2)
		{
			byte char0=value[offset];
			byte char1=value[offset+1];
			
			if((char0==StringFunctions.LOW_O_BYTE || char0==StringFunctions.HIGH_O_BYTE)
			&& (char1==StringFunctions.LOW_N_BYTE || char1==StringFunctions.HIGH_N_BYTE))					
				return EchoCancellation.EchoCancellationOn;
		}
		else if(length==3)
		{
			byte char0=value[offset];
			byte char1=value[offset+1];
			byte char2=value[offset+1];
			if((char0==StringFunctions.LOW_O_BYTE || char0==StringFunctions.HIGH_O_BYTE)
			&& (char1==StringFunctions.LOW_F_BYTE || char1==StringFunctions.HIGH_F_BYTE)
			&& (char2==StringFunctions.LOW_F_BYTE || char2==StringFunctions.HIGH_F_BYTE))					
				return EchoCancellation.EchoCancellationOff;
		}
		
		throw new ParseException("Invalid value for EchoCancellation :" + new String(value,offset,length), 0);		
	}
	
	public static int encode(byte[] destination,int offset,EchoCancellation echoCancellation)
	{
		if (echoCancellation.getEchoCancellation()) {
			destination[offset]=StringFunctions.LOW_O_BYTE;
			destination[offset+1]=StringFunctions.LOW_N_BYTE;
			return 2;
		} else {
			destination[offset]=StringFunctions.LOW_O_BYTE;
			destination[offset+1]=StringFunctions.LOW_F_BYTE;
			destination[offset+2]=StringFunctions.LOW_F_BYTE;
			return 3;
		}		
	}
}