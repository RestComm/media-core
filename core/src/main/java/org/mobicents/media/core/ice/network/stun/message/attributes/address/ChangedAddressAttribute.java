package org.mobicents.media.core.ice.network.stun.message.attributes.address;

import org.mobicents.media.core.ice.network.stun.message.attributes.StunAttribute;

/**
 * The CHANGED-ADDRESS attribute indicates the IP address and port where
 * responses would have been sent from if the "change IP" and "change port"
 * flags had been set in the CHANGE-REQUEST attribute of the Binding Request.
 * <p>
 * The attribute is always present in a Binding Response, independent of the
 * value of the flags. Its syntax is identical to MAPPED-ADDRESS.
 * </p>
 */
public class ChangedAddressAttribute extends AddressAttribute {

	public static final String NAME = "CHANGED-ADDRESS";

	public ChangedAddressAttribute() {
		super(StunAttribute.CHANGED_ADDRESS);
	}

	@Override
	public String getName() {
		return NAME;
	}
}
