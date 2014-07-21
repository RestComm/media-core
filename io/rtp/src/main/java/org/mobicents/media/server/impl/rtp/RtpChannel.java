package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfInput;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.handler.MultiplexedChannel;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;
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
	
	// Listeners
	private RTPChannelListener channelListener;

	// Channel attributes
	private int channelId;
	private final long ssrc;
	private int count;
	private boolean bound;
	private final RtpStatistics statistics;
	
	// Core elements
	private final ChannelsManager channelsManager;
	private final UdpManager udpManager;
	private final Scheduler scheduler;
	private final Clock clock;
	
	// Remote peer
	private SocketAddress remotePeer;
	
	// RTP clock
	private RtpClock rtpClock;
	private RtpClock oobClock;
	
	// Receiver and transmitter
	private RTPInput input;
	//private RTPOutput output;

	// RTP Reading Task
	private JitterBuffer jitterBuffer;
	private int jitterBufferSize;
	private volatile long rxCount;
	private long lastPacketReceived;

	private volatile boolean isReading;
	
	// DTMF receiver and transmitter
	private DtmfInput dtmfInput;
	// private DtmfOutput dtmfOutput;
	
	private boolean sendDtmf;
	
	// Transmitter
	private final RtpTransmitter transmitter;
	
	// Media components
	private AudioComponent audioComponent;
	private OOBComponent oobComponent;
	
	// Media flags
	private Boolean shouldReceive;
	private Boolean shouldLoop;
	
	// Media stream format
	private final RTPFormats rtpFormats;
	protected final static AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
	protected final static AudioFormat DTMF_FORMAT = FormatFactory.createAudioFormat("telephone-event", 8000);
	static {
		DTMF_FORMAT.setOptions(new Text("0-15"));
	}
	
	// WebRTC
	private boolean webRtc;

	// Protocol handlers
	private DtlsHandler dtlsHandler;
	private RtpHandler rtpHandler;
	// TODO declare stun handler
	
	
	protected RtpChannel(final ChannelsManager channelsManager, final int channelId) {
		// Initialize MultiplexedChannel elements
		super();
		
		// Channel attributes
		this.channelId = channelId;
		this.ssrc = System.currentTimeMillis();
		this.bound = false;
		this.count = 0;
		this.statistics = new RtpStatistics();
		
		// Core and network elements
		this.channelsManager = channelsManager;
		this.scheduler = channelsManager.getScheduler();
		this.udpManager = channelsManager.getUdpManager();

		// Create clock with RTP units
		rtpClock = new RtpClock(channelsManager.getClock());
		oobClock = new RtpClock(channelsManager.getClock());
		
		// Receiver and Transmitter
		this.input = new RTPInput(this.scheduler, this.jitterBuffer);
		//this.output = new RTPOutput(scheduler, this.transmitter);
		this.jitterBuffer.setListener(this.input);
		this.lastPacketReceived = 0;
		
		// RTP formats
		this.rtpFormats = new RTPFormats();

		// Media Flags
		this.shouldLoop = false;
		this.shouldReceive = false;
		
		// DTMF
		this.dtmfInput = new DtmfInput(scheduler, oobClock);
		//this.dtmfOutput = new DtmfOutput(scheduler, this.transmitter);
		this.sendDtmf = false;
		
		// Media Components
		audioComponent = new AudioComponent(channelId);
		audioComponent.addInput(input.getAudioInput());
//		audioComponent.addOutput(output.getAudioOutput());
		audioComponent.addOutput(this.transmitter.getRtpOutput().getAudioOutput());

		oobComponent = new OOBComponent(channelId);
		oobComponent.addInput(dtmfInput.getOOBInput());
//		oobComponent.addOutput(dtmfOutput.getOOBOutput());
		oobComponent.addOutput(this.transmitter.getDtmfOutput().getOOBOutput());
		
		// Protocol handlers pipeline
		this.rtpHandler = new RtpHandler(scheduler.getClock(), this.statistics);
		this.handlers.addHandler(this.rtpHandler);
		// TODO add STUN handler
		// TODO add DTLS handler
		
		// WebRTC
		this.webRtc = false;
	}
	
	private DatagramChannel getChannel() {
		return (DatagramChannel) this.selectionKey.channel(); 
	}
	
	public RtpTransmitter getTransmitter() {
		return this.transmitter;
	}
	
	public AudioComponent getAudioComponent() {
		return this.getAudioComponent();
	}
	
	public OOBComponent getOobComponent() {
		return this.getOobComponent();
	}
	
	public Processor getInputDsp() {
		return input.getDsp();
	}

	public void setInputDsp(Processor dsp) {
		this.input.setDsp(dsp);
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
	
	/**
	 * Modifies the map between format and RTP payload number
	 * 
	 * @param rtpFormats
	 *            the format map
	 */
	public void setFormatMap(RTPFormats rtpFormats) {
		this.sendDtmf = rtpFormats.contains(AVProfile.telephoneEventsID);
		this.rtpHandler.setFormatMap(rtpFormats);
	}
	
	public void updateMode(ConnectionMode connectionMode) {
		// TODO update channel mode
	}
	
	public void bind(boolean isLocal) throws IOException, SocketException {
		// TODO bind to channel chosen by UDP Manager 
	}

	public void bind(SocketAddress address) throws IOException, SocketException {
		// TODO binds to address and registers it on UDP Manager 
	}
	
	public boolean isBound() {
		return this.bound;
	}
	
	public boolean isAvailable() {
		// TODO Add criteria to decide availability of this channel
		return false;
	}
	
	public int getLocalPort() {
		// TODO get local port from bound channel
		return 0;
	}
	
	public void setRemotePeer(SocketAddress address) {
		// TODO bind channel to remote peer using UDP Manager
	}
	
	public void close() {
		// TODO close channel and clean resources
	}

}
