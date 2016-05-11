/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.mobicents.media.control.mgcp.connection.remote;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.connection.AbstractMgcpConnection;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionState;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.listener.MgcpConnectionListener;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptionType;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.CnameGenerator;
import org.mobicents.media.server.impl.rtp.RtpListener;
import org.mobicents.media.server.impl.rtp.channels.AudioChannel;
import org.mobicents.media.server.impl.rtp.channels.MediaChannelProvider;
import org.mobicents.media.server.impl.rtp.sdp.SdpFactory;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SessionDescription;
import org.mobicents.media.server.io.sdp.SessionDescriptionParser;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * Type of connection that connects one endpoint to a remote peer.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRemoteConnection extends AbstractMgcpConnection implements RtpListener {

    private static final Logger log = Logger.getLogger(MgcpRemoteConnection.class);

    // Connection Properties
    private final String localAddress;
    private final String externalAddress;
    private final String cname;
    private boolean outbound;
    private boolean webrtc;
    private SessionDescription localSdp;
    private SessionDescription remoteSdp;

    // Media Channels
    private final AudioChannel audioChannel;

    // Listeners
    private MgcpConnectionListener connectionListener;

    public MgcpRemoteConnection(int identifier, MediaChannelProvider channelProvider) {
        super(identifier);

        // Connection Properties
        this.localAddress = channelProvider.getLocalAddress();
        this.externalAddress = channelProvider.getExternalAddress();
        this.cname = CnameGenerator.generateCname();
        this.outbound = false;
        this.webrtc = false;
        this.localSdp = null;
        this.remoteSdp = null;

        // Media Channels
        this.audioChannel = channelProvider.provideAudioChannel();
        this.audioChannel.setCname(this.cname);
    }

    public void setConnectionListener(MgcpConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public void setMode(ConnectionMode mode) throws IllegalStateException {
        super.setMode(mode);
        this.audioChannel.setConnectionMode(mode);
    }

    @Override
    public String halfOpen(LocalConnectionOptions options) throws MgcpConnectionException {
        synchronized (this.stateLock) {
            switch (this.state) {
                case CLOSED:
                    // Update state
                    this.state = MgcpConnectionState.HALF_OPEN;

                    // Read options
                    String webrtcOption = options.get(LocalConnectionOptionType.WEBRTC);

                    // Setup audio channel
                    this.outbound = true;
                    this.webrtc = webrtcOption != null && webrtcOption.equals("true");
                    this.audioChannel.open();
                    try {
                        this.audioChannel.bind(false, this.webrtc);
                    } catch (IOException e) {
                        throw new MgcpConnectionException("Could not bind audio channel " + this.cname, e);
                    }

                    // Enable ICE and DTLS for WebRTC calls
                    if (this.webrtc) {
                        this.audioChannel.enableICE(this.externalAddress, true);
                        this.audioChannel.enableDTLS();
                    }

                    // Generate SDP offer
                    this.localSdp = SdpFactory.buildSdp(true, this.localAddress, this.externalAddress, this.audioChannel);
                    return this.localAddress;
                default:
                    throw new MgcpConnectionException("Cannot half-open connection " + this.getHexIdentifier()
                            + " because state is " + this.state.name());
            }
        }
    }

    @Override
    public String open(String sdp) throws MgcpConnectionException {
        synchronized (this.stateLock) {
            switch (this.state) {
                case CLOSED:
                case HALF_OPEN:
                    // Update state
                    this.state = MgcpConnectionState.OPEN;

                    // Parse remote SDP
                    try {
                        this.remoteSdp = SessionDescriptionParser.parse(sdp);
                    } catch (SdpException e) {
                        throw new MgcpConnectionException(e.getMessage(), e);
                    }

                    // Open connection
                    openConnection();
                    break;

                default:
                    throw new MgcpConnectionException(
                            "Cannot open connection " + this.getHexIdentifier() + " because state is " + this.state.name());
            }
        }
        return null;
    }

    private void openConnection() throws MgcpConnectionException {
        this.audioChannel.open();
        try {
            if (this.outbound) {
                openOutboundConnection();
            } else {
                openInboundConnection();
            }
        } catch (IOException e) {
            throw new MgcpConnectionException(e.getMessage(), e);
        }
    }

    /**
     * Sets the remote peer based on the remote SDP description.
     * 
     * <p>
     * In this case, the connection belongs to an inbound call. So, the remote SDP is the offer and, as result, the proper
     * answer is generated.<br>
     * The SDP answer can be sent later to the remote peer.
     * </p>
     * 
     * @throws IOException If an error occurs while setting up the remote peer
     */
    private void openInboundConnection() throws IOException {
        // Open audio channel
        this.audioChannel.open();

        // Connect audio channel to remote peer
        MediaDescriptionField remoteAudio = this.remoteSdp.getMediaDescription("audio");
        setupAudioChannelInbound(remoteAudio);

        // Generate SDP answer
        this.localSdp = SdpFactory.buildSdp(false, this.localAddress, this.externalAddress, this.audioChannel);

        // Reject video stream (not supported)
        MediaDescriptionField remoteVideo = this.remoteSdp.getMediaDescription("video");
        if (remoteVideo != null) {
            SdpFactory.rejectMediaField(this.localSdp, remoteVideo);
        }

        // Reject data stream (not supported)
        MediaDescriptionField remoteApplication = this.remoteSdp.getMediaDescription("application");
        if (remoteApplication != null) {
            SdpFactory.rejectMediaField(this.localSdp, remoteApplication);
        }
    }

    /**
     * Reads the remote SDP offer and sets up the available resources according to the call type.
     * 
     * @param remoteAudio The description of the remote audio channel.
     * 
     * @throws IOException When binding the audio data channel or when failing to negotiate codecs.
     */
    private void setupAudioChannelInbound(MediaDescriptionField remoteAudio) throws IOException {
        // Negotiate audio codecs
        this.audioChannel.negotiateFormats(remoteAudio);
        if (!this.audioChannel.containsNegotiatedFormats()) {
            throw new IOException("Audio codecs were not supported");
        }

        // Bind audio channel to an address provided by UdpManager
        this.audioChannel.bind(false, remoteAudio.isRtcpMux());

        boolean enableIce = remoteAudio.containsIce();
        if (enableIce) {
            // Enable ICE. Wait for ICE handshake to finish before connecting RTP/RTCP channels
            this.audioChannel.enableICE(this.externalAddress, remoteAudio.isRtcpMux());
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
     * Sets the remote peer based on the remote SDP description.
     * 
     * <p>
     * In this case, the connection belongs to an inbound call. So, the remote SDP is the answer which implies that this
     * connection already generated the proper offer.
     * </p>
     * 
     * @throws IOException If an error occurs while setting up the remote peer
     */
    private void openOutboundConnection() throws IOException {
        // Setup audio channel
        MediaDescriptionField remoteAudio = this.remoteSdp.getMediaDescription("audio");

        // Set remote DTLS fingerprint
        if (this.audioChannel.isDtlsEnabled()) {
            FingerprintAttribute fingerprint = remoteAudio.getFingerprint();
            this.audioChannel.setRemoteFingerprint(fingerprint.getHashFunction(), fingerprint.getFingerprint());
        }
        setupAudioChannelOutbound(remoteAudio);
    }

    /**
     * Reads the remote SDP answer and sets up the proper media channels.
     * 
     * @param remoteAudio The description of the remote audio channel.
     * 
     * @throws IOException When binding the audio data channel or when failing to negotiate codecs.
     */
    private void setupAudioChannelOutbound(MediaDescriptionField remoteAudio) throws IOException {
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
    public void close() throws MgcpConnectionException {
        synchronized (this.stateLock) {
            switch (this.state) {
                case HALF_OPEN:
                case OPEN:
                    // Update connection state
                    this.state = MgcpConnectionState.CLOSED;

                    // Deactivate connection
                    setMode(ConnectionMode.INACTIVE);

                    // Close audio channel
                    if (this.audioChannel.isOpen()) {
                        this.audioChannel.close();
                    }

                    // Reset internal state
                    reset();

                    break;
                default:
                    throw new MgcpConnectionException(
                            "Cannot close connection " + this.getHexIdentifier() + "because state is " + this.state.name());
            }
        }
    }

    @Override
    public AudioComponent getAudioComponent() {
        return this.audioChannel.getAudioComponent();
    }

    @Override
    public OOBComponent getOutOfBandComponent() {
        return this.audioChannel.getAudioOobComponent();
    }

    @Override
    public void onRtpFailure(Throwable e) {
        String message = "RTP channel failure on connection " + this.cname + "!";
        if (e != null && e.getMessage() != null) {
            message += " Reason: " + e.getMessage();
        }
        onRtpFailure(message);
    }

    @Override
    public void onRtpFailure(String message) {
        try {
            // RTP is mandatory, if it fails close everything
            close();
        } catch (Exception e) {
            log.warn("Failed to elegantly close connection " + this.cname + " after RTP failure", e);
        } finally {
            // Warn connection listener about failure
            if (this.connectionListener != null) {
                this.connectionListener.onConnectionFailure(this);
            }
        }
    }

    @Override
    public void onRtcpFailure(Throwable e) {
        String message = "Closing RTCP channel on connection " + this.cname + " due to failure!";
        if (e != null && e.getMessage() != null) {
            message += " Reason: " + e.getMessage();
        }
        onRtcpFailure(message);
    }

    @Override
    public void onRtcpFailure(String e) {
        log.warn(e);
    }

    private void reset() {
        this.outbound = false;
        this.webrtc = false;
        this.localSdp = null;
        this.remoteSdp = null;
    }

    // TODO Implement time-to-live mechanism

}
