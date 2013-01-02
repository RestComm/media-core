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

import jain.protocol.ip.mgcp.message.parms.LocalOptionValue;
import jain.protocol.ip.mgcp.message.parms.LocalOptionExtension;
import jain.protocol.ip.mgcp.message.parms.Bandwidth;
import jain.protocol.ip.mgcp.message.parms.CompressionAlgorithm;
import jain.protocol.ip.mgcp.message.parms.PacketizationPeriod;
import jain.protocol.ip.mgcp.message.parms.GainControl;
import jain.protocol.ip.mgcp.message.parms.EchoCancellation;
import jain.protocol.ip.mgcp.message.parms.EncryptionMethod;
import jain.protocol.ip.mgcp.message.parms.ResourceReservation;
import jain.protocol.ip.mgcp.message.parms.SilenceSuppression;
import jain.protocol.ip.mgcp.message.parms.TypeOfService;
import jain.protocol.ip.mgcp.message.parms.TypeOfNetwork;

public class LocalOptionValueHandler
{
	public static LocalOptionValue decode(byte[] value,int offset,int length) throws ParseException
	{		
		if(length>1 && value[offset+1]==StringFunctions.COLON_BYTE)
		{
			switch(value[offset])
			{
				case StringFunctions.LOW_A_BYTE:
				case StringFunctions.HIGH_A_BYTE:					
					return CompressionAlgorithmHandler.decode(value,offset+2,length-2); 
				case StringFunctions.LOW_B_BYTE:
				case StringFunctions.HIGH_B_BYTE:
					return BandwidthHandler.decode(value,offset+2,length-2);
				case StringFunctions.LOW_E_BYTE:
				case StringFunctions.HIGH_E_BYTE:
					return EchoCancellationHandler.decode(value,offset+2,length-2);
				case StringFunctions.LOW_K_BYTE:
				case StringFunctions.HIGH_K_BYTE:
					return EncryptionMethodHandler.decode(value,offset+2,length-2);
				case StringFunctions.LOW_P_BYTE:
				case StringFunctions.HIGH_P_BYTE:
					return PacketizationPeriodHandler.decode(value,offset+2,length-2);
				case StringFunctions.LOW_R_BYTE:
				case StringFunctions.HIGH_R_BYTE:
					return ResourceReservationHandler.decode(value,offset+2,length-2);
				case StringFunctions.LOW_S_BYTE:
				case StringFunctions.HIGH_S_BYTE:
					return SilenceSuppressionHandler.decode(value,offset+2,length-2);
				case StringFunctions.LOW_T_BYTE:
				case StringFunctions.HIGH_T_BYTE:
					return TypeOfServiceHandler.decode(value,offset+2,length-2);
			}
		}
		else if(length>2 && value[offset+2]==StringFunctions.COLON_BYTE)
		{
			switch(value[offset])
			{
				case StringFunctions.LOW_N_BYTE:
				case StringFunctions.HIGH_N_BYTE:
					if(value[offset+1]==StringFunctions.LOW_T_BYTE || value[offset+1]==StringFunctions.HIGH_T_BYTE)
						return TypeOfNetworkHandler.decode(value,offset+3,length-3); 
				case StringFunctions.LOW_G_BYTE:
				case StringFunctions.HIGH_G_BYTE:
					if(value[offset+1]==StringFunctions.LOW_C_BYTE || value[offset+1]==StringFunctions.HIGH_C_BYTE)
						return GainControlHandler.decode(value,offset+3,length-3);
			}
		}
		
		for(int i=3;i<length;i++)
			if(value[offset+i]==StringFunctions.COLON_BYTE)
				return new LocalOptionExtension(new String(value,offset,i), new String(value,offset+i+1,length-i-1));		
		
		throw new ParseException("Could not parse local connection option: " + new String(value,offset,length), 0);					
	}
	
