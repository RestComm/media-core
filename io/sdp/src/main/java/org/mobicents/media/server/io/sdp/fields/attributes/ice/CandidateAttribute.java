package org.mobicents.media.server.io.sdp.fields.attributes.ice;

import org.mobicents.media.server.io.sdp.exception.SdpException;
import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * a=candidate:[foundation][componentId][protocol][priority][address][port][type][relAddress*][relPort*][generation]<br>
 * 
 * [*] - optional param
 * 
 * <p>
 * Represents an ICEv19 candidate field as defined on <a
 * href="http://tools.ietf.org/html/rfc5245">RFC5245</a>.
 * </p>
 * 
 * <p>
 * a=candidate:1995739850 1 udp 2113937151 192.168.1.65 54550 typ host generation 0<br>
 * a=candidate:2162486046 1 udp 1845501695 85.241.121.60 60495 typ srflx raddr 192.168.1.65 rport 54550 generation 0<br>
 * a=candidate:2564697628 1 udp 33562367 75.126.93.124 53056 typ relay raddr 85.241.121.60 rport 55027 generation 0
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CandidateAttribute extends AttributeField {

	private static final String NAME = "candidate";
	
	private final StringBuilder builder;
	
	private String foundation;
	private String componentId;
	private String protocol;
	private String priority;
	private String address;
	private String port;
	private String type;
	private String relatedAddress;
	private String relatedPort;
	private String generation;
	
	public CandidateAttribute() {
		this.builder = new StringBuilder();
		this.key = NAME;
	}
	
	public String getFoundation() {
		return foundation;
	}

	public void setFoundation(String foundation) {
		this.foundation = foundation;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
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

	public String getRelatedPort() {
		return relatedPort;
	}

	public void setRelatedPort(String relatedPort) {
		this.relatedPort = relatedPort;
	}

	public String getGeneration() {
		return generation;
	}

	public void setGeneration(String generation) {
		this.generation = generation;
	}

	@Override
	protected boolean isComplex() {
		return true;
	}
	
	@Override
	public void parse(String text) throws SdpException {
		// TODO Auto-generated method stub
		super.parse(text);
	}
	
	@Override
	public String toString() {
		// Clear the builder
		this.builder.setLength(0);
		// Build the new string
		this.builder.append().append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ").append().append(" ");
		return super.toString();
	}

}
