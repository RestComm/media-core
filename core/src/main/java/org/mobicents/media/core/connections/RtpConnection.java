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
import org.mobicents.media.server.component.CompoundComponent;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.CnameGenerator;
import org.mobicents.media.server.impl.rtp.RtpListener;
import org.mobicents.media.server.impl.rtp.channels.AudioSession;
import org.mobicents.media.server.impl.rtp.channels.RtpSession;
import org.mobicents.media.server.impl.rtp.channels.VideoSession;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
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
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.utils.Text;

/**
 * Connection to a remote peer where RTP traffic flows.
 * 
 * @author Oifa Yulian
 * @author Amit Bhayani
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see AbstractConnection
 * @see RtpListener
 */
public class RtpConnection extends AbstractConnection implements RtpListener {

    private static final Logger logger = Logger.getLogger(RtpConnection.class);

    // Core elements
    private final ChannelsManager channelsManager;

    // RTP session elements
    private String cname;
    private boolean outbound;
    private boolean localInterface;

    // Media Sessions
    private AudioSession audioSession;
    private VideoSession videoSession;

    // Session Description
    private SessionDescription localSdp;
    private SessionDescription remoteSdp;

    // Listeners
    private ConnectionFailureListener connectionFailureListener;

    /**
     * Constructs a new RTP connection with one audio channel.
     * 
     * @param id The unique ID of the connection
     * @param channelsManager The media channel provider
     * @param relayType The default relay type to be used by the connection.
     */
    public RtpConnection(int id, ChannelsManager channelsManager, RelayType relayType) {
        // Core elements
        super(id, channelsManager.getScheduler(), relayType, ConnectionType.RTP);
        this.channelsManager = channelsManager;

        // Connection state
        this.outbound = false;
        this.localInterface = false;
        this.cname = CnameGenerator.generateCname();

        // Audio Channel
        this.audioSession = this.channelsManager.getAudioChannel();
        this.audioSession.setCname(this.cname);
        this.audioSession.setRtpListener(this);
        this.audioSession.setRelayType(relayType);

        // Video Channel
        this.videoSession = this.channelsManager.getVideoChannel();
        this.videoSession.setCname(this.cname);
        this.videoSession.setRtpListener(this);
        this.videoSession.setRelayType(relayType);
    }

