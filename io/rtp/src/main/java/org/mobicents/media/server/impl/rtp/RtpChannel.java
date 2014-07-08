package org.mobicents.media.server.impl.rtp;

import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.handler.DataChannel;
import org.mobicents.media.server.io.network.handler.ProtocolHandlerPipeline;
import org.mobicents.media.server.scheduler.Scheduler;
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
public class RtpChannel implements DataChannel {
	
	private static final Logger LOGGER = Logger.getLogger(RtpChannel.class);
	
	// Supported audio formats
	private final static AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
	private final static AudioFormat DTMF_FORMAT = FormatFactory.createAudioFormat("telephone-event", 8000);
	static {
		DTMF_FORMAT.setOptions(new Text("0-15"));
	}
	
	// Core and network elements
	private final ChannelsManager channelsManager;
	private final UdpManager udpManager;
	private final Scheduler scheduler;
	
	// Listeners
	private RTPChannelListener rtpChannelListener;
	
	// Audio elements
	private final Formats audioFormats;
	
	// Protocol handlers
	private final ProtocolHandlerPipeline handlers;
	private DtlsHandler dtlsHandler;
	private RtpHandler rtpHandler;
	// TODO declare stun handler
	
	// WebRTC
	private boolean webRtc;
	
	protected RtpChannel(final ChannelsManager channelsManager, final int channelId) {
		// Core
		this.channelsManager = channelsManager;
		this.scheduler = channelsManager.getScheduler();
		this.udpManager = channelsManager.getUdpManager();
		
		// Audio
		this.audioFormats = new Formats();
		this.audioFormats.add(LINEAR_FORMAT);

		// Protocol handlers pipeline
		this.handlers = new ProtocolHandlerPipeline();
		// TODO add STUN handler
		// TODO add RTP handler
		// TODO add DTLS handler
		
		// WebRTC
		this.webRtc = false;
	}

}
