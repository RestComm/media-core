/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.protocols.mgcp.stack.test.parser;

import jain.protocol.ip.mgcp.message.parms.Bandwidth;
import jain.protocol.ip.mgcp.message.parms.BearerInformation;
import jain.protocol.ip.mgcp.message.parms.CapabilityValue;
import jain.protocol.ip.mgcp.message.parms.CompressionAlgorithm;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.DigitMap;
import jain.protocol.ip.mgcp.message.parms.EchoCancellation;
import jain.protocol.ip.mgcp.message.parms.EmbeddedRequest;
import jain.protocol.ip.mgcp.message.parms.EncryptionMethod;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.GainControl;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.LocalOptionValue;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.PacketizationPeriod;
import jain.protocol.ip.mgcp.message.parms.ReasonCode;
import jain.protocol.ip.mgcp.message.parms.RegularConnectionParm;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.ResourceReservation;
import jain.protocol.ip.mgcp.message.parms.RestartMethod;
import jain.protocol.ip.mgcp.message.parms.SilenceSuppression;
import jain.protocol.ip.mgcp.message.parms.TypeOfNetwork;
import jain.protocol.ip.mgcp.message.parms.TypeOfService;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import java.text.ParseException;

import org.mobicents.protocols.mgcp.jain.pkg.AUMgcpEvent;
import org.mobicents.protocols.mgcp.jain.pkg.AUPackage;
import org.mobicents.protocols.mgcp.parser.Utils;
import org.mobicents.protocols.mgcp.parser.UtilsFactory;
import org.mobicents.protocols.mgcp.stack.test.TestHarness;

public class ParserTest extends TestHarness {
	Utils parser = null;
	UtilsFactory factory = null;

	public ParserTest() {
		super("ParserTest");
	}

	@Override
	public void setUp() {
		factory = new UtilsFactory(2);
		parser = factory.allocate();

	}

	@Override
	public void tearDown() {

	}

	public void testDecodeEncodeCapabilityList() throws ParseException {
		String capability = "a:PCMU;G729,p:10-100,e:on,s:off,v:L;S,m:sendonly;recvonly;sendrecv;inactive;netwloop;netwtest";
		CapabilityValue[] capabilities = parser.decodeCapabilityList(capability);

		assertNotNull(capabilities);
		assertEquals(6, capabilities.length);

		String encodedCapability = parser.encodeCapabilityList(capabilities);

		assertEquals(capability, encodedCapability);

		// Test 2
//		capability = "a:PCMU;G728, p:10-100, e:on, s:off, t:1, v:L, m:sendonly;recvonly;sendrecv;inactive, a:G729, p:30-90, e:on, s:on, t:1, v:L, m:sendonly;recvonly;sendrecv;inactive;confrnce";
//		capabilities = parser.decodeCapabilityList(capability);
	}

	public void testDecodeEncodeInfoCodeList() throws ParseException {
		String requestedInfo = "R,D,S,X,N,I,T,O,ES";
		InfoCode[] infoCodeList = parser.decodeInfoCodeList(requestedInfo);

		assertNotNull(infoCodeList);
		assertEquals(9, infoCodeList.length);

		String encodedInfoCodeList = parser.encodeInfoCodeList(infoCodeList);
		assertEquals(requestedInfo, encodedInfoCodeList);
	}

	public void testDecodeConnectionMode() {
		String connectionMode = "conttest";

		ConnectionMode cMode = parser.decodeConnectionMode(connectionMode);
		assertNotNull(cMode);
		assertEquals(connectionMode, cMode.toString());
	}

	public void testDecodeEncodeBearerInformation() throws ParseException {
		String text = "e:mu";
		BearerInformation bearerInformation = parser.decodeBearerInformation(text);
		assertNotNull(bearerInformation);
		assertEquals("mu", bearerInformation.toString());

		String encodedText = parser.encodeBearerInformation(bearerInformation);

		assertEquals(text, encodedText);
	}

