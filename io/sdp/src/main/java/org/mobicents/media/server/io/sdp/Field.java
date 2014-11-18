package org.mobicents.media.server.io.sdp;

import org.mobicents.media.server.io.sdp.exception.SdpException;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface Field {

	/**
	 * Gets the type of the field
	 * 
	 * @return the char that represents the field
	 */
	char getType();

	/**
	 * Parses a text line to initialize the field
	 * 
	 * @param text
	 *            the textual representation of the field
	 * @throws SdpException
	 *             In case the line cannot be correctly parsed
	 */
	void parse(String text) throws SdpException;

}
