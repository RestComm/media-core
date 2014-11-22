package org.mobicents.media.server.io.sdp;


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
public class AttributeField implements Field {

	// text parsing
	protected static final String ATTRIBUTE_SEPARATOR = ":";
	
	protected static final char TYPE = 'a';
	protected static final String BEGIN = TYPE + FIELD_SEPARATOR;
	protected static final int BEGIN_LENGTH = BEGIN.length();
	protected static final String BASE_REGEX = "^" + BEGIN + "\\S+";
	protected static final String SIMPLE_REGEX = BASE_REGEX + "$";
	protected static final String COMPLEX_REGEX = BASE_REGEX + ATTRIBUTE_SEPARATOR + "\\.*+$";
	
	protected final StringBuilder builder;
	
	protected String key;
	protected String value;
	protected boolean complex;
	
	public AttributeField(boolean complex) {
		this.builder = new StringBuilder(BEGIN);
		this.complex = complex;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
	
	@Override
	public char getFieldType() {
		return TYPE;
	}
	
	protected boolean isComplex() {
		return this.complex;
	}
	
	@Override
	public boolean canParse(String text) {
		if(text == null || text.isEmpty()) {
			return false;
		}
		
		if(this.complex) {
			return text.matches(COMPLEX_REGEX);
		}
		return text.matches(SIMPLE_REGEX);
	}
	
	@Override
	public void parse(String text) throws SdpException {
		try {
			String[] values = text.substring(2).split(ATTRIBUTE_SEPARATOR);
			this.key = values[0];
			if(key.isEmpty()) {
				throw new IllegalArgumentException("Attribute name is empty");
			}
			if(isComplex()) {
				this.value = values[1];
				if(value.isEmpty()) {
					throw new IllegalAccessException("Attribute value is empty");
				}
			} else {
				this.value = null;
			}
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + text, e);
		}
	}
	
	@Override
	public String toString() {
		// Clean String Builder
		this.builder.setLength(BEGIN_LENGTH);
		this.builder.append(this.key);
		if(this.complex) {
			this.builder.append(ATTRIBUTE_SEPARATOR).append(this.value);
		}
		return this.builder.toString();
	}
	
}
