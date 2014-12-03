package org.mobicents.media.server.io.sdp.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=fmtp:[format] [format specific parameters]<br>
 * 
 * <p>
 * This attribute allows parameters that are specific to a particular format to
 * be conveyed in a way that SDP does not have to understand them.<br>
 * The format must be one of the formats specified for the media.
 * Format-specific parameters may be any set of parameters required to be
 * conveyed by SDP and given unchanged to the media tool that will use this
 * format. At most one instance of this attribute is allowed for each format.<br>
 * It is a media-level attribute, and it is not dependent on charset.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class FormatParameterAttribute extends AttributeField {
	
	public static final String ATTRIBUTE_TYPE = "fmtp";
	private static final short DEFAULT_FORMAT = -1; 

	private short format;
	private String params;
	
	public FormatParameterAttribute(short format, String params) {
		super(ATTRIBUTE_TYPE);
		this.format = format;
		this.params = params;
	}

	public FormatParameterAttribute() {
		this(DEFAULT_FORMAT, null);
	}
	
	public short getFormat() {
		return format;
	}
	
	public void setFormat(short format) {
		this.format = format;
	}
	
	public String getParams() {
		return params;
	}
	
	public void setParams(String params) {
		this.params = params;
	}
	
	@Override
	public String toString() {
		super.builder.setLength(0);
		super.builder.append(BEGIN).append(ATTRIBUTE_TYPE).append(ATTRIBUTE_SEPARATOR);
		super.builder.append(this.format).append(" ").append(this.params);
		return super.builder.toString();
	}

}
