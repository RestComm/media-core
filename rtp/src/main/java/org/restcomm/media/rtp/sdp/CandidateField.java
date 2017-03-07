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

package org.restcomm.media.rtp.sdp;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.restcomm.media.spi.utils.Text;

/**
 * Represents an ICEv19 candidate field as defined on
 * http://tools.ietf.org/html/rfc5245
 * <p>
 * a=candidate:1995739850 1 udp 2113937151 192.168.1.65 54550 typ host
 * generation 0<br>
 * a=candidate:2162486046 1 udp 1845501695 85.241.121.60 60495 typ srflx raddr
 * 192.168.1.65 rport 54550 generation 0<br>
 * a=candidate:2564697628 1 udp 33562367 75.126.93.124 53056 typ relay raddr
 * 85.241.121.60 rport 55027 generation 0
 * </p>
 * 
 * @author Henrique Rosa
 * @deprecated Use new /network/io/sdp library
 */
@Deprecated
public class CandidateField implements Comparable<CandidateField>{

	public static final Text CANDIDATE_FIELD = new Text("a=candidate");

	/**
	 * According to draft-ietf-mmusic-ice-19, the foundation is used to optimize
	 * ICE performance in the Frozen algorithm
	 */
	private Text foundation;
	/**
	 * An indicator for (S)RTP or (S)RTCP
	 */
	private Text componentId;
	/**
	 * When the user agents perform address allocations to gather TCP-based
	 * candidates, two types of candidates can be obtained.<br>
	 * These are active candidates (TCP-ACT) or passive candidates (TCP-PASS).<br>
	 * An active candidate is one for which the agent will attempt to open an
	 * outbound connection, but will not receive incoming connection requests.<br>
	 * A passive candidate is one for which the agent will receive incoming
	 * connection attempts, but not attempt a connection.
	 */
	private Text protocol;
	/**
	 * This is the weight used to prioritize single candidates.<br>
	 * Higher numbers are preferred over lower numbers, in case a connection can
	 * be established to this IP, port and protocol combination.<br>
	 * Each candidate for a media stream must have a unique priority (positive
	 * integer up to 2^31-1).
	 */
	private Text weight;
	/**
	 * The IP address the second party can connect to.
	 */
	private Text address;
	/**
	 * The port number the second party can connect to. The port for TCP RTP and
	 * RTCP gets multiplexed, whereas UDP ports for RTP and RTCP always differ.
	 */
	private Text port;
	/**
	 * This is type information, describing the type of "advertised" address.<br>
	 * <b>host:</b> This is a local address<br>
	 * <b>relay:</b> This is the IP address from a relay (TURN) server<br>
	 * <b>srflx:</b> Server reflexive address is the NATed IP address<br>
	 */
	private Text type;
	private Text relatedAddress;
	private Text relatedPort;
	private Text generation;

	public CandidateField(Text line) {
		parseCandidateLine(line);
	}

	public Text getFoundation() {
		return foundation;
	}

	public Text getComponentId() {
		return componentId;
	}

	public Text getProtocol() {
		return protocol;
	}

	public Text getWeight() {
		return weight;
	}

	public Text getAddress() {
		return address;
	}

	public Text getPort() {
		return port;
	}

	public Text getType() {
		return type;
	}

	public Text getRelatedAddress() {
		return relatedAddress;
	}
	
	public boolean hasRelatedAddress() {
		return this.relatedAddress != null;
	}

	public Text getRelatedPort() {
		return relatedPort;
	}

	public Text getGeneration() {
		return generation;
	}

	private void parseCandidateLine(Text line) {
		if (!line.startsWith(CANDIDATE_FIELD)) {
			throw new IllegalArgumentException(
					"Not a valid candidate attribute" + line);
		}

		try {
			Text data = (Text) line.subSequence(CANDIDATE_FIELD.length() + 1, line.length());
			Collection<Text> tokens = data.split(' ');
			Iterator<Text> iterator = tokens.iterator();

			// Foundation
			this.foundation = iterator.next();
			this.componentId = iterator.next();
			this.protocol = iterator.next();
			this.weight = iterator.next();
			this.address = iterator.next();
			this.port = iterator.next();
			// skip 'typ'
			iterator.next();
			this.type = iterator.next();

			AddressType addressType = AddressType.fromDescription(type);
			if (addressType == null) {
				throw new IllegalArgumentException(
						"Unrecognized address type: " + type);
			}

			switch (addressType) {
			case RELAY:
			case SRFLX:
				// skip raddr
				iterator.next();
				this.relatedAddress = iterator.next();
				// skip rport
				iterator.next();
				this.relatedPort = iterator.next();
				break;
			default:
				break;
			}

			// Generation attribute is present on Chrome SDP but not on FireFox
			if(iterator.hasNext()) {
				// skip 'generation'
				iterator.next();
				this.generation = iterator.next();
			}
		} catch (NoSuchElementException e) {
			throw new IllegalArgumentException("Candidate line is badly formated: "+ e.getMessage(), e);
		}
	}

	public enum AddressType {
		HOST("host"), RELAY("relay"), SRFLX("srflx");

		private Text description;

		private AddressType(String description) {
			this.description = new Text(description);
		}

		public Text getDescription() {
			return description;
		}

		public static AddressType fromDescription(Text value) {
			for (AddressType type : values()) {
				if (type.getDescription().equals(value)) {
					return type;
				}
			}
			return null;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(CANDIDATE_FIELD).append(":");
		builder.append(this.foundation).append(" ");
		builder.append(this.componentId).append(" ");
		builder.append(this.protocol).append(" ");
		builder.append(this.weight).append(" ");
		builder.append(this.address).append(" ");
		builder.append(this.port).append(" ");
		builder.append("typ ");
		builder.append(this.type).append(" ");
		switch (AddressType.fromDescription(this.type)) {
		case RELAY:
		case SRFLX:
			builder.append("raddr ");
			builder.append(this.relatedAddress).append(" ");
			builder.append("rport ");
			builder.append(this.relatedPort).append(" ");
			break;
		default:
			break;
		}
		builder.append("generation ");
		builder.append(this.generation);
		return builder.toString();
	}

	/**
	 * Compares two {@link CandidateField} by weight.
	 */
	public int compareTo(CandidateField o) {
		if (o == null) {
			return 1;
		}
		
		int myWeight = this.weight == null ? 0 : this.weight.toInteger();
		int otherWeight = o.getWeight() == null ? 0 : o.getWeight().toInteger();
		
		if(myWeight > otherWeight) {
			return 1;
		}
		if(myWeight < otherWeight) {
			return -1;
		}
		return 0;
	}

}
