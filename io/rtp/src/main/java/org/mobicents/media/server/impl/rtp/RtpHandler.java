package org.mobicents.media.server.impl.rtp;

import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.network.ProtocolHandler;

/**
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpHandler implements ProtocolHandler {
	
	private static final Logger LOGGER = Logger.getLogger(RtpHandler.class);
	
	private RTPChannelListener channelListener;
	
	/**
	 *  The schedulable task for read operation
	 */
	private final RxTask rxTask;
	
	private volatile boolean isReading;
	
	private int count;	
	
	public RtpHandler() {
		this.rxTask = new RxTask();
		this.isReading = false;
		this.count = 0;
	}

	public void receive(DatagramChannel channel) {
		this.count++;
		rxTask.perform();
	}

	public void send(DatagramChannel channel) {
		// TODO Auto-generated method stub
		
	}

	public boolean isReadable() {
		return !this.isReading;
	}

	public boolean isWriteable() {
		return true;
	}

	public void setKey(SelectionKey key) {
		// TODO Auto-generated method stub
		
	}

	public void onClosed() {
		// TODO Auto-generated method stub
		
	}
	
	private void flush() {

	}
	
	/**
	 * Implements scheduled task for receiving RTP traffic.
	 * 
	 * @author Oifa Yulian
	 * @author Henrique Rosa
	 */
	private class RxTask {
		
		public void perform() {
			
		}
		
	}

}
