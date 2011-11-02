package org.mobicents.media.server.impl.rtp;

import java.nio.ByteBuffer;

/**
 * 
 * @author amit bhayani
 *
 */
public abstract class PacketHandler {

	public abstract boolean isClosed();

	public abstract void close();

	public abstract void receive(ByteBuffer readBuffer);
}
