package org.mobicents.media.io.ice.network.stun;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.media.io.ice.IceAuthenticator;
import org.mobicents.media.io.ice.network.ProtocolHandler;
import org.mobicents.media.io.stun.StunException;
import org.mobicents.media.io.stun.messages.StunMessage;
import org.mobicents.media.io.stun.messages.StunMessageFactory;
import org.mobicents.media.io.stun.messages.StunRequest;
import org.mobicents.media.io.stun.messages.StunResponse;
import org.mobicents.media.io.stun.messages.attributes.StunAttribute;
import org.mobicents.media.io.stun.messages.attributes.StunAttributeFactory;
import org.mobicents.media.io.stun.messages.attributes.general.MessageIntegrityAttribute;
import org.mobicents.media.io.stun.messages.attributes.general.PriorityAttribute;
import org.mobicents.media.io.stun.messages.attributes.general.UsernameAttribute;
import org.mobicents.media.server.io.network.TransportAddress;
import org.mobicents.media.server.io.network.TransportAddress.TransportProtocol;

/**
 * Handles STUN traffic.
 * 
 * @author Henrique Rosa
 * 
 */
public class StunHandler implements ProtocolHandler {
	
	private static final Logger LOGGER = Logger.getLogger(StunHandler.class);

	private static final String PROTOCOL = "stun";

	private final IceAuthenticator authenticator;
	private final List<StunListener> listeners;

	public StunHandler(IceAuthenticator authenticator) {
		this.authenticator = authenticator;
		this.listeners = new ArrayList<StunListener>();
	}

	public void addListener(StunListener listener) {
		synchronized (this.listeners) {
			if (!this.listeners.contains(listener)) {
				this.listeners.add(listener);
			}
		}
	}
	
	/**
	 * All STUN messages MUST start with a 20-byte header followed by zero or more Attributes.
	 * The STUN header contains a STUN message type, magic cookie, transaction ID, and message length.
	 * 
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |0 0|     STUN Message Type     |         Message Length        |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         Magic Cookie                          |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * |                     Transaction ID (96 bits)                  |
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * 
	 * @param data
	 * @param length
	 * @return
	 * @see <a href="http://tools.ietf.org/html/rfc5389#page-10">RFC5389</a>
	 */
	public boolean canHandle(byte[] data, int length, int offset) {
		/*
		 * All STUN messages MUST start with a 20-byte header followed by zero
		 * or more Attributes.
		 */
		if(length >= 20) {
			// The most significant 2 bits of every STUN message MUST be zeroes.
			byte b0 = data[offset];
			boolean firstBitsValid = ((b0 & 0xC0) == 0);
			
			// The magic cookie field MUST contain the fixed value 0x2112A442 in network byte order.
			boolean hasMagicCookie = data[offset + 4] == StunMessage.MAGIC_COOKIE[0]
					&& data[offset + 5] == StunMessage.MAGIC_COOKIE[1]
					&& data[offset + 6] == StunMessage.MAGIC_COOKIE[2]
					&& data[offset + 7] == StunMessage.MAGIC_COOKIE[3];
			
			return firstBitsValid && hasMagicCookie;
		}
		return false;
	}

	public byte[] process(SelectionKey key, byte[] data, int length) throws IOException {
		// Check whether handler can process the packet.
		// If it cannot, then the packet is dropped.
		if(!canHandle(data, length, 0)) {
			LOGGER.warn("Received message that cannot be handled. Dropped packet.");
			return null;
		}
		
		// Decode and process the packet
		try {
			StunMessage message = StunMessage.decode(data, (char) 0, (char) length);
			if (message instanceof StunRequest) {
				return processRequest((StunRequest) message, key);
			} else if (message instanceof StunResponse) {
				return processResponse((StunResponse) message, key);
			}
			// TODO STUN Indication is not supported as of yet
			return null;
		} catch (StunException e) {
			throw new IOException("Could not decode STUN packet.", e);
		}
	}

	public String getProtocol() {
		return PROTOCOL;
	}

