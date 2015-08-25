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

package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.mobicents.media.io.ice.IceAuthenticator;
import org.mobicents.media.io.ice.network.stun.StunHandler;
import org.mobicents.media.server.impl.rtcp.RtcpHandler;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.impl.srtp.DtlsListener;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.channel.MultiplexedChannel;
import org.mobicents.media.server.utils.Text;

/**
 * Channel from where RTP packets are sent and received.
 * 
 * <p>
 * Supports multiplexing of the following protocols: STUN, DTLS, (S)RTP and (S)RTCP.
 * </p>
 * 
 * @author Yulian Oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see MultiplexedChannel
 */
public class RtpTransport extends MultiplexedChannel implements DtlsListener {

    private static final Logger logger = Logger.getLogger(RtpTransport.class);

    private final static int PORT_ANY = -1;

    // Core elements
    private final UdpManager udpManager;

    // Channel attributes
    private SocketAddress remotePeer;
    private boolean bound;
    private boolean secure;
    private boolean rtcpMux;
    private boolean dtmfSupported;

    // RTP elements
    private RtpListener rtpListener;

    // Protocol handlers pipeline
    private static final int RTP_PRIORITY = 3; // a packet each 20ms
    private static final int STUN_PRIORITY = 2; // a packet each 400ms
    private static final int RTCP_PRIORITY = 1; // a packet each 5s

    private RtpHandler rtpHandler;
    private DtlsHandler dtlsHandler;
    private StunHandler stunHandler;
    private RtcpHandler rtcpHandler; // only used when rtcp-mux is enabled

    public RtpTransport(UdpManager udpManager, RtpRelay rtpRelay) {
        super();

        // Core elements
        this.udpManager = udpManager;

        // Channel attributes
        this.bound = false;
        this.secure = false;
        this.rtcpMux = false;
        this.dtmfSupported = false;

        // RTP elements
        this.rtpHandler = new RtpHandler(rtpRelay);
    }

    public void setRtpListener(RtpListener listener) {
        this.rtpListener = listener;
    }

    /**
     * Modifies the map between format and RTP payload number
     * 
     * @param rtpFormats the format map
     */
    public void setFormatMap(RTPFormats rtpFormats) {
        flush();
        this.dtmfSupported = rtpFormats.contains(AVProfile.telephoneEventsID);
        this.rtpHandler.setFormatMap(rtpFormats);
    }

    public RTPFormats getFormatMap() {
        return this.rtpHandler.getFormatMap();
    }

    private void onBinding(boolean useJitterBuffer) {
        // Set protocol handler priorities
        this.rtpHandler.setPipelinePriority(RTP_PRIORITY);
        if (this.rtcpMux) {
            this.rtcpHandler.setPipelinePriority(RTCP_PRIORITY);
        }
        if (this.secure) {
            this.stunHandler.setPipelinePriority(STUN_PRIORITY);
        }

        // Configure protocol handlers
        this.handlers.addHandler(this.rtpHandler);

        if (this.rtcpMux) {
            this.rtcpHandler.setChannel(this.dataChannel);
            this.handlers.addHandler(this.rtcpHandler);
        }

        if (this.secure) {
            this.dtlsHandler.setChannel(this.dataChannel);
            this.dtlsHandler.addListener(this);
            this.handlers.addHandler(this.stunHandler);

            // Start DTLS handshake
            this.dtlsHandler.handshake();
        }
    }

    public void bind(boolean isLocal) throws IOException {
        // Open this channel with UDP Manager on first available address
        this.selectionKey = udpManager.open(this);
        this.dataChannel = (DatagramChannel) this.selectionKey.channel();

        // activate media elements
        onBinding(!isLocal);

        // bind data channel
        this.udpManager.bind(this.dataChannel, PORT_ANY, isLocal);
        this.bound = true;
    }

    public void bind(DatagramChannel channel) throws IOException, SocketException {
        try {
            // Register the channel on UDP Manager
            this.selectionKey = udpManager.open(channel, this);
            this.dataChannel = channel;
        } catch (IOException e) {
            throw new SocketException(e.getMessage());
        }

        // activate media elements
        onBinding(true);

        // Only bind channel if necessary
        if (!channel.socket().isBound()) {
            this.udpManager.bind(channel, PORT_ANY);
        }
        this.bound = true;
    }

    public boolean isBound() {
        return this.bound;
    }

    public boolean isAvailable() {
        // The channel is available is is connected
        boolean available = this.dataChannel != null && this.dataChannel.isConnected();
        // In case of WebRTC calls the DTLS handshake must be completed
        if (this.secure) {
            available = available && this.dtlsHandler.isHandshakeComplete();
        }
        return available;
    }

