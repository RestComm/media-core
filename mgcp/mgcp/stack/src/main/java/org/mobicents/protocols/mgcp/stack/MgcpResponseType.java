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

package org.mobicents.protocols.mgcp.stack;

public enum MgcpResponseType {

	ResponseAcknowledgement(0,99), ProvisionalResponse(100,199), SuccessResponse(200,299),TransientError(400,499),PermanentError(500,599), PackageSpecific(800,899);
	
	
	private int lowRange=-1;
	private int highRange=-1;
	
	private MgcpResponseType(int low, int high)
	{
		this.lowRange=low;
		this.highRange=high;
	}
	
	
	public static MgcpResponseType getResponseTypeFromCode(int responseCode)
	{
		if(responseCode>=0 && responseCode<=99)
		{
			return ResponseAcknowledgement;
		}else if(responseCode>=100 && responseCode<=199)
		{
			return ProvisionalResponse;
		}else if(responseCode>=200 && responseCode<=299)
		{
			return SuccessResponse;
		}else if(responseCode>=400 && responseCode<=499)
		{
			return TransientError;
		}else if(responseCode>=500 && responseCode<=599)
		{
			return PermanentError;
		}else if(responseCode>=800 && responseCode<=899)
		{
			return PackageSpecific;
		}else 
		{
			return null;
		}
		
		
		
		
	}
	
	
}
