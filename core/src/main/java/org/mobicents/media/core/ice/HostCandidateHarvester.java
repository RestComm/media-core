package org.mobicents.media.core.ice;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.mobicents.media.core.ice.candidate.LocalCandidateWrapper;
import org.mobicents.media.core.ice.harvest.CandidateHarvester;
import org.mobicents.media.core.ice.harvest.HarvestingException;
import org.mobicents.media.core.ice.harvest.NoCandidateBoundException;

/**
 * Harvester that gathers Host candidates, ie transport addresses obtained
 * directly from a local interface.
 * 
 * @author Henrique Rosa
 * 
 */
public class HostCandidateHarvester implements CandidateHarvester {

	/**
	 * Finds all Network interfaces available on this server.
	 * 
	 * @return The list of available network interfaces.
	 * @throws HarvestingException
	 *             When an error occurs while retrieving the network interfaces
	 */
	private Enumeration<NetworkInterface> getNetworkInterfaces()
			throws HarvestingException {
		try {
			return NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			throw new HarvestingException(
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
	 * @throws HarvestingException
	 *             When an error occurs while inspecting the interface.
	 */
	private boolean useNetworkInterface(NetworkInterface networkInterface)
			throws HarvestingException {
		try {
			return networkInterface.isLoopback() || !networkInterface.isUp();
		} catch (SocketException e) {
			throw new HarvestingException(
					"Could not evaluate whether network interface is loopback.",
					e);
		}
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
	private DatagramChannel bindCandidate(InetAddress localAddress, int port)
			throws IOException {
		// TODO Implement lookup mechanism for a range of ports
		DatagramChannel channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.bind(new InetSocketAddress(localAddress, port));
		return channel;
	}

	public List<LocalCandidateWrapper> harvest(int port, FoundationsRegistry foundationsRegistry)
			throws HarvestingException, NoCandidateBoundException {
		List<LocalCandidateWrapper> candidates = new ArrayList<LocalCandidateWrapper>();

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

				DatagramChannel udpChannel;
				if (address instanceof Inet4Address) {
					try {
						udpChannel = bindCandidate(address, port);
					} catch (IOException e) {
						continue;
					}
				} else {
					// Only IPv4 addresses are supported thus far
					continue;
				}

				// Wrap the candidate and the datagram channel and register it
				// as a local component
				HostCandidate candidate = new HostCandidate(address, port);
				candidate.setVirtual(iface.isVirtual());
				foundationsRegistry.assignFoundation(candidate);
				candidates
						.add(new LocalCandidateWrapper(candidate, udpChannel));
			}
		}
		// ICE spec mandates that at least one candidate must be bound
		if (candidates.isEmpty()) {
			throw new NoCandidateBoundException(
					"The harvesting process finished but no available candidates were found.");
		}
		return candidates;
	}

}
