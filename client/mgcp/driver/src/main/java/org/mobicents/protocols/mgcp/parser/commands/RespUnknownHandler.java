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

import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.text.ParseException;

import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;

import org.mobicents.protocols.mgcp.handlers.TransactionHandler;
import org.mobicents.protocols.mgcp.parser.SplitDetails;


public class RespUnknownHandler extends TransactionHandler {

	public RespUnknownHandler(JainMgcpStackImpl stack) {
		super(stack);
	}
	
	@Override
	public JainMgcpCommandEvent decodeCommand(byte[] data,SplitDetails[] message) throws ParseException 
	{
		return null;
	}

	@Override
	public JainMgcpResponseEvent decodeResponse(byte[] data,SplitDetails[] msg,Integer txID,ReturnCode returnCode) throws ParseException 
	{
		return null;
	}

	@Override
	public int encode(JainMgcpCommandEvent event,byte[] array) {
		return 0;
	}

	@Override
	public int encode(JainMgcpResponseEvent event,byte[] array) {
		return 0;
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() {
		// TODO Auto-generated method stub
		return null;
	}

}
