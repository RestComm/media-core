/**
 * Start time:09:56:52 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
package org.mobicents.media.server.testsuite.general.rtp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;

import javax.sdp.Attribute;

import org.apache.log4j.Logger;

/**
 * Start time:09:56:52 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
public class RtpSocketFactoryImpl implements RtpSocketFactory {

	private static final Logger log = Logger.getLogger(RtpSocketFactoryImpl.class);

	private String localPortRange = "1024-6000";
	private int lowPort = 1024;
	private int highPort = 6000;
	private InetAddress bindAddress;

	private Vector<Attribute> formatMap;
	private Selector readSelector;
	private LinkedList<RtpSocket> notUsedSockets = new LinkedList<RtpSocket>();
	protected volatile HashMap<SelectionKey, RtpSocket> rtpSockets = new HashMap<SelectionKey, RtpSocket>();
	private Transceiver transceiver;

	public RtpSocketFactoryImpl() throws IOException {
		super();
		readSelector = SelectorProvider.provider().openSelector();

	}

	public RtpSocket createSocket() throws SocketException, IOException {
		// RtpSocket rtpSocket = new RtpSocketImpl(formatMap, this);
		// return rtpSocket;

		RtpSocket rtpSocket = notUsedSockets.poll();

		if (rtpSocket == null) {

			rtpSocket = new RtpSocketImpl(formatMap, this);
			rtpSocket.init(bindAddress, this.lowPort, this.highPort);
		} else if (!rtpSocket.isChannelOpen()) {
			log.error("The RTPSocket's DatagramChannel is closed. Re init() RtpSocket ");
			rtpSocket.init(bindAddress, lowPort, highPort);
		}

		return rtpSocket;
	}

	public void releaseSocket(RtpSocket rtpSocket) {

		this.rtpSockets.remove(rtpSocket.getSelectionKey());
		this.notUsedSockets.add(rtpSocket);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory# getFormatMap()
	 */
	public Vector<Attribute> getFormatMap() {
		return this.formatMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory# setBindAddress(java.lang.String)
	 */
	public void setBindAddress(String bindAddress) throws UnknownHostException {
		this.bindAddress = InetAddress.getByName(bindAddress);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory# setFormatMap(java.util.Map)
	 */
	public void setFormatMap(Vector<Attribute> originalFormatMap) {
		this.formatMap = originalFormatMap;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory#stop()
	 */
	public void stop() {
		if (this.transceiver != null) {
			this.transceiver.stop();
			this.transceiver = null;
		}
		for (RtpSocket socket : rtpSockets.values()) {
			socket.close();
		}
		rtpSockets.clear();
		for (RtpSocket socket : this.notUsedSockets) {
			socket.close();
		}
		this.notUsedSockets.clear();
		log.info("Stopped RTP Factory");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory#getBindAddress()
	 */
	public String getBindAddress() {
		return this.bindAddress.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory#start()
	 */
	public void start() throws SocketException, IOException {

		log.info("Binding RTP transceiver to " + bindAddress + ":" + localPortRange);

		transceiver = new Transceiver(rtpSockets, readSelector);
		transceiver.start();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory#getPort()
	 */
	public String getPortRange() {
		return this.localPortRange;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory#setPort(int)
	 */
	public void setPortRange(String port) {
		this.localPortRange = port;

	}

	public int getLowPort() {
		return lowPort;
	}

	public void setLowPort(int lowPort) {
		this.lowPort = lowPort;
	}

	public int getHighPort() {
		return highPort;
	}

	public void setHighPort(int hightPort) {
		this.highPort = hightPort;
	}

	/**
	 * @param rtpSocketImpl
	 * @return
	 * @throws ClosedChannelException
	 */
	public SelectionKey registerSocket(RtpSocketImpl rtpSocketImpl) throws ClosedChannelException {

		try {
			SelectionKey key = rtpSocketImpl.getSelectionKey();
			if (key != null) {
				this.rtpSockets.put(key, rtpSocketImpl);
				return key;
			} else {
				this.transceiver.addNewChannel(rtpSocketImpl);
				return null;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
