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
import org.mobicents.protocols.mgcp.parser.SplitDetails;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

public class EndpointIdentifierHandler
{
	public static EndpointIdentifier decode(byte[] value,int offset,int length) throws ParseException
	{		
		for(int i=0;i<length;i++)
			if(value[offset+i]==StringFunctions.AT_BYTE)
				return new EndpointIdentifier(new String(value,offset,i), new String(value,offset+i+1,length-i-1));					
		
		return new EndpointIdentifier(new String(value,offset,length),null);
	}
	
	public static int encode(byte[] destination,int offset,EndpointIdentifier endpointIdentifier)
	{
		int usedLength=0;
		byte[] name=endpointIdentifier.getLocalEndpointName().getBytes();
		System.arraycopy(name, 0, destination, offset, name.length);
		usedLength=name.length;
		if(endpointIdentifier.getDomainName() != null)
		{
			destination[offset+usedLength]=StringFunctions.AT_BYTE;
			usedLength++;
			name=endpointIdentifier.getDomainName().getBytes();
			System.arraycopy(name, 0, destination, offset+usedLength, name.length);
			usedLength+=name.length;
		}
				
		return usedLength;
	}
	
	public static int encodeList(byte[] destination,int offset,EndpointIdentifier[] endpointIds)
	{
		if(endpointIds.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<endpointIds.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,endpointIds[i]);
			destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
			totalLength++;
		}
			
		totalLength+=encode(destination,offset+totalLength,endpointIds[i]);
		return totalLength;		
	}
	
	public static EndpointIdentifier[] decodeList(byte[] value,int offset,int length) throws ParseException
	{
		SplitDetails[] splitDetails=StringFunctions.split(value,offset,length,StringFunctions.COMMA_BYTE);
		EndpointIdentifier[] endpointIds = new EndpointIdentifier[splitDetails.length];

		for (int i = 0; i < splitDetails.length; i++)
			endpointIds[i] = decode(value,splitDetails[i].getOffset(),splitDetails[i].getLength());		

		return endpointIds;		
	}
}