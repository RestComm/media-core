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
import jain.protocol.ip.mgcp.message.AuditConnection;
import jain.protocol.ip.mgcp.message.AuditConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.LocalOptionValue;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.text.ParseException;

import org.apache.log4j.Logger;

import org.mobicents.protocols.mgcp.handlers.MgcpContentHandler;
import org.mobicents.protocols.mgcp.handlers.TransactionHandler;

import org.mobicents.protocols.mgcp.parser.StringFunctions;
import org.mobicents.protocols.mgcp.parser.SplitDetails;
import org.mobicents.protocols.mgcp.parser.params.InfoCodeHandler;
import org.mobicents.protocols.mgcp.parser.params.ReturnCodeHandler;
import org.mobicents.protocols.mgcp.parser.params.NotifiedEntityHandler;
import org.mobicents.protocols.mgcp.parser.params.ConnectionParmHandler;
import org.mobicents.protocols.mgcp.parser.params.ConnectionModeHandler;
import org.mobicents.protocols.mgcp.parser.params.LocalOptionValueHandler;
import org.mobicents.protocols.mgcp.parser.params.EndpointIdentifierHandler;

import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;

/**
 * 
 * @author amit bhayani
 * @author yulian oifa
 * 
 */
public class AuditConnectionHandler extends TransactionHandler 
{
	public static final byte[] COMMAND_NAME=new byte[] {
		StringFunctions.HIGH_A_BYTE,StringFunctions.HIGH_U_BYTE,
		StringFunctions.HIGH_C_BYTE,StringFunctions.HIGH_X_BYTE
	};
	
	private static final Logger logger = Logger.getLogger(AuditConnectionHandler.class);

	private AuditConnection command;
	private AuditConnectionResponse response;
	private ConnectionIdentifier connectionIdentifier = null;

	private InfoCode[] requestedInfo = null;

	boolean RCfirst = false;

	public AuditConnectionHandler(JainMgcpStackImpl stack) 
	{
		super(stack);
	}

	public AuditConnectionHandler(JainMgcpStackImpl stack, InetAddress address, int port) 
	{
		super(stack, address, port);
	}

