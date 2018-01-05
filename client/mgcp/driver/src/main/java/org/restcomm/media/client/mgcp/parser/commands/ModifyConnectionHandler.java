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

/*
 * File Name     : CreateConnectionHandle.java
 *
 * The JAIN MGCP API implementaion.
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */

package org.restcomm.media.client.mgcp.parser.commands;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.ModifyConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotificationRequestParms;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
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
import org.restcomm.media.client.mgcp.parser.params.BearerInformationHandler;
import org.restcomm.media.client.mgcp.parser.params.ConnectionModeHandler;
import org.restcomm.media.client.mgcp.parser.params.EndpointIdentifierHandler;
import org.restcomm.media.client.mgcp.parser.params.EventNameHandler;
import org.restcomm.media.client.mgcp.parser.params.LocalOptionValueHandler;
import org.restcomm.media.client.mgcp.parser.params.NotificationRequestParamHandler;
import org.restcomm.media.client.mgcp.parser.params.NotifiedEntityHandler;
import org.restcomm.media.client.mgcp.parser.params.RequestedEventHandler;
import org.restcomm.media.client.mgcp.parser.params.ReturnCodeHandler;
import org.restcomm.media.client.mgcp.stack.JainMgcpStackImpl;

/**
 * 
 * @author Oleg Kulikov
 * @author Pavel Mitrenko
 * @author amit bhayani
 * @author yulian oifa
 */
public class ModifyConnectionHandler extends TransactionHandler 
{
	public static final byte[] COMMAND_NAME=new byte[] {
		StringFunctions.HIGH_M_BYTE,StringFunctions.HIGH_D_BYTE,
		StringFunctions.HIGH_C_BYTE,StringFunctions.HIGH_X_BYTE
	};
	
	private ModifyConnection command = null;
	private ModifyConnectionResponse response = null;

	private static final Logger logger = LogManager.getLogger(ModifyConnectionHandler.class);

	/** Creates a new instance of ModifyConnectionHandler */
	public ModifyConnectionHandler(JainMgcpStackImpl stack) {
		super(stack);
	}

	public ModifyConnectionHandler(JainMgcpStackImpl stack, InetAddress address, int port) 
	{
		super(stack, address, port);
	}

	public JainMgcpCommandEvent decodeCommand(byte[] data,SplitDetails[] message) throws ParseException 
	{
		command = new ModifyConnection(source != null ? source : stack, new CallIdentifier("00"), endpoint,new ConnectionIdentifier("00"));
		command.setTransactionHandle(remoteTID);
		
		try 
		{
			(new CommandContentHandle()).parse(data,message);
		} 
		catch (IOException e) 
		{
			logger.error("Decoding of MDCX command failed", e);
		}

		NotifiedEntity notifiedEntity = command.getNotifiedEntity();
		
		if (command.getNotifiedEntity() != null) 
			this.stack.provider.setNotifiedEntity(notifiedEntity);
		
		return command;
	}

	public JainMgcpResponseEvent decodeResponse(byte[] data,SplitDetails[] msg,Integer txID,ReturnCode returnCode) throws ParseException 
	{
		response = new ModifyConnectionResponse(source != null ? source : stack, returnCode);
		response.setTransactionHandle(txID);
		
		try 
		{
			(new ResponseContentHandle()).parse(data,msg);
		} 
		catch (IOException e) 
		{
			logger.error("Decoding of MDCX Response failed", e);
		}

		return response;
	}

