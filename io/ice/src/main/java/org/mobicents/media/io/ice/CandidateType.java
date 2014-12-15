package org.mobicents.media.io.ice;

/**
 * The ICE specification defines for candidate types: host, server reflexive,
 * peer reflexive and relayed candidates.
 * 
 * @author Henrique Rosa
 * @see <a href="http://tools.ietf.org/html/rfc5245#section-4.1.1.1">RFC5245</a>
 */
public enum CandidateType {

	HOST("host", 126), PRFLX("prflx", 110), SRFLX("srflx", 100), RELAY("relay",0);

	private String description;
	private int preference;

	private CandidateType(String description, int preference) {
		this.description = description;
		this.preference = preference;
	}

	public String getDescription() {
		return description;
	}

	public int getPreference() {
		return preference;
	}

	public static CandidateType fromDescription(String description) {
		for (CandidateType value : values()) {
			if (value.getDescription().equals(description)) {
				return value;
			}
		}
		return null;
	}
	
	public static int count() {
		return values().length;
	}
	
}
