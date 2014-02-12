package org.mobicents.media.core.ice.network.stun.messages.attributes.address;

/**
 * The RESPONSE-ADDRESS attribute indicates where the response to a Binding
 * Request should be sent.
 * 
 * Its syntax is identical to MAPPED-ADDRESS.
 */
public class ResponseAddressAttribute extends AddressAttribute {

	public static final String NAME = "RESPONSE-ADDRESS";

	public ResponseAddressAttribute() {
		super(RESPONSE_ADDRESS);
	}

	@Override
	public String getName() {
		return NAME;
	}
}
