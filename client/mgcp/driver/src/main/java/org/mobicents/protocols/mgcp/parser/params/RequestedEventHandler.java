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

import jain.protocol.ip.mgcp.message.parms.RequestedEvent;

public class RequestedEventHandler
{
	public static RequestedEvent decode(byte[] value,int offset,int length) throws ParseException
	{		
		int startIndex=offset;		
		while(value[startIndex]==StringFunctions.SPACE_BYTE)
			startIndex++;
		
		int endIndex=offset+length-1;
		while(value[endIndex]==StringFunctions.SPACE_BYTE)
			endIndex--;
		
		if(value[startIndex]==StringFunctions.OPEN_BRACKET_BYTE)
			throw new ParseException("Unknown value for Requested Event: " + new String(value,offset,length), 0);
		
		if(value[endIndex]==StringFunctions.CLOSE_BRACKET_BYTE)
		{
			endIndex--;
			while(value[endIndex]==StringFunctions.SPACE_BYTE)
				endIndex--;
						
			int i=startIndex;
			int lastEventNameIndex=startIndex;
			for(;i<=endIndex;i++)
				if(value[i]==StringFunctions.OPEN_BRACKET_BYTE)
					break;
				else if(value[i]!=StringFunctions.SPACE_BYTE)
					lastEventNameIndex=i;
								
			if(i==endIndex+1)
				throw new ParseException("Unknown value for Requested Event: " + new String(value,offset,length), 0);
					
			i++;
			while(i<=endIndex && value[i]==StringFunctions.SPACE_BYTE)
				i++;
			
			int firstBracketIndex=i;
			int middleBracketCloser=i;
			int level=1;
			for(;i<=endIndex;i++)
			{
				if(value[i]==StringFunctions.OPEN_BRACKET_BYTE)
				{
					level++;
					middleBracketCloser=i;
				}
				else if(value[i]==StringFunctions.CLOSE_BRACKET_BYTE)
				{
					level--;
					if(level==0)
						break;
					
					middleBracketCloser=i;
				}
				else if(value[i]!=StringFunctions.SPACE_BYTE)
					middleBracketCloser=i;
			}
			
			switch(level)
			{
				case 0:
					i++;
					for(;i<endIndex;i++)
						if(value[i]==StringFunctions.OPEN_BRACKET_BYTE)
						{
							i++;
							while(i<endIndex && value[i]==StringFunctions.SPACE_BYTE)
								i++;
							
							return new RequestedEvent(EventNameHandler.decodeWithParams(value, startIndex,lastEventNameIndex-startIndex+1,i,endIndex-i+1), RequestedActionHandler.decodeList(value,firstBracketIndex,middleBracketCloser-firstBracketIndex+1));
						}
						else if(value[i]!=StringFunctions.SPACE_BYTE)
							throw new ParseException("Unknown value for Requested Event: " + new String(value,offset,length), 0);
					
					throw new ParseException("Unknown value for Requested Event: " + new String(value,offset,length), 0);					
				case 1:
					return new RequestedEvent(EventNameHandler.decode(value, startIndex,lastEventNameIndex-startIndex+1), RequestedActionHandler.decodeList(value,firstBracketIndex,endIndex-firstBracketIndex+1));					
				default:
					throw new ParseException("Unknown value for Requested Event: " + new String(value,offset,length), 0);
			}
		}
		else
			return new RequestedEvent(EventNameHandler.decode(value, startIndex,endIndex-startIndex+1), null);		
	}
	
	public static int encode(byte[] destination,int offset,RequestedEvent requestedEvent)
	{
		int totalLength=EventNameHandler.encode(destination,offset,requestedEvent.getEventName());
		if(requestedEvent.getRequestedActions()!=null)
		{
			destination[offset+totalLength]=StringFunctions.OPEN_BRACKET_BYTE;
			totalLength++;
			totalLength+=RequestedActionHandler.encodeList(destination,offset+totalLength,requestedEvent.getRequestedActions());
			destination[offset+totalLength]=StringFunctions.CLOSE_BRACKET_BYTE;
			totalLength++;
		}
			
		
		totalLength+=EventNameHandler.encodeParams(destination,offset+totalLength,requestedEvent.getEventName());
		return totalLength;		
	}
	
	public static int encodeList(byte[] destination,int offset,RequestedEvent[] requestedEvents)
	{
		if(requestedEvents.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<requestedEvents.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,requestedEvents[i]);
			destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
			totalLength++;
		}
			
		totalLength+=encode(destination,offset+totalLength,requestedEvents[i]);
		return totalLength;		
	}
	
	public static RequestedEvent[] decodeList(byte[] value,int offset,int length) throws ParseException
	{		
		ArrayList<RequestedEvent> result=new ArrayList<RequestedEvent>();
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
						
						if(value[currIndex+1]==StringFunctions.COMMA_BYTE)
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
		
		RequestedEvent[] requestedEvents = new RequestedEvent[result.size()];
		requestedEvents=result.toArray(requestedEvents);
		result.clear();		
		return requestedEvents;		
	}
}