/**
 * Start time:09:34:34 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
package org.mobicents.media.server.testsuite.general.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;

import javax.sdp.Attribute;

import org.mobicents.media.server.testsuite.general.Timer;

/**
 * Start time:09:34:34 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * This interface defines RtpSocketFacctory - similar can be found in mms,
 * however tool should be independent so we cant reuse mms components.
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
public interface RtpSocketFactory {

	public RtpSocket createSocket() throws SocketException, IOException;

	public void releaseSocket(RtpSocket socket);

	/**
	 * Terminates execution of created sockets, this is hard stop method, which
	 * will propably break calls.
	 */
	public void stop();

	public void start() throws SocketException, IOException;


	/**
	 * Gets the IP address to which trunk is bound. All endpoints of the trunk
	 * use this address for RTP connection.
	 * 
	 * @return the IP address string to which this trunk is bound.
	 */
	public String getBindAddress();

	/**
	 * Modify the bind address. All endpoints of the trunk use this address for
	 * RTP connection.
	 * 
	 * @param bindAddress
	 *            IP address as string or host name.
	 */
	public void setBindAddress(String bindAddress) throws UnknownHostException;

	/**
	 * Gets the available port range.
	 * 
	 * @return the string in format "lowPort-highPort".
	 */
	public String getPortRange();

	
	public int getLowPort();
	public int getHighPort();
	/**
	 * Modify port used to create RTP stream.
	 * 
	 * @param portRange
	 *            the string in format "lowPort-highPort"
	 */
	public void setPortRange(String portRange);

	// FIXME: should we reuse org.mobicents.media.Format?

	public Vector<Attribute> getFormatMap();

	public void setFormatMap(Vector<Attribute> originalFormatMap);
}