    /**
     * Constructs a new RTP connection with one audio channel.
     * <p>
     * The connection will perform media mixing by default. To change settings, set the relay type.
     * </p>
     * 
     * @param id The unique ID of the connection
     * @param channelsManager The media channel provider
     */
    public RtpConnection(int id, ChannelsManager channelsManager) {
        this(id, channelsManager, RelayType.MIXER);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void generateCname() {
        this.cname = CnameGenerator.generateCname();
        this.audioSession.setCname(this.cname);
        this.videoSession.setCname(this.cname);
    }

    @Override
    public String getCname() {
        return this.cname;
    }

    @Override
    public boolean getIsLocal() {
        return this.localInterface;
    }

    @Override
    public void setIsLocal(boolean isLocal) {
        this.localInterface = isLocal;
    }

    @Override
    public void setRelayType(RelayType relayType) {
        super.setRelayType(relayType);
        this.audioSession.setRelayType(relayType);
        this.videoSession.setRelayType(relayType);
    }

    @Override
    public void setOtherParty(Connection other) throws IOException {
        throw new IOException("Applicable only for a local connection");
    }

    @Override
    public void setOtherParty(Text descriptor) throws IOException {
        try {
            this.remoteSdp = SessionDescriptionParser.parse(descriptor.toString());
            setOtherParty();
        } catch (SdpException e) {
            throw new IOException("Could not parse SDP offer: " + e.getMessage(), e);
        }
    }

    /**
     * Sets the remote peer based on the received remote SDP description.
     * 
     * <p>
     * The connection will be setup according to the SDP, specifically whether the call is WebRTC (with ICE enabled) or not.
     * </p>
     * 
     * <p>
     * <b>SIP call:</b><br>
     * The RTP audio channel can be immediately bound to the configured bind address.<br>
     * By reading connection fields of the SDP offer, we can set the remote peer of the audio channel.
     * </p>
     * 
     * <p>
     * <b>WebRTC call:</b><br>
     * An ICE-lite agent is created. This agent assumes a controlled role, and starts listening for connectivity checks. This
     * agent is responsible for providing a socket for DTLS hanshake and RTP streaming, once the connection is established with
     * the remote peer.<br>
     * So, we can only bind the audio channel and set its remote peer once the ICE agent has selected the candidate pairs and
     * made such socket available.
     * </p>
     * 
     * @throws IOException If an error occurs while setting up the remote peer
     */
    private void setOtherParty() throws IOException {
        if (this.outbound) {
            setOtherPartyOutboundCall();
        } else {
            setOtherPartyInboundCall();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Audio formats: " + this.audioSession.getFormatMap());
        }
    }

    /**
     * Sets the remote peer based on the remote SDP description.
     * 
     * <p>
     * In this case, the connection belongs to an outbound call. So, the remote SDP is the offer and, as result, the proper
     * answer is generated.<br>
     * The SDP answer can be sent later to the remote peer.
     * </p>
     * 
     * @throws IOException If an error occurs while setting up the remote peer
     */
    private void setOtherPartyInboundCall() throws IOException {
        // Setup the audio channel based on remote offer
        MediaDescriptionField remoteAudio = this.remoteSdp.getMediaDescription(AVProfile.AUDIO);
        if (remoteAudio != null) {
            this.audioSession.open();
            setupInboundSession(remoteAudio, this.audioSession);
        }

        // Setup the audio channel based on remote offer
        MediaDescriptionField remoteVideo = this.remoteSdp.getMediaDescription(AVProfile.VIDEO);
        if (remoteVideo != null) {
            this.videoSession.open();
            setupInboundSession(remoteVideo, this.videoSession);
        }

        // Generate SDP answer
        String bindAddress = this.localInterface ? this.channelsManager.getLocalBindAddress() : this.channelsManager
                .getBindAddress();
        String externalAddress = this.channelsManager.getUdpManager().getExternalAddress();
        this.localSdp = SdpFactory.buildSdp(bindAddress, externalAddress, this.audioSession, this.videoSession);

        // Reject channels besides audio and video
        MediaDescriptionField remoteApplication = this.remoteSdp.getMediaDescription("application");
        if (remoteApplication != null) {
            SdpFactory.rejectMediaField(this.localSdp, remoteApplication);
        }

        // Change the state of this RTP connection from HALF_OPEN to OPEN
        try {
            this.open();
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
        MediaDescriptionField remoteAudio = this.remoteSdp.getMediaDescription(AVProfile.AUDIO);
        if (remoteAudio != null) {
            setupOutboundSession(remoteAudio, this.audioSession);
        }

        // Setup video channel
        MediaDescriptionField remoteVideo = this.remoteSdp.getMediaDescription(AVProfile.VIDEO);
        if (remoteVideo != null) {
            setupOutboundSession(remoteVideo, this.videoSession);
        }

        // Change the state of this RTP connection from HALF_OPEN to OPEN
        try {
            this.open();
        } catch (Exception e) {
            // exception is possible here when already joined
            logger.warn("Could not set connection state to OPEN", e);
        }
    }

    /**
     * Reads the remote SDP offer and sets up the available resources according to the call type.
     * 
     * <p>
     * In case of a WebRTC call, an ICE-lite agent is created. The agent will start listening for connectivity checks from the
     * remote peer.<br>
     * Also, a WebRTC handler will be enabled on the corresponding audio channel.
     * </p>
     * 
     * @param remoteSession The description of the remote media session.
     * @param localSession The local media session
     * 
     * @throws IOException When binding the audio data channel. Non-WebRTC calls only.
     * @throws SocketException When binding the audio data channel. Non-WebRTC calls only.
     */
    private void setupInboundSession(MediaDescriptionField remoteSession, RtpSession localSession) throws IOException {
        // Negotiate codecs
        localSession.negotiateFormats(remoteSession);
        if (!localSession.containsNegotiatedFormats()) {
            throw new IOException(localSession.getMediaType() + " codecs were not supported.");
        }

        boolean rtcpMux = remoteSession.isRtcpMux();
        if (remoteSession.containsIce()) {
            /*
             * Operate media channel in ICE mode.
             * 
             * Candidates will be gathered and chosen via STUN handshake. The underlying datagram channel of the selected
             * candidate will be set directly in the MediaChannel.
             */
            try {
                localSession.enableICE(this.channelsManager.getExternalAddress(), rtcpMux);
                localSession.gatherIceCandidates(this.channelsManager.getPortManager());
                localSession.startIceAgent();
            } catch (HarvestException | IllegalStateException e) {
                throw new IOException("Cannot harvest ICE candidates", e);
            }
        } else {
            // ICE is not active. Bind RTP and RTCP channels right now.
            String remoteAddr = remoteSession.getConnection().getAddress();
            localSession.bind(this.localInterface, rtcpMux);
            localSession.connectRtp(remoteAddr, remoteSession.getPort());
            localSession.connectRtcp(remoteAddr, remoteSession.getRtcpPort());
        }

        // Check whether SRTP should be active
        if (remoteSession.containsDtls()) {
            FingerprintAttribute fingerprint = remoteSession.getFingerprint();
            localSession.enableDTLS(fingerprint.getHashFunction(), fingerprint.getFingerprint());
        }
    }

    /**
     * Reads the remote SDP answer and sets up the proper media channels.
     * 
     * @param remoteSession The description of the remote media session.
     * @param localSession The local media session.
     * 
     * @throws IOException When binding the audio data channel. Non-WebRTC calls only.
     * @throws SocketException When binding the audio data channel. Non-WebRTC calls only.
     */
    private void setupOutboundSession(MediaDescriptionField remoteSession, RtpSession localSession) throws IOException {
        // Negotiate audio codecs
        localSession.negotiateFormats(remoteSession);
        if (!localSession.containsNegotiatedFormats()) {
            throw new IOException(localSession.getMediaType() + "codecs were not supported.");
        }

        // connect to remote peer - RTP
        String remoteRtpAddress = remoteSession.getConnection().getAddress();
        int remoteRtpPort = remoteSession.getPort();
        localSession.connectRtp(remoteRtpAddress, remoteRtpPort);

        // connect to remote peer - RTCP
        boolean remoteRtcpMux = remoteSession.isRtcpMux();
        if (remoteRtcpMux) {
            localSession.connectRtcp(remoteRtpAddress, remoteRtpPort);
        } else {
            RtcpAttribute remoteRtcp = remoteSession.getRtcp();
            if (remoteRtcp == null) {
                // No specific RTCP port, so default is RTP port + 1
                localSession.connectRtcp(remoteRtpAddress, remoteRtpPort + 1);
            } else {
                // Specific RTCP address and port contained in SDP
                String remoteRtcpAddress = remoteRtcp.getAddress();
                if (remoteRtcpAddress == null) {
                    // address is optional in rtcp attribute
                    // will match RTP address if not defined
                    remoteRtcpAddress = remoteRtpAddress;
                }
                int remoteRtcpPort = remoteRtcp.getPort();
                localSession.connectRtcp(remoteRtcpAddress, remoteRtcpPort);
            }
        }
    }

    @Override
    public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
        this.audioSession.setConnectionMode(mode);
        this.videoSession.setConnectionMode(mode);
        super.setMode(mode);
    }

    @Override
    public String getDescriptor() {
        return (this.localSdp == null) ? "" : this.localSdp.toString();
    }

    @Override
    public void generateOffer() throws IOException {
        // Only open and bind a new channel if not currently configured
        if (!this.audioSession.isOpen()) {
            // call is outbound since the connection is generating the offer
            this.outbound = true;

            // setup audio channel
            this.audioSession.open();
            this.audioSession.bind(this.localInterface, false);

            // setup video channel
            this.videoSession.open();
            this.videoSession.bind(this.localInterface, false);

            // generate SDP offer
            String bindAddress = this.localInterface ? this.channelsManager.getLocalBindAddress() : this.channelsManager
                    .getBindAddress();
            String externalAddress = this.channelsManager.getUdpManager().getExternalAddress();
            this.localSdp = SdpFactory.buildSdp(bindAddress, externalAddress, this.audioSession, this.videoSession);
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
        return this.audioSession.getPacketsReceived();
    }

    @Override
    public long getBytesReceived() {
        return this.audioSession.getOctetsReceived();
    }

    @Override
    public long getPacketsTransmitted() {
        return this.audioSession.getPacketsSent();
    }

    @Override
    public long getBytesTransmitted() {
        return this.audioSession.getOctetsSent();
    }

    @Override
    public double getJitter() {
        return this.audioSession.getJitter();
    }

    @Override
    public boolean isAvailable() {
        boolean available = true;

        if (audioSession.isAvailable()) {
            available &= audioSession.isAvailable();
        }

        if (videoSession.isAvailable()) {
            available &= videoSession.isAvailable();
        }
        return available;
    }

    @Override
    public String toString() {
        return "RTP Connection [" + getEndpoint().getLocalName() + "]";
    }

    /**
     * Closes any active resources (like media channels) associated with the connection.
     */
    private void closeResources() {
        if (this.audioSession.isOpen()) {
            this.audioSession.close();
        }

        if (this.videoSession.isOpen()) {
            this.videoSession.close();
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
        // RTP is mandatory, if it fails close everything
        logger.warn(message);
        onFailed();
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
        logger.warn(e);
        // Close the RTCP channel only
        // Keep the RTP channel open because RTCP is not mandatory
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
    public void setConnectionFailureListener(ConnectionFailureListener connectionFailureListener) {
        this.connectionFailureListener = connectionFailureListener;
    }

    @Override
    protected void onCreated() {
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
    protected void onOpened() {
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
        this.connectionFailureListener = null;
    }

    @Override
    public CompoundComponent getMediaComponent(String mediaType) {
        switch (mediaType) {
            case AVProfile.AUDIO:
                return this.audioSession.getMediaComponent();

            case AVProfile.VIDEO:
                return this.videoSession.getMediaComponent();

            default:
                return null;
        }
    }

}
