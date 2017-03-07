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

import org.restcomm.media.client.mgcp.parser.StringFunctions;

import jain.protocol.ip.mgcp.message.parms.ResourceReservation;

public class ResourceReservationHandler
{
	public static ResourceReservation decode(byte[] value,int offset,int length) throws ParseException
	{
		if(length==1)
		{
			if(value[offset]==StringFunctions.LOW_G_BYTE || value[offset]==StringFunctions.HIGH_G_BYTE)
				return ResourceReservation.Guaranteed;			
		}
		else if(length==2)
		{
			byte byte0=value[offset];
			byte byte1=value[offset+1];
			switch(byte0)
			{
				case StringFunctions.LOW_C_BYTE:
				case StringFunctions.HIGH_C_BYTE:
					if(byte1==StringFunctions.LOW_L_BYTE || byte1==StringFunctions.HIGH_L_BYTE)
						return ResourceReservation.ControlledLoad;			
					break;
				case StringFunctions.LOW_B_BYTE:
				case StringFunctions.HIGH_B_BYTE:
					if(byte1==StringFunctions.LOW_E_BYTE || byte1==StringFunctions.HIGH_E_BYTE)
						return ResourceReservation.BestEffort;			
					break;
			}
		}
		
		throw new ParseException("Invalid value for Resource Reservation:" + new String(value,offset,length), 0);		
	}
	
	public static int encode(byte[] destination,int offset,ResourceReservation resourceReservation)
	{				
		StringBuffer s = new StringBuffer("");
		switch (resourceReservation.getResourceReservation()) {
			case ResourceReservation.BEST_EFFORT:
				destination[offset]=StringFunctions.LOW_B_BYTE;
				destination[offset+1]=StringFunctions.LOW_E_BYTE;
				return 2;				
			case ResourceReservation.CONTROLLED_LOAD:
				destination[offset]=StringFunctions.LOW_C_BYTE;
				destination[offset+1]=StringFunctions.LOW_L_BYTE;
				return 2;
			case ResourceReservation.GUARANTEED:
				destination[offset]=StringFunctions.LOW_G_BYTE;
				return 1;			
		}
		return 0;
	}
}