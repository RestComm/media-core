package org.mobicents.media.server.io.network.channel;


/**
 * A mock of the Packet Handler
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MediumPriorityPacketHandlerMock extends PacketHandlerMock {

	protected static final int PRIORITY = 3;
	protected static final String DATA = "medium";
	
	
	public MediumPriorityPacketHandlerMock() {
		super(PRIORITY, DATA);
	}

}
