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

import jain.protocol.ip.mgcp.message.parms.EncryptionMethod;

public class EncryptionMethodHandler
{
	private static final byte[] CLEAR_VALUE=new byte[] {
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_L_BYTE,StringFunctions.LOW_E_BYTE,
		StringFunctions.LOW_A_BYTE,StringFunctions.LOW_R_BYTE,StringFunctions.COLON_BYTE
	};
	
	private static final byte[] BASE64_VALUE=new byte[] {
		StringFunctions.LOW_B_BYTE,StringFunctions.LOW_A_BYTE,StringFunctions.LOW_S_BYTE,
		StringFunctions.LOW_E_BYTE,StringFunctions.SIX_BYTE,StringFunctions.FOUR_BYTE,StringFunctions.COLON_BYTE
	};
	
	private static final byte[] URI_VALUE=new byte[] {
		StringFunctions.LOW_U_BYTE,StringFunctions.LOW_R_BYTE,StringFunctions.LOW_I_BYTE,
		StringFunctions.COLON_BYTE
	};
	
	public static EncryptionMethod decode(byte[] value,int offset,int length) throws ParseException
	{		
		if(length>=URI_VALUE.length && value[offset + URI_VALUE.length-1]==StringFunctions.COLON_BYTE)
		{
			for(int i=0;i<URI_VALUE.length-1;i++)
				if(value[offset+i]!=URI_VALUE[i] && value[offset+i]!=(byte)(URI_VALUE[i]+StringFunctions.CASE_STEP))
					throw new ParseException("Invalid value for EncryptionData: " + new String(value,offset,length), 0);
			
			return new EncryptionMethod(EncryptionMethod.URI, new String(value,offset+URI_VALUE.length,length-URI_VALUE.length));
		}
		else if(length>=CLEAR_VALUE.length && value[offset + CLEAR_VALUE.length-1]==StringFunctions.COLON_BYTE)
		{
			for(int i=0;i<CLEAR_VALUE.length-1;i++)
				if(value[offset+i]!=CLEAR_VALUE[i] && value[offset+i]!=(byte)(CLEAR_VALUE[i]+StringFunctions.CASE_STEP))
					throw new ParseException("Invalid value for EncryptionData: " + new String(value,offset,length), 0);
			
			return new EncryptionMethod(EncryptionMethod.CLEAR, new String(value,offset+CLEAR_VALUE.length,length-CLEAR_VALUE.length));
		}
		else if(length>=BASE64_VALUE.length && value[offset + BASE64_VALUE.length-1]==StringFunctions.COLON_BYTE)
		{
			for(int i=0;i<BASE64_VALUE.length-1;i++)
				if(value[offset+i]!=BASE64_VALUE[i] && value[offset+i]!=(byte)(BASE64_VALUE[i]+StringFunctions.CASE_STEP))
					throw new ParseException("Invalid value for EncryptionData: " + new String(value,offset,length), 0);
			
			return new EncryptionMethod(EncryptionMethod.BASE64, new String(value,offset+BASE64_VALUE.length,length-BASE64_VALUE.length));
		}
		
		throw new ParseException("Invalid value for EncryptionData: " + new String(value,offset,length), 0);
	}
	
	public static int encode(byte[] destination,int offset,EncryptionMethod encryptionMethod)
	{
		int usedLength=0;
		byte[] key=encryptionMethod.getEncryptionKey().getBytes();
		switch (encryptionMethod.getEncryptionMethod()) {
			case EncryptionMethod.BASE64:
				System.arraycopy(BASE64_VALUE, 0, destination, offset, BASE64_VALUE.length);
				usedLength=BASE64_VALUE.length;
				System.arraycopy(key, 0, destination, offset+usedLength, key.length);
				usedLength+=key.length;			
			break;
			case EncryptionMethod.CLEAR:
				System.arraycopy(CLEAR_VALUE, 0, destination, offset, CLEAR_VALUE.length);
				usedLength=CLEAR_VALUE.length;
				System.arraycopy(key, 0, destination, offset+usedLength, key.length);
				usedLength+=key.length;
				break;
			case EncryptionMethod.URI:
				System.arraycopy(URI_VALUE, 0, destination, offset, URI_VALUE.length);
				usedLength=URI_VALUE.length;
				System.arraycopy(key, 0, destination, offset+usedLength, key.length);
				usedLength+=key.length;
				break;
		}
		
		return usedLength;
	}
}