package org.mobicents.media.server.io.sdp;

/**
 * List of supported SDP field types.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public enum FieldType {

	VERSION('v'), 
	ORIGIN('o'), 
	SESSION_NAME('s'), 
	TIMING('t'), 
	CONNECTION('c'),
	ATTRIBUTE('a'),
	MEDIA('m');

	private final char type;

	private FieldType(char type) {
		this.type = type;
	}
	
	public char getType() {
		return type;
	}
	
	public static FieldType fromType(char type) {
		for (FieldType fieldType : values()) {
			if(fieldType.type == type) {
				return fieldType;
			}
		}
		return null;
	}
}
