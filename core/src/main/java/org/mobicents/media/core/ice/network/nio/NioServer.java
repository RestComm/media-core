package org.mobicents.media.core.ice.network.nio;

import java.io.IOException;
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

import org.bouncycastle.util.Arrays;

public class NioServer implements Runnable {

	private static final int BUFFER_SIZE = 8192;

	// The monitored selector
	private Selector selector;

	// The buffer into which we'll read data when it's available
	private ByteBuffer buffer;

	// Schedules events for posterior IO operations
	private DataHandler dataHandler;

	// List of operation requests
	private List<OperationRequest> operationRequests;

	// Registers pending data per channel
	private Map<DatagramChannel, List<ByteBuffer>> pendingData;

	// Server execution controls
	private Thread handlerThread;
	private Thread serverThread;
	private boolean running;

	public NioServer(Selector selector) {
		this.selector = selector;
		this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.dataHandler = new DataHandler();
		this.operationRequests = new LinkedList<OperationRequest>();
		this.pendingData = new HashMap<DatagramChannel, List<ByteBuffer>>();
		this.running = false;
	}

	public boolean isRunning() {
		return running;
	}

	public void start() {
		if (!this.running) {
			this.handlerThread = new Thread(this.dataHandler);
			this.serverThread = new Thread(this);

			this.handlerThread.start();
			this.serverThread.start();
			this.running = true;
		}
	}

	public void stop() {
		if (this.running) {
			this.handlerThread.interrupt();
			this.serverThread.interrupt();
			this.running = false;
		}
	}

	public void run() {
		while (true) {
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
							key.interestOps(request.getOperations());
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
	}

	private List<ByteBuffer> registerChannel(DatagramChannel channel) {
		List<ByteBuffer> data = new ArrayList<ByteBuffer>();
		this.pendingData.put(channel, data);
		return data;
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
		int dataLength = 0;
		try {
			dataLength = channel.read(this.buffer);
		} catch (IOException e) {
			// The remote peer forcibly closed the connection
			key.cancel();
			channel.close();
			return;
		}

		if (dataLength == -1) {
			// Remote peer cleanly shut down the socket
			key.channel().close();
			key.cancel();
			return;
		}

		// Delegate work to a handler
		byte[] data = this.buffer.array();
		this.dataHandler.schedule(this, channel, data, dataLength);
	}

	private void write(SelectionKey key) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();
		synchronized (this.pendingData) {
			// Retrieve data from registered channel
			List<ByteBuffer> queue = this.pendingData.get(key);

			// Write all pending data
			while (!queue.isEmpty()) {
				ByteBuffer dataBuffer = queue.get(0);
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

	private class DataHandler implements Runnable {

		private List<DataHandlerEvent> events;

		public DataHandler() {
			this.events = new ArrayList<DataHandlerEvent>();
		}

		public void schedule(NioServer server, DatagramChannel channel,
				byte[] data, int dataLength) {
			byte[] dataCopy = Arrays.copyOf(data, dataLength);
			synchronized (this.events) {
				DataHandlerEvent event = new DataHandlerEvent(server, channel,
						dataCopy);
				this.events.add(event);
				this.events.notify();
			}
		}

		public void run() {
			DataHandlerEvent event;

			// Wait for data to become available
			while (true) {
				synchronized (this.events) {
					while (this.events.isEmpty()) {
						try {
							this.events.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					// Incoming event received
					event = this.events.remove(0);
				}
				// Launch the event on the server
				event.launch();
			}
		}

	}

	private class DataHandlerEvent {
		private NioServer server;
		private DatagramChannel channel;
		private byte[] data;

		public DataHandlerEvent(NioServer server, DatagramChannel channel,
				byte[] data) {
			super();
			this.server = server;
			this.channel = channel;
			this.data = data;
		}

		public void launch() {
			this.server.send(channel, data);
		}
	}

}
