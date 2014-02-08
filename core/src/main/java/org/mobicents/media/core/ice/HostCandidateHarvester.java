package org.mobicents.media.core.ice;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.mobicents.media.core.ice.harvest.CandidateHarvester;
import org.mobicents.media.core.ice.harvest.HarvestException;
import org.mobicents.media.core.ice.network.stun.StunHandler;

/**
 * Harvester that gathers Host candidates, ie transport addresses obtained
 * directly from a local interface.
 * 
 * @author Henrique Rosa
 * 
 */
public class HostCandidateHarvester implements CandidateHarvester {

	private final FoundationsRegistry foundations;

	public HostCandidateHarvester(FoundationsRegistry foundationsRegistry) {
		super();
		this.foundations = foundationsRegistry;
	}

	/**
	 * Finds all Network interfaces available on this server.
	 * 
	 * @return The list of available network interfaces.
	 * @throws HarvestException
	 *             When an error occurs while retrieving the network interfaces
	 */
	private Enumeration<NetworkInterface> getNetworkInterfaces()
			throws HarvestException {
		try {
			return NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			throw new HarvestException(
					"Could not retrieve list of available Network Interfaces.",
					e);
		}
	}

	/**
	 * Decides whether a certain network interface can be used as a host
	 * candidate.
	 * 
	 * @param networkInterface
	 *            The network interface to evaluate
	 * @return <code>true</code> if the interface can be used. Returns
	 *         <code>false</code>, otherwise.
	 * @throws HarvestException
	 *             When an error occurs while inspecting the interface.
	 */
	private boolean useNetworkInterface(NetworkInterface networkInterface)
			throws HarvestException {
		try {
			return !networkInterface.isLoopback() && networkInterface.isUp();
		} catch (SocketException e) {
			throw new HarvestException(
					"Could not evaluate whether network interface is loopback.",
					e);
		}
	}

	/**
	 * Finds available addresses that will be used to gather candidates from.
	 * 
	 * @return A list of collected addresses.
	 * @throws HarvestException
	 *             If an error occurs while searching for available addresses
	 */
	private List<InetAddress> findAddresses() throws HarvestException {
		// Stores found addresses
		List<InetAddress> found = new ArrayList<InetAddress>(3);

		// Retrieve list of available network interfaces
		Enumeration<NetworkInterface> interfaces = getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface iface = interfaces.nextElement();

			// Evaluate network interface
			if (!useNetworkInterface(iface)) {
				continue;
			}

			// Retrieve list of available addresses from the network interface
			Enumeration<InetAddress> addresses = iface.getInetAddresses();

			while (addresses.hasMoreElements()) {
				InetAddress address = addresses.nextElement();

				// loopback addresses are discarded
				if (address.isLoopbackAddress()) {
					continue;
				}

				// Ignore IPv6 addresses for now
				if (address instanceof Inet4Address) {
					found.add(address);
				}
			}
		}
		return found;
	}

	/**
	 * Opens a datagram channel and binds it to an address.
	 * 
	 * @param localAddress
	 *            The address to bind the channel to.
	 * @param port
	 *            The port to use
	 * @return The bound datagram channel
	 * @throws IOException
	 *             When an error occurs while binding the datagram channel.
	 */
	private DatagramChannel openUdpChannel(InetAddress localAddress, int port,
			Selector selector) throws IOException {
		// TODO Implement lookup mechanism for a range of ports
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		// Register selector for reading operations
		SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
		// Attach a STUN handler to the selection key
		StunHandler stunHandler = new StunHandler(key);
		key.attach(stunHandler);
		channel.bind(new InetSocketAddress(localAddress, port));
		return channel;
	}

	public void harvest(int preferredPort, IceMediaStream mediaStream,
			Selector selector) throws HarvestException {
		// Find available addresses
		List<InetAddress> addresses = findAddresses();

		// Gather a candidate for each available address
		for (InetAddress address : addresses) {
			// Gather candidate for RTP component
			IceComponent rtpComponent = mediaStream.getRtpComponent();
			int rtpPort = gatherCandidate(rtpComponent, address, preferredPort,
					selector);

			// Gather candidate for RTCP component IF supported
			// RTCP traffic will be bound to next logical port
			if (rtpPort > 0 && mediaStream.supportsRtcp()) {
				// FIXME rtcp port should be next 'logical' port - hrosa
				int rtcpPort = rtpPort + 1;
				IceComponent rtcpComponent = mediaStream.getRtcpComponent();
				rtcpPort = gatherCandidate(rtcpComponent, address, rtcpPort,
						selector);
			}
		}
	}

	/**
	 * Gathers a candidate and registers it in the respective ICE Component. A
	 * datagram channel will be bound to the local candidate address.
	 * 
	 * @param component
	 *            The component the candidate belongs to
	 * @param address
	 *            The address of the candidate
	 * @param port
	 *            The preferred port for the candidate to use.<br>
	 *            The candidate port will change if the preferred port is
	 *            already taken.
	 * @return The effective port of the gathered candidate.<br>
	 *         Returns 0 if gathering failed.
	 */
	private int gatherCandidate(IceComponent component, InetAddress address,
			int port, Selector selector) {
		try {
			DatagramChannel channel = openUdpChannel(address, port, selector);
			HostCandidate candidate = new HostCandidate(component, address,
					port);
			this.foundations.assignFoundation(candidate);
			component.addLocalCandidate(new LocalCandidateWrapper(candidate,
					channel));
			return port;
		} catch (IOException e) {
			// TODO retry next logical available port
			return 0;
		}
	}

}
