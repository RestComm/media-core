package org.mobicents.media.server.io.sdp.ice.attributes;

import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=candidate:[foundation][componentId][protocol][priority][address][port][type][relAddress*][relPort*][generation]<br>
 * 
 * [*] - optional param
 * 
 * <p>
 * a=candidate:1995739850 1 udp 2113937151 192.168.1.65 54550 typ host generation 0<br>
 * a=candidate:2162486046 1 udp 1845501695 85.241.121.60 60495 typ srflx raddr 192.168.1.65 rport 54550 generation 0<br>
 * a=candidate:2564697628 1 udp 33562367 75.126.93.124 53056 typ relay raddr 85.241.121.60 rport 55027 generation 0
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @see <a href="http://tools.ietf.org/html/rfc5245">RFC5245</a> 
 */
public class CandidateAttribute extends AttributeField {
	
	public static final String ATTRIBUTE_TYPE = "candidate";
	
	public static final String TYP = "typ";
	public static final String TYP_HOST = "host";
	public static final String TYP_SRFLX = "srflx";
	public static final String TYP_RELAY = "relay";
	public static final String GENERATION = "generation";
	public static final String RADDR = "raddr";
	public static final String RPORT = "rport";
	
	private long foundation;
	private short componentId;
	private String protocol;
	private int priority;
	private String address;
	private int port;
	private String type;
	private String relatedAddress;
	private int relatedPort;
	private int generation;

	public CandidateAttribute() {
		super(ATTRIBUTE_TYPE);
	}
	
	public long getFoundation() {
		return foundation;
	}

	public void setFoundation(long foundation) {
		this.foundation = foundation;
	}

	public short getComponentId() {
		return componentId;
	}

	public void setComponentId(short componentId) {
		this.componentId = componentId;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getCandidateType() {
		return type;
	}

	public void setCandidateType(String type) {
		this.type = type;
	}

	public String getRelatedAddress() {
		return relatedAddress;
	}

	public void setRelatedAddress(String relatedAddress) {
		this.relatedAddress = relatedAddress;
	}

	public int getRelatedPort() {
		return relatedPort;
	}

	public void setRelatedPort(int relatedPort) {
		this.relatedPort = relatedPort;
	}

	public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = generation;
	}
	
	public static boolean isCandidateTypeValid(String type) {
		return TYP_HOST.equals(type) || TYP_SRFLX.equals(type) || TYP_RELAY.equals(type);
	}

	@Override
	public String toString() {
		// Clear the builder
		super.builder.setLength(0);
		
		// Build the candidate string - mandatory fields first
		this.builder.append(BEGIN).append(ATTRIBUTE_TYPE).append(ATTRIBUTE_SEPARATOR)
				.append(this.foundation).append(" ")
				.append(this.componentId).append(" ")
				.append(this.protocol).append(" ")
				.append(this.priority).append(" ")
				.append(this.address).append(" ")
				.append(this.port).append(" ")
				.append(TYP).append(" ").append(this.type);
		
		// Depending of type of candidate we may need to parse raddr and rport
		if(!TYP_HOST.equals(this.type)) {
			super.builder.append(" ")
			        .append(RADDR).append(" ").append(this.relatedAddress).append(" ")
			        .append(RPORT).append(" ").append(this.relatedPort);
		}
		// Append generation and we are done
		super.builder.append(" ").append(GENERATION).append(" ").append(this.generation);
		return super.builder.toString();
	}
	
}
