package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.mobicents.media.io.ice.IceAuthenticator;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.impl.stun.StunHandler;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.channel.MultiplexedChannel;
import org.mobicents.media.server.scheduler.Scheduler;
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
public class RtpChannel extends MultiplexedChannel {
	
	private static final Logger LOGGER = Logger.getLogger(RtpChannel.class);
	
	/** Tells UDP manager to choose port to bind this channel to */
	private final static int PORT_ANY = -1;
	
	// Channel attributes
	private boolean bound;
	private final RtpStatistics statistics;
	
	// Core elements
	private final UdpManager udpManager;
	private final Scheduler scheduler;
	private final RtpClock rtpClock;
	
	// Heart beat
	private final HeartBeat heartBeat;
	
	// Remote peer
	private SocketAddress remotePeer;
	
	// Transmitter
	private final RtpTransmitter transmitter;
	
	// Receivers - Protocol handlers pipeline
	private RtpHandler rtpHandler;
	private DtlsHandler dtlsHandler;
	private StunHandler stunHandler;
	
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
	private boolean srtp;
	
	// Listeners
	private RTPChannelListener channelListener;
	
	protected RtpChannel(final ChannelsManager channelsManager, final int channelId) {
		// Initialize MultiplexedChannel elements
		super();
		
		// Core and network elements
		this.scheduler = channelsManager.getScheduler();
		this.udpManager = channelsManager.getUdpManager();
		this.rtpClock = new RtpClock(scheduler.getClock());
		// TODO RTP Transmitter/Receiver should share the same RTP clock!!!!
		
		// Channel attributes
		this.statistics = new RtpStatistics(this.rtpClock);
		this.bound = false;
		
		// Transmitter
		this.transmitter = new RtpTransmitter(scheduler, statistics);
		
		// Receiver(s) - Protocol handlers pipeline
		this.rtpHandler = new RtpHandler(scheduler, channelsManager.getJitterBufferSize(), this.statistics);
		this.handlers.addHandler(this.rtpHandler);
		
		// Media Components
		audioComponent = new AudioComponent(channelId);
		audioComponent.addInput(this.rtpHandler.getRtpInput().getAudioInput());
		audioComponent.addOutput(this.transmitter.getRtpOutput().getAudioOutput());
		
		oobComponent = new OOBComponent(channelId);
		oobComponent.addInput(this.rtpHandler.getDtmfInput().getOOBInput());
		oobComponent.addOutput(this.transmitter.getDtmfOutput().getOOBOutput());

		// WebRTC
		this.srtp = false;
		
		// Heartbeat
		this.heartBeat =  new HeartBeat();
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
	
	public void setRtpChannelListener(RTPChannelListener listener) {
		this.channelListener = listener;
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
	 * @param rtpFormats
	 *            the format map
	 */
	public void setFormatMap(RTPFormats rtpFormats) {
		flush();
		this.rtpHandler.setFormatMap(rtpFormats);
		this.transmitter.setFormatMap(rtpFormats);
	}
	
	/**
	 * Sets the connection mode of the channel.<br>
	 * Possible modes: send_only, recv_only, inactive, send_recv, conference, network_loopback.
	 * 
	 * @param connectionMode
	 *            the new connection mode adopted by the channel
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
	
	public void bind(boolean isLocal) throws IOException, SocketException {
		try {
			// Open this channel with UDP Manager on first available address
			this.channel = (DatagramChannel) udpManager.open(this).channel();
		} catch (IOException e) {
			throw new SocketException(e.getMessage());
		}

		// bind data channel
		this.rtpHandler.useJitterBuffer(!isLocal);
		this.udpManager.bind(this.channel, PORT_ANY, isLocal);
		this.bound = true;
		this.transmitter.setChannel(this.channel);
	}

	public void bind(DatagramChannel channel) throws IOException, SocketException {
		try {
			// Register the channel on UDP Manager
			this.channel = (DatagramChannel) udpManager.open(channel, this).channel();
		} catch (IOException e) {
			throw new SocketException(e.getMessage());
		}

		// Only bind channel if necessary
		if(!channel.socket().isBound()) {
			this.udpManager.bind(channel, PORT_ANY);
		}
		this.rtpHandler.useJitterBuffer(true);
		this.transmitter.setChannel(channel);
		if (this.srtp) {
			this.dtlsHandler.setChannel(this.channel);
			this.stunHandler.setChannel(this.channel);
			this.handlers.addHandler(stunHandler);
		}
		this.bound = true;
	}
	
	public boolean isBound() {
		return this.bound;
	}
	
	public boolean isAvailable() {
		// The channel is available is is connected
		boolean available = this.channel != null && this.channel.isConnected();
		// In case of WebRTC calls the DTLS handshake must be completed
		if(this.srtp) {
			available = available && this.dtlsHandler.isHandshakeComplete();
		}
		return available;
	}
	
	public int getLocalPort() {
		return this.channel != null ? this.channel.socket().getLocalPort() : 0;
	}
	
	public void setRemotePeer(SocketAddress address) {
		this.remotePeer = address;
		boolean connectImmediately = false;
		if (this.channel != null) {
			if (this.channel.isConnected())
				try {
					disconnect();
				} catch (IOException e) {
					LOGGER.error(e);
				}

			connectImmediately = udpManager.connectImmediately((InetSocketAddress) address);
			if (connectImmediately) {
				try {
					this.channel.connect(address);
				} catch (IOException e) {
					LOGGER.info("Can not connect to remote address , please check that you are not using local address - 127.0.0.X to connect to remote");
					LOGGER.error(e.getMessage(), e);
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
	
	public void enableSRTP(Text remotePeerFingerprint, IceAuthenticator authenticator) {
		this.srtp = true;
		
		// setup the DTLS handler
		if (this.dtlsHandler == null) {
			this.dtlsHandler = new DtlsHandler();
		}
		this.dtlsHandler.setRemoteFingerprint(remotePeerFingerprint);
		
		// setup the STUN handler
		if (this.stunHandler == null) {
			this.stunHandler = new StunHandler(authenticator);
		}
		
		// setup the RTP transmitter and receiver
		this.transmitter.enableSrtp(this.dtlsHandler);
		this.rtpHandler.enableSrtp(this.dtlsHandler);
	}
	
	public Text getWebRtcLocalFingerprint() {
		if(this.dtlsHandler != null) {
			return this.dtlsHandler.getLocalFingerprint();
		}
		return new Text();
	}
	
	@Override
	public void receive() throws IOException {
		// Make sure the DTLS handshake is complete for WebRTC calls
		if(this.srtp && !this.dtlsHandler.isHandshakeComplete()) {
			// TODO Need to implement own DTLS handler and drop bouncy castle implementation!
			if(!this.dtlsHandler.isHandshaking()) {
				this.dtlsHandler.handshake();
			}
		} else {
			// Receive traffic normally through the multiplexed channel
			super.receive();
		}
	}
	
	public void close() {
		super.close();
		reset();
	}
	
	private void reset() {
		this.statistics.reset();
		this.rtpHandler.reset();
		this.transmitter.reset();
		heartBeat.cancel();
	}
	
	private class HeartBeat extends Task {

		public int getQueueNumber() {
			return Scheduler.HEARTBEAT_QUEUE;
		}

		@Override
		public long perform() {
			if (scheduler.getClock().getTime() - statistics.getLastHeartbeat() > udpManager.getRtpTimeout() * 1000000000L) {
				if (channelListener != null) {
					channelListener.onRtpFailure();
				}
			} else {
				scheduler.submitHeatbeat(this);
			}
			return 0;
		}
	}

}
