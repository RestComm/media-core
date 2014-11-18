package org.mobicents.media.server.io.sdp;

import org.mobicents.media.server.io.sdp.fields.ConnectionField;
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
	
	// Timing
	// MediaDescription
	   // Attributes
	   
	public SessionDescription() {
		// TODO Auto-generated constructor stub
		this.version = new VersionField();
		this.origin = new OriginField();
	}
	
}