	private byte[] processRequest(StunRequest request, SelectionKey key) throws IOException {
		byte[] transactionID = request.getTransactionId();

		/*
		 * The agent MUST use a short-term credential to authenticate the
		 * request and perform a message integrity check.
		 */
		UsernameAttribute remoteUsernameAttribute = (UsernameAttribute) request
				.getAttribute(StunAttribute.USERNAME);

		String remoteUsername = new String(
				remoteUsernameAttribute.getUsername());
		long priority = extractPriority(request);
		boolean useCandidate = request
				.containsAttribute(StunAttribute.USE_CANDIDATE);
		/*
		 * The agent MUST consider the username to be valid if it consists of
		 * two values separated by a colon, where the first value is equal to
		 * the username fragment generated by the agent in an offer or answer
		 * for a session in-progress.
		 */
		int colon = remoteUsername.indexOf(":");
		String remoteUfrag = remoteUsername.substring(0, colon);
		String localUFrag = null;

		// Produce Binding Response
		DatagramChannel channel = (DatagramChannel) key.channel();
		InetSocketAddress remoteAddress = (InetSocketAddress) channel
				.getRemoteAddress();
		TransportAddress transportAddress = new TransportAddress(
				remoteAddress.getAddress(), remoteAddress.getPort(),
				TransportProtocol.UDP);
		StunResponse response = StunMessageFactory.createBindingResponse(
				request, transportAddress);
		try {
			response.setTransactionID(transactionID);
		} catch (StunException e) {
			throw new IOException("Illegal STUN Transaction ID: "
					+ new String(transactionID), e);
		}
		/*
		 * Add USERNAME and MESSAGE-INTEGRITY attribute in the response. The
		 * responses utilize the same usernames and passwords as the requests
		 */
		StunAttribute usernameAttribute = StunAttributeFactory
				.createUsernameAttribute(remoteUsernameAttribute.getUsername());
		response.addAttribute(usernameAttribute);

		byte[] localKey = this.authenticator.getLocalKey(remoteUsername);
		// String username = new String(uname.getUsername());
		MessageIntegrityAttribute messageIntegrityAttribute = StunAttributeFactory
				.createMessageIntegrityAttribute(new String(
						remoteUsernameAttribute.getUsername()), localKey);
		response.addAttribute(messageIntegrityAttribute);

		// If the client issues a USE-CANDIDATE, tell ICE Agent to select the candidate
		if (useCandidate) {
			fireOnSuccessResponse(key);
		}

		// Pass response to the server
		return response.encode();
	}

	private byte[] processResponse(StunResponse response, SelectionKey key) {
		throw new UnsupportedOperationException("Support to handle STUN responses is not implemented.");
	}

	private long extractPriority(StunRequest request)
			throws IllegalArgumentException {
		// make sure we have a priority attribute and ignore otherwise.
		PriorityAttribute priorityAttr = (PriorityAttribute) request
				.getAttribute(StunAttribute.PRIORITY);
		// extract priority
		if (priorityAttr == null) {
			throw new IllegalArgumentException("Missing PRIORITY attribtue!");
		}
		return priorityAttr.getPriority();
	}

	private void fireOnSuccessResponse(SelectionKey key) {
		StunListener[] copy;
		synchronized (this.listeners) {
			copy = this.listeners.toArray(new StunListener[this.listeners.size()]);
		}

		// Fire the successful binding event
		BindingSuccessEvent event = new BindingSuccessEvent(this, key);
		Notifier notifier = new Notifier(event, copy);
		// TODO Improve by creating a thread pool
//		new Thread(notifier).start();
		for (StunListener listener : copy) {
			listener.onBinding(event);
		}
	}
	
	private class Notifier implements Runnable {
		
		private final BindingSuccessEvent event;
		private final StunListener[] listeners;
		
		public Notifier(final BindingSuccessEvent event, final StunListener[] listeners) {
			this.event = event;
			this.listeners = listeners;
		}

		public void run() {
			for (StunListener listener : this.listeners) {
				listener.onBinding(this.event);
			}
		}
		
	}

}
