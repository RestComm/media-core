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

package org.mobicents.media.server.io.network;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;

/**
 * Implements schedulable IO over UDP
 *
 * Important! Any CPU-bound action here are illegal!
 *
 * @author kulikov
 */
public class UdpManager {
    private final static int PORT_ANY = -1;

    /** Channel selector */
    private Selector selector;

    /** bind address */
    private String bindAddress = "127.0.0.1";
    
    //port manager
    private PortManager portManager = new PortManager();

    //poll task
    private PollTask pollTask;

    //state flag
    private volatile boolean isActive;

    private volatile int count;

    //name of the interface
    private String name = "unknown";
    
    //logger instance
    private final static Logger logger = Logger.getLogger(UdpManager.class);
    private final Object LOCK = new Object();
    /**
     * Creates UDP periphery.
     * 
     * @param name the name of the interface.
     * @scheduler the job scheduler instance.
     * @throws IOException
     */
    public UdpManager(Scheduler scheduler) throws IOException {
        this.selector = SelectorProvider.provider().openSelector();
        pollTask = new PollTask(scheduler);
    }

    public int getCount() {
        return count;
    }
    /**
     * Modify bind address.
     * 
     * @param address the IP address as character string.
     */
    public void setBindAddress(String address) {
        this.bindAddress = address;
    }

    /**
     * Gets the bind address.
     *
     * @return the IP address as character string.
     */
    public String getBindAddress() {
        return bindAddress;
    }

    /**
     * Modify the low boundary.
     * @param low port number
     */
    public void setLowestPort(int low) {
        portManager.setLowestPort(low);
    }

    /**
     * Gets the low boundary of available range.
     * @return low min port number
     */
    public int getLowestPort() {
        return portManager.getLowestPort();
    }

    /**
     * Modify the upper boundary.
     * @param high port number
     */
    public void setHighestPort(int high) {
        portManager.setHighestPort(high);
    }

    /**
     * Gets the upper boundary of available range.
     * @retun min port number
     */
    public int getHighestPort() {
        return portManager.getLowestPort();
    }

    /**
     * Opens and binds new datagram channel.
     *
     * @param handler the packet handler implementation
     * @param  port the port to bind to
     * @return datagram channel
     * @throws IOException
     */
    public DatagramChannel open(ProtocolHandler handler) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        key.attach(handler);
        handler.setKey(key);
        return channel;
    }

    /**
     * Binds socket to global bind address and specified port.
     *
     * @param channel the channel
     * @param port the port to bind to
     * @throws SocketException
     */
    public void bind(DatagramChannel channel, int port) throws SocketException {
        //select port if wildcarded
        if (port == PORT_ANY) {
            port = portManager.next();
        }
        //try bind
        SocketException ex = null;
        for(int q=0;q<100;q++) {
        	try {
        		channel.socket().bind(new InetSocketAddress(bindAddress, port));
        		port = portManager.next();
        		ex = null;
        		break;
        	} catch (SocketException e) {
        		
        		ex = e;
        		logger.info("Failed trying to bind " + bindAddress + ":" + port);
        		port = portManager.next();
        	}
        }
        if(ex != null) throw ex;
    }

    /**
     * Starts polling the network.
     */
    public void start() {
    	synchronized(LOCK) {
    		if (this.isActive) return;

    		this.isActive = true;
    		this.pollTask.startNow();
        
    		logger.info(String.format("Initialized UDP interface[%s]: bind address=%s", name, bindAddress));
    	}
    }

    /**
     * Stops polling the network.
     */
    public void stop() {
    	synchronized(LOCK) {
    		if (!this.isActive) return;

    		this.isActive = false;        
    		this.pollTask.cancel();
    		logger.info("Stopped");
    	}
    }

    /**
     * Schedulable task for polling UDP channels
     */
    private class PollTask extends Task {

        /**
         * Creates new instance of this task
         * @param scheduler
         */
        public PollTask(Scheduler scheduler) {
            super(scheduler);
        }

        public int getQueueNumber() {
            return scheduler.UDP_MANAGER_QUEUE;
        }       

        @Override
        public long perform() {
            //force stop
            if (!isActive) return 0;

            //select channels ready for IO and ignore error
            try {
                selector.selectNow();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    //get references to channel and associated RTP socket
                    DatagramChannel channel = (DatagramChannel) key.channel();
                    ProtocolHandler handler = (ProtocolHandler) key.attachment();

                    if (handler == null) {
                        continue;
                    }
                    
                    //do read
                    if (key.isReadable()) {
                        handler.receive(channel);
                        count++;
                    }

                    //do write
//                    if (key.isWritable()) {
//                        handler.send(channel);
//                    }

                }
                selector.selectedKeys().clear();
            } catch (IOException e) {
                return 0;
            } finally {
                scheduler.submit(this,scheduler.UDP_MANAGER_QUEUE);
            }

            return 0;
        }

        /**
         * Immediately start current task
         */
        public void startNow() {
            scheduler.submit(this,scheduler.UDP_MANAGER_QUEUE);
        }
    }
}
