package org.mobicents.media.core.ice.network.stun;

/**
 * Represents a STUN response packet
 * 
 * @author Henrique Rosa
 * @see StunPacket
 */
public class StunResponse extends StunPacket {

	/**
	 * Determines whether this instance represents a STUN error response.
	 * 
	 * @return <tt>true</tt> if this instance represents a STUN error response;
	 *         otherwise, <tt>false</tt>
	 */
	public boolean isErrorResponse() {
		return super.isErrorResponseType(getMessageType());
	}

	/**
	 * Determines whether this instance represents a STUN success response.
	 * 
	 * @return <tt>true</tt> if this instance represents a STUN success
	 *         response; otherwise, <tt>false</tt>
	 */
	public boolean isSuccessResponse() {
		return super.isSuccessResponseType(getMessageType());
	}

	/**
	 * Checks whether responseType is a valid response type and if yes sets it
	 * as the type of the current instance.
	 * 
	 * @param responseType
	 *            the type to set
	 * @throws IllegalArgumentException
	 *             if responseType is not a valid response type
	 */
	public void setMessageType(char responseType)
			throws IllegalArgumentException {
		if (!isResponseType(responseType))
			throw new IllegalArgumentException(Integer.toString(responseType)
					+ " is not a valid response type.");

		super.setMessageType(responseType);
	}

}
