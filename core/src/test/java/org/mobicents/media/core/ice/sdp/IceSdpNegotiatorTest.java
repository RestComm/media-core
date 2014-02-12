package org.mobicents.media.core.ice.sdp;

import static org.junit.Assert.*;
import gov.nist.core.Separators;
import gov.nist.javax.sdp.fields.AttributeField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import org.junit.Test;
import org.mobicents.media.core.ice.IceAgent;
import org.mobicents.media.core.ice.IceCandidate;
import org.mobicents.media.core.ice.IceMediaStream;
import org.mobicents.media.core.ice.harvest.HarvestException;
import org.mobicents.media.core.ice.harvest.NoCandidatesGatheredException;
import org.mobicents.media.core.ice.lite.IceLiteAgent;
import org.mobicents.media.core.ice.sdp.attributes.CandidateAttribute;
import org.mobicents.media.core.ice.sdp.attributes.IceLiteAttribute;
import org.mobicents.media.core.ice.sdp.attributes.IcePwdAttribute;
import org.mobicents.media.core.ice.sdp.attributes.IceUfragAttribute;
import org.mobicents.media.core.ice.sdp.attributes.RtcpAttribute;

public class IceSdpNegotiatorTest {

	private String readSdpFile(String filename) throws IOException {
		InputStream inputStream = this.getClass().getResourceAsStream(filename);
		InputStreamReader inputReader = new InputStreamReader(inputStream);
		BufferedReader reader = new BufferedReader(inputReader);

		StringBuilder builder = new StringBuilder();
		String line = reader.readLine();
		while (line != null) {
			builder.append(line).append(Separators.NEWLINE);
			line = reader.readLine();
		}
		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAnswerNegotiation() throws IOException, SdpException,
			NoCandidatesGatheredException, HarvestException {
		// given
		IceSdpNegotiator negotiator = new IceSdpNegotiator();
		String sdpAnswer = readSdpFile("sdp_vanilla_answer.txt");

		IceAgent agent = new IceLiteAgent();
		agent.addMediaStream("audio", true);
		agent.gatherCandidates(61000);

		// when
		SessionDescription sdp = negotiator.updateAnswer(sdpAnswer, agent);

		// then
		// Verify ice-ufrag and ice-pwd attributes exist
		assertNotNull(sdp.getAttribute(IceUfragAttribute.NAME));
		assertNotNull(sdp.getAttribute(IcePwdAttribute.NAME));

		// If the ICE agent is lite, sdp must include a 'a=ice-lite'
		// attribute
		if (agent.isLite()) {
			Vector<AttributeField> attributes = sdp.getAttributes(true);
			assertTrue(attributeExists(IceLiteAttribute.NAME, attributes));
		}

		Vector<MediaDescription> mediaStreams = sdp.getMediaDescriptions(false);
		for (MediaDescription mediaStream : mediaStreams) {
			if (mediaStream.getMedia().getMediaType().equals("audio")) {
				// Number of candidates must match
				IceMediaStream iceStream = agent.getMediaStream("audio");
				assertEquals(countStreamCandidates(iceStream),
						countSdpCandidates(mediaStream));

				// Media connection must match address and port of default local
				// RTP candidate
				IceCandidate defaultRtpCandidate = iceStream.getRtpComponent()
						.getDefaultLocalCandidate().getCandidate();
				assertEquals(mediaStream.getConnection().getAddress(),
						defaultRtpCandidate.getAddress().getHostAddress());
				assertEquals(mediaStream.getMedia().getMediaPort(),
						defaultRtpCandidate.getPort());

				/*
				 * If the agent is using RTCP, it must encode the RTCP candidate
				 * using the 'a=rtcp' attribute.
				 */
				if (iceStream.supportsRtcp()) {
					String rtcpAttribute = mediaStream
							.getAttribute(RtcpAttribute.NAME);
					assertNotNull(rtcpAttribute);
				}
			}
		}
	}

	private int countStreamCandidates(IceMediaStream mediaStream) {
		int count = mediaStream.getRtpComponent().getLocalCandidates().size();
		if (mediaStream.supportsRtcp()) {
			count += mediaStream.getRtcpComponent().getLocalCandidates().size();
		}
		return count;
	}

	@SuppressWarnings("unchecked")
	private int countSdpCandidates(MediaDescription mediaDescription)
			throws SdpParseException {
		int count = 0;
		Vector<AttributeField> attributes = mediaDescription
				.getAttributes(true);
		for (AttributeField attribute : attributes) {
			if (attribute.getName().equals(CandidateAttribute.NAME)) {
				count++;
			}
		}
		return count;
	}

	private boolean attributeExists(String name,
			Vector<AttributeField> attributes) throws SdpParseException {
		for (AttributeField attribute : attributes) {
			if (attribute.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

}
