package org.mobicents.media.server.io.network.channel;


/**
 * A mock of the Packet Handler
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class LowPriorityPacketHandlerMock extends PacketHandlerMock {

	protected static final int PRIORITY = 1;
	protected static final String DATA = "low";
	
	
	public LowPriorityPacketHandlerMock() {
		super(PRIORITY, DATA);
	}

}
