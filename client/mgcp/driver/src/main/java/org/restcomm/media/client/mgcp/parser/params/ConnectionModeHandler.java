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

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

public class ConnectionModeHandler
{
	private static final byte[] SENDRECV_VALUE=new byte[] {
		StringFunctions.LOW_S_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_N_BYTE,
		StringFunctions.LOW_D_BYTE,StringFunctions.LOW_R_BYTE,StringFunctions.LOW_E_BYTE,
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_V_BYTE
	};
	
	private static final byte[] SENDONLY_VALUE=new byte[] {
		StringFunctions.LOW_S_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_N_BYTE,
		StringFunctions.LOW_D_BYTE,StringFunctions.LOW_O_BYTE,StringFunctions.LOW_N_BYTE,
		StringFunctions.LOW_L_BYTE,StringFunctions.LOW_Y_BYTE
	};
	
	private static final byte[] RECVONLY_VALUE=new byte[] {
		StringFunctions.LOW_R_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_C_BYTE,
		StringFunctions.LOW_V_BYTE,StringFunctions.LOW_O_BYTE,StringFunctions.LOW_N_BYTE,
		StringFunctions.LOW_L_BYTE,StringFunctions.LOW_Y_BYTE
	};
	
	private static final byte[] CONFRNCE_VALUE=new byte[] {
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_O_BYTE,StringFunctions.LOW_N_BYTE,
		StringFunctions.LOW_F_BYTE,StringFunctions.LOW_R_BYTE,StringFunctions.LOW_N_BYTE,
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_E_BYTE
	};
	
	private static final byte[] CONTTEST_VALUE=new byte[] {
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_O_BYTE,StringFunctions.LOW_N_BYTE,
		StringFunctions.LOW_T_BYTE,StringFunctions.LOW_T_BYTE,StringFunctions.LOW_E_BYTE,
		StringFunctions.LOW_S_BYTE,StringFunctions.LOW_T_BYTE
	};
	
	private static final byte[] LOOPBACK_VALUE=new byte[] {
		StringFunctions.LOW_L_BYTE,StringFunctions.LOW_O_BYTE,StringFunctions.LOW_O_BYTE,
		StringFunctions.LOW_P_BYTE,StringFunctions.LOW_B_BYTE,StringFunctions.LOW_A_BYTE,
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_K_BYTE
	};
	
	private static final byte[] NETWLOOP_VALUE=new byte[] {
		StringFunctions.LOW_N_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_T_BYTE,
		StringFunctions.LOW_W_BYTE,StringFunctions.LOW_L_BYTE,StringFunctions.LOW_O_BYTE,
		StringFunctions.LOW_O_BYTE,StringFunctions.LOW_P_BYTE
	};
	
	private static final byte[] NETWTEST_VALUE=new byte[] {
		StringFunctions.LOW_N_BYTE,StringFunctions.LOW_E_BYTE,StringFunctions.LOW_T_BYTE,
		StringFunctions.LOW_W_BYTE,StringFunctions.LOW_T_BYTE,StringFunctions.LOW_E_BYTE,
		StringFunctions.LOW_S_BYTE,StringFunctions.LOW_T_BYTE
	};
	
	private static final byte[] INACTIVE_VALUE=new byte[] {
		StringFunctions.LOW_I_BYTE,StringFunctions.LOW_N_BYTE,StringFunctions.LOW_A_BYTE,
		StringFunctions.LOW_C_BYTE,StringFunctions.LOW_T_BYTE,StringFunctions.LOW_I_BYTE,
		StringFunctions.LOW_V_BYTE,StringFunctions.LOW_E_BYTE
	};
	
	private static final byte[] DATA_VALUE=new byte[] {
		StringFunctions.LOW_D_BYTE,StringFunctions.LOW_A_BYTE,StringFunctions.LOW_T_BYTE,
		StringFunctions.LOW_A_BYTE
	};
	
