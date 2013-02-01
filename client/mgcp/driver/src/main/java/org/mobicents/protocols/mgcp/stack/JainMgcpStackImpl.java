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

/*
 * File Name     : JainMgcpStackImpl.java
 *
 * The JAIN MGCP API implementaion.
 *
 * The source code contained in this file is in in the public domain.
 * It can be used in any project or product without prior permission,
 * license or royalty payments. There is  NO WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR STATUTORY, INCLUDING, WITHOUT LIMITATION,
 * THE IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND DATA ACCURACY.  We do not warrant or make any representations
 * regarding the use of the software or the  results thereof, including
 * but not limited to the correctness, accuracy, reliability or
 * usefulness of the software.
 */
package org.mobicents.protocols.mgcp.stack;

import jain.protocol.ip.mgcp.CreateProviderException;
import jain.protocol.ip.mgcp.DeleteProviderException;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpStack;
import jain.protocol.ip.mgcp.OAM_IF;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.mobicents.protocols.mgcp.handlers.MessageHandler;
import org.mobicents.protocols.mgcp.handlers.TransactionHandler;

import org.mobicents.protocols.mgcp.utils.PacketRepresentation;
import org.mobicents.protocols.mgcp.utils.PacketRepresentationFactory;

import org.mobicents.media.server.concurrent.ConcurrentLinkedList;
/**
 * 
 * @author Oleg Kulikov
 * @author Pavel Mitrenko
 * @author Yulian Oifa
 */
public class JainMgcpStackImpl extends Thread implements JainMgcpStack, OAM_IF {

	// Static variables from properties files
	/**
	 * Defines how many executors will work on event delivery
	 */
	public static final String _EXECUTOR_TABLE_SIZE = "executorTableSize";
	/**
	 * Defines how many message can be stored in queue before new ones are discarded.
	 */
	public static final String _EXECUTOR_QUEUE_SIZE = "executorQueueSize";

	private static final Logger logger = Logger.getLogger(JainMgcpStackImpl.class);
	private static final String propertiesFileName = "mgcp-stack.properties";
	private String protocolVersion = "1.0";
	protected int port = 2727;
	private InetAddress localAddress = null;
	private boolean stopped = true;

	private PacketRepresentationFactory prFactory = null;

	// Should we ever get data more than 5000 bytes?
	private static final int BUFFER_SIZE = 5000;

	private DatagramChannel channel;
	// private Selector selector;
	ByteBuffer receiveBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
	ByteBuffer sendBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

	byte[] b = new byte[BUFFER_SIZE];

	// For now we have only one provider/delete prvider method wont work.
	public JainMgcpStackProviderImpl provider = null;

	private InetSocketAddress address = null;
	/**
	 * holds current active transactions (RFC 3435 [$3.2.1.2]: for tx sent & received).
	 * 
	 */
	private ConcurrentHashMap<Integer, TransactionHandler> localTransactions = new ConcurrentHashMap<Integer, TransactionHandler>();
	private ConcurrentHashMap<Integer, Integer> remoteTxToLocalTxMap = new ConcurrentHashMap<Integer, Integer>();

	private ConcurrentHashMap<Integer, TransactionHandler> completedTransactions = new ConcurrentHashMap<Integer, TransactionHandler>();

	private ConcurrentLinkedList<PacketRepresentation> inputQueue=new ConcurrentLinkedList<PacketRepresentation>();	
	
	private DatagramSocket socket;

	private long delay = 4;
	private long threshold = 2;
	
	protected int parserThreadPoolSize = 2;

	private DecodingThread[] decodingThreads;
	
	public void printStats() {
		System.out.println("localTransactions size = " + localTransactions.size());
		System.out.println("remoteTxToLocalTxMap size = " + remoteTxToLocalTxMap.size());
		System.out.println("completedTransactions size = " + completedTransactions.size());
	}

	// Defualt constructor for TCK
	public JainMgcpStackImpl() {
	}

