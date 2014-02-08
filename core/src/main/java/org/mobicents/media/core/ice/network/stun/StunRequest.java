package org.mobicents.media.core.ice.network.stun;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class StunRequest extends StunPacket {

	/**
	 * Checks whether requestType is a valid request type and if yes sets it as
	 * the type of the current instance.
	 * 
	 * @param requestType
	 *            the type to set
	 * @throws IllegalArgumentException
	 *             if requestType is not a valid request type
	 */
	@Override
	public void setMessageType(char requestType)
			throws IllegalArgumentException {
		if (!isRequestType(requestType))
			throw new IllegalArgumentException((int) (requestType)
					+ " - is not a valid request type.");
		super.setMessageType(requestType);
	}

}
