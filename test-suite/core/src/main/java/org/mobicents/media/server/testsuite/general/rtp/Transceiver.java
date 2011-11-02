/**
 * Start time:14:52:37 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
package org.mobicents.media.server.testsuite.general.rtp;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Start time:14:52:37 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * This class represent similar usability as Transceiver in mms, however it does only reiceve and math poper socket,
 * this is due that mms sends data on single port, thus we can not use the same scheme as mms.
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
public class Transceiver implements Runnable {

	private int bufferSize = 8196;
	private ByteBuffer readerBuffer = ByteBuffer.allocate(bufferSize);
	private volatile boolean started = false;
	private HashMap<SelectionKey, RtpSocket> rtpSockets;
	private Selector readSelector;

	private List<RtpSocketImpl> newChannels = new LinkedList<RtpSocketImpl>();

	public Transceiver(HashMap<SelectionKey, RtpSocket> rtpSockets, Selector readSelector) throws SocketException,
			IOException {
		this.rtpSockets = rtpSockets;
		this.readSelector = readSelector;
	}

	public void addNewChannel(RtpSocketImpl channel) {
		synchronized (this.newChannels) {
			newChannels.add(channel);
		}
	}

	public void start() {
		started = true;
		new Thread(this, "RTP Transceiver").start();
	}

	public void stop() {
		started = false;
		try {

		} catch (Exception e) {
		}
	}

	private void notifyError(Exception e) {
		Collection<RtpSocket> list = rtpSockets.values();
		for (RtpSocket s : list) {
			s.notify(e);
		}
	}

	public void send(byte[] data, int len, SocketAddress remoteAddress) throws IOException {
		// ByteBuffer byteBuffer1 = ByteBuffer.wrap(data);
		// int count = 0;
		//
		// while (count < len) {
		// count += channel.send(byteBuffer1, remoteAddress);
		// byteBuffer1.compact();
		// byteBuffer1.flip();
		// }
	}

	public void run() {
		while (started) {

			synchronized (this.newChannels) {
				try {
					for (RtpSocketImpl rtpSocketImpl : this.newChannels) {
						SelectionKey key = rtpSocketImpl.getChannel().register(this.readSelector, SelectionKey.OP_READ);
						rtpSocketImpl.setSelectionKey(key);

						this.rtpSockets.put(key, rtpSocketImpl);
					}
				} catch (ClosedChannelException e) {
					e.printStackTrace();
				}

				this.newChannels.clear();
			}

			try {
				readSelector.select(2);

				long receiveStamp = System.currentTimeMillis();
				Set<SelectionKey> keys = readSelector.selectedKeys();

				for (SelectionKey sk : keys) {
					// if (sk.isReadable()) {
					try {
						RtpSocket socket = this.rtpSockets.get(sk);
						if (socket == null) {
							// this can happen, its legal
							continue;
						}
						DatagramChannel channel = socket.getChannel();
						if (channel == null || !socket.isChannelOpen() || (channel != null && !channel.isConnected())) {
							continue;
						}
						int i = channel.read(readerBuffer);
						readerBuffer.flip();

						if (i > 0) {
							RtpPacket rtpPacket = new RtpPacket(readerBuffer);
							rtpPacket.setTime(new Date(receiveStamp));
							socket.receive(rtpPacket);
						}

						readerBuffer.clear();
					} catch (IOException e) {
						if (started) {
							notifyError(e);
						}
					}
					// }
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
}