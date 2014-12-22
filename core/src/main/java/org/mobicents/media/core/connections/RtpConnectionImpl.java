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
import java.net.SocketException;

import org.apache.log4j.Logger;
import org.mobicents.media.io.ice.harvest.HarvestException;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.CnameGenerator;
import org.mobicents.media.server.impl.rtp.RtpListener;
import org.mobicents.media.server.impl.rtp.channels.AudioChannel;
import org.mobicents.media.server.io.sdp.MediaProfile;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SessionDescription;
import org.mobicents.media.server.io.sdp.SessionDescriptionParser;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.OriginField;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;
import org.mobicents.media.server.io.sdp.fields.TimingField;
import org.mobicents.media.server.io.sdp.fields.VersionField;
import org.mobicents.media.server.io.sdp.ice.attributes.IceLiteAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.utils.Text;

/**
 * 
 * @author Oifa Yulian
 * @author Amit Bhayani
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see BaseConnection 
 */
public class RtpConnectionImpl extends BaseConnection implements RtpListener {
	
	private static final Logger logger = Logger.getLogger(RtpConnectionImpl.class);
	
	// Core elements
	private final ChannelsManager channelsManager;
	
	// RTP session elements
	private String cname;
	private boolean outbound;
	private boolean local = false;
	
	private ConnectionFailureListener connectionFailureListener;

	// Media
	private AudioChannel audioChannel;

