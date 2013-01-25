/*
 * Telestax, Open Source Cloud Communications
 * Copyright 2013, Telestax, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.server.impl.rtp;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import net.ripe.hadoop.pcap.PcapReader;
import net.ripe.hadoop.pcap.packet.Packet;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.network.ProtocolHandler;
import org.mobicents.media.server.io.network.UdpManager;

/**
 * 
 * This utility class reads RTP packets from a pcap file and sends them to a UDP
 * address:port
 * 
 * @author Oifa Yulian
 * @author Ivelin Ivanov <ivelin.ivanov@telestax.com>
 */
public final class PcapRtpPlayer {
	private final static int PORT_ANY = -1;

	// UDP channels
	private DatagramChannel dataChannel;

	private RTPHandler rtpHandler;

	private UdpManager udpManager;
	
	InputStream pcapInputStream = null;
	
	// iterator over the packets in a pcap file
	private Iterator<Packet> pcapPacketIterator = null;

	private PlayerListener playerListener;

	private Logger logger = Logger.getLogger(PcapRtpPlayer.class);

	// flags whether the player is closed for further activities
	private boolean isPlayerClosed = false;

	private String pcapFilePath;
	
	// The timestamp in microseconds of the last pcap packet
	// we use this to ensure that packets are played at the same
	// pace as they were recorded.
	private long	 lastPacketPcapTimestamp = 0;
	
	// The playback timestamp in microseconds for the last played packet
	// this timestamp is matched to the pcap timestamp of the same packet 
	// in order to ensure consistent packet time spacing between recording and playback.
	// The initial value ensures sufficient distance to let the first packet through  
	private long lastPacketPlaybackTimestamp = -1*0xFFFFFFFFL; 
	
	// while the current playback time distance between packets has not reached 
	// the recorded time distance, we suspend playback temporarily 
	private Packet suspendedPacket = null;



	/**
	 * Create Pcap RTP Player instance.
	 * 
	 * @param channelManager
	 *            Channel manager
	 * @throws IOException 
	 * 
	 */
	public PcapRtpPlayer(UdpManager theUdpManager, String thePcapFilePath) throws IOException {
		// open data channel
		rtpHandler = new RTPHandler();
		this.udpManager = theUdpManager;
		this.pcapFilePath = thePcapFilePath;
	}
	
	/**
	 * Binds channel to the first available port.
	 * 
	 * @throws SocketException
	 */
	public void bind(boolean isLocal) throws IOException, SocketException {
		try {
			dataChannel = udpManager.open(rtpHandler);
		} catch (IOException e) {
			throw new SocketException(e.getMessage());
		}
		// bind data channel
		if (!isLocal) {
			udpManager.bind(dataChannel, PORT_ANY);
		} else {
			udpManager.bindLocal(dataChannel, PORT_ANY);
		}
	}

	/**
	 * Gets the port number to which this channel is bound.
	 * 
	 * @return the port number.
	 */
	public int getLocalPort() {
		return dataChannel != null ? dataChannel.socket().getLocalPort() : 0;
	}

	/**
	 * Sets the address of remote peer.
	 * 
	 * @param address
	 *            the address object.
	 */
	public void setPeer(SocketAddress address) {
		if (dataChannel != null) {
			if (dataChannel.isConnected())
				try {
					dataChannel.disconnect();
				} catch (IOException e) {
					logger.error(e);
				}

			try {
				dataChannel.connect(address);
			} catch (IOException e) {
				logger.info("Can not connect to remote address , "
						+ "please check that you are not using local address "
						+ "- 127.0.0.X to connect to remote");
				logger.error(e);
			}
		}
	}

	/**
	 * Begins reading from the pcap file and transmitting packets over UDP 
	 * @throws IOException 
	 */
	public void activate() throws IOException {
		try {
			if (pcapFilePath.endsWith(".cap")) { 
				pcapInputStream = this.getClass().getResourceAsStream(pcapFilePath);
			} else if (pcapFilePath.endsWith(".cap.gz") || pcapFilePath.endsWith(".cap.gzip")) {
				pcapInputStream = this.getClass().getResourceAsStream(pcapFilePath);
				if (pcapInputStream == null) {
					String warnMsg = String.format("Failed to open system resource: %s", pcapFilePath);
					logger.info(warnMsg);
					throw new IOException(warnMsg);
				}
				pcapInputStream = new GZIPInputStream(pcapInputStream);
			} else {
				String warnMsg = "File extension is unknown. Expected .cap, .cap.gz or .cap.gzip";
				throw new IOException(warnMsg);
			}
		} catch (IOException e) {
			if (pcapInputStream != null)
				pcapInputStream.close();
			throw new IOException(String.format("Unable to open %s", pcapFilePath));
		}
		String pcapFullPath = this.getClass().getResource(pcapFilePath).toString();
		logger.info(String.format("Opened pcap file : %s", pcapFullPath));
		
		DataInputStream dis = new DataInputStream(pcapInputStream);
		PcapReader pcapReader = new PcapRtpReader(dis);
		
		pcapPacketIterator = pcapReader.iterator();
	}
	
