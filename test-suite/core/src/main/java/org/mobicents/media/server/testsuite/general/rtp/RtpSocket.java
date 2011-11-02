/**
 * Start time:09:28:08 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
package org.mobicents.media.server.testsuite.general.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

/**
 * Start time:09:28:08 2009-08-03<br>
 * Project: mobicents-media-server-test-suite<br>
 * 
 * @author <a href="mailto:baranowb@gmail.com">Bartosz Baranowski </a>
 */
public interface RtpSocket {

	/**
	 * 
	 */
	void close();

	public SelectionKey getSelectionKey();

	
	public void setPeer(InetAddress address, int port) throws IOException;

	public void addListener(RtpSocketListener listener);

	public void removeListener(RtpSocketListener listener);



	/**
	 * Gets address to which this socked is bound.
	 * 
	 * @return either local address to which this socket is bound
	 */
	public String getLocalAddress();

	/**
	 * Returns port number to which this socked is bound.
	 * 
	 * @return port number or -1 if socket not bound.
	 */
	public int getLocalPort();

	public void notify(Exception e);

	/**
	 * @param rtpPacket
	 */
	void receive(RtpPacket rtpPacket);

	/**
	 * @return
	 */
	DatagramChannel getChannel();
	public boolean isChannelOpen();

	public String getConnectionIdentifier();

	public void setConnectionIdentifier(String connectionIdentifier);
	/**
	 * 
	 */
	void release();

	/**
	 * @param bindAddress
	 * @param i
	 * @param j
	 */
	int init(InetAddress bindAddress, int lowPOrt, int highPort) throws SocketException, IOException;


}
