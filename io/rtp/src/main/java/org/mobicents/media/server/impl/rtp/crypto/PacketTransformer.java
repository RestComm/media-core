/**
 * 
 * Code derived and adapted from the Jitsi client side SRTP framework.
 * 
 * Distributed under LGPL license.
 * See terms of license at gnu.org.
 */
package org.mobicents.media.server.impl.rtp.crypto;

import org.mobicents.media.server.impl.rtp.RtpPacket;

/**
 * Encapsulate the concept of packet transformation. Given a packet,
 * <tt>PacketTransformer</tt> can either transform it or reverse the
 * transformation.
 * 
 * @author Bing SU (nova.su@gmail.com)
 * @author ivelin.ivanov@telestax.com
 */
public interface PacketTransformer {
	/**
	 * Transforms a specific packet.
	 * 
	 * @param pkt
	 *            the packet to be transformed
	 * @return Whether the packet was successfully transformed
	 */
	public boolean transform(RtpPacket pkt);

	/**
	 * Reverse-transforms a specific packet (i.e. transforms a transformed
	 * packet back).
	 * 
	 * @param pkt
	 *            the transformed packet to be restored
	 * @return Whether the packet was successfully restored
	 */
	public boolean reverseTransform(RtpPacket pkt);

	/**
	 * Close the transformer and underlying transform engine.
	 * 
	 * The close functions closes all stored crypto contexts. This deletes key
	 * data and forces a cleanup of the crypto contexts.
	 */
	public void close();
}
