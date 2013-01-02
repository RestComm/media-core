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
import org.mobicents.protocols.mgcp.stack.test.TestHarness;

import org.mobicents.protocols.mgcp.parser.StringFunctions;
import org.mobicents.protocols.mgcp.parser.SplitDetails;

import org.mobicents.protocols.mgcp.parser.params.CapabilityHandler;
import org.mobicents.protocols.mgcp.parser.params.InfoCodeHandler;
import org.mobicents.protocols.mgcp.parser.params.ConnectionModeHandler;
import org.mobicents.protocols.mgcp.parser.params.BearerInformationHandler;
import org.mobicents.protocols.mgcp.parser.params.LocalOptionValueHandler;
import org.mobicents.protocols.mgcp.parser.params.BandwidthHandler;
import org.mobicents.protocols.mgcp.parser.params.CompressionAlgorithmHandler;
import org.mobicents.protocols.mgcp.parser.params.EchoCancellationHandler;
import org.mobicents.protocols.mgcp.parser.params.EncryptionMethodHandler;
import org.mobicents.protocols.mgcp.parser.params.GainControlHandler;
import org.mobicents.protocols.mgcp.parser.params.PacketizationPeriodHandler;
import org.mobicents.protocols.mgcp.parser.params.ResourceReservationHandler;
import org.mobicents.protocols.mgcp.parser.params.SilenceSuppressionHandler;
import org.mobicents.protocols.mgcp.parser.params.TypeOfNetworkHandler;
import org.mobicents.protocols.mgcp.parser.params.TypeOfServiceHandler;
import org.mobicents.protocols.mgcp.parser.params.RestartMethodHandler;
import org.mobicents.protocols.mgcp.parser.params.EventNameHandler;
import org.mobicents.protocols.mgcp.parser.params.EmbeddedRequestHandler;
import org.mobicents.protocols.mgcp.parser.params.RequestedEventHandler;
import org.mobicents.protocols.mgcp.parser.params.ConnectionParmHandler;
import org.mobicents.protocols.mgcp.parser.params.ReasonCodeHandler;
import org.mobicents.protocols.mgcp.parser.params.NotifiedEntityHandler;
import org.mobicents.protocols.mgcp.parser.params.EndpointIdentifierHandler;

public class ParserTest extends TestHarness {

	public ParserTest() {
		super("ParserTest");
	}

	@Override
	public void setUp() {
	}

	@Override
	public void tearDown() {

	}

	public void testDecodeEncodeCapabilityList() throws ParseException {
		String capability = "a:PCMU;G729,p:10-100,e:on,s:off,v:L;S,m:sendonly;recvonly;sendrecv;inactive;netwloop;netwtest";						   
		byte[] value=capability.getBytes();
		CapabilityValue[] capabilities = CapabilityHandler.decodeList(value,0,value.length);

		assertNotNull(capabilities);
		assertEquals(6, capabilities.length);

		byte[] output=new byte[5000];
		int length=CapabilityHandler.encodeList(output,0,capabilities);
		String encodedCapability = new String(output,0,length);
		assertEquals(capability, encodedCapability);
	}

	public void testDecodeEncodeInfoCodeList() throws ParseException {
		String requestedInfo = "R,D,S,X,N,I,T,O,ES";
		byte[] value=requestedInfo.getBytes();		
		InfoCode[] infoCodeList = InfoCodeHandler.decodeList(value,0,value.length);

		assertNotNull(infoCodeList);
		assertEquals(9, infoCodeList.length);

		byte[] output=new byte[5000];
		int length=InfoCodeHandler.encodeList(output,0,infoCodeList);
		String encodedInfoCodeList = new String(output,0,length);
		assertEquals(requestedInfo, encodedInfoCodeList);		
	}

	public void testDecodeConnectionMode() throws ParseException {
		String connectionMode = "conttest";
		byte[] value=connectionMode.getBytes();		
		
		ConnectionMode cMode = ConnectionModeHandler.decode(value,0,value.length);
		assertNotNull(cMode);
		assertEquals(connectionMode, cMode.toString());
	}

