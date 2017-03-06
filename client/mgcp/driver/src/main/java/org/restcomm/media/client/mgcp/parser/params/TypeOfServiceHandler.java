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

import jain.protocol.ip.mgcp.message.parms.TypeOfService;

public class TypeOfServiceHandler
{
	public static TypeOfService decode(byte[] value,int offset,int length) throws ParseException
	{
		switch(length)
		{
			case 1:
				if(value[offset]>=StringFunctions.ZERO_BYTE && value[offset]<=StringFunctions.NINE_BYTE)
					return new TypeOfService((byte)(value[offset]-StringFunctions.ZERO_BYTE));
				else if(value[offset]>=StringFunctions.LOW_A_BYTE && value[offset]<=StringFunctions.LOW_F_BYTE)
					return new TypeOfService((byte)(value[offset]-StringFunctions.LOW_A_BYTE));
				else if(value[offset]>=StringFunctions.HIGH_A_BYTE && value[offset]<=StringFunctions.HIGH_F_BYTE)
					return new TypeOfService((byte)(value[offset]-StringFunctions.HIGH_A_BYTE));
				break;				
			case 2:
				int result=0;
				if(value[offset]>=StringFunctions.ZERO_BYTE && value[offset]<=StringFunctions.NINE_BYTE)
					result=(value[offset]-StringFunctions.ZERO_BYTE)<<4;
				else if(value[offset]>=StringFunctions.LOW_A_BYTE && value[offset]<=StringFunctions.LOW_F_BYTE)
					result=(value[offset]-StringFunctions.LOW_A_BYTE)<<4;
				else if(value[offset]>=StringFunctions.HIGH_A_BYTE && value[offset]<=StringFunctions.HIGH_F_BYTE)
					result=(value[offset]-StringFunctions.HIGH_A_BYTE)<<4;
				else
					throw new ParseException("Invalid value for TypeOfService :" + value, 0);
				
				if(value[offset+1]>=StringFunctions.ZERO_BYTE && value[offset+1]<=StringFunctions.NINE_BYTE)
					return new TypeOfService((byte)(result + value[offset+1]-StringFunctions.ZERO_BYTE));
				else if(value[offset+1]>=StringFunctions.LOW_A_BYTE && value[offset+1]<=StringFunctions.LOW_F_BYTE)
					return new TypeOfService((byte)(result + value[offset+1]-StringFunctions.LOW_A_BYTE));
				else if(value[offset+1]>=StringFunctions.HIGH_A_BYTE && value[offset+1]<=StringFunctions.HIGH_F_BYTE)
					return new TypeOfService((byte)(result + value[offset+1]-StringFunctions.HIGH_A_BYTE));
				
				break;			
		}
		
		throw new ParseException("Invalid value for TypeOfService :" + value, 0);			
	}
	
	public static int encode(byte[] destination,int offset,TypeOfService typeOfService)
	{				
		int usedLength=StringFunctions.encodeInt(destination,offset,typeOfService.getTypeOfService());
		return usedLength;
	}
}