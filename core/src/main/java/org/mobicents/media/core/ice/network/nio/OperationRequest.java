package org.mobicents.media.core.ice.network.nio;

import java.nio.channels.DatagramChannel;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class OperationRequest {

	public static final int REGISTER = 1;
	public static final int CHANGE_OPS = 2;

	public final DatagramChannel channel;
	public final int requestType;
	public final int operations;

	public OperationRequest(DatagramChannel channel, int type, int operations) {
		this.channel = channel;
		this.requestType = type;
		this.operations = operations;
	}

	public int getRequestType() {
		return requestType;
	}

	public int getOperations() {
		return operations;
	}

	public DatagramChannel getChannel() {
		return channel;
	}

}