	public void testDecodeEncodeBearerInformation() throws ParseException {
		String text = "e:mu";
		byte[] value=text.getBytes();		
		BearerInformation bearerInformation = BearerInformationHandler.decode(value,0,value.length);
		assertNotNull(bearerInformation);
		assertEquals("mu", bearerInformation.toString());

		byte[] output=new byte[5000];
		int length=BearerInformationHandler.encode(output,0,bearerInformation);
		String encodedText = new String(output,0,length);

		assertEquals(text, encodedText);
	}

	public void testDecodeLocalOptionValueList() throws ParseException {
		String text = "p:10,a:PCMU";
		byte[] value=text.getBytes();		
		LocalOptionValue[] localOptionValueList = LocalOptionValueHandler.decodeList(value,0,value.length);
		assertNotNull(localOptionValueList);
		assertEquals(2, localOptionValueList.length);

		byte[] output=new byte[5000];
		int length=LocalOptionValueHandler.encodeList(output,0,localOptionValueList);
		String encodedText = new String(output,0,length);
		assertEquals(text, encodedText);

		text = "a:G729;PCMU,p:30-90,e:on,s:on,customkey:customevalue";
		value=text.getBytes();
		localOptionValueList = LocalOptionValueHandler.decodeList(value,0,value.length);
		assertEquals(5, localOptionValueList.length);

		length=LocalOptionValueHandler.encodeList(output,0,localOptionValueList);
		encodedText = new String(output,0,length);
		assertEquals(text, encodedText);

	}

