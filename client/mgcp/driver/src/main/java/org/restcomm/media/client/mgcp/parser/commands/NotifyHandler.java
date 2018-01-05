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

package org.restcomm.media.client.mgcp.parser.commands;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.NotifyResponse;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.client.mgcp.handlers.MgcpContentHandler;
import org.restcomm.media.client.mgcp.handlers.TransactionHandler;
import org.restcomm.media.client.mgcp.parser.SplitDetails;
import org.restcomm.media.client.mgcp.parser.StringFunctions;
import org.restcomm.media.client.mgcp.parser.params.EndpointIdentifierHandler;
import org.restcomm.media.client.mgcp.parser.params.EventNameHandler;
import org.restcomm.media.client.mgcp.parser.params.NotifiedEntityHandler;
import org.restcomm.media.client.mgcp.parser.params.ReturnCodeHandler;
import org.restcomm.media.client.mgcp.stack.JainMgcpStackImpl;

/**
 * @author Oleg Kulikov
 * @author Amit Bhayani
 * @author Yulian Oifa
 * 
 */

public class NotifyHandler extends TransactionHandler 
{
	public static final byte[] COMMAND_NAME=new byte[] {
		StringFunctions.HIGH_N_BYTE,StringFunctions.HIGH_T_BYTE,
		StringFunctions.HIGH_F_BYTE,StringFunctions.HIGH_Y_BYTE
	};
	
	private Notify command;
	private NotifyResponse response;

	private static final Logger logger = LogManager.getLogger(NotifyHandler.class);

	public NotifyHandler(JainMgcpStackImpl stack) 
	{
		super(stack);
	}

	public NotifyHandler(JainMgcpStackImpl stack, InetAddress address, int port) 
	{
		super(stack, address, port);
	}

	@Override
	public JainMgcpCommandEvent decodeCommand(byte[] data,SplitDetails[] message) throws ParseException 
	{
		command = new Notify(source != null ? source : stack, endpoint, new RequestIdentifier("0"), new EventName[] {});
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
		response = new NotifyResponse(source != null ? source : stack, returnCode);
		response.setTransactionHandle(txID);
		
		try 
		{
			(new ResponseContentHandle()).parse(data,msg);
		} 
		catch (IOException e) 
		{
			logger.error("Something wrong while parsing the NOTIFY Response received", e);
		}

		return response;
	}

	@Override
	public int encode(JainMgcpCommandEvent event,byte[] array) 
	{
		Notify evt = (Notify) event;
		int totalLength=5;
		System.arraycopy(COMMAND_NAME, 0, array, 0, 4);
		array[4]=StringFunctions.SPACE_BYTE;		
		totalLength+=StringFunctions.encodeInt(array,5,event.getTransactionHandle());
		array[totalLength++]=StringFunctions.SPACE_BYTE;		
		totalLength+=EndpointIdentifierHandler.encode(array,totalLength,evt.getEndpointIdentifier());
		array[totalLength++]=StringFunctions.SPACE_BYTE;		
		System.arraycopy(MGCP_VERSION, 0, array, totalLength, MGCP_VERSION.length);
		totalLength+=MGCP_VERSION.length;
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;

		if (evt.getNotifiedEntity() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_N_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=NotifiedEntityHandler.encode(array,totalLength,evt.getNotifiedEntity());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		array[totalLength++]=StringFunctions.HIGH_X_BYTE;
		array[totalLength++]=StringFunctions.COLON_BYTE;
		byte[] requestBytes=evt.getRequestIdentifier().toString().getBytes();
		System.arraycopy(requestBytes, 0, array,totalLength, requestBytes.length);
		totalLength+=requestBytes.length;			
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;

		array[totalLength++]=StringFunctions.HIGH_O_BYTE;
		array[totalLength++]=StringFunctions.COLON_BYTE;
		totalLength+=EventNameHandler.encodeList(array,totalLength,evt.getObservedEvents());
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		
		return totalLength;
	}

	@Override
	public int encode(JainMgcpResponseEvent event,byte[] array) 
	{
		NotifyResponse response = (NotifyResponse) event;
		ReturnCode returnCode = response.getReturnCode();
		
		int totalLength=ReturnCodeHandler.encode(array,0,returnCode);
		array[totalLength++]=StringFunctions.SPACE_BYTE;		
		totalLength+=StringFunctions.encodeInt(array,totalLength,response.getTransactionHandle());
		array[totalLength++]=StringFunctions.SPACE_BYTE;				
		byte[] commentBytes=returnCode.getComment().getBytes();
		System.arraycopy(commentBytes, 0, array,totalLength, commentBytes.length);
		totalLength+=commentBytes.length;
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		
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
					case StringFunctions.LOW_N_BYTE:
					case StringFunctions.HIGH_N_BYTE:							
						command.setNotifiedEntity(NotifiedEntityHandler.decode(data,value.getOffset(),value.getLength(), false));
						break;
					case StringFunctions.LOW_X_BYTE:
					case StringFunctions.HIGH_X_BYTE:							
						command.setRequestIdentifier(new RequestIdentifier(new String(data,value.getOffset(),value.getLength())));
						break;
					case StringFunctions.LOW_O_BYTE:
					case StringFunctions.HIGH_O_BYTE:							
						command.setObservedEvents(EventNameHandler.decodeList(data,value.getOffset(),value.getLength()));
						break;
					default:
						logger.warn("Unidentified NTFY Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
						break;
				}
			}
			else
				logger.warn("Unidentified NTFY Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));					
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

		public void header(String header) throws ParseException 
		{
			
		}

		public void param(byte[] data,SplitDetails name,SplitDetails value) throws ParseException 
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void sessionDescription(String sd) throws ParseException 
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() 
	{
		NotifyResponse provisionalresponse = null;
		
		if (!sent) 
			provisionalresponse = new NotifyResponse(source != null ? source : stack, ReturnCode.Transaction_Being_Executed);
		
		return provisionalresponse;
	}
}