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
 * File Name     : MessageHandler.java
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
package org.mobicents.protocols.mgcp.handlers;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.text.ParseException;

import org.apache.log4j.Logger;
import org.mobicents.protocols.mgcp.utils.PacketRepresentation;

import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;

import org.mobicents.protocols.mgcp.parser.StringFunctions;
import org.mobicents.protocols.mgcp.parser.SplitDetails;
import org.mobicents.protocols.mgcp.parser.params.ReturnCodeHandler;
import org.mobicents.protocols.mgcp.parser.commands.AuditConnectionHandler;
import org.mobicents.protocols.mgcp.parser.commands.AuditEndpointHandler;
import org.mobicents.protocols.mgcp.parser.commands.CreateConnectionHandler;
import org.mobicents.protocols.mgcp.parser.commands.DeleteConnectionHandler;
import org.mobicents.protocols.mgcp.parser.commands.EndpointConfigurationHandler;
import org.mobicents.protocols.mgcp.parser.commands.ModifyConnectionHandler;
import org.mobicents.protocols.mgcp.parser.commands.NotifyHandler;
import org.mobicents.protocols.mgcp.parser.commands.NotificationRequestHandler;
import org.mobicents.protocols.mgcp.parser.commands.RestartInProgressHandler;
import org.mobicents.protocols.mgcp.parser.params.EndpointIdentifierHandler;

import jain.protocol.ip.mgcp.message.parms.ReturnCode;

/**
 * 
 * @author Oleg Kulikov
 * @author Yulian Oifa 
 */
public class MessageHandler 
{

	private JainMgcpStackImpl stack;
	private static Logger logger = Logger.getLogger(MessageHandler.class);

	public MessageHandler(JainMgcpStackImpl jainMgcpStackImpl) 
	{
		this.stack = jainMgcpStackImpl;		
	}	

	public boolean isRequest(byte headerByte) 
	{
		if (headerByte>=StringFunctions.ZERO_BYTE && headerByte<=StringFunctions.NINE_BYTE)
			return false;
		
		return true;
	}