	@Override
	public JainMgcpCommandEvent decodeCommand(byte[] data,SplitDetails[] message) throws ParseException {
		
		try 
		{
			(new CommandContentHandle()).parse(data,message);
			command = new AuditConnection(source != null ? source : stack, endpoint, connectionIdentifier, requestedInfo);
			command.setTransactionHandle(remoteTID);						
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
		response = new AuditConnectionResponse(source != null ? source : stack, returnCode);
		response.setTransactionHandle(txID);
		
		try 
		{
			(new ResponseContentHandle()).parse(data,msg);			
		} 
		catch (IOException e) 
		{
			logger.error("Parsing of AUCX Response failed ", e);
		}
		
		return response;
	}

	@Override
	public int encode(JainMgcpCommandEvent event,byte[] array) 
	{
		AuditConnection evt = (AuditConnection) event;
		
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
		
		if (evt.getConnectionIdentifier() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_I_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			byte[] connectionIdentifierBytes=evt.getConnectionIdentifier().toString().getBytes();
			System.arraycopy(connectionIdentifierBytes, 0, array,totalLength, connectionIdentifierBytes.length);
			totalLength+=connectionIdentifierBytes.length;			
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}

		InfoCode[] requestedInfos = evt.getRequestedInfo();
		if (requestedInfos != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_F_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=InfoCodeHandler.encodeList(array,totalLength,requestedInfos);
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
			
			int foundRC = 0;
            int foundLC = 0;

            for (int count = 0; count < requestedInfos.length; count++) 
            {
            	InfoCode info = requestedInfos[count];
            	switch (info.getInfoCode()) 
            	{
               		case InfoCode.REMOTE_CONNECTION_DESCRIPTOR:
               			foundRC = count;
                        if (foundLC != 0 && foundLC < count) 
                        	RCfirst = false;
                        else
                        	RCfirst = true;                            
                        break;
               		case InfoCode.LOCAL_CONNECTION_DESCRIPTOR:
               			foundLC = count;
               			if (foundRC != 0 && foundRC < count)
               				RCfirst = true;
               			else
               				RCfirst = false;
               			break;
            	}
            }
		}
		
		return totalLength;
	}

	@Override
	public int encode(JainMgcpResponseEvent event,byte[] array) 
	{
		AuditConnectionResponse response = (AuditConnectionResponse) event;
		ReturnCode returnCode = response.getReturnCode();

		int totalLength=ReturnCodeHandler.encode(array,0,returnCode);
		array[totalLength++]=StringFunctions.SPACE_BYTE;		
		totalLength+=StringFunctions.encodeInt(array,totalLength,response.getTransactionHandle());
		array[totalLength++]=StringFunctions.SPACE_BYTE;				
		byte[] commentBytes=returnCode.getComment().getBytes();
		System.arraycopy(commentBytes, 0, array,totalLength, commentBytes.length);
		totalLength+=commentBytes.length;
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;
				
		if (response.getCallIdentifier() != null) 
		{	
			array[totalLength++]=StringFunctions.HIGH_C_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			byte[] callBytes=response.getCallIdentifier().toString().getBytes();
			System.arraycopy(callBytes, 0, array,totalLength, callBytes.length);
			totalLength+=callBytes.length;
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}

		if (response.getNotifiedEntity() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_N_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=NotifiedEntityHandler.encode(array,totalLength,response.getNotifiedEntity());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (response.getLocalConnectionOptions() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_L_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;			
			totalLength+=LocalOptionValueHandler.encodeList(array,totalLength,response.getLocalConnectionOptions());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (response.getMode() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_M_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=ConnectionModeHandler.encode(array,totalLength,response.getMode());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (response.getConnectionParms() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_P_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=ConnectionParmHandler.encodeList(array,totalLength,response.getConnectionParms());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (RCfirst && response.getRemoteConnectionDescriptor() != null) 
		{
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
			byte[] rcdBytes=response.getRemoteConnectionDescriptor().toString().getBytes();
			System.arraycopy(rcdBytes, 0, array,totalLength, rcdBytes.length);
			totalLength+=rcdBytes.length;			
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (response.getLocalConnectionDescriptor() != null) 
		{
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
			byte[] lcdBytes=response.getLocalConnectionDescriptor().toString().getBytes();
			System.arraycopy(lcdBytes, 0, array,totalLength, lcdBytes.length);
			totalLength+=lcdBytes.length;						
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}

		if (!RCfirst && response.getRemoteConnectionDescriptor() != null) 
		{
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
			byte[] rcdBytes=response.getRemoteConnectionDescriptor().toString().getBytes();
			System.arraycopy(rcdBytes, 0, array,totalLength, rcdBytes.length);
			totalLength+=rcdBytes.length;			
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		return totalLength;
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() 
	{
		AuditConnectionResponse provisionalResponse = null;

		if (!sent) 
		{
			provisionalResponse = new AuditConnectionResponse(source != null ? source : stack,ReturnCode.Transaction_Being_Executed);
			provisionalResponse.setTransactionHandle(remoteTID);
		}

		return provisionalResponse;
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
		 *            the name of the parameter
		 * @param value
		 *            the value of the parameter.
		 */
		public void param(byte[] data,SplitDetails name, SplitDetails value) throws ParseException 
		{
			if(name.getLength()!=1)
				logger.warn("Unidentified AUCX Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
			else
			{
				switch(data[name.getOffset()])
				{
					case StringFunctions.LOW_I_BYTE:
					case StringFunctions.HIGH_I_BYTE:
						connectionIdentifier = new ConnectionIdentifier(new String(data,value.getOffset(),value.getLength()));
						break;
					case StringFunctions.LOW_F_BYTE:
					case StringFunctions.HIGH_F_BYTE:
						boolean hasLC=false;
						requestedInfo = InfoCodeHandler.decodeList(data,value.getOffset(),value.getLength());
						for(int i=0;i<requestedInfo.length;i++)
						{
							if(requestedInfo[i].getInfoCode()==InfoCode.REMOTE_CONNECTION_DESCRIPTOR)
							{
								if(!hasLC)
									RCfirst = true;		
								
								break;
							}
							else if(requestedInfo[i].getInfoCode()==InfoCode.LOCAL_CONNECTION_DESCRIPTOR)
								hasLC=true;
						}
						
						break;
					default:
						logger.warn("Unidentified AUCX Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
						break;
				}
			}			
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
			throw new ParseException("SessionDescription shouldn't have been included in AUCX command", 0);
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
			if(name.getLength()!=1)
				logger.warn("Unidentified AUCX Response parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
			else
			{
				switch(data[name.getOffset()])
				{
					case StringFunctions.LOW_C_BYTE:
					case StringFunctions.HIGH_C_BYTE:
						response.setCallIdentifier(new CallIdentifier(new String(data,value.getOffset(),value.getLength())));		
						break;
					case StringFunctions.LOW_N_BYTE:
					case StringFunctions.HIGH_N_BYTE:
						NotifiedEntity n = NotifiedEntityHandler.decode(data,value.getOffset(),value.getLength(), true);
						response.setNotifiedEntity(n);
						break;
					case StringFunctions.LOW_L_BYTE:
					case StringFunctions.HIGH_L_BYTE:
						LocalOptionValue[] LocalOptionValueList = LocalOptionValueHandler.decodeList(data,value.getOffset(),value.getLength());
						response.setLocalConnectionOptions(LocalOptionValueList);		
						break;
					case StringFunctions.LOW_M_BYTE:
					case StringFunctions.HIGH_M_BYTE:
						ConnectionMode connectionMode = ConnectionModeHandler.decode(data,value.getOffset(),value.getLength());
						response.setMode(connectionMode);
						break;
					case StringFunctions.LOW_P_BYTE:
					case StringFunctions.HIGH_P_BYTE:
						ConnectionParm[] connectionParms = ConnectionParmHandler.decodeList(data,value.getOffset(),value.getLength());
						response.setConnectionParms(connectionParms);
						break;
					default:
						logger.warn("Unidentified AUCX Response parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
						break;
				}
			}
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

			StringReader stringReader = new StringReader(sd);
			BufferedReader reader = new BufferedReader(stringReader);

			String line = null;
			boolean sdpPresent = false;
			StringBuffer sdp1 = new StringBuffer();
			StringBuffer sdp2 = new StringBuffer();
			try {
				while ((line = reader.readLine()) != null) 
				{
					line = line.trim();
					sdpPresent = line.length() == 0;
					
					if (sdpPresent)
						break;
					
					sdp1.append(line.trim()).append(SDP_NEW_LINE);				
				}

				while ((line = reader.readLine()) != null) 
				{
					line = line.trim();
					sdp2.append(line.trim()).append(SDP_NEW_LINE);					
				}

			} 
			catch (IOException e) 
			{
				logger.error("Error while reading the SDP for AUCX Response and decoding to AUCX command ", e);
			}

			if (RCfirst) 
			{
				response.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdp1.toString()));				
				if (sdp2.length()!=0) 
					response.setLocalConnectionDescriptor(new ConnectionDescriptor(sdp2.toString()));				
			} 
			else 
			{
				response.setLocalConnectionDescriptor(new ConnectionDescriptor(sdp1.toString()));
				if (sdp2.length()!=0) 
					response.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdp2.toString()));				
			}
		}
	}
}