package org.mobicents.media.server.io.sdp;


/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface SdpField {

	static final String FIELD_SEPARATOR = "=";

	/**
	 * Gets the type of the field
	 * 
	 * @return the char that represents the field
	 */
	char getFieldType();

}
