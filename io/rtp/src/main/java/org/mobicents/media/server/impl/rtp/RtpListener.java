package org.mobicents.media.server.impl.rtp;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface RtpListener {

	/**
	 * Event triggered when an RTP-related failure occurs.
	 * 
	 * @param e
	 *            The exception that originated the failure.
	 */
	public void onRtpFailure(Throwable e);

	/**
	 * Event triggered when an RTP-related failure occurs.
	 * 
	 * @param message
	 *            The reason why the failure occurred
	 */
	public void onRtpFailure(String message);

	/**
	 * Event triggered when an RTCP-related failure occurs.
	 * 
	 * @param e
	 *            The exception that originated the failure.
	 */
	public void onRtcpFailure(Throwable e);

	/**
	 * Event triggered when an RTCP-related failure occurs.
	 * 
	 * @param message
	 *            The reason why the failure occurred
	 */
	public void onRtcpFailure(String e);

}
