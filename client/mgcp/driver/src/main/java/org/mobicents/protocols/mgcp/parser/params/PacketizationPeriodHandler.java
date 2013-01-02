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

package org.mobicents.protocols.mgcp.parser.params;

import java.text.ParseException;

import org.mobicents.protocols.mgcp.parser.StringFunctions;
import jain.protocol.ip.mgcp.message.parms.PacketizationPeriod;

public class PacketizationPeriodHandler
{
	public static PacketizationPeriod decode(byte[] value,int offset,int length) throws ParseException
	{
		int lowValue=0;
		int highValue=0;
		int i=0;
		int currIndex=offset;
		
		for(;i<length;i++,currIndex++)
		{			
			if(value[currIndex]==StringFunctions.MINUS_BYTE)
				break;
			else if(value[currIndex]>=StringFunctions.ZERO_BYTE && value[currIndex]<=StringFunctions.NINE_BYTE)
				lowValue=lowValue*10+(value[currIndex]-StringFunctions.ZERO_BYTE);
			else 
				throw new ParseException("Invalid packetization period:" + new String(value,offset,length), 0);
		}
		
		if(i==length)
			return new PacketizationPeriod(lowValue);
		
		i++;
		currIndex++;
		
		for(;i<length;i++,currIndex++)
		{			
			if(value[currIndex]>=StringFunctions.ZERO_BYTE && value[currIndex]<=StringFunctions.NINE_BYTE)
				highValue=highValue*10+(value[currIndex]-StringFunctions.ZERO_BYTE);
			else 
				throw new ParseException("Invalid packetization period:" + new String(value,offset,length), 0);
		}
		
		return new PacketizationPeriod(lowValue, highValue);		
	}
	
	public static int encode(byte[] destination,int offset,PacketizationPeriod packetizationPeriod)
	{				
		int usedLength=StringFunctions.encodeInt(destination,offset,packetizationPeriod.getPacketizationPeriodLowerBound());
		if(packetizationPeriod.getPacketizationPeriodLowerBound()!=packetizationPeriod.getPacketizationPeriodUpperBound())
		{
			destination[offset+usedLength]=StringFunctions.MINUS_BYTE;
			usedLength++;
			usedLength+=StringFunctions.encodeInt(destination,offset+usedLength,packetizationPeriod.getPacketizationPeriodUpperBound());
		}
		
		return usedLength;
	}
}