	public void testDecodeLocalOptionValueList() throws ParseException {
		String text = "p:10,a:PCMU";
		LocalOptionValue[] localOptionValueList = parser.decodeLocalOptionValueList(text);
		assertNotNull(localOptionValueList);
		assertEquals(2, localOptionValueList.length);

		String encodedText = parser.encodeLocalOptionValueList(localOptionValueList);
		assertEquals(text, encodedText);

		text = "a:G729;PCMU,p:30-90,e:on,s:on,customkey:customevalue";
		localOptionValueList = parser.decodeLocalOptionValueList(text);
		assertEquals(5, localOptionValueList.length);

		encodedText = parser.encodeLocalOptionValueList(localOptionValueList);
		assertEquals(text, encodedText);

	}

	public void testDecodeEncodeBandwidth() throws ParseException {
		String text = "64";
		Bandwidth bandwidth = parser.decodeBandwidth(text);
		assertNotNull(bandwidth);
		assertEquals(64, bandwidth.getBandwidthLowerBound());
		assertEquals(64, bandwidth.getBandwidthUpperBound());

		String encodedText = parser.encodeBandwidth(bandwidth);

		assertEquals(text, encodedText);

		text = "64-128";
		bandwidth = parser.decodeBandwidth(text);
		assertNotNull(bandwidth);
		assertEquals(64, bandwidth.getBandwidthLowerBound());
		assertEquals(128, bandwidth.getBandwidthUpperBound());

		encodedText = parser.encodeBandwidth(bandwidth);

		assertEquals(text, encodedText);

	}

	public void testDecodeEncodeCompressionAlgorithm() throws ParseException {
		String text = "PCMU;G726";
		CompressionAlgorithm compressionAlgorithm = parser.decodeCompressionAlgorithm(text);
		assertNotNull(compressionAlgorithm);
		assertEquals(2, compressionAlgorithm.getCompressionAlgorithmNames().length);

		String encodedText = parser.encodeCompressionAlgorithm(compressionAlgorithm);

		assertEquals(text, encodedText);

	}

