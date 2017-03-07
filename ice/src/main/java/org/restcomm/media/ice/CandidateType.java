/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.restcomm.media.ice;

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
