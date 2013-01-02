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

package org.mobicents.protocols.mgcp.parser.commands;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.RestartInProgress;
import jain.protocol.ip.mgcp.message.RestartInProgressResponse;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RestartMethod;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;

import org.apache.log4j.Logger;

import org.mobicents.protocols.mgcp.handlers.MgcpContentHandler;
import org.mobicents.protocols.mgcp.handlers.TransactionHandler;

import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;

import org.mobicents.protocols.mgcp.parser.StringFunctions;
import org.mobicents.protocols.mgcp.parser.SplitDetails;

import org.mobicents.protocols.mgcp.parser.params.EndpointIdentifierHandler;
import org.mobicents.protocols.mgcp.parser.params.RestartMethodHandler;
import org.mobicents.protocols.mgcp.parser.params.ReasonCodeHandler;
import org.mobicents.protocols.mgcp.parser.params.ReturnCodeHandler;
import org.mobicents.protocols.mgcp.parser.params.NotifiedEntityHandler;

/**
 * Parse/encode RSIP commands.
 * 
 * @author Tom Uijldert
 * @author Yulian Oifa
 * 
 */
public class RestartInProgressHandler extends TransactionHandler {

	public static final byte[] COMMAND_NAME=new byte[] {
		StringFunctions.HIGH_R_BYTE,StringFunctions.HIGH_S_BYTE,
		StringFunctions.HIGH_I_BYTE,StringFunctions.HIGH_P_BYTE
	};
	
	private static final Logger logger = Logger.getLogger(RestartInProgressHandler.class);

	private RestartInProgress command;
	private RestartInProgressResponse response;

	public RestartInProgressHandler(JainMgcpStackImpl stack) 
	{
		super(stack);
	}

	public RestartInProgressHandler(JainMgcpStackImpl stack, InetAddress address, int port) 
	{
		super(stack, address, port);
	}

	@Override
	public JainMgcpCommandEvent decodeCommand(byte[] data,SplitDetails[] message) throws ParseException 
	{
		command = new RestartInProgress(source != null ? source : stack, endpoint, RestartMethod.Restart);
		command.setTransactionHandle(remoteTID);
		try 
		{
			(new CommandContentHandle()).parse(data,message);
		} 
		catch (Exception e) 
		{
			throw new ParseException(e.getMessage(), -1);
		}
		
		return command;
	}

	@Override
	public JainMgcpResponseEvent decodeResponse(byte[] data,SplitDetails[] msg,Integer txID,ReturnCode returnCode) throws ParseException 
	{
		response = new RestartInProgressResponse(source != null ? source : stack, returnCode);
		response.setTransactionHandle(txID);
		
		try 
		{
			(new ResponseContentHandle()).parse(data,msg);
		} 
		catch (IOException e) 
		{
			// should never happen
		}
		return response;
	}

	@Override
	public int encode(JainMgcpCommandEvent event,byte[] array) 
	{
		RestartInProgress rsip = (RestartInProgress) event;
		
		int totalLength=5;
		System.arraycopy(COMMAND_NAME, 0, array, 0, 4);
		array[4]=StringFunctions.SPACE_BYTE;		
		totalLength+=StringFunctions.encodeInt(array,5,event.getTransactionHandle());
		array[totalLength++]=StringFunctions.SPACE_BYTE;		
		totalLength+=EndpointIdentifierHandler.encode(array,totalLength,rsip.getEndpointIdentifier());
		array[totalLength++]=StringFunctions.SPACE_BYTE;		
		System.arraycopy(MGCP_VERSION, 0, array, totalLength, MGCP_VERSION.length);
		totalLength+=MGCP_VERSION.length;
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		
		array[totalLength++]=StringFunctions.HIGH_R_BYTE;
		array[totalLength++]=StringFunctions.HIGH_M_BYTE;
		array[totalLength++]=StringFunctions.COLON_BYTE;
		totalLength+=RestartMethodHandler.encode(array,totalLength,rsip.getRestartMethod());		
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		
		if (rsip.getRestartDelay() != 0) 
		{
			array[totalLength++]=StringFunctions.HIGH_R_BYTE;
			array[totalLength++]=StringFunctions.HIGH_D_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=StringFunctions.encodeInt(array,totalLength,rsip.getRestartDelay());		
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;							
		}
		
		if (rsip.getReasonCode() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_E_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=ReasonCodeHandler.encode(array,totalLength,rsip.getReasonCode());		
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}
		
		return totalLength;
	}

