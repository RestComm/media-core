package org.mobicents.media.server.io.sdp.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * Fake attribute that gathers recvonly, sendonly, sendrecv.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public abstract class AbstractConnectionModeAttribute extends AttributeField {
	
	protected AbstractConnectionModeAttribute(String mode) {
		super(mode);
	}

}
