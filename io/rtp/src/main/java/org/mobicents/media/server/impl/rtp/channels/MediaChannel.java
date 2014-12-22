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

package org.mobicents.media.server.impl.rtp.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.media.io.ice.CandidatePair;
import org.mobicents.media.io.ice.IceAgent;
import org.mobicents.media.io.ice.IceCandidate;
import org.mobicents.media.io.ice.IceFactory;
import org.mobicents.media.io.ice.IceMediaStream;
import org.mobicents.media.io.ice.LocalCandidateWrapper;
import org.mobicents.media.io.ice.events.IceEventListener;
import org.mobicents.media.io.ice.events.SelectedCandidatesEvent;
import org.mobicents.media.io.ice.harvest.HarvestException;
import org.mobicents.media.server.impl.rtcp.RtcpChannel;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.RtpChannel;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpListener;
import org.mobicents.media.server.impl.rtp.SsrcGenerator;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.io.network.PortManager;
import org.mobicents.media.server.io.sdp.MediaProfile;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.attributes.FormatParameterAttribute;
import org.mobicents.media.server.io.sdp.attributes.PacketTimeAttribute;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.mobicents.media.server.io.sdp.attributes.SsrcAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.SetupAttribute;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.ice.attributes.CandidateAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpMuxAttribute;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;

