/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
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

package org.mobicents.media.server.mgcp.connection;

import java.io.IOException;
import java.net.SocketException;

import org.apache.log4j.Logger;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.CnameGenerator;
import org.mobicents.media.server.impl.rtp.RtpListener;
import org.mobicents.media.server.impl.rtp.channels.AudioChannel;
import org.mobicents.media.server.impl.rtp.sdp.SdpFactory;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SessionDescription;
import org.mobicents.media.server.io.sdp.SessionDescriptionParser;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.spi.pooling.PooledObject;
import org.mobicents.media.server.utils.Text;

/**
 * 
 * @author Oifa Yulian
 * @author Amit Bhayani
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see BaseConnection
 */
public class RtpConnectionImpl extends BaseConnection implements RtpListener, PooledObject {

	private static final Logger logger = Logger.getLogger(RtpConnectionImpl.class);

	// Core elements
	private final ChannelsManager channelsManager;

	// RTP session elements
	private String cname;
	private boolean outbound;
	private boolean local;

	// Media Channels
	private AudioChannel audioChannel;

	// Session Description
	private SessionDescription localSdp;
	private SessionDescription remoteSdp;

	// Listeners
	private ConnectionFailureListener connectionFailureListener;

	/**
	 * Constructs a new RTP connection with one audio channel.
	 * 
	 * @param id
	 *            The unique ID of the connection
	 * @param channelsManager
	 *            The media channel provider
	 * @param dspFactory
	 *            The DSP provider
	 */
	public RtpConnectionImpl(int id, ChannelsManager channelsManager, DspFactory dspFactory) {
		// Core elements
		super(id, channelsManager.getScheduler());
		this.channelsManager = channelsManager;

		// Connection state
		this.outbound = false;
		this.local = false;
		this.cname = CnameGenerator.generateCname();

		// Audio Channel
		this.audioChannel = this.channelsManager.getAudioChannel();
		this.audioChannel.setCname(this.cname);
		try {
			this.audioChannel.setInputDsp(dspFactory.newProcessor());
			this.audioChannel.setOutputDsp(dspFactory.newProcessor());
		} catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
			// exception may happen only if invalid classes have been set in configuration
			throw new RuntimeException("There are invalid classes specified in the configuration.", e);
		}
	}

	@Override
	public void generateCname() {
		this.cname = CnameGenerator.generateCname();
		if (this.audioChannel != null) {
			this.audioChannel.setCname(this.cname);
		}
	}

	@Override
	public String getCname() {
		return this.cname;
	}

	@Override
	public AudioComponent getAudioComponent() {
		return this.audioChannel.getAudioComponent();
	}

	@Override
	public OOBComponent getOOBComponent() {
		return this.audioChannel.getAudioOobComponent();
	}

	@Override
	public boolean getIsLocal() {
		return this.local;
	}

	@Override
	public void setIsLocal(boolean isLocal) {
		this.local = isLocal;
	}

	@Override
	public void setOtherParty(Connection other) throws IOException {
		throw new IOException("Applicable only for a local connection");
	}

	@Override
	public void setOtherParty(byte[] descriptor) throws IOException {
		try {
			this.remoteSdp = SessionDescriptionParser.parse(new String(
					descriptor));
			setOtherParty();
		} catch (SdpException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void setOtherParty(Text descriptor) throws IOException {
		setOtherParty(descriptor.toString().getBytes());
	}

	/**
	 * Sets the remote peer based on the received remote SDP description.
	 * 
	 * <p>
	 * The connection will be setup according to the SDP, specifically whether
	 * the call is WebRTC (with ICE enabled) or not.
	 * </p>
	 * 
	 * <p>
	 * <b>SIP call:</b><br>
	 * The RTP audio channel can be immediately bound to the configured bind
	 * address.<br>
	 * By reading connection fields of the SDP offer, we can set the remote peer
	 * of the audio channel.
	 * </p>
	 * 
	 * <p>
	 * <b>WebRTC call:</b><br>
	 * An ICE-lite agent is created. This agent assumes a controlled role, and
	 * starts listening for connectivity checks. This agent is responsible for
	 * providing a socket for DTLS hanshake and RTP streaming, once the
	 * connection is established with the remote peer.<br>
	 * So, we can only bind the audio channel and set its remote peer once the
	 * ICE agent has selected the candidate pairs and made such socket
	 * available.
	 * </p>
	 * 
	 * @throws IOException
	 *             If an error occurs while setting up the remote peer
	 */
	private void setOtherParty() throws IOException {
		if (this.outbound) {
			setOtherPartyOutboundCall();
		} else {
			setOtherPartyInboundCall();
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Audio formats: " + this.audioChannel.getFormatMap());
		}
	}

	/**
	 * Sets the remote peer based on the remote SDP description.
	 * 
	 * <p>
	 * In this case, the connection belongs to an outbound call. So, the remote
	 * SDP is the offer and, as result, the proper answer is generated.<br>
	 * The SDP answer can be sent later to the remote peer.
	 * </p>
	 * 
	 * @throws IOException
	 *             If an error occurs while setting up the remote peer
	 */
	private void setOtherPartyInboundCall() throws IOException {
		// Setup the audio channel based on remote offer
		MediaDescriptionField remoteAudio = this.remoteSdp
				.getMediaDescription("audio");
		if (remoteAudio != null) {
			this.audioChannel.open();
			setupAudioChannelInbound(remoteAudio);
		}

		boolean isWebRtc = this.remoteSdp.containsIce();

		// Generate SDP answer
		String bindAddress = this.local ? this.channelsManager
				.getLocalBindAddress() : this.channelsManager.getBindAddress();
		String externalAddress = isWebRtc ? this.channelsManager.getUdpManager().getWebRTCAddress() : this.channelsManager.getUdpManager().getExternalAddress();
		if (this.audioChannel.isOpen()) {
			this.localSdp = SdpFactory.buildSdp(false, bindAddress, externalAddress,
					this.audioChannel);
		} else {
			// In case remote peer did not offer audio channel
			this.localSdp = SdpFactory.buildSdp(false, bindAddress, externalAddress);
		}

		// Reject any channels other than audio
		MediaDescriptionField remoteVideo = this.remoteSdp
				.getMediaDescription("video");
		if (remoteVideo != null) {
			SdpFactory.rejectMediaField(this.localSdp, remoteVideo);
		}

		MediaDescriptionField remoteApplication = this.remoteSdp
				.getMediaDescription("application");
		if (remoteApplication != null) {
			SdpFactory.rejectMediaField(this.localSdp, remoteApplication);
		}

		// Change the state of this RTP connection from HALF_OPEN to OPEN
		try {
			this.join();
		} catch (Exception e) {
			// exception is possible here when already joined
			logger.warn("Could not set connection state to OPEN", e);
		}
	}

    /**
     * Sets the remote peer based on the remote SDP description.
     * 
     * <p>
     * In this case, the connection belongs to an inbound call. So, the remote SDP is the answer which implies that this
     * connection already generated the proper offer.
     * </p>
     * 
     * @throws IOException If an error occurs while setting up the remote peer
     */
    private void setOtherPartyOutboundCall() throws IOException {
        // Setup audio channel
        MediaDescriptionField remoteAudio = this.remoteSdp.getMediaDescription("audio");
        if (remoteAudio != null) {
            // Set remote DTLS fingerprint
            if(this.audioChannel.isDtlsEnabled()) {
                FingerprintAttribute fingerprint = remoteAudio.getFingerprint();
                this.audioChannel.setRemoteFingerprint(fingerprint.getHashFunction(), fingerprint.getFingerprint());
            }
            setupAudioChannelOutbound(remoteAudio);
        }

        // Change the state of this RTP connection from HALF_OPEN to OPEN
        try {
            this.join();
        } catch (Exception e) {
            // exception is possible here when already joined
            logger.warn("Could not set connection state to OPEN", e);
        }
    }

	/**
	 * Reads the remote SDP offer and sets up the available resources according
	 * to the call type.
	 * 
	 * <p>
	 * In case of a WebRTC call, an ICE-lite agent is created. The agent will
	 * start listening for connectivity checks from the remote peer.<br>
	 * Also, a WebRTC handler will be enabled on the corresponding audio
	 * channel.
	 * </p>
	 * 
	 * @param remoteAudio
	 *            The description of the remote audio channel.
	 * 
	 * @throws IOException
	 *             When binding the audio data channel. Non-WebRTC calls only.
	 * @throws SocketException
	 *             When binding the audio data channel. Non-WebRTC calls only.
	 */
    private void setupAudioChannelInbound(MediaDescriptionField remoteAudio) throws IOException {
        // Negotiate audio codecs
        this.audioChannel.negotiateFormats(remoteAudio);
        if (!this.audioChannel.containsNegotiatedFormats()) {
            throw new IOException("Audio codecs were not supported");
        }

        // Bind audio channel to an address provided by UdpManager
        this.audioChannel.bind(this.local, remoteAudio.isRtcpMux());

        boolean enableIce = remoteAudio.containsIce();
        if (enableIce) {
            // Enable ICE. Wait for ICE handshake to finish before connecting RTP/RTCP channels
            this.audioChannel.enableICE(this.channelsManager.getExternalAddress(), remoteAudio.isRtcpMux());
        } else {
            String remoteAddr = remoteAudio.getConnection().getAddress();
            this.audioChannel.connectRtp(remoteAddr, remoteAudio.getPort());
            this.audioChannel.connectRtcp(remoteAddr, remoteAudio.getRtcpPort());
        }

        // Enable DTLS according to remote SDP description
        boolean enableDtls = this.remoteSdp.containsDtls();
        if (enableDtls) {
            FingerprintAttribute fingerprint = this.remoteSdp.getFingerprint(audioChannel.getMediaType());
            this.audioChannel.enableDTLS(fingerprint.getHashFunction(), fingerprint.getFingerprint());
        }
    }

	/**
	 * Reads the remote SDP answer and sets up the proper media channels.
	 * 
	 * @param remoteAudio
	 *            The description of the remote audio channel.
	 * 
	 * @throws IOException
	 *             When binding the audio data channel. Non-WebRTC calls only.
	 * @throws SocketException
	 *             When binding the audio data channel. Non-WebRTC calls only.
	 */
	private void setupAudioChannelOutbound(MediaDescriptionField remoteAudio)
			throws IOException {
	    // Negotiate audio codecs
		this.audioChannel.negotiateFormats(remoteAudio);
		if (!this.audioChannel.containsNegotiatedFormats()) {
			throw new IOException("Audio codecs were not supported");
		}

		// connect to remote peer - RTP
		String remoteRtpAddress = remoteAudio.getConnection().getAddress();
		int remoteRtpPort = remoteAudio.getPort();
		
		// only connect is calls are plain old SIP
		// For WebRTC cases, the ICE Agent must connect upon candidate selection
		boolean connectNow = !(this.outbound && audioChannel.isIceEnabled());
        if (connectNow) {
            this.audioChannel.connectRtp(remoteRtpAddress, remoteRtpPort);
            // connect to remote peer - RTCP
            boolean remoteRtcpMux = remoteAudio.isRtcpMux();
            if (remoteRtcpMux) {
                this.audioChannel.connectRtcp(remoteRtpAddress, remoteRtpPort);
            } else {
                RtcpAttribute remoteRtcp = remoteAudio.getRtcp();
                if (remoteRtcp == null) {
                    // No specific RTCP port, so default is RTP port + 1
                    this.audioChannel.connectRtcp(remoteRtpAddress, remoteRtpPort + 1);
                } else {
                    // Specific RTCP address and port contained in SDP
                    String remoteRtcpAddress = remoteRtcp.getAddress();
                    if (remoteRtcpAddress == null) {
                        // address is optional in rtcp attribute
                        // will match RTP address if not defined
                        remoteRtcpAddress = remoteRtpAddress;
                    }
                    int remoteRtcpPort = remoteRtcp.getPort();
                    this.audioChannel.connectRtcp(remoteRtcpAddress, remoteRtcpPort);
                }
            }
        }
	}

	@Override
	public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
		this.audioChannel.setConnectionMode(mode);
		super.setMode(mode);
	}

	@Override
	public String getDescriptor() {
		return (this.localSdp == null) ? "" : this.localSdp.toString();
	}

    @Override
    public void generateOffer(boolean webrtc) throws IOException {
        // Only open and bind a new channel if not currently configured
        if (!this.audioChannel.isOpen()) {
            // call is outbound since the connection is generating the offer
            this.outbound = true;

            // setup audio channel
            this.audioChannel.open();
            this.audioChannel.bind(this.local, webrtc);

            if (webrtc) {
                this.audioChannel.enableICE(this.channelsManager.getExternalAddress(), true);
                this.audioChannel.enableDTLS();
            }

            // generate SDP offer based on audio channel
            String bindAddress = this.local ? this.channelsManager.getLocalBindAddress() : this.channelsManager.getBindAddress();
            String externalAddress = webrtc ? this.channelsManager.getUdpManager().getWebRTCAddress() : this.channelsManager.getUdpManager().getExternalAddress();
            this.localSdp = SdpFactory.buildSdp(true, bindAddress, externalAddress, this.audioChannel);
            this.remoteSdp = null;
        }
    }

	@Override
	public String getLocalDescriptor() {
		return (this.localSdp == null) ? "" : this.localSdp.toString();
	}

	@Override
	public String getRemoteDescriptor() {
		return (this.remoteSdp == null) ? "" : this.remoteSdp.toString();
	}

	public long getPacketsReceived() {
		return this.audioChannel.getPacketsReceived();
	}

	@Override
	public long getBytesReceived() {
		return this.audioChannel.getOctetsReceived();
	}

	@Override
	public long getPacketsTransmitted() {
		return this.audioChannel.getPacketsSent();
	}

	@Override
	public long getBytesTransmitted() {
		return this.audioChannel.getOctetsSent();
	}

	@Override
	public double getJitter() {
		return this.audioChannel.getJitter();
	}

	@Override
	public boolean isAvailable() {
		return this.audioChannel.isAvailable();
	}

	@Override
	public String toString() {
		return "RTP Connection [" + getEndpoint().getLocalName() + "]";
	}

	/**
	 * Closes any active resources (like media channels) associated with the
	 * connection.
	 */
	private void closeResources() {
		if (this.audioChannel.isOpen()) {
			this.audioChannel.close();
		}
	}

	/**
	 * Resets the state of the connection.
	 */
	private void reset() {
		// Reset SDP
		this.outbound = false;
		this.localSdp = null;
		this.remoteSdp = null;
	}

	@Override
	public void onRtpFailure(String message) {
		if (this.audioChannel.isOpen()) {
			logger.warn(message);
			// RTP is mandatory, if it fails close everything
			onFailed();
		}
	}

	@Override
	public void onRtpFailure(Throwable e) {
		String message = "RTP failure!";
		if (e != null) {
			message += " Reason: " + e.getMessage();
		}
		onRtpFailure(message);
	}

	@Override
	public void onRtcpFailure(String e) {
		if (this.audioChannel.isOpen()) {
			logger.warn(e);
			// Close the RTCP channel only
			// Keep the RTP channel open because RTCP is not mandatory
			onFailed();
		}
	}

	@Override
	public void onRtcpFailure(Throwable e) {
		String message = "RTCP failure!";
		if (e != null) {
			message += " Reason: " + e.getMessage();
		}
		onRtcpFailure(message);
	}

	@Override
	public void setConnectionFailureListener(
			ConnectionFailureListener connectionFailureListener) {
		this.connectionFailureListener = connectionFailureListener;
	}

	@Override
	protected void onCreated() throws Exception {
		// Reset components so they can be re-used in new calls
		reset();
	}

	@Override
	protected void onFailed() {
		closeResources();
		if (this.connectionFailureListener != null) {
			this.connectionFailureListener.onFailure();
		}
	}

	@Override
	protected void onOpened() throws Exception {
		// TODO not implemented
	}

	@Override
	protected void onClosed() {
		closeResources();
		try {
			setMode(ConnectionMode.INACTIVE);
		} catch (ModeNotSupportedException e) {
			logger.warn("Could not set connection mode to INACTIVE.", e);
		}
		releaseConnection(ConnectionType.RTP);
		this.connectionFailureListener = null;
	}

    @Override
    public void checkIn() {
        closeResources();
        reset();
    }

    @Override
    public void checkOut() {
        generateCname();
    }

}
