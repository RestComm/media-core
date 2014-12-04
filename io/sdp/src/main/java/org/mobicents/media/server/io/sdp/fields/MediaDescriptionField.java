package org.mobicents.media.server.io.sdp.fields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mobicents.media.server.io.sdp.MediaProfile;
import org.mobicents.media.server.io.sdp.SdpField;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.attributes.FormatParameterAttribute;
import org.mobicents.media.server.io.sdp.attributes.GenericAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.CandidateAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpMuxAttribute;

/**
 * m=[media] [port] [proto] [fmt]
 * 
 * <p>
 * A session description may contain a number of media descriptions.<br>
 * Each media description starts with an "m=" field and is terminated by either
 * the next "m=" field or by the end of the session description.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MediaDescriptionField implements SdpField {
	
	public static final char FIELD_TYPE = 'm';
	private static final String BEGIN = "m=";

	// SDP fields (media description specific)
	private String media;
	private int port;
	private MediaProfile protocol;
	private final List<Integer> formats;
	
	// SDP fields and attributes (media-level)
	private ConnectionField connection;
	private ConnectionModeAttribute connectionMode;
	private RtcpAttribute rtcp;
	private RtcpMuxAttribute rtcpMux;
	private final Map<Integer, FormatParameterAttribute> formatParameterMap;
	
	// Generic attributes that cannot be recognized
	private final List<GenericAttribute> genericAttributes;
	
	// ICE attributes (session-level)
	private IcePwdAttribute icePwd;
	private IceUfragAttribute iceUfrag;
	private List<CandidateAttribute> candidates;
	
	// WebRTC attributes (session-level)
	private FingerprintAttribute fingerprint;

	private final StringBuilder builder;

	public MediaDescriptionField() {
		this.builder = new StringBuilder(BEGIN);
		this.formats = new ArrayList<Integer>(10);
		this.formatParameterMap = new HashMap<Integer, FormatParameterAttribute>(10);
		this.genericAttributes = new ArrayList<GenericAttribute>(10);
	}
	
	public String getMedia() {
		return media;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getProtocol() {
		return protocol.getProfile();
	}

	public void setProtocol(MediaProfile protocol) {
		this.protocol = protocol;
	}
	
	public void setFormats(int ...formats) {
		this.formats.clear();
		int numFormats = formats.length;
		for (int i = 0; i < numFormats; i++) {
			this.formats.add(formats[i]);
		}
	}
	
	public void addFormats(int ...formats) {
		int numFormats = formats.length;
		for (int i = 0; i < numFormats; i++) {
			this.formats.add(formats[i]);
		}
	}
	
	public boolean containsFormat(int format) {
		for (int fmt : this.formats) {
			if(format == fmt) {
				return true;
			}
		}
		return false;
	}

	public ConnectionField getConnection() {
		return connection;
	}

	public void setConnection(ConnectionField connection) {
		this.connection = connection;
	}
	
	public ConnectionModeAttribute getConnectionMode() {
		return connectionMode;
	}
	
	public void setConnectionMode(ConnectionModeAttribute connectionMode) {
		this.connectionMode = connectionMode;
	}
	
	public RtcpAttribute getRtcp() {
		return rtcp;
	}
	
	public void setRtcp(RtcpAttribute rtcp) {
		this.rtcp = rtcp;
	}
	
	public RtcpMuxAttribute getRtcpMux() {
		return rtcpMux;
	}
	
	public void setRtcpMux(RtcpMuxAttribute rtcpMux) {
		this.rtcpMux = rtcpMux;
	}
	
	public IceUfragAttribute getIceUfrag() {
		return iceUfrag;
	}
	
	public void setIceUfrag(IceUfragAttribute iceUfrag) {
		this.iceUfrag = iceUfrag;
	}
	
	public IcePwdAttribute getIcePwd() {
		return icePwd;
	}
	
	public void setIcePwd(IcePwdAttribute icePwd) {
		this.icePwd = icePwd;
	}
	
	public CandidateAttribute[] getCandidates() {
		if(this.candidates == null || this.candidates.isEmpty()) {
			return null;
		}
		return candidates.toArray(new CandidateAttribute[this.candidates.size()]);
	}
	
	public void addCandidate(CandidateAttribute candidate) {
		if(this.candidates == null) {
			this.candidates = new ArrayList<CandidateAttribute>(8);
			this.candidates.add(candidate);
		} else if(!this.candidates.contains(candidate)) {
			this.candidates.add(candidate);
		}
	}
	
	public void removeCandidate(CandidateAttribute candidate) {
		if(this.candidates != null) {
			this.candidates.remove(candidate);
		}
	}
	
	public void removeAllCandidates() {
		if(this.candidates != null) {
			this.candidates.clear();
		}
	}
	
	public FingerprintAttribute getFingerprint() {
		return fingerprint;
	}
	
	public void setFingerprint(FingerprintAttribute fingerprint) {
		this.fingerprint = fingerprint;
	}

	@Override
	public char getFieldType() {
		return FIELD_TYPE;
	}

	@Override
	public String toString() {
		// Clean builder
		this.builder.setLength(0);
		this.builder.append(BEGIN)
		        .append(this.media).append(" ")
				.append(this.port).append(" ")
				.append(this.protocol);
		for (int payload : this.formats) {
			this.builder.append(" ").append(payload);
		}
		// Append new lines for attributes
		for (GenericAttribute attribute : this.genericAttributes) {
			this.builder.append("\n").append(attribute.toString());
		}
		return this.builder.toString();
	}
	
	public static boolean isValidProfile(String profile) {
		return MediaProfile.containsProfile(profile);
	}

}
