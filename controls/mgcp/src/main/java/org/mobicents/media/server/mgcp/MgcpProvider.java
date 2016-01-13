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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.mobicents.media.server.concurrent.ConcurrentCyclicFIFO;
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
public class MgcpProvider implements MgcpHandlerListener {

    private final static Logger logger = Logger.getLogger(MgcpProvider.class);

    // MGCP Transport
    private final MgcpServer server;
    private SelectionKey selectionKey;
    private DatagramChannel channel;
    private final String address;
    private final int port;

    // Transport Buffers
    private final ConcurrentCyclicFIFO<ByteBuffer> txBuffer;

    // Event Pool
    private final ConcurrentCyclicFIFO<MgcpEventImpl> events;

    // Event Listeners
    private final Listeners<MgcpListener> listeners;

    /**
     * Creates new provider instance.
     * 
     * @param port port number to bind
     * @throws IOException
     */
    public MgcpProvider(String address, int port) {
        // MGCP Transport
        this.server = new MgcpServer(new MgcpHandler(this));
        this.address = address;
        this.port = port;

        // Transport Buffers
        this.txBuffer = new ConcurrentCyclicFIFO<ByteBuffer>();
        for (int i = 0; i < 100; i++) {
            this.txBuffer.offer(ByteBuffer.allocate(8192));
        }

        // Event Pool
        this.events = new ConcurrentCyclicFIFO<MgcpEventImpl>();
        for (int i = 0; i < 100; i++) {
            this.events.offer(new MgcpEventImpl(this));
        }

        // Event Listeners
        this.listeners = new Listeners<MgcpListener>();
    }

    /**
     * Creates new event object.
     * 
     * @param eventID the event identifier: REQUEST or RESPONSE
     * @return event object.
     */
    public MgcpEvent createEvent(int eventID, SocketAddress address) {
        MgcpEventImpl evt = this.events.poll();
        if (evt == null) {
            evt = new MgcpEventImpl(this);
        }

        evt.setInQueue(false);
        evt.setEventID(eventID);
        evt.setAddress(address);
        return evt;
    }

    private void recycleEvent(MgcpEventImpl event) {
        if (event.isInQueue()) {
            logger.warn("====================== ALARM ALARM ALARM==============");
        } else {
            event.setInQueue(true);
            event.cleanMessage();
            this.events.offer(event);
        }
    }

    /**
     * Sends message.
     * 
     * @param message the message to send.
     * @param destination the IP address of the destination.
     */
    public void send(MgcpEvent event, SocketAddress destination) throws IOException {
        MgcpMessage msg = event.getMessage();
        ByteBuffer currBuffer = txBuffer.poll();
        if (currBuffer == null) {
            currBuffer = ByteBuffer.allocate(8192);
        }
        msg.write(currBuffer);

        channel.send(currBuffer, destination);

        currBuffer.clear();
        txBuffer.offer(currBuffer);
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
        if (currBuffer == null) {
            currBuffer = ByteBuffer.allocate(8192);
        }

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
        logger.info("Opening MGCP channel");
        try {
            this.server.start();
            this.selectionKey = this.server.registerChannel(this.address, this.port);
            if (selectionKey == null) {
                logger.warn("Could not open UDP channel on " + this.address + ":" + this.port);
            } else {
                this.channel = (DatagramChannel) selectionKey.channel();
            }
        } catch (IOException e) {
            logger.error("Could not start MGCP server in elegant manner", e);
        }
    }

    public void shutdown() {
        // cancel selection key
        this.selectionKey.cancel();
        
        // close channel
        try {
            this.channel.close();
        } catch (IOException e) {
            logger.error("Could not close MGCP channel in elegant manner", e);
        }
        
        // stop mgcp server
        try {
            this.server.stop();
        } catch (IOException e) {
            logger.error("Could not shutdown MGCP server in elegant manner", e);
        }
    }

    @Override
    public void onMgcpEventReceived(MgcpEvent event) {
        // deliver event to listeners
        if (logger.isDebugEnabled()) {
            logger.debug("Dispatching message");
        }
        listeners.dispatch(event);
    }

    public class MgcpEventImpl implements MgcpEvent {
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

        public void cleanMessage() {
            this.request.clean();
            this.response.clean();
        }

        @Override
        public int getEventID() {
            return eventID;
        }

        protected void setEventID(int eventID) {
            this.eventID = eventID;
        }

        public boolean isInQueue() {
            return this.inQueue.get();
        }

        public void setInQueue(boolean inQueue) {
            this.inQueue.set(inQueue);
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