	@Override
	public int encode(JainMgcpResponseEvent event,byte[] array) 
	{
		RestartInProgressResponse response = (RestartInProgressResponse) event;
				
		ReturnCode returnCode = response.getReturnCode();
		int totalLength=ReturnCodeHandler.encode(array,0,returnCode);
		array[totalLength++]=StringFunctions.SPACE_BYTE;		
		totalLength+=StringFunctions.encodeInt(array,totalLength,response.getTransactionHandle());
		array[totalLength++]=StringFunctions.SPACE_BYTE;				
		byte[] commentBytes=returnCode.getComment().getBytes();
		System.arraycopy(commentBytes, 0, array,totalLength, commentBytes.length);
		totalLength+=commentBytes.length;
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;
				
		if (response.getNotifiedEntity() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_N_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=NotifiedEntityHandler.encode(array,totalLength,response.getNotifiedEntity());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		return totalLength;		
	}

	private class CommandContentHandle extends MgcpContentHandler 
	{
		public CommandContentHandle() 
		{
		}

		public void param(byte[] data,SplitDetails name,SplitDetails value) throws ParseException 
		{
			if(name.getLength()==1)
			{
				switch(data[name.getOffset()])
				{
					case StringFunctions.LOW_E_BYTE:
					case StringFunctions.HIGH_E_BYTE:
						command.setReasonCode(ReasonCodeHandler.decode(data,value.getOffset(),value.getLength()));		
						break;
					default:
						logger.warn("Unidentified RSIP Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
						break;
				}
			}
			else if(name.getLength()==2)
			{
				switch(data[name.getOffset()+1])
				{
					case StringFunctions.LOW_M_BYTE:
					case StringFunctions.HIGH_M_BYTE:
						if(data[name.getOffset()]==StringFunctions.LOW_R_BYTE || data[name.getOffset()]==StringFunctions.HIGH_R_BYTE)							
							command.setRestartMethod(RestartMethodHandler.decode(data,value.getOffset(),value.getLength()));
						else
							logger.warn("Unidentified RSIP Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));							
						break;
					case StringFunctions.LOW_D_BYTE:
					case StringFunctions.HIGH_D_BYTE:
						if(data[name.getOffset()]==StringFunctions.LOW_R_BYTE || data[name.getOffset()]==StringFunctions.HIGH_R_BYTE)
						{
							Integer restartDelay = new Integer(0);
							int currIndex=value.getOffset();
							for(int j=0;j<value.getLength();j++,currIndex++)
							{
								if(data[currIndex]>=StringFunctions.ZERO_BYTE && data[currIndex]<=StringFunctions.NINE_BYTE)
									restartDelay=restartDelay*10+(data[currIndex]-StringFunctions.ZERO_BYTE);
								else 
									throw new ParseException("Invalid restart delay:" + new String(data,value.getOffset(),value.getLength()), 0);
							}
							command.setRestartDelay(restartDelay);
						}
						else
							logger.warn("Unidentified RSIP Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));							
						break;
					default:
						logger.warn("Unidentified RSIP Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
						break;
				}
			}
			else 
				logger.warn("Unidentified RSIP Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));			
		}

		public void sessionDescription(String sd) throws ParseException 
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

	private class ResponseContentHandle extends MgcpContentHandler 
	{
		public ResponseContentHandle() 
		{
		}

		public void param(byte[] data,SplitDetails name,SplitDetails value) throws ParseException 
		{
			if(name.getLength()==1)
			{
				switch(data[name.getOffset()])
				{
					case StringFunctions.LOW_N_BYTE:
					case StringFunctions.HIGH_N_BYTE:
						NotifiedEntity n = NotifiedEntityHandler.decode(data,value.getOffset(),value.getLength(), true);
						response.setNotifiedEntity(n);		
						break;
					default:
						logger.warn("Unidentified RSIP Response parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));		
				}
			}
			else
				logger.warn("Unidentified RSIP Response parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));				
		}

		public void sessionDescription(String sd) throws ParseException 
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() 
	{
		RestartInProgressResponse provisionalResponse = null;
		if (!sent) 
		{
			provisionalResponse = new RestartInProgressResponse(source != null ? source : stack,ReturnCode.Transaction_Being_Executed);
			provisionalResponse.setTransactionHandle(remoteTID);
		}
		return provisionalResponse;
	}
}