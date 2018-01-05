/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.ice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.ice.events.IceEventListener;
import org.restcomm.media.ice.events.SelectedCandidatesEvent;
import org.restcomm.media.network.deprecated.TransportAddress;
import org.restcomm.media.network.deprecated.TransportAddress.TransportProtocol;
import org.restcomm.media.network.deprecated.channel.PacketHandler;
import org.restcomm.media.network.deprecated.channel.PacketHandlerException;
import org.restcomm.media.stun.StunException;
import org.restcomm.media.stun.messages.StunMessage;
import org.restcomm.media.stun.messages.StunMessageFactory;
import org.restcomm.media.stun.messages.StunRequest;
import org.restcomm.media.stun.messages.StunResponse;
import org.restcomm.media.stun.messages.attributes.StunAttribute;
import org.restcomm.media.stun.messages.attributes.StunAttributeFactory;
import org.restcomm.media.stun.messages.attributes.general.ErrorCodeAttribute;
import org.restcomm.media.stun.messages.attributes.general.MessageIntegrityAttribute;
import org.restcomm.media.stun.messages.attributes.general.UsernameAttribute;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IceHandler implements PacketHandler {
    
    private static final Logger logger = LogManager.getLogger(IceHandler.class);

    // Packet Handler properties
    private int pipelinePriority;

    // Candidate properties
    private final short componentId;

    // Message Integrity
    private IceAuthenticator authenticator;

    // Handshake state
    private final IceEventListener iceListener;
    private final AtomicBoolean candidateSelected;

    public IceHandler(short componentId, IceEventListener iceListener) {
        // Packet Handler properties
        this.pipelinePriority = 1;

        // Candidate properties
        switch (componentId) {
            case IceComponent.RTP_ID:
            case IceComponent.RTCP_ID:
                this.componentId = componentId;
                break;

            default:
                throw new IllegalArgumentException("Invalid component ID: " + componentId);
        }

        // Handshake state
        this.iceListener = iceListener;
        this.candidateSelected = new AtomicBoolean(false);
    }

    public short getComponentId() {
        return componentId;
    }
    
    public void setAuthenticator(IceAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public int compareTo(PacketHandler o) {
        return (o == null) ? 1 : this.getPipelinePriority() - o.getPipelinePriority();
    }

    /*
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
     * @see <a href="http://tools.ietf.org/html/rfc5389#page-10">RFC5389</a>
     */
    @Override
    public boolean canHandle(byte[] packet) {
        return canHandle(packet, packet.length, 0);
    }

    @Override
    public boolean canHandle(byte[] packet, int dataLength, int offset) {
        byte b0 = packet[offset];
        int b0Int = b0 & 0xff;

        // STUN message must start with 20-byte header followed by zero or more attributes
        // First byte must be less than 2 (https://tools.ietf.org/html/rfc5764#section-5.1.2)
        if (b0Int < 2 && dataLength >= 20) {
            // The most significant 2 bits of every STUN message MUST be zeroes.
            boolean firstBitsValid = ((b0 & 0xC0) == 0);

            // The magic cookie field MUST contain the fixed value 0x2112A442 in network byte order.
            boolean hasMagicCookie = packet[offset + 4] == StunMessage.MAGIC_COOKIE[0]
                    && packet[offset + 5] == StunMessage.MAGIC_COOKIE[1] && packet[offset + 6] == StunMessage.MAGIC_COOKIE[2]
                    && packet[offset + 7] == StunMessage.MAGIC_COOKIE[3];
            return firstBitsValid && hasMagicCookie;
        }
        return false;
    }

    @Override
    public byte[] handle(byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer)
            throws PacketHandlerException {
        return handle(packet, packet.length, 0, localPeer, remotePeer);
    }

    @Override
    public byte[] handle(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer, InetSocketAddress remotePeer)
            throws PacketHandlerException {
        try {
            StunMessage message = StunMessage.decode(packet, (char) offset, (char) dataLength);
            if (message instanceof StunRequest) {
                return processRequest((StunRequest) message, localPeer, remotePeer);
            } else if (message instanceof StunResponse) {
                return processResponse((StunResponse) message);
            } else {
                // TODO STUN Indication is not supported as of yet
                return null;
            }
        } catch (StunException e) {
            throw new PacketHandlerException("Could not decode STUN packet", e);
        } catch (IOException e) {
            throw new PacketHandlerException(e.getMessage(), e);
        }
    }

    private byte[] processRequest(StunRequest request, InetSocketAddress localPeer, InetSocketAddress remotePeer)
            throws IOException, StunException {

        byte[] transactionID = request.getTransactionId();

        // The agent MUST use a short-term credential to authenticate the request and perform a message integrity check.
        UsernameAttribute remoteUnameAttribute = (UsernameAttribute) request.getAttribute(StunAttribute.USERNAME);

        // Send binding error response if username is null
        if(remoteUnameAttribute == null) {
            StunResponse errorResponse = new StunResponse();
            errorResponse.setTransactionID(transactionID);
            errorResponse.setMessageType(StunMessage.BINDING_ERROR_RESPONSE);
            errorResponse.addAttribute(StunAttributeFactory.createErrorCodeAttribute(ErrorCodeAttribute.BAD_REQUEST,
                ErrorCodeAttribute.getDefaultReasonPhrase(ErrorCodeAttribute.BAD_REQUEST)));
            return errorResponse.encode();
        }

        String remoteUsername = new String(remoteUnameAttribute.getUsername());

        // The agent MUST consider the username to be valid if it consists of two values separated by a colon, where the first
        // value is equal to the username fragment generated by the agent in an offer or answer for a session in-progress.
        if (!this.authenticator.validateUsername(remoteUsername)) {
            // TODO return error response
            throw new IOException("Invalid username " + remoteUsername);
        }

        // The username for the credential is formed by concatenating the username fragment provided by the peer with the
        // username fragment of the agent sending the request, separated by a colon (":").
        int colon = remoteUsername.indexOf(":");
        String localUFrag = remoteUsername.substring(0, colon);
        String remoteUfrag = remoteUsername.substring(colon + 1);

        // Produce Binding Response
        TransportAddress transportAddress = new TransportAddress(remotePeer.getAddress(), remotePeer.getPort(), TransportProtocol.UDP);
        StunResponse response = StunMessageFactory.createBindingResponse(request, transportAddress);
        try {
            response.setTransactionID(transactionID);
        } catch (StunException e) {
            throw new IOException("Illegal STUN Transaction ID: " + new String(transactionID), e);
        }

        // Add USERNAME and MESSAGE-INTEGRITY attribute in the response.
        // The responses utilize the same usernames and passwords as the requests.
        String localUsername = remoteUfrag.concat(":").concat(localUFrag);
        StunAttribute unameAttribute = StunAttributeFactory.createUsernameAttribute(localUsername);
        response.addAttribute(unameAttribute);

        byte[] localKey = this.authenticator.getLocalKey(localUFrag);
        MessageIntegrityAttribute integrityAttribute = StunAttributeFactory.createMessageIntegrityAttribute(remoteUsername,
                localKey);
        response.addAttribute(integrityAttribute);

        // If the client issues a USE-CANDIDATE, tell ICE Agent to select the candidate
        if (request.containsAttribute(StunAttribute.USE_CANDIDATE)) {
            if (!this.candidateSelected.get()) {
                this.candidateSelected.set(true);
                if (logger.isDebugEnabled()) {
                    logger.debug("Selected candidate " + remotePeer.toString());
                }
                this.iceListener.onSelectedCandidates(new SelectedCandidatesEvent(remotePeer));
            }
        }

        // Pass response to the server
        return response.encode();
    }

    private byte[] processResponse(StunResponse response) {
        throw new UnsupportedOperationException("Support to handle STUN responses is not implemented.");
    }

    @Override
    public int getPipelinePriority() {
        return this.pipelinePriority;
    }

    public void setPipelinePriority(int pipelinePriority) {
        this.pipelinePriority = pipelinePriority;
    }
    
    public void reset() {
        this.authenticator = null;
        this.candidateSelected.set(false);
    }

}
