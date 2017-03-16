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

package org.restcomm.media.control.mgcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageParser;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;
import org.restcomm.media.control.mgcp.network.MgcpChannel;
import org.restcomm.media.control.mgcp.network.MgcpPacketHandler;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.network.deprecated.channel.RestrictedNetworkGuard;
import org.restcomm.media.spi.listener.Listeners;
import org.restcomm.media.spi.listener.TooManyListenersException;

/**
 *
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MgcpProvider {

    private final static Logger log = Logger.getLogger(MgcpProvider.class);

    private final UdpManager transport;
    private final MgcpChannel dataChannel;
    private final MgcpPacketHandler mgcpHandler;
    private final RestrictedNetworkGuard networkGuard;
    private final int port;

    private final ConcurrentLinkedQueue<ByteBuffer> txBuffer = new ConcurrentLinkedQueue<ByteBuffer>();
    private final ConcurrentLinkedQueue<MgcpEventImpl> events = new ConcurrentLinkedQueue<MgcpEventImpl>();
    private final Listeners<MgcpListener> listeners = new Listeners<MgcpListener>();

    /**
     * Creates new provider instance.
     * 
     * @param transport the UDP interface instance.
     * @param port port number to bind
     */
    public MgcpProvider(UdpManager transport, int port) {
        this.transport = transport;
        this.networkGuard = new RestrictedNetworkGuard(transport.getLocalNetwork(), transport.getLocalSubnet());
        this.mgcpHandler = new MgcpPacketHandler(new MgcpMessageParser());
        this.dataChannel = new MgcpChannel(this.networkGuard, this.mgcpHandler);
        this.port = port;

        // prepare event pool
        for (int i = 0; i < 100; i++) {
            events.offer(new MgcpEventImpl(this));
        }

        for (int i = 0; i < 100; i++) {
            txBuffer.offer(ByteBuffer.allocate(8192));
        }
    }

    /**
     * Creates new event object.
     * 
     * @param eventID the event identifier: REQUEST or RESPONSE
     * @return event object.
     */
    public MgcpEvent createEvent(int eventID, SocketAddress address) {
        MgcpEventImpl evt = events.poll();
        if (evt == null) {
            evt = new MgcpEventImpl(this);
        }

        evt.inQueue.set(false);
        evt.setEventID(eventID);
        evt.setAddress(address);
        return evt;
    }

    /**
     * Sends message.
     * 
     * @param message the message to send.
     */
    public void send(MgcpEvent event) throws IOException {
        MgcpMessage msg = event.getMessage();
        ByteBuffer currBuffer = txBuffer.poll();
        if (currBuffer == null) {
            currBuffer = ByteBuffer.allocate(8192);
        }

        msg.write(currBuffer);
        this.dataChannel.send(currBuffer, (InetSocketAddress) event.getAddress());

        currBuffer.clear();
        txBuffer.offer(currBuffer);
    }

    /**
     * Registers new even listener.
     * 
     * @param listener the listener instance to be registered.
     * @throws TooManyListenersException
     */
    public void addListener(MgcpListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    /**
     * Unregisters event listener.
     * 
     * @param listener the event listener instance to be unregistered.
     */
    public void removeListener(MgcpListener listener) {
        listeners.remove(listener);
    }

    public void activate() {
        try {
            // Open channel
            this.dataChannel.open();

            // Bind channel to local address
            final InetSocketAddress address = new InetSocketAddress(this.transport.getLocalBindAddress(), this.port);
            this.dataChannel.bind(address);
            
            // Register channel in Network Manager to handle read operations
            this.transport.register(this.dataChannel);
            
            if (log.isInfoEnabled()) {
                log.info("Opened MGCP channel at " + address.getAddress().toString());
            }
        } catch (Exception e) {
            log.error("Could not open MGCP channel properly.", e);
            this.dataChannel.close();
        }
    }

    public void shutdown() {
        if (log.isInfoEnabled()) {
            log.info("Closing the MGCP channel.");
        }
        this.dataChannel.close();
    }

    private void recycleEvent(MgcpEventImpl event) {
        if (event.inQueue.getAndSet(true))
            log.warn("====================== ALARM ALARM ALARM==============");
        else {
            event.response.clean();
            event.request.clean();
            events.offer(event);
        }
    }
    
    void onIncomingEvent(MgcpEvent event) {
        listeners.dispatch(event);
    }

    /**
     * MGCP event object implementation.
     */
    private class MgcpEventImpl implements MgcpEvent {

        // provides instance
        private MgcpProvider provider;

        // event type
        private int eventID;

        // patterns for messages: request and response
        private MgcpRequest request = new MgcpRequest();
        private MgcpResponse response = new MgcpResponse();

        // the source address
        private SocketAddress address;

        private AtomicBoolean inQueue = new AtomicBoolean(true);

        /**
         * Creates new event object.
         * 
         * @param provider the MGCP provider instance.
         */
        public MgcpEventImpl(MgcpProvider provider) {
            this.provider = provider;
        }

        @Override
        public MgcpProvider getSource() {
            return provider;
        }

        @Override
        public MgcpMessage getMessage() {
            return eventID == MgcpEvent.REQUEST ? request : response;
        }

        @Override
        public int getEventID() {
            return eventID;
        }

        /**
         * Modifies event type to this event objects.
         * 
         * @param eventID the event type constant.
         */
        protected void setEventID(int eventID) {
            this.eventID = eventID;
        }

        @Override
        public void recycle() {
            recycleEvent(this);
        }

        @Override
        public SocketAddress getAddress() {
            return address;
        }

        /**
         * Modify source address of the message.
         * 
         * @param address the socket address as an object.
         */
        protected void setAddress(SocketAddress address) {
            InetSocketAddress a = (InetSocketAddress) address;
            this.address = new InetSocketAddress(a.getAddress().getHostAddress(), a.getPort());
        }
    }
}
