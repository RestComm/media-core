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

package org.mobicents.media.server.io.ss7;

import java.io.IOException;
import org.mobicents.protocols.stream.api.SelectorKey;
import org.mobicents.media.hardware.dahdi.Channel;
import org.mobicents.media.hardware.dahdi.SelectorKeyImpl;
import org.mobicents.media.hardware.dahdi.Selector;
import java.util.Iterator;
import javolution.util.FastList;
import org.apache.log4j.Logger;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;

/**
 * Implements schedulable IO over UDP
 *
 * Important! Any CPU-bound action here are illegal!
 *
 * @author yulian oifa
 */
public class SS7Manager {

    /** Channel selector */
    private Selector selector;

     //poll task
    private PollTask pollTask;

    //state flag
    private volatile boolean isActive;

    private volatile int count;

    //name of the interface
    private String name = "unknown";
    
    //logger instance
    private final static Logger logger = Logger.getLogger(SS7Manager.class);
    private final Object LOCK = new Object();
    
    protected Scheduler scheduler;
    /**
     * Creates UDP periphery.
     * 
     * @param name the name of the interface.
     * @scheduler the job scheduler instance.
     * @throws IOException
     */
    public SS7Manager(Scheduler scheduler) throws IOException {
    	this.scheduler=scheduler;
        this.selector = new Selector();
        pollTask = new PollTask(scheduler);
    }

    public int getCount() {
        return count;
    }
    
    /**
     * Opens and binds new datagram channel.
     *
     * @param handler the packet handler implementation
     * @param  port the port to bind to
     * @return datagram channel
     * @throws IOException
     */
    public Channel open(int channelID) throws IOException {
        Channel channel=new Channel();
        channel.setChannelID(channelID);        
        return channel;
    }

    /**
     * Binds socket to global bind address and specified port.
     *
     * @param channel the channel
     * @param port the port to bind to
     * @throws SocketException
     */
    public SelectorKeyImpl bind(Channel channel,ProtocolHandler protocolHandler) {
    	channel.open();
    	SelectorKeyImpl selectorKey=(SelectorKeyImpl)selector.register(channel);
        selectorKey.attach(protocolHandler);
        return selectorKey;
    }

    public void unbind(Channel channel) {
    	channel.close();
    	selector.unregister(channel);
    }
    /**
     * Starts polling the network.
     */
    public void start() {
    	synchronized(LOCK) {
    		if (this.isActive) return;

    		this.isActive = true;
    		this.pollTask.startNow();
        
    		logger.info(String.format("Initialized SS7 interface[%s]", name));
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
            return scheduler.RECEIVER_QUEUE;
        }       

        @Override
        public long perform() {
            //force stop
            if (!isActive) return 0;

            //select channels ready for IO and ignore error
            try {
            	FastList<SelectorKey> it=selector.selectNow(Selector.READ,1);
            	for (FastList.Node<SelectorKey> n = it.head(), end = it
                        .tail(); (n = n.getNext()) != end;) {
            		SelectorKeyImpl key = (SelectorKeyImpl) n.getValue();
            		((ProtocolHandler)key.attachment()).receive((Channel)key.getStream());
                }            	            	
                
            } catch (IOException e) {              	
                return 0;
            } finally {
                scheduler.submit(this,scheduler.RECEIVER_QUEUE);
            }

            return 0;
        }

        /**
         * Immediately start current task
         */
        public void startNow() {
            scheduler.submit(this,scheduler.RECEIVER_QUEUE);
        }
    }
}
