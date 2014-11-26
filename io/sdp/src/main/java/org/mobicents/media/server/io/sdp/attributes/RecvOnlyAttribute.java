package org.mobicents.media.server.io.sdp.attributes;


/**
 * a=recvonly
 * 
 * <p>
 * This specifies that the tools should be started in receive-only mode where
 * applicable.
 * </p>
 * <p>
 * It can be either a session- or media- level attribute, and it is not
 * dependent on charset. Note that recvonly applies to the media only, not to
 * any associated control protocol (e.g., an RTP-based system in recvonly mode
 * SHOULD still send RTCP packets).
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RecvOnlyAttribute extends AbstractConnectionModeAttribute {
	
	private static final String NAME = "recvonly";
	private static final String FULL = "a=recvonly";
	
	public RecvOnlyAttribute() {
		super(NAME, ConnectionMode.RECV_ONLY);
	}

	@Override
	public String toString() {
		return FULL;
	}
}
