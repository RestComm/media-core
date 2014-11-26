package org.mobicents.media.server.io.sdp.attributes;

/**
 * a=sendonly
 * 
 * <p>
 * This specifies that the tools should be started in send-only mode.
 * </p>
 * <p>
 * An example may be where a different unicast address is to be used for a
 * traffic destination than for a traffic source. In such a case, two media
 * descriptions may be used, one sendonly and one recvonly. It can be either a
 * session- or media-level attribute, but would normally only be used as a media
 * attribute. It is not dependent on charset. Note that sendonly applies only to
 * the media, and any associated control protocol (e.g., RTCP) SHOULD still be
 * received and processed as normal.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SendOnlyAttribute extends AbstractConnectionModeAttribute {

	private static final String NAME = "sendonly";
	private static final String FULL = "a=sendonly";

	public SendOnlyAttribute() {
		super(NAME, ConnectionMode.SEND_ONLY);
	}

	@Override
	public String toString() {
		return FULL;
	}

}
