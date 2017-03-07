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

import jain.protocol.ip.mgcp.message.parms.GainControl;

public class GainControlHandler
{
	private static final byte[] AUTO_VALUE=new byte[] {
		StringFunctions.LOW_A_BYTE,StringFunctions.LOW_U_BYTE,
		StringFunctions.LOW_T_BYTE,StringFunctions.LOW_O_BYTE
	};
	
	public static GainControl decode(byte[] value,int offset,int length) throws ParseException
	{				
		if(length==4)
		{
			boolean autoFound=true;
			for(int i=0;i<length;i++)
				if(value[offset+i]!=AUTO_VALUE[i] && value[offset+i]==(byte)(AUTO_VALUE[i]+StringFunctions.CASE_STEP))
				{
					autoFound=false;
					break;
				}
			
			if(autoFound)
				return new GainControl();		
		}
		
		int currIndex=offset;
		int resultValue=0;
		for(int i=0;i<length;i++,currIndex++)
		{			
			if(value[currIndex]>=StringFunctions.ZERO_BYTE && value[currIndex]<=StringFunctions.NINE_BYTE)
				resultValue=resultValue*10+(value[currIndex]-StringFunctions.ZERO_BYTE);
			else 
				throw new ParseException("Invalid gain control:" + new String(value,offset,length), 0);
		}
		
		return new GainControl(resultValue);
	}
	
	public static int encode(byte[] destination,int offset,GainControl gainControl)
	{		
		if (gainControl.getGainControl() == 0) {
			System.arraycopy(AUTO_VALUE, 0, destination, offset, AUTO_VALUE.length);
			return AUTO_VALUE.length;
		} else
			return StringFunctions.encodeInt(destination,offset,gainControl.getGainControl());			
	}
}