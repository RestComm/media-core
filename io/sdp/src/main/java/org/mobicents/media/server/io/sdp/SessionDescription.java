package org.mobicents.media.server.io.sdp;

import java.util.HashMap;
import java.util.Map;

import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.OriginField;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;
import org.mobicents.media.server.io.sdp.fields.TimingField;
import org.mobicents.media.server.io.sdp.fields.VersionField;
import org.mobicents.media.server.io.sdp.ice.attributes.IceLiteAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SessionDescription {
	
	private static final String NEWLINE = "\n";
	
	private static final String PARSE_ERROR = "Cannot parse SDP: ";
	private static final String PARSE_ERROR_EMPTY = PARSE_ERROR + "empty";
	
	// SDP fields (session-level)
	private VersionField version;
	private OriginField origin;
	private SessionNameField sessionName;
	private ConnectionField connection;
	private TimingField timing;
	private ConnectionModeAttribute connectionMode;
	
	// ICE attributes (session-level)
	private IceLiteAttribute iceLite;
	private IcePwdAttribute icePwd;
	private IceUfragAttribute iceUfrag;
	
	// WebRTC attributes (session-level)
	private FingerprintAttribute fingerprint;

	// Media Descriptions
	private final Map<String, MediaDescriptionField> mediaMap;
	private MediaDescriptionField currentMedia;
	
	// PARSING
	private final SdpParserPipeline parsers = new SdpParserPipeline();
	   
	public SessionDescription() {
		this.mediaMap = new HashMap<String, MediaDescriptionField>(5);
	}
	
	public VersionField getVersion() {
		return version;
	}
	
	public OriginField getOrigin() {
		return origin;
	}
	
	public SessionNameField getSessionName() {
		return sessionName;
	}
	
	public ConnectionField getConnection() {
		return connection;
	}
	
	public TimingField getTiming() {
		return timing;
	}
	
	public MediaDescriptionField getMediaDescription(String mediaType) {
		return this.mediaMap.get(mediaType);
	}
	
	public void parse(String sdp) throws SdpException {
		if(sdp == null || sdp.isEmpty()) {
			throw new SdpException(PARSE_ERROR_EMPTY);
		}
		
		// Process each line of SDP
		String[] lines = sdp.split(NEWLINE);
		for (String line : lines) {
			parseLine(line);
		}
		
		// Clean pointer to current media descriptor
		this.currentMedia = null;
	}
	
	private void parseLine(String line) throws SdpException {
		FieldType fieldType = FieldType.fromType(line.charAt(0));

		// check whether field type is supported
		if(fieldType == null) {
			return;
		}
		
		switch (fieldType) {
		case ATTRIBUTE:
			// TODO get attribute parser
			break;
		default:
			SdpParser<? extends SdpField> parser = this.parsers.getFieldParser(fieldType);
			if(parser != null) {
				convertAndApply(parser.parse(line));
			}
			break;
		}
	}
	
	private void convertAndApply(SdpField field) {
		switch (field.getFieldType()) {
		case VERSION:
			this.version = (VersionField) field;
			break;

		case ORIGIN:
			this.origin = (OriginField) field;
			break;

		case SESSION_NAME:
			this.sessionName = (SessionNameField) field;

			break;
		case TIMING:
			this.timing = (TimingField) field;
			break;

		case CONNECTION:
			if(this.currentMedia == null) {
				this.connection = (ConnectionField) field;
			} else {
				// TODO this.currentMedia.setConnection((ConnectionField) field);
			}
			break;

		case MEDIA:
			MediaDescriptionField mediaField = (MediaDescriptionField) field;
			this.currentMedia = mediaField;
			this.mediaMap.put(mediaField.getMedia(), mediaField);
			break;

		default:
			// Ignore unsupported type
			break;
		}
	}
	
}
