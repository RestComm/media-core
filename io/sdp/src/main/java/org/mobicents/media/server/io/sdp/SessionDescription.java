package org.mobicents.media.server.io.sdp;

import java.util.List;

import org.mobicents.media.server.io.sdp.fields.AttributeField;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.OriginField;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;
import org.mobicents.media.server.io.sdp.fields.TimingField;
import org.mobicents.media.server.io.sdp.fields.VersionField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SessionDescription {

	private VersionField version;
	private OriginField origin;
	private SessionNameField sessionName;
	private ConnectionField connection;
	private TimingField timingField;
	private List<AttributeField> attributes;
	private List<MediaDescriptionField> mediaDescriptions;
	
	// MediaDescription
	   
	public SessionDescription() {
		
	}
	
}
