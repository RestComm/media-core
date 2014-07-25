package org.mobicents.media.io.ice.sdp.attributes;

import gov.nist.core.NameValue;
import gov.nist.core.Separators;
import gov.nist.javax.sdp.fields.AttributeField;

import org.mobicents.media.io.ice.IceCandidate;

public class RtcpAttribute extends AttributeField {

	private static final long serialVersionUID = 2147297739130543122L;

	public static final String NAME = "rtcp";
	public static final String IP4 = "IP4";
	public static final String IP6 = "IP6";

	private String direction;
	private String address;
	private String addressType;
	private int port;

	public RtcpAttribute(IceCandidate candidate, String direction) {
		this.direction = direction;
		this.address = candidate.getAddress().getHostAddress();
		this.addressType = candidate.isIPv4() ? IP4 : IP6;
		this.port = candidate.getPort();
	}

	@Override
	public char getTypeChar() {
		return 'a';
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void setName(String name) {
		throw new UnsupportedOperationException(
				"Cannot change the name of the " + NAME + " attribute");
	}

	@Override
	public String getValue() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.port).append(" ");
		builder.append(this.direction).append(" ");
		builder.append(this.addressType).append(" ");
		builder.append(this.address);
		return builder.toString();
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder(ATTRIBUTE_FIELD);
		builder.append(getName()).append(Separators.COLON);
		builder.append(getValue()).append(Separators.NEWLINE);
		return builder.toString();
	}

	@Override
	public NameValue getAttribute() {
		if (this.attribute == null) {
			this.attribute = new NameValue(getName(), getValue());
		}
		return this.attribute;
	}
}
