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

package org.restcomm.media.client.mgcp.parser.pkg;

import java.text.ParseException;

import org.restcomm.media.client.mgcp.jain.pkg.AUMgcpEvent;
import org.restcomm.media.client.mgcp.jain.pkg.AUPackage;
import org.restcomm.media.client.mgcp.jain.pkg.SLPackage;
import org.restcomm.media.client.mgcp.parser.SplitDetails;
import org.restcomm.media.client.mgcp.parser.StringFunctions;

import jain.protocol.ip.mgcp.pkg.PackageName;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;

public class PackageNameHandler
{
	static PackageName auPackage=AUPackage.AU;
	static PackageName slPackage=SLPackage.SL;
	static MgcpEvent rfc2897pa = AUMgcpEvent.aupa;
	
	public static PackageName decode(byte[] value,int offset,int length) throws ParseException
	{		
		if(length==1)
		{
			switch(value[offset])
			{
				case StringFunctions.LOW_G_BYTE:
				case StringFunctions.HIGH_G_BYTE:
					return PackageName.GenericMedia;					
				case StringFunctions.LOW_D_BYTE:
				case StringFunctions.HIGH_D_BYTE:
					return PackageName.Dtmf;					
				case StringFunctions.LOW_M_BYTE:
				case StringFunctions.HIGH_M_BYTE:
					return PackageName.Mf;					
				case StringFunctions.LOW_T_BYTE:
				case StringFunctions.HIGH_T_BYTE:
					return PackageName.Trunk;					
				case StringFunctions.LOW_L_BYTE:
				case StringFunctions.HIGH_L_BYTE:
					return PackageName.Line;					
				case StringFunctions.LOW_H_BYTE:
				case StringFunctions.HIGH_H_BYTE:
					return PackageName.Handset;					
				case StringFunctions.LOW_R_BYTE:
				case StringFunctions.HIGH_R_BYTE:
					return PackageName.Rtp;					
				case StringFunctions.LOW_N_BYTE:
				case StringFunctions.HIGH_N_BYTE:
					return PackageName.Nas;					
				case StringFunctions.LOW_A_BYTE:
				case StringFunctions.HIGH_A_BYTE:
					return PackageName.Announcement;					
				case StringFunctions.ASTERISK_BYTE:
					return PackageName.AllPackages;					
			}
		}
		else if(length==2)
		{
			if((value[offset]==StringFunctions.LOW_A_BYTE || value[offset]==StringFunctions.HIGH_A_BYTE)
					&& (value[offset+1]==StringFunctions.LOW_U_BYTE || value[offset+1]==StringFunctions.HIGH_U_BYTE))
				return AUPackage.AU;
		}
		
		return PackageName.factory(new String(value,offset,length));
	}
	
	public static int encode(byte[] destination,int offset,PackageName packageName)
	{
		switch(packageName.intValue())		
		{
			case PackageName.ALL_PACKAGES:
				destination[offset]=StringFunctions.ASTERISK_BYTE;
				return 1;
			case PackageName.GENERIC_MEDIA:
				destination[offset]=StringFunctions.HIGH_G_BYTE;
				return 1;
			case PackageName.DTMF:
				destination[offset]=StringFunctions.HIGH_D_BYTE;
				return 1;
			case PackageName.MF:
				destination[offset]=StringFunctions.HIGH_M_BYTE;
				return 1;
			case PackageName.TRUNK:
				destination[offset]=StringFunctions.HIGH_T_BYTE;
				return 1;
			case PackageName.LINE:
				destination[offset]=StringFunctions.HIGH_L_BYTE;
				return 1;
			case PackageName.HANDSET:
				destination[offset]=StringFunctions.HIGH_H_BYTE;
				return 1;
			case PackageName.RTP:
				destination[offset]=StringFunctions.HIGH_R_BYTE;
				return 1;
			case PackageName.NAS:
				destination[offset]=StringFunctions.HIGH_N_BYTE;
				return 1;
			case PackageName.ANNOUNCEMENT:
				destination[offset]=StringFunctions.HIGH_A_BYTE;
				return 1;
			case AUPackage.ADVANCED_AUDIO:
				destination[offset]=StringFunctions.HIGH_A_BYTE;
				destination[offset+1]=StringFunctions.HIGH_U_BYTE;
				return 2;
		}
		
		byte[] value=packageName.toString().getBytes();
		System.arraycopy(value, 0, destination, offset, value.length);
		return value.length;
	}
	
	public static int encodeList(byte[] destination,int offset,PackageName[] packageNames)
	{
		if(packageNames.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<packageNames.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,packageNames[i]);
			destination[offset+totalLength]=StringFunctions.SEMICOLON_BYTE;
			totalLength++;
		}
			
		totalLength+=encode(destination,offset+totalLength,packageNames[i]);
		return totalLength;		
	}
	
	public static PackageName[] decodeList(byte[] value,int offset,int length) throws ParseException
	{
		SplitDetails[] splitDetails=StringFunctions.split(value,offset,length,StringFunctions.SEMICOLON_BYTE);
		PackageName[] packageNames = new PackageName[splitDetails.length];

		for (int i = 0; i < splitDetails.length; i++)
			packageNames[i] = decode(value,splitDetails[i].getOffset(),splitDetails[i].getLength());		

		return packageNames;		
	}
}