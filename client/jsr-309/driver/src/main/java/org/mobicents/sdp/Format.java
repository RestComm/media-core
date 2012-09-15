package org.mobicents.sdp;

import java.io.Serializable;

/**
 * 
 * @author amit bhayani
 *
 */
public class Format implements Cloneable, Serializable {

	public static final int NOT_SPECIFIED = -1;
	public static final int TRUE = 1;
	public static final int FALSE = 0;
	protected String encoding; // allowed to be null


	private long encodingCode; // This is set during equals/matches comparisons via isSameEncoding. Allows for fast
								// string comparisons.
	private int hash;

	public final static Format ANY = new Format("ANY");

	public Format(String encoding) {
		this.encoding = encoding;
		hash = encoding.hashCode();
	}

	public static final int FORMAT_HASHMAP_DEFAULT_INITIAL_CAPACITY = 8;

	public static final float FORMAT_HASHMAP_DEFAULT_LOAD_FACTOR = 1f;

	public String getEncoding() {
		return encoding;
	}

	@Override
	public boolean equals(Object format) {
		return format.hashCode() == hash;

	}

	@Override
	public int hashCode() {
		return hash;
	}

	public boolean matches(Format format) {
		if (format == null) {
			return false;
		}

		if (this == ANY) {
			return true;
		}

		if (format == ANY) {
			return true;
		}

		return (this.encoding == format.encoding || this.encoding == null || format.encoding == null || isSameEncoding(format));

	}

	public boolean isSameEncoding(Format other) {
		if (other == null) {
			return false;
		}
		if (other.encoding == null) {
			return false;
		}
		if (this.encoding == null) {
			return false;
		}
		if (other.encoding.equalsIgnoreCase(this.encoding)) {
			return true;
		}
		if (this.encodingCode == 0) {
			this.encodingCode = getEncodingCode(this.encoding);
		}
		if (other.encodingCode == 0) {
			other.encodingCode = getEncodingCode(other.encoding);
		}
		return encodingCode == other.encodingCode;
	}

	public boolean isSameEncoding(String encoding) {
		if (encoding == null) {
			return false;
		}
		if (this.encoding == null) {
			return false;
		}
		if (encoding == this.encoding) {
			return true;
		}
		if (this.encodingCode == 0) {
			this.encodingCode = getEncodingCode(this.encoding);
		}
		return this.encodingCode == getEncodingCode(encoding);

	}

	private long getEncodingCode(String enc) {
		if (enc == null) {
			return 0;
		}
		return FormatUtils.stringEncodingCodeVal(enc);
	}

	public String toString() {
		return getEncoding();
	}

}
