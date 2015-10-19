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
package org.mobicents.media.server.mgcp.tx.cmd;

import java.io.IOException;
import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.controller.MgcpCall;
import org.mobicents.media.server.mgcp.controller.MgcpConnection;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.MgcpResponse;
import org.mobicents.media.server.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.mgcp.params.LocalConnectionOptions;
import org.mobicents.media.server.mgcp.tx.Action;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.TaskChain;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.utils.Text;
import org.apache.log4j.Logger;
/**
 * Modify connection command.
 * 
 * @author Oifa Yulian
 */
public class ModifyConnectionCmd extends Action {
    //response strings
    private final static Text CALLID_MISSING = new Text("Missing call identifier");
    private final static Text UNKNOWN_CALL_IDENTIFIER = new Text("Could not find this call with specified identifier");
    private final static Text CONNECTIONID_EXPECTED = new Text("Connection identifier was not specified");
    private final static Text SDP_NEGOTIATION_FAILED = new Text("SDP_NEGOTIATION_FAILED");

    private final static Text SUCCESS= new Text("Success");
    
    private MgcpRequest request;
    
    private Parameter connectionID;
    private TaskChain handler;

    //error code and message
    private int code;
    private Text message;
    
    private MgcpConnection mgcpConnection = null;    
    //local connection options
    private LocalConnectionOptions lcOptions = new LocalConnectionOptions();
    
    private final static Logger logger = Logger.getLogger(ModifyConnectionCmd.class);    
    
    public ModifyConnectionCmd(Scheduler scheduler) {
    	handler = new TaskChain(1,scheduler);
        
        Modifier modifier = new Modifier();
        
        handler.add(modifier);
        
        ErrorHandler errorHandler = new ErrorHandler();
        
        this.setActionHandler(handler);
        this.setRollbackHandler(errorHandler);        
    }
    
    private class Modifier extends Task {
        
        public Modifier() {
            super();
        }

        public int getQueueNumber()
        {
        	return Scheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
        	request = (MgcpRequest) getEvent().getMessage();
            
            Parameter callID = request.getParameter(Parameter.CALL_ID);
            if (callID == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, CALLID_MISSING);
            }
            
            //modify local connection options
            Parameter l = request.getParameter(Parameter.LOCAL_CONNECTION_OPTIONS);
            if (l != null) {
                lcOptions.setValue(l.getValue());
            } else {
                lcOptions.setValue(null);
            }
            
            //getting call
            MgcpCall call = transaction().getCall(callID.getValue().hexToInteger());
            if (call == null) {
                throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID, UNKNOWN_CALL_IDENTIFIER);
            }
            
            connectionID = request.getParameter(Parameter.CONNECTION_ID);
            if (connectionID == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, CONNECTIONID_EXPECTED);
            }
            
            try {
                mgcpConnection = call.getMgcpConnection(connectionID.getValue().hexToInteger());
            } catch (Exception e) {
                throw new MgcpCommandException(MgcpResponseCode.CONNECTION_WAS_DELETED, new Text("Unknown connectionidentifier, probably it was deleted"));
            }
            
            if(mgcpConnection==null)
            	throw new MgcpCommandException(MgcpResponseCode.CONNECTION_WAS_DELETED, new Text("Unknown connectionidentifier, probably it was deleted"));
            
            //set SDP if requested
            Parameter sdp = request.getParameter(Parameter.SDP);
            Parameter mode = request.getParameter(Parameter.MODE);
            
            if (sdp != null) {
                try {
                    mgcpConnection.setOtherParty(sdp.getValue());
                } catch (IOException e) {
                	logger.error("Could not set remote peer", e);
                    throw new MgcpCommandException(MgcpResponseCode.UNSUPPORTED_SDP, SDP_NEGOTIATION_FAILED);
                }
            }
            
            if (mode != null) {
                try {
                    mgcpConnection.setMode(mode.getValue());
                } catch (ModeNotSupportedException e) {
                    throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE, new Text("problem with mode"));
                }
            }
            
            mgcpConnection.setDtmfClamp(lcOptions.getDtmfClamp());
            
            MgcpEvent evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
            MgcpResponse response = (MgcpResponse) evt.getMessage();
            response.setResponseCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED);
            response.setResponseString(SUCCESS);
            response.setTxID(transaction().getId());

            if (connectionID != null) {
                response.setParameter(Parameter.CONNECTION_ID, connectionID.getValue());
            }

            Text descriptor=mgcpConnection.getDescriptor();
            if(descriptor!=null)
            	response.setParameter(Parameter.SDP, mgcpConnection.getDescriptor());
            
            try {
                transaction().getProvider().send(evt);
            } catch (IOException e) {
            	logger.error(e);
            } finally {
                evt.recycle();
            }
            
            return 0;
        }
    }    

    private class ErrorHandler extends Task {

        public ErrorHandler() {
            super();
        }
        
        @Override
        public int getQueueNumber() {
        	return Scheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            code = ((MgcpCommandException)transaction().getLastError()).getCode();
            message = ((MgcpCommandException)transaction().getLastError()).getErrorMessage();
            
            MgcpEvent evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
            MgcpResponse response = (MgcpResponse) evt.getMessage();
            response.setResponseCode(code);
            response.setResponseString(message);
            response.setTxID(transaction().getId());

            if (connectionID != null) {
                response.setParameter(Parameter.CONNECTION_ID, connectionID.getValue());
            }

            try {
                transaction().getProvider().send(evt);
            } catch (IOException e) {
            	logger.error(e);
            } finally {
                evt.recycle();
            } 
            
            return 0;
        }
        
    }
    
}
