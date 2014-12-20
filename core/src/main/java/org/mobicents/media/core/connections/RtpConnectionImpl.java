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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.media.core.MediaTypes;
import org.mobicents.media.io.ice.CandidatePair;
import org.mobicents.media.io.ice.IceAgent;
import org.mobicents.media.io.ice.IceCandidate;
import org.mobicents.media.io.ice.IceFactory;
import org.mobicents.media.io.ice.IceMediaStream;
import org.mobicents.media.io.ice.LocalCandidateWrapper;
import org.mobicents.media.io.ice.events.IceEventListener;
import org.mobicents.media.io.ice.events.SelectedCandidatesEvent;
import org.mobicents.media.io.ice.harvest.HarvestException;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtcp.RtcpChannel;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.MediaChannel;
import org.mobicents.media.server.impl.rtp.RtpChannel;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpListener;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.sdp.SdpComparator;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.io.network.PortManager;
import org.mobicents.media.server.io.sdp.MediaProfile;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SessionDescription;
import org.mobicents.media.server.io.sdp.SessionDescriptionParser;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.attributes.FormatParameterAttribute;
import org.mobicents.media.server.io.sdp.attributes.PacketTimeAttribute;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.mobicents.media.server.io.sdp.attributes.SsrcAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.SetupAttribute;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.OriginField;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;
import org.mobicents.media.server.io.sdp.fields.TimingField;
import org.mobicents.media.server.io.sdp.fields.VersionField;
import org.mobicents.media.server.io.sdp.ice.attributes.CandidateAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceLiteAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpMuxAttribute;
import org.mobicents.media.server.scheduler.Scheduler;
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
 * @author Amit Bhayani
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtpConnectionImpl extends BaseConnection implements RtpListener {
	
	private static final Logger logger = Logger.getLogger(RtpConnectionImpl.class);
	
	// Core elements
	private final ChannelsManager channelsManager;
	private final Scheduler scheduler;
	
	private final String externalAddress;
	
	// RTP session elements
	private String cname;
	private boolean outbound;
	
	// Media
	private MediaChannel audioChannel;

	// Session Description
	private SessionDescription localSdp;
	private SessionDescription remoteSdp;
	
	// Registered formats
	private final static AudioFormat DTMF_FORMAT = FormatFactory.createAudioFormat("telephone-event", 8000);
	private final static AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
	
	// Supported formats
	protected RTPFormats audioSupportedFormats;
	protected RTPFormats videoFormats;
	protected RTPFormats applicationFormats;
	
	private RTPFormats audioOfferedFormats = new RTPFormats();
	
	private RTPFormats audioNegotiatedFormats = new RTPFormats();

	// Connection status
	private boolean isLocal = false;
	private ConnectionFailureListener connectionFailureListener;

	public RtpConnectionImpl(int id, ChannelsManager channelsManager, DspFactory dspFactory) {
		super(id, channelsManager.getScheduler());
		
		// Core elements
		this.channelsManager = channelsManager;
		this.scheduler = channelsManager.getScheduler();
		this.externalAddress = channelsManager.getUdpManager().getExternalAddress();
		
		// outbound vs inbound call
		this.outbound = false;
		
		this.cname = this.audioStatistics.getCname();

		// Audio Channel
		this.audioChannel = new MediaChannel("audio", scheduler.getClock(), channelsManager);
		this.audioChannel.setRtpListener(this);
		this.audioChannel.setInputDsp(dspFactory.newProcessor());
		this.audioChannel.setOutputDsp(dspFactory.newProcessor());
		
		// create sdp template
		this.audioSupportedFormats = getRTPMap(AVProfile.audio);
		this.videoFormats = getRTPMap(AVProfile.video);
		this.applicationFormats = getRTPMap(AVProfile.application);
	}

	@Override
	public AudioComponent getAudioComponent() {
		return this.audioChannel.getAudioComponent();
	}

	@Override
	public OOBComponent getOOBComponent() {
		return this.audioChannel.getAudioOobComponent();
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
		
		if (this.audioChannel.getOutputDsp() != null) {
			Codec[] currCodecs = this.audioChannel.getOutputDsp().getCodecs();
			for (int i = 0; i < currCodecs.length; i++) {
				if (currCodecs[i].getSupportedInputFormat().matches(LINEAR_FORMAT)) {
					fmts.add(currCodecs[i].getSupportedOutputFormat());
				}
			}
		}

		fmts.add(DTMF_FORMAT);

		if (fmts != null) {
			for (int i = 0; i < fmts.size(); i++) {
				RTPFormat f = profile.find(fmts.get(i));
				if (f != null) {
					list.add(f.clone());
				}
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
		this.audioChannel.setConnectionMode(mode);
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
//		if (sdp_old != null && sdp_old.getAudioDescriptor() != null && sdp_old.getAudioDescriptor().getFormats() != null) {
//			logger.info("Audio Formats" + sdp_old.getAudioDescriptor().getFormats());
//		}
		if(this.outbound) {
			setOtherPartyOutboundCall();
		} else {
			setOtherPartyInboundCall();
		}
	}
	
	private void setOtherPartyInboundCall() throws IOException {
		MediaDescriptionField audioOffer = this.remoteSdp.getMediaDescription("audio");
		
		// Process the SDP offer to know whether this is a WebRTC call or not
		processRemoteOffer();

		/*
		 * For ICE-enabled calls, we need to wait for the ICE agent to provide a
		 * socket. This only happens once the SDP has been exchanged between
		 * both parties and ICE agent replies to remote connectivity checks.
		 */
		if (audioOffer.containsIce()) {
			ConnectionField audioOfferConnection = audioOffer.getConnection();
			setAudioChannelRemotePeer(audioOfferConnection.getAddress(), audioOffer.getPort());
		} else {
			// Start ICE agent before we send the SDP answer.
			// The ICE agent will start listening for connectivity checks.
			// FULL ICE implementations will also start connectivity checks.
			this.audioChannel.startIceAgent();
		}

		this.audioNegotiatedFormats = negotiateFormats(audioOffer);
		if (audioNegotiatedFormats.isEmpty() || !audioNegotiatedFormats.hasNonDTMF()) {
			throw new IOException("Audio codecs are not negotiated");
		}
		
		this.audioChannel.setFormats(this.audioNegotiatedFormats);
		this.audioChannel.setOutputFormats(this.audioNegotiatedFormats);
		
		// Process the SDP answer
		try {
			this.localSdp = generateSdpAnswer();
		} catch (SdpException e) {
			throw new IOException(e.getMessage(), e);
		}

		// Change the state of this RTP connection from HALF_OPEN to OPEN
		try {
			this.join();
		} catch (Exception e) {
			// exception is possible here when already joined , should not log
		}
	}
	
	private void setOtherPartyOutboundCall() throws IOException {
		// process remote sdp answer
		MediaDescriptionField remoteAudio = this.remoteSdp.getMediaDescription("audio");
		
		// negotiate codecs
		this.audioNegotiatedFormats = negotiateFormats(remoteAudio);
		if (audioNegotiatedFormats.isEmpty() || !audioNegotiatedFormats.hasNonDTMF()) {
			throw new IOException("Audio codecs are not negotiated");
		}
		
		this.audioChannel.setFormats(this.audioNegotiatedFormats);
		this.audioChannel.setOutputFormats(this.audioNegotiatedFormats);
		
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
			this.audioChannel.bind(this.isLocal, false);
			this.audioChannel.setFormats(this.audioSupportedFormats);
			
			// generate SDP offer based on audio channel
			this.localSdp = generateSdpOffer();
			this.remoteSdp = null;
		}
	}
	
	private SessionDescription generateSdpOffer() {
		String bindAddress = this.isLocal ? this.channelsManager.getLocalBindAddress() : this.channelsManager.getBindAddress();
		String externalAddress = this.channelsManager.getUdpManager().getExternalAddress();
		String originAddress = (externalAddress == null || externalAddress.isEmpty()) ? bindAddress : externalAddress;
		
		// Session-level fields
		SessionDescription offer = new SessionDescription();
		offer.setVersion(new VersionField((short) 0));
		offer.setOrigin(new OriginField("-", String.valueOf(System.currentTimeMillis()), "1", "IN", "IP4", originAddress));
		offer.setSessionName(new SessionNameField("Mobicents Media Server"));
		offer.setConnection(new ConnectionField("IN", "IP4", bindAddress));
		offer.setTiming(new TimingField(0, 0));
		
		// Media Descrription - audio
		MediaDescriptionField audio = this.audioChannel.getMediaDescriptor(this.audioSupportedFormats);
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
	

	
	// TODO integrate this logic somewhere else...
	private RTPFormats negotiateFormats(MediaDescriptionField media) {
		// Clean currently offered formats
		this.audioOfferedFormats.clean();
		
		// Map payload types tp RTP Format
		
		for (int payloadType : media.getPayloadTypes()) {
			RTPFormat format = AVProfile.getFormat(payloadType, SdpComparator.AUDIO);
			if(format != null) {
				this.audioOfferedFormats.add(format);
			}
		}
		
		// Negotiate the formats and store intersection
		RTPFormats negotiated = new RTPFormats();
		this.audioSupportedFormats.intersection(this.audioOfferedFormats, negotiated);
		return negotiated;
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
	private void processRemoteOffer() throws SocketException, IOException {
		// Configure RTCP
		MediaDescriptionField audio = this.remoteSdp.getMediaDescription("audio");
		boolean rtcpMux = audio.isRtcpMux();
		
		/*
		 * For ICE-enabled calls, the RTP channels can only be bound after the
		 * ICE agent selected the candidate pairs. Since Media Server only
		 * implements ICE-lite, we need to wait for the SDP to be exchanged
		 * before we know which candidate socket to use.
		 */
		boolean ice = audio.containsIce();
		if (ice) {
			try {
				// Configure ICE Agent and harvest candidates
				this.audioChannel.enableICE(this.externalAddress);
				this.audioChannel.gatherIceCandidates();
			} catch (HarvestException e) {
				throw new IOException("Could not harvest ICE candidates: " + e.getMessage(), e);
			}
		} else {
			// For non-ICE calls the RTP audio channel can be bound immediately
			this.audioChannel.bind(this.isLocal, rtcpMux);
		}
		
		// Configure WebRTC-related resources on audio channel
		boolean webrtc = this.remoteSdp.containsDtls();
		if (webrtc) {
			audioChannel.enableDTLS(audio.getFingerprint().getValue());
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

		String bindAddress = isLocal ? channelsManager.getLocalBindAddress() : channelsManager.getBindAddress();
		String originatorAddress = this.externalAddress;
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
			
		}
		
		// Media Description - video
		if(this.remoteSdp.getMediaDescription("video") != null) { 
			MediaDescriptionField videoDescription = new MediaDescriptionField(answer);
			videoDescription.setMedia("video");
			// Video is unsupported - reject channel
			videoDescription.setPort(0);
			videoDescription.setProtocol(this.webrtc ? MediaProfile.RTP_SAVPF : MediaProfile.RTP_AVP);
			answer.addMediaDescription(videoDescription);
		}
		return answer;
	}
	
	private CandidateAttribute processCandidate(IceCandidate candidate) {
		CandidateAttribute candidateSdp = new CandidateAttribute();
		candidateSdp.setFoundation(candidate.getFoundation());
		candidateSdp.setComponentId(candidate.getComponentId());
		candidateSdp.setProtocol(candidate.getProtocol().getDescription());
		candidateSdp.setPriority(candidate.getPriority());
		candidateSdp.setAddress(candidate.getHostString());
		candidateSdp.setPort(candidate.getPort());
		String candidateType = candidate.getType().getDescription();
		candidateSdp.setCandidateType(candidateType);
		if(CandidateAttribute.TYP_HOST != candidateType) {
			candidateSdp.setRelatedAddress(candidate.getBase().getHostString());
			candidateSdp.setRelatedPort(candidate.getBase().getPort());
		}
		candidateSdp.setGeneration(0);
		return candidateSdp;
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
		if (this.audioCapabale) {
			rtpAudioChannel.setRemotePeer(new InetSocketAddress(address, port));
		}
	}

}
