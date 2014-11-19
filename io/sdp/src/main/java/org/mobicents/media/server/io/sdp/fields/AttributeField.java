package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;
import org.mobicents.media.server.io.sdp.exception.SdpException;

/**
 * a=[attribute]<br>
 * a=[attribute]:[value]
 * <p>
 * Attributes are the primary means for extending SDP.<br>
 * Attributes may be defined to be used as "session-level" attributes,
 * "media-level" attributes, or both.
 * </p>
 * <p>
 * A media description may have any number of attributes ("a=" fields) that are
 * media specific. These are referred to as "media-level" attributes and add
 * information about the media stream. Attribute fields can also be added before
 * the first media field; these "session-level" attributes convey additional
 * information that applies to the conference as a whole rather than to
 * individual media.
 * </p>
 * <p>
 * Attribute fields may be of two forms:
 * 
 * <ul>
 * <li>A property attribute is simply of the form "a=<flag>". These are binary
 * attributes, and the presence of the attribute conveys that the attribute is a
 * property of the session. An example might be "a=recvonly".
 * 
 * <li>A value attribute is of the form "a=<attribute>:<value>". For example, a
 * whiteboard could have the value attribute "a=orient: landscape"
 * </ul>
 * </p>
 * 
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public abstract class AttributeField implements Field {

	// text parsing
	protected static final String ATTRIBUTE_SEPARATOR = ":";
	
	protected static final char TYPE = 'a';
	protected static final String BEGIN = String.valueOf(TYPE) + FIELD_SEPARATOR;
	protected static final String SIMPLE_FORMAT = BEGIN + "%s";
	protected static final String COMPLEX_FORMAT = SIMPLE_FORMAT + ATTRIBUTE_SEPARATOR + "%s";
	
	// error messages
	private static final String EMPTY_TEXT = "Could not parse Attribute Field because text is empty";
	private static final String INVALID_TEXT = "Could not parse Attribute Field text: %s";
	
	protected String key;
	protected String value;
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
	
	@Override
	public char getType() {
		return TYPE;
	}
	
	protected abstract boolean isComplex();
	
	@Override
	public void parse(String text) throws SdpException {
		if(text == null || text.isEmpty()) {
			throw new SdpException(EMPTY_TEXT);
		}
		
		if(text.startsWith(BEGIN)) {
			try {
				String[] values = text.substring(2).split(ATTRIBUTE_SEPARATOR);
				this.key = values[0];
				if(key.isEmpty()) {
					throw new SdpException(String.format(INVALID_TEXT, text));
				}
				if(isComplex()) {
					this.value = values[1];
					if(value.isEmpty()) {
						throw new SdpException(String.format(INVALID_TEXT, text));
					}
				}
			} catch (IndexOutOfBoundsException e) {
				throw new SdpException(String.format(INVALID_TEXT, text), e);
			}
		} else {
			throw new SdpException(String.format(INVALID_TEXT, text));
		}
	}
	
	@Override
	public String toString() {
		if(isComplex()) {
			return String.format(COMPLEX_FORMAT, this.key, this.value);
		}
		return String.format(SIMPLE_FORMAT, this.key);
	}
	
}
