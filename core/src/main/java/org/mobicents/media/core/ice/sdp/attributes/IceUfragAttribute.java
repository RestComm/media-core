package org.mobicents.media.core.ice.sdp.attributes;

import gov.nist.core.Separators;
import gov.nist.javax.sdp.fields.AttributeField;

public class IceUfragAttribute extends AttributeField {

	private static final long serialVersionUID = -2841586967616367922L;

	public static final String NAME = "ice-ufrag";

	private String ufrag;

	public IceUfragAttribute(String ufrag) {
		this.ufrag = ufrag;
	}

	@Override
	public void setValue(String value) {
		this.ufrag = value;
	}

	@Override
	public String getValue() {
		return this.ufrag;
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
