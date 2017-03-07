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

import org.restcomm.media.client.mgcp.parser.SplitDetails;
import org.restcomm.media.client.mgcp.parser.StringFunctions;

import jain.protocol.ip.mgcp.message.parms.InfoCode;

public class InfoCodeHandler
{
	public static InfoCode decode(byte[] value,int offset,int length) throws ParseException
	{		
		int startIndex=offset;
		int newLength=length;
		for(int i=0;i<length;i++)
		{
			if(value[offset+i]==StringFunctions.SPACE_BYTE)
			{
				startIndex++;
				newLength--;
			}
		}
		
		int finalLength=newLength;
		for(int i=0;i<newLength;i++)
		{
			if(value[offset+length-i-1]==StringFunctions.SPACE_BYTE)
				finalLength--;			
		}
		
		if(finalLength==1)
		{						
			switch(value[startIndex])
			{
				case StringFunctions.LOW_B_BYTE:
				case StringFunctions.HIGH_B_BYTE:
					return InfoCode.BearerInformation;
				case StringFunctions.LOW_C_BYTE:
				case StringFunctions.HIGH_C_BYTE:					
					return InfoCode.CallIdentifier; 
				case StringFunctions.LOW_I_BYTE:
				case StringFunctions.HIGH_I_BYTE:
					return InfoCode.ConnectionIdentifier;
				case StringFunctions.LOW_N_BYTE:
				case StringFunctions.HIGH_N_BYTE:
					return InfoCode.NotifiedEntity;
				case StringFunctions.LOW_X_BYTE:
				case StringFunctions.HIGH_X_BYTE:
					return InfoCode.RequestIdentifier;
				case StringFunctions.LOW_L_BYTE:
				case StringFunctions.HIGH_L_BYTE:
					return InfoCode.LocalConnectionOptions;
				case StringFunctions.LOW_M_BYTE:
				case StringFunctions.HIGH_M_BYTE:
					return InfoCode.ConnectionMode;
				case StringFunctions.LOW_R_BYTE:
				case StringFunctions.HIGH_R_BYTE:
					return InfoCode.RequestedEvents;
				case StringFunctions.LOW_S_BYTE:
				case StringFunctions.HIGH_S_BYTE:
					return InfoCode.SignalRequests;
				case StringFunctions.LOW_D_BYTE:
				case StringFunctions.HIGH_D_BYTE:				
					return InfoCode.DigitMap;										
				case StringFunctions.LOW_O_BYTE:
				case StringFunctions.HIGH_O_BYTE:				
					return InfoCode.ObservedEvents;					
				case StringFunctions.LOW_P_BYTE:
				case StringFunctions.HIGH_P_BYTE:				
					return InfoCode.ConnectionParameters;					
				case StringFunctions.LOW_E_BYTE:
				case StringFunctions.HIGH_E_BYTE:				
					return InfoCode.ReasonCode;					
				case StringFunctions.LOW_Z_BYTE:
				case StringFunctions.HIGH_Z_BYTE:
					return InfoCode.SpecificEndpointID;					
				case StringFunctions.LOW_Q_BYTE:
				case StringFunctions.HIGH_Q_BYTE:
					return InfoCode.QuarantineHandling;					
				case StringFunctions.LOW_T_BYTE:
				case StringFunctions.HIGH_T_BYTE:				
					return InfoCode.DetectEvents;					
				case StringFunctions.LOW_A_BYTE:
				case StringFunctions.HIGH_A_BYTE:				
					return InfoCode.Capabilities;				
			}
		}
		else if(finalLength==2)
		{
			switch(value[startIndex])
			{
				case StringFunctions.LOW_L_BYTE:
				case StringFunctions.HIGH_L_BYTE:
					if(value[startIndex+1]==StringFunctions.LOW_C_BYTE || value[startIndex+1]==StringFunctions.HIGH_C_BYTE)
						return InfoCode.LocalConnectionDescriptor; 
				case StringFunctions.LOW_E_BYTE:
				case StringFunctions.HIGH_E_BYTE:
					if(value[startIndex+1]==StringFunctions.LOW_S_BYTE || value[startIndex+1]==StringFunctions.HIGH_S_BYTE)
						return InfoCode.EventStates;
				case StringFunctions.LOW_R_BYTE:
				case StringFunctions.HIGH_R_BYTE:
					switch(value[startIndex+1])
					{
						case StringFunctions.LOW_C_BYTE:
						case StringFunctions.HIGH_C_BYTE:
							return InfoCode.RemoteConnectionDescriptor;							
						case StringFunctions.LOW_M_BYTE:
						case StringFunctions.HIGH_M_BYTE:
							return InfoCode.RestartMethod;							
						case StringFunctions.LOW_D_BYTE:
						case StringFunctions.HIGH_D_BYTE:
							return InfoCode.RestartDelay;							
					}					
			}
		}
		
		throw new ParseException("Could not parse info code: " + new String(value,offset,length), 0);									
	}
	
