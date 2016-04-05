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

package org.mobicents.media.server.mgcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.network.ProtocolHandler;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.channel.MultiplexedChannel;
import org.mobicents.media.server.io.network.channel.PacketHandler;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;
import org.mobicents.media.server.mgcp.exception.MgcpDecodeException;
import org.mobicents.media.server.mgcp.message.MgcpMessage;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.MgcpResponse;
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MgcpProvider extends MultiplexedChannel {

    private final static Logger logger = Logger.getLogger(MgcpProvider.class);

    private final UdpManager transport;
    private final MGCPHandler mgcpHandler;
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
        this.mgcpHandler = new MGCPHandler();
        this.port = port;

        // Setup packet handlers
        this.handlers.addHandler(this.mgcpHandler);

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
        if (currBuffer == null)
            currBuffer = ByteBuffer.allocate(8192);

        msg.write(currBuffer);
        channel.send(currBuffer, event.getAddress());

        currBuffer.clear();
        txBuffer.offer(currBuffer);
    }

    /**
     * Sends message.
     * 
     * @param message the message to send.
     * @param destination the IP address of the destination.
     */
    public void send(MgcpMessage message, SocketAddress destination) throws IOException {
        ByteBuffer currBuffer = txBuffer.poll();
        if (currBuffer == null)
            currBuffer = ByteBuffer.allocate(8192);

        message.write(currBuffer);
        channel.send(currBuffer, destination);

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
            if(logger.isInfoEnabled()) {
                logger.info("Opening MGCP channel");
            }
            this.selectionKey = this.transport.open(this);
            this.dataChannel = (DatagramChannel) this.selectionKey.channel();
        } catch (Exception e) {
            logger.error("Could not open MGCP channel", e);
            return;
        }
        
        try {
            if(logger.isInfoEnabled()) {
                logger.info("Binding channel to " + transport.getLocalBindAddress() + ":" + port);
            }
            transport.bindLocal(this.dataChannel, this.port);
        } catch (Exception e) {
            logger.error("Could not bind MGCP channel. Closing the channel.", e);
            close();
        }
    }

    public void shutdown() {
        if(logger.isInfoEnabled()) {
            logger.info("Closing the MGCP channel.");
        }
        close();
    }

    private void recycleEvent(MgcpEventImpl event) {
        if (event.inQueue.getAndSet(true))
            logger.warn("====================== ALARM ALARM ALARM==============");
        else {
            event.response.clean();
            event.request.clean();
            events.offer(event);
        }
    }

    /**
     * MGCP message handler asynchronous implementation.
     */
    private class MGCPHandler implements PacketHandler {

        @Override
        public int compareTo(PacketHandler o) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean canHandle(byte[] packet) {
            return canHandle(packet, packet.length, 0);
        }

        @Override
        public boolean canHandle(byte[] packet, int dataLength, int offset) {
            // TODO [MGCPHandler] Check if packet can be handled
            return true;
        }

        @Override
        public byte[] handle(byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer)
                throws PacketHandlerException {
            return handle(packet, packet.length, 0, localPeer, remotePeer);
        }

        @Override
        public byte[] handle(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer,
                InetSocketAddress remotePeer) throws PacketHandlerException {
            // Create event
            byte b = packet[0];
            int msgType = (b >= 48 && b <= 57) ? MgcpEvent.RESPONSE: MgcpEvent.REQUEST;
            MgcpEvent evt = createEvent(msgType, remotePeer);
            
            // Parse message
            if (logger.isDebugEnabled()) {
                logger.debug("Parsing message: " + new String(packet, offset, dataLength));
            }
            try {
                evt.getMessage().read(packet, offset, dataLength);
            } catch (MgcpDecodeException e) {
                throw new PacketHandlerException(e.getCause());
            }
            
            // Dispatch message
            listeners.dispatch(evt);
            return null;
        }

        @Override
        public int getPipelinePriority() {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    /**
     * MGCP event object implementation.
     * 
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
            this.address = new InetSocketAddress(a.getHostName(), a.getPort());
        }
    }
}
