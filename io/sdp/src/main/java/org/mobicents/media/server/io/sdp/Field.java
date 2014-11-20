package org.mobicents.media.server.io.sdp;

import org.mobicents.media.server.io.sdp.exception.SdpException;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface Field {

	static final String FIELD_SEPARATOR = "=";
	
	static final String PARSE_ERROR = "Could not parse SDP: %s";

	/**
	 * Gets the type of the field
	 * 
	 * @return the char that represents the field
	 */
	char getFieldType();

	/**
	 * Checks whether the attribute can parse or not the provided SDP
	 * 
	 * @param text
	 *            the SDP line to be parsed
	 * @return <code>true</code> if line can be parsed; <code>false</code>
	 *         otherwise.
	 */
	boolean canParse(String text);

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
