/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.restcomm.media.control.mgcp.transaction;

import java.net.InetSocketAddress;

import org.restcomm.media.control.mgcp.command.MgcpCommand;
import org.restcomm.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.restcomm.media.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpMessageSubject;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface MgcpTransactionManager extends MgcpMessageSubject {

    /**
     * Processes a new MGCP Request to be handled in a new transaction.
     * 
     * @param from The sender of the request.
     * @param to The recipient of the request.
     * @param request The MGCP request message.
     * @param command The MGCP command to be executed.
     * @param direction Dictates whether the message is incoming or outgoing.
     * @throws DuplicateMgcpTransactionException In case there is an existing transaction for the same call agent.
     */
    void process(InetSocketAddress from, InetSocketAddress to, MgcpRequest request, MgcpCommand command, MessageDirection direction) throws DuplicateMgcpTransactionException;

    /**
     * Processes an MGCP Response to close an open transaction.
     * 
     * @param from The sender of the request.
     * @param to The recipient of the request.
     * @param response The MGCP response message.
     * @param direction Dictates whether the message is incoming or outgoing.
     * @throws MgcpTransactionNotFoundException In case there is no matching transaction to close.
     */
    void process(InetSocketAddress from, InetSocketAddress to, MgcpResponse response, MessageDirection direction) throws MgcpTransactionNotFoundException;

}
