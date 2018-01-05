/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.network.deprecated.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.network.deprecated.channel.PacketHandler;
import org.restcomm.media.network.deprecated.channel.PacketHandlerException;
import org.restcomm.media.network.deprecated.channel.PacketHandlerPipeline;

/**
 * Non-blocking, single-threaded server that relies on a pipeline of handlers to
 * process incoming packets.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class NioServer {
	
	private static final Logger logger = LogManager.getLogger(NioServer.class);
	
	private static final int MAX_BUFFER_SIZE = 8192;
	
	private final ByteBuffer buffer;
	private final PacketHandlerPipeline packetHandlers;
	private boolean running;
	protected DatagramChannel currentChannel;
	private Selector selector;
	private final Worker worker;
	private Thread workerThread;
	
	public NioServer() {
		this.buffer = ByteBuffer.allocateDirect(MAX_BUFFER_SIZE);
		this.packetHandlers = new PacketHandlerPipeline();
		this.worker = new Worker();
		this.running = false;
	}
	
	/**
	 * Indicates whether the server is running or not.
	 * 
	 * @return Returns true if the server is currently running. Returns false
	 *         otherwise.
	 */
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * Registers a packet handler in the pipeline.
	 * 
	 * @param handler
	 *            The packet handler to be registered.
	 * @return Returns true if the handler was successfully registered. Returns
	 *         false otherwise.
	 */
	public boolean addPacketHandler(PacketHandler handler) {
		return this.packetHandlers.addHandler(handler);
	}
	
	private byte[] getBufferedData(int dataLength) {
		byte[] data = new byte[dataLength];
		this.buffer.rewind();
		this.buffer.get(data, 0, dataLength);
		return data;
	}
	
	private int receive(DatagramChannel channel, ByteBuffer buffer) {
		int dataLength = 0;
		try {
			buffer.clear();
			SocketAddress remotePeer = channel.receive(buffer);
			
			if(!channel.isConnected() && remotePeer != null) {
				channel.connect(remotePeer);
			}
			dataLength = this.buffer.position();
		} catch (IOException e) {
			logger.error("Could not receive data: "+ e.getMessage(), e);
			dataLength = -1;
		}
		return dataLength;
	}
	
	private int send(DatagramChannel channel, byte[] data, ByteBuffer buffer) {
		// Prepare buffer
		buffer.clear();
		buffer.put(data).flip();
		
		// Send data to remote peer
		int dataLength = 0;
		try {
			dataLength = channel.send(buffer, channel.getRemoteAddress());
		} catch (IOException e) {
			logger.error("Could not send data: "+ e.getMessage(), e);
			dataLength = -1;
		}
		return dataLength;
	}
	
	public void start(final Selector selector) {
		if(!this.running) {
			logger.info("Started NIO Server");
			this.running = true;
			this.selector = selector;
			this.workerThread = new Thread(this.worker);
			this.workerThread.start();
		}
	}

	public void stop() {
		if(this.running) {
			logger.info("Stopping NIO Server...");
			this.running = false;
		}
	}
	
	private class Worker implements Runnable {
		
		public void run() {
			while(running && selector.isOpen()) {
				try {
					// select channels ready for IO
					selector.selectNow();
					Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
					
					while(keys.hasNext()) {
						SelectionKey key = keys.next();
						keys.remove();
						
						// receive data from underlying channel
						currentChannel = (DatagramChannel) key.channel();
						int dataLength = receive(currentChannel, buffer);
						
						/*
						 * If an error occurred, close the channel and continue iterating.
						 * If data was received, process incoming packet.
						 * If no data was received, leave channel open and skip to next key.
						 */
						if(dataLength < 0) {
							try {
								currentChannel.close();
							} catch (IOException e) {
								logger.error("Could not close defective channel: "+ e.getMessage(), e);
							}
							continue;
						} else if(dataLength > 0) {
							// Find proper handler to process incoming packet
							byte[] data = getBufferedData(dataLength);
							PacketHandler handler = packetHandlers.getHandler(data);
							if(handler != null) {
								try {
									// Process incoming packet and send response back, if any
									byte[] response = handler.handle(data, (InetSocketAddress) currentChannel.getLocalAddress(), (InetSocketAddress) currentChannel.getRemoteAddress());
									if(response != null && key.isWritable()) {
										send(currentChannel, response, buffer);
									}
								} catch (PacketHandlerException e) {
									logger.error("Could not process incoming packet. Packet will be dropped.");
								}
							} else {
								logger.warn("No handler found to process incoming packet. Packet will be dropped.");
							}
						}
					}
				} catch (IOException e) {
					logger.error("Could not select keys: "+ e.getMessage(), e);
				}
			}
			
			if(selector.isOpen()) {
				try {
					selector.close();
				} catch (IOException e) {
					logger.error("Could not close selector: "+ e.getMessage(), e);
				}
			}
			currentChannel = null;
			selector = null;
			logger.info("NIO Server stopped");
		}
		
	}
	

}