	/** Creates a new instance of JainMgcpStackImpl */
	public JainMgcpStackImpl(InetAddress localAddress, int port) {

		this.localAddress = localAddress;
		this.port = port;

	}

	private void init() throws IOException {
		readProperties();
		// initExecutors();

		if (channel == null) {
			try {
				InetSocketAddress bindAddress = new InetSocketAddress(this.localAddress, this.port);
				this.channel = DatagramChannel.open();

				socket = this.channel.socket();
				socket.bind(bindAddress);

				this.channel.configureBlocking(false);

				this.localAddress = socket.getLocalAddress();
				logger.info("Jain Mgcp stack bound to IP " + this.localAddress + " and UDP port " + this.port);

				// This is for TCK don't remove
				System.out.println("Jain Mgcp stack bound to IP " + this.localAddress + " and UDP port " + this.port);
			} catch (SocketException e) {
				logger.error(e);
				throw new RuntimeException("Failed to find a local port " + this.port + " to bound stack");
			}
		}

		stopped = false;
		if (logger.isDebugEnabled()) {
			logger.debug("Starting main thread " + this);
		}

		this.provider = new JainMgcpStackProviderImpl(this);
		
		this.prFactory = new PacketRepresentationFactory(50, BUFFER_SIZE);

		decodingThreads=new DecodingThread[this.parserThreadPoolSize];
		for(int i=0;i<decodingThreads.length;i++)
			decodingThreads[i]=new DecodingThread(this);
		
		// So stack does not die
		this.setDaemon(false);
		start();
	}

	private void readProperties() {

		try {
			Properties props = new Properties();
			InputStream is = this.getClass().getClassLoader().getResourceAsStream(this.propertiesFileName);
			if (is == null) {
				logger.warn("Failed to locate properties file, using default values");
				return;
			}

			props.load(is);

			String val = null;

			val = props.getProperty(_EXECUTOR_QUEUE_SIZE, "" + this.parserThreadPoolSize);
			this.parserThreadPoolSize = Integer.parseInt(val);			
			val = null;

			logger.info(this.propertiesFileName + " read successfully! \nexecutorQueueSize = "
					+ this.parserThreadPoolSize);

		} catch (Exception e) {
			logger.warn("Failed to read properties file due to some error \"" + e.getMessage() + "\", using defualt values!!!!");
		}
	}

	/**
	 * Closes the stack and it's underlying resources.
	 */
	public void close() {
		stopped = true;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing socket");
			}
			// selector.close();
			socket.close();

