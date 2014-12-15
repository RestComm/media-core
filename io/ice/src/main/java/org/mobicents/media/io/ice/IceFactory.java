package org.mobicents.media.io.ice;

import org.mobicents.media.io.ice.lite.IceLiteAgent;

/**
 * A factory to produce ICE agents.<br>
 * The user can decide whether to create agents for full or lite ICE
 * implementations.
 * 
 * @author Henrique Rosa
 * 
 */
public class IceFactory {

	/**
	 * Produces a new agent that implements ICE-lite.<br>
	 * As such, this agent won't keep states nor attempt connectivity checks.<br>
	 * It will only listen to incoming connectivity checks.
	 * 
	 * @return The ice-lite agent
	 * @see <a href="http://tools.ietf.org/html/rfc5245#section-4.2">RFC5245</a>
	 */
	public static IceLiteAgent createLiteAgent() {
		return new IceLiteAgent();
	}
}