	public void scheduleMessages(PacketRepresentation pr) throws ParseException
	{
		Integer remoteTxIdIntegere = new Integer(0);
		
		try 
		{
			final InetAddress address = pr.getRemoteAddress();
			final int port = pr.getRemotePort();
			byte[] data=pr.getRawData();
			ArrayList<SplitDetails[]> result=StringFunctions.splitLinesWithTrim(data,0,pr.getLength());
			for (int i=0;i<result.size();i++) 
			{
				SplitDetails[] message=result.get(i);
				SplitDetails[] tokens = StringFunctions.split(data,message[0].getOffset(),message[0].getLength(),StringFunctions.SPACE_BYTE);
				int currIndex=tokens[1].getOffset();
				for(int j=0;j<tokens[1].getLength();j++,currIndex++)
				{
					if(data[currIndex]>=StringFunctions.ZERO_BYTE && data[currIndex]<=StringFunctions.NINE_BYTE)
						remoteTxIdIntegere=remoteTxIdIntegere*10+(data[currIndex]-StringFunctions.ZERO_BYTE);
					else 
						throw new ParseException("Invalid tx:" + new String(data,tokens[1].getOffset(),tokens[1].getLength()), 0);
				}
				
				if (isRequest(data[message[0].getOffset()])) 
				{
					final EndpointIdentifier endpoint = EndpointIdentifierHandler.decode(data,tokens[2].getOffset(),tokens[2].getLength());					
					
					if (logger.isDebugEnabled()) 
						logger.debug("Processing command message = " + new String(data,tokens[0].getOffset(),tokens[0].getLength()) + " remote Tx = " + remoteTxIdIntegere);
					
					
					TransactionHandler completedTxHandler = stack.getCompletedTransactions().get(remoteTxIdIntegere);
					if (completedTxHandler != null) 
					{
						completedTxHandler.markRetransmision();
						completedTxHandler.send();
						
						if (logger.isDebugEnabled()) 
							logger.debug("Received Command for which stack has already sent response Tx = " + new String(data,tokens[0].getOffset(),tokens[0].getLength()) + " " + remoteTxIdIntegere);
						
						return;
					}

					Integer tmpLoaclTID = stack.getRemoteTxToLocalTxMap().get(remoteTxIdIntegere);
					if (tmpLoaclTID != null) 
					{
						TransactionHandler ongoingTxHandler = stack.getLocalTransactions().get(tmpLoaclTID);
						ongoingTxHandler.sendProvisionalResponse();
						if (logger.isDebugEnabled()) 
							logger.debug("Received Command for ongoing Tx = " + remoteTxIdIntegere);
						
						return;
					}

					TransactionHandler handler=null;
					boolean found=true;
					if(tokens[0].getLength()==4)
					{
						switch(data[tokens[0].getOffset()])
						{
							case StringFunctions.LOW_C_BYTE:
							case StringFunctions.HIGH_C_BYTE:
								for(int k=1;k<4;k++)
									if(data[tokens[0].getOffset()+k]!=CreateConnectionHandler.COMMAND_NAME[k] && data[tokens[0].getOffset()+k]!=(byte)(CreateConnectionHandler.COMMAND_NAME[k]-StringFunctions.CASE_STEP))
									{
										found=false;
										break;
									}
								
								if (found) 
									handler = new CreateConnectionHandler(stack, address, port);
								break;
							case StringFunctions.LOW_M_BYTE:
							case StringFunctions.HIGH_M_BYTE:
								for(int k=1;k<4;k++)
									if(data[tokens[0].getOffset()+k]!=ModifyConnectionHandler.COMMAND_NAME[k] && data[tokens[0].getOffset()+k]!=(byte)(ModifyConnectionHandler.COMMAND_NAME[k]-StringFunctions.CASE_STEP))
									{
										found=false;
										break;
									}
								
								if (found) 
									handler = new ModifyConnectionHandler(stack, address, port);
								break;
							case StringFunctions.LOW_D_BYTE:
							case StringFunctions.HIGH_D_BYTE:
								for(int k=1;k<4;k++)
									if(data[tokens[0].getOffset()+k]!=DeleteConnectionHandler.COMMAND_NAME[k] && data[tokens[0].getOffset()+k]!=(byte)(DeleteConnectionHandler.COMMAND_NAME[k]-StringFunctions.CASE_STEP))
									{
										found=false;
										break;
									}
								
								if (found) 
									handler = new DeleteConnectionHandler(stack, address, port);
								break;
							case StringFunctions.LOW_E_BYTE:
							case StringFunctions.HIGH_E_BYTE:
								for(int k=1;k<4;k++)
									if(data[tokens[0].getOffset()+k]!=EndpointConfigurationHandler.COMMAND_NAME[k] && data[tokens[0].getOffset()+k]!=(byte)(EndpointConfigurationHandler.COMMAND_NAME[k]-StringFunctions.CASE_STEP))
									{
										found=false;
										break;
									}
								
								if (found)
									handler = new EndpointConfigurationHandler(stack, address, port);
								break;
							case StringFunctions.LOW_N_BYTE:
							case StringFunctions.HIGH_N_BYTE:
								for(int k=1;k<4;k++)
									if(data[tokens[0].getOffset()+k]!=NotifyHandler.COMMAND_NAME[k] && data[tokens[0].getOffset()+k]!=(byte)(NotifyHandler.COMMAND_NAME[k]-StringFunctions.CASE_STEP))
									{
										found=false;
										break;
									}
								
								if (found)
									handler = new NotifyHandler(stack, address, port);
								break;
							case StringFunctions.LOW_R_BYTE:
							case StringFunctions.HIGH_R_BYTE:
								switch(data[tokens[0].getOffset()+1])
								{
									case StringFunctions.LOW_Q_BYTE:
									case StringFunctions.HIGH_Q_BYTE:
										for(int k=2;k<4;k++)
											if(data[tokens[0].getOffset()+k]!=NotificationRequestHandler.COMMAND_NAME[k] && data[tokens[0].getOffset()+k]!=(byte)(NotificationRequestHandler.COMMAND_NAME[k]-StringFunctions.CASE_STEP))
											{
												found=false;
												break;
											}
										
										if (found)
											handler = new NotificationRequestHandler(stack, address, port);
										break;
									case StringFunctions.LOW_S_BYTE:
									case StringFunctions.HIGH_S_BYTE:
										for(int k=2;k<4;k++)
											if(data[tokens[0].getOffset()+k]!=RestartInProgressHandler.COMMAND_NAME[k] && data[tokens[0].getOffset()+k]!=(byte)(RestartInProgressHandler.COMMAND_NAME[k]-StringFunctions.CASE_STEP))
											{
												found=false;
												break;
											}
										
										if (found)
											handler = new RestartInProgressHandler(stack, address, port);
										break;
								}
								break;
							case StringFunctions.LOW_A_BYTE:
							case StringFunctions.HIGH_A_BYTE:
								switch(data[tokens[0].getOffset()+3])
								{
									case StringFunctions.LOW_P_BYTE:
									case StringFunctions.HIGH_P_BYTE:
										for(int k=1;k<3;k++)
											if(data[tokens[0].getOffset()+k]!=AuditEndpointHandler.COMMAND_NAME[k] && data[tokens[0].getOffset()+k]!=(byte)(AuditEndpointHandler.COMMAND_NAME[k]-StringFunctions.CASE_STEP))
											{
												found=false;
												break;
											}
										
										if (found)
											handler = new AuditEndpointHandler(stack, address, port);
										break;
									case StringFunctions.LOW_X_BYTE:
									case StringFunctions.HIGH_X_BYTE:
										for(int k=1;k<3;k++)
											if(data[tokens[0].getOffset()+k]!=AuditConnectionHandler.COMMAND_NAME[k] && data[tokens[0].getOffset()+k]!=(byte)(AuditConnectionHandler.COMMAND_NAME[k]-StringFunctions.CASE_STEP))
											{
												found=false;
												break;
											}
										
										if (found)
											handler = new AuditConnectionHandler(stack, address, port);
										break;
								}
								break;								
						}
					}
					
					if(handler==null) 
					{
						logger.warn("Unsupported message verbose " + new String(data,tokens[0].getOffset(),tokens[0].getLength()));
						return;
					}

					handler.receiveRequest(endpoint, data, message, remoteTxIdIntegere);
				} 
				else 
				{
					if (logger.isDebugEnabled())
						logger.debug("Processing response message " + new String(data,message[0].getOffset(),message[0].getLength()));					

					TransactionHandler handler = (TransactionHandler) stack.getLocalTransactions().get(remoteTxIdIntegere);
					if (handler == null) 
					{
						logger.warn("---  Address:" + address + "\nPort:" + port + "\nID:" + this.hashCode() + "\n Unknown transaction: " + remoteTxIdIntegere);
						return;
					}
										
					ReturnCode returnCode = ReturnCodeHandler.decode(data,tokens[0].getOffset(),tokens[0].getLength());
					handler.receiveResponse(data, message , remoteTxIdIntegere, returnCode);
				}
			}
		} 
		finally 
		{
			pr.setEndTime(System.currentTimeMillis());
			if(logger.isDebugEnabled())
				logger.debug("Tx ID:" + remoteTxIdIntegere + ",Received message time:" + pr.getReceiveTime() + ",parse start time:" + pr.getParseTime() + ",end time:" + pr.getEndTime());

			pr.release();
		}
	}
}