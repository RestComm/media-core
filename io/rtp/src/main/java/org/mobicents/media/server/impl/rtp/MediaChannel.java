package org.mobicents.media.server.impl.rtp;

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
import org.mobicents.media.io.ice.IceFactory;
import org.mobicents.media.io.ice.IceMediaStream;
import org.mobicents.media.io.ice.LocalCandidateWrapper;
import org.mobicents.media.io.ice.events.IceEventListener;
import org.mobicents.media.io.ice.events.SelectedCandidatesEvent;
import org.mobicents.media.io.ice.harvest.HarvestException;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtcp.RtcpChannel;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.io.sdp.MediaProfile;
import org.mobicents.media.server.io.sdp.SessionLevelAccessor;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.attributes.FormatParameterAttribute;
import org.mobicents.media.server.io.sdp.attributes.PacketTimeAttribute;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.mobicents.media.server.io.sdp.attributes.SsrcAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.SetupAttribute;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpMuxAttribute;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.AudioFormat;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaChannel {
	
	private static final Logger logger = Logger.getLogger(MediaChannel.class);
	
	private long ssrc;
	private String cname;
	private final String mediaType;
	private RtpClock clock;
	private RtpClock oobClock;
	private RtpChannel rtpChannel;
	private RtcpChannel rtcpChannel;
	private boolean rtcpMux;
	private RtpStatistics statistics;
	private boolean active;
	
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
	}
	
	public void activate() {
		// TODO open channels
		this.active = true;
	}
	
	public void deactivate() throws IllegalStateException {
		if(this.active) {
			// Close channels
			this.rtpChannel.close();
			if(!this.rtcpMux) {
				this.rtcpChannel.close();
			}

			// Stop ICE agent
			if(this.ice && this.iceAgent.isRunning()) {
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
		if(this.rtcpMux) {
			this.rtcpMux = false;
			this.rtpChannel.enableRtcpMux(false);
		}
		
		// Reset ICE
		if(this.ice) {
			this.ice = false;
			if(this.iceAgent != null) {
				this.iceAgent.reset();
			}
		}
		
		// Reset WebRTC
		if(this.dtls) {
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
		if(!this.rtcpMux) {
			available = available && this.rtcpChannel.isAvailable();
		}
		return available;
	}
	
	public void setRtpListener(RtpListener listener) {
		// TODO set rtp listener
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
	
	// XXX this breaks abstraction
	public AudioComponent getAudioComponent() {
		return this.rtpChannel.getAudioComponent();
	}
	
	// XXX this breaks abstraction
	public OOBComponent getAudioOobComponent() {
		return this.rtpChannel.getOobComponent();
	}
	
	public void setConnectionMode(ConnectionMode mode) {
		this.rtpChannel.updateMode(mode);
	}
	
	public void setFormats(RTPFormats formats) {
		this.rtpChannel.setFormatMap(formats);
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
		if(mux) {
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
	 * ICE
	 */
	public void enableICE(String externalAddress) throws UnknownHostException, IllegalStateException {
		if(!this.active) {
			throw new IllegalStateException("Media Channel is not active");
		}
		
		if(this.ice) {
			throw new IllegalStateException("ICE is already enabled");
		}
		
		this.ice = true;
		if(this.iceAgent == null) {
			this.iceAgent = IceFactory.createLiteAgent();
			this.iceListener = new IceListener();
		}
		this.iceAgent.addIceListener(this.iceListener);
		this.iceAgent.generateIceCredentials();
		this.iceAgent.addMediaStream("audio", true, this.rtcpMux);
		
		// Add SRFLX candidate harvester if external address is defined
		if(externalAddress != null && !externalAddress.isEmpty()) {
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
	
	public void gatherIceCandidates() throws HarvestException, IllegalStateException {
		if(!this.ice) {
			throw new IllegalStateException("ICE is not enabled on this media channel");
		}
		this.iceAgent.harvest(portManager);
	}
	
	public void startIceAgent() throws IllegalStateException {
		if(!this.ice) {
			throw new IllegalStateException("ICE is not enabled on this media channel");
		}
		
		// Start ICE Agent only if necessary
		if(!this.iceAgent.isRunning()) {
			this.iceAgent.start();
		}
	}
	
	public void stopIceAgent() throws IllegalStateException {
		if(!this.ice) {
			throw new IllegalStateException("ICE is not enabled on this media channel");
		}
		
		// Start ICE Agent only if necessary
		if(this.iceAgent.isRunning()) {
			this.iceAgent.stop();
		}
	}
	
	private class IceListener implements IceEventListener {

		@Override
		public void onSelectedCandidates(SelectedCandidatesEvent event) {
			try {
				logger.info("Finished ICE candidates selection! Preparing for binding.");
				
				// Get selected RTP candidate for audio channel
				IceAgent agent = event.getSource();
				CandidatePair rtpCandidate = agent.getSelectedRtpCandidate("audio");
				// Bind candidate to RTP audio channel
				rtpChannel.bind(rtpCandidate.getChannel());

				CandidatePair rtcpCandidate = agent.getSelectedRtcpCandidate("audio");
				if(rtcpCandidate != null) {
					rtcpChannel.bind(rtcpCandidate.getChannel());
				}
			} catch (IOException e) {
				onRtpFailure("Could not select ICE candidates: " + e.getMessage());
			}
		}
	}
	
	/*
	 * DTLS
	 */
	public void enableDTLS(String remoteFingerprint) throws IllegalStateException {
		if(this.dtls) {
			throw new IllegalStateException("DTLS is already enabled on this channel");
		}
		
		if(!this.ice) {
			throw new IllegalStateException("ICE must be enabled on this channel");
		}
		
		this.rtpChannel.enableSRTP(remoteFingerprint, this.iceAgent);
		if(!this.rtcpMux) {
			rtcpChannel.enableSRTCP(remoteFingerprint, this.iceAgent);
		}
	}
	
	public void disableDTLS() throws IllegalStateException {
		
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
	 * SDP
	 * XXX Create an SDP factory
	 */
	public MediaDescriptionField getMediaDescriptor(RTPFormats supportedFormats) {
		MediaDescriptionField mediaField = new MediaDescriptionField();
		mediaField.setMedia(this.mediaType);
		mediaField.setPort(this.rtpChannel.getLocalPort());
		mediaField.setProtocol(MediaProfile.RTP_AVP);
		mediaField.setConnection(new ConnectionField("IN", "IP4", this.rtpChannel.getLocalHost()));
		mediaField.setPtime(new PacketTimeAttribute(20));
		mediaField.setRtcp(new RtcpAttribute(this.rtcpChannel.getLocalPort(), "IN", "IP4", this.rtcpChannel.getLocalHost()));
		
		// Media formats
		supportedFormats.rewind();
		while(supportedFormats.hasMore()) {
			RTPFormat f = supportedFormats.next();
			AudioFormat audioFormat = (AudioFormat) f.getFormat();

			RtpMapAttribute rtpMap = new RtpMapAttribute();
			rtpMap.setPayloadType(f.getID());
			rtpMap.setCodec(f.getFormat().getName().toString());
			rtpMap.setClockRate(f.getClockRate());
			if(audioFormat.getChannels() > 1) {
				rtpMap.setCodecParams(audioFormat.getChannels());
			}
			if(audioFormat.getOptions() != null) {
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
		
		if(this.ice) {
			LocalCandidateWrapper rtpCandidate = this.iceAgent.getMediaStream(this.mediaType).getRtpComponent().getDefaultLocalCandidate();
			rtpAddress = rtpCandidate.getCandidate().getHostString();
			rtpPort = rtpCandidate.getCandidate().getPort();

			if(this.rtcpMux) {
				rtcpAddress = rtpAddress;
				rtcpPort = rtpPort;
			} else {
				CandidatePair rtcpCandidate = this.iceAgent.getSelectedRtcpCandidate("audio");
				rtcpAddress = rtcpCandidate.getLocalAddress();
				rtcpPort = rtcpCandidate.getLocalPort();
			}
			
			// Fix session-level attribute
			answer.getConnection().setAddress(rtpAddress);
		}

		MediaDescriptionField audioDescription = new MediaDescriptionField(answer);
		audioDescription.setMedia("audio");
		audioDescription.setPort(rtpPort);
		audioDescription.setProtocol(this.webrtc ? MediaProfile.RTP_SAVPF : MediaProfile.RTP_AVP);
		audioDescription.setConnection(new ConnectionField("IN", "IP4", rtpAddress));
		audioDescription.setPtime(new PacketTimeAttribute(20));
		audioDescription.setRtcp(new RtcpAttribute(rtcpPort, "IN", "IP4", rtcpAddress));
		if(this.audioRtcpMux) {
			audioDescription.setRtcpMux(new RtcpMuxAttribute());
		}
		
		// ICE attributes
		if(this.ice) {
			audioDescription.setIceUfrag(new IceUfragAttribute(this.iceAgent.getUfrag()));
			audioDescription.setIcePwd(new IcePwdAttribute(this.iceAgent.getPassword()));
			
			IceMediaStream audioIce = iceAgent.getMediaStream("audio");
			List<LocalCandidateWrapper> rtpCandidates = audioIce.getRtpComponent().getLocalCandidates();
			for (LocalCandidateWrapper candidate : rtpCandidates) {
				audioDescription.addCandidate(processCandidate(candidate.getCandidate()));
			}
			if(!this.audioRtcpMux) {
				List<LocalCandidateWrapper> rtcpCandidates = audioIce.getRtcpComponent().getLocalCandidates();
				for (LocalCandidateWrapper candidate : rtcpCandidates) {
					audioDescription.addCandidate(processCandidate(candidate.getCandidate()));
				}
			}
		}
		
		// Media formats
		this.audioNegotiatedFormats.rewind();
		while(this.audioNegotiatedFormats.hasMore()) {
			RTPFormat f = this.audioNegotiatedFormats.next();
			AudioFormat audioFormat = (AudioFormat) f.getFormat();

			RtpMapAttribute rtpMap = new RtpMapAttribute();
			rtpMap.setPayloadType(f.getID());
			rtpMap.setCodec(f.getFormat().getName().toString());
			rtpMap.setClockRate(f.getClockRate());
			if(audioFormat.getChannels() > 1) {
				rtpMap.setCodecParams(audioFormat.getChannels());
			}
			if(audioFormat.getOptions() != null) {
				rtpMap.setParameters(new FormatParameterAttribute(f.getID(), audioFormat.getOptions().toString()));
			}
			audioDescription.addPayloadType(f.getID());
			audioDescription.addFormat(rtpMap);
		}
		
		// DTLS attributes
		if(this.webrtc) {
			audioDescription.setSetup(new SetupAttribute(SetupAttribute.PASSIVE));
			String fingerprint = this.rtpAudioChannel.getWebRtcLocalFingerprint().toString();
			int whitespace = fingerprint.indexOf(" ");
			String fingerprintHash = fingerprint.substring(0, whitespace);
			String fingerprintValue = fingerprint.substring(whitespace + 1);
			answer.setFingerprint(new FingerprintAttribute(fingerprintHash, fingerprintValue));
		}
		
		audioDescription.setConnectionMode(new ConnectionModeAttribute(ConnectionModeAttribute.SENDRECV));
		
		SsrcAttribute ssrcAttribute = new SsrcAttribute(Long.toString(this.ssrc));
		ssrcAttribute.addAttribute("cname", this.cname);
		audioDescription.setSsrc(ssrcAttribute);
		
		answer.addMediaDescription(audioDescription);
		
		return null;
	}
}
