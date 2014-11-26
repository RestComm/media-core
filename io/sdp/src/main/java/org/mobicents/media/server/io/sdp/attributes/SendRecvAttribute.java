package org.mobicents.media.server.io.sdp.attributes;

/**
 * a=sendrecv
 * <p>
 * This specifies that the tools should be started in send and receive mode.<br>
 * This is necessary for interactive conferences with tools that default to
 * receive-only mode. It can be either a session or media-level attribute, and
 * it is not dependent on charset.
 * </p>
 * <p>
 * If none of the attributes "sendonly", "recvonly", "inactive", and "sendrecv"
 * is present, "sendrecv" SHOULD be assumed as the default for sessions that are
 * not of the conference type "broadcast" or "H332".
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SendRecvAttribute extends AbstractConnectionModeAttribute {

	private static final String NAME = "sendrecv";
	private static final String FULL = "a=sendrecv";

	public SendRecvAttribute() {
		super(NAME, ConnectionMode.SEND_RECV);
	}

	@Override
	public String toString() {
		return FULL;
	}

}
