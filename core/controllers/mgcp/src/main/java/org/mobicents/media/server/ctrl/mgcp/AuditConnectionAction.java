/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.ctrl.mgcp;

import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.AuditConnection;
import jain.protocol.ip.mgcp.message.AuditConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionParm;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.RegularConnectionParm;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.util.concurrent.Callable;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;

/**
 * 
 * @author amit bhayani
 * 
 */
public class AuditConnectionAction implements Callable {

    private AuditConnection aucx;
    private MgcpController controller;
    private MgcpUtils utils = new MgcpUtils();
    private static Logger logger = Logger.getLogger(AuditConnectionAction.class);

    protected AuditConnectionAction(MgcpController controller, AuditConnection req) {
        this.controller = controller;
        this.aucx = req;
    }

    public JainMgcpResponseEvent call() throws Exception {

        InfoCode[] infCodes = aucx.getRequestedInfo();
        ConnectionIdentifier connectionID = this.aucx.getConnectionIdentifier();
        int txID = aucx.getTransactionHandle();
        String localName = aucx.getEndpointIdentifier().getLocalEndpointName();

        if (logger.isDebugEnabled()) {
            logger.debug("AUCX Request TX= " + aucx.getTransactionHandle() + ", connId = " + aucx.getConnectionIdentifier() + ", endpt =" + aucx.getEndpointIdentifier());
            StringBuffer s = new StringBuffer();
            for (InfoCode infCd : infCodes) {
                s.append(infCd.toString());
                s.append(",");
            }
            logger.debug("InfoCode[] = " + s.toString());
        }

        if (localName.contains("*") || localName.contains("$")) {
            if (logger.isEnabledFor(Level.WARN)) {
                logger.warn("TX = " + txID + ", The endpoint name is underspecified with 'all off/any' wildcard, Response code: " + ReturnCode.ENDPOINT_UNKNOWN);
            }
            return new AuditConnectionResponse(aucx.getSource(), ReturnCode.Endpoint_Unknown);
        }

        Endpoint endpoint = null;

        try {
            // Endpoint must be in use for auditing
            endpoint = controller.getServer().lookup(localName, false);
            if (logger.isEnabledFor(Level.WARN)) {
                logger.warn("TX = " + txID + ", The endpoint name is not in use, Response code: " + ReturnCode.ENDPOINT_NOT_READY);
            }
            return new AuditConnectionResponse(aucx.getSource(), ReturnCode.Endpoint_Not_Ready);
        } catch (Exception e) {
            // This is OK
        }

        // Check if Connection is live
        ConnectionActivity connectionActivity = null;//controller.getActivity(endpoint.getLocalName(), connectionID.toString());

        if (connectionActivity == null) {
            if (logger.isEnabledFor(Level.WARN)) {
                logger.warn("No Connection found for ConnectionIdentifier " + connectionID + " Sending back" + ReturnCode.Connection_Was_Deleted.toString());
            }
            return new AuditConnectionResponse(aucx.getSource(), ReturnCode.Connection_Was_Deleted);
        }

        // If no info was requested and the EndpointId is valid, the gateway simply checks that the connection exists,
        // and if so returns a positive acknowledgement.
        if (infCodes.length == 0) {
            if (logger.isEnabledFor(Level.INFO)) {
                logger.info("TX = " + txID + ", The endpoint name is in use and InfoCode[] length is zero, Response code: " + ReturnCode.Transaction_Executed_Normally);
            }
            return new AuditConnectionResponse(aucx.getSource(), ReturnCode.Transaction_Executed_Normally);
        }

        Connection connection = connectionActivity.getMediaConnection();

        // Info present, lets fill corresponding information
        AuditConnectionResponse aucxResp = new AuditConnectionResponse(aucx.getSource(),
                ReturnCode.Transaction_Executed_Normally);
        for (InfoCode infCode : infCodes) {
            switch (infCode.getInfoCode()) {
                case InfoCode.CALL_IDENTIFIER:
//                    aucxResp.setCallIdentifier(new CallIdentifier(connectionActivity.getCall().getID()));
                    break;
                case InfoCode.NOTIFIED_ENTITY:
                    aucxResp.setNotifiedEntity(controller.getNotifiedEntity());
                    break;
                case InfoCode.LOCAL_CONNECTION_OPTIONS:
                    // TODO
                    break;
                case InfoCode.CONNECTION_MODE:
                    // TODO : How do we know if Mode is for Audio or Video?
                    aucxResp.setMode(utils.getMode(connection.getMode(MediaType.AUDIO)));
                    break;
                case InfoCode.REMOTE_CONNECTION_DESCRIPTOR:
                    aucxResp.setRemoteConnectionDescriptor(new ConnectionDescriptor(connection.getRemoteDescriptor()));
                    break;
                case InfoCode.LOCAL_CONNECTION_DESCRIPTOR:
                    aucxResp.setLocalConnectionDescriptor(new ConnectionDescriptor(connection.getLocalDescriptor()));
                    break;
                case InfoCode.CONNECTION_PARAMETERS:

                    ConnectionParm[] parms = new ConnectionParm[3];
                    parms[0] = new RegularConnectionParm(RegularConnectionParm.OCTETS_RECEIVED, (int) connection.getBytesReceived());
                    parms[1] = new RegularConnectionParm(RegularConnectionParm.OCTETS_SENT, (int) connection.getBytesTransmitted());
                    parms[2] = new RegularConnectionParm(RegularConnectionParm.JITTER,
                            (int) (connection.getJitter() * 1000));

                    aucxResp.setConnectionParms(parms);

                    break;

                default:
                    if (logger.isEnabledFor(Level.WARN)) {
                        logger.warn("The InfoCode is not recognized " + infCode);
                    }
                    break;
            }
        }

        return aucxResp;
    }
}
