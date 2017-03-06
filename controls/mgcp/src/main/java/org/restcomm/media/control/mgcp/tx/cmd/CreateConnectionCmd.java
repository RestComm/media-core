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
package org.restcomm.media.control.mgcp.tx.cmd;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.TaskChain;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.utils.Text;
import org.restcomm.media.control.mgcp.MgcpEvent;
import org.restcomm.media.control.mgcp.controller.MgcpCall;
import org.restcomm.media.control.mgcp.controller.MgcpConnection;
import org.restcomm.media.control.mgcp.controller.MgcpEndpoint;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.message.Parameter;
import org.restcomm.media.control.mgcp.params.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.tx.Action;
import org.restcomm.media.control.mgcp.tx.Transaction;

/**
 *
 * @author Oifa Yulian
 */
public class CreateConnectionCmd extends Action {
    //response strings
    private final static Text CALLID_MISSING = new Text("Missing call identifier");
    private final static Text MODE_MISSING = new Text("Missing mode value");
    private final static Text WILDCARD_ALL_NOT_ALLOWED= new Text("Wildcard <all> not allowed here");
    private final static Text SDP_NEGOTIATION_FAILED = new Text("SDP_NEGOTIATION_FAILED");
    private final static Text ERROR_ENDPOINT_UNAVAILAVALE = new Text("Endpoint not available");
    
    private final static Text SUCCESS= new Text("Success");

    //endpoints for searching
    private MgcpEndpoint[] endpoints = new MgcpEndpoint[2];    
    
    //requested endpoints
    private MgcpEndpoint endpoint, endpoint2;
    
    private MgcpConnection[] connections = new MgcpConnection[2];
    
    //call identifier parameter
    private Parameter callID;
    
    //local and domain name parts of the endpoint identifier
    private Text localName = new Text();
    private Text domainName = new Text();
            
    //layout local and domain names into endpoint identifier
    private Text[] endpointName = new Text[]{localName, domainName};      

    //local and domain name parts of the second endpoint identifier
    private Text localName2 = new Text();
    private Text domainName2 = new Text();
            
    //layout local and domain names into second endpoint identifier
    private Text[] endpointName2 = new Text[]{localName2, domainName2};      
    
    //transmission mode parameter
    private Parameter mode;
    
    //session descriptor parameter
    private Parameter sdp;
    
    private TaskChain handler;
    private Preprocessor preprocessor;
    
    private MgcpRequest request;
    
    //subtasks
    private Responder responder;
    private ErrorHandle errorHandle;
    
    private MgcpCall call;
    
    //error code and message
    private int code;
    private Text message;
    
    //local connection options
    private LocalConnectionOptions lcOptions = new LocalConnectionOptions();
    
    private final static Logger logger = Logger.getLogger(CreateConnectionCmd.class);
    
    /**
     * Creates new instance of this action executor.
     * 
     * @param scheduler the job scheduler.
     */
    public CreateConnectionCmd(Scheduler scheduler) {

        handler = new TaskChain(2,scheduler);
        
        //intialize action's subtasks
        responder = new Responder();
        preprocessor = new Preprocessor();
        
        errorHandle = new ErrorHandle();
                
        handler.add(preprocessor);
        handler.add(responder);
        
        this.setActionHandler(handler);
        this.setRollbackHandler(errorHandle);
    }
    
    @Override
    public void start(Transaction tx) {
        handler.clean();
        handler.add(preprocessor);
        handler.add(responder);
        
        super.start(tx);
    }
    /**
     * Preprocesses MGCP create connection message and dynamically constructs
     * task chain.
     */
    private class Preprocessor extends Task {

        public Preprocessor() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
        	endpoint = null;
            endpoint2 = null;
                        
            request = (MgcpRequest) getEvent().getMessage();
            
            Parameter z2 = request.getParameter(Parameter.SECOND_ENDPOINT);
            sdp = request.getParameter(Parameter.SDP);

            //second endpoint and sdp should not be all together in one message
            if (z2 != null && sdp != null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, new Text("Second endpoint and remote SDP present in message"));
            }

