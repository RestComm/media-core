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
import jain.protocol.ip.mgcp.message.AuditEndpoint;
import jain.protocol.ip.mgcp.message.AuditEndpointResponse;
import jain.protocol.ip.mgcp.message.parms.BearerInformation;
import jain.protocol.ip.mgcp.message.parms.CapabilityValue;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.DigitMap;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.ReasonCode;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.RestartMethod;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

import org.mobicents.protocols.mgcp.handlers.MgcpContentHandler;
import org.mobicents.protocols.mgcp.handlers.TransactionHandler;

import org.mobicents.protocols.mgcp.parser.StringFunctions;
import org.mobicents.protocols.mgcp.parser.SplitDetails;
import org.mobicents.protocols.mgcp.parser.params.InfoCodeHandler;
import org.mobicents.protocols.mgcp.parser.params.ReturnCodeHandler;
import org.mobicents.protocols.mgcp.parser.params.EndpointIdentifierHandler;
import org.mobicents.protocols.mgcp.parser.params.NotifiedEntityHandler;
import org.mobicents.protocols.mgcp.parser.params.RestartMethodHandler;
import org.mobicents.protocols.mgcp.parser.params.EventNameHandler;
import org.mobicents.protocols.mgcp.parser.params.RequestedEventHandler;
import org.mobicents.protocols.mgcp.parser.params.DigitMapHandler;
import org.mobicents.protocols.mgcp.parser.params.ReasonCodeHandler;
import org.mobicents.protocols.mgcp.parser.params.CapabilityHandler;
import org.mobicents.protocols.mgcp.parser.params.BearerInformationHandler;

import jain.protocol.ip.mgcp.message.parms.RestartMethod;


import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;

/**
 * 
 * @author amit bhayani
 * @author yulian oifa
 * 
 */
public class AuditEndpointHandler extends TransactionHandler 
{
	public static final byte[] COMMAND_NAME=new byte[] {
		StringFunctions.HIGH_A_BYTE,StringFunctions.HIGH_U_BYTE,
		StringFunctions.HIGH_E_BYTE,StringFunctions.HIGH_P_BYTE
	};
	
	private static final Logger logger = Logger.getLogger(AuditEndpointHandler.class);

	private AuditEndpoint command;
	private AuditEndpointResponse response;

	public AuditEndpointHandler(JainMgcpStackImpl stack) {
		super(stack);
	}

	public AuditEndpointHandler(JainMgcpStackImpl stack, InetAddress address, int port) {
		super(stack, address, port);
	}

	@Override
	public JainMgcpCommandEvent decodeCommand(byte[] data,SplitDetails[] message) throws ParseException 
	{
		command = new AuditEndpoint(source != null ? source : stack, endpoint);
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
		response = new AuditEndpointResponse(source != null ? source : stack, returnCode);
		response.setTransactionHandle(txID);
		
		try 
		{
			(new ResponseContentHandle()).parse(data,msg);
		} 
		catch (IOException e) 
		{
			logger.error("Decoding of AUEP Response failed", e);
		}

		return response;
	}

	@Override
	public int encode(JainMgcpCommandEvent event,byte[] array) 
	{
		AuditEndpoint evt = (AuditEndpoint) event;
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
		
		InfoCode[] requestedInfos = evt.getRequestedInfo();
		if (requestedInfos != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_F_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			array[totalLength++]=StringFunctions.SPACE_BYTE;
			totalLength+=InfoCodeHandler.encodeList(array,totalLength,requestedInfos);
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}
		
		return totalLength;
	}

