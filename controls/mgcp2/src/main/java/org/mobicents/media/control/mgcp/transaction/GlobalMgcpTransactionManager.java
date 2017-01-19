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

package org.mobicents.media.control.mgcp.transaction;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.mobicents.media.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GlobalMgcpTransactionManager implements TransactionManager {

    private final ConcurrentHashMap<String, TransactionManager> managers;

    public GlobalMgcpTransactionManager(MgcpTransactionNumberspace numberspace) {
        this.managers = new ConcurrentHashMap<>();
    }

    @Override
    public void observe(MgcpMessageObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void forget(MgcpMessageObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void notify(Object originator, InetSocketAddress from, InetSocketAddress to, MgcpMessage message,
            MessageDirection direction) {
        // TODO Auto-generated method stub

    }

    @Override
    public void process(InetSocketAddress from, InetSocketAddress to, MgcpRequest request, MgcpCommand command, MessageDirection direction) throws DuplicateMgcpTransactionException {
        final String key = from.toString();

        TransactionManager manager = this.managers.get(key);
        if (manager == null) {
            // TODO manager = this.managers.putIfAbsent(key, provider.provide());
        }
        
        manager.process(from, to, request, command, direction);
    }

    @Override
    public void process(InetSocketAddress from, InetSocketAddress to, MgcpResponse response, MessageDirection direction) throws MgcpTransactionNotFoundException {
        // TODO Auto-generated method stub

    }

}
