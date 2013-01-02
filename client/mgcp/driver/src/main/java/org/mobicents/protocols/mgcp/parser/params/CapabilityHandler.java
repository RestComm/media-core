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

import org.mobicents.protocols.mgcp.parser.pkg.PackageNameHandler;

import jain.protocol.ip.mgcp.message.parms.CapabilityValue;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.LocalOptVal;
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
import jain.protocol.ip.mgcp.message.parms.SupportedPackages;
import jain.protocol.ip.mgcp.message.parms.SupportedModes;
import jain.protocol.ip.mgcp.message.parms.TypeOfService;
import jain.protocol.ip.mgcp.message.parms.TypeOfNetwork;
import jain.protocol.ip.mgcp.pkg.PackageName;

public class CapabilityHandler
{
	public static CapabilityValue decode(byte[] value,int offset,int length) throws ParseException
	{		
		if(length>1 && value[offset+1]==StringFunctions.COLON_BYTE)
		{
			switch(value[offset])
			{
				case StringFunctions.LOW_A_BYTE:
				case StringFunctions.HIGH_A_BYTE:					
					return new LocalOptVal(CompressionAlgorithmHandler.decode(value,offset+2,length-2)); 
				case StringFunctions.LOW_B_BYTE:
				case StringFunctions.HIGH_B_BYTE:
					return new LocalOptVal(BandwidthHandler.decode(value,offset+2,length-2));
				case StringFunctions.LOW_E_BYTE:
				case StringFunctions.HIGH_E_BYTE:
					return new LocalOptVal(EchoCancellationHandler.decode(value,offset+2,length-2));
				case StringFunctions.LOW_K_BYTE:
				case StringFunctions.HIGH_K_BYTE:
					return new LocalOptVal(EncryptionMethodHandler.decode(value,offset+2,length-2));
				case StringFunctions.LOW_P_BYTE:
				case StringFunctions.HIGH_P_BYTE:
					return new LocalOptVal(PacketizationPeriodHandler.decode(value,offset+2,length-2));
				case StringFunctions.LOW_R_BYTE:
				case StringFunctions.HIGH_R_BYTE:
					return new LocalOptVal(ResourceReservationHandler.decode(value,offset+2,length-2));
				case StringFunctions.LOW_S_BYTE:
				case StringFunctions.HIGH_S_BYTE:
					return new LocalOptVal(SilenceSuppressionHandler.decode(value,offset+2,length-2));
				case StringFunctions.LOW_T_BYTE:
				case StringFunctions.HIGH_T_BYTE:
					return new LocalOptVal(TypeOfServiceHandler.decode(value,offset+2,length-2));
				case StringFunctions.LOW_V_BYTE:
				case StringFunctions.HIGH_V_BYTE:
					PackageName[] supportedPackageNames = PackageNameHandler.decodeList(value,offset+2,length-2);
					return new SupportedPackages(supportedPackageNames);
				case StringFunctions.LOW_M_BYTE:
				case StringFunctions.HIGH_M_BYTE:
					ConnectionMode[] supportedConnectionModes=ConnectionModeHandler.decodeList(value,offset+2,length-2);
					return new SupportedModes(supportedConnectionModes);					
			}
		}
		else if(length>2 && value[offset+2]==StringFunctions.COLON_BYTE)
		{
			switch(value[offset])
			{
				case StringFunctions.LOW_N_BYTE:
				case StringFunctions.HIGH_N_BYTE:
					if(value[offset+1]==StringFunctions.LOW_T_BYTE || value[offset+1]==StringFunctions.HIGH_T_BYTE)
						return new LocalOptVal(TypeOfNetworkHandler.decode(value,offset+3,length-3)); 
				case StringFunctions.LOW_G_BYTE:
				case StringFunctions.HIGH_G_BYTE:
					if(value[offset+1]==StringFunctions.LOW_C_BYTE || value[offset+1]==StringFunctions.HIGH_C_BYTE)
						return new LocalOptVal(GainControlHandler.decode(value,offset+3,length-3));
			}
		}
		
		throw new ParseException("Could not parse capability: " + new String(value,offset,length), 0);					
	}
	
	public static int encode(byte[] destination,int offset,CapabilityValue capabilityValue)
	{		
		switch (capabilityValue.getCapabilityValueType()) 
		{
			case CapabilityValue.LOCAL_OPTION_VALUE:
				LocalOptVal localOptVal = (LocalOptVal) capabilityValue;
				LocalOptionValue localOptionValue = localOptVal.getLocalOptionValue();
				return LocalOptionValueHandler.encode(destination,offset,localOptionValue);				
			case CapabilityValue.SUPPORTED_PACKAGES:
				destination[offset]=StringFunctions.LOW_V_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				SupportedPackages supportedPackages = (SupportedPackages) capabilityValue;
				PackageName[] packageNameList = supportedPackages.getSupportedPackageNames();
				return PackageNameHandler.encodeList(destination,offset+2,packageNameList)+2;				
			case CapabilityValue.SUPPORTED_MODES:
				destination[offset]=StringFunctions.LOW_M_BYTE;
				destination[offset+1]=StringFunctions.COLON_BYTE;
				SupportedModes supportedModes = (SupportedModes) capabilityValue;
				ConnectionMode[] connectionModeList = supportedModes.getSupportedModes();
				return ConnectionModeHandler.encodeList(destination,offset+2,connectionModeList)+2;						
			default:
				return 0;
		}
	}
		
	public static int encodeList(byte[] destination,int offset,CapabilityValue[] capabilitiesList)
	{
		if(capabilitiesList.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<capabilitiesList.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,capabilitiesList[i]);
			destination[offset+totalLength]=StringFunctions.COMMA_BYTE;
			totalLength++;
		}
			
		totalLength+=encode(destination,offset+totalLength,capabilitiesList[i]);
		return totalLength;
	}	
	
	public static CapabilityValue[] decodeList(byte[] value,int offset,int length) throws ParseException
	{
		SplitDetails[] splitDetails=StringFunctions.split(value,offset,length,StringFunctions.COMMA_BYTE);
		CapabilityValue[] capabilities = new CapabilityValue[splitDetails.length];

		for (int i = 0; i < splitDetails.length; i++)
			capabilities[i] = decode(value,splitDetails[i].getOffset(),splitDetails[i].getLength());		

		return capabilities;					
	}
}