	public static int encode(byte[] destination,int offset,InfoCode infoCode)
	{		
		switch (infoCode.getInfoCode()) 
		{
			case InfoCode.BEARER_INFORMATION:
				destination[offset]=StringFunctions.HIGH_B_BYTE;
				return 1;				
			case InfoCode.CALL_IDENTIFIER:
				destination[offset]=StringFunctions.HIGH_C_BYTE;
				return 1;				
			case InfoCode.CONNECTION_IDENTIFIER:
				destination[offset]=StringFunctions.HIGH_I_BYTE;
				return 1;				
			case InfoCode.NOTIFIED_ENTITY:
				destination[offset]=StringFunctions.HIGH_N_BYTE;
				return 1;				
			case InfoCode.REQUEST_IDENTIFIER:
				destination[offset]=StringFunctions.HIGH_X_BYTE;
				return 1;				
			case InfoCode.LOCAL_CONNECTION_OPTIONS:
				destination[offset]=StringFunctions.HIGH_L_BYTE;
				return 1;				
			case InfoCode.CONNECTION_MODE:
				destination[offset]=StringFunctions.HIGH_M_BYTE;
				return 1;								
			case InfoCode.REQUESTED_EVENTS:
				destination[offset]=StringFunctions.HIGH_R_BYTE;
				return 1;				
			case InfoCode.SIGNAL_REQUESTS:
				destination[offset]=StringFunctions.HIGH_S_BYTE;
				return 1;				
			case InfoCode.DIGIT_MAP:
				destination[offset]=StringFunctions.HIGH_D_BYTE;
				return 1;				
			case InfoCode.OBSERVED_EVENTS:
				destination[offset]=StringFunctions.HIGH_O_BYTE;
				return 1;				
			case InfoCode.CONNECTION_PARAMETERS:
				destination[offset]=StringFunctions.HIGH_P_BYTE;
				return 1;				
			case InfoCode.REASON_CODE:
				destination[offset]=StringFunctions.HIGH_E_BYTE;
				return 1;				
			case InfoCode.SPECIFIC_ENDPOINT_ID:
				destination[offset]=StringFunctions.HIGH_Z_BYTE;
				return 1;				
			case InfoCode.QUARANTINE_HANDLING:
				destination[offset]=StringFunctions.HIGH_Q_BYTE;
				return 1;				
			case InfoCode.DETECT_EVENTS:
				destination[offset]=StringFunctions.HIGH_T_BYTE;
				return 1;				
			case InfoCode.CAPABILITIES:
				destination[offset]=StringFunctions.HIGH_A_BYTE;
				return 1;												
			case InfoCode.REMOTE_CONNECTION_DESCRIPTOR:
				destination[offset]=StringFunctions.HIGH_R_BYTE;
				destination[offset+1]=StringFunctions.HIGH_C_BYTE;
				return 2;				
			case InfoCode.LOCAL_CONNECTION_DESCRIPTOR:
				destination[offset]=StringFunctions.HIGH_L_BYTE;
				destination[offset+1]=StringFunctions.HIGH_C_BYTE;
				return 2;				
			case InfoCode.EVENT_STATES:
				destination[offset]=StringFunctions.HIGH_E_BYTE;
				destination[offset+1]=StringFunctions.HIGH_S_BYTE;
				return 2;			
			case InfoCode.RESTART_METHOD:
				destination[offset]=StringFunctions.HIGH_R_BYTE;
				destination[offset+1]=StringFunctions.HIGH_M_BYTE;
				return 2;				
			case InfoCode.RESTART_DELAY:
				destination[offset]=StringFunctions.HIGH_R_BYTE;
				destination[offset+1]=StringFunctions.HIGH_D_BYTE;
				return 2;				
			default:
				return 0;				
		}
	}
		
	public static int encodeList(byte[] destination,int offset,InfoCode[] infoCodes)
	{
		if(infoCodes.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<infoCodes.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,infoCodes[i]);
			destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
			totalLength++;
		}
			
		totalLength+=encode(destination,offset+totalLength,infoCodes[i]);
		return totalLength;
	}
	
	public static InfoCode[] decodeList(byte[] value,int offset,int length) throws ParseException
	{
		SplitDetails[] splitDetails=StringFunctions.split(value,offset,length,StringFunctions.COMMA_BYTE);
		InfoCode[] infoCodes = new InfoCode[splitDetails.length];

		for (int i = 0; i < splitDetails.length; i++)
			infoCodes[i] = decode(value,splitDetails[i].getOffset(),splitDetails[i].getLength());		

		return infoCodes;					
	}
}