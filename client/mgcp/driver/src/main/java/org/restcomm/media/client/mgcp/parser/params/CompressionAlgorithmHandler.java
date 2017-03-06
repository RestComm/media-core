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

import org.restcomm.media.client.mgcp.parser.SplitDetails;
import org.restcomm.media.client.mgcp.parser.StringFunctions;

import jain.protocol.ip.mgcp.message.parms.CompressionAlgorithm;

public class CompressionAlgorithmHandler
{
	public static CompressionAlgorithm decode(byte[] value,int offset,int length) throws ParseException
	{
		SplitDetails[] splitDetails=StringFunctions.split(value, offset, length, StringFunctions.SEMICOLON_BYTE);
		String[] result=new String[splitDetails.length];
		for(int i=0;i<result.length;i++)
			result[i]=new String(value,splitDetails[i].getOffset(),splitDetails[i].getLength());
		
		return new CompressionAlgorithm(result);
	}
	
	public static int encode(byte[] destination,int offset,CompressionAlgorithm compressionAlgorithm)
	{
		String[] names = compressionAlgorithm.getCompressionAlgorithmNames();
		if(names.length==0)
			return 0;
		
		int usedLength=0;
		byte[] currBytes;
		int i=0;
		for(;i<names.length-1;i++)
		{
			currBytes=names[i].getBytes();
			System.arraycopy(currBytes, 0, destination, offset+usedLength, currBytes.length);
			usedLength+=currBytes.length;
			destination[offset+usedLength]=StringFunctions.SEMICOLON_BYTE;
			usedLength++;
		}
		
		currBytes=names[i].getBytes();
		System.arraycopy(currBytes, 0, destination, offset+usedLength, currBytes.length);
		usedLength+=currBytes.length;
		return usedLength;
	}
}