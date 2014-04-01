package org.mobicents.media.server.impl.srtp;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.security.SecureRandom;

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

	private static final int MTU = 1500;

	private DtlsSrtpServer server;
	private DatagramChannel channel;
	private boolean handshakeComplete;

	private Text localFingerprint;
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

	public DtlsHandler(DatagramChannel channel) {
		this.server = new DtlsSrtpServer();
		this.channel = channel;
		this.handshakeComplete = false;
		this.localFingerprint = new Text(server.getFingerprint());
	}

	public DtlsHandler() {
		this.server = new DtlsSrtpServer();
		this.handshakeComplete = false;
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

	public Text getLocalFingerprint() {
		return new Text(this.server.getFingerprint());
		//return localFingerprint;
	}

	public void setLocalFingerprint(Text localFingerprint) {
		this.localFingerprint = localFingerprint;
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
	 * Decodes an RTP Packet
	 * 
	 * @param packet
	 *            The encoded RTP packet
	 * @return The decoded RTP packet
	 */
	public RtpPacket decode(RtpPacket packet) {
		return this.srtpDecoder.reverseTransform(packet);
	}

	/**
	 * Encodes an RTP packet
	 * 
	 * @param packet
	 *            The decoded RTP packet
	 * @return The encoded RTP packet
	 */
	public RtpPacket encode(RtpPacket packet) {
		return this.srtpEncoder.transform(packet);
	}

	public void handshake() throws IOException {
		SecureRandom secureRandom = new SecureRandom();
		DTLSServerProtocol serverProtocol = new DTLSServerProtocol(secureRandom);
		NioUdpTransport transport = new NioUdpTransport(getChannel(), MTU);
		
		// Perform the handshake in a NIO fashion
		serverProtocol.accept(this.server, transport);
		
		// Prepare the shared key to be used in RTP streaming
		server.prepareSrtpSharedSecret();

		// Generate encoders for DTLS traffic
		this.srtpDecoder = generateDecoder();
		this.srtpEncoder = generateEncoder();
		
		// Declare handshake as complete
		this.handshakeComplete = true;
	}

}
