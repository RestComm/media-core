/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.network.deprecated.channel;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * Represents a channel where data can be exchanged
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @deprecated Use {@link NetworkChannel} instead.
 */
@Deprecated
public interface Channel {

	/**
	 * Gets the address the channel is bound to.
	 * 
	 * @return Returns the local address of the channel. Returns an empty string
	 *         if the channel is not currently bound.
	 */
	String getLocalHost();

	/**
	 * Gets the port that the channel listens to.
	 * 
	 * @return The number of the local port. Returns zero if the channel is not
	 *         currently bound.
	 */
	int getLocalPort();
	
	SocketAddress getLocalAddress();

	/**
	 * Receive data through the channel
	 * 
	 * @throws IOException
	 */
	void receive() throws IOException;

	/**
	 * Send data through the channel
	 * 
	 * @throws IOException
	 */
	void send() throws IOException;

	/**
	 * Gets whether the channel has pending data to be written
	 * 
	 * @return whether the channel has pending data
	 */
	boolean hasPendingData();

	/**
	 * Gets whether channel is connected or not.
	 * 
	 * @return <code>true</code> if the channel is connected. Otherwise, returns
	 *         <code>false</code>.
	 */
	boolean isConnected();

	/**
	 * Gets whether channel is open or not.
	 * 
	 * @return <code>true</code> if the channel is open. Otherwise, returns
	 *         <code>false</code>.
	 */
	boolean isOpen();

	/**
	 * Binds the channel to an address.
	 * 
	 * @param address
	 *            the address the channel will be bounds to
	 * @throws IOException
	 *             In case the channel is closed or could not be bound.
	 */
	void bind(SocketAddress address) throws IOException;

	/**
	 * Connects the channel to a remote peer.<br>
	 * The channel will only be able to accept traffic from that peer.
	 * 
	 * @param address
	 *            The address of the remote peer to connect to
	 * @throws IOException
	 */
	void connect(SocketAddress address) throws IOException;

	/**
	 * Disconnect the channel
	 * 
	 * @throws IOException
	 */
	void disconnect() throws IOException;

	/**
	 * Opens the channel.
	 * 
	 * @throws IOException
	 *             If the channel is already open or if it could not be opened.
	 */
	void open() throws IOException;
	
	/**
	 * Opens the channel with a pre-defined data channel.
	 * 
	 * @param transport the underlying data channel where the traffic will flow
	 * 
	 * @throws IOException In case the channel is already open or the data channel is invalid.
	 */
	void open(DatagramChannel dataChannel) throws IOException;
	
	/**
	 * Disconnects and closes the channel.<br>
	 * Invoking this method will have no effect if the channel is already
	 * closed.
	 */
	void close();

}
