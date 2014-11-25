package org.mobicents.media.server.io.sdp.ice.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.ice.attributes.CandidateAttribute;


/**
 * Parses SDP text to construct {@link CandidateAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class CandidateAttributeParser implements SdpParser<CandidateAttribute> {
	
	// TODO use proper IP address regex instead of [0-9\\.]+
	private static final String REGEX = "^a=candidate:\\d+\\s\\d\\s\\w+\\s\\d+\\s[0-9\\.]+\\s\\d+\\s(typ)\\s\\w+(\\s(raddr)\\s[0-9\\.]+\\s(rport)\\s\\d+)?\\s(generation)\\s\\d+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public CandidateAttribute parse(String sdp) throws SdpException {
		try {
			String[] values = sdp.substring(12).split(" ");
			int index = 0;

			// extract data from SDP
			long foundation = Long.parseLong(values[index++]);
			short componentId = Short.parseShort(values[index++]);
			String protocol = values[index++];
			int priority = Integer.parseInt(values[index++]);
			String address = values[index++];
			int port = Integer.parseInt(values[index++]);
			index++; // TYP
			String type = values[index++];
			
			if(!CandidateAttribute.isCandidateTypeValid(type)) {
				throw new IllegalArgumentException("Unrecognized candidate type: " + type);
			}
			
			String relatedAddress = null;
			int relatedPort = 0;
			if(!CandidateAttribute.TYP_HOST.equals(type)) {
				index++; // RADDR
				relatedAddress = values[index++];
				index++; // RPORT
				relatedPort = Integer.parseInt(values[index++]);
			}
			
			index++; // GENERATION
			int generation = Integer.parseInt(values[index++]);
			
			// Create object from extracted data
			CandidateAttribute candidate = new CandidateAttribute();
			candidate.setFoundation(foundation);
			candidate.setComponentId(componentId);
			candidate.setProtocol(protocol);
			candidate.setPriority(priority);
			candidate.setAddress(address);
			candidate.setPort(port);
			candidate.setCandidateType(type);
			candidate.setRelatedAddress(relatedAddress);
			candidate.setRelatedPort(relatedPort);
			candidate.setGeneration(generation);
			return candidate;
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(CandidateAttribute field, String sdp) throws SdpException {
		try {
			String[] values = sdp.substring(12).split(" ");
			int index = 0;

			// extract data from SDP
			long foundation = Long.parseLong(values[index++]);
			short componentId = Short.parseShort(values[index++]);
			String protocol = values[index++];
			int priority = Integer.parseInt(values[index++]);
			String address = values[index++];
			int port = Integer.parseInt(values[index++]);
			index++; // TYP
			String type = values[index++];
			
			if(!CandidateAttribute.isCandidateTypeValid(type)) {
				throw new IllegalArgumentException("Unrecognized candidate type: " + type);
			}
			
			String relatedAddress = null;
			int relatedPort = 0;
			if(!CandidateAttribute.TYP_HOST.equals(type)) {
				index++; // RADDR
				relatedAddress = values[index++];
				index++; // RPORT
				relatedPort = Integer.parseInt(values[index++]);
			}
			
			index++; // GENERATION
			int generation = Integer.parseInt(values[index++]);
			
			// Create object from extracted data
			field.setFoundation(foundation);
			field.setComponentId(componentId);
			field.setProtocol(protocol);
			field.setPriority(priority);
			field.setAddress(address);
			field.setPort(port);
			field.setCandidateType(type);
			field.setRelatedAddress(relatedAddress);
			field.setRelatedPort(relatedPort);
			field.setGeneration(generation);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
