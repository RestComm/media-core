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

import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.ExtendedConnectionParm;
import jain.protocol.ip.mgcp.message.parms.RegularConnectionParm;

public class ConnectionParmHandler
{
	public static final int PACKETS_SENT=1;
	public static final int OCTETS_SENT=2;
	public static final int PACKETS_RECEIVED=3;
	public static final int OCTETS_RECEIVED=4;
	public static final int PACKETS_LOST=5;
	public static final int JITTER=6;
	public static final int LATENCY=7;	
	public static final int EXTENSION=8;
	
	public static ConnectionParm decode(byte[] value,int offset,int length) throws ParseException
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
				
		int spaceStart=startIndex;
		int numberStart=startIndex;
		for(int i=0;i<finalLength;i++)
		{			
			if(value[startIndex+i]==StringFunctions.EQUAL_BYTE)
			{
				numberStart=startIndex+i+1;
				finalLength=finalLength-i-1;
				break;
			}
			else if(value[startIndex+i]!=StringFunctions.SPACE_BYTE)
				spaceStart=startIndex+i+1;
		}
		
		if(numberStart==startIndex)
			throw new ParseException("Could not parse connection param: " + new String(value,offset,length), 0);
		
		if(spaceStart-startIndex!=2)
			throw new ParseException("Could not parse connection param: " + new String(value,offset,length), 0);
		
		int numericValue=0;
		for(int i=0;i<finalLength;i++,numberStart++)
		{			
			if(value[numberStart]>=StringFunctions.ZERO_BYTE && value[numberStart]<=StringFunctions.NINE_BYTE)
				numericValue=numericValue*10+(value[numberStart]-StringFunctions.ZERO_BYTE);
			else if(numericValue>0 || value[numberStart]!=StringFunctions.SPACE_BYTE)
				throw new ParseException("Could not parse connection param: " + new String(value,offset,length), 0);
		}
		
		switch(value[startIndex])
		{
			case StringFunctions.LOW_L_BYTE:
			case StringFunctions.HIGH_L_BYTE:
				if(value[startIndex+1]==StringFunctions.LOW_A_BYTE || value[startIndex+1]==StringFunctions.HIGH_A_BYTE)
					return new RegularConnectionParm(RegularConnectionParm.LATENCY, numericValue);					
			case StringFunctions.LOW_J_BYTE:
			case StringFunctions.HIGH_J_BYTE:
				if(value[startIndex+1]==StringFunctions.LOW_I_BYTE || value[startIndex+1]==StringFunctions.HIGH_I_BYTE)
					return new RegularConnectionParm(RegularConnectionParm.JITTER, numericValue);
			case StringFunctions.LOW_O_BYTE:
			case StringFunctions.HIGH_O_BYTE:
				switch(value[startIndex+1])
				{
					case StringFunctions.LOW_R_BYTE:
					case StringFunctions.HIGH_R_BYTE:
						return new RegularConnectionParm(RegularConnectionParm.OCTETS_RECEIVED, numericValue);						
					case StringFunctions.LOW_S_BYTE:
					case StringFunctions.HIGH_S_BYTE:
						return new RegularConnectionParm(RegularConnectionParm.OCTETS_SENT, numericValue);										
				}
			case StringFunctions.LOW_P_BYTE:
			case StringFunctions.HIGH_P_BYTE:
				switch(value[startIndex+1])
				{
					case StringFunctions.LOW_L_BYTE:
					case StringFunctions.HIGH_L_BYTE:
						return new RegularConnectionParm(RegularConnectionParm.PACKETS_LOST, numericValue);												
					case StringFunctions.LOW_R_BYTE:
					case StringFunctions.HIGH_R_BYTE:
						return new RegularConnectionParm(RegularConnectionParm.PACKETS_RECEIVED, numericValue);						
					case StringFunctions.LOW_S_BYTE:
					case StringFunctions.HIGH_S_BYTE:
						return new RegularConnectionParm(RegularConnectionParm.PACKETS_SENT, numericValue);						
				}					
		}
		
		return new ExtendedConnectionParm(new String(value,startIndex,2), numericValue);																
	}
	
	public static int encode(byte[] destination,int offset,ConnectionParm connectionParm)
	{		
		switch (connectionParm.getConnectionParmType()) 
		{
			case JITTER:
				destination[offset]=StringFunctions.HIGH_J_BYTE;
				destination[offset+1]=StringFunctions.HIGH_I_BYTE;
				break;				
			case LATENCY:
				destination[offset]=StringFunctions.HIGH_L_BYTE;
				destination[offset+1]=StringFunctions.HIGH_A_BYTE;
				break;				
			case OCTETS_RECEIVED:
				destination[offset]=StringFunctions.HIGH_O_BYTE;
				destination[offset+1]=StringFunctions.HIGH_R_BYTE;
				break;				
			case OCTETS_SENT:
				destination[offset]=StringFunctions.HIGH_O_BYTE;
				destination[offset+1]=StringFunctions.HIGH_S_BYTE;
				break;				
			case PACKETS_LOST:
				destination[offset]=StringFunctions.HIGH_P_BYTE;
				destination[offset+1]=StringFunctions.HIGH_L_BYTE;
				break;				
			case PACKETS_RECEIVED:
				destination[offset]=StringFunctions.HIGH_P_BYTE;
				destination[offset+1]=StringFunctions.HIGH_R_BYTE;
				break;				
			case PACKETS_SENT:
				destination[offset]=StringFunctions.HIGH_P_BYTE;
				destination[offset+1]=StringFunctions.HIGH_S_BYTE;
				break;
			case EXTENSION:
				byte[] nameBytes=((ExtendedConnectionParm) connectionParm).getConnectionParmExtensionName().getBytes();
				System.arraycopy(nameBytes, 0, destination, offset, 2);
				break;
			default:
				return 0;
		}
		
		destination[offset+2]=StringFunctions.EQUAL_BYTE;
		return StringFunctions.encodeInt(destination,offset+3,connectionParm.getConnectionParmValue())+3;
	}
		
	public static int encodeList(byte[] destination,int offset,ConnectionParm[] connectionParms)
	{
		if(connectionParms.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<connectionParms.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,connectionParms[i]);
			destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
			totalLength++;
		}
			
		totalLength+=encode(destination,offset+totalLength,connectionParms[i]);
		return totalLength;
	}
	
	public static ConnectionParm[] decodeList(byte[] value,int offset,int length) throws ParseException
	{
		SplitDetails[] splitDetails=StringFunctions.split(value,offset,length,StringFunctions.COMMA_BYTE);
		ConnectionParm[] connectionParms = new ConnectionParm[splitDetails.length];

		for (int i = 0; i < splitDetails.length; i++)
			connectionParms[i] = decode(value,splitDetails[i].getOffset(),splitDetails[i].getLength());		

		return connectionParms;					
	}
}