package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;

import org.apache.log4j.Logger;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.handler.MultiplexedChannel;
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
	
	// Channel attributes
	private int channelId;
	private final long ssrc;
	private boolean bound;
	private final RtpStatistics statistics;
	
	// Core elements
	private final ChannelsManager channelsManager;
	private final UdpManager udpManager;
	private final Scheduler scheduler;
	
	// Heartbeat
	private final HeartBeat heartBeat;
	
	// Remote peer
	private SocketAddress remotePeer;
	
	// Transmitter
	private final RtpTransmitter transmitter;
	
	// Receivers - Protocol handlers pipeline
	private RtpHandler rtpHandler;
	
	// Media components
	private AudioComponent audioComponent;
	private OOBComponent oobComponent;
	
	// Media formats
	protected final static AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
	protected final static AudioFormat DTMF_FORMAT = FormatFactory.createAudioFormat("telephone-event", 8000);
	static {
		DTMF_FORMAT.setOptions(new Text("0-15"));
	}
	
	private Formats formats;
	
	// WebRTC
	private boolean webRtc;
	
	// Listeners
	private RTPChannelListener channelListener;
	
	protected RtpChannel(final ChannelsManager channelsManager, final int channelId) {
		// Initialize MultiplexedChannel elements
		super();
		
		// Channel attributes
		this.channelId = channelId;
		this.ssrc = System.currentTimeMillis();
		this.statistics = new RtpStatistics();
		this.bound = false;
		
		// Core and network elements
		this.channelsManager = channelsManager;
		this.scheduler = channelsManager.getScheduler();
		this.udpManager = channelsManager.getUdpManager();
		
		// Media Components
		audioComponent = new AudioComponent(channelId);
		audioComponent.addInput(this.rtpHandler.getRtpInput().getAudioInput());
		audioComponent.addOutput(this.transmitter.getRtpOutput().getAudioOutput());

		oobComponent = new OOBComponent(channelId);
		oobComponent.addInput(this.rtpHandler.getDtmfInput().getOOBInput());
		oobComponent.addOutput(this.transmitter.getDtmfOutput().getOOBOutput());
		
		// Media formats
		this.formats = new Formats();
		this.formats.add(LINEAR_FORMAT);

		// Transmitter
		this.transmitter = new RtpTransmitter(scheduler, statistics, ssrc);
		
		// Receiver(s) - Protocol handlers pipeline
		this.rtpHandler = new RtpHandler(scheduler, channelsManager.getJitterBufferSize(), this.statistics);
		this.handlers.addHandler(this.rtpHandler);
		
		// WebRTC
		this.webRtc = false;
		
		// Heartbeat
		this.heartBeat =  new HeartBeat();
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
	
	/**
	 * Modifies the map between format and RTP payload number
	 * 
	 * @param rtpFormats
	 *            the format map
	 */
	public void setFormatMap(RTPFormats rtpFormats) {
		this.rtpHandler.setFormatMap(rtpFormats);
		this.transmitter.setFormatMap(rtpFormats);
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
	
	private class HeartBeat extends Task {

		public int getQueueNumber() {
			return Scheduler.HEARTBEAT_QUEUE;
		}

		@Override
		public long perform() {
			if (scheduler.getClock().getTime() - statistics.getLastPacketReceived() > udpManager.getRtpTimeout() * 1000000000L) {
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
