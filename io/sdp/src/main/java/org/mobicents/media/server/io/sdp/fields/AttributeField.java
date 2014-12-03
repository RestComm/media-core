package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.FieldType;
import org.mobicents.media.server.io.sdp.SdpField;


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
public class AttributeField implements SdpField {

	// text parsing
	public static final String ATTRIBUTE_SEPARATOR = ":";
	
	protected static final FieldType TYPE = FieldType.ATTRIBUTE;
	protected static final String BEGIN = "a=";
	protected static final int BEGIN_LENGTH = BEGIN.length();
	
	protected final StringBuilder builder;
	
	protected String key;
	protected String value;
	
	protected AttributeField() {
		this.builder = new StringBuilder(BEGIN);
	}
	
	protected AttributeField(String key, String value) {
		this();
		this.key = key;
		this.value = value;
	}
	
	protected AttributeField(String key) {
		this(key, null);
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
	
	@Override
	public FieldType getFieldType() {
		return TYPE;
	}
	
	@Override
	public String toString() {
		// Clean String Builder
		this.builder.setLength(BEGIN_LENGTH);
		this.builder.append(this.key);
		if(this.value != null && !this.value.isEmpty()) {
			this.builder.append(ATTRIBUTE_SEPARATOR).append(this.value);
		}
		return this.builder.toString();
	}
	
}