	public void testDecodeEncodeEchoCancellation() throws ParseException {
		String text = "on";
		EchoCancellation echoCancellation = parser.decodeEchoCancellation(text);
		assertNotNull(echoCancellation);
		assertTrue(echoCancellation.getEchoCancellation());

		String encodedText = parser.encodeEchoCancellation(echoCancellation);

		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeEncryptionMethod() throws ParseException {
		String text = "base64:somekey";
		EncryptionMethod encryptionMethod = parser.decodeEncryptionMethod(text);
		assertNotNull(encryptionMethod);
		assertEquals(2, encryptionMethod.getEncryptionMethod());
		assertEquals("somekey", encryptionMethod.getEncryptionKey());

		String encodedText = parser.encodeEncryptionMethod(encryptionMethod);

		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeGainControl() throws ParseException {
		String text = "auto";
		GainControl gainControl = parser.decodeGainControl(text);
		assertNotNull(gainControl);
		assertTrue(gainControl.getGainControlAuto());

		String encodedText = parser.encodeGainControl(gainControl);

		assertEquals(text, encodedText);

		text = "101";
		gainControl = parser.decodeGainControl(text);
		assertEquals(101, gainControl.getGainControl());
		encodedText = parser.encodeGainControl(gainControl);
		assertEquals(text, encodedText);

	}

	public void testDecodeEncodePacketizationPeriod() throws ParseException {
		String text = "20";
		PacketizationPeriod packetizationPeriod = parser.decodePacketizationPeriod(text);
		assertNotNull(packetizationPeriod);
		assertEquals(20, packetizationPeriod.getPacketizationPeriodLowerBound());
		assertEquals(packetizationPeriod.getPacketizationPeriodLowerBound(), packetizationPeriod
				.getPacketizationPeriodUpperBound());

		String encodedText = parser.encodePacketizationPeriod(packetizationPeriod);
		assertEquals(text, encodedText);

		text = "20-25";
		packetizationPeriod = parser.decodePacketizationPeriod(text);
		assertEquals(20, packetizationPeriod.getPacketizationPeriodLowerBound());
		assertEquals(25, packetizationPeriod.getPacketizationPeriodUpperBound());
		encodedText = parser.encodePacketizationPeriod(packetizationPeriod);
		assertEquals(text, encodedText);

	}

	public void testDecodeEncodeResourceReservation() throws ParseException {
		String text = "be";
		ResourceReservation resourceReservation = parser.decodeResourceReservation(text);
		assertNotNull(resourceReservation);
		assertEquals(ResourceReservation.BEST_EFFORT, resourceReservation.getResourceReservation());

		String encodedText = parser.encodeResourceReservation(resourceReservation);

		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeSilenceSuppression() throws ParseException {
		String text = "off";
		SilenceSuppression silenceSuppression = parser.decodeSilenceSuppression(text);
		assertNotNull(silenceSuppression);
		assertEquals(false, silenceSuppression.getSilenceSuppression());

		String encodedText = parser.encodeSilenceSuppression(silenceSuppression);
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeTypeOfNetwork() throws ParseException {
		String text = "atm";
		TypeOfNetwork typeOfNetwork = parser.decodeTypeOfNetwork(text);
		assertNotNull(typeOfNetwork);
		assertEquals(TypeOfNetwork.ATM, typeOfNetwork.getTypeOfNetwork());

		String encodedText = parser.encodeTypeOfNetwork(typeOfNetwork);

		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeTypeOfService() throws ParseException {
		String text = "5";
		TypeOfService typeOfService = parser.decodeTypeOfService(text);
		assertNotNull(typeOfService);
		assertEquals(5, typeOfService.getTypeOfService());

		String encodedText = parser.encodeTypeOfService(typeOfService);
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeRestartMethod() throws ParseException {
		String text = "disconnected";
		RestartMethod restartMethod = parser.decodeRestartMethod(text);
		assertNotNull(restartMethod);
		assertEquals(RestartMethod.DISCONNECTED, restartMethod.getRestartMethod());

		String encodedText = restartMethod.toString();

		assertEquals(text, encodedText);

	}

	public void testDecodeEncodeEventName() throws ParseException {
		String text = "L/rg@112A9FF";
		String param = "to=6000";
		EventName eventName = parser.decodeEventName(text, param);

		assertNotNull(eventName);
		PackageName packageName = eventName.getPackageName();
		assertEquals(PackageName.LINE, packageName.intValue());

		MgcpEvent mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.RINGING, mgcpEvent.intValue());
		assertEquals("rg", mgcpEvent.getName());
		assertEquals("to=6000", mgcpEvent.getParms());

		ConnectionIdentifier connectionIdentifier = eventName.getConnectionIdentifier();
		assertNotNull(connectionIdentifier);
		assertEquals("112A9FF", connectionIdentifier.toString());

		String encodedText = parser.encodeEventName(eventName);
		assertEquals(text + "(" + param + ")", encodedText);

		// Test 2
		text = "G/rt@$";
		eventName = parser.decodeEventName(text, null);

		assertNotNull(eventName);
		packageName = eventName.getPackageName();
		assertEquals(PackageName.GENERIC_MEDIA, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.RINGBACK_TONE, mgcpEvent.intValue());
		assertEquals("rt", mgcpEvent.getName());
		assertNull(mgcpEvent.getParms());

		connectionIdentifier = eventName.getConnectionIdentifier();
		assertNotNull(connectionIdentifier);
		assertEquals(ConnectionIdentifier.AnyConnection.toString(), connectionIdentifier.toString());

		encodedText = parser.encodeEventName(eventName);
		assertEquals(text, encodedText);

		// Test 3
		text = "R/qa@*";
		eventName = parser.decodeEventName(text, null);

		assertNotNull(eventName);
		packageName = eventName.getPackageName();
		assertEquals(PackageName.RTP, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.QUALITY_ALERT, mgcpEvent.intValue());
		assertEquals("qa", mgcpEvent.getName());
		assertNull(mgcpEvent.getParms());

		connectionIdentifier = eventName.getConnectionIdentifier();
		assertNotNull(connectionIdentifier);
		assertEquals(ConnectionIdentifier.AllConnections.toString(), connectionIdentifier.toString());

		encodedText = parser.encodeEventName(eventName);
		assertEquals(text, encodedText);

		// Test 4
		text = "D/[0-9#T]";
		eventName = parser.decodeEventName(text, null);

		packageName = eventName.getPackageName();
		assertEquals(PackageName.DTMF, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.getCurrentLargestEventValue(), mgcpEvent.intValue());
		assertEquals("[0-9#T]", mgcpEvent.getName());

		connectionIdentifier = eventName.getConnectionIdentifier();
		assertNull(connectionIdentifier);

		encodedText = parser.encodeEventName(eventName);
		assertEquals(text, encodedText);

		// Test 5
		text = "T/AllEvents";
		eventName = parser.decodeEventName(text, null);

		packageName = eventName.getPackageName();
		assertEquals(PackageName.TRUNK, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.ALL_EVENTS, mgcpEvent.intValue());
		assertEquals("AllEvents", mgcpEvent.getName());

		connectionIdentifier = eventName.getConnectionIdentifier();
		assertNull(connectionIdentifier);

		encodedText = parser.encodeEventName(eventName);
		assertEquals(text, encodedText);

		// Test 6
		text = "*/AllEvents";
		eventName = parser.decodeEventName(text, null);

		packageName = eventName.getPackageName();
		assertEquals(PackageName.ALL_PACKAGES, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.ALL_EVENTS, mgcpEvent.intValue());
		assertEquals("AllEvents", mgcpEvent.getName());

		connectionIdentifier = eventName.getConnectionIdentifier();
		assertNull(connectionIdentifier);

		encodedText = parser.encodeEventName(eventName);
		assertEquals(text, encodedText);
		
		//Test 7
		text = "AU/aupr";
		eventName = parser.decodeEventName(text, "ip=22 ns=42 na=2");
		
		packageName = eventName.getPackageName();
		assertEquals(AUPackage.ADVANCED_AUDIO, packageName.intValue());
		
		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(AUMgcpEvent.PLAY_RECORD, mgcpEvent.intValue());
		
		assertEquals("ip=22 ns=42 na=2", mgcpEvent.getParms());

	}

	public void testDecodeEncodeEmbeddedRequest() throws ParseException {
		String text = "R(D/[0-9#T] (D),L/hu (N)),S(L/dl)";

		EmbeddedRequest embeddedRequest = parser.decodeEmbeddedRequest(text);

		assertNotNull(embeddedRequest);

		RequestedEvent[] requestedEventList = embeddedRequest.getEmbeddedRequestList();
		assertNotNull(requestedEventList);
		assertEquals(2, requestedEventList.length);

		// Test for Dtmf event D/[0-9#T]
		RequestedEvent event1 = requestedEventList[0];
		EventName eventName1 = event1.getEventName();

		PackageName packageName = eventName1.getPackageName();
		assertEquals(PackageName.DTMF, packageName.intValue());

		MgcpEvent mgcpEvent = eventName1.getEventIdentifier();

		// There are two custom Events and hence CurrentLargestEventValue will
		// be greater than 2
		assertEquals(MgcpEvent.getCurrentLargestEventValue(), mgcpEvent.intValue());
		assertEquals("[0-9#T]", mgcpEvent.getName());

		ConnectionIdentifier connectionIdentifier = eventName1.getConnectionIdentifier();
		assertNull(connectionIdentifier);

		RequestedAction[] requestedActionList1 = event1.getRequestedActions();
		assertEquals(1, requestedActionList1.length);
		assertEquals(RequestedAction.TREAT_ACCORDING_TO_DIGIT_MAP, requestedActionList1[0].getRequestedAction());

		// Test for Dtmf event L/hu(N)
		RequestedEvent event2 = requestedEventList[1];
		EventName eventName2 = event2.getEventName();

		PackageName packageName2 = eventName2.getPackageName();
		assertEquals(PackageName.LINE, packageName2.intValue());

		MgcpEvent mgcpEvent2 = eventName2.getEventIdentifier();

		// There are two custom Events and hence CurrentLargestEventValue will
		// be greater than 2
		assertEquals(MgcpEvent.ON_HOOK_TRANSITION, mgcpEvent2.intValue());
		assertEquals(MgcpEvent.hu.toString(), mgcpEvent2.getName());

		ConnectionIdentifier connectionIdentifier2 = eventName2.getConnectionIdentifier();
		assertNull(connectionIdentifier2);

		RequestedAction[] requestedActionList2 = event2.getRequestedActions();
		assertEquals(1, requestedActionList2.length);
		assertEquals(RequestedAction.NOTIFY_IMMEDIATELY, requestedActionList2[0].getRequestedAction());

		// Test the EmbeddedSignalRequest
		EventName[] embeddedSignalRequestList = embeddedRequest.getEmbeddedSignalRequest();
		assertNotNull(embeddedSignalRequestList);
		assertEquals(1, embeddedSignalRequestList.length);
		EventName eventName = embeddedSignalRequestList[0];

		packageName = eventName.getPackageName();
		assertEquals(PackageName.LINE, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.DIAL_TONE, mgcpEvent.intValue());
		assertNull(mgcpEvent.getParms());
		assertNull(eventName.getConnectionIdentifier());

		// Test DigitMap
		DigitMap digitMap = embeddedRequest.getEmbeddedDigitMap();
		assertNull(digitMap);

		String encodedText = parser.encodeEmbeddedRequest(embeddedRequest);

		assertEquals(text, encodedText);

	}

	public void testDecodeEncodeRequestedEvent() throws ParseException {

		// Test 1
		String text = "L/hu (N)";
		RequestedEvent requestedEvent = parser.decodeRequestedEvent(text);
		assertNotNull(requestedEvent);

		EventName eventName = requestedEvent.getEventName();
		assertEquals(PackageName.LINE, eventName.getPackageName().intValue());
		assertEquals(MgcpEvent.ON_HOOK_TRANSITION, eventName.getEventIdentifier().intValue());

		RequestedAction[] requestedActionList = requestedEvent.getRequestedActions();
		assertEquals(1, requestedActionList.length);
		assertEquals(RequestedAction.NOTIFY_IMMEDIATELY, requestedActionList[0].getRequestedAction());

		String encodedText = parser.encodeRequestedEvent(requestedEvent);
		assertEquals(text, encodedText);

		// Test 2
		text = "L/hf (S,N)";
		requestedEvent = parser.decodeRequestedEvent(text);
		assertNotNull(requestedEvent);

		eventName = requestedEvent.getEventName();
		assertEquals(PackageName.LINE, eventName.getPackageName().intValue());
		assertEquals(MgcpEvent.FLASH_HOOK, eventName.getEventIdentifier().intValue());

		requestedActionList = requestedEvent.getRequestedActions();
		assertEquals(2, requestedActionList.length);
		assertEquals(RequestedAction.SWAP, requestedActionList[0].getRequestedAction());
		assertEquals(RequestedAction.NOTIFY_IMMEDIATELY, requestedActionList[1].getRequestedAction());

		encodedText = parser.encodeRequestedEvent(requestedEvent);
		assertEquals(text, encodedText);

		// Test 3
		text = "R/foobar (N) (epar=2)";
		requestedEvent = parser.decodeRequestedEvent(text);
		assertNotNull(requestedEvent);

		eventName = requestedEvent.getEventName();
		assertEquals(PackageName.RTP, eventName.getPackageName().intValue());
		assertEquals(MgcpEvent.getCurrentLargestEventValue(), eventName.getEventIdentifier().intValue());
		assertEquals("foobar", eventName.getEventIdentifier().getName());
		assertEquals("epar=2", eventName.getEventIdentifier().getParms());

		requestedActionList = requestedEvent.getRequestedActions();
		assertEquals(1, requestedActionList.length);
		assertEquals(RequestedAction.NOTIFY_IMMEDIATELY, requestedActionList[0].getRequestedAction());

		encodedText = parser.encodeRequestedEvent(requestedEvent);
		assertEquals(text, encodedText);

		// Test 4
		text = "L/hd (E(R(D/[0-9#T] (D),L/hu (N)),S(L/dl),D([0-9].[#T])))";
		requestedEvent = parser.decodeRequestedEvent(text);
		assertNotNull(requestedEvent);
		eventName = requestedEvent.getEventName();
		assertEquals(PackageName.LINE, eventName.getPackageName().intValue());

		requestedActionList = requestedEvent.getRequestedActions();
		assertEquals(1, requestedActionList.length);

		assertEquals(RequestedAction.EMBEDDED_NOTIFICATION_REQUEST, requestedActionList[0].getRequestedAction());

		EmbeddedRequest embeddedRequest = requestedActionList[0].getEmbeddedRequest();
		assertNotNull(embeddedRequest);

		RequestedEvent[] requestedEventList = embeddedRequest.getEmbeddedRequestList();
		assertNotNull(requestedEventList);
		assertEquals(2, requestedEventList.length);

		// Test for Dtmf event D/[0-9#T]
		RequestedEvent event1 = requestedEventList[0];
		EventName eventName1 = event1.getEventName();

		PackageName packageName = eventName1.getPackageName();
		assertEquals(PackageName.DTMF, packageName.intValue());

		MgcpEvent mgcpEvent = eventName1.getEventIdentifier();

		// There are two custom Events and hence CurrentLargestEventValue will
		// be greater than 2
		assertEquals(MgcpEvent.getCurrentLargestEventValue() - 1, mgcpEvent.intValue());
		assertEquals("[0-9#T]", mgcpEvent.getName());

		ConnectionIdentifier connectionIdentifier = eventName1.getConnectionIdentifier();
		assertNull(connectionIdentifier);

		RequestedAction[] requestedActionList1 = event1.getRequestedActions();
		assertEquals(1, requestedActionList1.length);
		assertEquals(RequestedAction.TREAT_ACCORDING_TO_DIGIT_MAP, requestedActionList1[0].getRequestedAction());

		// Test for Dtmf event L/hu(N)
		RequestedEvent event2 = requestedEventList[1];
		EventName eventName2 = event2.getEventName();

		PackageName packageName2 = eventName2.getPackageName();
		assertEquals(PackageName.LINE, packageName2.intValue());

		MgcpEvent mgcpEvent2 = eventName2.getEventIdentifier();

		// There are two custom Events and hence CurrentLargestEventValue will
		// be greater than 2
		assertEquals(MgcpEvent.ON_HOOK_TRANSITION, mgcpEvent2.intValue());
		assertEquals(MgcpEvent.hu.toString(), mgcpEvent2.getName());

		ConnectionIdentifier connectionIdentifier2 = eventName2.getConnectionIdentifier();
		assertNull(connectionIdentifier2);

		RequestedAction[] requestedActionList2 = event2.getRequestedActions();
		assertEquals(1, requestedActionList2.length);
		assertEquals(RequestedAction.NOTIFY_IMMEDIATELY, requestedActionList2[0].getRequestedAction());

		// Test the EmbeddedSignalRequest
		EventName[] embeddedSignalRequestList = embeddedRequest.getEmbeddedSignalRequest();
		assertNotNull(embeddedSignalRequestList);
		assertEquals(1, embeddedSignalRequestList.length);
		eventName = embeddedSignalRequestList[0];

		packageName = eventName.getPackageName();
		assertEquals(PackageName.LINE, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.DIAL_TONE, mgcpEvent.intValue());
		assertNull(mgcpEvent.getParms());
		assertNull(eventName.getConnectionIdentifier());

		// Test DigitMap
		DigitMap digitMap = embeddedRequest.getEmbeddedDigitMap();
		assertEquals("[0-9].[#T]", digitMap.toString());

		encodedText = parser.encodeRequestedEvent(requestedEvent);
		assertEquals(text, encodedText);

	}

	public void testDecodeEncodeConnectionParm() {
		String text = "PR=780";

		ConnectionParm connectionParm = parser.decodeConnectionParm(text);
		assertNotNull(connectionParm);
		assertEquals(RegularConnectionParm.PACKETS_RECEIVED, connectionParm.getConnectionParmType());
		assertEquals(780, connectionParm.getConnectionParmValue());

		String encodedText = parser.encodeConnectionParm(connectionParm);

		assertEquals(text, encodedText);

		// Test Custom
		text = "MS=1";
		connectionParm = parser.decodeConnectionParm(text);
		assertNotNull(connectionParm);
		assertEquals(1, connectionParm.getConnectionParmValue());
		encodedText = parser.encodeConnectionParm(connectionParm);

		assertEquals(text, encodedText);

	}

	public void testDecodeReasonCode() {
		String text = "0 Endpoint state is nominal.";
		ReasonCode reasonCode = parser.decodeReasonCode(text);
		assertNotNull(reasonCode);
		assertEquals(ReasonCode.ENDPOINT_STATE_IS_NOMINAL, reasonCode.getValue());

		String encodedText = reasonCode.toString();

		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeNotifiedEntity() throws ParseException {
		String text = "128.96.41.12";
		NotifiedEntity notifiedEntity = parser.decodeNotifiedEntity(text, true);
		assertNotNull(notifiedEntity);
		assertEquals("128.96.41.12", notifiedEntity.getDomainName());
		assertEquals(2427, notifiedEntity.getPortNumber());

		String encodedText = parser.encodeNotifiedEntity(notifiedEntity);
		assertEquals("128.96.41.12:2427", encodedText);

		// Test 2
		text = "CA-1@whatever.net";
		notifiedEntity = parser.decodeNotifiedEntity(text, false);
		assertEquals("whatever.net", notifiedEntity.getDomainName());
		assertEquals("CA-1", notifiedEntity.getLocalName());
		assertEquals(2727, notifiedEntity.getPortNumber());
		encodedText = parser.encodeNotifiedEntity(notifiedEntity);
		assertEquals("CA-1@whatever.net:2727", encodedText);

		// Test 3
		text = "ca@ca1.whatever.net:5678";
		notifiedEntity = parser.decodeNotifiedEntity(text, false);
		assertEquals("ca1.whatever.net", notifiedEntity.getDomainName());
		assertEquals("ca", notifiedEntity.getLocalName());
		assertEquals(5678, notifiedEntity.getPortNumber());
		encodedText = parser.encodeNotifiedEntity(notifiedEntity);
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeEndpointIdentifier() {
		String text = "aaln/1@rgw.whatever.net";
		EndpointIdentifier endpointIdentifier = parser.decodeEndpointIdentifier(text);
		assertNotNull(endpointIdentifier);
		assertEquals("aaln/1", endpointIdentifier.getLocalEndpointName());
		assertEquals("rgw.whatever.net", endpointIdentifier.getDomainName());

		String encodedText = parser.encodeEndpointIdentifier(endpointIdentifier);
		assertEquals(text, encodedText);

	}

}
