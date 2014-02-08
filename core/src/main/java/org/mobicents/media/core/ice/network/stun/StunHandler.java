package org.mobicents.media.core.ice.network.stun;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.List;

import org.ice4j.StunException;
import org.ice4j.message.Message;
import org.mobicents.media.server.io.network.ProtocolHandler;
import org.mobicents.media.server.scheduler.Task;

/**
 * Handles incoming/outgoing STUN traffic
 * 
 * @author Henrique Rosa
 * 
 * @see ProtocolHandler
 */
public class StunHandler implements ProtocolHandler {

	private SelectionKey selectionKey;

	private RxTask rxTask;
	private TxTask txTask;

	private ByteBuffer buffer = ByteBuffer.allocate(StunPacket.HEADER_LENGTH);

	public StunHandler(SelectionKey selectionKey) {
		this.selectionKey = selectionKey;
		this.rxTask = new RxTask();
		this.txTask = new TxTask();
	}

	public void receive(DatagramChannel channel) {
		this.buffer.clear();

		int read = 0;
		try {
			read = channel.read(buffer);
		} catch (IOException e) {
			// Remote peer forcibly closed the connection
			closeChannel(channel, selectionKey);
			return;
		}

		if (read == -1) {
			// Remote peer closed the connection
			closeChannel(channel, selectionKey);
			return;
		}

		// Delegate data processing to worker thread
		this.rxTask.perform(this, channel, this.buffer.array(), read);
	}

	public void send(DatagramChannel channel) {
		this.txTask.perform();
	}

	public boolean isReadable() {
		return this.selectionKey.isReadable();
	}

	public boolean isWriteable() {
		return this.selectionKey.isWritable();
	}

	public void setKey(SelectionKey key) {
		this.selectionKey = key;
	}

	public void onClosed() {
		// TODO Auto-generated method stub

	}

	private void closeChannel(DatagramChannel channel, SelectionKey key) {
		try {
			key.cancel();
			if (channel.isOpen()) {
				channel.close();
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	private class RxTask implements Runnable {

		private List<StunMessage> packets = new LinkedList<StunMessage>();

		public void perform(StunHandler handler, DatagramChannel channel,
				byte[] data, int count) {
			byte[] dataCopy = new byte[count];
			System.arraycopy(data, 0, dataCopy, 0, count);
			synchronized (this.packets) {
				this.packets.add(new StunMessage(handler, channel, dataCopy));
				this.packets.notify();
			}
		}

		public void run() {
			StunMessage packet;
			
			while(true) {
				synchronized (this.packets) {
					while(this.packets.isEmpty()) {
						try {
							this.packets.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					packet = this.packets.remove(0);
					// TODO process packet
					try {
						Message message = Message.decode(packet.data, 'c', 'c');
						
					} catch (StunException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

	}

	private class TxTask implements Runnable {

		public long perform() {
			// TODO Auto-generated method stub
			return 0;
		}

		public void run() {
			// TODO Auto-generated method stub

		}

	}

	private class StunMessage {
		private StunHandler handler;
		private DatagramChannel channel;
		private byte[] data;

		public StunMessage(StunHandler handler, DatagramChannel channel,
				byte[] data) {
			this.handler = handler;
			this.channel = channel;
			this.data = data;
		}
	}

}