            //getting call identifier
            callID = request.getParameter(Parameter.CALL_ID);
            if (callID == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, CALLID_MISSING);
            }
            
            //getting transmission mode
            mode = request.getParameter(Parameter.MODE);
            if (mode == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, MODE_MISSING);
            }
            
            //getting endpoint name
            request.getEndpoint().divide('@', endpointName);

            //checking the local name:
            if (localName.contains('*')) {
                throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED, WILDCARD_ALL_NOT_ALLOWED);
            }

            if (z2 != null) {
                z2.getValue().divide('@', endpointName2);
                //checking the local name 2
                if (localName2.contains('*')) {
                    throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED, new Text("Wildcard all is not allowed here"));
                }
            }
            
            //modify local connection options
            Parameter l = request.getParameter(Parameter.LOCAL_CONNECTION_OPTIONS);
            if (l != null) {
                lcOptions.setValue(l.getValue());
            } else {
                lcOptions.setValue(null);
            }
            
            //for some reason they are not cleared,and may lead problems
            connections[0]=null;
            connections[1]=null;
            
            call = transaction().getCall(callID.getValue().hexToInteger(), true);
            
            try {
                //searching endpoint
            	int n = transaction().find(localName, endpoints);
                if (n == 0) {
                	throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ERROR_ENDPOINT_UNAVAILAVALE);
                }
                
                //extract found endpoint
                endpoint = endpoints[0];
            } catch (Exception e) { 
            	throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ERROR_ENDPOINT_UNAVAILAVALE);
            }
            
            if (z2 != null) {            	
                //create two local connections
                try {
                    //searching endpoint
                    int n = transaction().find(localName2, endpoints);
                    
                    if (n == 0) {
                        throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ERROR_ENDPOINT_UNAVAILAVALE);
                    }
                    
                    //extract found endpoint
                    endpoint2 = endpoints[0];
                } catch (Exception e) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ERROR_ENDPOINT_UNAVAILAVALE);
                }
                
                try {
                    connections[0] = endpoint.createConnection(call, ConnectionType.LOCAL,false);
                } catch (Exception e) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Problem with connection"));
                }

                try {
                    connections[1] = endpoint2.createConnection(call, ConnectionType.LOCAL,false);
                } catch (Exception e) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Problem with connection"));
                }
                
                
                try {
                    connections[0].setOtherParty(connections[1]);
                } catch (Exception e) {
                	logger.error("Could not set remote peer", e);
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Problem with joining"));
                }

                if (mode == null) {
                    throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, new Text("Mode was not specified"));
                }
                
                ConnectionMode m = ConnectionMode.fromDescription(mode.getValue().toString());
                
                try {
                    connections[0].setMode(m);
                    connections[1].setMode(ConnectionMode.SEND_RECV);
                } catch (Exception e) {
                    throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE, new Text("Unsupported mode"));
                }
                
                connections[0].setDtmfClamp(lcOptions.getDtmfClamp());                
            } else {            	
                //create one RTP connection            	                             
            	try {
                    connections[0] = endpoint.createConnection(call, ConnectionType.RTP,lcOptions.getIsLocal());
                    connections[0].setCallAgent(getEvent().getAddress());                
                } catch (Exception e) {            	
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Problem with connection" + e.getMessage()));
                }
                
                if (sdp != null) {
                    try {
                        connections[0].setOtherParty(sdp.getValue());
                    } catch (IOException e) {
                    	logger.error("Could not set remote peer", e);
                    	throw new MgcpCommandException(MgcpResponseCode.MISSING_REMOTE_CONNECTION_DESCRIPTOR, SDP_NEGOTIATION_FAILED);
                    }
                } else {
                	try {
						connections[0].generateLocalDescriptor(lcOptions.isWebRTC());
					} catch (IOException e) {
						throw new MgcpCommandException(MgcpResponseCode.INTERNAL_INCONSISTENCY_IN_LOCAL_SDP, new Text("Could not generate local connection descriptor."));
					}
                }
                
                try {
                    connections[0].setMode(mode.getValue());
                } catch (ModeNotSupportedException e) {
                    throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE, new Text("Not supported mode"));
                }
                
                connections[0].setDtmfClamp(lcOptions.getDtmfClamp());                
            }
            return 0;
        }        
    }
        
    private class Responder extends Task {

        public Responder() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {     
            MgcpEvent evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
            try {
                MgcpResponse response = (MgcpResponse) evt.getMessage();
                response.setResponseCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED);
                response.setResponseString(SUCCESS);

                //set parameters
                response.setParameter(Parameter.CONNECTION_ID, connections[0].getTextualID());
                response.setParameter(Parameter.ENDPOINT_ID, endpoint.getFullName());

                if (endpoint2 == null) {
                    response.setParameter(Parameter.SDP, connections[0].getDescriptor());
                }

                response.setTxID(transaction().getId());

                if (endpoint2 != null) {
                    response.setParameter(Parameter.SECOND_ENDPOINT, endpoint2.getFullName());
                }

                if (connections[1] != null) {
                    response.setParameter(Parameter.CONNECTION_ID2, connections[1].getTextualID());
                }

                transaction().getProvider().send(evt);
            } catch (IOException e) {
            	logger.error(e);
            } finally {
                evt.recycle();
            }


            return 0;
        }
        
    }

    private class ErrorHandle extends Task {
    	public ErrorHandle() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            if (endpoint != null) {
                endpoint.share();
                if (connections[0] != null) {
                    endpoint.deleteConnection(connections[0].getID());
                }
            }

            if (endpoint2 != null) {
                endpoint2.share();
                if (connections[1] != null) {
                    endpoint2.deleteConnection(connections[1].getID());
                }
            }

            code = ((MgcpCommandException)transaction().getLastError()).getCode();
            message = ((MgcpCommandException)transaction().getLastError()).getErrorMessage();
            
            MgcpEvent evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
            try {
                MgcpResponse response = (MgcpResponse) evt.getMessage();
                response.setResponseCode(code);
                response.setResponseString(message);
                response.setTxID(transaction().getId());

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
