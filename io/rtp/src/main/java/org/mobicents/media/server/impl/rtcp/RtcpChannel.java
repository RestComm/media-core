package org.mobicents.media.server.impl.rtcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.channel.MultiplexedChannel;

/**
 * Channel for exchanging RTCP traffic
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpChannel extends MultiplexedChannel {

	private static final Logger logger = Logger.getLogger(RtcpChannel.class);

	// Core elements
	private final UdpManager udpManager;

	// Channel attribute
	private int channelId;
	private SocketAddress remotePeer;
	private boolean bound;

	// Statistics
	private final RtpStatistics statistics;

	// Protocol handler pipeline
	private final RtcpHandler rtcpHandler;

	public RtcpChannel(int channelId, RtpStatistics statistics, UdpManager udpManager) {
		// Initialize MultiplexedChannel elements
		super();

		// Core elements
		this.udpManager = udpManager;

		// Channel attributes
		this.channelId = channelId;
		this.remotePeer = null;
		this.bound = false;

		// Statistics
		this.statistics = statistics;

		// Protocol Handler pipeline
		this.rtcpHandler = new RtcpHandler(statistics);
	}
	
	public int getLocalPort() {
		return this.channel != null ? this.channel.socket().getLocalPort() : 0;
	}

	public void setRemotePeer(SocketAddress remotePeer) {
		this.remotePeer = remotePeer;

		if (this.channel != null && this.channel.isConnected()) {
			try {
				this.channel.disconnect();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

			boolean connectNow = this.udpManager
					.connectImmediately((InetSocketAddress) remotePeer);
			if (connectNow) {
				try {
					this.channel.connect(remotePeer);
				} catch (IOException e) {
					logger.info("Can not connect to remote address , please check that you are not using local address - 127.0.0.X to connect to remote");
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	public boolean isBound() {
		return bound;
	}

	private void onBinding() {
		// Protocol Handler pipeline
		this.rtcpHandler.setChannel(this.channel);
		this.rtcpHandler.joinRtpSession();
		this.handlers.addHandler(this.rtcpHandler);
	}

	/**
	 * Binds the channel to an address and port
	 * 
	 * @param isLocal
	 *            whether the connection is local or not
	 * @param port
	 *            The RTCP port. Usually the RTP channel gets the even port and
	 *            RTCP channel get the next port.
	 * @throws SocketException
	 *             When the channel cannot be openend or bound
	 */
	public void bind(boolean isLocal, int port) throws SocketException {
		try {
			// Open this channel with UDP Manager on first available address
			this.channel = (DatagramChannel) udpManager.open(this).channel();
		} catch (IOException e) {
			throw new SocketException(e.getMessage());
		}

		// activate media elements
		onBinding();

		// bind data channel
		this.udpManager.bind(this.channel, port, isLocal);
		this.bound = true;
	}

	public void bind(DatagramChannel channel) throws SocketException {
		// External channel must be bound already
		if (!channel.socket().isBound()) {
			throw new SocketException("Datagram channel is not bound!");
		}

		try {
			// Register the channel on UDP Manager
			this.channel = (DatagramChannel) udpManager.open(channel, this).channel();
		} catch (IOException e) {
			throw new SocketException(e.getMessage());
		}

		// activate media elements
		onBinding();
		this.bound = true;
	}
	
	@Override
	public void close() {
		this.rtcpHandler.leaveRtpSession();
		super.close();
	}

}
