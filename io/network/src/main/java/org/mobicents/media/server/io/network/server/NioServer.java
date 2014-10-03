package org.mobicents.media.server.io.network.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.network.channel.PacketHandler;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;
import org.mobicents.media.server.io.network.channel.PacketHandlerPipeline;

/**
 * Non-blocking, single-threaded server that runs on a single thread.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class NioServer {
	
	private static final Logger logger = Logger.getLogger(NioServer.class);
	
	private static final int MAX_BUFFER_SIZE = 8192;
	
	private final Selector selector;
	private final ByteBuffer buffer;
	private final PacketHandlerPipeline packetHandlers;
	private boolean running;
	
	private final Worker worker;
	private Thread workerThread;
	
	public NioServer(final Selector selector) {
		this.selector = selector;
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
	
	public void start() {
		if(!this.running) {
			logger.info("Started NIO Server");
			this.running = true;
			
			if(this.workerThread == null) {
				this.workerThread = new Thread(this.worker);
			}
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
						DatagramChannel channel = (DatagramChannel) key.channel();
						int dataLength = receive(channel, buffer);
						
						/*
						 * If an error occurred, close the channel and continue iterating.
						 * If data was received, process incoming packet.
						 * If no data was received, leave channel open and skip to next key.
						 */
						if(dataLength < 0) {
							try {
								channel.close();
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
									byte[] response = handler.handle(data, (InetSocketAddress) channel.getLocalAddress(), (InetSocketAddress) channel.getRemoteAddress());
									if(response != null && key.isWritable()) {
										send(channel, response, buffer);
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
			logger.info("NIO Server stopped");
		}
		
	}
	

}
