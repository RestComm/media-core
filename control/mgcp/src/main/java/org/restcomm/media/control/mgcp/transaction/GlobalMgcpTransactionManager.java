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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.control.mgcp.command.MgcpCommand;
import org.restcomm.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.restcomm.media.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;

import com.google.common.collect.Sets;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GlobalMgcpTransactionManager implements MgcpTransactionManager, MgcpMessageObserver {
    
    private static final Logger log = LogManager.getLogger(GlobalMgcpTransactionManager.class);

    private final MgcpTransactionManagerProvider provider;
    private final ConcurrentHashMap<String, MgcpTransactionManager> managers;
    private final Set<MgcpMessageObserver> observers;

    public GlobalMgcpTransactionManager(MgcpTransactionManagerProvider provider) {
        this.provider = provider;
        this.managers = new ConcurrentHashMap<>();
        this.observers = Sets.newConcurrentHashSet();
    }

    @Override
    public void observe(MgcpMessageObserver observer) {
        final boolean added = this.observers.add(observer);
        if(added && log.isTraceEnabled()) {
            if (log.isTraceEnabled()) {
                log.trace("Registered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
            }
        }
    }

    @Override
    public void forget(MgcpMessageObserver observer) {
        final boolean removed = this.observers.remove(observer);
        if(removed && log.isTraceEnabled()) {
            if (log.isTraceEnabled()) {
                log.trace("Unregistered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
            }
        }
    }
    
    @Override
    public void notify(Object originator, InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        Iterator<MgcpMessageObserver> observers = this.observers.iterator();
        while (observers.hasNext()) {
            MgcpMessageObserver observer = observers.next();
            if(observer != originator) {
                observer.onMessage(from, to, message, direction);
            }
        }
    }
    
    @Override
    public void onMessage(InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        // Forward message from sub transaction manager to remaining observers
        notify(this, from, to, message, direction);
    }

    @Override
    public void process(InetSocketAddress from, InetSocketAddress to, MgcpRequest request, MgcpCommand command, MessageDirection direction) throws DuplicateMgcpTransactionException {
        final String callAgent = MessageDirection.INCOMING.equals(direction) ? from.toString() : to.toString();
        MgcpTransactionManager manager = this.managers.get(callAgent);
        if (manager == null) {
            MgcpTransactionManager newManager = provider.provide();
            MgcpTransactionManager oldManager = this.managers.putIfAbsent(callAgent, newManager);
            
            if(oldManager == null) {
                manager = newManager;
                manager.observe(this);
                
                if(log.isDebugEnabled()) {
                    log.debug("Created new transaction manager to handle transactions for call agent " + callAgent);
                }
            } else {
                manager = oldManager;
            }
        }
        manager.process(from, to, request, command, direction);
    }

    @Override
    public void process(InetSocketAddress from, InetSocketAddress to, MgcpResponse response, MessageDirection direction) throws MgcpTransactionNotFoundException {
        final String callAgent = MessageDirection.INCOMING.equals(direction) ? from.toString() : to.toString();
        MgcpTransactionManager manager = this.managers.get(callAgent);
        if (manager == null) {
            MgcpTransactionManager newManager = provider.provide();
            MgcpTransactionManager oldManager = this.managers.putIfAbsent(callAgent, newManager);
            
            if(oldManager == null) {
                manager = newManager;
                manager.observe(this);
                
                if(log.isDebugEnabled()) {
                    log.debug("Created new transaction manager to handle transactions for call agent " + callAgent);
                }
            } else {
                manager = oldManager;
            }
        }
        manager.process(from, to, response, direction);
    }

}
