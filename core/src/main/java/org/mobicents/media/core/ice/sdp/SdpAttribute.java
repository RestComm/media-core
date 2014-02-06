package org.mobicents.media.core.ice.sdp;

import gov.nist.javax.sdp.fields.AttributeField;

public interface SdpAttribute<T extends AttributeField> {

	/**
	 * Transforms the object into an SDP attribute field.
	 * 
	 * @return The attribute field based on this object.
	 */
	T toSdpAttribute();

	/**
	 * Retrieves the textual representation of the SDP attribute.
	 * 
	 * @return The text line corresponding to the SDP attribute.
	 */
	String toSdpLine();

}
