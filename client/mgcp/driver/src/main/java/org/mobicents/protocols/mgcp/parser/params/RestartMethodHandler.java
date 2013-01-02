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
import jain.protocol.ip.mgcp.message.parms.RestartMethod;

public class RestartMethodHandler
{
	private static final byte[] GRACEFUL_VALUE=new byte[] {
		StringFunctions.LOW_G_BYTE,StringFunctions.LOW_R_BYTE,StringFunctions.LOW_A_BYTE,
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_F_BYTE,
		StringFunctions.LOW_U_BYTE,StringFunctions.LOW_L_BYTE
	};
	
	private static final byte[] FORCED_VALUE=new byte[] {
		StringFunctions.LOW_F_BYTE,StringFunctions.LOW_O_BYTE,StringFunctions.LOW_R_BYTE,
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_D_BYTE		
	};
	
	private static final byte[] RESTART_VALUE=new byte[] {
		StringFunctions.LOW_R_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_S_BYTE,
		StringFunctions.LOW_T_BYTE,StringFunctions.LOW_A_BYTE,StringFunctions.LOW_R_BYTE,
		StringFunctions.LOW_T_BYTE
	};
	
	private static final byte[] DISCONNECTED_VALUE=new byte[] {
		StringFunctions.LOW_D_BYTE,StringFunctions.LOW_I_BYTE,StringFunctions.LOW_S_BYTE,
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_O_BYTE,StringFunctions.LOW_N_BYTE,
		StringFunctions.LOW_N_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_C_BYTE,
		StringFunctions.LOW_T_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_D_BYTE,
	};
	
	private static final byte[] CANCEL_GRACEFUL_VALUE=new byte[] {
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_A_BYTE,StringFunctions.LOW_N_BYTE,
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_L_BYTE,
		StringFunctions.MINUS_BYTE,StringFunctions.LOW_G_BYTE,StringFunctions.LOW_R_BYTE,
		StringFunctions.LOW_A_BYTE,StringFunctions.LOW_T_BYTE,StringFunctions.LOW_E_BYTE,
		StringFunctions.LOW_F_BYTE,StringFunctions.LOW_U_BYTE,StringFunctions.LOW_L_BYTE,
	};
	
	public static RestartMethod decode(byte[] value,int offset,int length) throws ParseException
	{
		switch(length)
		{
			case 6:
				for(int i=0;i<length;i++)
					if(value[offset+i]!=FORCED_VALUE[i] && value[offset+i]!=(byte)(FORCED_VALUE[i]+StringFunctions.CASE_STEP))
						throw new ParseException("Extension restarts not (yet) supported:" + new String(value,offset,length), 0);
				
				return RestartMethod.Forced;				
			case 7:
				for(int i=0;i<length;i++)
					if(value[offset+i]!=RESTART_VALUE[i] && value[offset+i]!=(byte)(RESTART_VALUE[i]+StringFunctions.CASE_STEP))
						throw new ParseException("Extension restarts not (yet) supported:" + new String(value,offset,length), 0);
				
				return RestartMethod.Restart;				
			case 8:
				for(int i=0;i<length;i++)
					if(value[offset+i]!=GRACEFUL_VALUE[i] && value[offset+i]!=(byte)(GRACEFUL_VALUE[i]+StringFunctions.CASE_STEP))
						throw new ParseException("Extension restarts not (yet) supported:" + new String(value,offset,length), 0);
				
				return RestartMethod.Graceful;						
			case 12:
				for(int i=0;i<length;i++)
					if(value[offset+i]!=DISCONNECTED_VALUE[i] && value[offset+i]!=(byte)(DISCONNECTED_VALUE[i]+StringFunctions.CASE_STEP))
						throw new ParseException("Extension restarts not (yet) supported:" + new String(value,offset,length), 0);
				
				return RestartMethod.Disconnected;				
			case 15:
				for(int i=0;i<length;i++)
					if(value[offset+i]!=CANCEL_GRACEFUL_VALUE[i] && value[offset+i]!=(byte)(CANCEL_GRACEFUL_VALUE[i]+StringFunctions.CASE_STEP))
						throw new ParseException("Extension restarts not (yet) supported:" + new String(value,offset,length), 0);
				
				return RestartMethod.CancelGraceful;
		}
		
		throw new ParseException("Extension restarts not (yet) supported:" + new String(value,offset,length), 0);								
	}
	
	public static int encode(byte[] destination,int offset,RestartMethod restartMethod)
	{		
		switch(restartMethod.getRestartMethod()) 
		{
			case RestartMethod.FORCED:
				System.arraycopy(FORCED_VALUE, 0, destination, offset, FORCED_VALUE.length);
				return FORCED_VALUE.length;				
			case RestartMethod.RESTART:
				System.arraycopy(RESTART_VALUE, 0, destination, offset, RESTART_VALUE.length);
				return RESTART_VALUE.length;				
			case RestartMethod.GRACEFUL:
				System.arraycopy(GRACEFUL_VALUE, 0, destination, offset, GRACEFUL_VALUE.length);
				return GRACEFUL_VALUE.length;				
			case RestartMethod.DISCONNECTED:
				System.arraycopy(DISCONNECTED_VALUE, 0, destination, offset, DISCONNECTED_VALUE.length);
				return DISCONNECTED_VALUE.length;				
			case RestartMethod.CANCEL_GRACEFUL:
				System.arraycopy(CANCEL_GRACEFUL_VALUE, 0, destination, offset, CANCEL_GRACEFUL_VALUE.length);
				return CANCEL_GRACEFUL_VALUE.length;				
		}	
		
		return 0;
	}
}