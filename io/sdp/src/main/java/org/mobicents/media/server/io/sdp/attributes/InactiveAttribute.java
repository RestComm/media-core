package org.mobicents.media.server.io.sdp.attributes;

/**
 * a=inactive
 * <p>
 * This specifies that the tools should be started in inactive mode.
 * </p>
 * <p>
 * This is necessary for interactive conferences where users can put other users
 * on hold. No media is sent over an inactive media stream. Note that an
 * RTP-based system SHOULD still send RTCP, even if started inactive. It can be
 * either a session or media-level attribute, and it is not dependent on
 * charset.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class InactiveAttribute extends AbstractConnectionModeAttribute {

	private static final String NAME = "inactive";
	private static final String FULL = "a=inactive";

	public InactiveAttribute() {
		super(NAME, ConnectionMode.INACTIVE);
	}

	@Override
	public String toString() {
		return FULL;
	}

}
