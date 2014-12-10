package org.mobicents.media.server.io.sdp.dtls.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=setup:[active|passive|actpass|holdconn]
 * 
 * <p>
 * The 'setup' attribute indicates which of the end points should initiate the
 * connection establishment.<br>
 * It is charset-independent and can be a session-level or a media-level
 * attribute.
 * </p>
 * 
 * <p>
 * Possible Values:
 * 
 * <ul>
 * <li><b>active</b> - The endpoint will initiate an outgoing connection.</li>
 * <li><b>passive</b> - The endpoint will accept an incoming connection.</li>
 * <li><b>actpass</b> - The endpoint is willing to accept an incoming connection
 * or to initiate an outgoing connection.</li>
 * <li><b>holdconn</b> - The endpoint does not want the connection to be
 * established for the time being.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * This parameter was initialing defined in RFC4145, which has been updated by
 * RFC4572.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4145">RFC4145</a>, <a
 *      href="http://tools.ietf.org/html/rfc4572">RFC4572</a>
 */
public class SetupAttribute extends AttributeField {
	
	public static final String ATTRIBUTE_TYPE = "setup";
	
	public static final String ACTIVE = "active";
	public static final String PASSIVE = "passive";
	public static final String ACTPASS = "actpass";
	public static final String HOLDCON = "holdconn";
	
	public SetupAttribute(String value) {
		super(ATTRIBUTE_TYPE);
		setValue(value);
	}
	
	public void setValue(String value) {
		if(!isSetupValid(value)) {
			throw new IllegalArgumentException("Invalid setup: " + value);
		}
		super.value = value;
	}
	
	public static boolean isSetupValid(String value) {
		if(value == null || value.isEmpty()) {
			return false;
		}
		return ACTIVE.equals(value) ||
				PASSIVE.equals(value) ||
				ACTPASS.equals(value) ||
				HOLDCON.equals(value);
	}

}
