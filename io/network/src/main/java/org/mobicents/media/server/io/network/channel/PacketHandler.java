package org.mobicents.media.server.io.network.channel;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface PacketHandler {

	/**
	 * Checks whether the handler can process the incoming packet or not.
	 * 
	 * @param packet
	 *            The packet to be processed.
	 * @return <code>true</code>, if the packet can be handled.
	 *         <code>false</code>, otherwise.
	 */
	boolean canHandle(byte[] packet);

	boolean canHandle(byte[] packet, int dataLength, int offset);

	/**
	 * Processes the packet and provides a suitable answer.
	 * 
	 * @param packet
	 *            The packet to be processed.
	 * @return The answer to be sent to the remote peer as response to the
	 *         incoming packet.
	 * @throws PacketHandlerException
	 *             When the handler cannot process the packet.
	 */
	byte[] handle(byte[] packet) throws PacketHandlerException;

	/**
	 * Processes the packet and provides a suitable answer.
	 * 
	 * @param packet
	 *            The packet to be processed.
	 * @param dataLength
	 *            The length of the data to be read.
	 * @param offset
	 *            The initial position to start reading data from.
	 * @return The answer to be sent to the remote peer as response to the
	 *         incoming packet.
	 * @throws PacketHandlerException
	 *             When the handler cannot process the packet.
	 */
	byte[] handle(byte[] packet, int dataLength, int offset) throws PacketHandlerException;

}
