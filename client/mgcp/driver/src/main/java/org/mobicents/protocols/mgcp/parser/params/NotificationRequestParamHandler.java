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

import jain.protocol.ip.mgcp.message.parms.NotificationRequestParms;

public class NotificationRequestParamHandler
{
	public static int encode(byte[] destination,int offset,NotificationRequestParms notificationRequestParam)
	{
		destination[offset]=StringFunctions.HIGH_X_BYTE;
		destination[offset+1]=StringFunctions.COLON_BYTE;
		int totalLength=2;
		
		byte[] requestIDBytes=notificationRequestParam.getRequestIdentifier().toString().getBytes();
		System.arraycopy(requestIDBytes, 0, destination, offset+totalLength, requestIDBytes.length);
		totalLength+=requestIDBytes.length;
		
		destination[offset+totalLength]=StringFunctions.NEWLINE_BYTE;
		totalLength++;
		
		if (notificationRequestParam.getSignalRequests() != null)
		{
			destination[offset+totalLength]=StringFunctions.HIGH_S_BYTE;
			destination[offset+totalLength+1]=StringFunctions.COLON_BYTE;
			totalLength+=2;
			
			totalLength+=EventNameHandler.encodeList(destination,offset+totalLength,notificationRequestParam.getSignalRequests());
			
			destination[offset+totalLength]=StringFunctions.NEWLINE_BYTE;
			totalLength++;			
		}
		
		if (notificationRequestParam.getRequestedEvents() != null)
		{
			destination[offset+totalLength]=StringFunctions.HIGH_R_BYTE;
			destination[offset+totalLength+1]=StringFunctions.COLON_BYTE;
			totalLength+=2;
			
			totalLength+=RequestedEventHandler.encodeList(destination,offset+totalLength,notificationRequestParam.getRequestedEvents());
			
			destination[offset+totalLength]=StringFunctions.NEWLINE_BYTE;
			totalLength++;			
		}
		
		if (notificationRequestParam.getDetectEvents() != null)
		{
			destination[offset+totalLength]=StringFunctions.HIGH_T_BYTE;
			destination[offset+totalLength+1]=StringFunctions.COLON_BYTE;
			totalLength+=2;
			
			totalLength+=EventNameHandler.encodeList(destination,offset+totalLength,notificationRequestParam.getDetectEvents());
			
			destination[offset+totalLength]=StringFunctions.NEWLINE_BYTE;
			totalLength++;			
		}
		
		if (notificationRequestParam.getDigitMap() != null)
		{
			destination[offset+totalLength]=StringFunctions.HIGH_D_BYTE;
			destination[offset+totalLength+1]=StringFunctions.COLON_BYTE;
			totalLength+=2;
			
			totalLength+=DigitMapHandler.encode(destination,offset+totalLength,notificationRequestParam.getDigitMap());
			
			destination[offset+totalLength]=StringFunctions.NEWLINE_BYTE;
			totalLength++;			
		}
		
		return totalLength;
	}
}