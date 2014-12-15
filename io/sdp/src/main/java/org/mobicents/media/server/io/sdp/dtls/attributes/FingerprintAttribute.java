package org.mobicents.media.server.io.sdp.dtls.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=fingerprint:[hash-function][fingerprint]<br>
 * 
 * <p>
 * Example:<br>
 * a=fingerprint:sha-256 D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:
 * 4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39
 * </p>
 * 
 * <p>
 * The fingerprint is the result of a hash function of the certificates used in
 * the DTLS-SRTP negotiation. This line creates a binding between the signaling
 * (which is supposed to be trusted) and the certificates used in DTLS, if the
 * fingerprint doesn’t match, then the session should be rejected.
 * </p>
 * 
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see <a href="https://tools.ietf.org/html/rfc4572#section-5">RFC4572</a>
 */
public class FingerprintAttribute extends AttributeField {

	public static final String ATTRIBUTE_TYPE = "fingerprint";

	private String hashFunction;
	private String fingerprint;

	public FingerprintAttribute() {
		this(null, null);
	}

	public FingerprintAttribute(String hashFunction, String fingerprint) {
		super(ATTRIBUTE_TYPE);
		this.hashFunction = hashFunction;
		this.fingerprint = fingerprint;
	}

	public String getHashFunction() {
		return hashFunction;
	}

	public void setHashFunction(String hashFunction) {
		this.hashFunction = hashFunction;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public void setFingerprint(String fingerprint) {
		this.fingerprint = fingerprint;
	}

	@Override
	public String toString() {
		super.builder.setLength(0);
		super.builder.append(BEGIN).append(ATTRIBUTE_TYPE).append(ATTRIBUTE_SEPARATOR);
		super.builder.append(this.hashFunction).append(" ").append(this.fingerprint);
		return super.builder.toString();
	}

}