/**
 * Abstract representation of a media channel with RTP and RTCP components.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public abstract class MediaChannel {

	private static final Logger logger = Logger.getLogger(MediaChannel.class);
	
	// Registered formats
	private final static AudioFormat DTMF_FORMAT = FormatFactory.createAudioFormat("telephone-event", 8000);
	private final static AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);

	protected long ssrc;
	protected String cname;
	protected final String mediaType;
	protected RtpClock clock;
	protected RtpClock oobClock;
	protected RtpChannel rtpChannel;
	protected RtcpChannel rtcpChannel;
	protected boolean rtcpMux;
	protected RtpStatistics statistics;
	protected boolean active;

	protected RTPFormats supportedFormats;
	protected RTPFormats offeredFormats;
	protected RTPFormats negotiatedFormats;
	
	private RtpListener rtpListener;

	// ICE
	private boolean ice;
	private IceAgent iceAgent;
	private IceListener iceListener;

	// DTLS
	private boolean dtls;

	public MediaChannel(String mediaType, Clock wallClock, ChannelsManager channelsManager) {
		this.ssrc = SsrcGenerator.generateSsrc();
		this.mediaType = mediaType;

		this.statistics = new RtpStatistics(clock, this.ssrc);

		this.clock = new RtpClock(wallClock);
		this.oobClock = new RtpClock(wallClock);

		this.rtpChannel = channelsManager.getRtpChannel(this.statistics, this.clock, this.oobClock);
		this.rtcpChannel = channelsManager.getRtcpChannel(this.statistics);

		this.rtcpMux = false;
		this.ice = false;
		this.dtls = false;
		this.active = false;
	}

	public long getSsrc() {
		return ssrc;
	}

	public void setSsrc(long ssrc) {
		this.ssrc = ssrc;
	}

	public String getCname() {
		return cname;
	}

	public void setCname(String cname) {
		this.cname = cname;
		this.statistics.setCname(cname);
	}

	public void activate() {
		// TODO open channels
		this.active = true;
	}

	public void deactivate() throws IllegalStateException {
		if (this.active) {
			// Close channels
			this.rtpChannel.close();
			if (!this.rtcpMux) {
				this.rtcpChannel.close();
			}

			// Stop ICE agent
			if (this.ice && this.iceAgent.isRunning()) {
				this.iceAgent.stop();
			}

			// Reset state
			reset();

			// Update flag
			this.active = false;
		} else {
			throw new IllegalStateException("Channel is already inactive");
		}
	}

	private void reset() {
		// Reset statistics
		this.statistics.reset();
		this.cname = "";
		this.ssrc = 0;

		// Reset channels
		if (this.rtcpMux) {
			this.rtcpMux = false;
			this.rtpChannel.enableRtcpMux(false);
		}

		// Reset ICE
		if (this.ice) {
			this.ice = false;
			if (this.iceAgent != null) {
				this.iceAgent.reset();
			}
		}

		// Reset WebRTC
		if (this.dtls) {
			this.dtls = false;
			this.rtpChannel.disableSRTP();
			this.rtcpChannel.disableSRTCP();
		}
	}

	public boolean isActive() {
		return active;
	}

	public boolean isAvailable() {
		boolean available = this.rtpChannel.isAvailable();
		if (!this.rtcpMux) {
			available = available && this.rtcpChannel.isAvailable();
		}
		return available;
	}

	public void setRtpListener(RtpListener listener) {
		this.rtpListener = listener;
	}

	public void setInputDsp(Processor dsp) {
		this.rtpChannel.setInputDsp(dsp);
	}

	public Processor getInputDsp() {
		return this.rtpChannel.getInputDsp();
	}

	public void setOutputDsp(Processor dsp) {
		this.rtpChannel.setOutputDsp(dsp);
	}

	public Processor getOutputDsp() {
		return this.rtpChannel.getOutputDsp();
	}

	public void setConnectionMode(ConnectionMode mode) {
		this.rtpChannel.updateMode(mode);
	}

	public void setFormats(RTPFormats formats) {
		this.rtpChannel.setFormatMap(formats);
	}
	
	public RTPFormats getFormats() {
		return this.rtpChannel.getFormatMap();
	}

	public void setOutputFormats(RTPFormats formats) {
		try {
			this.rtpChannel.setOutputFormats(formats.getFormats());
		} catch (FormatNotSupportedException e) {
			// Never happens
			logger.warn("Could not set output formats", e);
		}
	}

	public void bind(boolean isLocal, boolean rtcpMux) throws SocketException, IOException {
		this.bindRtp(isLocal);
		this.bindRtcp(isLocal, rtcpMux);
		this.rtcpMux = rtcpMux;
	}

	private void bindRtp(boolean isLocal) throws SocketException, IOException {
		this.rtpChannel.bind(isLocal);
	}

	private void bindRtcp(boolean isLocal, boolean mux) throws SocketException, IOException {
		if (mux) {
			this.rtcpChannel.bind(isLocal, this.rtpChannel.getLocalPort());
		} else {
			this.rtcpChannel.bind(isLocal, this.rtpChannel.getLocalPort() + 1);
		}
	}

	public void connectRtp(SocketAddress address) {
		this.rtpChannel.setRemotePeer(address);
	}

	public void connectRtp(String address, int port) {
		this.connectRtp(new InetSocketAddress(address, port));
	}

	public void bindRtcp(boolean isLocal, int port) throws SocketException, IOException {
		this.rtcpChannel.bind(isLocal, port);
		this.rtcpMux = (port == this.rtpChannel.getLocalPort());
	}

	public void connectRtcp(SocketAddress remoteAddress) {
		this.rtcpChannel.setRemotePeer(remoteAddress);
	}

	public void connectRtcp(String address, int port) {
		this.connectRtcp(new InetSocketAddress(address, port));
	}
	
	/*
	 * CODECS
	 */
	/**
	 * Constructs RTP payloads for given channel.
	 * 
	 * @param channel
	 *            the media channel
	 * @param profile
	 *            AVProfile part for media type of given channel
	 * @return collection of RTP formats.
	 */
	protected RTPFormats buildRTPMap(RTPFormats profile) {
		RTPFormats list = new RTPFormats();
		Formats fmts = new Formats();
		
		if (this.rtpChannel.getOutputDsp() != null) {
			Codec[] currCodecs = this.rtpChannel.getOutputDsp().getCodecs();
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
	
	public void resetFormats() {
		this.offeredFormats.clean();
		this.negotiatedFormats.clean();
		setFormats(this.supportedFormats);
		setOutputFormats(this.supportedFormats);
	}
	
	public void negotiateFormats(MediaDescriptionField media) {
		// Clean currently offered formats
		this.offeredFormats.clean();
		
		// Map payload types tp RTP Format
		for (int payloadType : media.getPayloadTypes()) {
			RTPFormat format = AVProfile.getFormat(payloadType, AVProfile.AUDIO);
			if(format != null) {
				this.offeredFormats.add(format);
			}
		}
		
		// Negotiate the formats and store intersection
		this.negotiatedFormats.clean();
		this.supportedFormats.intersection(this.offeredFormats, this.negotiatedFormats);
		
		// Apply formats
		setFormats(this.negotiatedFormats);
		setOutputFormats(this.negotiatedFormats);
	}
	
	public boolean hasNegotiatedFormats() {
		return !negotiatedFormats.isEmpty() && negotiatedFormats.hasNonDTMF();
	}

	/*
	 * ICE
	 */
	public void enableICE(String externalAddress) throws UnknownHostException, IllegalStateException {
		if (!this.active) {
			throw new IllegalStateException("Media Channel is not active");
		}

		if (this.ice) {
			throw new IllegalStateException("ICE is already enabled");
		}

		this.ice = true;
		if (this.iceAgent == null) {
			this.iceAgent = IceFactory.createLiteAgent();
			this.iceListener = new IceListener();
		}
		this.iceAgent.addIceListener(this.iceListener);
		this.iceAgent.generateIceCredentials();
		this.iceAgent.addMediaStream("audio", true, this.rtcpMux);

		// Add SRFLX candidate harvester if external address is defined
		if (externalAddress != null && !externalAddress.isEmpty()) {
			this.iceAgent.setExternalAddress(InetAddress.getByName(externalAddress));
		}
	}

	public void disableICE() {
		// XXX close ICE resources
		this.ice = false;
	}

	public boolean hasICE() {
		return this.ice;
	}

	public void gatherIceCandidates(PortManager portManager) throws HarvestException, IllegalStateException {
		if (!this.ice) {
			throw new IllegalStateException(
					"ICE is not enabled on this media channel");
		}
		this.iceAgent.harvest(portManager);
	}

	public void startIceAgent() throws IllegalStateException {
		if (!this.ice) {
			throw new IllegalStateException("ICE is not enabled on this media channel");
		}

		// Start ICE Agent only if necessary
		if (!this.iceAgent.isRunning()) {
			this.iceAgent.start();
		}
	}

	public void stopIceAgent() throws IllegalStateException {
		if (!this.ice) {
			throw new IllegalStateException("ICE is not enabled on this media channel");
		}

		// Start ICE Agent only if necessary
		if (this.iceAgent.isRunning()) {
			this.iceAgent.stop();
		}
	}

	private class IceListener implements IceEventListener {

		@Override
		public void onSelectedCandidates(SelectedCandidatesEvent event) {
			try {
				if(logger.isDebugEnabled()) {
					logger.debug("Finished ICE candidates selection! Preparing for binding.");
				}

				// Get selected RTP candidate for audio channel
				IceAgent agent = event.getSource();
				CandidatePair rtpCandidate = agent.getSelectedRtpCandidate("audio");
				// Bind candidate to RTP audio channel
				rtpChannel.bind(rtpCandidate.getChannel());

				CandidatePair rtcpCandidate = agent.getSelectedRtcpCandidate("audio");
				if (rtcpCandidate != null) {
					rtcpChannel.bind(rtcpCandidate.getChannel());
				}
			} catch (IOException e) {
				rtpListener.onRtpFailure("Could not select ICE candidates: " + e.getMessage());
			}
		}
	}

	/*
	 * DTLS
	 */
	public void enableDTLS(String remoteFingerprint)
			throws IllegalStateException {
		if (this.dtls) {
			throw new IllegalStateException("DTLS is already enabled on this channel");
		}

		if (!this.ice) {
			throw new IllegalStateException("ICE must be enabled on this channel");
		}

		this.rtpChannel.enableSRTP(remoteFingerprint, this.iceAgent);
		if (!this.rtcpMux) {
			rtcpChannel.enableSRTCP(remoteFingerprint, this.iceAgent);
		}
	}

	public void disableDTLS() throws IllegalStateException {
		// XXX disable DTLS components
		this.dtls = false;
	}

	/*
	 * Statistics
	 */
	public long getPacketsReceived() {
		return this.statistics.getRtpPacketsReceived();
	}

	public long getOctetsReceived() {
		return this.statistics.getRtpOctetsReceived();
	}

	public long getPacketsSent() {
		return this.statistics.getRtpPacketsSent();
	}

	public long getOctetsSent() {
		return this.statistics.getRtpOctetsSent();
	}

	public long getJitter() {
		return this.statistics.getMember(this.ssrc).getJitter();
	}

	/*
	 * SDP XXX Create an SDP factory
	 */
	public MediaDescriptionField getMediaDescriptor() {
		MediaDescriptionField mediaField = new MediaDescriptionField();
		mediaField.setMedia(this.mediaType);
		mediaField.setPort(this.rtpChannel.getLocalPort());
		mediaField.setProtocol(MediaProfile.RTP_AVP);
		mediaField.setConnection(new ConnectionField("IN", "IP4", this.rtpChannel.getLocalHost()));
		mediaField.setPtime(new PacketTimeAttribute(20));
		mediaField.setRtcp(new RtcpAttribute(this.rtcpChannel.getLocalPort(), "IN", "IP4", this.rtcpChannel.getLocalHost()));

		// Media formats
		this.supportedFormats.rewind();
		while (this.supportedFormats.hasMore()) {
			RTPFormat f = this.supportedFormats.next();
			AudioFormat audioFormat = (AudioFormat) f.getFormat();

			RtpMapAttribute rtpMap = new RtpMapAttribute();
			rtpMap.setPayloadType(f.getID());
			rtpMap.setCodec(f.getFormat().getName().toString());
			rtpMap.setClockRate(f.getClockRate());
			if (audioFormat.getChannels() > 1) {
				rtpMap.setCodecParams(audioFormat.getChannels());
			}
			if (audioFormat.getOptions() != null) {
				rtpMap.setParameters(new FormatParameterAttribute(f.getID(), audioFormat.getOptions().toString()));
			}
			mediaField.addPayloadType(f.getID());
			mediaField.addFormat(rtpMap);
		}
		mediaField.setConnectionMode(new ConnectionModeAttribute(ConnectionModeAttribute.SENDRECV));
		SsrcAttribute ssrcAttribute = new SsrcAttribute(Long.toString(this.ssrc));
		ssrcAttribute.addAttribute("cname", this.cname);
		mediaField.setSsrc(ssrcAttribute);
		return mediaField;
	}

	public MediaDescriptionField generateAnswer() {
		String rtpAddress = this.rtpChannel.getLocalHost();
		int rtpPort = this.rtpChannel.getLocalPort();

		String rtcpAddress = this.rtcpMux ? rtpAddress : this.rtcpChannel.getLocalHost();
		int rtcpPort = this.rtcpMux ? rtpPort : this.rtcpChannel.getLocalPort();

		if (this.ice) {
			LocalCandidateWrapper rtpCandidate = this.iceAgent
					.getMediaStream(this.mediaType).getRtpComponent()
					.getDefaultLocalCandidate();
			rtpAddress = rtpCandidate.getCandidate().getHostString();
			rtpPort = rtpCandidate.getCandidate().getPort();

			if (this.rtcpMux) {
				rtcpAddress = rtpAddress;
				rtcpPort = rtpPort;
			} else {
				CandidatePair rtcpCandidate = this.iceAgent.getSelectedRtcpCandidate("audio");
				rtcpAddress = rtcpCandidate.getLocalAddress();
				rtcpPort = rtcpCandidate.getLocalPort();
			}
		}

		MediaDescriptionField audioDescription = new MediaDescriptionField();
		audioDescription.setMedia("audio");
		audioDescription.setPort(rtpPort);
		audioDescription.setProtocol(this.dtls ? MediaProfile.RTP_SAVPF : MediaProfile.RTP_AVP);
		audioDescription.setConnection(new ConnectionField("IN", "IP4", rtpAddress));
		audioDescription.setPtime(new PacketTimeAttribute(20));
		audioDescription.setRtcp(new RtcpAttribute(rtcpPort, "IN", "IP4", rtcpAddress));
		if (this.rtcpMux) {
			audioDescription.setRtcpMux(new RtcpMuxAttribute());
		}

		// ICE attributes
		if (this.ice) {
			audioDescription.setIceUfrag(new IceUfragAttribute(this.iceAgent.getUfrag()));
			audioDescription.setIcePwd(new IcePwdAttribute(this.iceAgent.getPassword()));

			IceMediaStream audioIce = iceAgent.getMediaStream("audio");
			List<LocalCandidateWrapper> rtpCandidates = audioIce.getRtpComponent().getLocalCandidates();
			for (LocalCandidateWrapper candidate : rtpCandidates) {
				audioDescription.addCandidate(processCandidate(candidate.getCandidate()));
			}
			if (!this.rtcpMux) {
				List<LocalCandidateWrapper> rtcpCandidates = audioIce.getRtcpComponent().getLocalCandidates();
				for (LocalCandidateWrapper candidate : rtcpCandidates) {
					audioDescription.addCandidate(processCandidate(candidate.getCandidate()));
				}
			}
		}

		// Media formats
		this.negotiatedFormats.rewind();
		while (this.negotiatedFormats.hasMore()) {
			RTPFormat f = this.negotiatedFormats.next();
			AudioFormat audioFormat = (AudioFormat) f.getFormat();

			RtpMapAttribute rtpMap = new RtpMapAttribute();
			rtpMap.setPayloadType(f.getID());
			rtpMap.setCodec(f.getFormat().getName().toString());
			rtpMap.setClockRate(f.getClockRate());
			if (audioFormat.getChannels() > 1) {
				rtpMap.setCodecParams(audioFormat.getChannels());
			}
			if (audioFormat.getOptions() != null) {
				rtpMap.setParameters(new FormatParameterAttribute(f.getID(), audioFormat.getOptions().toString()));
			}
			audioDescription.addPayloadType(f.getID());
			audioDescription.addFormat(rtpMap);
		}

		// DTLS attributes
		if (this.dtls) {
			audioDescription.setSetup(new SetupAttribute(SetupAttribute.PASSIVE));
			String fingerprint = this.rtpChannel.getWebRtcLocalFingerprint().toString();
			int whitespace = fingerprint.indexOf(" ");
			String fingerprintHash = fingerprint.substring(0, whitespace);
			String fingerprintValue = fingerprint.substring(whitespace + 1);
			audioDescription.setFingerprint(new FingerprintAttribute(fingerprintHash, fingerprintValue));
		}

		audioDescription.setConnectionMode(new ConnectionModeAttribute(ConnectionModeAttribute.SENDRECV));
		SsrcAttribute ssrcAttribute = new SsrcAttribute(Long.toString(this.ssrc));
		ssrcAttribute.addAttribute("cname", this.cname);
		audioDescription.setSsrc(ssrcAttribute);
		return audioDescription;
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
		if (CandidateAttribute.TYP_HOST != candidateType) {
			candidateSdp.setRelatedAddress(candidate.getBase().getHostString());
			candidateSdp.setRelatedPort(candidate.getBase().getPort());
		}
		candidateSdp.setGeneration(0);
		return candidateSdp;
	}
}
