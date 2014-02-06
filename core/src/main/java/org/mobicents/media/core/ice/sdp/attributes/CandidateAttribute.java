package org.mobicents.media.core.ice.sdp.attributes;

import gov.nist.core.NameValue;
import gov.nist.core.Separators;
import gov.nist.javax.sdp.fields.AttributeField;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.sdp.SdpException;

import org.mobicents.media.core.ice.CandidateType;
import org.mobicents.media.core.ice.candidate.IceCandidate;

/**
 * Represents an ICEv19 candidate field as defined on <a
 * href="http://tools.ietf.org/html/rfc5245">RFC5245</a>.
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
 * 
 */
public class CandidateAttribute extends AttributeField {

	private static final long serialVersionUID = 6430392549555710175L;

	public static final String NAME = "candidate";
	public static final char TYPE_CHAR = 'a';

	/**
	 * According to draft-ietf-mmusic-ice-19, the foundation is used to optimize
	 * ICE performance in the Frozen algorithm
	 */
	private String foundation;
	/**
	 * An indicator for (S)RTP or (S)RTCP
	 */
	private String componentId;
	/**
	 * When the user agents perform address allocations to gather TCP-based
	 * candidates, two types of candidates can be obtained.<br>
	 * These are active candidates (TCP-ACT) or passive candidates (TCP-PASS).<br>
	 * An active candidate is one for which the agent will attempt to open an
	 * outbound connection, but will not receive incoming connection requests.<br>
	 * A passive candidate is one for which the agent will receive incoming
	 * connection attempts, but not attempt a connection.
	 */
	private String protocol;
	/**
	 * This is the weight used to prioritize single candidates.<br>
	 * Higher numbers are preferred over lower numbers, in case a connection can
	 * be established to this IP, port and protocol combination.<br>
	 * Each candidate for a media stream must have a unique priority (positive
	 * integer up to 2^31-1).
	 */
	private String priority;
	/**
	 * The IP address the second party can connect to.
	 */
	private String address;
	/**
	 * The port number the second party can connect to. The port for TCP RTP and
	 * RTCP gets multiplexed, whereas UDP ports for RTP and RTCP always differ.
	 */
	private String port;
	/**
	 * This is type information, describing the type of "advertised" address.<br>
	 * <b>host:</b> This is a local address<br>
	 * <b>relay:</b> This is the IP address from a relay (TURN) server<br>
	 * <b>srflx:</b> Server reflexive address is the NATed IP address<br>
	 */
	private String type;
	private String relatedAddress;
	private String relatedPort;
	private String generation;

	private String value;

	public CandidateAttribute(IceCandidate candidate) throws SdpException {
		super();
		setValue(candidate);
	}

	public CandidateAttribute(String value) throws SdpException {
		super();
		setValue(value);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void setName(String name) {
		throw new UnsupportedOperationException(
				"Cannot set name of a candidate attribute");
	}

	@Override
	public char getTypeChar() {
		return TYPE_CHAR;
	}

	@Override
	public void setValue(String value) throws SdpException {
		parseValue(value);
		this.value = value;
	}

	public void setValue(IceCandidate value) throws SdpException {
		parseCandidate(value);
		this.value = buildValue();
	}

	@Override
	public boolean hasValue() {
		return this.value != null && !this.value.isEmpty();
	}

	@Override
	public String getValue() {
		return this.value;
	}

	private void parseValue(String data) {
		try {
			List<String> tokens = Arrays.asList(data.split(" "));
			Iterator<String> iterator = tokens.iterator();

			// Foundation
			this.foundation = iterator.next();
			this.componentId = iterator.next();
			this.protocol = iterator.next();
			this.priority = iterator.next();
			this.address = iterator.next();
			this.port = iterator.next();
			// skip 'typ'
			iterator.next();
			this.type = iterator.next();

			CandidateType candidateType = CandidateType.fromDescription(type);
			if (candidateType == null) {
				throw new IllegalArgumentException(
						"Unrecognized candidate type " + type);
			}

			switch (candidateType) {
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
			// skip 'generation'
			iterator.next();
			this.generation = iterator.next();
		} catch (NoSuchElementException e) {
			throw new IllegalArgumentException(
					"Candidate line is badly formated: " + e.getMessage(), e);
		}
	}

	private void parseCandidate(IceCandidate candidate) {
		this.foundation = candidate.getFoundation();
		this.componentId = String.valueOf(candidate.getComponentId());
		this.protocol = candidate.getProtocol().name().toLowerCase();
		this.priority = String.valueOf(candidate.getPriority());
		this.address = candidate.getAddress().getHostAddress();
		this.port = String.valueOf(candidate.getPort());
		this.type = candidate.getType().getDescription();

		switch (candidate.getType()) {
		case RELAY:
		case SRFLX:
			// TODO Implement related address on ICE Candidate
			this.relatedAddress = "";
			this.relatedPort = "";
			break;
		default:
			break;
		}
		// TODO implement generation on ICE candidate
		this.generation = "";
	}

	private String buildValue() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.foundation).append(" ");
		builder.append(this.componentId).append(" ");
		builder.append(this.protocol).append(" ");
		builder.append(this.priority).append(" ");
		builder.append(this.address).append(" ");
		builder.append(this.port).append(" ");
		builder.append("typ ");
		builder.append(this.type).append(" ");
		switch (CandidateType.fromDescription(this.type)) {
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

	@Override
	public NameValue getAttribute() {
		if (this.attribute == null) {
			this.attribute = new NameValue(getName(), getValue());
		}
		return this.attribute;
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder(ATTRIBUTE_FIELD);
		builder.append(getName()).append(Separators.COLON);
		builder.append(getValue()).append(Separators.NEWLINE);
		return builder.toString();
	}
}