			if (this.channel != null) {
				this.channel.close();
			}

		} catch (Exception e) {
			if(logger.isEnabledFor(Level.ERROR))
			{
				logger.error("Could not gracefully close socket", e);
			}
		}
	}

	public JainMgcpProvider createProvider() throws CreateProviderException {
		if (this.provider != null) {
			throw new CreateProviderException(
					"Provider already created. Only 1 provider can be created. Delete the first and then re-create");
		}
		try {
			init();
		} catch (IOException e) {
			if(logger.isEnabledFor(Level.ERROR))
			{
				logger.error("Failed to open Socket ", e);
			}
			throw new CreateProviderException(e.getMessage());
		}
		return this.provider;
	}

	public void deleteProvider(JainMgcpProvider provider) throws DeleteProviderException {
		if (this.provider == null) {
			throw new DeleteProviderException("No Provider exist.");
		}
		if (this.provider != provider) {
			throw new DeleteProviderException("Passed provider is not current one.");
		}
		this.close();
		this.provider = null;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public InetAddress getAddress() {
		if (this.localAddress != null) {
			return this.localAddress;
		} else {
			return null;
		}
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public PacketRepresentation allocatePacket() 
	{
		return this.prFactory.allocate();		
	}
	
	public void releasePacket(PacketRepresentation pr) 
	{
		this.prFactory.deallocate(pr);		
	}
	
	public void send(PacketRepresentation pr) 
	{
		try {						
			this.channel.send(pr.getBuffer(), pr.getInetAddress());											
		} catch (IOException e) {
			if(logger.isEnabledFor(Level.ERROR))
			{							
				logger.error("I/O Exception occured, caused by", e);
			}
		}			
	}	
	
	public boolean isRequest(String header) {
		return header.matches("[\\w]{4}(\\s|\\S)*");
	}

	@Override
	public void run() {
		if (logger.isDebugEnabled()) {
			logger.debug("MGCP stack started successfully on " + this.localAddress + ":" + this.port);
		}
		int length = 0;

		long start = 0;
		long finish = 0;
		long drift = 0;
		long latency = 0;
		for(int i=0;i<this.decodingThreads.length;i++)
			this.decodingThreads[i].activate();
		
		if(this.provider!=null)
			this.provider.start();
			
		start = System.currentTimeMillis();		
		while (!stopped) {
			try {
				PacketRepresentation pr;
				do {
					this.receiveBuffer.clear();
					address = (InetSocketAddress) this.channel.receive(this.receiveBuffer);
					this.receiveBuffer.flip();
					length = this.receiveBuffer.limit();
					
					if (length != 0) {
						pr = this.prFactory.allocate();
						receiveBuffer.get(pr.getRawData(), 0, length);
						pr.setLength(length);						
						pr.setRemoteAddress(address);						
						inputQueue.offer(pr);						
					}
				} while (this.address != null);

				//this is for async send
				this.provider.flush();
				
				finish = System.currentTimeMillis();
				drift = finish - start;
				latency = delay - drift;				
				
				start = finish;
				
				if (drift <= threshold) {
					try {
						Thread.currentThread().sleep(latency);
					} catch (InterruptedException e) {
						return;
					}
				}				
			} catch (IOException e) {
				if (stopped) {
					break;
				}
				if(logger.isEnabledFor(Level.ERROR))
				{
					logger.error("I/O exception occured:", e);
				}
				continue;
			}catch(Exception e)
			{
				//catch everything, so worker wont die.
				if (stopped) {
					break;
				}
				if(logger.isEnabledFor(Level.ERROR))
				{
					logger.error("Unexpected exception occured:", e);
				}
				continue;
			}
		}

		for(int i=0;i<this.decodingThreads.length;i++)
			this.decodingThreads[i].shutdown();
		
		if(this.provider!=null)
			this.provider.stop();
		
		if (logger.isDebugEnabled()) {			
			logger.debug("MGCP stack stopped gracefully on" + this.localAddress + ":" + this.port);
		}
	}

	public Map<Integer, TransactionHandler> getLocalTransactions() {
		return localTransactions;
	}

	public Map<Integer, Integer> getRemoteTxToLocalTxMap() {
		return remoteTxToLocalTxMap;
	}

	public Map<Integer, TransactionHandler> getCompletedTransactions() {
		return completedTransactions;
	}

	private class DecodingThread extends Thread
	{
		private volatile boolean active;
        private JainMgcpStackImpl stack;
        protected MessageHandler messageHandler = null;
    	
		public DecodingThread(JainMgcpStackImpl stack)
		{
			this.stack=stack;
			messageHandler=new MessageHandler(stack);
		}
		
    	public void run() {
    		while(active)
    			try {
    				PacketRepresentation current=inputQueue.take();
    				messageHandler.scheduleMessages(current);
    			}
    			catch(Exception e)
    			{
    				//catch everything, so worker wont die.
    				if(logger.isEnabledFor(Level.ERROR))
    					logger.error("Unexpected exception occured:", e);    				    		
    			}
    	}
    	
    	public void activate() {        	        	
        	this.active = true;
        	this.start();
        }
    	
    	/**
         * Terminates thread.
         */
        private void shutdown() {
            this.active = false;
        }
	}
}
