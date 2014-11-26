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
	
	private static final String PARSE_ERROR = "Cannot parse SDP: ";
	
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
	   
	public SessionDescription() {
		this.mediaMap = new HashMap<String, MediaDescriptionField>(5);
	}
	
}
