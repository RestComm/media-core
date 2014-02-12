package org.mobicents.media.core.ice.network.stun.messages.attributes.address;

import org.mobicents.media.core.ice.network.stun.messages.attributes.StunAttribute;

/**
 * The SOURCE-ADDRESS attribute is present in Binding Responses.
 * 
 * It indicates the source IP address and port that the server is sending the
 * response from.<br>
 * A Its syntax is identical to that of MAPPED-ADDRESS.
 */
public class SourceAddressAttribute extends AddressAttribute {

	public static final String NAME = "SOURCE-ADDRESS";

	public SourceAddressAttribute() {
		super(StunAttribute.SOURCE_ADDRESS);
	}

	@Override
	public String getName() {
		return NAME;
	}
}
