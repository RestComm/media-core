package org.mobicents.media.server.ctrl.mgcp;

import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.AuditEndpoint;
import jain.protocol.ip.mgcp.message.AuditEndpointResponse;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.util.concurrent.Callable;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * 
 * @author amit bhayani
 * 
 */
public class AuditEndpointAction implements Callable {

    private AuditEndpoint auep;
    private MgcpController controller;
    private String domainName = null;
    private static Logger logger = Logger.getLogger(AuditConnectionAction.class);

    protected AuditEndpointAction(MgcpController controller, AuditEndpoint req) {
        this.controller = controller;
        this.auep = req;

        this.domainName = controller.getBindAddress() + ":"
                + controller.getPort();
    }

    public JainMgcpResponseEvent call() throws Exception {
        InfoCode[] infCodes = auep.getRequestedInfo();
        int txID = auep.getTransactionHandle();
        String localName = auep.getEndpointIdentifier().getLocalEndpointName();

        if (logger.isDebugEnabled()) {
            logger.debug("AUEP Request TX= " + txID + ", endpt ="
                    + auep.getEndpointIdentifier());
            StringBuffer s = new StringBuffer();
            for (InfoCode infCd : infCodes) {
                s.append(infCd.toString());
                s.append(",");
            }
            logger.debug("InfoCode[] = " + s.toString());
        }

        if (localName.contains("$")) {
            if (logger.isEnabledFor(Level.WARN)) {
                logger.warn("TX = "
                        + txID
                        + ", The endpoint name is underspecified with 'any' wildcard, Response code: "
                        + ReturnCode.ENDPOINT_UNKNOWN);
            }
            return new AuditEndpointResponse(auep.getSource(),
                    ReturnCode.Endpoint_Unknown);
        }

        Endpoint[] endpoints = null;
        if (localName.contains("*")) {
            // retrieve all Endpoints that match this localName and return back the AUEP Response. Ignore the InfoCode[]
            // if any passed.
            // lookup endpoints
            try {
                endpoints = controller.getServer().lookupall(localName);
            } catch (ResourceUnavailableException e) {
                if (logger.isEnabledFor(Level.ERROR)) {
                    logger.error("TX = " + txID
                            + ", There is no free endpoint: " + localName
                            + ", ResponseCode: " + ReturnCode.ENDPOINT_UNKNOWN);
                }
                return new AuditEndpointResponse(auep.getSource(),
                        ReturnCode.Endpoint_Unknown);
            }

            if (endpoints == null) {
                return new AuditEndpointResponse(auep.getSource(),
                        ReturnCode.Endpoint_Unknown);
            }

            EndpointIdentifier[] endpointIdentifierList = new EndpointIdentifier[endpoints.length];
            for (int i = 0; i < endpoints.length; i++) {
                endpointIdentifierList[i] = new EndpointIdentifier(endpoints[i].getLocalName(), this.domainName);
            }

            return new AuditEndpointResponse(auep.getSource(),
                    ReturnCode.Transaction_Executed_Normally,
                    endpointIdentifierList);
        }

        Endpoint endpoint = null;
        // Audit specific endpoint.
        try {
            endpoint = controller.getServer().lookup(localName, true);
        } catch (ResourceUnavailableException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("TX = " + txID + ", There is no free endpoint: "
                        + localName + ", ResponseCode: "
                        + ReturnCode.ENDPOINT_UNKNOWN);
            }
            return new AuditEndpointResponse(auep.getSource(),
                    ReturnCode.Endpoint_Unknown);
        }
        AuditEndpointResponse auepResp = new AuditEndpointResponse(auep.getSource(),
                ReturnCode.Transaction_Executed_Normally);
        if (infCodes != null) {

/*            Request request = controller.requests.get(endpoint.getLocalName());

            for (InfoCode infoCode : infCodes) {
                switch (infoCode.getInfoCode()) {
                    case InfoCode.REQUESTED_EVENTS:
                        if (request != null) {
                            auepResp.setRequestedEvents(request.getRequestedEvents());
                        }
                        break;
                    case InfoCode.DIGIT_MAP:
                        //TODO : Digit Map is not yet implemented
                        break;
                    case InfoCode.SIGNAL_REQUESTS:
                        if (request != null) {
                            auepResp.setSignalRequests(request.getSignalRequests());
                        }
                        break;
                    case InfoCode.REQUEST_IDENTIFIER:
                        if (request != null) {
                            auepResp.setRequestIdentifier(request.getRequestIdentifier());
                        }
                        break;
                    case InfoCode.QUARANTINE_HANDLING:
                        //Not implemented
                        break;
                    case InfoCode.DETECT_EVENTS:
                        //TODO : Need to have reference to Detected Events
                        break;
                    case InfoCode.NOTIFIED_ENTITY:
                        auepResp.setNotifiedEntity(this.controller.getNotifiedEntity());
                        break;
                    case InfoCode.CONNECTION_IDENTIFIER:
                        					Collection<ConnectionActivity> connActivities = this.controller.getActivities(endpoint.getLocalName());
                        if(connActivities.size() > 0 ){
                        ConnectionIdentifier[] connIdArr = new  ConnectionIdentifier[connActivities.size()];
                        int count=0;
                        for(ConnectionActivity connAct : connActivities){
                        connIdArr[count++] = new ConnectionIdentifier(connAct.getID());
                        }
                        auepResp.setConnectionIdentifiers(connIdArr);
                        }

                        break;
                    case InfoCode.OBSERVED_EVENTS:
                        //TODO : Need to have reference to Observed Events for an Endpoint
                        break;
                    case InfoCode.EVENT_STATES:
                        //TODO : Events don't maintain states.
                        break;
                    case InfoCode.BEARER_INFORMATION:
                        //TODO : Bearer Information is not implemented
                        break;
                    case InfoCode.RESTART_METHOD:
                        auepResp.setRestartMethod(RestartMethod.Restart);
                        break;
                    case InfoCode.RESTART_DELAY:
                        auepResp.setRestartDelay(0);
                        break;
                    case InfoCode.REASON_CODE:
                        auepResp.setReasonCode(ReasonCode.Endpoint_State_Is_Nominal);
                        break;
                    case InfoCode.CAPABILITIES:
                        //TODO:
                        break;
                }
            } */
        }
        return auepResp;
    }
}
