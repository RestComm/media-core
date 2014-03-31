package org.mobicents.media.core.ice.network.nio;

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

import org.mobicents.media.core.ice.network.ExpirableProtocolHandler;
import org.mobicents.media.core.ice.network.ExpiringPipeline;
import org.mobicents.media.core.ice.network.Pipeline;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class NioServer implements Runnable {

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

	// Pipeline of packet handlers
	private Pipeline<ExpirableProtocolHandler> protocolHandlers;

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
		this.protocolHandlers = new ExpiringPipeline<ExpirableProtocolHandler>();
	}

	public boolean isRunning() {
		return running;
	}

	public void start() {
		if (!this.running) {
			this.schedulerThread = new Thread(this.scheduler);
			this.serverThread = new Thread(this);

			this.schedulerThread.start();
			this.serverThread.start();
			this.running = true;
		}
	}

	public void stop() {
		// if (this.running) {
		// this.schedulerThread.interrupt();
		// this.serverThread.interrupt();
		// this.running = false;
		// }
		this.running = false;
		this.scheduler.stop();
	}

	private void cleanup() {
		if (this.selector.isOpen()) {
			try {
				this.selector.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void run() {
		while (this.running && this.selector.isOpen()) {
			try {
				// Process any pending changes
				synchronized (this.operationRequests) {
					Iterator<OperationRequest> changes = this.operationRequests
							.iterator();
					while (changes.hasNext()) {
						// Process the request
						OperationRequest request = changes.next();
						if (OperationRequest.CHANGE_OPS == request
								.getRequestType()) {
							SelectionKey key = request.getChannel().keyFor(
									this.selector);
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
				Iterator<SelectionKey> selectedKeys = this.selector
						.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (key.isValid()) {
						// Take action based on current key mode
						if (key.isReadable()) {
							this.read(key);
						} else if (key.isWritable()) {
							this.write(key);
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Cleanup resource after server stops
		cleanup();
	}

	private List<ByteBuffer> registerChannel(DatagramChannel channel) {
		List<ByteBuffer> data = new ArrayList<ByteBuffer>();
		this.pendingData.put(channel, data);
		return data;
	}

	public void addProtocolHandler(ExpirableProtocolHandler handler) {
		this.protocolHandlers.add(handler);
	}

	public void send(DatagramChannel channel, byte[] data) {
		synchronized (this.operationRequests) {
			// Alternate the channel mode to write
			OperationRequest writeRequest = new OperationRequest(channel,
					OperationRequest.CHANGE_OPS, SelectionKey.OP_WRITE);
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
		if (channel.isConnected()) {

		}

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
		byte[] data = this.buffer.array();
		ExpirableProtocolHandler protocolHandler = this.protocolHandlers
				.getCurrent();

		if (protocolHandler != null) {
			// Handler processes the requests and provides an answer
			byte[] response = protocolHandler.handleRead(key, data, dataLength);
			// Keep reading if handler provided no answer
			if (response != null) {
				this.scheduler.schedule(channel, response, response.length);

				// XXX schedule STUN request to be sent to browser
				
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
				ExpirableProtocolHandler handler = this.protocolHandlers
						.getCurrent();
				handler.handleWrite(key, dataBuffer.array(),
						dataBuffer.array().length);
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

			if (queue.isEmpty()) {
				// All pending data was written
				// Switch channel mode to read
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

}
