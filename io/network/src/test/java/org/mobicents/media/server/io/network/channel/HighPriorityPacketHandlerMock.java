package org.mobicents.media.server.io.network.channel;


/**
 * A mock of the Packet Handler
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class HighPriorityPacketHandlerMock extends PacketHandlerMock {

	protected static final int PRIORITY = 5;
	protected static final String DATA = "high";
	
	
	public HighPriorityPacketHandlerMock() {
		super(PRIORITY, DATA);
	}

}
