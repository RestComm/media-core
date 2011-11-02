/**
 * 
 */
package org.mobicents.media.server.testsuite.general.cnf;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.NotifyResponse;
import jain.protocol.ip.mgcp.message.parms.ConflictingParameterException;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedAction;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.logging.Level;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import org.apache.log4j.Logger;

import org.mobicents.media.server.testsuite.general.AbstractCall;
import org.mobicents.media.server.testsuite.general.AbstractTestCase;
import org.mobicents.media.server.testsuite.general.CallState;
import org.mobicents.media.server.testsuite.general.ann.AnnCall;
import org.mobicents.media.server.testsuite.general.ann.AnnCallState;
import org.mobicents.media.server.testsuite.general.rtp.RtpPacket;
import org.mobicents.media.server.testsuite.general.rtp.RtpSocketFactory;

/**
 * @author baranowb
 * 
 */
public class CnfCall extends AbstractCall {

	protected transient Logger logger = Logger.getLogger(CnfCall.class);
	private AnnCallState localFlowState = AnnCallState.INITIAL;
	private String HELLO_WORLD = "";

	private int numberOfConnections = 1;
	private int createdConnections = 0;
	private RequestIdentifier ri = null;
	private HashMap<String, org.mobicents.media.server.testsuite.general.rtp.RtpSocket> connectionIdToSocket = new HashMap<String, org.mobicents.media.server.testsuite.general.rtp.RtpSocket>();
	private HashMap<String, List<RtpPacket>> connectionIdToRtpData = new HashMap<String, List<RtpPacket>>();

	private org.mobicents.media.server.testsuite.general.rtp.RtpSocket tenativeSocket;

	public CnfCall(AbstractTestCase testCase, String fileToPlay)
			throws IOException {
		super(testCase);
		super.endpointName = "/mobicents/media/cnf/$";
		this.HELLO_WORLD = fileToPlay;

	}

	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.media.server.testsuite.general.AbstractCall#performDataDumps
	 * (boolean)
	 */
	@Override
	protected void performDataDumps(boolean isEnding) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.mobicents.media.server.testsuite.general.AbstractCall#
	 * releaseResourcesOnTermination()
	 */
	@Override
	protected void releaseResourcesOnTermination() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.AbstractCall#start()
	 */
	@Override
	public void start() {
		try {
			sendCrCx(null, true);

			this.setLocalFlowState(AnnCallState.SENT_CRCX);
			setState(CallState.INITIAL);
			super.onStart();
		} catch (Exception e) {
			e.printStackTrace();
			this.setLocalFlowState(AnnCallState.TERMINATED);
			super.setState(CallState.IN_ERROR);
		}
	}

