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

package org.mobicents.media.core.connections;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.ParseException;

import javax.sdp.SdpException;

import org.apache.log4j.Logger;
import org.mobicents.media.core.MediaTypes;
import org.mobicents.media.core.SdpTemplate;
import org.mobicents.media.core.WebRTCSdpTemplate;
import org.mobicents.media.core.ice.IceAgent;
import org.mobicents.media.core.ice.IceException;
import org.mobicents.media.core.ice.IceFactory;
import org.mobicents.media.core.sdp.SdpNegotiator;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.RTPChannelListener;
import org.mobicents.media.server.impl.rtp.RTPDataChannel;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.rtp.sdp.MediaDescriptorField;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.sdp.SessionDescription;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.utils.Text;

/**
 * 
 * @author Oifa Yulian
 * @author amit bhayani
 * @author Henrique Rosa
 */
public class RtpConnectionImpl extends BaseConnection implements
		RTPChannelListener {
	private static final Logger logger = Logger
			.getLogger(RtpConnectionImpl.class);

	// Registered formats
	private final static AudioFormat DTMF_FORMAT = FormatFactory
			.createAudioFormat("telephone-event", 8000);
	private final static AudioFormat LINEAR_FORMAT = FormatFactory
			.createAudioFormat("LINEAR", 8000, 16, 1);

	// Audio Channel
	private ChannelsManager channelsManager;
	private RTPDataChannel rtpAudioChannel;
	private boolean isAudioCapabale;

	// Session Description
	private SessionDescription sdp = new SessionDescription();
	protected SdpTemplate offerTemplate;
	protected SdpTemplate answerTemplate;
	protected String sdpOffer;
	private String sdpAnswer;

	// Supported formats
	protected RTPFormats audioFormats;
	protected RTPFormats videoFormats;
	protected RTPFormats applicationFormats;

	// WebRTC
	private boolean webRtc;
	private IceAgent iceAgent;

	// Connection status
	private boolean isLocal = false;
	private ConnectionFailureListener connectionFailureListener;

	public RtpConnectionImpl(int id, ChannelsManager channelsManager,
			DspFactory dspFactory) {
		super(id, channelsManager.getScheduler());
		this.channelsManager = channelsManager;

		// check endpoint capabilities
		this.isAudioCapabale = true;

		// create audio and video channel
		this.rtpAudioChannel = channelsManager.getChannel();
		this.rtpAudioChannel.setRtpChannelListener(this);

		try {
			this.rtpAudioChannel.setInputDsp(dspFactory.newProcessor());
			this.rtpAudioChannel.setOutputDsp(dspFactory.newProcessor());
		} catch (Exception e) {
			// exception may happen only if invalid classes have been set in
			// config
			throw new RuntimeException(
					"There are probably invalid classes specified in the configuration.",
					e);
		}

		// create sdp template
		this.audioFormats = getRTPMap(AVProfile.audio);
		this.videoFormats = getRTPMap(AVProfile.video);
		this.applicationFormats = getRTPMap(AVProfile.application);

		offerTemplate = new SdpTemplate(this.audioFormats, this.videoFormats);
	}

	/**
	 * Gets the ICE agent for this connection.<br>
	 * An ICE agent is only available if the connection is WebRTC and only after
	 * the remote peer was set.
	 * 
	 * @return The ICE-lite agent for the WebRTC connection.
	 */
	public IceAgent getIceAgent() {
		return iceAgent;
	}

	/**
	 * Indicates whether this connection was set for a WebRTC call.<br>
	 * This can only be known after the SDP offer is received and processed.
	 * 
	 * @return Returns whether this connection addresses a WebRTC call.
	 */
	public boolean isWebRtc() {
		return webRtc;
	}

	public AudioComponent getAudioComponent() {
		return this.rtpAudioChannel.getAudioComponent();
	}

	public OOBComponent getOOBComponent() {
		return this.rtpAudioChannel.getOOBComponent();
	}

	/**
	 * Gets whether connection should be bound to local or remote interface ,
	 * supported only for rtp connections.
	 * 
	 * @return boolean value
	 */
	@Override
	public boolean getIsLocal() {
		return this.isLocal;
	}

	/**
	 * Gets whether connection should be bound to local or remote interface ,
	 * supported only for rtp connections.
	 * 
	 * @return boolean value
	 */
	@Override
	public void setIsLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}

	/**
	 * Constructs RTP payloads for given channel.
	 * 
	 * @param channel
	 *            the media channel
	 * @param profile
	 *            AVProfile part for media type of given channel
	 * @return collection of RTP formats.
	 */
	private RTPFormats getRTPMap(RTPFormats profile) {
		RTPFormats list = new RTPFormats();
		Formats fmts = new Formats();
		if (rtpAudioChannel.getOutputDsp() != null) {
			Codec[] currCodecs = rtpAudioChannel.getOutputDsp().getCodecs();
			for (int i = 0; i < currCodecs.length; i++)
				if (currCodecs[i].getSupportedInputFormat().matches(
						LINEAR_FORMAT))
					fmts.add(currCodecs[i].getSupportedOutputFormat());
		}

		fmts.add(DTMF_FORMAT);

		if (fmts != null) {
			for (int i = 0; i < fmts.size(); i++) {
				RTPFormat f = profile.find(fmts.get(i));
				if (f != null)
					list.add(f.clone());
			}
		}

		return list;
	}

	@Override
	public void setOtherParty(Connection other) throws IOException {
		throw new IOException("Applicable only for a local connection");
	}

	@Override
	public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
		rtpAudioChannel.updateMode(mode);
		super.setMode(mode);
	}

	/**
	 * Reads the SDP offer and generates an answer.<br>
	 * This is where we know if this is a WebRTC call or not, and the resources
	 * are allocated and configured accordingly.<br>
	 * <p>
	 * <b>For a WebRTC call:</b><br>
	 * An ICE-lite agent is created. This agent assumes a controlled role, and
	 * starts listening for connectivity checks. This agent is responsible for
	 * providing a socket for DTLS hanshake and RTP streaming, once <u>the
	 * connection is established with the remote peer</u>.<br>
	 * So, we can only bind the audio channel and set its remote peer once the
	 * ICE agent has selected the candidate pairs and made such socket
	 * available.
	 * </p>
	 * <p>
	 * <b>For a regular SIP call:</b><br>
	 * The RTP audio channel can be immediately bound to the configured bind
	 * address.<br>
	 * By reading connection fields of the SDP offer, we can set the remote peer
	 * of the audio channel.
	 * </p>
	 * 
	 * @throws IOException
	 */
	private void setOtherParty() throws IOException {
		if (sdp != null && sdp.getAudioDescriptor() != null
				&& sdp.getAudioDescriptor().getFormats() != null) {
			logger.info("Audio Formats" + sdp.getAudioDescriptor().getFormats());
		}

		// Process the SDP offer to know whether this is a WebRTC call or not
		processSdpOffer();

		// Process the SDP answer
		try {
			generateSdpAnswer();
		} catch (SdpException e1) {
			throw new IOException(e1.getMessage(), e1);
		}

		RTPFormats audio = this.answerTemplate.getNegotiatedAudioFormats();
		if (audio.isEmpty() || !audio.hasNonDTMF()) {
			throw new IOException("Audio codecs are not negotiated");
		}

		rtpAudioChannel.setFormatMap(audioFormats);
		try {
			rtpAudioChannel.setOutputFormats(audio.getFormats());
		} catch (FormatNotSupportedException e) {
			// never happen
			throw new IOException(e);
		}

		// Audio channel will only be initialized in case of regular SIP calls
		// For WebRTC calls, we need to wait for the ICE agent to provide a
		// socket
		if (!webRtc) {
			setAudioChannelRemotePeer(this.sdp.getAudioDescriptor());
		}

		// Change the state of this RTP connection from HALF_OPEN to OPEN
		try {
			this.join();
		} catch (Exception e) {
			// exception is possible here when already joined , should not log
		}
	}

	public void setOtherParty(byte[] descriptor) throws IOException {
		try {
			sdp.parse(descriptor);
		} catch (ParseException e) {
			throw new IOException(e.getMessage());
		}
		setOtherParty();
	}

	public void setOtherParty(Text descriptor) throws IOException {
		try {
			sdp.init(descriptor);
		} catch (ParseException e) {
			throw new IOException(e.getMessage());
		}
		setOtherParty();
	}

	/**
	 * (Non Java-doc).
	 * 
	 * @see org.mobicents.media.server.spi.Connection#getDescriptor()
	 */
	@Override
	public String getDescriptor() {
		return sdpAnswer != null ? sdpAnswer : sdpOffer;
	}

	public long getPacketsReceived() {
		return rtpAudioChannel.getPacketsReceived();
	}

	public long getBytesReceived() {
		return 0;
	}

	/**
	 * (Non Java-doc).
	 * 
	 * @see org.mobicents.media.server.spi.Connection#getPacketsTransmitted(org.mobicents.media.server.spi.MediaType)
	 */
	public long getPacketsTransmitted() {
		return rtpAudioChannel.getPacketsTransmitted();
	}

	/**
	 * (Non Java-doc).
	 * 
	 * @see org.mobicents.media.server.spi.Connection#getBytesTransmitted()
	 */
	public long getBytesTransmitted() {
		return 0;
	}

	/**
	 * (Non Java-doc).
	 * 
	 * @see org.mobicents.media.server.spi.Connection#getJitter()
	 */
	public double getJitter() {
		return 0;
	}

	@Override
	public String toString() {
		return "RTP Connection [" + getEndpoint().getLocalName();
	}

	public void onRtpFailure() {
		onFailed();
	}

	@Override
	public void setConnectionFailureListener(
			ConnectionFailureListener connectionFailureListener) {
		this.connectionFailureListener = connectionFailureListener;
	}

	@Override
	protected void onCreated() throws Exception {
		String bindAddress = isLocal ? channelsManager.getLocalBindAddress()
				: channelsManager.getBindAddress();
		this.sdpOffer = offerTemplate.getSDP(bindAddress, "IN", "IP4",
				bindAddress, rtpAudioChannel.getLocalPort(), 0);
	}

	@Override
	protected void onFailed() {
		if (this.connectionFailureListener != null)
			connectionFailureListener.onFailure();

		if (rtpAudioChannel != null)
			rtpAudioChannel.close();
	}

	@Override
	protected void onOpened() throws Exception {
	}

	@Override
	protected void onClosed() {
		sdpAnswer = null;

		try {
			setMode(ConnectionMode.INACTIVE);
		} catch (ModeNotSupportedException e) {
		}

		if (this.isAudioCapabale)
			this.rtpAudioChannel.close();

		releaseConnection(ConnectionType.RTP);
		this.connectionFailureListener = null;
	}

	/**
	 * Reads the SDP offer and sets up the available resources according to the
	 * call type.<br>
	 * In case of a WebRTC call, an ICE-lite agent is created. The agent will
	 * start listening for connectivity checks from the remote peer.<br>
	 * Also, a WebRTC handler will be enabled on the corresponding audio
	 * channel.
	 * 
	 * @throws IOException
	 *             When binding the audio data channel. Non-WebRTC calls only.
	 * @throws SocketException
	 *             When binding the audio data channel. Non-WebRTC calls only.
	 */
	private void processSdpOffer() throws SocketException, IOException {
		// WebRTC check
		this.webRtc = this.sdp.getAudioDescriptor().isWebRTCProfile();
		if (this.webRtc) {
			/*
			 * Integrate with ICE Lite for WebRTC calls
			 * https://bitbucket.org/telestax/telscale-media-server/issue/13
			 */
			this.iceAgent = IceFactory.createLiteAgent();
			try {
				if (this.isAudioCapabale) {
					this.iceAgent.createStream(
							this.rtpAudioChannel.getLocalPort(),
							MediaTypes.AUDIO.description());
				}
			} catch (IceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/*
			 * Configure WebRTC-related resources on audio channel
			 */
			Text remotePeerFingerprint = this.sdp.getAudioDescriptor()
					.getWebRTCFingerprint();
			rtpAudioChannel.enableWebRTC(remotePeerFingerprint);
		} else {
			// If this is NOT a WebRTC call, the RTP audio channel can be bound
			// to a DatagramChannel
			this.rtpAudioChannel.bind(this.isLocal);
		}
	}

	/**
	 * Generates an SDP answer to be sent to the remote peer.
	 * 
	 * @return The template of the SDP answer
	 * @throws SdpException
	 *             In case the SDP is malformed
	 */
	private void generateSdpAnswer() throws SdpException {
		if (!isLocal && this.webRtc) {
			this.answerTemplate = new WebRTCSdpTemplate(this.sdp);
		} else {
			this.answerTemplate = new SdpTemplate(this.sdp);
		}

		// Session-level descriptors
		String bindAddress = isLocal ? channelsManager.getLocalBindAddress()
				: channelsManager.getBindAddress();
		this.answerTemplate.setBindAddress(bindAddress);
		this.answerTemplate.setNetworkType("IN");
		this.answerTemplate.setAddressType("IP4");
		this.answerTemplate.setConnectionAddress(bindAddress);

		// Media Descriptors
		this.answerTemplate.setSupportedAudioFormats(this.audioFormats);
		this.answerTemplate.setAudioPort(rtpAudioChannel.getLocalPort());

		if (this.answerTemplate instanceof WebRTCSdpTemplate) {
			// TODO Update video port based on video channel configuration
			this.answerTemplate.setSupportedVideoFormats(this.videoFormats);
			this.answerTemplate.setVideoPort(0);
			// TODO Update application port based on video channel configuration
			this.answerTemplate
					.setSupportedApplicationFormats(this.applicationFormats);
			this.answerTemplate.setApplicationPort(0);
		}

		this.sdpAnswer = this.answerTemplate.build();
		if (this.webRtc) {
			this.sdpAnswer = SdpNegotiator
					.answer(this.iceAgent, this.sdpAnswer);
		}
	}
	
	/**
	 * Binds the audio channel to its internal <code>bindAddress</code> and sets
	 * the remote peer.
	 * 
	 * @param address
	 *            The address of the remote peer.
	 * @param port
	 *            The port of the remote peer.
	 * @throws SocketException
	 * @throws IOException
	 */
	private void setAudioChannelRemotePeer(String address, int port)
			throws SocketException, IOException {
		if (this.isAudioCapabale) {
			rtpAudioChannel.setPeer(new InetSocketAddress(address, port));
		}
	}

	/**
	 * Binds the audio channel to its internal <code>bindAddress</code> and sets
	 * the remote peer.
	 * 
	 * @param descriptor
	 *            The audio descriptor taken from the SDP template.
	 * @throws SocketException
	 * @throws IOException
	 */
	private void setAudioChannelRemotePeer(MediaDescriptorField descriptor)
			throws SocketException, IOException {
		int peerPort = descriptor.getPort();
		String peerAddress;

		// The connection description defined at media level
		// takes priority over the session-wide connection line
		if (descriptor.getConnection() != null) {
			peerAddress = descriptor.getConnection().getAddress();
		} else {
			peerAddress = sdp.getConnection().getAddress();
		}
		setAudioChannelRemotePeer(peerAddress, peerPort);
	}

}
