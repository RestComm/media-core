package org.mobicents.media.io.ice.network;

/**
 * 
 * @author Henrique Rosa
 *
 */
public interface Expirable {

	boolean isExpired();
	
	void expire();

}
