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
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;

public class NotifiedEntityHandler
{
	public static final String emptyString="";
	
	public static NotifiedEntity decode(byte[] value,int offset,int length,boolean toGateway) throws ParseException
	{
		int currIndex=offset;
		int atIndex=-1;
		int colonIndex=-1;
		for(int i=0;i<length;i++,currIndex++)
		{
			if(value[currIndex]==StringFunctions.AT_BYTE)
			{
				if(atIndex>=0 || colonIndex>=0)
					throw new ParseException("Unknown value for Notified Entity: " + new String(value,offset,length), 0);
				
				atIndex=currIndex;
			}
			else if(value[currIndex]==StringFunctions.COLON_BYTE)
			{
				if(colonIndex>=0)
					throw new ParseException("Unknown value for Notified Entity: " + new String(value,offset,length), 0);
				
				colonIndex=currIndex;
				break;
			}
		}
		
		String domainName,localName;
		if(atIndex>=0)
		{
			localName=new String(value,offset,atIndex-offset);
			if(colonIndex>=0)
				domainName=new String(value,atIndex+1,colonIndex-atIndex-1);
			else
				domainName=new String(value,atIndex+1,length+offset-atIndex-1);
		}
		else
		{
			localName=emptyString;
			if(colonIndex>=0)
				domainName=new String(value,offset,colonIndex-offset);
			else
				domainName=new String(value,offset,length);
		}
		
		int port=2727;
		if(colonIndex>=0)
		{
			port=0;
			int numberLength=length+offset-colonIndex-1;
			currIndex=colonIndex+1;
			for(int i=0;i<numberLength;i++,currIndex++)
				if(value[currIndex]>=StringFunctions.ZERO_BYTE && value[currIndex]<=StringFunctions.NINE_BYTE)
					port=port*10+(value[currIndex]-StringFunctions.ZERO_BYTE);
				else
					throw new ParseException("Unknown value for Notified Entity: " + new String(value,offset,length), 0);						
		}
		else if(toGateway)
			port=2427;
		
		return new NotifiedEntity(localName, domainName, port);
	}
	
	public static int encode(byte[] destination,int offset,NotifiedEntity notifiedEntity)
	{
		int totalLength=0;
		byte[] currNode;
		if (notifiedEntity.getLocalName() != null && notifiedEntity.getLocalName().length()>0) 
		{
			currNode=notifiedEntity.getLocalName().getBytes();
			System.arraycopy(currNode, 0, destination, offset, currNode.length);
			totalLength+=currNode.length;
			
			destination[offset+totalLength]=StringFunctions.AT_BYTE;
			totalLength++;
		}

		if (notifiedEntity.getDomainName() != null && notifiedEntity.getDomainName().length()>0) 
		{
			currNode=notifiedEntity.getDomainName().getBytes();
			System.arraycopy(currNode, 0, destination, offset+totalLength, currNode.length);
			totalLength+=currNode.length;
		}

		if (notifiedEntity.getPortNumber() > 0) 
		{
			destination[offset+totalLength]=StringFunctions.COLON_BYTE;
			totalLength++;
			totalLength+=StringFunctions.encodeInt(destination,offset+totalLength,notifiedEntity.getPortNumber());			
		}
		
		return totalLength;
	}
}