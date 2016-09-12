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
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.mobicents.media.io.ice.IceAuthenticator;
import org.mobicents.media.io.ice.IceComponent;
import org.mobicents.media.io.ice.IceHandler;
import org.mobicents.media.io.ice.events.IceEventListener;
import org.mobicents.media.io.ice.events.SelectedCandidatesEvent;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtcp.RtcpHandler;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.impl.srtp.DtlsListener;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.channel.MultiplexedChannel;
import org.mobicents.media.server.io.sdp.format.RTPFormats;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.utils.Text;

/**
 * 
 * @author Yulian Oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpChannel extends MultiplexedChannel implements DtlsListener, IceEventListener {

    private static final Logger logger = Logger.getLogger(RtpChannel.class);

    /** Tells UDP manager to choose port to bind this channel to */
    private final static int PORT_ANY = -1;

    // Channel attributes
    private final int channelId;
    private boolean bound;
    private RtpStatistics statistics;

    // Core elements
    private final UdpManager udpManager;
    private final PriorityQueueScheduler scheduler;
    private final RtpClock clock;
    private final RtpClock oobClock;
    private final int jitterBufferSize;

    // Heart beat
    private final HeartBeat heartBeat;

    // Remote peer
    private SocketAddress remotePeer;

    // Transmitter
    private RtpTransmitter transmitter;

    // Protocol handlers pipeline
    private static final int RTP_PRIORITY = 4; // a packet each 20ms
    private static final int STUN_PRIORITY = 3; // a packet each 400ms
    private static final int RTCP_PRIORITY = 2; // a packet each 5s
    private static final int DTLS_PRIORITY = 1; // only for handshake

    private RtpHandler rtpHandler;
    private DtlsHandler dtlsHandler;
    private IceHandler stunHandler;
    private RtcpHandler rtcpHandler; // only used when rtcp-mux is enabled

    // Media components
    private AudioComponent audioComponent;
    private OOBComponent oobComponent;

    // Media formats
    protected final static AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    protected final static AudioFormat DTMF_FORMAT = FormatFactory.createAudioFormat("telephone-event", 8000);
    static {
        DTMF_FORMAT.setOptions(new Text("0-15"));
    }

    // WebRTC
    private boolean ice;
    private boolean secure;
    private boolean rtcpMux;

    // Listeners
    private RtpListener rtpListener;

    protected RtpChannel(int channelId, int jitterBufferSize, RtpStatistics statistics, RtpClock clock, RtpClock oobClock,
            PriorityQueueScheduler scheduler, UdpManager udpManager) {
        // Initialize MultiplexedChannel elements
        super();

        // Core and network elements
        this.scheduler = scheduler;
        this.udpManager = udpManager;
        this.clock = clock;
        this.oobClock = oobClock;

        // Channel attributes
        this.channelId = channelId;
        this.jitterBufferSize = jitterBufferSize;
        this.statistics = statistics;
        this.bound = false;

        // Protocol Handlers
        this.transmitter = new RtpTransmitter(scheduler, clock, statistics);
        this.rtpHandler = new RtpHandler(scheduler, clock, oobClock, jitterBufferSize, statistics);
        this.rtpHandler.setPipelinePriority(RTP_PRIORITY);
        this.rtcpHandler = new RtcpHandler(this.udpManager.getScheduler(), statistics);
        this.rtpHandler.setPipelinePriority(RTCP_PRIORITY);
        this.dtlsHandler = new DtlsHandler();
        this.rtpHandler.setPipelinePriority(DTLS_PRIORITY);
        this.stunHandler = new IceHandler(IceComponent.RTP_ID, this);
        this.stunHandler.setPipelinePriority(STUN_PRIORITY);

        // Media Components
        this.audioComponent = new AudioComponent(channelId);
        this.audioComponent.addInput(this.rtpHandler.getRtpInput().getAudioInput());
        this.audioComponent.addOutput(this.transmitter.getRtpOutput().getAudioOutput());
        this.oobComponent = new OOBComponent(channelId);
        this.oobComponent.addInput(this.rtpHandler.getDtmfInput().getOOBInput());
        this.oobComponent.addOutput(this.transmitter.getDtmfOutput().getOOBOutput());

        // WebRTC
        this.secure = false;
        this.rtcpMux = false;

        // Heartbeat
        this.heartBeat = new HeartBeat();
    }

    public RtpTransmitter getTransmitter() {
        return this.transmitter;
    }

    public AudioComponent getAudioComponent() {
        return this.audioComponent;
    }

    public OOBComponent getOobComponent() {
        return this.oobComponent;
    }

    public Processor getInputDsp() {
        return this.rtpHandler.getRtpInput().getDsp();
    }

    public void setInputDsp(Processor dsp) {
        this.rtpHandler.getRtpInput().setDsp(dsp);
    }

    public Processor getOutputDsp() {
        return this.transmitter.getRtpOutput().getDsp();
    }

    public void setOutputDsp(Processor dsp) {
        this.transmitter.getRtpOutput().setDsp(dsp);
    }

    public void setOutputFormats(Formats fmts) throws FormatNotSupportedException {
        this.transmitter.getRtpOutput().setFormats(fmts);
    }

    public void setRtpListener(RtpListener listener) {
        this.rtpListener = listener;
    }

    public long getPacketsReceived() {
        return this.statistics.getRtpPacketsReceived();
    }

    public long getPacketsTransmitted() {
        return this.statistics.getRtpPacketsSent();
    }

    /**
     * Modifies the map between format and RTP payload number
     * 
     * @param rtpFormats the format map
     */
    public void setFormatMap(RTPFormats rtpFormats) {
        flush();
        this.rtpHandler.setFormatMap(rtpFormats);
        this.transmitter.setFormatMap(rtpFormats);
    }

    public RTPFormats getFormatMap() {
        return this.rtpHandler.getFormatMap();
    }

    /**
     * Sets the connection mode of the channel.<br>
     * Possible modes: send_only, recv_only, inactive, send_recv, conference, network_loopback.
     * 
     * @param connectionMode the new connection mode adopted by the channel
     */
    public void updateMode(ConnectionMode connectionMode) {
        switch (connectionMode) {
            case SEND_ONLY:
                this.rtpHandler.setReceivable(false);
                this.rtpHandler.setLoopable(false);
                audioComponent.updateMode(false, true);
                oobComponent.updateMode(false, true);
                this.rtpHandler.deactivate();
                this.transmitter.activate();
                break;
            case RECV_ONLY:
                this.rtpHandler.setReceivable(true);
                this.rtpHandler.setLoopable(false);
                audioComponent.updateMode(true, false);
                oobComponent.updateMode(true, false);
                this.rtpHandler.activate();
                this.transmitter.deactivate();
                break;
            case INACTIVE:
                this.rtpHandler.setReceivable(false);
                this.rtpHandler.setLoopable(false);
                audioComponent.updateMode(false, false);
                oobComponent.updateMode(false, false);
                this.rtpHandler.deactivate();
                this.transmitter.deactivate();
                break;
            case SEND_RECV:
            case CONFERENCE:
                this.rtpHandler.setReceivable(true);
                this.rtpHandler.setLoopable(false);
                audioComponent.updateMode(true, true);
                oobComponent.updateMode(true, true);
                this.rtpHandler.activate();
                this.transmitter.activate();
                break;
            case NETWORK_LOOPBACK:
                this.rtpHandler.setReceivable(false);
                this.rtpHandler.setLoopable(true);
                audioComponent.updateMode(false, false);
                oobComponent.updateMode(false, false);
                this.rtpHandler.deactivate();
                this.transmitter.deactivate();
                break;
            default:
                break;
        }

        boolean connectImmediately = false;
        if (this.remotePeer != null) {
            connectImmediately = udpManager.connectImmediately((InetSocketAddress) this.remotePeer);
        }

        if (udpManager.getRtpTimeout() > 0 && this.remotePeer != null && !connectImmediately) {
            if (this.rtpHandler.isReceivable()) {
                this.statistics.setLastHeartbeat(scheduler.getClock().getTime());
                scheduler.submitHeatbeat(heartBeat);
            } else {
                heartBeat.cancel();
            }
        }
    }

    private void onBinding(boolean useJitterBuffer) {
//        // Set protocol handler priorities
//        this.rtpHandler.setPipelinePriority(RTP_PRIORITY);
//        if (this.rtcpMux) {
//            this.rtcpHandler.setPipelinePriority(RTCP_PRIORITY);
//        }
//        if (this.secure) {
//            this.stunHandler.setPipelinePriority(STUN_PRIORITY);
//        }
//
//        // Configure protocol handlers
        this.transmitter.setChannel(this.dataChannel);
        this.rtpHandler.useJitterBuffer(useJitterBuffer);
        this.handlers.addHandler(this.rtpHandler);

        if (this.rtcpMux) {
            this.rtcpHandler.setChannel(this.dataChannel);
            this.handlers.addHandler(this.rtcpHandler);
        }
//
//        if (this.secure) {
//            this.dtlsHandler.setPipelinePriority(DTLS_PRIORITY);
//            this.dtlsHandler.setChannel(this.dataChannel);
//            this.dtlsHandler.addListener(this);
//            this.handlers.addHandler(this.stunHandler);
//
//            // Start DTLS handshake
//            this.dtlsHandler.handshake();
//        }
    }

    public void bind(boolean isLocal, boolean rtcpMux) throws IOException {
        // Open this channel with UDP Manager on first available address
        this.selectionKey = udpManager.open(this);
        this.dataChannel = (DatagramChannel) this.selectionKey.channel();

        // bind data channel
        this.udpManager.bind(this.dataChannel, PORT_ANY, isLocal);

        this.rtcpMux = rtcpMux;
        this.bound = true;

        // activate media elements
        onBinding(!isLocal);
    }

    @Deprecated
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
    
    public boolean isConnected() {
        return this.dataChannel != null && this.dataChannel.isConnected();
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

    public void setRemotePeer(SocketAddress address) {
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

            connectImmediately = udpManager.connectImmediately((InetSocketAddress) address);
            if (connectImmediately) {
                try {
                    this.dataChannel.connect(address);
                } catch (IOException e) {
                    logger.info("Can not connect to remote address , please check that you are not using local address - 127.0.0.X to connect to remote");
                    logger.error(e.getMessage(), e);
                }
            }
        }

        if (udpManager.getRtpTimeout() > 0 && !connectImmediately) {
            if (this.rtpHandler.isReceivable()) {
                this.statistics.setLastHeartbeat(scheduler.getClock().getTime());
                scheduler.submitHeatbeat(heartBeat);
            } else {
                heartBeat.cancel();
            }
        }
    }

    public String getExternalAddress() {
        return this.udpManager.getExternalAddress();
    }

    public String getWebRTCAddress() {
        return this.udpManager.getWebRTCAddress();
    }

    public boolean hasExternalAddress() {
        return notEmpty(this.udpManager.getExternalAddress());
    }

    private boolean notEmpty(String text) {
        return text != null && !text.isEmpty();
    }
    
    public void enableIce(IceAuthenticator authenticator) {
        if(!this.ice) {
            this.ice = true;
            this.stunHandler.setAuthenticator(authenticator);
            this.handlers.addHandler(this.stunHandler);
        }
    }
    
    public void disableIce() {
        if(this.ice) {
            this.ice = false;
            this.handlers.removeHandler(this.stunHandler);
        }
    }

    public void enableSRTP(String hashFunction, String remotePeerFingerprint) {
        if (!this.secure) {
            this.secure = true;
            this.dtlsHandler.setRemoteFingerprint(hashFunction, remotePeerFingerprint);
            this.dtlsHandler.addListener(this);

            // Setup the RTP handler
            this.transmitter.enableSrtp(this.dtlsHandler);
            this.rtpHandler.enableSrtp(this.dtlsHandler);

            // Setup the RTCP handler. RTCP-MUX channels only!
            if (this.rtcpMux) {
                this.rtcpHandler.enableSRTCP(this.dtlsHandler);
            }

            // Add handler to pipeline to handle incoming DTLS packets
            this.dtlsHandler.setChannel(this.dataChannel);
            this.handlers.addHandler(this.dtlsHandler);
        }
    }

    public void enableSRTP() {
        if (!this.secure) {
            this.secure = true;

            // Setup the RTP handler
            this.transmitter.enableSrtp(this.dtlsHandler);
            this.rtpHandler.enableSrtp(this.dtlsHandler);

            // Setup the RTCP handler. RTCP-MUX channels only!
            if (this.rtcpMux) {
                this.rtcpHandler.enableSRTCP(this.dtlsHandler);
            }
            
            // Add handler to pipeline to handle incoming DTLS packets
            this.dtlsHandler.setChannel(this.dataChannel);
            this.dtlsHandler.addListener(this);
            this.handlers.addHandler(this.dtlsHandler);
        }
    }

    public void setRemoteFingerprint(String hashFunction, String fingerprint) {
        this.dtlsHandler.setRemoteFingerprint(hashFunction, fingerprint);
    }

    public void disableSRTP() {
        if (this.secure) {
            this.secure = false;

            // setup the DTLS handler
            this.handlers.removeHandler(dtlsHandler);
            this.dtlsHandler.setRemoteFingerprint("", "");
            this.dtlsHandler.resetLocalFingerprint();

            // Setup the RTP handler
            this.transmitter.disableSrtp();
            this.rtpHandler.disableSrtp();

            // Setup the RTCP handler
            if (this.rtcpMux) {
                this.rtcpHandler.disableSRTCP();
            }
        }
    }

    public Text getWebRtcLocalFingerprint() {
        if (this.secure) {
            return new Text(this.dtlsHandler.getLocalFingerprint());
        }
        return new Text();
    }

    public void close() {
        if (rtcpMux) {
            this.rtcpHandler.leaveRtpSession();
        }

        super.close();
        reset();
        this.bound = false;
    }

    private void reset() {
        // Heartbeat reset
        heartBeat.cancel();

        // RTP reset
        this.handlers.removeHandler(this.rtpHandler);
        this.rtpHandler.reset();
        this.transmitter.reset();

        // RTCP reset
        if (this.rtcpMux) {
            this.handlers.removeHandler(this.rtcpHandler);
            this.rtcpHandler.reset();
            this.rtcpMux = false;
        }
        
        if(this.ice) {
            disableIce();
            this.stunHandler.reset();
        }

        // DTLS reset
        if (this.secure) {
            disableSRTP();
            this.dtlsHandler.reset();
        }
    }

    public void onDtlsHandshakeComplete() {
        logger.info("DTLS handshake completed for RTP candidate.");
        if (this.rtcpMux) {
            this.rtcpHandler.joinRtpSession();
        }
    }

    public void onDtlsHandshakeFailed(Throwable e) {
        if (rtpListener != null) {
            this.rtpListener.onRtpFailure(e);
        }
    }

    private class HeartBeat extends Task {

        public int getQueueNumber() {
            return PriorityQueueScheduler.HEARTBEAT_QUEUE;
        }

        @Override
        public long perform() {
            long elapsedTime = scheduler.getClock().getTime() - statistics.getLastHeartbeat();
            if (elapsedTime > udpManager.getRtpTimeout() * 1000000000L) {
                if (rtpListener != null) {
                    rtpListener.onRtpFailure("RTP timeout! Elapsed time since last heartbeat: " + elapsedTime);
                }
            } else {
                scheduler.submitHeatbeat(this);
            }
            return 0;
        }
    }
    
    @Override
    public void onSelectedCandidates(SelectedCandidatesEvent event) {
            // Connect channel to start receiving traffic from remote peer
//            this.connect(event.getRemotePeer());

            if (this.secure) {
                // Start DTLS handshake
                this.dtlsHandler.handshake();
            }
    }

}
