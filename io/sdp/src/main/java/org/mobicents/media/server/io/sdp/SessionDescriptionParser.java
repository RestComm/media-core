package org.mobicents.media.server.io.sdp;

import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.attributes.FormatParameterAttribute;
import org.mobicents.media.server.io.sdp.attributes.MaxPacketTimeAttribute;
import org.mobicents.media.server.io.sdp.attributes.PacketTimeAttribute;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.fields.AttributeField;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.OriginField;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;
import org.mobicents.media.server.io.sdp.fields.TimingField;
import org.mobicents.media.server.io.sdp.fields.VersionField;
import org.mobicents.media.server.io.sdp.ice.attributes.CandidateAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceLiteAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpMuxAttribute;

/**
 * Parses an SDP text description into a {@link SessionDescription} object.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SessionDescriptionParser {
	
	private static final String NEWLINE = "\n";
	private static final String PARSE_ERROR = "Cannot parse SDP: ";
	private static final String PARSE_ERROR_EMPTY = PARSE_ERROR + "empty";
	
	private static final SdpParserPipeline PARSERS = new SdpParserPipeline();
	
	public static SessionDescription parse(String text) throws SdpException { 
		
		if(text == null || text.isEmpty()) {
			throw new SdpException(PARSE_ERROR_EMPTY);
		}
		
		SessionDescription sdp = new SessionDescription();
		MediaDescriptionField currentMedia = null;
		
		// Process each line of SDP
		String[] lines = text.split(NEWLINE);
		for (String line : lines) {
			try {
				// Get type of field to check whether its an sdp attribute or not
				char fieldType = line.charAt(0);
				switch (fieldType) {
				case AttributeField.FIELD_TYPE:
					// The field is an attribute
					// Get the type of attribute so we can invoke the right parser 
					int separator = line.indexOf(AttributeField.ATTRIBUTE_SEPARATOR);
					String attributeType = (separator == - 1) ? line.substring(2) : line.substring(2, separator);
					
					// Get the right parser for the attribute and parse the text
					SdpParser<? extends AttributeField> attributeParser = PARSERS.getAttributeParser(attributeType);
					if(attributeParser != null) {
						convertAndApplyAttribute(attributeParser.parse(line), sdp, currentMedia);
					}
					break;
				default:
					// Get the right parser for the field and parse the text
					SdpParser<? extends SdpField> fieldParser = PARSERS.getFieldParser(fieldType);
					if(fieldParser != null) {
						currentMedia = convertAndApplyField(fieldParser.parse(line), sdp, currentMedia);
					}
					break;
				}
			} catch (Exception e) {
				throw new SdpException("Could not parse SDP: " + line, e);
			}
		}
		return sdp;
	}
	
	private static MediaDescriptionField convertAndApplyField(SdpField field, SessionDescription sdp, MediaDescriptionField media) {
		switch (field.getFieldType()) {
		case VersionField.FIELD_TYPE:
			sdp.setVersion((VersionField) field);
			break;

		case OriginField.FIELD_TYPE:
			sdp.setOrigin((OriginField) field);
			break;

		case SessionNameField.FIELD_TYPE:
			sdp.setSessionName((SessionNameField) field);
			break;
			
		case TimingField.FIELD_TYPE:
			sdp.setTiming((TimingField) field);
			break;

		case ConnectionField.FIELD_TYPE:
			if(media == null) {
				sdp.setConnection((ConnectionField) field);
			} else {
				media.setConnection((ConnectionField) field);
			}
			break;

		case MediaDescriptionField.FIELD_TYPE:
			media = (MediaDescriptionField) field;
			sdp.addMediaDescription(media);
			break;

		default:
			// Ignore unsupported type
			break;
		}
		
		return media;
	}
	
	private static void convertAndApplyAttribute(AttributeField attribute, SessionDescription sdp, MediaDescriptionField media) {
		switch (attribute.getKey()) {
		case RtpMapAttribute.ATTRIBUTE_TYPE:
			// TODO apply rtpmap attribute
			break;
			
		case FormatParameterAttribute.ATTRIBUTE_TYPE:
			// TODO apply fmt attribute
			break;
		
		case PacketTimeAttribute.ATTRIBUTE_TYPE:
			// TODO apply ptime attribute
			break;

		case MaxPacketTimeAttribute.ATTRIBUTE_TYPE:
			// TODO apply maxptime attribute
			break;
			
		case ConnectionModeAttribute.SENDONLY:
		case ConnectionModeAttribute.RECVONLY:
		case ConnectionModeAttribute.SENDRECV:
		case ConnectionModeAttribute.INACTIVE:
			if(media == null) {
				sdp.setConnectionMode((ConnectionModeAttribute) attribute);
			} else {
				media.setConnectionMode((ConnectionModeAttribute) attribute);
			}
			break;
			
		case RtcpAttribute.ATTRIBUTE_TYPE:
			media.setRtcp((RtcpAttribute) attribute);
			break;
			
		case RtcpMuxAttribute.ATTRIBUTE_TYPE:
			media.setRtcpMux((RtcpMuxAttribute) attribute);
			break;
			
		case IceLiteAttribute.ATTRIBUTE_TYPE:
			sdp.setIceLite((IceLiteAttribute) attribute);
			break;
			
		case IceUfragAttribute.ATTRIBUTE_TYPE:
			if(media == null) {
				sdp.setIceUfrag((IceUfragAttribute) attribute);
			} else {
				media.setIceUfrag((IceUfragAttribute) attribute);
			}
			break;
			
		case IcePwdAttribute.ATTRIBUTE_TYPE:
			if(media == null) {
				sdp.setIcePwd((IcePwdAttribute) attribute);
			} else {
				media.setIcePwd((IcePwdAttribute) attribute);
			}
			break;
			
		case CandidateAttribute.ATTRIBUTE_TYPE:
			media.addCandidate((CandidateAttribute) attribute);
			break;
			
		case FingerprintAttribute.ATTRIBUTE_TYPE:
			if(media == null) {
				sdp.setFingerprint((FingerprintAttribute) attribute);
			} else {
				media.setFingerprint((FingerprintAttribute) attribute);
			}
			break;
			
		default:
			break;
		}
	}
	
}
