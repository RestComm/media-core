/**
 * Start time:10:17:34 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
package org.mobicents.media.server.testsuite.general.rtp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.sdp.Attribute;

import org.apache.log4j.Logger;

/**
 * Start time:10:17:34 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski</a>
 */
public class RtpSocketImpl implements RtpSocket {

	private static final transient Logger log = Logger.getLogger(RtpSocketImpl.class);

	private int localPort;
	private String localAddress;

	private int remotePort;
	private String remoteAddress;
	private InetSocketAddress remoteInetSocketAddress = null;

	// holder for dynamic payloads.
	private Vector<Attribute> rtpMap = new Vector<Attribute>();

	private final List<RtpSocketListener> dataSinks = new ArrayList<RtpSocketListener>();

	private RtpSocketFactoryImpl rtpSocketFactoryImpl;
	private DatagramChannel channel;
	private DatagramSocket socket;
	private SelectionKey key;
	private String connectionIdentifier;
	

	/**
	 * 
	 * @param transceiver
	 * @param timer
	 * @param formatMap
	 * @param rtpSocketFactoryImpl2
	 */
	public RtpSocketImpl(Vector<Attribute> formatMap, RtpSocketFactoryImpl rtpSocketFactory) {
		this.rtpSocketFactoryImpl = rtpSocketFactory;
		this.rtpMap.addAll(formatMap);
		this.localAddress = this.rtpSocketFactoryImpl.getBindAddress();

	}

	public int init(InetAddress localAddress, int lowPort, int highPort) throws SocketException, IOException {

		if (this.key != null) {
			this.key.cancel();
			this.key = null;
		}
		channel = DatagramChannel.open();
		channel.configureBlocking(false);

		socket = channel.socket();

		boolean bound = false;

		this.localAddress = localAddress.getHostAddress();
		this.localPort = lowPort;

		// looking for first unused port
		while (!bound) {
			try {
				// creating local address and trying to bind socket to this
				// address
				InetSocketAddress bindAddress = new InetSocketAddress(localAddress, localPort);
				socket.bind(bindAddress);
				bound = true;
				// if stunHost is assigned then stun ussage is supposed
				// discovering paublic address

			} catch (SocketException e) {
				// increment port number util upper limit is not reached
				localPort++;
				if (localPort > highPort) {
					throw e;
				}
			}
		}

		return localPort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.rtp.RtpSocket#addListener
	 *      (org.mobicents.media.server.testsuite.general.rtp.RtpSocketListener)
	 */
	public void addListener(RtpSocketListener listener) {
		this.dataSinks.add(listener);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.rtp.RtpSocket#removeListener
	 *      (org.mobicents.media.server.testsuite.general.rtp.RtpSocketListener)
	 */
	public void removeListener(RtpSocketListener listener) {
		this.dataSinks.remove(listener);

	}

	public void resetRtpMap() {
		this.rtpMap.clear();
		this.rtpMap.addAll(this.rtpSocketFactoryImpl.getFormatMap());
	}

	public boolean isChannelOpen() {
		return (this.channel != null && this.channel.isOpen());
	}

	public void release() {

		this.resetRtpMap();
		if (socket != null) {
			socket.disconnect();
			// socket.close();
		}

		dataSinks.clear();
		this.connectionIdentifier = null;
		this.rtpSocketFactoryImpl.releaseSocket(this);
		
	}

	/**
	 * Closes socket
	 * 
	 */
	public void close() {
		if (channel != null) {
			try {
				channel.disconnect();

			} catch (IOException e) {
			}
		}

	}

	/**
	 * Assigns remote end.
	 * 
	 * @param address
	 *            the address of the remote party.
	 * @param port
	 *            the port number of the remote party.
	 */
	public void setPeer(InetAddress address, int port) throws IOException {
		remoteAddress = address.getHostAddress();
		remotePort = port;
		remoteInetSocketAddress = new InetSocketAddress(remoteAddress, remotePort);
		channel.connect(remoteInetSocketAddress);
		this.key = rtpSocketFactoryImpl.registerSocket(this);

		if (log.isDebugEnabled()) {
			log.debug("Connect RTP socket[" + localAddress + ":" + localPort + " to " + remoteAddress + ":"
					+ remotePort);
		}
	}

	/**
	 * Gets address to which this socked is bound.
	 * 
	 * @return either local address to which this socket is bound
	 */
	public String getLocalAddress() {
		return localAddress;
	}

	/**
	 * Returns port number to which this socked is bound.
	 * 
	 * @return port number or -1 if socket not bound.
	 */
	public int getLocalPort() {
		return localPort;
	}

	public void notify(Exception e) {
		for (RtpSocketListener l : this.dataSinks)
			l.error(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.rtp.RtpSocket#receive(org
	 *      .mobicents.media.server.testsuite.general.rtp.RtpPacket)
	 */
	public void receive(RtpPacket rtpPacket) {
		rtpPacket.setConnectionIdentifier(this.connectionIdentifier);
		for (RtpSocketListener l : this.dataSinks)
			l.receive(rtpPacket);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.rtp.RtpSocket#getSelectionKey()
	 */
	public SelectionKey getSelectionKey() {

		return this.key;
	}

	public void setSelectionKey(SelectionKey key) {
		this.key = key;
	}

	public DatagramChannel getChannel() {
		return this.channel;
	}

	public String getConnectionIdentifier() {
		return connectionIdentifier;
	}

	public void setConnectionIdentifier(String connectionIdentifier) {
		this.connectionIdentifier = connectionIdentifier;
	}
	
}
