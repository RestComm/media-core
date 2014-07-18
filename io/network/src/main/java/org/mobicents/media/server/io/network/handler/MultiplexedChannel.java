package org.mobicents.media.server.io.network.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Data channel that supports multiplexing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MultiplexedChannel implements Multiplexer {
	
	private static final Logger LOGGER = Logger.getLogger(Multiplexer.class);

	// Registered protocol handlers. Used for multiplexing.
	protected final ProtocolHandlerPipeline handlers;

	// The buffer into which we will read data when it's available
	private static final int BUFFER_SIZE = 8192;
	private final ByteBuffer buffer;

	// Data that is pending for writing
	private final Map<DatagramChannel, List<ByteBuffer>> pendingData;

	public MultiplexedChannel() {
		this.handlers = new ProtocolHandlerPipeline();
		this.pendingData = new HashMap<DatagramChannel, List<ByteBuffer>>();
		this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
	}

	private void queueData(final DatagramChannel channel, final byte[] data) {
		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		List<ByteBuffer> queue = this.pendingData.get(channel);
		
		// Create queue for channel if it does not exist
		if(queue == null) {
			queue = new ArrayList<ByteBuffer>();
			this.pendingData.put(channel, queue);
		}
		
		// Add pending data to channel queue
		queue.add(dataBuffer);
	}
	
	public void receive(final SelectionKey key) throws IOException {
		// Get channel from selection key
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

		// Delegate work to the proper handler
		byte[] data = this.buffer.array();
		ProtocolHandler handler = this.handlers.getHandler(data);

		if (handler != null) {
			try {
				// Let the handler process the incoming packet.
				// A response MAY be provided as result.
				byte[] response = handler.handle(data, dataLength, 0);
				
				/*
				 * If handler intends to send a response to the remote peer,
				 * queue the data to send it on writing cycle. Only allowed if
				 * Selection Key is writable!
				 */
				if (key.isWritable() && response != null && response.length > 0) {
					queueData(channel, data);
				}
			} catch (ProtocolHandlerException e) {
				LOGGER.error("Could not handle incoming packet: " + e.getMessage());
			}
		} else {
			// No proper handler was found. Drop packet.
			LOGGER.warn("No protocol handler was found to process an incoming packet. Packet will be dropped.");
		}
	}

	public void send(final SelectionKey key) throws IOException {
		// Get all pending data on the requests channel
		DatagramChannel channel = (DatagramChannel) key.channel();
		List<ByteBuffer> queue = this.pendingData.get(channel);

		// Write all pending data on requested channel
		if(queue != null) {
			while (!queue.isEmpty()) {
				ByteBuffer dataBuffer = queue.get(0);
				channel.send(dataBuffer, channel.getRemoteAddress());
				// XXX channel.write(dataBuffer);

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
		}
	}
	
	public void close() {
		
	}

}
