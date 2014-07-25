package org.mobicents.media.io.ice.sdp.attributes;

import gov.nist.core.Separators;
import gov.nist.javax.sdp.fields.AttributeField;

/**
 * Represent an attribute field that takes no value
 * 
 * @author Henrique Rosa
 * 
 */
public abstract class UnvaluedAttributeField extends AttributeField {

	private static final long serialVersionUID = -5050923408054775066L;

	private String name;

	public UnvaluedAttributeField(String name) {
		this.name = name;
	}

	@Override
	public void setValue(String value) {
		// No value
	}

	@Override
	public String getValue() {
		// No value
		return null;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public char getTypeChar() {
		return 'a';
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder(ATTRIBUTE_FIELD);
		builder.append(getName()).append(Separators.NEWLINE);
		return builder.toString();
	}
}
