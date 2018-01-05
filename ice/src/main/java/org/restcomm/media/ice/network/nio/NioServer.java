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

package org.restcomm.media.ice.network.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.ice.network.ProtocolHandler;

/**
 * Non-blocking server that runs on a single thread, alternating between reading
 * and writing modes.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class NioServer implements Runnable {
	
	private Logger logger = LogManager.getLogger(NioServer.class);

	private static final int BUFFER_SIZE = 8192;

	// The selector that monitors the registered channels
	private Selector selector;

	// The buffer into which we will read data when it's available
	private ByteBuffer buffer;

	// Schedules events for posterior IO operations
	private EventScheduler scheduler;

	// List of operation requests
	private List<OperationRequest> operationRequests;

	// Registers pending data per channel
	private Map<DatagramChannel, List<ByteBuffer>> pendingData;
	
	// Protocol Handler to process incoming packets
	protected ProtocolHandler protocolHandler;

	// Server execution controls
	private Thread schedulerThread;
	private Thread serverThread;
	private volatile boolean running;

	public NioServer(Selector selector) {
		this.selector = selector;
		this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.scheduler = new EventScheduler(this);
		this.operationRequests = new LinkedList<OperationRequest>();
		this.pendingData = new HashMap<DatagramChannel, List<ByteBuffer>>();
		this.running = false;
	}

	public boolean isRunning() {
		return running;
	}

	public void start() {
		if (!this.running) {
			this.schedulerThread = new Thread(this.scheduler);
			this.serverThread = new Thread(this);

			this.running = true;
			this.schedulerThread.start();
			this.serverThread.start();
		}
	}

	public void stop() {
		this.running = false;
	}
	
	public void stopNow() {
		this.running = false;
		this.scheduler.stop();
	}

	private void cleanup() {
		if (this.selector.isOpen()) {
			try {
				this.selector.close();
			} catch (IOException e) {
				this.logger.error("Server did not stop in an elegant manner: "+ e.getMessage(), e);
			}
		}
	}

	@Override
	public void run() {
		logger.info("NIO Server started");
		while (this.running && this.selector.isOpen()) {
			try {
				// Process any pending changes
				synchronized (this.operationRequests) {
					Iterator<OperationRequest> changes = this.operationRequests.iterator();
					while (changes.hasNext()) {
						// Process the request
						OperationRequest request = changes.next();
						if (OperationRequest.CHANGE_OPS == request.getRequestType()) {
							SelectionKey key = request.getChannel().keyFor(this.selector);
							if (key != null && key.isValid()) {
								// The null check is necessary because a key can
								// be canceled by external agents.
								key.interestOps(request.getOperations());
							}
						}
						// Remove the request from the queue
						changes.remove();
					}
				}

				// Wait for an event one of the registered channels
				this.selector.select();

				// Iterate over the set of keys for which events are available
				Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();
					
					// Verify if key was not invalidated in a previous operation
					if (key.isValid()) {
						try {
							// Take action based on current key mode
							if (key.isReadable()) {
								this.read(key);
							} else if (key.isWritable()) {
								this.write(key);
							}
						} catch (Exception e) {
							this.logger.error("Could not process packet: "+ e.getMessage(), e);
						}
					}
				}
			} catch (Exception e) {
				this.logger.error(e.getMessage(), e);
			}
		}
		// Cleanup resource after server stops
		cleanup();
		logger.info("NIO Server stopped");
	}

	private List<ByteBuffer> registerChannel(DatagramChannel channel) {
		List<ByteBuffer> data = new ArrayList<ByteBuffer>();
		this.pendingData.put(channel, data);
		return data;
	}

	public void send(DatagramChannel channel, byte[] data) {
		synchronized (this.operationRequests) {
			// Alternate the channel mode to write
			OperationRequest writeRequest = new OperationRequest(channel, OperationRequest.CHANGE_OPS, SelectionKey.OP_WRITE);
			this.operationRequests.add(writeRequest);

			// Enqueue the data to be written
			synchronized (this.pendingData) {
				List<ByteBuffer> queue = this.pendingData.get(channel);
				if (queue == null) {
					queue = registerChannel(channel);
				}
				queue.add(ByteBuffer.wrap(data));
			}
		}
		// Wake selector thread so it can make the required changes
		this.selector.wakeup();
	}

	private void read(SelectionKey key) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();

		// Get buffer ready to read new data
		this.buffer.clear();

		// Read data from channel
		int dataLength = 0;
		try {
			SocketAddress remotePeer = channel.receive(this.buffer);
			if (!channel.isConnected() && remotePeer != null) {
				channel.connect(remotePeer);
			}
			dataLength = (remotePeer == null) ? -1 : this.buffer.position();
		} catch (IOException e) {
			dataLength = -1;
		}

		// Stop if socket was shutdown or error occurred
		if (dataLength == -1) {
			channel.close();
			key.cancel();
			return;
		}

		// Delegate work to a handler
		if (protocolHandler != null) {
			// Handler processes the requests and provides an answer
			byte[] data = this.buffer.array();
			byte[] response = protocolHandler.process(key, data, dataLength);
			// Keep reading if handler provided no answer
			if (response != null) {
				// schedule STUN request to be sent to browser
				this.scheduler.schedule(channel, response, response.length);
			}
		}
	}

	private void write(SelectionKey key) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();
		synchronized (this.pendingData) {
			// Retrieve data from registered channel
			List<ByteBuffer> queue = this.pendingData.get(key.channel());

			// Write all pending data
			while (!queue.isEmpty()) {
				ByteBuffer dataBuffer = queue.get(0);
				channel.send(dataBuffer, channel.getRemoteAddress());
				channel.write(dataBuffer);

				if (dataBuffer.remaining() > 0) {
					// Channel buffer is full
					// Data will be written in the next write operation
					break;
				} else {
					// All data was passed to the channel
					// Remove empty buffer
					queue.remove(0);
				}
			}

			// All pending data was written
			// Switch channel mode to read
			key.interestOps(SelectionKey.OP_READ);
		}
	}

}