	// Session Description
	private SessionDescription localSdp;
	private SessionDescription remoteSdp;

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
		this.audioChannel.setRtpListener(this);
		try {
			this.audioChannel.setInputDsp(dspFactory.newProcessor());
			this.audioChannel.setOutputDsp(dspFactory.newProcessor());
		} catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
			// exception may happen only if invalid classes have been set in configuration
			throw new RuntimeException("There are invalid classes specified in the configuration.", e);
		}
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
	public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
		this.audioChannel.setConnectionMode(mode);
		super.setMode(mode);
	}

	/**
	 * Reads the SDP offer and generates an answer.<br>
	 * This is where we know if this is a WebRTC call or not, and the resources
	 * are allocated and configured accordingly.<br>
	 * 
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
	 * 
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
		
		if(this.outbound) {
			setOtherPartyOutboundCall();
		} else {
			setOtherPartyInboundCall();
		}
		
		if(logger.isDebugEnabled()) {
			logger.debug("Audio formats: " + this.audioChannel.getFormats());
		}
	}
	
	private void setOtherPartyInboundCall() throws IOException {
		// Setup the audio channel based on remote offer
		MediaDescriptionField remoteAudio = this.remoteSdp.getMediaDescription("audio");
		if(remoteAudio != null) {
			this.audioChannel.activate();
			setupAudioChannel(remoteAudio);
		}
		
		// Generate SDP answer
		try {
			this.localSdp = generateSdpAnswer();
		} catch (SdpException e) {
			throw new IOException(e.getMessage(), e);
		}

		// Change the state of this RTP connection from HALF_OPEN to OPEN
		try {
			this.join();
		} catch (Exception e) {
			// exception is possible here when already joined
			logger.warn("Could not set connection state to OPEN", e);
		}
	}
	
	private void setOtherPartyOutboundCall() throws IOException {
		// process remote sdp answer
		MediaDescriptionField remoteAudio = this.remoteSdp.getMediaDescription("audio");
		
		// negotiate codecs
		this.audioChannel.negotiateFormats(remoteAudio);
		if (!this.audioChannel.hasNegotiatedFormats()) {
			throw new IOException("Audio codecs are not negotiated");
		}
		
		// connect to remote peer
		String remoteRtpAddress = remoteAudio.getConnection().getAddress();
		int remoteRtpPort = remoteAudio.getPort();
		
		this.audioChannel.connectRtp(remoteRtpAddress, remoteRtpPort);
		
		boolean remoteRtcpMux = remoteAudio.isRtcpMux();
		if(remoteRtcpMux) {
			this.audioChannel.connectRtcp(remoteRtpAddress, remoteRtpPort);
		} else {
			RtcpAttribute remoteRtcp = remoteAudio.getRtcp();
			if(remoteRtcp == null) {
				// No specific RTCP port, so default is RTP port + 1
				this.audioChannel.connectRtcp(remoteRtpAddress, remoteRtpPort + 1);
			} else {
				// Specific RTCP address and port contained in SDP
				String remoteRtcpAddress = remoteRtcp.getAddress();
				if(remoteRtcpAddress == null) {
					// address is optional in rtcp attribute
					// will match RTP address if not defined
					remoteRtcpAddress = remoteRtpAddress;
				}
				int remoteRtcpPort = remoteRtcp.getPort();
				this.audioChannel.connectRtcp(remoteRtcpAddress, remoteRtcpPort);
			}
		}
		
		// Change the state of this RTP connection from HALF_OPEN to OPEN
		try {
			this.join();
		} catch (Exception e) {
			// exception is possible here when already joined , should not log
		}
	}

	@Override
	public void setOtherParty(byte[] descriptor) throws IOException {
		try {
			this.remoteSdp = SessionDescriptionParser.parse(new String(descriptor));
			setOtherParty();
		} catch (SdpException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void setOtherParty(Text descriptor) throws IOException {
		setOtherParty(descriptor.toString().getBytes());
	}

	@Override
	public String getDescriptor() {
		return (this.localSdp == null) ? "" : this.localSdp.toString();
	}
	
	@Override
	public void generateLocalDescriptor() throws IOException {
		// Only open and bind a new channel if not currently configured
		if(this.audioChannel.isActive()) {
			// call is outbound since the connection is generating the offer
			this.outbound = true;
			
			// setup audio channel
			this.audioChannel.bind(this.local, false);
			this.audioChannel.resetFormats();
			
			// generate SDP offer based on audio channel
			this.localSdp = generateSdpOffer();
			this.remoteSdp = null;
		}
	}
	
	private SessionDescription generateSdpOffer() {
		String bindAddress = this.local ? this.channelsManager.getLocalBindAddress() : this.channelsManager.getBindAddress();
		String externalAddress = this.channelsManager.getUdpManager().getExternalAddress();
		String originAddress = (externalAddress == null || externalAddress.isEmpty()) ? bindAddress : externalAddress;
		
		// Session-level fields
		SessionDescription offer = new SessionDescription();
		offer.setVersion(new VersionField((short) 0));
		offer.setOrigin(new OriginField("-", String.valueOf(System.currentTimeMillis()), "1", "IN", "IP4", originAddress));
		offer.setSessionName(new SessionNameField("Mobicents Media Server"));
		offer.setConnection(new ConnectionField("IN", "IP4", bindAddress));
		offer.setTiming(new TimingField(0, 0));
		
		// Media Description - audio
		MediaDescriptionField audio = this.audioChannel.getMediaDescriptor();
		audio.setSession(offer);
		offer.addMediaDescription(audio);
		
		return offer;
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
	
	private void closeResources() {
		if(this.audioChannel.isActive()) {
			this.audioChannel.deactivate();
		}
	}
	
	private void reset() {
		// Reset SDP
		this.outbound = false;
		this.localSdp = null;
		this.remoteSdp = null;
	}

	@Override
	public void onRtpFailure(String message) {
		if(this.audioChannel.isActive()) {
			logger.warn(message);
			// RTP is mandatory, if it fails close everything
			onFailed();
		}
	}
	
	@Override
	public void onRtpFailure(Throwable e) {
		String message = "RTP failure!";
		if(e != null) {
			message += " Reason: "+ e.getMessage();
		}
		onRtpFailure(message);
	}
	
	@Override
	public void onRtcpFailure(String e) {
		if (this.audioChannel.isActive()) {
			logger.warn(e);
			// Close the RTCP channel only
			// Keep the RTP channel open because RTCP is not mandatory
			onFailed();
		}
	}
	
	@Override
	public void onRtcpFailure(Throwable e) {
		String message = "RTCP failure!";
		if(e != null) {
			message += " Reason: "+ e.getMessage();
		}
		onRtcpFailure(message);
	}
	
	@Override
	public void setConnectionFailureListener(ConnectionFailureListener connectionFailureListener) {
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
		}
		releaseConnection(ConnectionType.RTP);
		this.connectionFailureListener = null;
	}
	
	/**
	 * Reads the SDP offer and sets up the available resources according to the
	 * call type.
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
	private void setupAudioChannel(MediaDescriptionField remoteAudio) throws IOException {
		// Negotiate audio codecs
		this.audioChannel.negotiateFormats(remoteAudio);
		if (!this.audioChannel.hasNegotiatedFormats()) {
			throw new IOException("Audio codecs were not supported");
		}
		
		boolean rtcpMux = remoteAudio.isRtcpMux();
		if(remoteAudio.containsIce()) {
			/*
			 * Operate media channel in ICE mode.
			 * 
			 * Candidates will be gathered and chosen via STUN handshake. The
			 * underlying datagram channel of the selected candidate will be set
			 * directly in the MediaChannel.
			 */
			try {
				this.audioChannel.enableICE(this.channelsManager.getExternalAddress(), rtcpMux);
				this.audioChannel.gatherIceCandidates(this.channelsManager.getPortManager());
				this.audioChannel.startIceAgent();
			} catch (HarvestException | IllegalStateException e) {
				throw new IOException("Cannot harvest ICE candidates", e);
			}
		} else {
			// ICE is not active. Bind RTP and RTCP channels right now.
			String remoteAddr = remoteAudio.getConnection().getAddress();
			this.audioChannel.bind(this.local, rtcpMux);
			this.audioChannel.connectRtp(remoteAddr, remoteAudio.getPort());
			this.audioChannel.connectRtcp(remoteAddr, remoteAudio.getRtcpPort());
		}
		
		// Check whether SRTP should be active
		if(remoteAudio.containsDtls()) {
			this.audioChannel.enableDTLS(remoteAudio.getFingerprint().getValue());
		}
	}

	/**
	 * Generates an SDP answer to be sent to the remote peer.
	 * 
	 * @return The template of the SDP answer
	 * @throws SdpException
	 *             In case the SDP is malformed
	 */
	private SessionDescription generateSdpAnswer() throws SdpException {
		SessionDescription answer = new SessionDescription();

		String bindAddress = local ? channelsManager.getLocalBindAddress() : channelsManager.getBindAddress();
		String originatorAddress = this.channelsManager.getExternalAddress();
		if(originatorAddress == null || originatorAddress.isEmpty()) {
			originatorAddress = bindAddress;
		}
		
		// Session Description
		answer.setVersion(new VersionField((short) 0));
		answer.setOrigin(new OriginField("-", String.valueOf(System.currentTimeMillis()), "1", "IN", "IP4", originatorAddress));
		answer.setSessionName(new SessionNameField("Mobicents Media Server"));
		answer.setConnection(new ConnectionField("IN", "IP4", bindAddress));
		answer.setTiming(new TimingField(0, 0));

		// Session-level ICE
		if(this.audioChannel.hasICE()) {
			answer.setIceLite(new IceLiteAttribute());
		}
		
		// Media Description - audio
		if(this.remoteSdp.getMediaDescription("audio") != null) {
			MediaDescriptionField audioDescription = this.audioChannel.generateAnswer();
			answer.addMediaDescription(audioDescription);
			audioDescription.setSession(answer);
			if(audioDescription.containsIce()) {
				// Fix session-level attribute
				String rtpAddress = audioDescription.getConnection().getAddress();
				answer.getConnection().setAddress(rtpAddress);
			}
		}
		
		// Media Description - video
		if(this.remoteSdp.getMediaDescription("video") != null) { 
			MediaDescriptionField videoDescription = new MediaDescriptionField(answer);
			videoDescription.setMedia("video");
			// Video is unsupported - reject channel
			videoDescription.setPort(0);
			videoDescription.setProtocol(videoDescription.containsDtls() ? MediaProfile.RTP_SAVPF : MediaProfile.RTP_AVP);
			answer.addMediaDescription(videoDescription);
		}
		return answer;
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
	private void setAudioChannelRemotePeer(String address, int port) throws SocketException, IOException {
		if (this.audioChannel.isActive()) {
			this.audioChannel.connectRtp(address, port);
		}
	}

}
