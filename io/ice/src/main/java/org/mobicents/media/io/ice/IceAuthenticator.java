package org.mobicents.media.io.ice;

/**
 * Authenticator that verifies the integrity of incoming messages.
 * 
 * @author Henrique Rosa
 * 
 */
public interface IceAuthenticator {

	/**
	 * Gets the password assigned to a local user.
	 * 
	 * @param ufrag
	 *            the fragment of the local user
	 * @return The password of the specified user.<br>
	 *         If the user has no password associated, returns an empty key.<br>
	 *         If the user does not exist, returns null.
	 */
	public byte[] getLocalKey(String ufrag);

	/**
	 * Gets the password assigned to a remote user.
	 * 
	 * @param ufrag
	 *            the fragment of the remote user
	 * @param media
	 *            the name of the target media stream
	 * @return The password of the specified user.<br>
	 *         If the user has no password associated, returns an empty key.<br>
	 *         If the user or media stream do not exist, returns null.
	 */
	public byte[] getRemoteKey(String ufrag, String media);

	/**
	 * Verifies whether a local user is registered.
	 * 
	 * @param ufrag
	 *            the fragment of the local user
	 * @return true if the user exists, false otherwise.
	 */
	boolean isUserRegistered(String ufrag);
}
