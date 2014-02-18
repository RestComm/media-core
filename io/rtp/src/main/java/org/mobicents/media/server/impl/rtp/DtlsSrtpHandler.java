package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.bouncycastle.crypto.tls.DTLSServerProtocol;
import org.bouncycastle.crypto.tls.DatagramTransport;
import org.bouncycastle.crypto.tls.UDPTransport;
import org.mobicents.media.server.impl.rtp.crypto.DtlsSrtpServer;
import org.mobicents.media.server.impl.rtp.crypto.PacketTransformer;
import org.mobicents.media.server.impl.rtp.crypto.SRTPPolicy;
import org.mobicents.media.server.impl.rtp.crypto.SRTPTransformEngine;
import org.mobicents.media.server.utils.Text;

/**
 * Handler responsible for the DTLS handshake between two endpoints.<br>
 * It also provides methods to encode or decode RTP packets using keying
 * material resulting from the DTLS handshake.
 * 
 * @author Henrique Rosa
 * @author Ivelin Ivanov
 * 
 */
public class DtlsSrtpHandler {

	private static final int MTU = 1500;

	private DatagramSocket socket;
	private DatagramChannel channel;
	private DtlsSrtpServer server;

	// TODO calculate fingerprint using generated certificate - hrosa
	private Text localFingerprint = new Text(
			"sha-256 28:D5:4A:00:0E:4A:53:F9:DC:57:67:17:49:BC:E2:85:24:A3:52:70:99:76:48:B8:72:11:BB:DF:14:A7:4D:3B");
	private Text remoteFingerprint;

	private boolean handshakeComplete;
	private List<HandshakeCompleteListener> handshakeListeners;
	private Object handshakeLock = new Object();

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

	/**
	 * Creates a new DTLS-SRTP handler to perform the handshake and
	 * encode/decode RTP packets.
	 * 
	 * @param channel
	 *            The datagram channel the will be blocked during the handshake.
	 * @param socket
	 *            The UDP socket used to establish the handshake.
	 */
	public DtlsSrtpHandler(final DatagramChannel channel) {
		if (channel != null) {
			this.channel = channel;
			this.socket = channel.socket();
		}
		this.server = new DtlsSrtpServer();
		this.handshakeComplete = false;
		this.handshakeListeners = new ArrayList<HandshakeCompleteListener>();
	}

	public DtlsSrtpHandler() {
		this.server = new DtlsSrtpServer();
		this.handshakeComplete = false;
		this.handshakeListeners = new ArrayList<HandshakeCompleteListener>();
	}

	public void setChannel(DatagramChannel channel) {
		this.channel = channel;
		this.socket = channel.socket();
	}

	public Text getLocalFingerprint() {
		return localFingerprint;
	}

	public void setLocalFingerprint(Text localFingerprint) {
		this.localFingerprint = localFingerprint;
	}

	public Text getRemoteFingerprint() {
		return remoteFingerprint;
	}

	public void setRemoteFingerprint(Text remoteFingerprint) {
		this.remoteFingerprint = remoteFingerprint;
	}

	public PacketTransformer getSrtpDecoder() {
		return srtpDecoder;
	}

	public PacketTransformer getSrtpEncoder() {
		return srtpEncoder;
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

	public boolean isHandshakeComplete() {
		return handshakeComplete;
	}

	/**
	 * Performs DTLS handshake and prepares keying material for the encrypted
	 * SRTP channel
	 * 
	 * @throws IOException
	 *             An error occurred while blocking/unblocking the Datagram
	 *             Channel
	 */
	public void handshake() throws IOException {
		SecureRandom secureRandom = new SecureRandom();
		DTLSServerProtocol serverProtocol = new DTLSServerProtocol(secureRandom);

		// Block UDP channel momentarily
		this.channel.configureBlocking(true);

		// Perform DTLS handshake
		DatagramTransport transport = new LoggingDatagramTransport(
				new UDPTransport(socket, MTU), System.out);
		serverProtocol.accept(server, transport);

		// Unblock UDP channel
		channel.configureBlocking(false);

		// Prepare the shared key to be used in RTP streaming
		server.prepareSrtpSharedSecret();

		// Setup encoders for RTP streaming
		this.srtpEncoder = generateEncoder();
		this.srtpDecoder = generateDecoder();

		fireHandshakeComplete(new HandshakeCompleteEvent(this));
	}

	private void fireHandshakeComplete(HandshakeCompleteEvent event) {
		HandshakeCompleteListener[] listeners;
		synchronized (handshakeLock) {
			listeners = this.handshakeListeners
					.toArray(new HandshakeCompleteListener[this.handshakeListeners
							.size()]);
		}

		this.handshakeComplete = true;
		for (HandshakeCompleteListener listener : listeners) {
			listener.onHandshakeComplete(event);
		}
	}

	/**
	 * Generates an SRTP encoder for outgoing RTP packets using keying material
	 * from the DTLS handshake.
	 */
	private PacketTransformer generateEncoder() {
		return new SRTPTransformEngine(getMasterServerKey(),
				getMasterServerSalt(), getSrtpPolicy(), getSrtcpPolicy())
				.getRTPTransformer();
	}

	/**
	 * Generates an SRTP decoder for incoming RTP packets using keying material
	 * from the DTLS handshake.
	 */
	private PacketTransformer generateDecoder() {
		return new SRTPTransformEngine(getMasterClientKey(),
				getMasterClientSalt(), getSrtpPolicy(), getSrtcpPolicy())
				.getRTPTransformer();
	}

	/**
	 * Decodes a received RTP Packet
	 * 
	 * @param packet
	 *            The encoded RTP packet
	 * @return The decoded RTP packet
	 */
	public RtpPacket decode(RtpPacket packet) {
		return this.srtpDecoder.reverseTransform(packet);
	}

	/**
	 * Encodes a RTP packet to be transmitted
	 * 
	 * @param packet
	 *            The decoded RTP packet
	 * @return The encoded RTP packet
	 */
	public RtpPacket encode(RtpPacket packet) {
		return this.srtpEncoder.transform(packet);
	}

	public interface HandshakeCompleteListener {

		void onHandshakeComplete(HandshakeCompleteEvent event);
	}

	public class HandshakeCompleteEvent extends EventObject {

		private static final long serialVersionUID = -8877042657692986391L;

		public HandshakeCompleteEvent(DtlsSrtpHandler source) {
			super(source);
		}
	}

}
