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
	protected boolean open;

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

	/**
	 * Constructs a new media channel containing both RTP and RTCP components.
	 * 
	 * <p>
	 * The channel supports SRTP and ICE, but these features are turned off by
	 * default.
	 * </p>
	 * 
	 * @param mediaType
	 *            The type of media flowing in the channel
	 * @param wallClock
	 *            The wall clock used to synchronize media flows
	 * @param channelsManager
	 *            The RTP and RTCP channel provider
	 */
	protected MediaChannel(String mediaType, Clock wallClock, ChannelsManager channelsManager) {
		this.ssrc = 0L;
		this.mediaType = mediaType;

		this.clock = new RtpClock(wallClock);
		this.oobClock = new RtpClock(wallClock);

		this.statistics = new RtpStatistics(clock, this.ssrc);

		this.rtpChannel = channelsManager.getRtpChannel(this.statistics, this.clock, this.oobClock);
		this.rtcpChannel = channelsManager.getRtcpChannel(this.statistics);
		
		this.offeredFormats = new RTPFormats();
		this.negotiatedFormats = new RTPFormats();

		this.rtcpMux = false;
		this.ice = false;
		this.dtls = false;
		this.open = false;
	}

	/**
	 * Gets the synchronization source of the channel.
	 * 
	 * @return The unique SSRC identifier of the channel
	 */
	public long getSsrc() {
		return ssrc;
	}

	/**
	 * Sets the synchronization source of the channel.
	 * 
	 * @param ssrc
	 *            The unique SSRC identifier of the channel
	 */
	public void setSsrc(long ssrc) {
		this.ssrc = ssrc;
	}

	/**
	 * Gets the CNAME of the channel.
	 * 
	 * @return The CNAME associated with the channel
	 */
	public String getCname() {
		return cname;
	}

	/**
	 * Sets the CNAME of the channel.
	 * 
	 * <p>
	 * This attribute associates a media source with its endpoint, so it must be
	 * shared between all media channels owned by the same connection.
	 * </p>
	 * 
	 * @param cname The Canonical End-Point Identifier of the channel
	 */
	public void setCname(String cname) {
		this.cname = cname;
		this.statistics.setCname(cname);
	}

	/**
	 * Enables the channel and activates it's resources.
	 */
	public void open() {
		// generate a new unique identifier for the channel
		this.ssrc = SsrcGenerator.generateSsrc();
		this.open = true;
		
		if(logger.isDebugEnabled()) {
			logger.debug(this.mediaType + " channel " + this.ssrc + " is open");
		}
	}

	/**
	 * Disables the channel and deactivates it's resources.
	 * 
	 * @throws IllegalStateException
	 *             When an attempt is done to deactivate the channel while
	 *             inactive.
	 */
	public void close() throws IllegalStateException {
		if (this.open) {
			// Close channels
			this.rtpChannel.close();
			if (!this.rtcpMux) {
				this.rtcpChannel.close();
			}
			
			if(logger.isDebugEnabled()) {
				logger.debug(this.mediaType + " channel " + this.ssrc + " is closed");
			}

			// Reset state
			reset();
			this.open = false;
		} else {
			throw new IllegalStateException("Channel is already inactive");
		}
	}

	/**
	 * Resets the state of the channel.
	 * 
	 * Should be invoked whenever there is intention of reusing the same channel
	 * for different calls.
	 */
	private void reset() {
		// Reset channels
		if (this.rtcpMux) {
			this.rtcpMux = false;
			this.rtpChannel.enableRtcpMux(false);
		}

		// Reset ICE
		if (this.ice) {
			disableICE();
		}

		// Reset WebRTC
		if (this.dtls) {
			disableDTLS();
		}
		
		// Reset statistics
		this.statistics.reset();
		this.cname = "";
		this.ssrc = 0L;
	}

	/**
	 * Indicates whether the channel is active or not.
	 * 
	 * @return Returns true if the channel is active. Returns false otherwise.
	 */
	public boolean isOpen() {
		return open;
	}

	/**
	 * Indicates whether the channel is available (ready to use).
	 * 
	 * For regular SIP calls, the channel should be available as soon as it is
	 * activated.<br>
	 * But for WebRTC calls the channel will only become available as soon as
	 * the DTLS handshake completes.
	 * 
	 * @return Returns true if the channel is available. Returns false otherwise.
	 */
	public boolean isAvailable() {
		boolean available = this.rtpChannel.isAvailable();
		if (!this.rtcpMux) {
			available = available && this.rtcpChannel.isAvailable();
		}
		return available;
	}

	/**
	 * Sets the RTP listener of the channel.
	 * 
	 * <p>
	 * The listener will always be warned whenever an RTP or RTCP failure
	 * occurs. Such listener must be capable of handling such situations gracefully.
	 * </p>
	 * 
	 * @param listener
	 *            The RTP listener capable of handling RTP/RTCP failures.
	 */
	public void setRtpListener(RtpListener listener) {
		this.rtpListener = listener;
	}

	/**
	 * Sets the input Digital Signaling Processor (DSP) of the RTP component.
	 * 
	 * @param dsp The input DSP of the RTP component
	 */
	public void setInputDsp(Processor dsp) {
		this.rtpChannel.setInputDsp(dsp);
	}

	/**
	 * Gets the input Digital Signaling Processor (DSP) of the RTP component.
	 * 
	 * @return The input DSP of the RTP component
	 */
	public Processor getInputDsp() {
		return this.rtpChannel.getInputDsp();
	}

	/**
	 * Sets the output Digital Signaling Processor (DSP) of the RTP component.
	 * 
	 * @param dsp The input DSP of the RTP component
	 */
	public void setOutputDsp(Processor dsp) {
		this.rtpChannel.setOutputDsp(dsp);
	}

	/**
	 * Gets the output Digital Signaling Processor (DSP) of the RTP component.
	 * 
	 * @return The input DSP of the RTP component
	 */
	public Processor getOutputDsp() {
		return this.rtpChannel.getOutputDsp();
	}

	/**
	 * Sets the connection mode of the channel, affecting the receiving and
	 * transmitting capabilities of the underlying RTP component.
	 * 
	 * @param mode
	 *            The new connection mode of the RTP component
	 */
	public void setConnectionMode(ConnectionMode mode) {
		this.rtpChannel.updateMode(mode);
	}

	/**
	 * Sets the supported codecs of the RTP components.
	 * 
	 * @param formats
	 *            The supported codecs resulting from SDP negotiation
	 */
	private void setFormats(RTPFormats formats) {
		try {
			this.rtpChannel.setFormatMap(formats);
			this.rtpChannel.setOutputFormats(formats.getFormats());
		} catch (FormatNotSupportedException e) {
			// Never happens
			logger.warn("Could not set output formats", e);
		}
	}
	
	/**
	 * Gets the supported codecs of the RTP components.
	 * 
	 * @return The codecs currently supported by the RTP component
	 */
	public RTPFormats getFormats() {
		return this.rtpChannel.getFormatMap();
	}

	/**
	 * Binds the RTP and RTCP components to a suitable address and port.
	 * 
	 * @param isLocal
	 *            Whether the binding address is in local range.
	 * @param rtcpMux
	 *            Whether RTCP multiplexing is supported.<br>
	 *            If so, both RTP and RTCP components will be merged into one
	 *            channel only. Otherwise, the RTCP component will be bound to
	 *            the odd port immediately after the RTP port.
	 * @throws IOException
	 *             When channel cannot be bound to an address.
	 * @throws IllegalStateException
	 *             Binding operation is not allowed when ICE is active.
	 */
	public void bind(boolean isLocal, boolean rtcpMux) throws IOException, IllegalStateException {
		if(this.ice) {
			throw new IllegalStateException("Cannot bind when ICE is enabled");
		}
		this.rtpChannel.bind(isLocal);
		if(!rtcpMux) {
			this.rtcpChannel.bind(isLocal, this.rtpChannel.getLocalPort() + 1);
		}
		this.rtcpMux = rtcpMux;
		
		if(logger.isDebugEnabled()) {
			logger.debug(this.mediaType + " RTP channel " + this.ssrc + " is bound to " + this.rtpChannel.getLocalHost() + ":" + this.rtpChannel.getLocalPort());
			if(rtcpMux) {
				logger.debug(this.mediaType + " is multiplexing RTCP");
			} else {
				logger.debug(this.mediaType + " RTCP channel " + this.ssrc + " is bound to " + this.rtcpChannel.getLocalHost() + ":" + this.rtcpChannel.getLocalPort());
			}
		}
	}
	
	/**
	 * Indicates whether the media channel is multiplexing RTCP or not.
	 * 
	 * @return Returns true if using rtcp-mux. Returns false otherwise.
	 */
	public boolean isRtcpMux() {
		return this.rtcpMux;
	}

	/**
	 * Connected the RTP component to the remote peer.
	 * 
	 * <p>
	 * Once connected, the RTP component can only send/received traffic to/from
	 * the remote peer.
	 * </p>
	 * 
	 * @param address
	 *            The address of the remote peer
	 */
	public void connectRtp(SocketAddress address) {
		this.rtpChannel.setRemotePeer(address);
		
		if(logger.isDebugEnabled()) {
			logger.debug(this.mediaType + " RTP channel " + this.ssrc + " connected to remote peer " + address.toString());
		}
	}

	/**
	 * Connected the RTP component to the remote peer.
	 * 
	 * <p>
	 * Once connected, the RTP component can only send/received traffic to/from
	 * the remote peer.
	 * </p>
	 * 
	 * @param address
	 *            The address of the remote peer
	 * @param port
	 *            The port of the remote peer
	 */
	public void connectRtp(String address, int port) {
		this.connectRtp(new InetSocketAddress(address, port));
	}
	
	/**
	 * Binds the RTCP component to a suitable address and port.
	 * 
	 * @param isLocal
	 *            Whether the binding address must be in local range.
	 * @param port
	 *            A specific port to bind to
	 * @throws IOException
	 *             When the RTCP component cannot be bound to an address.
	 * @throws IllegalStateException
	 *             The binding operation is not allowed if ICE is active
	 */
	public void bindRtcp(boolean isLocal, int port) throws IOException, IllegalStateException {
		if(this.ice) {
			throw new IllegalStateException("Cannot bind when ICE is enabled");
		}
		this.rtcpChannel.bind(isLocal, port);
		this.rtcpMux = (port == this.rtpChannel.getLocalPort());
	}

	/**
	 * Connects the RTCP component to the remote peer.
	 * 
	 * <p>
	 * Once connected, the RTCP component can only send/received traffic to/from
	 * the remote peer.
	 * </p>
	 * 
	 * @param address
	 *            The address of the remote peer
	 */
	public void connectRtcp(SocketAddress remoteAddress) {
		this.rtcpChannel.setRemotePeer(remoteAddress);
		
		if(logger.isDebugEnabled()) {
			logger.debug(this.mediaType + " RTCP channel " + this.ssrc + " has connected to remote peer " + remoteAddress.toString());
		}
	}

	/**
	 * Connects the RTCP component to the remote peer.
	 * 
	 * <p>
	 * Once connected, the RTCP component can only send/received traffic to/from
	 * the remote peer.
	 * </p>
	 * 
	 * @param address
	 *            The address of the remote peer
	 * @param port
	 *            A specific port to connect to
	 */
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

	/**
	 * Resets the list of supported codecs.
	 */
	public void resetFormats() {
		this.offeredFormats.clean();
		this.negotiatedFormats.clean();
		setFormats(this.supportedFormats);
	}
	
	/**
	 * Negotiates the list of supported codecs with the remote peer over SDP.
	 * 
	 * @param media
	 *            The corresponding media description of the remote peer which
	 *            contains the payload types.
	 */
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
	}
	
	/**
	 * Indicates whether the channel has successfully negotiated supported
	 * codecs over SDP.
	 * 
	 * @return Returns true if codecs have been negotiated. Returns false
	 *         otherwise.
	 */
	public boolean hasNegotiatedFormats() {
		return !negotiatedFormats.isEmpty() && negotiatedFormats.hasNonDTMF();
	}

	/*
	 * ICE
	 */
	/**
	 * Enables ICE on the channel.
	 * 
	 * <p>
	 * An ICE-enabled channel will start an ICE Agent which gathers local
	 * candidates and listens to incoming STUN requests as a mean to select the
	 * proper address to be used during the call.
	 * </p>
	 * <p>
	 * As such <b>bind() operations are not allowed when ICE is enabled</b>
	 * </p>
	 * 
	 * @param externalAddress
	 *            The public address of the Media Server. Used for SRFLX
	 *            candidates.
	 * @param rtcpMux
	 *            Whether RTCP is multiplexed or not. Affects number of
	 *            candidates.
	 * @throws UnknownHostException
	 *             When the external address is invalid
	 * @throws IllegalStateException
	 *             Attempt to enable ICE when channel is inactive or ICE is
	 *             already enabled.
	 */
	public void enableICE(String externalAddress, boolean rtcpMux) throws UnknownHostException, IllegalStateException {
		if (!this.open) {
			throw new IllegalStateException("Media Channel is not active");
		}
		
		if (this.ice) {
			throw new IllegalStateException("ICE is already enabled");
		}

		this.ice = true;
		this.rtcpMux = rtcpMux;
		
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
		
		if(logger.isDebugEnabled()) {
			logger.debug(this.mediaType + " channel " + this.ssrc + " enabled ICE");
		}
	}

	/**
	 * Disables ICE and closes ICE-related resources
	 */
	public void disableICE() throws IllegalStateException {
		if (!this.open) {
			throw new IllegalStateException("Media Channel is not active");
		}
		
		if (!this.ice) {
			throw new IllegalStateException("ICE is not enabled");
		}

		// Stop the ICE agent
		if(this.iceAgent.isRunning()) {
			this.iceAgent.stop();
		}
		this.iceAgent.reset();
		this.ice = false;
		
		if(logger.isDebugEnabled()) {
			logger.debug(this.mediaType + " channel " + this.ssrc + " disabled ICE");
		}
	}

	/**
	 * Indicates whether ICE is active or not.
	 * 
	 * @return Returns true if ICE is enabled. Returns false otherwise.
	 */
	public boolean hasICE() {
		return this.ice;
	}

	/**
	 * Asks the underlying ICE Agent to gather candidates on all available
	 * network interfaces.
	 * 
	 * @param portManager
	 *            The manager that restricts interval for port lookup.
	 * @throws HarvestException
	 *             When an error occurs when gathering candidates on available
	 *             network interfaces.
	 * @throws IllegalStateException
	 *             ICE candidates cannot be gathered while ICE is inactive.
	 */
	public void gatherIceCandidates(PortManager portManager) throws HarvestException, IllegalStateException {
		if (!this.ice) {
			throw new IllegalStateException("ICE is not enabled on this media channel");
		}
		this.iceAgent.harvest(portManager);
	}

	/**
	 * Runs the ICE agent which starts listening for incoming STUN connectivity
	 * checks from the remote peer.
	 * 
	 * <p>
	 * Whenever a successful connectivity check is received, the ICE Agent
	 * passes the selected candidate using the ICE Listener registered on this
	 * channel.<br>
	 * Upon receiving the event, the listener will bind the RTP and RTCP
	 * components directly to the DatagramChannels provided by the ICE Agent.
	 * The media channel then becomes ready for media flowing.
	 * </p>
	 * 
	 * <p>
	 * Starting the ICE Agent multiple times has no effect.
	 * </p>
	 * 
	 * @throws IllegalStateException
	 *             The ICE Agent cannot be started while ICE is inactive
	 */
	public void startIceAgent() throws IllegalStateException {
		if (!this.ice) {
			throw new IllegalStateException("ICE is not enabled on this media channel");
		}

		// Start ICE Agent only if necessary
		if (!this.iceAgent.isRunning()) {
			this.iceAgent.start();
		}
	}

	/**
	 * Stops the ICE Agent.
	 * 
	 * <p>
	 * Has no effect if the agent is not running.
	 * </p>
	 * 
	 * @throws IllegalStateException
	 *             The ICE Agent cannot be stopped while ICE is inactive.
	 */
	public void stopIceAgent() throws IllegalStateException {
		if (!this.ice) {
			throw new IllegalStateException("ICE is not enabled on this media channel");
		}

		// Start ICE Agent only if necessary
		if (this.iceAgent.isRunning()) {
			this.iceAgent.stop();
		}
	}

	/**
	 * Implementation of a listener that handles ICE-related events.
	 * 
	 * <p>
	 * Whenever an ICE Agent selects the effective candidate pairs to be used on
	 * an ICE-enabled call, it will fire a {@link SelectedCandidatesEvent} which
	 * will be handled by this listener.
	 * <p>
	 * <p>
	 * The listener will then grab the underlyind DatagramChannels of each
	 * selected candidate and bind them directly to the RTP and RTCP components
	 * of the media channel.<br>
	 * The media channel is then prepared for media flowing.
	 * </p>
	 * 
	 * @author Henrique Rosa (henrique.rosa@telestax.com)
	 * 
	 */
	private class IceListener implements IceEventListener {

		@Override
		public void onSelectedCandidates(SelectedCandidatesEvent event) {
			try {
				if(logger.isDebugEnabled()) {
					logger.debug("Finished ICE candidates selection for " + mediaType + " channel  " + ssrc + "! Preparing for binding.");
				}

				// Get selected RTP candidate for audio channel
				IceAgent agent = event.getSource();
				CandidatePair rtpCandidate = agent.getSelectedRtpCandidate(mediaType);
				// Bind candidate to RTP audio channel
				rtpChannel.bind(rtpCandidate.getChannel());

				CandidatePair rtcpCandidate = agent.getSelectedRtcpCandidate(mediaType);
				if (rtcpCandidate != null) {
					rtcpChannel.bind(rtcpCandidate.getChannel());
				}
			} catch (IOException e) {
				// Warn RTP listener a failure happened and connection must be closed
				rtpListener.onRtpFailure("Could not select ICE candidates: " + e.getMessage());
			}
		}
	}

	/*
	 * DTLS
	 */
	/**
	 * Enables DTLS on the channel. RTP and RTCP packets flowing through this
	 * channel will be secured.
	 * 
	 * @param remoteFingerprint
	 *            The DTLS finger print of the remote peer.
	 * @throws IllegalStateException
	 *             Cannot be invoked when DTLS is already enabled
	 */
	public void enableDTLS(String remoteFingerprint) throws IllegalStateException {
		if (this.dtls) {
			throw new IllegalStateException("DTLS is already enabled on this channel");
		}

		this.rtpChannel.enableSRTP(remoteFingerprint, this.iceAgent);
		if (!this.rtcpMux) {
			rtcpChannel.enableSRTCP(remoteFingerprint, this.iceAgent);
		}
		this.dtls = true;
		
		if(logger.isDebugEnabled()) { 
			logger.debug(this.mediaType + " channel " + this.ssrc + " enabled DTLS");
		}
	}

	/**
	 * Disables DTLS and closes related resources.
	 * 
	 * @throws IllegalStateException
	 *             Cannot be invoked when DTLS is already disabled
	 */
	public void disableDTLS() throws IllegalStateException {
		if (!this.dtls) {
			throw new IllegalStateException("DTLS is already disabled on this channel");
		}
		
		this.rtpChannel.disableSRTP();
		if(!this.rtcpMux) {
			this.rtcpChannel.disableSRTCP();
		}
		this.dtls = false;
		
		if(logger.isDebugEnabled()) { 
			logger.debug(this.mediaType + " channel " + this.ssrc + " disabled DTLS");
		}

	}

	/*
	 * Statistics
	 */
	/**
	 * Gets the number of RTP packets received during the current call.
	 * 
	 * @return The number of packets received
	 */
	public long getPacketsReceived() {
		if(this.open) {
			return this.statistics.getRtpPacketsReceived();
		}
		return 0;
	}

	/**
	 * Gets the number of bytes received during the current call.
	 * <p>
	 * <b>This number reflects only the payload of all RTP packets</b> received
	 * up to the moment the method is invoked.
	 * </p>
	 * 
	 * @return The number of bytes received.
	 */
	public long getOctetsReceived() {
		if(this.open) {
			return this.statistics.getRtpOctetsReceived();
		}
		return 0;
	}

	/**
	 * Gets the number of RTP packets sent during the current call.
	 * 
	 * @return The number of packets sent
	 */
	public long getPacketsSent() {
		if(this.open) {
			return this.statistics.getRtpPacketsSent();
		}
		return 0;
	}

	/**
	 * Gets the number of bytes sent during the current call.
	 * <p>
	 * <b>This number reflects only the payload of all RTP packets</b> sent
	 * up to the moment the method is invoked.
	 * </p>
	 * 
	 * @return The number of bytes sent.
	 */
	public long getOctetsSent() {
		if(this.open) {
			return this.statistics.getRtpOctetsSent();
		}
		return 0;
	}

	/**
	 * Gets the current jitter of the call.
	 * 
	 * <p>
	 * The jitter is an estimate of the statistical variance of the RTP data packet
	 * interarrival time, measured in timestamp units and expressed as an
	 * unsigned integer.
	 * </p>
	 * 
	 * @return The current jitter.
	 */
	public long getJitter() {
		if(this.open) {
			return this.statistics.getMember(this.ssrc).getJitter();
		}
		return 0;
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
		
		// DTLS attributes
		if(this.dtls) {
			String fingerprint = this.rtpChannel.getWebRtcLocalFingerprint().toString();
			int whitespace = fingerprint.indexOf(" ");
			audioDescription.setFingerprint(new FingerprintAttribute(fingerprint.substring(0, whitespace), fingerprint.substring(whitespace + 1)));
			audioDescription.setSetup(new SetupAttribute(SetupAttribute.PASSIVE));
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
