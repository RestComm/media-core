package org.mobicents.media.server.io.sdp.fields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mobicents.media.server.io.sdp.MediaProfile;
import org.mobicents.media.server.io.sdp.SdpField;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.attributes.FormatParameterAttribute;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.mobicents.media.server.io.sdp.attributes.SsrcAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.SetupAttribute;
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
	
	private static final String NEWLINE = "\n";
	public static final char FIELD_TYPE = 'm';
	private static final String BEGIN = "m=";

	// SDP fields (media description specific)
	private String media;
	private int port;
	private MediaProfile protocol;
	private final List<Short> payloadTypes;
	private final Map<Short, RtpMapAttribute> formats;
	
	// SDP fields and attributes (media-level)
	private ConnectionField connection;
	private ConnectionModeAttribute connectionMode;
	private RtcpAttribute rtcp;
	private RtcpMuxAttribute rtcpMux;
	private SsrcAttribute ssrc;
	
	// ICE attributes (session-level)
	private IcePwdAttribute icePwd;
	private IceUfragAttribute iceUfrag;
	private List<CandidateAttribute> candidates;
	
	// WebRTC attributes (session-level)
	private FingerprintAttribute fingerprint;
	private SetupAttribute setup;

	private final StringBuilder builder;

	public MediaDescriptionField() {
		this.builder = new StringBuilder(BEGIN);
		this.payloadTypes = new ArrayList<Short>(10);
		this.formats = new HashMap<Short, RtpMapAttribute>(10);
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
	
	public void addPayloadType(short payloadType) {
		if(!this.payloadTypes.contains(payloadType)) {
			this.payloadTypes.add(payloadType);
		}
	}
	
	public void setPayloadTypes(short... payloadTypes) {
		this.payloadTypes.clear();
		for (short payloadType : payloadTypes) {
			addPayloadType(payloadType);
		}
	}
	
	public boolean containsPayloadType(short payloadType) {
		return this.payloadTypes.contains(payloadType);
	}
	
	public void setFormats(RtpMapAttribute ...formats) {
		this.formats.clear();
		int numFormats = formats.length;
		for (int i = 0; i < numFormats; i++) {
			addFormat(formats[i]);
		}
	}
	
	public void addFormat(RtpMapAttribute format) {
		this.formats.put(format.getPayloadType(), format);
	}
	
	public void addFormats(RtpMapAttribute ...formats) {
		int numFormats = formats.length;
		for (int i = 0; i < numFormats; i++) {
			addFormat(formats[i]);
		}
	}
	
	public boolean containsFormat(short format) {
		return this.formats.containsKey(format);
	}
	
	public void setFormatParameters(short payloadType, FormatParameterAttribute parameters) {
		RtpMapAttribute format = this.formats.get(payloadType);
		if(format != null) {
			format.setParameters(parameters);
		}
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
	
	public RtpMapAttribute[] getFormats() {
		if(this.formats.isEmpty()) {
			return null;
		}
		return this.formats.values().toArray(new RtpMapAttribute[this.formats.size()]);
	}
	
	public RtpMapAttribute getFormat(short payloadType) {
		return this.formats.get(payloadType);
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
	
	public SsrcAttribute getSsrc() {
		return ssrc;
	}
	
	public void setSsrc(SsrcAttribute ssrc) {
		this.ssrc = ssrc;
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
	
	public SetupAttribute getSetup() {
		return setup;
	}
	
	public void setSetup(SetupAttribute setup) {
		this.setup = setup;
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
		for (Short payloadType : this.payloadTypes) {
			this.builder.append(" ").append(payloadType);
		}
		
		appendField(this.connection);
		appendField(this.connectionMode);
		appendField(this.rtcp);
		appendField(this.rtcpMux);
		appendField(this.iceUfrag);
		appendField(this.icePwd);
		
		if (this.candidates != null && !this.candidates.isEmpty()) {
			for (CandidateAttribute candidate : this.candidates) {
				appendField(candidate);
			}
		}

		if (this.formats != null && !this.formats.isEmpty()) {
			for (RtpMapAttribute format : this.formats.values()) {
				appendField(format);
			}
		}
		
		appendField(this.setup);
		appendField(this.fingerprint);
		appendField(this.ssrc);
		return this.builder.toString();
	}
	
	private void appendField(SdpField field) {
		if(field != null) {
			this.builder.append(NEWLINE).append(field.toString());
		}
	}
	
	public static boolean isValidProfile(String profile) {
		return MediaProfile.containsProfile(profile);
	}

}
