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
import java.util.ArrayList;

import org.mobicents.protocols.mgcp.parser.StringFunctions;
import org.mobicents.protocols.mgcp.parser.SplitDetails;

import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.LocalOptionExtension;
import jain.protocol.ip.mgcp.message.parms.Bandwidth;
import jain.protocol.ip.mgcp.message.parms.CompressionAlgorithm;
import jain.protocol.ip.mgcp.message.parms.PacketizationPeriod;
import jain.protocol.ip.mgcp.message.parms.GainControl;
import jain.protocol.ip.mgcp.message.parms.EchoCancellation;
import jain.protocol.ip.mgcp.message.parms.EncryptionMethod;
import jain.protocol.ip.mgcp.message.parms.ResourceReservation;
import jain.protocol.ip.mgcp.message.parms.SilenceSuppression;
import jain.protocol.ip.mgcp.message.parms.TypeOfService;
import jain.protocol.ip.mgcp.message.parms.TypeOfNetwork;

public class RequestedActionHandler
{
	public static RequestedAction decode(byte[] value,int offset,int length) throws ParseException
	{
		if(length==1)
		{
			switch(value[offset])
			{
				case StringFunctions.LOW_N_BYTE:
				case StringFunctions.HIGH_N_BYTE:
					return RequestedAction.NotifyImmediately;					
				case StringFunctions.LOW_A_BYTE:
				case StringFunctions.HIGH_A_BYTE:
					return RequestedAction.Accumulate;
				case StringFunctions.LOW_S_BYTE:
				case StringFunctions.HIGH_S_BYTE:
					return RequestedAction.Swap;
				case StringFunctions.LOW_I_BYTE:
				case StringFunctions.HIGH_I_BYTE:
					return RequestedAction.Ignore;
				case StringFunctions.LOW_K_BYTE:
				case StringFunctions.HIGH_K_BYTE:
					return RequestedAction.KeepSignalsActive;
				case StringFunctions.LOW_D_BYTE:
				case StringFunctions.HIGH_D_BYTE:
					return RequestedAction.TreatAccordingToDigitMap;
				case StringFunctions.LOW_X_BYTE:
				case StringFunctions.HIGH_X_BYTE:
					return RequestedAction.Ignore;			
			}
		}
		else if(value[offset]==StringFunctions.LOW_E_BYTE || value[offset]==StringFunctions.HIGH_E_BYTE)
		{
			if(value[offset+1]!=StringFunctions.OPEN_BRACKET_BYTE)
				throw new ParseException("Invalid requested action " + new String(value,offset,length), 0);
			
			if(value[offset+length-1]!=StringFunctions.CLOSE_BRACKET_BYTE)
				throw new ParseException("Invalid requested action " + new String(value,offset,length), 0);
			
			int startIndex=offset+2;
			while(value[startIndex]==StringFunctions.SPACE_BYTE)
				startIndex++;
			
			int endIndex=offset+length-2;
			while(value[endIndex]==StringFunctions.SPACE_BYTE)
				endIndex--;
			
			return new RequestedAction(EmbeddedRequestHandler.decode(value,startIndex,endIndex-startIndex+1));
		}		
		
		throw new ParseException("Invalid requested action " + new String(value,offset,length), 0);
	}
	
	public static int encode(byte[] destination,int offset,RequestedAction requestedAction)
	{		
		switch(requestedAction.getRequestedAction())
		{	
			case RequestedAction.NOTIFY_IMMEDIATELY:
				destination[offset]=StringFunctions.HIGH_N_BYTE;
				return 1;
			case RequestedAction.ACCUMULATE:
				destination[offset]=StringFunctions.HIGH_A_BYTE;
				return 1;
			case RequestedAction.TREAT_ACCORDING_TO_DIGIT_MAP:
				destination[offset]=StringFunctions.HIGH_D_BYTE;
				return 1;
			case RequestedAction.SWAP:
				destination[offset]=StringFunctions.HIGH_S_BYTE;
				return 1;
			case RequestedAction.IGNORE:
				destination[offset]=StringFunctions.HIGH_I_BYTE;
				return 1;
			case RequestedAction.KEEP_SIGNALS_ACTIVE:
				destination[offset]=StringFunctions.HIGH_K_BYTE;
				return 1;
			case RequestedAction.EMBEDDED_NOTIFICATION_REQUEST:
				destination[offset]=StringFunctions.HIGH_E_BYTE;				
				destination[offset+1]=StringFunctions.OPEN_BRACKET_BYTE;
				int returnLength=EmbeddedRequestHandler.encode(destination,offset+2,requestedAction.getEmbeddedRequest());
				destination[offset+2+returnLength]=StringFunctions.CLOSE_BRACKET_BYTE;
				return returnLength+3;				
		}
		
		return 0; 
	}
		
	public static int encodeList(byte[] destination,int offset,RequestedAction[] actions)
	{
		if(actions.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<actions.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,actions[i]);
			destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
			totalLength++;
		}
			
		totalLength+=encode(destination,offset+totalLength,actions[i]);
		return totalLength;		
	}
	
	public static RequestedAction[] decodeList(byte[] value,int offset,int length) throws ParseException
	{
		ArrayList<RequestedAction> result=new ArrayList<RequestedAction>();
		int currIndex=offset;
		int startIndex=offset;
		int i=0;
		
		while(value[startIndex]==StringFunctions.SPACE_BYTE && i<length)
		{
			startIndex++;
			i++;
			currIndex++;
		}	
		
		int depth=0;
		for(;i<length;i++,currIndex++)
		{
			switch(value[currIndex])
			{
				case StringFunctions.OPEN_BRACKET_BYTE:
					depth++;
					break;
				case StringFunctions.COMMA_BYTE:
					if(depth==0 && i<length)
					{
						result.add(decode(value,startIndex,currIndex-startIndex));
						startIndex=currIndex+1;
						
						while(i<length-1 && value[startIndex]==StringFunctions.SPACE_BYTE)
						{
							startIndex++;
							i++;
							currIndex++;
						}
					}
					break;
				case StringFunctions.CLOSE_BRACKET_BYTE:
					depth--;
					if(depth==0 && i<length)
					{
						while(i<length && value[currIndex]==StringFunctions.SPACE_BYTE)
						{
							i++;
							currIndex++;
						}
						
						if(i<length-1 && value[currIndex+1]==StringFunctions.COMMA_BYTE)
						{
							result.add(decode(value,startIndex,currIndex-startIndex+1));
							startIndex=currIndex+2;
							i++;
							currIndex++;
							
							while(i<length-1 && value[startIndex]==StringFunctions.SPACE_BYTE)
							{
								startIndex++;
								i++;
								currIndex++;
							}
						}
					}
					break;
			}
		}
		
		if(startIndex<offset+length)
			result.add(decode(value,startIndex,offset+length-startIndex));
		
		RequestedAction[] actions = new RequestedAction[result.size()];
		actions=result.toArray(actions);
		result.clear();		
		return actions;			
	}
}