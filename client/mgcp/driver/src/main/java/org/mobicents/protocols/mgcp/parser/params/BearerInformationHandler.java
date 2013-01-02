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
import jain.protocol.ip.mgcp.message.parms.BearerInformation;

public class BearerInformationHandler
{
	public static BearerInformation decode(byte[] value,int offset,int length) throws ParseException
	{
		if(value[offset]!=StringFunctions.LOW_E_BYTE && value[offset]!=StringFunctions.HIGH_E_BYTE)
			throw new ParseException("Bearer extensions not supported", 0);
		
		if(value[offset+1]!=StringFunctions.COLON_BYTE)
			throw new ParseException("Unknown value for BearerInformation: " + new String(value,offset,length), 0);
		
		switch(length)
		{
			case 3:
				if(value[offset+2]==StringFunctions.LOW_A_BYTE || value[offset+2]==StringFunctions.HIGH_A_BYTE)
					return BearerInformation.EncMethod_A_Law;				
				break;
			case 4:
				if((value[offset+2]==StringFunctions.LOW_M_BYTE || value[offset+2]==StringFunctions.HIGH_M_BYTE)
				 && (value[offset+3]==StringFunctions.LOW_U_BYTE || value[offset+3]==StringFunctions.HIGH_U_BYTE))
					return BearerInformation.EncMethod_mu_Law;		
				break;
		}
		
		throw new ParseException("Unknown value for BearerInformation: " + new String(value,offset,length), 0);		
	}
	
	public static int encode(byte[] destination,int offset,BearerInformation bearerInformation)
	{
		destination[offset]=StringFunctions.LOW_E_BYTE;
		destination[offset+1]=StringFunctions.COLON_BYTE;
		
		if (BearerInformation.ENC_METHOD_A_LAW == bearerInformation.getEncodingMethod()) {
			destination[offset+2]=StringFunctions.HIGH_A_BYTE;
			return 3;
		} else if (BearerInformation.ENC_METHOD_MU_LAW == bearerInformation.getEncodingMethod()) {
			destination[offset+2]=StringFunctions.LOW_M_BYTE;
			destination[offset+3]=StringFunctions.LOW_U_BYTE;
			return 4;
		}			
		
		return 2;
	}
}