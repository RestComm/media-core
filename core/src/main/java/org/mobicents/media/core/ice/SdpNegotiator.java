package org.mobicents.media.core.ice;

import java.util.StringTokenizer;
import java.util.Vector;

import javax.sdp.Attribute;
import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.Candidate;
import org.ice4j.ice.CandidateType;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.RemoteCandidate;
import org.ice4j.ice.sdp.CandidateAttribute;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class SdpNegotiator {

	public void negotiateSdp(Agent localAgent, String sdp) throws SdpException {
		SdpFactory factory = SdpFactory.getInstance();
		SessionDescription sessionDescription = factory
				.createSessionDescription(sdp);

		for (IceMediaStream mediaStream : localAgent.getStreams()) {
			mediaStream.setRemotePassword(sessionDescription
					.getAttribute("ice-pwd"));
			mediaStream.setRemoteUfrag(sessionDescription
					.getAttribute("ice-ufrag"));
		}

		Connection globalConn = sessionDescription.getConnection();
		String globalConnectionAddress = null;
		if (globalConn != null) {
			globalConnectionAddress = globalConn.getAddress();
		}

		Vector<MediaDescription> mediaDescriptions = sessionDescription.getMediaDescriptions(true);
		for (MediaDescription mediaDescription : mediaDescriptions) {
			processMediaDescription(globalConnectionAddress, mediaDescription, localAgent);
		}

	}

	private void processMediaDescription(String connectionAddress, MediaDescription mediaDescription, Agent localAgent) throws SdpParseException {
		String mediaType = mediaDescription.getMedia().getMediaType();
		IceMediaStream stream = localAgent.getStream(mediaType);

		if (stream == null) {
			return;
		}

		Vector<Attribute> attributes = mediaDescription.getAttributes(true);
		for (Attribute attribute : attributes) {
			if (attribute.getName().equals(CandidateAttribute.NAME)) {
				parseCandidate(attribute, stream);
			}
		}

		// set default candidates
		Connection streamConn = mediaDescription.getConnection();
		String streamConnAddr = null;
		if (streamConn != null) {
			streamConnAddr = streamConn.getAddress();
		} else {
			streamConnAddr = connectionAddress;
		}

		int port = mediaDescription.getMedia().getMediaPort();
		TransportAddress defaultRtpAddress = new TransportAddress(
				streamConnAddr, port, Transport.UDP);

		int rtcpPort = port + 1;
		String rtcpAttributeValue = mediaDescription.getAttribute("rtcp");

		if (rtcpAttributeValue != null) {
			rtcpPort = Integer.parseInt(rtcpAttributeValue);
		}

		TransportAddress defaultRtcpAddress = new TransportAddress(
				streamConnAddr, rtcpPort, Transport.UDP);

		Component rtpComponent = stream.getComponent(Component.RTP);
		Component rtcpComponent = stream.getComponent(Component.RTCP);

		Candidate defaultRtpCandidate = rtpComponent
				.findRemoteCandidate(defaultRtpAddress);
		rtpComponent.setDefaultRemoteCandidate(defaultRtpCandidate);

		if (rtcpComponent != null) {
			Candidate defaultRtcpCandidate = rtcpComponent
					.findRemoteCandidate(defaultRtcpAddress);
			rtcpComponent.setDefaultRemoteCandidate(defaultRtcpCandidate);
		}
	}

	private RemoteCandidate parseCandidate(Attribute attribute, IceMediaStream stream) {
		String value = null;
		try {
			value = attribute.getValue();
		} catch (Throwable t) {
			// can't happen
		}

        StringTokenizer tokenizer = new StringTokenizer(value);

        //TODO add exception handling - hrosa
        String foundation = tokenizer.nextToken();
        int componentID = Integer.parseInt(tokenizer.nextToken());
        Transport transport = Transport.parse(tokenizer.nextToken());
        long priority = Long.parseLong(tokenizer.nextToken());
        String address = tokenizer.nextToken();
        int port = Integer.parseInt(tokenizer.nextToken());

        TransportAddress transportAddress = new TransportAddress(address, port, transport);

		// skip the "typ" String
        tokenizer.nextToken();
        CandidateType candidateType = CandidateType.parse(tokenizer.nextToken());

        Component component = stream.getComponent(componentID);
        if(component == null) {
        	return null;
        }

		// check if there's a related address property
		RemoteCandidate relatedCandidate = null;
		if (tokenizer.countTokens() >= 4) {
			tokenizer.nextToken(); // skip the raddr element
			String relatedAddress = tokenizer.nextToken();
			tokenizer.nextToken(); // skip the rport element
			int relatedPort = Integer.parseInt(tokenizer.nextToken());

			TransportAddress raddr = new TransportAddress(relatedAddress, relatedPort, Transport.UDP);
			relatedCandidate = component.findRemoteCandidate(raddr);
		}

        RemoteCandidate remoteCandidate = new RemoteCandidate(transportAddress, component, candidateType, foundation, priority, relatedCandidate);
        component.addRemoteCandidate(remoteCandidate);
        return remoteCandidate;
	}

}
