package org.mobicents.media.server.io.sdp;

public class SessionDescription {

	private VersionField version;
	private OriginField origin;
	// Origin
	// Connection
	// Timing
	// MediaDescription
	   // Attributes
	   
	public SessionDescription() {
		// TODO Auto-generated constructor stub
		this.version = new VersionField();
		this.origin = new OriginField();
	}
	
}
