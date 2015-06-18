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

package org.mobicents.media.server.impl.rtcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.mobicents.media.io.ice.IceAuthenticator;
import org.mobicents.media.io.ice.network.stun.StunHandler;
import org.mobicents.media.server.impl.rtp.RtpListener;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.impl.srtp.DtlsListener;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.channel.MultiplexedChannel;
import org.mobicents.media.server.utils.Text;

/**
 * Channel for exchanging RTCP traffic
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpChannel extends MultiplexedChannel implements DtlsListener {

	private static final Logger logger = Logger.getLogger(RtcpChannel.class);

	// Core elements
	private final UdpManager udpManager;

	// Channel attribute
	private int channelId;
	private boolean bound;

	// Protocol handler pipeline
	private static final int STUN_PRIORITY = 2; // a packet each 400ms
	private static final int RTCP_PRIORITY = 1; // a packet each 5s
	
	private RtcpHandler rtcpHandler;
	private DtlsHandler dtlsHandler;
	private StunHandler stunHandler;
	
	// WebRTC
	private boolean secure;
	
	// Listeners
	private RtpListener rtpListener;

	public RtcpChannel(int channelId, RtpStatistics statistics, UdpManager udpManager) {
		// Initialize MultiplexedChannel elements
		super();

		// Core elements
		this.udpManager = udpManager;

		// Channel attributes
		this.channelId = channelId;
		this.bound = false;

		// Protocol Handler pipeline
		this.rtcpHandler = new RtcpHandler(statistics);
		
		// WebRTC
		this.secure = false;
	}

	public void setRemotePeer(SocketAddress remotePeer) {
		if (this.dataChannel != null) {
			if (this.dataChannel.isConnected()) {
				try {
					this.dataChannel.disconnect();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}

			boolean connectNow = this.udpManager.connectImmediately((InetSocketAddress) remotePeer);
			if (connectNow) {
				try {
					this.dataChannel.connect(remotePeer);
				} catch (IOException e) {
					logger.error("Can not connect to remote address. Check that you are not using local address (127.0.0.X)", e);
				}
			}
		}
	}
	
	public void setRtpListener(RtpListener rtpListener) {
		this.rtpListener = rtpListener;
	}
	
	public boolean isAvailable() {
		// The channel is available is is connected
		boolean available = this.dataChannel != null && this.dataChannel.isConnected();
		// In case of WebRTC calls the DTLS handshake must be completed
		if(this.secure) {
			available = available && this.dtlsHandler.isHandshakeComplete();
		}
		return available;
	}

	public boolean isBound() {
		return bound;
	}

	private void onBinding() {
		// Set protocol handler priorities
		this.rtcpHandler.setPipelinePriority(RTCP_PRIORITY);
		if(this.secure) {
			this.stunHandler.setPipelinePriority(STUN_PRIORITY);
		}
		
		// Protocol Handler pipeline
		this.rtcpHandler.setChannel(this.dataChannel);
		this.handlers.addHandler(this.rtcpHandler);

		if(this.secure) {
			this.dtlsHandler.setChannel(this.dataChannel);
			this.dtlsHandler.addListener(this);
			this.handlers.addHandler(this.stunHandler);
			
			// Start DTLS handshake
			this.dtlsHandler.handshake();
		} else {
			this.rtcpHandler.joinRtpSession();
		}
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
			this.selectionKey = udpManager.open(this);
			this.dataChannel = (DatagramChannel) this.selectionKey.channel();
		} catch (IOException e) {
			throw new SocketException(e.getMessage());
		}

		// activate media elements
		onBinding();

		// bind data channel
		this.udpManager.bind(this.dataChannel, port, isLocal);
		this.bound = true;
	}

	public void bind(DatagramChannel channel) throws SocketException {
		// External channel must be bound already
		if (!channel.socket().isBound()) {
			throw new SocketException("Datagram channel is not bound!");
		}

		try {
			// Register the channel on UDP Manager
			this.selectionKey = udpManager.open(channel, this);
			this.dataChannel = channel;
		} catch (IOException e) {
			throw new SocketException(e.getMessage());
		}

		// activate media elements
		onBinding();
		this.bound = true;
	}
	
	/**
	 * Checks whether the channel is secure or not.
	 * 
	 * @return Whether the channel handles regular RTCP traffic or SRTCP (secure).
	 */
	public boolean isSecure() {
		return secure;
	}
	
	public void enableSRTCP(String hashFunction, String remotePeerFingerprint, IceAuthenticator authenticator) {
		this.secure = true;
		
		// setup the DTLS handler
		if(this.dtlsHandler == null) {
			this.dtlsHandler = new DtlsHandler(this.dataChannel);
		}
		this.dtlsHandler.setRemoteFingerprint(hashFunction, remotePeerFingerprint);
		
		// setup the SRTCP handler
		this.rtcpHandler.enableSRTCP(this.dtlsHandler);

		// setup the STUN handler
		if (this.stunHandler == null) {
			this.stunHandler = new StunHandler(authenticator);
		} else {
			this.stunHandler.setIceAuthenticator(authenticator);
		}
		this.handlers.addHandler(stunHandler);
	}

	public void disableSRTCP() {
		this.secure = false;
		
		// setup the DTLS handler
		if(this.dtlsHandler != null) {
			this.dtlsHandler.setRemoteFingerprint("", "");
		}
		
		// setup the SRTCP handler
		this.rtcpHandler.disableSRTCP();
		
		// setup the STUN handler
		if (this.stunHandler != null) {
			this.handlers.removeHandler(this.stunHandler);
		}
	}
	
	public Text getDtlsLocalFingerprint() {
		if(this.secure) {
			return this.dtlsHandler.getLocalFingerprint();
		}
		return new Text("");
	}
	
	@Override
	public void close() {
		/*
		 * Instruct the RTCP handler to leave the RTP session.
		 * 
		 * This will result in scheduling an RTCP BYE to be sent. Since the BYE
		 * is not sent right away, the datagram channel can only be closed once
		 * the BYE has been sent. So, the handler is responsible for closing the
		 * channel.
		 */
		this.rtcpHandler.leaveRtpSession();
		this.bound = false;
		super.close();
	}
	
	public void reset() {
		this.rtcpHandler.reset();
		if(this.secure) {
			this.dtlsHandler.reset();
		}
	}

	
	public void onDtlsHandshakeComplete() {
		logger.info("DTLS handshake completed for RTCP candidate.\nJoining RTP session.");
		this.rtcpHandler.joinRtpSession();
	}

	public void onDtlsHandshakeFailed(Throwable e) {
		if(this.rtpListener != null) {
			this.rtpListener.onRtcpFailure(e);
		}
	}
	
}