	@Override
	public int encode(JainMgcpResponseEvent event,byte[] array) 
	{
		AuditEndpointResponse response = (AuditEndpointResponse) event;
		ReturnCode returnCode = response.getReturnCode();

		int totalLength=ReturnCodeHandler.encode(array,0,returnCode);
		array[totalLength++]=StringFunctions.SPACE_BYTE;		
		totalLength+=StringFunctions.encodeInt(array,totalLength,response.getTransactionHandle());
		array[totalLength++]=StringFunctions.SPACE_BYTE;				
		byte[] commentBytes=returnCode.getComment().getBytes();
		System.arraycopy(commentBytes, 0, array,totalLength, commentBytes.length);
		totalLength+=commentBytes.length;
		array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		
		if (response.getCapabilities() != null) 
		{	
			array[totalLength++]=StringFunctions.HIGH_A_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=CapabilityHandler.encodeList(array,totalLength,response.getCapabilities());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		if (response.getBearerInformation() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_B_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=BearerInformationHandler.encode(array,totalLength,response.getBearerInformation());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}
		
		ConnectionIdentifier[] connectionIdentifiers = response.getConnectionIdentifiers();
		if (connectionIdentifiers != null) 
		{		
			array[totalLength++]=StringFunctions.HIGH_I_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			
			byte[] connectionBytes;
			if(connectionIdentifiers.length>0)
			{				
				int i=0;
				for(;i<connectionIdentifiers.length-1;i++)
				{
					connectionBytes=connectionIdentifiers[i].toString().getBytes();
					System.arraycopy(connectionBytes, 0, array, totalLength, connectionBytes.length);					
					totalLength+=connectionBytes.length;
					array[totalLength++]=StringFunctions.COMMA_BYTE;					
				}
				
				connectionBytes=connectionIdentifiers[i].toString().getBytes();
				System.arraycopy(connectionBytes, 0, array, totalLength, connectionBytes.length);					
				totalLength+=connectionBytes.length;				
			}
			
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;								
		}
		
		if (response.getNotifiedEntity() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_N_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=NotifiedEntityHandler.encode(array,totalLength,response.getNotifiedEntity());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		if (response.getRequestIdentifier() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_X_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			byte[] requestBytes=response.getRequestIdentifier().toString().getBytes();
			System.arraycopy(requestBytes, 0, array,totalLength, requestBytes.length);
			totalLength+=requestBytes.length;			
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}
		
		RequestedEvent[] r = response.getRequestedEvents();
		if (r != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_S_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=RequestedEventHandler.encodeList(array,totalLength,r);
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		EventName[] sEvet = response.getSignalRequests();
		if (sEvet != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_S_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=EventNameHandler.encodeList(array,totalLength,sEvet);
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}
		
		if (response.getDigitMap() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_D_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=DigitMapHandler.encode(array,totalLength,response.getDigitMap());
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		EventName[] o = response.getObservedEvents();
		if (o != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_O_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=EventNameHandler.encodeList(array,totalLength,o);
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}
		
		if (response.getReasonCode() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_E_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=ReasonCodeHandler.encode(array,totalLength,response.getReasonCode());		
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		EventName[] t = response.getDetectEvents();
		if (t != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_T_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=EventNameHandler.encodeList(array,totalLength,t);
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;			
		}
		
		EventName[] es = response.getEventStates();
		if (es != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_E_BYTE;
			array[totalLength++]=StringFunctions.HIGH_S_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=EventNameHandler.encodeList(array,totalLength,es);
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		if (response.getRestartMethod() != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_R_BYTE;
			array[totalLength++]=StringFunctions.HIGH_M_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=RestartMethodHandler.encode(array,totalLength,response.getRestartMethod());		
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		if (response.getRestartDelay() > 0) 
		{
			array[totalLength++]=StringFunctions.HIGH_R_BYTE;
			array[totalLength++]=StringFunctions.HIGH_D_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=StringFunctions.encodeInt(array,totalLength,response.getRestartDelay());		
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		
		EndpointIdentifier[] z = response.getEndpointIdentifierList();
		if (z != null) 
		{
			array[totalLength++]=StringFunctions.HIGH_Z_BYTE;
			array[totalLength++]=StringFunctions.COLON_BYTE;
			totalLength+=EndpointIdentifierHandler.encodeList(array,totalLength,z);
			array[totalLength++]=StringFunctions.NEWLINE_BYTE;
		}
		return totalLength;
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() {
		AuditEndpointResponse provisionalResponse = null;

		if (!sent) {
			provisionalResponse = new AuditEndpointResponse(source != null ? source : stack,
					ReturnCode.Transaction_Being_Executed);
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
		public void param(byte[] data,SplitDetails name,SplitDetails value) throws ParseException 
		{
			if(name.getLength()!=1)
				logger.warn("Unidentified AUEP Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
			else
			{
				switch(data[name.getOffset()])
				{
					case StringFunctions.LOW_F_BYTE:
					case StringFunctions.HIGH_F_BYTE:
						command.setRequestedInfo(InfoCodeHandler.decodeList(data,value.getOffset(),value.getLength()));
						break;
					default:
						logger.warn("Unidentified AUEP Request parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
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
			throw new ParseException("SessionDescription shouldn't have been included in AUEP command", 0);
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
		public void param(byte[] data,SplitDetails name,SplitDetails value) throws ParseException 
		{	
			if(name.getLength()!=1)
			{
				switch(data[name.getOffset()])
				{
					case StringFunctions.LOW_Z_BYTE:
					case StringFunctions.HIGH_Z_BYTE:
						EndpointIdentifier[] endpointIdentifierList = EndpointIdentifierHandler.decodeList(data,value.getOffset(),value.getLength());
						response.setEndpointIdentifierList(endpointIdentifierList);
						break;
					case StringFunctions.LOW_B_BYTE:
					case StringFunctions.HIGH_B_BYTE:
						BearerInformation b = BearerInformationHandler.decode(data,value.getOffset(),value.getLength());
						response.setBearerInformation(b);
						break;
					case StringFunctions.LOW_I_BYTE:
					case StringFunctions.HIGH_I_BYTE:
						ConnectionIdentifier[] is = response.getConnectionIdentifiers();
						if (is == null) 
						{
							ConnectionIdentifier i = new ConnectionIdentifier(new String(data,value.getOffset(),value.getLength()));
							response.setConnectionIdentifiers(new ConnectionIdentifier[] { i });
						} 
						else 
						{
							ArrayList<ConnectionIdentifier> arrayList = new ArrayList<ConnectionIdentifier>();
							Collections.addAll(arrayList, is);
							arrayList.add(new ConnectionIdentifier(new String(data,value.getOffset(),value.getLength())));

							ConnectionIdentifier[] temp = new ConnectionIdentifier[arrayList.size()];
							response.setConnectionIdentifiers(arrayList.toArray(temp));
						}
						break;
					case StringFunctions.LOW_N_BYTE:
					case StringFunctions.HIGH_N_BYTE:
						NotifiedEntity n = NotifiedEntityHandler.decode(data,value.getOffset(),value.getLength(), true);
						response.setNotifiedEntity(n);
						break;
					case StringFunctions.LOW_X_BYTE:
					case StringFunctions.HIGH_X_BYTE:						
						RequestIdentifier ri = new RequestIdentifier(new String(data,value.getOffset(),value.getLength()));
						response.setRequestIdentifier(ri);
						break;
					case StringFunctions.LOW_R_BYTE:
					case StringFunctions.HIGH_R_BYTE:						
						RequestedEvent[] r = RequestedEventHandler.decodeList(data,value.getOffset(),value.getLength());
						response.setRequestedEvents(r);
						break;
					case StringFunctions.LOW_S_BYTE:
					case StringFunctions.HIGH_S_BYTE:						
						EventName[] s = EventNameHandler.decodeList(data,value.getOffset(),value.getLength());
						response.setSignalRequests(s);
						break;
					case StringFunctions.LOW_D_BYTE:
					case StringFunctions.HIGH_D_BYTE:						
						DigitMap d = DigitMapHandler.decode(data,value.getOffset(),value.getLength());
						response.setDigitMap(d);
						break;
					case StringFunctions.LOW_O_BYTE:
					case StringFunctions.HIGH_O_BYTE:						
						EventName[] o = EventNameHandler.decodeList(data,value.getOffset(),value.getLength());
						response.setObservedEvents(o);
						break;
					case StringFunctions.LOW_E_BYTE:
					case StringFunctions.HIGH_E_BYTE:						
						ReasonCode e = ReasonCodeHandler.decode(data,value.getOffset(),value.getLength());
						response.setReasonCode(e);
						break;
					case StringFunctions.LOW_T_BYTE:
					case StringFunctions.HIGH_T_BYTE:						
						EventName[] t = EventNameHandler.decodeList(data,value.getOffset(),value.getLength());
						response.setDetectEvents(t);
						break;
					case StringFunctions.LOW_A_BYTE:
					case StringFunctions.HIGH_A_BYTE:						
						CapabilityValue[] capabilities = response.getCapabilities();
						if (capabilities == null) 
							response.setCapabilities(CapabilityHandler.decodeList(data,value.getOffset(),value.getLength()));
						else 
						{
							CapabilityValue[] newCapability = CapabilityHandler.decodeList(data,value.getOffset(),value.getLength());
							CapabilityValue[] temp = new CapabilityValue[capabilities.length + newCapability.length];
							System.arraycopy(capabilities, 0, temp, 0, capabilities.length);
							System.arraycopy(newCapability, 0, temp, capabilities.length, newCapability.length);							
							response.setCapabilities(temp);
						}
						break;
					default:
						logger.warn("Unidentified AUEP Response parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));
						break;
				}
			}
			else if(name.getLength()==2)
			{
				switch(data[name.getOffset()+1])
				{
					case StringFunctions.LOW_S_BYTE:
					case StringFunctions.HIGH_S_BYTE:						
						if(data[name.getOffset()]==StringFunctions.LOW_E_BYTE || data[name.getOffset()]==StringFunctions.HIGH_E_BYTE)
						{
							EventName[] es = EventNameHandler.decodeList(data,value.getOffset(),value.getLength());
							response.setEventStates(es);
						}
						else							
							logger.warn("Unidentified AUEP Response parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));						
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
							response.setRestartDelay(restartDelay);							
						}
						else							
							logger.warn("Unidentified AUEP Response parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));						
						break;
					case StringFunctions.LOW_M_BYTE:
					case StringFunctions.HIGH_M_BYTE:						
						if(data[name.getOffset()]==StringFunctions.LOW_R_BYTE || data[name.getOffset()]==StringFunctions.HIGH_R_BYTE)
						{
							RestartMethod rm = RestartMethodHandler.decode(data,value.getOffset(),value.getLength());
							response.setRestartMethod(rm);
						}
						else							
							logger.warn("Unidentified AUEP Response parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));												
						break;
					default:
						logger.warn("Unidentified AUEP Response parameter " + new String(data,name.getOffset(),name.getLength()) + " with value = " + new String(data,value.getOffset(),value.getLength()));						
						break;
				}
			}
			else
				logger.warn("Unidentified AUEP Response parameter " + name + " with value = " + value);			
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
			// response.setLocalConnectionDescriptor(new
			// ConnectionDescriptor(sd));
		}
	}

}
