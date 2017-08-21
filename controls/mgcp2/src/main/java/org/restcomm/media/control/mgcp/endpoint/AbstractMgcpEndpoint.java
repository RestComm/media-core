/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.endpoint;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.command.NotificationRequest;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.restcomm.media.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AbstractMgcpEndpoint implements MgcpEndpoint {

    private static final Logger log = Logger.getLogger(AbstractMgcpEndpoint.class);

    private final MgcpEndpointContext context;

    public AbstractMgcpEndpoint(MgcpEndpointContext context) {
        super();
        this.context = context;
    }

    @Override
    public void observe(MgcpEndpointObserver observer) {
        Set<MgcpEndpointObserver> observers = context.getEndpointObservers();
        boolean added = observers.add(observer);
        if (added && log.isTraceEnabled()) {
            EndpointIdentifier endpointId = context.getEndpointId();
            log.trace("Endpoint " + endpointId.toString() + " registered MgcpEndpointObserver@" + observer.hashCode()
                    + ". Count: " + observers.size());
        }
    }

    @Override
    public void forget(MgcpEndpointObserver observer) {
        Set<MgcpEndpointObserver> observers = context.getEndpointObservers();
        boolean removed = observers.remove(observer);
        if (removed && log.isTraceEnabled()) {
            EndpointIdentifier endpointId = context.getEndpointId();
            log.trace("Endpoint " + endpointId.toString() + " unregistered MgcpEndpointObserver@" + observer.hashCode()
                    + ". Count: " + observers.size());
        }
    }

    @Override
    public void notify(MgcpEndpoint endpoint, MgcpEndpointState state) {
        Set<MgcpEndpointObserver> observers = context.getEndpointObservers();
        Iterator<MgcpEndpointObserver> iterator = observers.iterator();

        while (iterator.hasNext()) {
            MgcpEndpointObserver observer = iterator.next();
            observer.onEndpointStateChanged(this, state);
        }
    }

    @Override
    public void observe(MgcpMessageObserver observer) {
        Set<MgcpMessageObserver> observers = this.context.getMessageObservers();
        boolean added = observers.add(observer);
        if (added && log.isTraceEnabled()) {
            EndpointIdentifier endpointId = context.getEndpointId();
            log.trace("Endpoint " + endpointId.toString() + " registered MgcpMessageObserver@" + observer.hashCode()
                    + ". Count: " + observers.size());
        }
    }

    @Override
    public void forget(MgcpMessageObserver observer) {
        Set<MgcpMessageObserver> observers = this.context.getMessageObservers();
        boolean removed = observers.remove(observer);
        if (removed && log.isTraceEnabled()) {
            EndpointIdentifier endpointId = context.getEndpointId();
            log.trace("Endpoint " + endpointId.toString() + " unregistered MgcpMessageObserver@" + observer.hashCode()
                    + ". Count: " + observers.size());
        }
    }

    @Override
    public void notify(Object originator, InetSocketAddress from, InetSocketAddress to, MgcpMessage message,
            MessageDirection direction) {
        Set<MgcpMessageObserver> observers = this.context.getMessageObservers();
        Iterator<MgcpMessageObserver> iterator = observers.iterator();
        while (iterator.hasNext()) {
            MgcpMessageObserver observer = iterator.next();
            if (observer != originator) {
                observer.onMessage(from, to, message, direction);
            }
        }
    }

    @Override
    public void onEvent(Object originator, MgcpEvent event) {
        // TODO Auto-generated method stub

    }

    @Override
    public EndpointIdentifier getEndpointId() {
        return this.context.getEndpointId();
    }

    @Override
    public MgcpConnection getConnection(int callId, int connectionId) {
        ConcurrentHashMap<Integer,MgcpConnection> connections = this.context.getConnections();
        MgcpConnection connection = connections.get(connectionId);
        if(connection != null && connection.getCallIdentifier() == callId) {
            return connection;
        }
        return null;
    }

    @Override
    public void registerConnection(int callId, boolean local, MgcpConnection connection) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MgcpConnection unregisterConnection(int callId, int connectionId)
            throws MgcpCallNotFoundException, MgcpConnectionNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MgcpConnection> unregisterConnections() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MgcpConnection> unregisterConnections(int callId) throws MgcpCallNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void requestNotification(NotificationRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void cancelSignal(String signal) {
        // TODO Auto-generated method stub

    }

    @Override
    public MediaGroup getMediaGroup() {
        // TODO Auto-generated method stub
        return null;
    }

}
