package org.mobicents.media.server.io.network.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Data channel that supports multiplexing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MultiplexedChannel implements Channel {
	
	private static final Logger logger = Logger.getLogger(Channel.class);
	
	// Data channel where data will be received and transmitted
	protected DatagramChannel dataChannel;

	// Registered protocol handlers. Used for multiplexing.
	protected final PacketHandlerPipeline handlers;

	// The buffer into which we will read data when it's available
	private static final int BUFFER_SIZE = 8192;
	private final ByteBuffer receiveBuffer;
	
	// Data that is pending for writing
	private final List<byte[]> pendingData;
	private final ByteBuffer pendingDataBuffer;

	public MultiplexedChannel() {
		this.handlers = new PacketHandlerPipeline();
		this.pendingData = new ArrayList<byte[]>();
		this.pendingDataBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
		this.receiveBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
	}
	
	@Override
	public SocketAddress getLocalAddress() {
		if(isOpen()) {
			try {
				return this.dataChannel.getLocalAddress();
			} catch (IOException e) {
				logger.error("Could not get local addres of the data channel: "+ e.getMessage());
			}
		}
		return null;
	}
	
	public String getLocalHost() {
		if(this.dataChannel != null && this.dataChannel.isOpen()) {
			try {
				return ((InetSocketAddress) this.dataChannel.getLocalAddress()).getHostString();
			} catch (IOException e) {
				logger.error("Could not lookup the channel address: "+ e.getMessage(), e);
			}
		}
		return "";
	}
	
	public int getLocalPort() {
		if(this.dataChannel != null && this.dataChannel.isOpen()) {
			return this.dataChannel.socket().getLocalPort();
		}
		return 0;
	}
	
	public void setTransport(final DatagramChannel channel) {
		this.dataChannel = channel;
	}
	
	protected void queueData(final byte[] data) {
		if (data != null && data.length > 0) {
			synchronized (this.pendingData) {
				this.pendingData.add(data);	
			}
		}
	}
	
	public boolean hasPendingData() {
		synchronized (this.pendingData) {
			return !this.pendingData.isEmpty();
		}
	}
	
	protected void flush() {
		if(this.dataChannel != null && this.dataChannel.isOpen()) {
			try {
				// lets clear the receiver
				SocketAddress currAddress;
				this.receiveBuffer.clear();
				do {
					currAddress = this.dataChannel.receive(this.receiveBuffer);
					this.receiveBuffer.clear();
				} while(currAddress != null);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	public void receive() throws IOException {
		// Get buffer ready to read new data
		this.receiveBuffer.clear();

		// Read data from channel
		int dataLength = 0;
		try {
			SocketAddress remotePeer = dataChannel.receive(this.receiveBuffer);
			if (!isConnected() && remotePeer != null) {
				connect(remotePeer);
			}
			dataLength = this.receiveBuffer.position();
		} catch (IOException e) {
			dataLength = -1;
		}

		// Stop if socket was shutdown or error occurred
		if (dataLength == -1) {
			close();
			return;
		} else if (dataLength > 0) {
			// Copy data from buffer so we don't mess with original
			byte[] dataCopy = new byte[dataLength];
			this.receiveBuffer.rewind();
			this.receiveBuffer.get(dataCopy, 0, dataLength);
			
			// Delegate work to the proper handler
			PacketHandler handler = this.handlers.getHandler(dataCopy);
			if (handler != null) {
				try {
					// Let the handler process the incoming packet.
					// A response MAY be provided as result.
					byte[] response = handler.handle(dataCopy, dataLength, 0, (InetSocketAddress) dataChannel.getLocalAddress(), (InetSocketAddress) dataChannel.getRemoteAddress());
					
					/*
					 * If handler intends to send a response to the remote peer,
					 * queue the data to send it on writing cycle. Only allowed if
					 * Selection Key is writable!
					 */
					if (response != null && response.length > 0) {
						queueData(response);
					}
				} catch (PacketHandlerException e) {
					logger.error("Could not handle incoming packet: " + e.getMessage());
				}
			} else {
				logger.warn("No protocol handler was found to process an incoming packet. Packet will be dropped.");
			}
		}
	}

	public void send() throws IOException {
		while (!this.pendingData.isEmpty()) {
			// Get pending data into the proper buffer
			this.pendingDataBuffer.clear();
			this.pendingDataBuffer.put(this.pendingData.get(0));
			this.pendingDataBuffer.flip();
			
			// Send data over the channel
			this.dataChannel.send(this.pendingDataBuffer, this.dataChannel.getRemoteAddress());

			if (this.pendingDataBuffer.remaining() > 0) {
				// Channel buffer is full
				// Data will be written in the next write operation
				break;
			} else {
				// All data was passed to the channel
				// Remove empty buffer
				this.pendingData.remove(0);
			}
		}
	}
	
	@Override
	public boolean isOpen() {
		if(this.dataChannel != null) {
			return this.dataChannel.isOpen();
		}
		return false;
	}
	
	public boolean isConnected() {
		return this.dataChannel != null && this.dataChannel.isConnected();
	}
	
	@Override
	public void bind(SocketAddress address) throws IOException {
		if(!isOpen()) {
			throw new IOException("The channel is closed.");
		}
		this.dataChannel.bind(address);
	}
	
	public void connect(SocketAddress address) throws IOException {
		if(this.dataChannel == null) {
			throw new IOException("No channel available to connect.");
		}
		this.dataChannel.connect(address);
	}
	
	public void disconnect() throws IOException {
		if(isConnected()) {
			this.dataChannel.disconnect();
		}
	}
	
	@Override
	public void open() throws IOException {
		if(this.dataChannel == null || !this.dataChannel.isOpen()) {
			this.dataChannel = DatagramChannel.open();
		} else {
			throw new IOException("Channel is already open.");
		}
	}
	
	@Override
	public void open(DatagramChannel dataChannel) throws IOException {
		if(dataChannel == null) {
			throw new IOException("The data channel cannot be null.");
		}
		
		if(!dataChannel.isOpen()) {
			throw new IOException("The data channel is closed.");
		}
		
		this.dataChannel= dataChannel;
	}
	
	public void close() {
		if (isOpen()) {
			if (isConnected()) {
				try {
					dataChannel.disconnect();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}
			try {
				dataChannel.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
}
