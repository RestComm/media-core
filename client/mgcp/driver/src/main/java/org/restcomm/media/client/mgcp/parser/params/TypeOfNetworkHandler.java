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

import jain.protocol.ip.mgcp.message.parms.TypeOfNetwork;

public class TypeOfNetworkHandler
{
	private static final byte[] IN_VALUE=new byte[] {
		StringFunctions.LOW_I_BYTE,StringFunctions.LOW_N_BYTE
	};
	
	private static final byte[] ATM_VALUE=new byte[] {
		StringFunctions.LOW_A_BYTE,StringFunctions.LOW_T_BYTE,StringFunctions.LOW_M_BYTE			
	};
	
	private static final byte[] LOCAL_VALUE=new byte[] {
		StringFunctions.LOW_L_BYTE,StringFunctions.LOW_O_BYTE,StringFunctions.LOW_C_BYTE,
		StringFunctions.LOW_A_BYTE,StringFunctions.LOW_L_BYTE
	};	
	
	public static TypeOfNetwork decode(byte[] value,int offset,int length) throws ParseException
	{
		switch(length)
		{
			case 2:
				for(int i=0;i<length;i++)
					if(value[offset+i]!=IN_VALUE[i] && value[offset+i]!=(byte)(IN_VALUE[i]+StringFunctions.CASE_STEP))
						throw new ParseException("Invalid type of network:" + new String(value,offset,length), 0);
				
				return TypeOfNetwork.In;				
			case 3:
				for(int i=0;i<length;i++)
					if(value[offset+i]!=ATM_VALUE[i] && value[offset+i]!=(byte)(ATM_VALUE[i]+StringFunctions.CASE_STEP))
						throw new ParseException("Invalid type of network:" + new String(value,offset,length), 0);
				
				return TypeOfNetwork.Atm;				
			case 5:
				for(int i=0;i<length;i++)
					if(value[offset+i]!=LOCAL_VALUE[i] && value[offset+i]!=(byte)(LOCAL_VALUE[i]+StringFunctions.CASE_STEP))
						throw new ParseException("Invalid type of network:" + new String(value,offset,length), 0);
				
				return TypeOfNetwork.Local;					
		}
		
		throw new ParseException("Extension restarts not (yet) supported:" + new String(value,offset,length), 0);								
	}
	
	public static int encode(byte[] destination,int offset,TypeOfNetwork typeOfNetwork)
	{				
		switch (typeOfNetwork.getTypeOfNetwork()) {
			case TypeOfNetwork.IN:
				System.arraycopy(IN_VALUE, 0, destination, offset, IN_VALUE.length);
				return IN_VALUE.length;				
			case TypeOfNetwork.ATM:
				System.arraycopy(ATM_VALUE, 0, destination, offset, ATM_VALUE.length);
				return ATM_VALUE.length;				
			case TypeOfNetwork.LOCAL:
				System.arraycopy(LOCAL_VALUE, 0, destination, offset, LOCAL_VALUE.length);
				return LOCAL_VALUE.length;				
		}
		
		return 0;
	}
}