	public int encode(JainMgcpCommandEvent event,byte[] array) 
	{
		// encode message header
		ModifyConnection evt = (ModifyConnection) event;
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

		// encode mandatory parameters
		array[totalLength++]=StringFunctions.HIGH_C_BYTE;
		array[totalLength++]=StringFunctions.COLON_BYTE;
		byte[] callBytes=evt.getCallIdentifier().toString().getBytes();
		System.arraycopy(callBytes, 0, array,totalLength, callBytes.length);
		totalLength+=callBytes.length;
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		
		array[totalLength++]=StringFunctions.HIGH_I_BYTE;
		array[totalLength++]=StringFunctions.COLON_BYTE;
		byte[] connectionIdentifierBytes=evt.getConnectionIdentifier().toString().getBytes();
		System.arraycopy(connectionIdentifierBytes, 0, array,totalLength, connectionIdentifierBytes.length);
		totalLength+=connectionIdentifierBytes.length;			
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;

		// encode optional parameters

		if (evt.getBearerInformation() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_B_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=BearerInformationHandler.encode(array,totalLength,evt.getBearerInformation());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (evt.getLocalConnectionOptions() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_L_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;			
			totalLength+=LocalOptionValueHandler.encodeList(array,totalLength,evt.getLocalConnectionOptions());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (evt.getMode() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_M_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=ConnectionModeHandler.encode(array,totalLength,evt.getMode());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (evt.getNotificationRequestParms() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_L_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;			
			totalLength+=NotificationRequestParamHandler.encode(array,totalLength,evt.getNotificationRequestParms());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (evt.getNotifiedEntity() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_N_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=NotifiedEntityHandler.encode(array,totalLength,evt.getNotifiedEntity());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (evt.getRemoteConnectionDescriptor() != null) 
		{
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
			byte[] rcdBytes=evt.getRemoteConnectionDescriptor().toString().getBytes();
			System.arraycopy(rcdBytes, 0, array,totalLength, rcdBytes.length);
			totalLength+=rcdBytes.length;			
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		return totalLength;
	}

	public int encode(JainMgcpResponseEvent event,byte[] array) 
	{
		ModifyConnectionResponse response = (ModifyConnectionResponse) event;
		ReturnCode returnCode = response.getReturnCode();
		int totalLength=ReturnCodeHandler.encode(array,0,returnCode);
		array[totalLength++]=StringFunctions.SPACE_BYTE;		
		totalLength+=StringFunctions.encodeInt(array,totalLength,response.getTransactionHandle());
		array[totalLength++]=StringFunctions.SPACE_BYTE;				
		byte[] commentBytes=returnCode.getComment().getBytes();
		System.arraycopy(commentBytes, 0, array,totalLength, commentBytes.length);
		totalLength+=commentBytes.length;
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		
		if (response.getLocalConnectionDescriptor() != null) 
		{
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
			byte[] rcdBytes=response.getLocalConnectionDescriptor().toString().getBytes();
			System.arraycopy(rcdBytes, 0, array,totalLength, rcdBytes.length);
			totalLength+=rcdBytes.length;			
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}

		return totalLength;
	}

	private class CommandContentHandle extends MgcpContentHandler 
	{
		public CommandContentHandle() 
		{		
		}

		/**
		 * Receive notification of the parameter of a message. Parser will call
		 * this method to report about parameter reading.
		 * 
		 * @param name
		 *            the name of the paremeter
		 * @param value
		 *            the value of the parameter.
		 */
		public void param(byte[] data,SplitDetails name, SplitDetails value) throws ParseException 
		{			
			if(name.getLength()==1)
			{
				switch(data[name.getOffset()])
				{
					case StringFunctions.LOW_B_BYTE:
					case StringFunctions.HIGH_B_BYTE:
						command.setBearerInformation(BearerInformationHandler.decode(data,value.getOffset(),value.getLength()));		
						break;
					case StringFunctions.LOW_C_BYTE:
					case StringFunctions.HIGH_C_BYTE:
						command.setCallIdentifier(new CallIdentifier(new String(data,value.getOffset(),value.getLength())));
						break;
					case StringFunctions.LOW_I_BYTE:
					case StringFunctions.HIGH_I_BYTE:
						command.setConnectionIdentifier(new ConnectionIdentifier(new String(data,value.getOffset(),value.getLength())));
						break;
					case StringFunctions.LOW_M_BYTE:
					case StringFunctions.HIGH_M_BYTE:
						command.setMode(ConnectionModeHandler.decode(data,value.getOffset(),value.getLength()));
						break;
					case StringFunctions.LOW_L_BYTE:
					case StringFunctions.HIGH_L_BYTE:
						command.setLocalConnectionOptions(LocalOptionValueHandler.decodeList(data,value.getOffset(),value.getLength()));		
						break;
					case StringFunctions.LOW_N_BYTE:
					case StringFunctions.HIGH_N_BYTE:
						command.setNotifiedEntity(NotifiedEntityHandler.decode(data,value.getOffset(),value.getLength(), true));		
						break;
					case StringFunctions.LOW_X_BYTE:
					case StringFunctions.HIGH_X_BYTE:
						command.setNotificationRequestParms(new NotificationRequestParms(new RequestIdentifier(new String(data,value.getOffset(),value.getLength()))));		
						break;
					case StringFunctions.LOW_R_BYTE:
					case StringFunctions.HIGH_R_BYTE:
						command.getNotificationRequestParms().setRequestedEvents(RequestedEventHandler.decodeList(data,value.getOffset(),value.getLength()));		
						break;
					case StringFunctions.LOW_S_BYTE:
					case StringFunctions.HIGH_S_BYTE:
						command.getNotificationRequestParms().setSignalRequests(EventNameHandler.decodeList(data,value.getOffset(),value.getLength()));		
						break;
					case StringFunctions.LOW_T_BYTE:
					case StringFunctions.HIGH_T_BYTE:
						command.getNotificationRequestParms().setDetectEvents(EventNameHandler.decodeList(data,value.getOffset(),value.getLength()));
						break;
					default:
						logger.warn("Unidentified MDCX Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
						break;						
				}
			}
			else
				logger.warn("Unidentified MDCX Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));			
		}

		/**
		 * Receive notification of the session description. Parser will call
		 * this method to report about session descriptor reading.
		 * 
		 * @param sd
		 *            the session description from message.
		 */
		public void sessionDescription(String sd) throws ParseException 
		{
			command.setRemoteConnectionDescriptor(new ConnectionDescriptor(sd));
		}
	}

	private class ResponseContentHandle extends MgcpContentHandler 
	{
		public ResponseContentHandle() 
		{
		}

		/**
		 * Receive notification of the parameter of a message. Parser will call
		 * this method to report about parameter reading.
		 * 
		 * @param name
		 *            the name of the paremeter
		 * @param value
		 *            the value of the parameter.
		 */
		public void param(byte[] data,SplitDetails name, SplitDetails value) throws ParseException 
		{
			// hasn't parameters
		}

		/**
		 * Receive notification of the session description. Parser will call
		 * this method to report about session descriptor reading.
		 * 
		 * @param sd
		 *            the session description from message.
		 */
		public void sessionDescription(String sd) throws ParseException 
		{
			response.setLocalConnectionDescriptor(new ConnectionDescriptor(sd));
		}
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() 
	{
		ModifyConnectionResponse provisionalresponse = null;
		if (!sent) 
			provisionalresponse = new ModifyConnectionResponse(source != null ? source : stack,ReturnCode.Transaction_Being_Executed);
		
		return provisionalresponse;
	}
}