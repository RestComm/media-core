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
	
	public VersionField getVersion() {
		return version;
	}
	
	public void setVersion(VersionField version) {
		this.version = version;
	}
	
	public OriginField getOrigin() {
		return origin;
	}
	
	public void setOrigin(OriginField origin) {
		this.origin = origin;
	}
	
	public SessionNameField getSessionName() {
		return sessionName;
	}
	
	public void setSessionName(SessionNameField sessionName) {
		this.sessionName = sessionName;
	}
	
	public ConnectionField getConnection() {
		return connection;
	}
	
	public void setConnection(ConnectionField connection) {
		this.connection = connection;
	}
	
	public TimingField getTiming() {
		return timing;
	}
	
	public void setTiming(TimingField timing) {
		this.timing = timing;
	}
	
	public ConnectionModeAttribute getConnectionMode() {
		return connectionMode;
	}
	
	public void setConnectionMode(ConnectionModeAttribute connectionMode) {
		this.connectionMode = connectionMode;
	}

	public IceLiteAttribute getIceLite() {
		return iceLite;
	}

	public void setIceLite(IceLiteAttribute iceLite) {
		this.iceLite = iceLite;
	}

	public IcePwdAttribute getIcePwd() {
		return icePwd;
	}

	public void setIcePwd(IcePwdAttribute icePwd) {
		this.icePwd = icePwd;
	}

	public IceUfragAttribute getIceUfrag() {
		return iceUfrag;
	}

	public void setIceUfrag(IceUfragAttribute iceUfrag) {
		this.iceUfrag = iceUfrag;
	}

	public FingerprintAttribute getFingerprint() {
		return fingerprint;
	}

	public FingerprintAttribute getFingerprint(String media) {
		if(this.mediaMap.containsKey(media)) {
			FingerprintAttribute audioFingerprint = this.mediaMap.get(media).getFingerprint();
			if(audioFingerprint != null) {
				return audioFingerprint;
			}
		}
		return fingerprint;
	}

	public void setFingerprint(FingerprintAttribute fingerprint) {
		this.fingerprint = fingerprint;
	}

	public MediaDescriptionField getMediaDescription(String mediaType) {
		return this.mediaMap.get(mediaType);
	}
	
	public void addMediaDescription(MediaDescriptionField media) {
		this.mediaMap.put(media.getMedia(), media);
	}
	
	public boolean containsIce() {
		// Look for session-level ICE attributes
		if(this.iceLite != null || this.iceUfrag != null || this.icePwd != null) {
			return true;
		}
		// Look for media-level ICE attributes
		if(!this.mediaMap.isEmpty()) {
			for (MediaDescriptionField media : this.mediaMap.values()) {
				if(media.containsIce()) {
					return true;
				}
			}
		}
		// No ICE attributes found
		return false;
	}
	
	public boolean containsDtls() {
		// Look for session-level DTLS attributes
		if(this.fingerprint != null) {
			return true;
		}
		// Look for media-level DTLS attributes
		if(!this.mediaMap.isEmpty()) {
			for (MediaDescriptionField media : this.mediaMap.values()) {
				if(media.containsDtls()) {
					return true;
				}
			}
		}
		// No DTLS attributes found
		return false;
	}
	
}
