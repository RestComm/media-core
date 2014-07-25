package org.mobicents.media.io.ice.sdp.attributes;

import gov.nist.core.Separators;
import gov.nist.javax.sdp.fields.AttributeField;

public class IcePwdAttribute extends AttributeField {

	public static final String NAME = "ice-pwd";

	private String pwd;

	public IcePwdAttribute(String ufrag) {
		this.pwd = ufrag;
	}

	@Override
	public void setValue(String value) {
		this.pwd = value;
	}

	@Override
	public String getValue() {
		return this.pwd;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public char getTypeChar() {
		return 'a';
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder(ATTRIBUTE_FIELD);
		builder.append(getName()).append(Separators.COLON);
		builder.append(getValue()).append(Separators.NEWLINE);
		return builder.toString();
	}
	
}