	private void sendCrCx(EndpointIdentifier ei, boolean addCall)
			throws IOException, ConflictingParameterException {
		initSocket();
		if (ei == null)
			ei = new EndpointIdentifier(super.endpointName, super.testCase
					.getServerJbossBindAddress().getHostAddress()
					+ ":"
					+ super.testCase.getCallDisplayInterface().getRemotePort());

		CreateConnection crcx = new CreateConnection(this, this.callIdentifier,
				ei, ConnectionMode.SendOnly);
		// int localPort = this.datagramChannel.socket().getLocalPort();
		int localPort = tenativeSocket.getLocalPort();
		String sdp = super.getLocalDescriptor(localPort);
		// System.err.println(sdp);
		crcx.setRemoteConnectionDescriptor(new ConnectionDescriptor(sdp));
		crcx.setTransactionHandle(this.provider.getUniqueTransactionHandler());
		if (addCall)
			super.testCase.addCall(crcx, this);
		super.provider.sendMgcpEvents(new JainMgcpEvent[] { crcx });

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.AbstractCall#stop()
	 */
	@Override
	public void stop() {
		cancelTimeoutHandle();
		if (ri != null) {
			super.testCase.removeCall(ri.toString());
		}

		if (this.state == CallState.IN_ERROR
				|| this.state == CallState.TIMED_OUT) {
			this.setLocalFlowState(AnnCallState.TERMINATED);
		} else {

			// FIXME: Should in case of forced stop this indicate error?
			super.setState(CallState.ENDED);
			this.setLocalFlowState(AnnCallState.TERMINATED);
		}

		super.onStop();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.media.server.testsuite.general.AbstractCall#timeOut()
	 */
	@Override
	public void timeOut() {
		sendDelete();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.mgcp.stack.JainMgcpExtendedListener#transactionEnded(int)
	 */
	public void transactionEnded(int arg0) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.mgcp.stack.JainMgcpExtendedListener#transactionRxTimedOut
	 * (jain.protocol.ip.mgcp.JainMgcpCommandEvent)
	 */
	public void transactionRxTimedOut(JainMgcpCommandEvent arg0) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.mgcp.stack.JainMgcpExtendedListener#transactionTxTimedOut
	 * (jain.protocol.ip.mgcp.JainMgcpCommandEvent)
	 */
	public void transactionTxTimedOut(JainMgcpCommandEvent arg0) {
		switch (this.localFlowState) {
		case SENT_CRCX:
			this.setLocalFlowState(AnnCallState.TERMINATED);
			setState(CallState.IN_ERROR);
			break;
		case SENT_ANN:
			// THis is really bad, ... wech
			sendDelete();
			// If it fails, we dont have means to recover do we?
			this.setLocalFlowState(AnnCallState.TERMINATED);
			setState(CallState.IN_ERROR);
			break;
		case SENT_DLCX:
			this.setLocalFlowState(AnnCallState.TERMINATED);
			setState(CallState.IN_ERROR);
			break;

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jain.protocol.ip.mgcp.JainMgcpListener#processMgcpCommandEvent(jain.protocol
	 * .ip.mgcp.JainMgcpCommandEvent)
	 */
	public void processMgcpCommandEvent(JainMgcpCommandEvent mgcpCommand) {
		switch (this.localFlowState) {
		case SENT_ANN:
			if (mgcpCommand instanceof Notify) {
				// here we could do something, instead we respond with 200 and
				// remove
				Notify notify = (Notify) mgcpCommand;

				ReturnCode rc = ReturnCode.Transaction_Executed_Normally;
				NotifyResponse notifyResponse = new NotifyResponse(this, rc);
				notifyResponse.setTransactionHandle(notify
						.getTransactionHandle());
				super.provider
						.sendMgcpEvents(new JainMgcpEvent[] { notifyResponse });

				// lets remove us from call maps
				super.testCase.removeCall(mgcpCommand);
				super.testCase.removeCall(notify.getRequestIdentifier()
						.toString());

				cancelTimeoutHandle();
				sendDelete();
				// stop();
			}
			break;
		default:
			stop();
			sendDelete();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * jain.protocol.ip.mgcp.JainMgcpListener#processMgcpResponseEvent(jain.
	 * protocol.ip.mgcp.JainMgcpResponseEvent)
	 */
	public void processMgcpResponseEvent(JainMgcpResponseEvent mgcpResponse) {
		int code = mgcpResponse.getReturnCode().getValue();
		int objId = mgcpResponse.getObjectIdentifier();
		super.testCase.removeCall(mgcpResponse);
		switch (this.localFlowState) {
		case SENT_CRCX:
			// here we wait for answer, we need to send 200 to invite

			if (objId == Constants.RESP_CREATE_CONNECTION) {
				CreateConnectionResponse ccr = (CreateConnectionResponse) mgcpResponse;
				if (99 < code && code < 200) {
					// its provisional
				} else if (199 < code && code < 300) {
					try {
						// its success
						if (super.endpointIdentifier == null) {
							super.endpointIdentifier = ccr
									.getSpecificEndpointIdentifier();
						}

						this.createdConnections++;
						// store socket

						this.connectToPeer(ccr);

						if (this.numberOfConnections != this.createdConnections) {
							// we must send crcx again, and again ;)
							sendCrCx(super.endpointIdentifier, true);
							break;
						} else {

							ri = provider.getUniqueRequestIdentifier();
							NotificationRequest notificationRequest = new NotificationRequest(
									this, super.endpointIdentifier, ri);
							EventName[] signalRequests = { new EventName(
									PackageName.Announcement, MgcpEvent.ann
											.withParm(HELLO_WORLD), null) };
							notificationRequest
									.setSignalRequests(signalRequests);
							RequestedAction[] actions = new RequestedAction[] { RequestedAction.NotifyImmediately };

							RequestedEvent[] requestedEvents = { new RequestedEvent(
									new EventName(PackageName.Announcement,
											MgcpEvent.oc, null), actions) };
							notificationRequest
									.setRequestedEvents(requestedEvents);
							// notificationRequest.setTransactionHandle(mgcpProvider.getUniqueTransactionHandler());
							NotifiedEntity notifiedEntity = new NotifiedEntity(
									super.testCase.getClientTestNodeAddress()
											.getHostAddress(), super.testCase
											.getClientTestNodeAddress()
											.getHostAddress(), super.testCase
											.getCallDisplayInterface()
											.getLocalPort());
							notificationRequest
									.setNotifiedEntity(notifiedEntity);

							notificationRequest.setTransactionHandle(provider
									.getUniqueTransactionHandler());
							// We dont care
							// super.testCase.addCall(ri, this);
							super.testCase.addCall(notificationRequest, this);
							super.testCase.addCall(ri.toString(), this);
							super.provider
									.sendMgcpEvents(new JainMgcpCommandEvent[] { notificationRequest });

							// IS this wrong? should we wait unitl notification
							// is
							// played?
							setState(CallState.ESTABILISHED);
							this.setLocalFlowState(AnnCallState.SENT_ANN);
						}
					} catch (Exception ex) {
						java.util.logging.Logger.getLogger(
								AnnCall.class.getName()).log(Level.SEVERE,
								null, ex);
						this.setLocalFlowState(AnnCallState.TERMINATED);
						setState(CallState.IN_ERROR);
					}

				} else {

					// ADD ERROR
					this.setLocalFlowState(AnnCallState.TERMINATED);
					setState(CallState.IN_ERROR);
					// FIXME: add error dump
					logger.error("FAILED[" + this.localFlowState
							+ "] ON CRCX RESPONSE: "
							+ buildResponseHeader(mgcpResponse));
				}
			} else {

				// ADD ERROR
				this.setLocalFlowState(AnnCallState.TERMINATED);
				setState(CallState.IN_ERROR);
				// FIXME: add error dump
				logger
						.error("FAILED[" + this.localFlowState
								+ "] ON RESPONSE: "
								+ buildResponseHeader(mgcpResponse));
			}
			break;

		case SENT_ANN:
			if (objId == Constants.RESP_NOTIFICATION_REQUEST) {

				if (99 < code && code < 200) {
					// its provisional
				} else if (199 < code && code < 300) {
					// its success

				} else {
					// its error always?
					// ADD ERROR
					this.setLocalFlowState(AnnCallState.TERMINATED);
					setState(CallState.IN_ERROR);
					// FIXME: add error dump
					logger.error("FAILED[" + this.localFlowState
							+ "] ON RESPONSE: "
							+ buildResponseHeader(mgcpResponse));
				}

			} else {
				this.setLocalFlowState(AnnCallState.TERMINATED);
				setState(CallState.IN_ERROR);
				// FIXME: add error dump
				logger.error("FAILED[" + this.localFlowState
						+ "] ON CRCX RESPONSE: "
						+ buildResponseHeader(mgcpResponse) + " objId1 = "
						+ objId);

			}
			break;

		case SENT_DLCX:

			if (objId == Constants.RESP_DELETE_CONNECTION) {
				if (99 < code && code < 200) {
					// its provisional
				} else if (199 < code && code < 300) {
					// its success
					stop();
				} else {
					// its error always?
					// ADD ERROR
					this.setLocalFlowState(AnnCallState.TERMINATED);
					setState(CallState.IN_ERROR);
					// FIXME: add error dump
					logger.error("FAILED[" + this.localFlowState
							+ "] ON DLCX RESPONSE: "
							+ buildResponseHeader(mgcpResponse));
				}
			} else {
				this.setLocalFlowState(AnnCallState.TERMINATED);
				setState(CallState.IN_ERROR);
				// FIXME: add error dump
				logger.error("FAILED[" + this.localFlowState
						+ "] ON CRCX RESPONSE: "
						+ buildResponseHeader(mgcpResponse) + " objId = "
						+ objId);
			}

			break;
		default:
			logger.error("GOT RESPONSE UNKONWN[" + this.localFlowState
					+ "] ON CRCX RESPONSE: "
					+ buildResponseHeader(mgcpResponse));
		}

	}

	@Override
	public void receive(RtpPacket packet) {
		// TODO Auto-generated method stub

	}

	// //////////////////
	// HELPER METHODS //
	// //////////////////
	protected void initSocket() throws IOException {
		if (this.testCase != null && this.testCase.getSocketFactory() != null) {
			RtpSocketFactory factory = this.testCase.getSocketFactory();
			this.tenativeSocket = factory.createSocket();
			tenativeSocket.addListener(this);

		}
	}

	protected void setLocalFlowState(AnnCallState state) {

		if (this.localFlowState == state) {
			return;
		}

		// FIXME: add more

		this.localFlowState = state;

	}

	private String buildResponseHeader(JainMgcpResponseEvent response) {
		ReturnCode r = response.getReturnCode();
		String s = new String(r.getValue() + " "
				+ response.getTransactionHandle() + " "
				+ (r.getComment() == null ? "" : r.getComment()) + " "
				+ this.getMGCPComand(response.getObjectIdentifier()) + "\n");
		return s;

	}

	private String getMGCPComand(int objectIdentifier) {
		String cmd = null;
		switch (objectIdentifier) {
		case Constants.RESP_CREATE_CONNECTION:
			cmd = "CRCX";
			break;
		case Constants.RESP_DELETE_CONNECTION:
			cmd = "DLCX";
			break;
		case Constants.RESP_NOTIFICATION_REQUEST:
			cmd = "RQNT";
			break;
		default:
			cmd = "UNKNOWN";
			break;
		}
		return cmd;
	}

	private void cancelTimeoutHandle() {
		if (super.getTimeoutHandle() != null) {
			super.getTimeoutHandle().cancel(false);
		}
	}

	private void sendDelete() {
		DeleteConnection dlcx = new DeleteConnection(this,
				super.endpointIdentifier);
		dlcx.setCallIdentifier(this.callIdentifier);
		// dlcx.setConnectionIdentifier(this.allocatedConnection);
		dlcx.setTransactionHandle(provider.getUniqueTransactionHandler());

		super.testCase.addCall(dlcx, this);

		// IS this wrong? should we wait unitl notification is played?
		this.setLocalFlowState(AnnCallState.SENT_DLCX);

		super.provider.sendMgcpEvents(new JainMgcpCommandEvent[] { dlcx });

	}

	private void connectToPeer(CreateConnectionResponse ccr)
			throws SdpParseException, SdpException, UnknownHostException,
			IOException {
		String connectionId = ccr.getConnectionIdentifier().toString();
		this.connectionIdToRtpData
				.put(connectionId, new ArrayList<RtpPacket>());
		this.connectionIdToSocket.put(connectionId, this.tenativeSocket);
		this.tenativeSocket.setConnectionIdentifier(connectionId);
		ConnectionDescriptor cd = ccr.getLocalConnectionDescriptor();

		SessionDescription sessionDesc = super.testCase.getSdpFactory()
				.createSessionDescription(cd.toString());
		String cAddress = sessionDesc.getConnection().getAddress();
		Vector v = sessionDesc.getMediaDescriptions(true);
		MediaDescription md = (MediaDescription) v.get(0);
		int port = md.getMedia().getMediaPort();

		SocketAddress sa = new InetSocketAddress(InetAddress
				.getAllByName(cAddress)[0], port);

		// for now we do that like that this will go away with
		// rtp socket
		tenativeSocket.setPeer(InetAddress.getAllByName(cAddress)[0], port);
		tenativeSocket = null;

	}

	@Override
	protected void performGraphs() {
		// TODO Auto-generated method stub
		
	}

}
