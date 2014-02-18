package org.mobicents.media.core.ice.network;

/**
 * 
 * @author Henrique Rosa
 *
 */
public interface Expirable {

	boolean isExpired();
	
	void expire();

}
