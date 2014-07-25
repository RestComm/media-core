package org.mobicents.media.io.ice.harvest;

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

import org.apache.log4j.Logger;
import org.mobicents.media.io.ice.CandidateType;
import org.mobicents.media.io.ice.FoundationsRegistry;
import org.mobicents.media.io.ice.HostCandidate;
import org.mobicents.media.io.ice.IceComponent;
import org.mobicents.media.io.ice.IceMediaStream;
import org.mobicents.media.io.ice.LocalCandidateWrapper;
import org.mobicents.media.server.io.network.PortManager;

/**
 * Harvester that gathers Host candidates, ie transport addresses obtained
 * directly from a local interface.
 * 
 * @author Henrique Rosa
 * 
 */
public class HostCandidateHarvester implements CandidateHarvester {
	
	Logger logger = Logger.getLogger(HostCandidateHarvester.class);

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
	private DatagramChannel openUdpChannel(InetAddress localAddress, int port, Selector selector) throws IOException {
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		// Register selector for reading operations
		channel.register(selector, SelectionKey.OP_READ);
		channel.bind(new InetSocketAddress(localAddress, port));
		return channel;
	}
	
	public void harvest(PortManager portManager, IceMediaStream mediaStream, Selector selector) throws HarvestException {
		// Find available addresses
		List<InetAddress> addresses = findAddresses();

		// Gather a candidate for each available address
		for (InetAddress address : addresses) {
			// Gather candidate for RTP component
			IceComponent rtpComponent = mediaStream.getRtpComponent();
			boolean gathered = gatherCandidate(rtpComponent, address, portManager.next(), portManager, selector);
			
			if(!gathered) {
				logCandidateNotFound(address.toString(), portManager.getLowestPort(), portManager.getHighestPort());
			}
			
			// Gather candidate for RTCP component IF supported
			if (gathered && mediaStream.supportsRtcp()) {
				// RTCP traffic will be bound to next logical port
				IceComponent rtcpComponent = mediaStream.getRtcpComponent();
				gathered = gatherCandidate(rtcpComponent, address, portManager.next(), portManager, selector);
				
				if(!gathered) {
					logCandidateNotFound(address.toString(), portManager.getLowestPort(), portManager.getHighestPort());
				}
			}
		}
	}
	
	private void logCandidateNotFound(String address, int lowPort, int highPort) {
		this.logger
				.warn(String
						.format("Could not find RTP candidate for address %s between ports %d and %d",
								address, lowPort, highPort));
	}
	
	/**
	 * Gathers a candidate and registers it in the respective ICE Component. A
	 * datagram channel will be bound to the local candidate address.
	 * 
	 * @param component
	 *            The component the candidate belongs to
	 * @param address
	 *            The address of the candidate
	 * @param startingPort
	 *            The preferred port for the candidate to use.<br>
	 *            The candidate port will change if the preferred port is
	 *            already taken.
	 * @param portManager
	 *            The port manager that keeps track of port range that can be
	 *            used for candidate gathering.
	 * @param selector
	 *            The selector to bind the candidate address to.
	 * @return Whether a candidate was successfully gathered. The portManager
	 *         will keep track of the effective port.
	 */
	private boolean gatherCandidate(IceComponent component, InetAddress address, int startingPort, PortManager portManager, Selector selector) {
		// Recursion stop criteria
		if(startingPort == portManager.peek()) {
			return false;
		}
		
		// Gather the candidate using current port
		try {
			int port = portManager.current();
			DatagramChannel channel = openUdpChannel(address, port, selector);
			HostCandidate candidate = new HostCandidate(component, address, port);
			this.foundations.assignFoundation(candidate);
			component.addLocalCandidate(new LocalCandidateWrapper(candidate, channel));
			return true;
		} catch (IOException e) {
			// The port is occupied. Try again with next logical port.
			portManager.next();
			return gatherCandidate(component, address, startingPort, portManager, selector);
		}
	}

	public CandidateType getCandidateType() {
		return CandidateType.HOST;
	}

}
