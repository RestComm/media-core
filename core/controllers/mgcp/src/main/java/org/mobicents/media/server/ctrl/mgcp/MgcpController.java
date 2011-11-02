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

import jain.protocol.ip.mgcp.DeleteProviderException;
import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.AuditConnection;
import jain.protocol.ip.mgcp.message.AuditEndpoint;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;

import javolution.util.FastMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mobicents.media.server.ctrl.mgcp.signal.RequestExecutor;
import org.mobicents.media.server.ctrl.mgcp.signal.RequestExecutors;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.protocols.mgcp.stack.ExtendedJainMgcpProvider;
import org.mobicents.protocols.mgcp.stack.JainMgcpStackImpl;

/**
 * @author amit bhayani
 * @author baranowb
 * @author kulikov
 */
public class MgcpController implements JainMgcpListener {
    
    private static int GENERATOR;
    
    private JainMgcpProvider mgcpProvider;
    private ExtendedJainMgcpProvider extMgcpProvider;
    
    private JainMgcpStackImpl mgcpStack;
    
    private InetAddress inetAddress = null;
    private String bindAddress = null;
    private int port = 2727;
    
    private MediaServer server;
    
    private FastMap<String, Call> calls = new FastMap<String, Call>();    
    private NotifiedEntity notifiedEntity;

    private RequestExecutors requestExecutors;
    protected FastMap<String, EndpointActivity> endpoints = new FastMap();
    
    protected Activities activities;
    private static final Logger logger = Logger.getLogger(MgcpController.class);
    
/*    protected int crcxCount;
    protected int dlcxCount;

    protected int crcxReqCount;
    protected int dlcxReqCount;
*/    
    public MgcpController() {
        activities = new Activities(this);
    }

    public MediaServer getServer() {
        return server;
    }

    public void setServer(MediaServer server) {
        this.server = server;
    }

    public NotifiedEntity getNotifiedEntity() {
        return notifiedEntity;
    }

    public void setDefaultNotifiedEntity(String value) {
        String[] tokens = value.split(":");
        int remotePort = this.port;
        if (tokens.length == 2) {
            remotePort = Integer.parseInt(tokens[1]);
        }
        tokens = tokens[0].split("@");
        this.notifiedEntity = new NotifiedEntity(tokens[0], tokens[1], remotePort);
    }

    public String getBindAddress() {
        return this.bindAddress;
    }

    public void setBindAddress(String bindAddress) throws UnknownHostException {
        this.bindAddress = bindAddress;
        this.inetAddress = InetAddress.getByName(bindAddress);
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void create() {
        logger.info("Starting MGCP Controller module for MMS");
    }

    /**
     * Starts MGCP controller.
     * 
     * @throws java.lang.Exception
     */
    public void start() throws Exception {

        // jainFactory = JainIPFactory.getInstance();
        // jainFactory.setPathName("org.mobicents");

        mgcpStack = new JainMgcpStackImpl(this.inetAddress, this.port);

        mgcpProvider = mgcpStack.createProvider();
        extMgcpProvider = (ExtendedJainMgcpProvider) mgcpProvider;
        mgcpProvider.addJainMgcpListener(this);

        this.port = mgcpStack.getPort();
        
        requestExecutors = new RequestExecutors(5);
        logger.info("Started MGCP Controller module for MMS");
    // new Thread(new CallMon()).start();
    }

    /**
     * Stops MGCP controller.
     * 
     * @throws java.lang.Exception
     */
    public void stop() {
        logger.info("Stoping MGCP Controller module for MMS. Listening at IP " + this.inetAddress + " port " + this.port);
        mgcpProvider.removeJainMgcpListener(this);
        try {
            mgcpStack.deleteProvider(mgcpProvider);
        } catch (DeleteProviderException e) {
        }
    }

    public void destroy() {
        logger.info("Stopped MGCP Controller module for MMS");
    }

    public JainMgcpStackImpl getMgcpSatck() {
        return this.mgcpStack;
    }

    public JainMgcpProvider getMgcpProvider() {
        return this.mgcpProvider;
    }

    public ExtendedJainMgcpProvider getExtendedMgcpProvider() {
        return this.extMgcpProvider;
    }

    /**
     * Gets the first free request executor.
     * 
     * @return request executor object.
     * @throws java.lang.Exception
     */
    public RequestExecutor getRequestExecutor() throws Exception {
        return requestExecutors.poll();
    }
    
    /**
     * Processes a Command Event object received from a JainMgcpProvider.
     * 
     * @param evt -
     *            The JAIN MGCP Command Event Object that is to be processed.
     */
    public void processMgcpCommandEvent(JainMgcpCommandEvent evt) {
        // define action to be performed
        Callable<JainMgcpResponseEvent> action = null;

        // construct object implementing requested action using
        // object identifier
        int eventID = evt.getObjectIdentifier();
        switch (eventID) {
            case Constants.CMD_CREATE_CONNECTION:
                action = new CreateConnectionAction(this, (CreateConnection) evt);
                break;
            case Constants.CMD_MODIFY_CONNECTION:
                action = new ModifyConnectionAction(this, (ModifyConnection) evt);
                break;
            case Constants.CMD_DELETE_CONNECTION:
                action = new DeleteConnectionAction(this, (DeleteConnection) evt);
                break;
            case Constants.CMD_NOTIFICATION_REQUEST:
                action = new NotificationRequestAction1(this, (NotificationRequest) evt);
                break;
            case Constants.CMD_AUDIT_CONNECTION:
            	action = new AuditConnectionAction(this, (AuditConnection)evt );
            	break;
            case Constants.CMD_AUDIT_ENDPOINT:
            	action = new AuditEndpointAction(this, (AuditEndpoint)evt );
            	break;            	
            default:
                logger.error("Unknown message type: " + eventID);
                return;
        }

        // try to perform action and send response back.
        try {
            JainMgcpResponseEvent response = action.call();
            mgcpProvider.sendMgcpEvents(new JainMgcpEvent[]{response});
        } catch (Exception e) {
            logger.warn("Unexpected error during processing,Caused by ", e);
        }
    }

    /**
     * Processes a Response Event object (acknowledgment to a Command Event
     * object) received from a JainMgcpProvider.
     * 
     * @param evt -
     *            The JAIN MGCP Response Event Object that is to be processed.
     */
    public void processMgcpResponseEvent(JainMgcpResponseEvent evt) {
    }

    public int nextID() {
        if (GENERATOR == Integer.MAX_VALUE) GENERATOR = 0;
        return ++GENERATOR;
    }
}
