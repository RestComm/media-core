package org.mobicents.media.core.ice.network.stun.messages.attributes.address;

import org.mobicents.media.core.ice.network.stun.messages.attributes.StunAttribute;

/**
 * The DESTINATION-ADDRESS is present in Send Requests of old TURN versions.
 * <p>
 * It specifies the address and port where the data is to be sent. It is encoded
 * in the same way as MAPPED-ADDRESS.
 * </p>
 */
public class DestinationAddressAttribute extends AddressAttribute {

	public static final String NAME = "DESTINATION-ADDRESS";

	public DestinationAddressAttribute() {
		super(StunAttribute.DESTINATION_ADDRESS);
	}

	@Override
	public String getName() {
		return NAME;
	}
	
}
