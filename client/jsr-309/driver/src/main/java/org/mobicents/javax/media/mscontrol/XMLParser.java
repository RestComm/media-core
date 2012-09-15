package org.mobicents.javax.media.mscontrol;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.resource.enums.EventTypeEnum;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mobicents.javax.media.mscontrol.resource.ExtendedParameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLParser implements TagNames {

	private static final String LINE_SEPARATOR = "\n";

	public static Map<String, Parameter> strvsParameter = new HashMap<String, Parameter>();

	public static Map<String, EventType> strvsEventType = new HashMap<String, EventType>();

	static {

		// Parameter Mapping
		strvsParameter.put(ExtendedParameter.ENDPOINT_LOCAL_NAME.toString(),
				ExtendedParameter.ENDPOINT_LOCAL_NAME);

		// EventType mapping
		strvsEventType.put(EventTypeEnum.PLAY_COMPLETED.toString(), EventTypeEnum.PLAY_COMPLETED);
		strvsEventType.put(EventTypeEnum.SIGNAL_DETECTED.toString(), EventTypeEnum.SIGNAL_DETECTED);

	};

/*	private void populatePlayer(NodeList playerNodeList, MediaConfigImpl medConfimpl) {
		// Player

		for (int i = 0; i < playerNodeList.getLength(); i++) {

			List<DefaultEventGeneratorFactory> generatorList = new ArrayList<DefaultEventGeneratorFactory>();

			List<PlayerEventDetectorFactory> detectorList = new ArrayList<PlayerEventDetectorFactory>();

			Node player = playerNodeList.item(i);
			NodeList sigAndEves = player.getChildNodes();
			for (int j = 0; j < sigAndEves.getLength(); j++) {
				Node sigOrEve = sigAndEves.item(j);

				if (SIGNAL.compareTo(sigOrEve.getNodeName()) == 0) {

					NodeList signalsList = sigOrEve.getChildNodes();
					Node mgcpEventNode = signalsList.item(0);
					Node mgcpPackageNode = signalsList.item(1);
					Node onEndpointNode = signalsList.item(2);

					generatorList.add(new DefaultEventGeneratorFactory(mgcpPackageNode.getTextContent(), mgcpEventNode
							.getTextContent(), Boolean.parseBoolean(onEndpointNode.getTextContent())));

				} else if (EVENT.compareTo(sigOrEve.getNodeName()) == 0) {
					NodeList signalsList = sigOrEve.getChildNodes();
					Node mgcpEventNode = signalsList.item(0);
					Node mgcpPackageNode = signalsList.item(1);
					Node onEndpointNode = signalsList.item(2);
					Node eventTypeNode = signalsList.item(3);

//					detectorList.add(new PlayerEventDetectorFactory(mgcpPackageNode.getTextContent(), mgcpEventNode
//							.getTextContent(), Boolean.parseBoolean(onEndpointNode.getTextContent()), strvsEventType
//							.get(eventTypeNode.getTextContent())));
				}

			}

//			medConfimpl.setPlayerDetFactList(detectorList);
//			medConfimpl.setPlayerGeneFactList(generatorList);
//			medConfimpl.setPlayer(true);

		}
	}
*/
/*	private void populateRecorder(NodeList recorderNodeList, MediaConfigImpl medConfimpl) {

		for (int i = 0; i < recorderNodeList.getLength(); i++) {

			List<DefaultEventGeneratorFactory> generatorList = new ArrayList<DefaultEventGeneratorFactory>();

			List<RecorderEventDetectorFactory> detectorList = new ArrayList<RecorderEventDetectorFactory>();

			Node recorder = recorderNodeList.item(i);
			NodeList sigAndEves = recorder.getChildNodes();
			for (int j = 0; j < sigAndEves.getLength(); j++) {
				Node sigOrEve = sigAndEves.item(j);

				if (SIGNAL.compareTo(sigOrEve.getNodeName()) == 0) {

					NodeList signalsList = sigOrEve.getChildNodes();
					Node mgcpEventNode = signalsList.item(0);
					Node mgcpPackageNode = signalsList.item(1);
					Node onEndpointNode = signalsList.item(2);

					generatorList.add(new DefaultEventGeneratorFactory(mgcpPackageNode.getTextContent(), mgcpEventNode
							.getTextContent(), Boolean.parseBoolean(onEndpointNode.getTextContent())));

				} else if (EVENT.compareTo(sigOrEve.getNodeName()) == 0) {
					NodeList signalsList = sigOrEve.getChildNodes();
					Node mgcpEventNode = signalsList.item(0);
					Node mgcpPackageNode = signalsList.item(1);
					Node onEndpointNode = signalsList.item(2);
					Node eventTypeNode = signalsList.item(3);

//					detectorList.add(new RecorderEventDetectorFactory(mgcpPackageNode.getTextContent(), mgcpEventNode
//							.getTextContent(), Boolean.parseBoolean(onEndpointNode.getTextContent()), strvsEventType
//							.get(eventTypeNode.getTextContent())));
				}

			}

//			medConfimpl.setRecorderDetFactList(detectorList);
//			medConfimpl.setRecorderGeneFactList(generatorList);
//			medConfimpl.setRecorder(true);

		}
	}
*/
/*	private void populateSignalDetector(NodeList sigDetNodeList, MediaConfigImpl medConfimpl) {
		for (int i = 0; i < sigDetNodeList.getLength(); i++) {

			List<DefaultEventGeneratorFactory> generatorList = new ArrayList<DefaultEventGeneratorFactory>();

			List<SignalDetectorEventDetectorFactory> detectorList = new ArrayList<SignalDetectorEventDetectorFactory>();

			Node sigDet = sigDetNodeList.item(i);
			NodeList sigAnEves = sigDet.getChildNodes();
			for (int j = 0; j < sigAnEves.getLength(); j++) {
				Node sigOrEve = sigAnEves.item(j);

				if (SIGNAL.compareTo(sigOrEve.getNodeName()) == 0) {

					NodeList signalsList = sigOrEve.getChildNodes();
					Node mgcpEventNode = signalsList.item(0);
					Node mgcpPackageNode = signalsList.item(1);
					Node onEndpointNode = signalsList.item(2);

					generatorList.add(new DefaultEventGeneratorFactory(mgcpPackageNode.getTextContent(), mgcpEventNode
							.getTextContent(), Boolean.parseBoolean(onEndpointNode.getTextContent())));

				} else if (EVENT.compareTo(sigOrEve.getNodeName()) == 0) {
					NodeList signalsList = sigOrEve.getChildNodes();
					Node mgcpEventNode = signalsList.item(0);
					Node mgcpPackageNode = signalsList.item(1);
					Node onEndpointNode = signalsList.item(2);
					Node eventTypeNode = signalsList.item(3);

//					detectorList.add(new SignalDetectorEventDetectorFactory(mgcpPackageNode.getTextContent(),
//							mgcpEventNode.getTextContent(), Boolean.parseBoolean(onEndpointNode.getTextContent()),
//							strvsEventType.get(eventTypeNode.getTextContent())));					
					
				}

			}
//			medConfimpl.setSigDeteEveDetFactList(detectorList);
//			medConfimpl.setSigDeteEveGeneFactList(generatorList);
//			medConfimpl.setSignaldetector(true);
		}
	}
*/
/*	protected MediaConfigImpl parse(MgcpWrapper mgcpWrapper, InputStream stream) throws ParserConfigurationException,
			SAXException, IOException {
		MediaConfigImpl medConfimpl = null;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		Document document = builder.parse(stream);

		Element root = document.getDocumentElement();

		// Parameter
		NodeList params = root.getElementsByTagName(PARAMETER);

		ParametersImpl parameters = new ParametersImpl();

		for (int i = 0; i < params.getLength(); i++) {
			Node n = params.item(i);
			NodeList childs = n.getChildNodes();
			String key = childs.item(0).getTextContent();
			String value = childs.item(1).getTextContent();
			parameters.put(strvsParameter.get(key), value);

		}

//		SupportedFeaturesImpl suppfetImpl = new SupportedFeaturesImpl();
//		suppfetImpl.setParameter(parameters.keySet());
//
//		medConfimpl = new MediaConfigImpl(mgcpWrapper);
//		medConfimpl.setParameters(parameters);
//		
//		medConfimpl.setSupportedFeatures(suppfetImpl);

		// Player
		NodeList playerNodeList = root.getElementsByTagName(PLAYER);
		if (playerNodeList.getLength() > 0) {
			populatePlayer(playerNodeList, medConfimpl);
		}

		// Recorder
		NodeList recorderNodeList = root.getElementsByTagName(RECORDER);
		if (recorderNodeList.getLength() > 0) {
			populateRecorder(recorderNodeList, medConfimpl);
		}

		// Signal-Detector

		NodeList sigDetNodeList = root.getElementsByTagName(SIGNAL_DETECTOR);
		if (sigDetNodeList.getLength() > 0) {
			populateSignalDetector(sigDetNodeList, medConfimpl);
		}

		return medConfimpl;
	}

	public String serialize(MediaConfigImpl mediaConfig) {
		StringBuffer b = new StringBuffer();
		b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		b.append(LINE_SEPARATOR);
		b.append("<");
		b.append(RESOURCE_CONTAINER);
		b.append(">");
		b.append(LINE_SEPARATOR);

//		if (mediaConfig.getParameters() != null) {
//			for (Parameter p : mediaConfig.getParameters().keySet()) {
//				b.append("<").append(PARAMETER).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(KEY).append(">");
//
//				b.append(p.toString());
//
//				b.append("</").append(KEY).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(VALUE).append(">");
//
//				b.append(mediaConfig.getParameters().get(p).toString());
//
//				b.append("</").append(VALUE).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("</").append(PARAMETER).append(">");
//
//				b.append(LINE_SEPARATOR);
//
//			}
//		}

//		if (mediaConfig.isPlayer()) {
//			b.append("<").append(PLAYER).append(">");
//			b.append(LINE_SEPARATOR);
//			for (DefaultEventGeneratorFactory d : mediaConfig.getPlayerGeneFactList()) {
//				b.append("<").append(SIGNAL).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(MGCP_EVENT).append(">");
//				b.append(d.getEventName());
//				b.append("</").append(MGCP_EVENT).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(MGCP_PACKAGE).append(">");
//				b.append(d.getPkgName());
//				b.append("</").append(MGCP_PACKAGE).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(ON_ENDPOINT).append(">");
//				b.append(d.isOnEndpoint());
//				b.append("</").append(ON_ENDPOINT).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("</").append(SIGNAL).append(">");
//				b.append(LINE_SEPARATOR);
//
//			}
//
//			if (mediaConfig.getPlayerDetFactList() != null) {
//				for (PlayerEventDetectorFactory d : mediaConfig.getPlayerDetFactList()) {
//					b.append("<").append(EVENT).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("<").append(MGCP_EVENT).append(">");
//					b.append(d.getEventName());
//					b.append("</").append(MGCP_EVENT).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("<").append(MGCP_PACKAGE).append(">");
//					b.append(d.getPkgName());
//					b.append("</").append(MGCP_PACKAGE).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("<").append(ON_ENDPOINT).append(">");
//					b.append(d.isOnEndpoint());
//					b.append("</").append(ON_ENDPOINT).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("<").append(MEDIA_EVENT).append(">");
//					b.append(d.getMediaEventType().toString());
//					b.append("</").append(MEDIA_EVENT).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("</").append(EVENT).append(">");
//					b.append(LINE_SEPARATOR);
//
//				}
//			}
//			b.append("</").append(PLAYER).append(">");
//			b.append(LINE_SEPARATOR);
//		}
//
//		if (mediaConfig.isRecorder()) {
//			b.append("<").append(RECORDER).append(">");
//			b.append(LINE_SEPARATOR);
//			for (DefaultEventGeneratorFactory d : mediaConfig.getRecorderGeneFactList()) {
//				b.append("<").append(SIGNAL).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(MGCP_EVENT).append(">");
//				b.append(d.getEventName());
//				b.append("</").append(MGCP_EVENT).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(MGCP_PACKAGE).append(">");
//				b.append(d.getPkgName());
//				b.append("</").append(MGCP_PACKAGE).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(ON_ENDPOINT).append(">");
//				b.append(d.isOnEndpoint());
//				b.append("</").append(ON_ENDPOINT).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("</").append(SIGNAL).append(">");
//				b.append(LINE_SEPARATOR);
//
//			}
//
//			if (mediaConfig.getRecorderDetFactList() != null) {
//				for (RecorderEventDetectorFactory d : mediaConfig.getRecorderDetFactList()) {
//					b.append("<").append(EVENT).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("<").append(MGCP_EVENT).append(">");
//					b.append(d.getEventName());
//					b.append("</").append(MGCP_EVENT).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("<").append(MGCP_PACKAGE).append(">");
//					b.append(d.getPkgName());
//					b.append("</").append(MGCP_PACKAGE).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("<").append(ON_ENDPOINT).append(">");
//					b.append(d.isOnEndpoint());
//					b.append("</").append(ON_ENDPOINT).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("<").append(MEDIA_EVENT).append(">");
//					b.append(d.getMediaEventType().toString());
//					b.append("</").append(MEDIA_EVENT).append(">");
//					b.append(LINE_SEPARATOR);
//
//					b.append("</").append(EVENT).append(">");
//					b.append(LINE_SEPARATOR);
//
//				}
//			}
//			b.append("</").append(RECORDER).append(">");
//			b.append(LINE_SEPARATOR);
//		}
//
//		if (mediaConfig.isSignaldetector()) {
//			b.append("<").append(SIGNAL_DETECTOR).append(">");
//			b.append(LINE_SEPARATOR);
//
//			for (SignalDetectorEventDetectorFactory d : mediaConfig.getSigDeteEveDetFactList()) {
//				b.append("<").append(EVENT).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(MGCP_EVENT).append(">");
//				b.append(d.getEventName());
//				b.append("</").append(MGCP_EVENT).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(MGCP_PACKAGE).append(">");
//				b.append(d.getPkgName());
//				b.append("</").append(MGCP_PACKAGE).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(ON_ENDPOINT).append(">");
//				b.append(d.isOnEndpoint());
//				b.append("</").append(ON_ENDPOINT).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("<").append(MEDIA_EVENT).append(">");
//				b.append(d.getMediaEventType().toString());
//				b.append("</").append(MEDIA_EVENT).append(">");
//				b.append(LINE_SEPARATOR);
//
//				b.append("</").append(EVENT).append(">");
//				b.append(LINE_SEPARATOR);
//
//			}
//
//			b.append("</").append(SIGNAL_DETECTOR).append(">");
//			b.append(LINE_SEPARATOR);
//		}
//
//		b.append("</").append(RESOURCE_CONTAINER).append(">");
		return b.toString();
	}
 */ 
}
