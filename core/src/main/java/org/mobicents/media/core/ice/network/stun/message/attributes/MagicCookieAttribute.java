package org.mobicents.media.core.ice.network.stun.message.attributes;

import org.ice4j.attribute.LifetimeAttribute;
import org.mobicents.media.core.ice.network.stun.StunException;

/**
 * The MAGIC-COOKIE attribute.
 * 
 * It is used with old version of TURN (Google, Live messenger variant, ...).
 */
public class MagicCookieAttribute extends StunAttribute {

	public static final String NAME = "MAGIC-COOKIE";
	public static final char DATA_LENGTH = 4;

	private int value = 0x72c64bc6;

	protected MagicCookieAttribute() {
		super(StunAttribute.MAGIC_COOKIE);
	}

	@Override
	public char getDataLength() {
		return DATA_LENGTH;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof LifetimeAttribute)) {
			return false;
		}
		if (other == this) {
			return true;
		}
		MagicCookieAttribute att = (MagicCookieAttribute) other;
		if (att.getAttributeType() != this.getAttributeType()
				|| att.getDataLength() != this.getDataLength()
				|| att.value != this.value) {
			return false;
		}
		return true;
	}

	@Override
	public byte[] encode() {
		byte binValue[] = new byte[HEADER_LENGTH + DATA_LENGTH];

		// Type
		binValue[0] = (byte) (getAttributeType() >> 8);
		binValue[1] = (byte) (getAttributeType() & 0x00FF);
		// Length
		binValue[2] = (byte) (getDataLength() >> 8);
		binValue[3] = (byte) (getDataLength() & 0x00FF);
		// Data
		binValue[4] = (byte) ((value >> 24) & 0xff);
		binValue[5] = (byte) ((value >> 16) & 0xff);
		binValue[6] = (byte) ((value >> 8) & 0xff);
		binValue[7] = (byte) ((value) & 0xff);

		return binValue;
	}

	@Override
	protected void decodeAttributeBody(byte[] data, char offset, char length)
			throws StunException {
		if (length != DATA_LENGTH) {
			throw new StunException("Invalid length:" + length);
		}

		this.value = ((data[0] << 24) & 0xff000000)
				+ ((data[1] << 16) & 0x00ff0000)
				+ ((data[2] << 8) & 0x0000ff00) + (data[3] & 0x000000ff);
	}

}