	public static ConnectionMode decode(byte[] value,int offset,int length) throws ParseException
	{		
		if(length==8)
		{
			byte startByte=value[offset];
			byte middleByte=value[offset+4];
			
			byte char1=value[offset+1];
			byte char2=value[offset+2];
			byte char3=value[offset+3];
			byte char5=value[offset+5];
			byte char6=value[offset+6];
			byte char7=value[offset+7];
			
			switch(startByte)
			{
				case StringFunctions.LOW_S_BYTE:
				case StringFunctions.HIGH_S_BYTE:
					switch(middleByte)
					{
						case StringFunctions.LOW_R_BYTE:
						case StringFunctions.HIGH_R_BYTE:
							for(int i=1;i<4;i++)
								if(value[offset+i]!=SENDRECV_VALUE[i] && value[offset+i]!=(byte)(SENDRECV_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							for(int i=5;i<8;i++)
								if(value[offset+i]!=SENDRECV_VALUE[i] && value[offset+i]!=(byte)(SENDRECV_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							return ConnectionMode.SendRecv;								
						case StringFunctions.LOW_O_BYTE:
						case StringFunctions.HIGH_O_BYTE:
							for(int i=1;i<4;i++)
								if(value[offset+i]!=SENDONLY_VALUE[i] && value[offset+i]!=(byte)(SENDONLY_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							for(int i=5;i<8;i++)
								if(value[offset+i]!=SENDONLY_VALUE[i] && value[offset+i]!=(byte)(SENDONLY_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							return ConnectionMode.SendOnly;							
					}
					break;
				case StringFunctions.LOW_R_BYTE:
				case StringFunctions.HIGH_R_BYTE:
					switch(middleByte)
					{
						case StringFunctions.LOW_O_BYTE:
						case StringFunctions.HIGH_O_BYTE:
							for(int i=1;i<4;i++)
								if(value[offset+i]!=RECVONLY_VALUE[i] && value[offset+i]!=(byte)(RECVONLY_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							for(int i=5;i<8;i++)
								if(value[offset+i]!=RECVONLY_VALUE[i] && value[offset+i]!=(byte)(RECVONLY_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							return ConnectionMode.RecvOnly;							
					}
					break;
				case StringFunctions.LOW_C_BYTE:
				case StringFunctions.HIGH_C_BYTE:
					switch(middleByte)
					{
						case StringFunctions.LOW_R_BYTE:
						case StringFunctions.HIGH_R_BYTE:
							for(int i=1;i<4;i++)
								if(value[offset+i]!=CONFRNCE_VALUE[i] && value[offset+i]!=(byte)(CONFRNCE_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							for(int i=5;i<8;i++)
								if(value[offset+i]!=CONFRNCE_VALUE[i] && value[offset+i]!=(byte)(CONFRNCE_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							return ConnectionMode.Confrnce;							
						case StringFunctions.LOW_T_BYTE:
						case StringFunctions.HIGH_T_BYTE:
							for(int i=1;i<4;i++)
								if(value[offset+i]!=CONTTEST_VALUE[i] && value[offset+i]!=(byte)(CONTTEST_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							for(int i=5;i<8;i++)
								if(value[offset+i]!=CONTTEST_VALUE[i] && value[offset+i]!=(byte)(CONTTEST_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							return ConnectionMode.Conttest;							
					}
					break;
				case StringFunctions.LOW_L_BYTE:
				case StringFunctions.HIGH_L_BYTE:
					switch(middleByte)
					{
						case StringFunctions.LOW_B_BYTE:
						case StringFunctions.HIGH_B_BYTE:
							for(int i=1;i<4;i++)
								if(value[offset+i]!=LOOPBACK_VALUE[i] && value[offset+i]!=(byte)(LOOPBACK_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							for(int i=5;i<8;i++)
								if(value[offset+i]!=LOOPBACK_VALUE[i] && value[offset+i]!=(byte)(LOOPBACK_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							return ConnectionMode.Loopback;												
					}
					break;
				case StringFunctions.LOW_N_BYTE:
				case StringFunctions.HIGH_N_BYTE:
					switch(middleByte)
					{
						case StringFunctions.LOW_L_BYTE:
						case StringFunctions.HIGH_L_BYTE:
							for(int i=1;i<4;i++)
								if(value[offset+i]!=NETWLOOP_VALUE[i] && value[offset+i]!=(byte)(NETWLOOP_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							for(int i=5;i<8;i++)
								if(value[offset+i]!=NETWLOOP_VALUE[i] && value[offset+i]!=(byte)(NETWLOOP_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							return ConnectionMode.Netwloop;							
						case StringFunctions.LOW_T_BYTE:
						case StringFunctions.HIGH_T_BYTE:
							for(int i=1;i<4;i++)
								if(value[offset+i]!=NETWTEST_VALUE[i] && value[offset+i]!=(byte)(NETWTEST_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							for(int i=5;i<8;i++)
								if(value[offset+i]!=NETWTEST_VALUE[i] && value[offset+i]!=(byte)(NETWTEST_VALUE[i]+StringFunctions.CASE_STEP))
									return ConnectionMode.Inactive;
							
							return ConnectionMode.Netwtest;							
					}
					break;
			}
		}
		else if(length==4)
		{
			for(int i=0;i<4;i++)
				if(value[offset+i]!=DATA_VALUE[i] && value[offset+i]!=(byte)(DATA_VALUE[i]+StringFunctions.CASE_STEP))
					return ConnectionMode.Inactive;
			
			return ConnectionMode.Data; 
		}
		
		return ConnectionMode.Inactive;
	}
	
	public static int encode(byte[] destination,int offset,ConnectionMode connectionMode)
	{		
		switch(connectionMode.getConnectionModeValue()) 
		{
			case ConnectionMode.SENDRECV:
				System.arraycopy(SENDRECV_VALUE, 0, destination, offset, SENDRECV_VALUE.length);
				return SENDRECV_VALUE.length;				
			case ConnectionMode.SENDONLY:
				System.arraycopy(SENDONLY_VALUE, 0, destination, offset, SENDONLY_VALUE.length);
				return SENDONLY_VALUE.length;				
			case ConnectionMode.RECVONLY:
				System.arraycopy(RECVONLY_VALUE, 0, destination, offset, RECVONLY_VALUE.length);
				return RECVONLY_VALUE.length;				
			case ConnectionMode.CONFRNCE:
				System.arraycopy(CONFRNCE_VALUE, 0, destination, offset, CONFRNCE_VALUE.length);
				return CONFRNCE_VALUE.length;				
			case ConnectionMode.INACTIVE:
				System.arraycopy(INACTIVE_VALUE, 0, destination, offset, INACTIVE_VALUE.length);
				return INACTIVE_VALUE.length;				
			case ConnectionMode.LOOPBACK:
				System.arraycopy(LOOPBACK_VALUE, 0, destination, offset, LOOPBACK_VALUE.length);
				return LOOPBACK_VALUE.length;				
			case ConnectionMode.CONTTEST:
				System.arraycopy(CONTTEST_VALUE, 0, destination, offset, CONTTEST_VALUE.length);
				return CONTTEST_VALUE.length;				
			case ConnectionMode.NETWLOOP:
				System.arraycopy(NETWLOOP_VALUE, 0, destination, offset, NETWLOOP_VALUE.length);
				return NETWLOOP_VALUE.length;				
			case ConnectionMode.NETWTEST:
				System.arraycopy(NETWTEST_VALUE, 0, destination, offset, NETWTEST_VALUE.length);
				return NETWTEST_VALUE.length;				
			case ConnectionMode.DATA:
				System.arraycopy(DATA_VALUE, 0, destination, offset, DATA_VALUE.length);
				return DATA_VALUE.length;				
		}
		
		return 0;
	}
	
	public static int encodeList(byte[] destination,int offset,ConnectionMode[] connectionModes)
	{
		if(connectionModes.length==0)
			return 0;
			
		int totalLength=0;
		int i=0;
		for(;i<connectionModes.length-1;i++)
		{
			totalLength+=encode(destination,offset+totalLength,connectionModes[i]);
			destination[offset+totalLength]=StringFunctions.SEMICOLON_BYTE;
			totalLength++;
		}
			
		totalLength+=encode(destination,offset+totalLength,connectionModes[i]);
		return totalLength;		
	}
	
	public static ConnectionMode[] decodeList(byte[] value,int offset,int length) throws ParseException
	{
		SplitDetails[] splitDetails=StringFunctions.split(value,offset,length,StringFunctions.SEMICOLON_BYTE);
		ConnectionMode[] connectionModes = new ConnectionMode[splitDetails.length];

		for (int i = 0; i < splitDetails.length; i++)
			connectionModes[i] = decode(value,splitDetails[i].getOffset(),splitDetails[i].getLength());		

		return connectionModes;		
	}
}