	public static int encode(byte[] destination,int offset,LocalOptionValue localOptionValue)
	{		
		switch (localOptionValue.getLocalOptionValueType()) 
		{
			case LocalOptionValue.BANDWIDTH:
				destination[offset]=StringFunctions.LOW_B_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				return BandwidthHandler.encode(destination,offset+2,(Bandwidth) localOptionValue)+2;				
			case LocalOptionValue.COMPRESSION_ALGORITHM:
				destination[offset]=StringFunctions.LOW_A_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				return CompressionAlgorithmHandler.encode(destination,offset+2,(CompressionAlgorithm) localOptionValue)+2;				
			case LocalOptionValue.ECHO_CANCELLATION:
				destination[offset]=StringFunctions.LOW_E_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				return EchoCancellationHandler.encode(destination,offset+2,(EchoCancellation) localOptionValue)+2;				
			case LocalOptionValue.ENCRYPTION_METHOD:
				destination[offset]=StringFunctions.LOW_K_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				return EncryptionMethodHandler.encode(destination,offset+2,(EncryptionMethod) localOptionValue)+2;				
			case LocalOptionValue.PACKETIZATION_PERIOD:
				destination[offset]=StringFunctions.LOW_P_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				return PacketizationPeriodHandler.encode(destination,offset+2,(PacketizationPeriod) localOptionValue)+2;				
			case LocalOptionValue.RESOURCE_RESERVATION:
				destination[offset]=StringFunctions.LOW_R_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				return ResourceReservationHandler.encode(destination,offset+2,(ResourceReservation) localOptionValue)+2;				
			case LocalOptionValue.SILENCE_SUPPRESSION:
				destination[offset]=StringFunctions.LOW_S_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				return SilenceSuppressionHandler.encode(destination,offset+2,(SilenceSuppression) localOptionValue)+2;				
			case LocalOptionValue.TYPE_OF_SERVICE:
				destination[offset]=StringFunctions.LOW_T_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				return TypeOfServiceHandler.encode(destination,offset+2,(TypeOfService) localOptionValue)+2;				
			case LocalOptionValue.TYPE_OF_NETWORK:
				destination[offset]=StringFunctions.LOW_N_BYTE;
				destination[offset+1]=StringFunctions.LOW_T_BYTE;
				destination[offset+2]=StringFunctions.COLON_BYTE;
				return TypeOfNetworkHandler.encode(destination,offset+3,(TypeOfNetwork) localOptionValue)+3;				
			case LocalOptionValue.GAIN_CONTROL:
				destination[offset]=StringFunctions.LOW_G_BYTE;
				destination[offset+1]=StringFunctions.LOW_C_BYTE;
				destination[offset+2]=StringFunctions.COLON_BYTE;
				return GainControlHandler.encode(destination,offset+3,(GainControl) localOptionValue)+3;				
			case LocalOptionValue.LOCAL_OPTION_EXTENSION:
				LocalOptionExtension localOptionExtension = (LocalOptionExtension) localOptionValue;
				byte[] name=localOptionExtension.getLocalOptionExtensionName().getBytes();
				byte[] value=localOptionExtension.getLocalOptionExtensionValue().getBytes();
				System.arraycopy(name, 0, destination, offset, name.length);
				destination[offset+name.length]=StringFunctions.COLON_BYTE;
				System.arraycopy(value, 0, destination, offset+name.length+1, value.length);
				return name.length+value.length+1;
			default:
				return 0;
		}
	}
		
	public static int encodeList(byte[] destination,int offset,LocalOptionValue[] optionsList)
	{
		if(optionsList.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<optionsList.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,optionsList[i]);
			destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
			totalLength++;
		}
			
		totalLength+=encode(destination,offset+totalLength,optionsList[i]);
		return totalLength;
	}	
	
	public static LocalOptionValue[] decodeList(byte[] value,int offset,int length) throws ParseException
	{
		SplitDetails[] splitDetails=StringFunctions.split(value,offset,length,StringFunctions.COMMA_BYTE);
		LocalOptionValue[] options = new LocalOptionValue[splitDetails.length];

		for (int i = 0; i < splitDetails.length; i++)
			options[i] = decode(value,splitDetails[i].getOffset(),splitDetails[i].getLength());		

		return options;					
	}
}