package org.mobicents.media.core.ice.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import org.mobicents.media.core.ice.IceAgent;
import org.mobicents.media.core.ice.IceMediaStream;
import org.mobicents.media.core.ice.network.stun.StunChannel;
import org.mobicents.media.core.ice.network.stun.StunHandler;
import org.mobicents.media.core.ice.security.IceAuthenticator;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class ConnectivityCheckServer implements IceAuthenticator {

	private IceAgent agent;
	private StunChannel stunChannel;
	private Selector selector;

	private boolean running;

	public ConnectivityCheckServer(IceAgent agent, Selector selector) {
		this.agent = agent;
		this.selector = selector;
		this.running = false;
	}

	public byte[] getLocalKey(String ufrag) {
		if (isUserRegistered(ufrag)) {
			return agent.getPassword().getBytes();
		}
		return null;
	}

	public byte[] getRemoteKey(String ufrag, String media) {
		// TODO add reference to remote candidate
		IceMediaStream stream = this.agent.getMediaStream(media);
		if (stream != null) {
			if (ufrag.equals(stream.getRemoteUfrag())) {
				return stream.getRemotePassword().getBytes();
			}
		}
		return null;
	}

	public boolean isUserRegistered(String ufrag) {
		return ufrag.equals(agent.getUfrag());
	}
	
	public void read(SelectionKey key) throws IOException {
		DatagramChannel channel = (DatagramChannel) key.channel();
		StunHandler handler = (StunHandler) key.attachment();
		handler.receive(channel);
	}

	public void start() {
		this.running = true;
		while (true) {
			try {
				// Wait for an event on one of the registered channels
				this.selector.select();

				// Iterate over the set of keys for which events are available
				Iterator<SelectionKey> keys = this.selector.keys().iterator();
				while (keys.hasNext()) {
					SelectionKey key = keys.next();
					keys.remove();

					if (key.isValid() && key.isReadable()) {
						
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		this.running = false;
	}

}
