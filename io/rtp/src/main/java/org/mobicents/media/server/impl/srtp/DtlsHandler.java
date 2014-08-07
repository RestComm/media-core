package org.mobicents.media.server.impl.srtp;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;

import org.apache.log4j.Logger;
import org.bouncycastle.crypto.tls.DTLSServerProtocol;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.impl.rtp.crypto.DtlsSrtpServer;
import org.mobicents.media.server.impl.rtp.crypto.PacketTransformer;
import org.mobicents.media.server.impl.rtp.crypto.SRTPPolicy;
import org.mobicents.media.server.impl.rtp.crypto.SRTPTransformEngine;
import org.mobicents.media.server.utils.Text;

/**
 * Handler to process DTLS-SRTP packets
 * 
 * @author Henrique Rosa
 * 
 */
public class DtlsHandler {

	private static final Logger logger = Logger.getLogger("DTLS");
	
	private static final int MTU = 1500;

	private DtlsSrtpServer server;
	private DatagramChannel channel;
	private volatile boolean handshakeComplete;
	private volatile boolean handshaking;
	private Thread worker;
	private Text remoteFingerprint;
	

	/**
	 * Handles encryption of outbound RTP packets for a given RTP stream
	 * identified by its SSRC
	 * 
	 * @see http://tools.ietf.org/html/rfc5764#section-4.2
	 */
	private PacketTransformer srtpEncoder;

	/**
	 * Handles decryption of inbound RTP packets for a given RTP stream
	 * identified by its SSRC
	 * 
	 * @see http://tools.ietf.org/html/rfc5764#section-4.2
	 */
	private PacketTransformer srtpDecoder;

	public DtlsHandler(final DatagramChannel channel) {
		this.server = new DtlsSrtpServer();
		this.channel = channel;
		this.handshakeComplete = false;
		this.handshaking = false;
	}

	public DtlsHandler() {
		this(null);
	}

	public DatagramChannel getChannel() {
		return this.channel;
	}

	public void setChannel(DatagramChannel channel) {
		this.channel = channel;
	}

	public boolean isHandshakeComplete() {
		return handshakeComplete;
	}
	
	public boolean isHandshaking() {
		return handshaking;
	}

	public Text getLocalFingerprint() {
		return new Text(this.server.getFingerprint());
	}

	public Text getRemoteFingerprint() {
		return remoteFingerprint;
	}

	public void setRemoteFingerprint(Text remotePeerFingerprint) {
		this.remoteFingerprint = remotePeerFingerprint;
	}

	private byte[] getMasterServerKey() {
		return server.getSrtpMasterServerKey();
	}

	private byte[] getMasterServerSalt() {
		return server.getSrtpMasterServerSalt();
	}

	private byte[] getMasterClientKey() {
		return server.getSrtpMasterClientKey();
	}

	private byte[] getMasterClientSalt() {
		return server.getSrtpMasterClientSalt();
	}

	private SRTPPolicy getSrtpPolicy() {
		return server.getSrtpPolicy();
	}

	private SRTPPolicy getSrtcpPolicy() {
		return server.getSrtcpPolicy();
	}

	public PacketTransformer getSrtpDecoder() {
		return srtpDecoder;
	}

	public PacketTransformer getSrtpEncoder() {
		return srtpEncoder;
	}

	/**
	 * Generates an SRTP encoder for outgoing RTP packets using keying material
	 * from the DTLS handshake.
	 */
	private PacketTransformer generateEncoder() {
		return new SRTPTransformEngine(getMasterServerKey(), getMasterServerSalt(), getSrtpPolicy(), getSrtcpPolicy()).getRTPTransformer();
	}

	/**
	 * Generates an SRTP decoder for incoming RTP packets using keying material
	 * from the DTLS handshake.
	 */
	private PacketTransformer generateDecoder() {
		return new SRTPTransformEngine(getMasterClientKey(), getMasterClientSalt(), getSrtpPolicy(), getSrtcpPolicy()).getRTPTransformer();
	}

	/**
	 * Decodes an RTP Packet
	 * 
	 * @param packet
	 *            The encoded RTP packet
	 * @return The decoded RTP packet. Returns null is packet is not valid.
	 */
	public boolean decode(RtpPacket packet) {
		return this.srtpDecoder.reverseTransform(packet);
	}

	/**
	 * Encodes an RTP packet
	 * 
	 * @param packet
	 *            The decoded RTP packet
	 * @return The encoded RTP packet
	 */
	public boolean encode(RtpPacket packet) {
		return this.srtpEncoder.transform(packet);
	}

	public void handshake() {
		if(!handshaking && !handshakeComplete) {
			this.handshaking = true;
			if(this.worker == null) {
				this.worker = new Thread(new HandshakeWorker());
			}
			this.worker.start();
		}
	}
	
	private class HandshakeWorker implements Runnable {

		public void run() {
			SecureRandom secureRandom = new SecureRandom();
			DTLSServerProtocol serverProtocol = new DTLSServerProtocol(secureRandom);
			NioUdpTransport transport = new NioUdpTransport(getChannel(), MTU);
			
			try {
				// Perform the handshake in a non-blocking fashion
				logger.info("Started DTLS handshake");
				serverProtocol.accept(server, transport);

				// Prepare the shared key to be used in RTP streaming
				server.prepareSrtpSharedSecret();
				
				// Generate encoders for DTLS traffic
				srtpDecoder = generateDecoder();
				srtpEncoder = generateEncoder();
				
				// Declare handshake as complete
				handshakeComplete = true;
				logger.info("Completed DTLS handshake");
			} catch (IOException e) {
				handshakeComplete = false;
				logger.error("Could not perform DTLS handshake", e);
			} finally {
				handshaking = false;
			}
		}
		
	}

}
