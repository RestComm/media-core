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

import org.restcomm.media.client.mgcp.jain.pkg.AUMgcpEvent;
import org.restcomm.media.client.mgcp.jain.pkg.AUPackage;
import org.restcomm.media.client.mgcp.jain.pkg.SLPackage;
import org.restcomm.media.client.mgcp.parser.SplitDetails;
import org.restcomm.media.client.mgcp.parser.StringFunctions;
import org.restcomm.media.client.mgcp.parser.pkg.PackageNameHandler;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.pkg.PackageName;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;

public class EventNameHandler
{
	static PackageName auPackage=AUPackage.AU;
	static PackageName slPackage=SLPackage.SL;
	static MgcpEvent rfc2897pa = AUMgcpEvent.aupa;
	
	private static MgcpEvent generateEvent(byte[] value,int offset,int length) throws ParseException
	{
		if(length==2)		
		{
			switch(value[offset+1])
			{
				case StringFunctions.LOW_S_BYTE: 
				case StringFunctions.HIGH_S_BYTE:
					if(value[offset]==StringFunctions.LOW_E_BYTE || value[offset]==StringFunctions.HIGH_E_BYTE)
						return AUMgcpEvent.aues;
					
				case StringFunctions.LOW_R_BYTE: 
				case StringFunctions.HIGH_R_BYTE:
					if(value[offset]==StringFunctions.LOW_P_BYTE || value[offset]==StringFunctions.HIGH_P_BYTE)
						return AUMgcpEvent.aupr;
					
				case StringFunctions.LOW_C_BYTE: 
				case StringFunctions.HIGH_C_BYTE:
					if(value[offset]==StringFunctions.LOW_P_BYTE || value[offset]==StringFunctions.HIGH_P_BYTE)
						return AUMgcpEvent.aupc;
			}
		}
		
		return MgcpEvent.factory(new String(value,offset,length));
	}
	
	private static EventName decode(byte[] value,int offset,int length,String params) throws ParseException
	{
		int currIndex=offset;
		int slashIndex=-1;
		int atIndex=-1;		
		for(int i=0;i<length;i++,currIndex++)
		{
			if(value[currIndex]==StringFunctions.SLASH_BYTE)
			{
				if(slashIndex>=0)
					throw new ParseException("Unknown value for Event Name: " + new String(value,offset,length), 0);
				
				slashIndex=currIndex;
			}
			else if(value[currIndex]==StringFunctions.AT_BYTE)
			{
				if(slashIndex<0)
					throw new ParseException("Unknown value for Event Name: " + new String(value,offset,length), 0);
				
				atIndex=currIndex;
				break;
			}
		}
		
		PackageName packageName;
		MgcpEvent mgcpEvent;
		if(slashIndex>=0)
		{			
			packageName=PackageNameHandler.decode(value,offset,slashIndex-offset);
			if(atIndex>=0)				
				mgcpEvent=generateEvent(value,slashIndex+1,atIndex-slashIndex-1);
			else
				mgcpEvent=generateEvent(value,slashIndex+1,length+offset-slashIndex-1);									
		}
		else
		{
			packageName=PackageName.AllPackages;
			if(atIndex>=0)
				mgcpEvent=generateEvent(value,offset,atIndex-offset);
			else
				mgcpEvent=generateEvent(value,offset,length);			
		}
		
		ConnectionIdentifier connectionIdentifier=null;
		if(atIndex>=0)
		{
			if(length+offset-atIndex-1==1)
			{
				switch(value[atIndex+1])
				{
					case StringFunctions.DOLLAR_BYTE:
						connectionIdentifier=ConnectionIdentifier.AnyConnection;
						break;
					case StringFunctions.ASTERISK_BYTE:
						connectionIdentifier=ConnectionIdentifier.AllConnections;
						break;
					default:
						connectionIdentifier=new ConnectionIdentifier(new String(value,atIndex+1,1));
						break;
				}
			}
			else
				connectionIdentifier=new ConnectionIdentifier(new String(value,atIndex+1,length+offset-atIndex-1));
		}
		
		if(params==null)
			return new EventName(packageName,mgcpEvent,connectionIdentifier);	
		
		return new EventName(packageName,mgcpEvent.withParm(params),connectionIdentifier);
	}
	