    public void connect(SocketAddress address) {
        this.remotePeer = address;
        boolean connectImmediately = false;
        if (this.dataChannel != null) {
            if (this.dataChannel.isConnected()) {
                try {
                    disconnect();
                } catch (IOException e) {
                    logger.error(e);
                }
            }

            // connectImmediately = udpManager.connectImmediately((InetSocketAddress) address);
            // if (connectImmediately) {
            try {
                this.dataChannel.connect(address);
            } catch (IOException e) {
                logger.info("Can not connect to remote address , please check that you are not using local address - 127.0.0.X to connect to remote");
                logger.error(e.getMessage(), e);
            }
            // }
        }
    }

    public void connect(String address, int port) {
        connect(new InetSocketAddress(address, port));
    }

    public String getExternalAddress() {
        return this.udpManager.getExternalAddress();
    }

    public boolean hasExternalAddress() {
        return notEmpty(this.udpManager.getExternalAddress());
    }

    private boolean notEmpty(String text) {
        return text != null && !text.isEmpty();
    }

    public void enableSRTP(String hashFunction, String remotePeerFingerprint, IceAuthenticator authenticator) {
        this.secure = true;

        // setup the DTLS handler
        if (this.dtlsHandler == null) {
            this.dtlsHandler = new DtlsHandler();
        }
        this.dtlsHandler.setRemoteFingerprint(hashFunction, remotePeerFingerprint);

        // setup the STUN handler
        if (this.stunHandler == null) {
            this.stunHandler = new StunHandler(authenticator);
        }

        // Setup the RTP handler
        this.rtpHandler.enableSrtp(this.dtlsHandler);

        // Setup the RTCP handler. RTCP-MUX channels only!
        if (this.rtcpMux) {
            this.rtcpHandler.enableSRTCP(this.dtlsHandler);
        }
    }

    public void disableSRTP() {
        this.secure = false;

        // setup the DTLS handler
        if (this.dtlsHandler != null) {
            this.dtlsHandler.setRemoteFingerprint("", "");
            this.dtlsHandler.resetLocalFingerprint();
        }

        // setup the STUN handler
        if (this.stunHandler != null) {
            this.handlers.removeHandler(this.stunHandler);
        }

        // Setup the RTP handler
        this.rtpHandler.disableSrtp();

        // Setup the RTCP handler
        if (this.rtcpMux) {
            this.rtcpHandler.disableSRTCP();
        }
    }

    public void disableRtcp() {
        this.rtcpMux = false;
    }

    public void enableRtcp(RtpStatistics statistics) {
        this.rtcpHandler = new RtcpHandler(statistics);
        this.rtcpMux = true;
    }

    public Text getWebRtcLocalFingerprint() {
        if (this.dtlsHandler != null) {
            return this.dtlsHandler.getLocalFingerprint();
        }
        return new Text();
    }

    public void close() {
        if (rtcpMux) {
            this.rtcpHandler.leaveRtpSession();
            reset();
        } else {
            super.close();
            reset();
        }
        this.bound = false;
    }

    private void reset() {
        // RTP reset
        this.dtmfSupported = false;
        this.rtpHandler.reset();
        this.remotePeer = null;

        // RTCP reset
        if (this.rtcpMux) {
            this.rtcpHandler.reset();
            this.rtcpMux = false;
        }

        // DTLS reset
        if (this.secure) {
            this.dtlsHandler.reset();
            this.secure = false;
        }
    }

    @Override
    public void onDtlsHandshakeComplete() {
        logger.info("DTLS handshake completed for RTP candidate.");
        if (this.rtcpMux) {
            this.rtcpHandler.joinRtpSession();
        }
    }

    @Override
    public void onDtlsHandshakeFailed(Throwable e) {
        this.rtpListener.onRtpFailure(e);
    }

    public void send(RtpPacket packet, boolean dtmf) throws IOException {
        // Do not send data while DTLS handshake is ongoing. WebRTC calls only.
        if (this.secure && !this.dtlsHandler.isHandshakeComplete()) {
            throw new IOException("Ongoing DTLS handshake.");
        }

        if (dtmf && !this.dtmfSupported) {
            throw new IOException("DTMF format is not supported.");
        }

        if (this.dataChannel.isConnected()) {
            // Get the contents of the packet
            ByteBuffer buffer = packet.getBuffer();

            // If the channel is using DTLS then the payload must be secured.
            if (this.secure) {
                // Secure the packet
                byte[] rtpData = new byte[buffer.limit()];
                buffer.get(rtpData, 0, rtpData.length);
                byte[] srtpData = this.dtlsHandler.encodeRTP(rtpData, 0, rtpData.length);

                // SRTP handler returns null if an error occurs
                if (srtpData == null || srtpData.length == 0) {
                    logger.warn("An RTP packet was dropped because it could not be secured.");
                    return;
                }

                buffer.clear();
                buffer.put(srtpData);
                buffer.flip();
            }

            // send RTP packet to the network and update statistics for RTCP
            this.dataChannel.send(buffer, this.remotePeer);
        } else {
            throw new IOException("The RTP channel is not connected.");
        }
    }

}
