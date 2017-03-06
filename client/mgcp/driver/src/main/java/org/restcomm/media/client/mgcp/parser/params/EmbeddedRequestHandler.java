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

import jain.protocol.ip.mgcp.message.parms.DigitMap;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.EmbeddedRequest;

public class EmbeddedRequestHandler
{
	private static Object decodeSingle(byte[] value,int offset,int length) throws ParseException
	{
		if(length<=3)
			throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
		
		int startIndex=offset+1;
		while(startIndex<offset+length && value[startIndex]==StringFunctions.SPACE_BYTE)
			startIndex++;
		
		if(startIndex==offset+length)
			throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
		
		if(value[startIndex]!=StringFunctions.OPEN_BRACKET_BYTE)
			throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
		
		if(value[offset+length-1]!=StringFunctions.CLOSE_BRACKET_BYTE)
			throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
		
		int contentStart=startIndex+1;
		int contentEnd=offset+length-2;
		
		while(contentStart<contentEnd && value[contentStart]==StringFunctions.SPACE_BYTE)
			contentStart++;
		
		while(contentEnd>contentStart && value[contentEnd]==StringFunctions.SPACE_BYTE)
			contentEnd--;
		
		if(contentStart==contentEnd)
			throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
		
		byte currByte=value[offset];
		switch(currByte)
		{
			case StringFunctions.LOW_R_BYTE:
			case StringFunctions.HIGH_R_BYTE:
				return RequestedEventHandler.decodeList(value,contentStart,contentEnd-contentStart+1);					
			case StringFunctions.LOW_D_BYTE:
			case StringFunctions.HIGH_D_BYTE:
				return DigitMapHandler.decode(value,contentStart,contentEnd-contentStart+1);				
			case StringFunctions.LOW_S_BYTE:
			case StringFunctions.HIGH_S_BYTE:
				return EventNameHandler.decodeList(value,contentStart,contentEnd-contentStart+1);				
			default:
				throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
		}
	}
	
	public static EmbeddedRequest decode(byte[] value,int offset,int length) throws ParseException
	{		
		Object currObject;
		RequestedEvent[] requestedEvents = null;
		EventName[] signalEvents = null;
		DigitMap digitMap = null;
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
						currObject=decodeSingle(value,startIndex,currIndex-startIndex);
						if(currObject instanceof DigitMap)
						{
							if(digitMap!=null)
								throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);														
							
							digitMap=(DigitMap)currObject;
						}
						else if(currObject instanceof RequestedEvent[])
						{
							if(requestedEvents!=null)
								throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
									
							requestedEvents=(RequestedEvent[])currObject;
						}
						else if(currObject instanceof EventName[])
						{
							if(signalEvents!=null)
								throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
							
							signalEvents=(EventName[])currObject;
						}
						
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
							currObject=decodeSingle(value,startIndex,currIndex-startIndex+1);
							if(currObject instanceof DigitMap)
							{
								if(digitMap!=null)
									throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);														
								
								digitMap=(DigitMap)currObject;
							}
							else if(currObject instanceof RequestedEvent[])
							{
								if(requestedEvents!=null)
									throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
										
								requestedEvents=(RequestedEvent[])currObject;
							}
							else if(currObject instanceof EventName[])
							{
								if(signalEvents!=null)
									throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
								
								signalEvents=(EventName[])currObject;
							}
							
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
		{	
			currObject=decodeSingle(value,startIndex,offset+length-startIndex);
			if(currObject instanceof DigitMap)
			{
				if(digitMap!=null)
					throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);														
				
				digitMap=(DigitMap)currObject;
			}
			else if(currObject instanceof RequestedEvent[])
			{
				if(requestedEvents!=null)
					throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
						
				requestedEvents=(RequestedEvent[])currObject;
			}
			else if(currObject instanceof EventName[])
			{
				if(signalEvents!=null)
					throw new ParseException("Unknown value for Embedded Request: " + new String(value,offset,length), 0);
				
				signalEvents=(EventName[])currObject;
			}
		}
		
		return new EmbeddedRequest(requestedEvents, signalEvents, digitMap);													
	}
	
	public static int encode(byte[] destination,int offset,EmbeddedRequest embeddedRequest)
	{
		RequestedEvent[] requestedEventList = embeddedRequest.getEmbeddedRequestList();
		EventName[] eventNameList = embeddedRequest.getEmbeddedSignalRequest();
		DigitMap digitMap = embeddedRequest.getEmbeddedDigitMap();
		
		Boolean initial=true;
		
		int totalLength=0;
		if(requestedEventList!=null)
		{
			destination[offset+totalLength]=StringFunctions.HIGH_R_BYTE;
			destination[offset+totalLength+1]=StringFunctions.OPEN_BRACKET_BYTE;
			totalLength+=2;			
			
			totalLength+=RequestedEventHandler.encodeList(destination,offset+totalLength,requestedEventList);
			
			destination[offset+totalLength]=StringFunctions.CLOSE_BRACKET_BYTE;
			totalLength++;
			initial=false;
		}
		
		if(eventNameList!=null)
		{
			if(!initial)
			{
				destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
				totalLength++;
			}
			
			destination[offset+totalLength]=StringFunctions.HIGH_S_BYTE;
			destination[offset+totalLength+1]=StringFunctions.OPEN_BRACKET_BYTE;
			totalLength+=2;			
			
			totalLength+=EventNameHandler.encodeList(destination,offset+totalLength,eventNameList);
			
			destination[offset+totalLength]=StringFunctions.CLOSE_BRACKET_BYTE;
			totalLength++;
			initial=false;
		}
		
		if(digitMap!=null)
		{
			if(!initial)
			{
				destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
				totalLength++;
			}
			
			destination[offset+totalLength]=StringFunctions.HIGH_D_BYTE;
			destination[offset+totalLength+1]=StringFunctions.OPEN_BRACKET_BYTE;
			totalLength+=2;
			
			totalLength+=DigitMapHandler.encode(destination,offset+totalLength,digitMap);
			
			destination[offset+totalLength]=StringFunctions.CLOSE_BRACKET_BYTE;
			totalLength++;
		}
		
		return totalLength;
	}
}