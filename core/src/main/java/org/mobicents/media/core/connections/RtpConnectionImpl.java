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
import org.mobicents.media.server.impl.rtp.sdp.CandidateField;
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
	protected SdpTemplate template;
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
		rtpAudioChannel = channelsManager.getChannel();
		rtpAudioChannel.setRtpChannelListener(this);

		try {
			rtpAudioChannel.setInputDsp(dspFactory.newProcessor());
			rtpAudioChannel.setOutputDsp(dspFactory.newProcessor());
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

		template = new SdpTemplate(this.audioFormats, this.videoFormats);
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

	private void setOtherParty() throws IOException {
		if (sdp != null && sdp.getAudioDescriptor() != null
				&& sdp.getAudioDescriptor().getFormats() != null) {
			logger.info("Audio Formats" + sdp.getAudioDescriptor().getFormats());
		}

		/*
		 * Build SDP answer
		 */
		this.webRtc = sdp.getAudioDescriptor().isWebRTCProfile();

		SdpTemplate sdpAnswerTemplate;
		if (!isLocal && this.webRtc) {
			sdpAnswerTemplate = new WebRTCSdpTemplate(this.sdp);
		} else {
			sdpAnswerTemplate = new SdpTemplate(this.sdp);
		}

		// Session-level descriptors
		String bindAddress = isLocal ? channelsManager.getLocalBindAddress()
				: channelsManager.getBindAddress();
		sdpAnswerTemplate.setBindAddress(bindAddress);
		sdpAnswerTemplate.setNetworkType("IN");
		sdpAnswerTemplate.setAddressType("IP4");
		sdpAnswerTemplate.setConnectionAddress(bindAddress);

		// Media Descriptors
		sdpAnswerTemplate.setSupportedAudioFormats(this.audioFormats);
		sdpAnswerTemplate.setAudioPort(rtpAudioChannel.getLocalPort());

		if (sdpAnswerTemplate instanceof WebRTCSdpTemplate) {
			// TODO Update video port based on video channel configuration
			sdpAnswerTemplate.setSupportedVideoFormats(this.videoFormats);
			sdpAnswerTemplate.setVideoPort(0);
			// TODO Update application port based on video channel configuration
			sdpAnswerTemplate
					.setSupportedApplicationFormats(this.applicationFormats);
			sdpAnswerTemplate.setApplicationPort(0);
		}
		sdpAnswer = sdpAnswerTemplate.build();

		/*
		 * Integrate with ICE Lite for WebRTC calls
		 * https://bitbucket.org/telestax/telscale-media-server/issue/13
		 */
		if (this.webRtc) {
			try {
				this.iceAgent = IceFactory.createLiteAgent();
				this.iceAgent.createStream(this.rtpAudioChannel.getLocalPort(),
						MediaTypes.AUDIO.description());
				sdpAnswer = SdpNegotiator.answer(iceAgent, sdpAnswer);
			} catch (SdpException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/*
		 * Configure RTP Audio Channel
		 */
		RTPFormats audio = sdpAnswerTemplate.getNegotiatedAudioFormats();
		if (!audio.isEmpty()) {
			rtpAudioChannel.setFormatMap(audioFormats);
			try {
				rtpAudioChannel.setOutputFormats(audio.getFormats());
			} catch (FormatNotSupportedException e) {
				// never happen
				throw new IOException(e);
			}
		}

		if (this.webRtc) {
			Text remotePeerFingerprint = sdp.getAudioDescriptor()
					.getWebRTCFingerprint();
			rtpAudioChannel.enableWebRTC(remotePeerFingerprint);
		}

		String peerAddress = null;
		int peerPort = -1;
		if (sdp.getAudioDescriptor() != null) {
			MediaDescriptorField audioDescriptor = sdp.getAudioDescriptor();
			if (this.webRtc) {
				// For WebRTC connections its necessary to query for the address
				// of the most relevant candidate
				CandidateField candidate = audioDescriptor
						.getMostRelevantCandidate();
				peerAddress = candidate.getAddress().toString();
				peerPort = candidate.getPort().toInteger();
			} else {
				// For regular calls the connection description defined at media
				// level takes priority over the session-wide connection line
				if (audioDescriptor.getConnection() != null) {
					peerAddress = audioDescriptor.getConnection().getAddress();
				} else {
					peerAddress = sdp.getConnection().getAddress();
				}
				peerPort = audioDescriptor.getPort();
			}
			rtpAudioChannel
					.setPeer(new InetSocketAddress(peerAddress, peerPort));
		}

		if (audio.isEmpty() || !audio.hasNonDTMF()) {
			throw new IOException("Codecs are not negotiated");
		}

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
		if (this.isAudioCapabale) {
			rtpAudioChannel.bind(isLocal);
		}
		String bindAddress = isLocal ? channelsManager.getLocalBindAddress()
				: channelsManager.getBindAddress();
		sdpOffer = template.getSDP(bindAddress, "IN", "IP4", bindAddress,
				rtpAudioChannel.getLocalPort(), 0);
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
}
