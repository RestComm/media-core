package org.mobicents.media.server.io.sdp.ice.attributes;

import org.mobicents.media.server.io.sdp.AttributeField;
import org.mobicents.media.server.io.sdp.SdpException;

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
	
	// error messages
	private static final String INVALID_TYP = "Candidate type %s is not recognized.";

	private static final String NAME = "candidate";
	private static final String TO_ATTR_SEPARATOR = BEGIN + NAME + ATTRIBUTE_SEPARATOR;
	private static final int TO_ATTR_SEPARATOR_LENGTH = TO_ATTR_SEPARATOR.length();
	
	private static final String TYP = "typ";
	private static final String TYP_HOST = "host";
	private static final String TYP_SRFLX = "srflx";
	private static final String TYP_RELAY = "relay";
	private static final String GENERATION = "generation";
	private static final String RADDR = "raddr";
	private static final String RPORT = "rport";
	
	// TODO use proper IP address regex instead of [0-9\\.]+
	private static final String REGEX = "^" + TO_ATTR_SEPARATOR + "\\d+\\s\\d\\s\\w+\\s\\d+\\s[0-9\\.]+\\s\\d+\\s(typ)\\s\\w+(\\s(raddr)\\s[0-9\\.]+\\s(rport)\\s\\d+)?\\s(generation)\\s\\d+$";

	private final StringBuilder builder;

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
		this.builder = new StringBuilder();
		this.key = NAME;
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
	
	private boolean isCandidateTypeValid(String type) {
		return TYP_HOST.equals(type) || TYP_SRFLX.equals(type) || TYP_RELAY.equals(type);
	}

	@Override
	protected boolean isComplex() {
		return true;
	}
	
	@Override
	public boolean canParse(String text) {
		if(text == null || text.isEmpty()) {
			return false;
		}
		return text.matches(REGEX);
	}

	@Override
	public void parse(String text) throws SdpException {
		int index = 0;
		try {
			this.value = text.substring(TO_ATTR_SEPARATOR_LENGTH);
			String[] values = this.value.split(" ");
			
			this.foundation = Long.valueOf(values[index++]);
			this.componentId = Short.valueOf(values[index++]);
			this.protocol = values[index++];
			this.priority = Integer.valueOf(values[index++]);
			this.address = values[index++];
			this.port = Integer.valueOf(values[index++]);
			index++; // TYP
			this.type = values[index++];
			
			if(!isCandidateTypeValid(this.type)) {
				throw new IllegalArgumentException(String.format(INVALID_TYP, this.type));
			}
			
			if(!TYP_HOST.equals(this.type)) {
				index++; // RADDR
				this.relatedAddress = values[index++];
				index++; // RPORT
				this.relatedPort = Integer.valueOf(values[index++]);
			} else {
				this.relatedAddress = null;
				this.relatedPort = 0;
			}
			
			index++; // GENERATION
			this.generation = Integer.valueOf(values[index++]);
		} catch (Exception e) {
			throw new SdpException(String.format(PARSE_ERROR, text), e);
		}
		
	}

	@Override
	public String toString() {
		// Clear the builder
		this.builder.setLength(0);
		
		// Build the candidate string - mandatory fields first
		this.builder.append(BEGIN).append(NAME).append(ATTRIBUTE_SEPARATOR)
				.append(this.foundation).append(" ")
				.append(this.componentId).append(" ")
				.append(this.protocol).append(" ")
				.append(this.priority).append(" ")
				.append(this.address).append(" ")
				.append(this.port).append(" ")
				.append(TYP).append(" ").append(this.type);
		
		// Depending of type of candidate we may need to parse raddr and rport
		if(!TYP_HOST.equals(this.type)) {
			this.builder.append(" ")
			        .append(RADDR).append(" ").append(this.relatedAddress).append(" ")
			        .append(RPORT).append(" ").append(this.relatedPort);
		}
		// Append generation and we are done
		this.builder.append(" ").append(GENERATION).append(" ").append(this.generation);
		return this.builder.toString();
	}
	
}