	public void testDecodeEncodeBandwidth() throws ParseException {
		String text = "64";
		byte[] value=text.getBytes();		
		Bandwidth bandwidth = BandwidthHandler.decode(value,0,value.length);
		assertNotNull(bandwidth);
		assertEquals(64, bandwidth.getBandwidthLowerBound());
		assertEquals(64, bandwidth.getBandwidthUpperBound());

		byte[] output=new byte[5000];
		int length=BandwidthHandler.encode(output,0,bandwidth);
		String encodedText = new String(output,0,length);
		
		assertEquals(text, encodedText);

		text = "64-128";
		value=text.getBytes();		
		bandwidth = BandwidthHandler.decode(value,0,value.length);
		assertNotNull(bandwidth);
		assertEquals(64, bandwidth.getBandwidthLowerBound());
		assertEquals(128, bandwidth.getBandwidthUpperBound());

		length=BandwidthHandler.encode(output,0,bandwidth);
		encodedText = new String(output,0,length);

		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeCompressionAlgorithm() throws ParseException {
		String text = "PCMU;G726";
		byte[] value=text.getBytes();		
		CompressionAlgorithm compressionAlgorithm = CompressionAlgorithmHandler.decode(value,0,value.length);
		assertNotNull(compressionAlgorithm);
		assertEquals(2, compressionAlgorithm.getCompressionAlgorithmNames().length);

		byte[] output=new byte[5000];
		int length=CompressionAlgorithmHandler.encode(output,0,compressionAlgorithm);
		String encodedText = new String(output,0,length);

		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeEchoCancellation() throws ParseException {
		String text = "on";
		byte[] value=text.getBytes();		
		EchoCancellation echoCancellation = EchoCancellationHandler.decode(value,0,value.length);
		assertNotNull(echoCancellation);
		assertTrue(echoCancellation.getEchoCancellation());

		byte[] output=new byte[5000];
		int length=EchoCancellationHandler.encode(output,0,echoCancellation);
		String encodedText = new String(output,0,length);

		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeEncryptionMethod() throws ParseException {
		String text = "base64:somekey";
		byte[] value=text.getBytes();		
		EncryptionMethod encryptionMethod = EncryptionMethodHandler.decode(value,0,value.length);
		assertNotNull(encryptionMethod);
		assertEquals(2, encryptionMethod.getEncryptionMethod());
		assertEquals("somekey", encryptionMethod.getEncryptionKey());

		byte[] output=new byte[5000];
		int length=EncryptionMethodHandler.encode(output,0,encryptionMethod);
		String encodedText = new String(output,0,length);

		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeGainControl() throws ParseException {
		String text = "auto";
		byte[] value=text.getBytes();		
		GainControl gainControl = GainControlHandler.decode(value,0,value.length);
		assertNotNull(gainControl);
		assertTrue(gainControl.getGainControlAuto());

		byte[] output=new byte[5000];
		int length=GainControlHandler.encode(output,0,gainControl);
		String encodedText = new String(output,0,length);

		assertEquals(text, encodedText);

		text = "101";
		value=text.getBytes();		
		gainControl = GainControlHandler.decode(value,0,value.length);
		assertEquals(101, gainControl.getGainControl());
		length=GainControlHandler.encode(output,0,gainControl);
		encodedText = new String(output,0,length);
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodePacketizationPeriod() throws ParseException {
		String text = "20";
		byte[] value=text.getBytes();		
		PacketizationPeriod packetizationPeriod = PacketizationPeriodHandler.decode(value,0,value.length);
		assertNotNull(packetizationPeriod);
		assertEquals(20, packetizationPeriod.getPacketizationPeriodLowerBound());
		assertEquals(packetizationPeriod.getPacketizationPeriodLowerBound(), packetizationPeriod.getPacketizationPeriodUpperBound());

		byte[] output=new byte[5000];
		int length=PacketizationPeriodHandler.encode(output,0,packetizationPeriod);
		String encodedText = new String(output,0,length);
		assertEquals(text, encodedText);

		text = "20-25";
		value=text.getBytes();		
		packetizationPeriod = PacketizationPeriodHandler.decode(value,0,value.length);
		assertEquals(20, packetizationPeriod.getPacketizationPeriodLowerBound());
		assertEquals(25, packetizationPeriod.getPacketizationPeriodUpperBound());
		length=PacketizationPeriodHandler.encode(output,0,packetizationPeriod);
		encodedText = new String(output,0,length);
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeResourceReservation() throws ParseException {
		String text = "be";
		byte[] value=text.getBytes();		
		ResourceReservation resourceReservation = ResourceReservationHandler.decode(value,0,value.length);
		assertNotNull(resourceReservation);
		assertEquals(ResourceReservation.BEST_EFFORT, resourceReservation.getResourceReservation());

		byte[] output=new byte[5000];
		int length=ResourceReservationHandler.encode(output,0,resourceReservation);
		String encodedText = new String(output,0,length);
		
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeSilenceSuppression() throws ParseException {
		String text = "off";
		byte[] value=text.getBytes();		
		SilenceSuppression silenceSuppression = SilenceSuppressionHandler.decode(value,0,value.length);
		assertNotNull(silenceSuppression);
		assertEquals(false, silenceSuppression.getSilenceSuppression());

		byte[] output=new byte[5000];
		int length=SilenceSuppressionHandler.encode(output,0,silenceSuppression);
		String encodedText = new String(output,0,length);
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeTypeOfNetwork() throws ParseException {
		String text = "atm";
		byte[] value=text.getBytes();		
		TypeOfNetwork typeOfNetwork = TypeOfNetworkHandler.decode(value,0,value.length);
		assertNotNull(typeOfNetwork);
		assertEquals(TypeOfNetwork.ATM, typeOfNetwork.getTypeOfNetwork());

		byte[] output=new byte[5000];
		int length=TypeOfNetworkHandler.encode(output,0,typeOfNetwork);
		String encodedText = new String(output,0,length);
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeTypeOfService() throws ParseException {
		String text = "5";
		byte[] value=text.getBytes();		
		TypeOfService typeOfService = TypeOfServiceHandler.decode(value,0,value.length);
		assertNotNull(typeOfService);
		assertEquals(5, typeOfService.getTypeOfService());

		byte[] output=new byte[5000];
		int length=TypeOfServiceHandler.encode(output,0,typeOfService);
		String encodedText = new String(output,0,length);
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeRestartMethod() throws ParseException {
		String text = "disconnected";
		byte[] value=text.getBytes();		
		RestartMethod restartMethod = RestartMethodHandler.decode(value,0,value.length);
		assertNotNull(restartMethod);
		assertEquals(RestartMethod.DISCONNECTED, restartMethod.getRestartMethod());

		byte[] output=new byte[5000];
		int length=RestartMethodHandler.encode(output,0,restartMethod);
		String encodedText = new String(output,0,length);
		assertEquals(text, encodedText);
	}

	public void testDecodeEncodeEventName() throws ParseException {
		String text = "L/rg@112A9FF to=6000";
		byte[] value=text.getBytes();		
		EventName eventName = EventNameHandler.decodeWithParams(value,0,12,13,7);

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

		byte[] output=new byte[5000];
		int length=EventNameHandler.encodeList(output,0,new EventName[] { eventName });
		String encodedText = new String(output,0,length);
		assertEquals("L/rg@112A9FF(to=6000)", encodedText);

		// Test 2
		text = "G/rt@$";
		value=text.getBytes();
		eventName = EventNameHandler.decode(value,0,value.length);

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

		length=EventNameHandler.encode(output,0,eventName);
		encodedText = new String(output,0,length);
		assertEquals(text, encodedText);
		// Test 3
		text = "R/qa@*";
		value=text.getBytes();
		eventName = EventNameHandler.decode(value,0,value.length);
		
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

		length=EventNameHandler.encode(output,0,eventName);
		encodedText = new String(output,0,length);
		assertEquals(text, encodedText);

		// Test 4
		text = "D/[0-9#T]";
		value=text.getBytes();
		eventName = EventNameHandler.decode(value,0,value.length);
		
		packageName = eventName.getPackageName();
		assertEquals(PackageName.DTMF, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.getCurrentLargestEventValue(), mgcpEvent.intValue());
		assertEquals("[0-9#T]", mgcpEvent.getName());

		connectionIdentifier = eventName.getConnectionIdentifier();
		assertNull(connectionIdentifier);

		length=EventNameHandler.encode(output,0,eventName);
		encodedText = new String(output,0,length);
		assertEquals(text, encodedText);

		// Test 5
		text = "T/AllEvents";
		value=text.getBytes();
		eventName = EventNameHandler.decode(value,0,value.length);
		
		packageName = eventName.getPackageName();
		assertEquals(PackageName.TRUNK, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.ALL_EVENTS, mgcpEvent.intValue());
		assertEquals("AllEvents", mgcpEvent.getName());

		connectionIdentifier = eventName.getConnectionIdentifier();
		assertNull(connectionIdentifier);

		length=EventNameHandler.encode(output,0,eventName);
		encodedText = new String(output,0,length);
		assertEquals(text, encodedText);

		// Test 6
		text = "*/AllEvents";
		value=text.getBytes();
		eventName = EventNameHandler.decode(value,0,value.length);
		
		packageName = eventName.getPackageName();
		assertEquals(PackageName.ALL_PACKAGES, packageName.intValue());

		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(MgcpEvent.ALL_EVENTS, mgcpEvent.intValue());
		assertEquals("AllEvents", mgcpEvent.getName());

		connectionIdentifier = eventName.getConnectionIdentifier();
		assertNull(connectionIdentifier);

		length=EventNameHandler.encode(output,0,eventName);
		encodedText = new String(output,0,length);
		assertEquals(text, encodedText);
		
		//Test 7
		text = "AU/pr ip=22 ns=42 na=2";
		value=text.getBytes();		
		eventName = EventNameHandler.decodeWithParams(value,0,5,6,16);
		
		packageName = eventName.getPackageName();
		assertEquals(AUPackage.ADVANCED_AUDIO, packageName.intValue());
		mgcpEvent = eventName.getEventIdentifier();
		assertEquals(AUMgcpEvent.PLAY_RECORD, mgcpEvent.intValue());		
		assertEquals("ip=22 ns=42 na=2", mgcpEvent.getParms());
	}

	public void testDecodeEncodeEmbeddedRequest() throws ParseException {
		String text = "R(D/[0-9#T] (D),L/hu (N)),S(L/dl)";
		byte[] value=text.getBytes();		
		EmbeddedRequest embeddedRequest = EmbeddedRequestHandler.decode(value,0,value.length);

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

		byte[] output=new byte[5000];
		int length=EmbeddedRequestHandler.encode(output,0,embeddedRequest);
		String encodedText = new String(output,0,length);
		assertEquals("R(D/[0-9#T](D),L/hu(N)),S(L/dl)", encodedText);		
	}

	public void testDecodeEncodeRequestedEvent() throws ParseException {
		// Test 1
		String text = "L/hu (N)";
		byte[] value=text.getBytes();		
		RequestedEvent requestedEvent = RequestedEventHandler.decode(value,0,value.length);
		assertNotNull(requestedEvent);

		EventName eventName = requestedEvent.getEventName();
		assertEquals(PackageName.LINE, eventName.getPackageName().intValue());
		assertEquals(MgcpEvent.ON_HOOK_TRANSITION, eventName.getEventIdentifier().intValue());

		RequestedAction[] requestedActionList = requestedEvent.getRequestedActions();
		assertEquals(1, requestedActionList.length);
		assertEquals(RequestedAction.NOTIFY_IMMEDIATELY, requestedActionList[0].getRequestedAction());

		byte[] output=new byte[5000];
		int length=RequestedEventHandler.encode(output,0,requestedEvent);
		String encodedText = new String(output,0,length);		
		assertEquals("L/hu(N)", encodedText);

		// Test 2
		text = "L/hf (S,N)";
		value=text.getBytes();		
		requestedEvent = RequestedEventHandler.decode(value,0,value.length);
		assertNotNull(requestedEvent);

		eventName = requestedEvent.getEventName();
		assertEquals(PackageName.LINE, eventName.getPackageName().intValue());
		assertEquals(MgcpEvent.FLASH_HOOK, eventName.getEventIdentifier().intValue());

		requestedActionList = requestedEvent.getRequestedActions();
		assertEquals(2, requestedActionList.length);
		assertEquals(RequestedAction.SWAP, requestedActionList[0].getRequestedAction());
		assertEquals(RequestedAction.NOTIFY_IMMEDIATELY, requestedActionList[1].getRequestedAction());

		length=RequestedEventHandler.encode(output,0,requestedEvent);
		encodedText = new String(output,0,length);		
		assertEquals("L/hf(S,N)", encodedText);

		// Test 3
		text = "R/foobar (N) (epar=2)";
		value=text.getBytes();		
		requestedEvent = RequestedEventHandler.decode(value,0,value.length);
		assertNotNull(requestedEvent);

		eventName = requestedEvent.getEventName();
		assertEquals(PackageName.RTP, eventName.getPackageName().intValue());
		assertEquals(MgcpEvent.getCurrentLargestEventValue(), eventName.getEventIdentifier().intValue());
		assertEquals("foobar", eventName.getEventIdentifier().getName());
		assertEquals("epar=2", eventName.getEventIdentifier().getParms());

		requestedActionList = requestedEvent.getRequestedActions();
		assertEquals(1, requestedActionList.length);
		assertEquals(RequestedAction.NOTIFY_IMMEDIATELY, requestedActionList[0].getRequestedAction());

		length=RequestedEventHandler.encode(output,0,requestedEvent);
		encodedText = new String(output,0,length);		
		assertEquals("R/foobar(N)(epar=2)", encodedText);

		// Test 4
		text = "L/hd (E(R(D/[0-9#T] (D),L/hu (N)),S(L/dl),D([0-9].[#T])))";
		value=text.getBytes();		
		requestedEvent = RequestedEventHandler.decode(value,0,value.length);
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

		length=RequestedEventHandler.encode(output,0,requestedEvent);
		encodedText = new String(output,0,length);		
		assertEquals("L/hd(E(R(D/[0-9#T](D),L/hu(N)),S(L/dl),D([0-9].[#T])))", encodedText);
	}

	public void testDecodeEncodeConnectionParm() throws ParseException {
		String text = "PR=780";
		byte[] value=text.getBytes();		
		ConnectionParm connectionParm = ConnectionParmHandler.decode(value,0,value.length);
		assertNotNull(connectionParm);
		assertEquals(RegularConnectionParm.PACKETS_RECEIVED, connectionParm.getConnectionParmType());
		assertEquals(780, connectionParm.getConnectionParmValue());

		byte[] output=new byte[5000];
		int length=ConnectionParmHandler.encode(output,0,connectionParm);
		String encodedText = new String(output,0,length);				
		assertEquals(text, encodedText);

		// Test Custom
		text = "MS=1";
		value=text.getBytes();		
		connectionParm = ConnectionParmHandler.decode(value,0,value.length);
		assertNotNull(connectionParm);
		assertEquals(1, connectionParm.getConnectionParmValue());
		length=ConnectionParmHandler.encode(output,0,connectionParm);
		encodedText = new String(output,0,length);	
		assertEquals(text, encodedText);

	}

	public void testDecodeReasonCode() throws ParseException {
		String text = "0 Endpoint state is nominal.";
		byte[] value=text.getBytes();		
		ReasonCode reasonCode = ReasonCodeHandler.decode(value,0,value.length);
		assertNotNull(reasonCode);
		assertEquals(ReasonCode.ENDPOINT_STATE_IS_NOMINAL, reasonCode.getValue());

		byte[] output=new byte[5000];
		int length=ReasonCodeHandler.encode(output,0,reasonCode);
		String encodedText = new String(output,0,length);
		assertEquals(text, encodedText);		
	}

	public void testDecodeEncodeNotifiedEntity() throws ParseException {
		String text = "128.96.41.12";
		byte[] value=text.getBytes();		
		NotifiedEntity notifiedEntity = NotifiedEntityHandler.decode(value,0,value.length,true);
		assertNotNull(notifiedEntity);
		assertEquals("128.96.41.12", notifiedEntity.getDomainName());
		assertEquals(2427, notifiedEntity.getPortNumber());

		byte[] output=new byte[5000];
		int length=NotifiedEntityHandler.encode(output,0,notifiedEntity);
		String encodedText = new String(output,0,length);				
		assertEquals("128.96.41.12:2427", encodedText);

		// Test 2
		text = "CA-1@whatever.net";
		value=text.getBytes();		
		notifiedEntity = NotifiedEntityHandler.decode(value,0,value.length,false);
		assertEquals("whatever.net", notifiedEntity.getDomainName());
		assertEquals("CA-1", notifiedEntity.getLocalName());
		assertEquals(2727, notifiedEntity.getPortNumber());
		length=NotifiedEntityHandler.encode(output,0,notifiedEntity);
		encodedText = new String(output,0,length);		
		assertEquals("CA-1@whatever.net:2727", encodedText);

		// Test 3
		text = "ca@ca1.whatever.net:5678";
		value=text.getBytes();		
		notifiedEntity = NotifiedEntityHandler.decode(value,0,value.length,false);
		assertEquals("ca1.whatever.net", notifiedEntity.getDomainName());
		assertEquals("ca", notifiedEntity.getLocalName());
		assertEquals(5678, notifiedEntity.getPortNumber());
		length=NotifiedEntityHandler.encode(output,0,notifiedEntity);
		encodedText = new String(output,0,length);
		assertEquals(text, encodedText);		
	}

	public void testDecodeEncodeEndpointIdentifier() throws ParseException {
		String text = "aaln/1@rgw.whatever.net";
		byte[] value=text.getBytes();		
		EndpointIdentifier endpointIdentifier = EndpointIdentifierHandler.decode(value,0,value.length);
		assertNotNull(endpointIdentifier);
		assertEquals("aaln/1", endpointIdentifier.getLocalEndpointName());
		assertEquals("rgw.whatever.net", endpointIdentifier.getDomainName());

		byte[] output=new byte[5000];
		int length=EndpointIdentifierHandler.encode(output,0,endpointIdentifier);
		String encodedText = new String(output,0,length);				
		assertEquals(text, encodedText);
	}
}