	/**
	 * Closes this player: udp socket, pcap file and any other external resources.
	 * Also notifies the registered listener (if any) that the player finished.
	 */
	public void close() {
		if (isPlayerClosed) return;
		logger.info("Closing pcap rtp player and related resources.");
		try {
			pcapInputStream.close();
			logger.info(String.format("Closing pcap file: %s", pcapFilePath));
		} catch (IOException e) {
			logger.warn("Failed to close pcap file", e);
		}
		if (dataChannel.isConnected())
			try {
				dataChannel.disconnect();
			} catch (IOException e) {
				logger.error("Failed to disconnect rtp datagram channel", e);
			}
		try {
			dataChannel.socket().close();
			dataChannel.close();
		} catch (IOException e) {
			logger.error("Failed to close rtp datagram channel", e);
		}
		if (playerListener != null) {
			playerListener.playFinished();
		}
		isPlayerClosed = true;
	}

	public void setPlayerListener(PlayerListener pl) {
		this.playerListener = pl;
	}
	
	/**
	 * Interface intended to allow notification that a pcap file 
	 * play completed.
	 * 
	 */
	public interface PlayerListener {
		public void playFinished();
	}
	
	/**
	 * Implements IO operations for RTP protocol.
	 * 
	 * This class is attached to channel and when channel is ready for IO the
	 * scheduler will call either receive or send.
	 */
	private class RTPHandler implements ProtocolHandler {

		/**
		 * (Non Java-doc.)
		 * 
		 * @see org.mobicents.media.server.io.network.ProtocolHandler#receive(java.nio.channels.DatagramChannel)
		 */
		public void receive(DatagramChannel channel) {
			// we are not doing anything with received UDP packets yet
		}

		public boolean isReadable() {
			return true;
		}

		public boolean isWriteable() {
			return true;
		}

		/**
		 * (Non Java-doc.)
		 * 
		 * @see org.mobicents.media.server.io.network.ProtocolHandler#send(java.nio.channels.DatagramChannel)
		 */
		public void send(DatagramChannel channel) {
			try {
				if (dataChannel.isConnected()) {
					byte[] nextRawRtpPacket = readNextPacketFromPcapFile();
					if (nextRawRtpPacket != null) {
						ByteBuffer bb = ByteBuffer.wrap(nextRawRtpPacket);
						SocketAddress remoteAddress = dataChannel
								.socket().getRemoteSocketAddress();
						dataChannel.send(bb, remoteAddress);
					}
				}
			} catch (PortUnreachableException e) {
				// icmp unreachable received
				// disconnect and wait for new packet
				logger.warn("Unable to send UDP packet", e);
				try {
					dataChannel.disconnect();
				} catch (IOException ex) {
					logger.error(ex);
				}
			} catch (IOException e) {
				logger.warn("Unable to send UDP packet", e);
			}
		}

		private byte[] readNextPacketFromPcapFile() {
			if (pcapPacketIterator == null) return null;
			Packet nextPcapPacket = null; 
			if (suspendedPacket != null) {
				nextPcapPacket = suspendedPacket;
			} else if (pcapPacketIterator.hasNext()) {
				nextPcapPacket = pcapPacketIterator.next();
			} else {
				// no more packets to send
				close();
				return null;
			};
				
			long nextPcapTimestampSeconds = (Long)nextPcapPacket.get(Packet.TIMESTAMP);
			long nextPcapTimestampMicros = (Long)nextPcapPacket.get(Packet.TIMESTAMP_MICROS);
			long nextPcapTimestamp = nextPcapTimestampSeconds*1000000L + nextPcapTimestampMicros;
			long nowMicros = System.nanoTime()/1000;
			if (nowMicros - lastPacketPlaybackTimestamp < nextPcapTimestamp - lastPacketPcapTimestamp) {
				// more time has to pass before we play the next packet
				// in order to preserve the packet time spacing from the pcap recording
				suspendedPacket = nextPcapPacket;
				logger.info("Suspending pcap packet playback for " + ((nextPcapTimestamp - lastPacketPcapTimestamp) - (nowMicros - lastPacketPlaybackTimestamp)) + " microseconds in order to simulate recorded pcap packet timestamp difference.");
				return null;
			} else {
				// its time to play(send) the next packet
				suspendedPacket = null;
				lastPacketPlaybackTimestamp = nowMicros;
				lastPacketPcapTimestamp = nextPcapTimestamp;
				byte[] pcapPacket = (byte[]) nextPcapPacket.get(PcapRtpReader.RTP_PACKET);
				// logger.info("Read next RTP packet from RTP file. Packet size: " + pcapPacket.length);
				return pcapPacket;
			}
		}

		public void setKey(SelectionKey key) {
		}

		public void onClosed() {
		}
		
	}

	/**
	 * Subclasses RcapReader to make the raw rtp packet available in 
	 * the Packet map  
	 * 
	 */
	private class PcapRtpReader extends PcapReader {

		private final static String RTP_PACKET = "rtp_packet";

		public PcapRtpReader(DataInputStream is) throws IOException {
			super(is);
		}
		
		protected void processPacketPayload(Packet packet, byte[] payload) {
			packet.put(PcapRtpReader.RTP_PACKET, payload);
		}
	}

}
