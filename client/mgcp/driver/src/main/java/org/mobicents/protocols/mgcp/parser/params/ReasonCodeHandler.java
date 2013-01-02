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

import jain.protocol.ip.mgcp.message.parms.ReasonCode;

public class ReasonCodeHandler
{
	public static ReasonCode decode(byte[] value,int offset,int length) throws ParseException
	{		
		int currIndex=offset;
		int numericValue=0;
		for(int i=0;i<length;i++,currIndex++)
		{			
			if(value[currIndex]>=StringFunctions.ZERO_BYTE && value[currIndex]<=StringFunctions.NINE_BYTE)
				numericValue=numericValue*10+(value[currIndex]-StringFunctions.ZERO_BYTE);
			else if(i>0 && value[currIndex]==StringFunctions.SPACE_BYTE)
				break;
			else 
				throw new ParseException("Invalid reason code:" + new String(value,offset,length), 0);
		}
		
		switch(numericValue)
		{
			case ReasonCode.ENDPOINT_MALFUNCTIONING:
				return ReasonCode.Endpoint_Malfunctioning;				
			case ReasonCode.ENDPOINT_OUT_OF_SERVICE:
				return ReasonCode.Endpoint_Out_Of_Service;				
			case ReasonCode.ENDPOINT_STATE_IS_NOMINAL:
				return ReasonCode.Endpoint_State_Is_Nominal;				
			case ReasonCode.LOSS_OF_LOWER_LAYER_CONNECTIVITY:
				return ReasonCode.Loss_Of_Lower_Layer_Connectivity;				
		}
		
		return null;									
	}
	
	public static int encode(byte[] destination,int offset,ReasonCode reasonCode)
	{		
		int totalLength=StringFunctions.encodeInt(destination,offset,reasonCode.getValue());
		destination[offset+totalLength]=StringFunctions.SPACE_BYTE;
		totalLength++;
		byte[] commentData=reasonCode.getComment().getBytes();
		System.arraycopy(commentData,0,destination,offset+totalLength,commentData.length);
		return totalLength+commentData.length;
	}	
}