	public static EventName decode(byte[] value,int offset,int length) throws ParseException
	{
		return decode(value,offset,length,null);
	}
	
	public static EventName decodeWithParams(byte[] value,int offset,int length,int paramsOffset,int paramsLength) throws ParseException
	{
		return decode(value,offset,length,new String(value,paramsOffset,paramsLength));		
	}
	
	public static int encode(byte[] destination,int offset,EventName eventName)
	{
		int totalLength=PackageNameHandler.encode(destination,offset,eventName.getPackageName());
		destination[offset+totalLength]=StringFunctions.SLASH_BYTE;
		totalLength++;
		
		byte[] nameBytes=eventName.getEventIdentifier().getName().getBytes();
		System.arraycopy(nameBytes, 0, destination, offset+totalLength, nameBytes.length);
		totalLength+=nameBytes.length;
		
		if(eventName.getConnectionIdentifier()!=null)
		{
			destination[offset+totalLength]=StringFunctions.AT_BYTE;
			totalLength++;
			byte[] connectionBytes=eventName.getConnectionIdentifier().toString().getBytes();
			System.arraycopy(connectionBytes, 0, destination, offset+totalLength, connectionBytes.length);
			totalLength+=connectionBytes.length;			
		}
		
		return totalLength;
	}
	
	public static int encodeParams(byte[] destination,int offset,EventName eventName)
	{
		if(eventName.getEventIdentifier().getParms()==null)
			return 0;
		
		destination[offset]=StringFunctions.OPEN_BRACKET_BYTE;
		byte[] paramsBytes=eventName.getEventIdentifier().getParms().getBytes();
		System.arraycopy(paramsBytes, 0, destination, offset+1, paramsBytes.length);
		destination[offset+paramsBytes.length+1]=StringFunctions.CLOSE_BRACKET_BYTE;
		return paramsBytes.length+2;
	}
	
	public static int encodeList(byte[] destination,int offset,EventName[] eventNames)
	{
		if(eventNames.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<eventNames.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,eventNames[i]);
			totalLength+=encodeParams(destination,offset+totalLength,eventNames[i]);			
			destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
			totalLength++;					
		}
			
		totalLength+=encode(destination,offset+totalLength,eventNames[i]);
		totalLength+=encodeParams(destination,offset+totalLength,eventNames[i]);			
		return totalLength;		
	}
	
	public static EventName[] decodeList(byte[] value,int offset,int length) throws ParseException
	{		
		SplitDetails[] splitDetails=StringFunctions.split(value,offset,length,StringFunctions.COMMA_BYTE);
		EventName[] events = new EventName[splitDetails.length];
		Boolean found=false;
		
		for (int i = 0; i < splitDetails.length; i++)
		{
			if(value[splitDetails[i].getOffset()+splitDetails[i].getLength()-1]==StringFunctions.CLOSE_BRACKET_BYTE)
			{	
				found=false;
				for(int j=0;j<splitDetails[i].getLength();j++)
					if(value[splitDetails[i].getOffset()+j]==StringFunctions.OPEN_BRACKET_BYTE)
					{				
						events[i] = decodeWithParams(value,splitDetails[i].getOffset(),j,splitDetails[i].getOffset()+j+1,splitDetails[i].getLength()-j-2);
						found=true;
						break;
					}
				
				if(!found)
					events[i] = decode(value,splitDetails[i].getOffset(),splitDetails[i].getLength());
			}
			else
				events[i] = decode(value,splitDetails[i].getOffset(),splitDetails[i].getLength());			
		}
		
		return events;		
	}
}