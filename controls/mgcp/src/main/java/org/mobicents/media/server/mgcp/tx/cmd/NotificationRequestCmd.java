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
import java.util.Collection;
import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.controller.Request;
import org.mobicents.media.server.mgcp.controller.MgcpEndpoint;
import org.mobicents.media.server.mgcp.controller.UnknownEventException;
import org.mobicents.media.server.mgcp.controller.UnknownPackageException;
import org.mobicents.media.server.mgcp.controller.UnknownSignalException;
import org.mobicents.media.server.mgcp.controller.naming.UnknownEndpointException;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.MgcpResponse;
import org.mobicents.media.server.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.mgcp.tx.Action;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.TaskChain;
import org.mobicents.media.server.utils.Text;
import org.apache.log4j.Logger;
/**
 *
 * @author kulikov
 */
public class NotificationRequestCmd extends Action {
	private final static Text SUCCESS= new Text("Success");
    
    private MgcpRequest request;

    private MgcpEndpoint endpoint;
    private MgcpEndpoint[] endpoints = new MgcpEndpoint[1];

    private TaskChain handler;
    private ErrorHandler errorHandler;
    
    //error code and message
    private int code;
    private Text message;
    
    private final static Logger logger = Logger.getLogger(NotificationRequestCmd.class);    
          
    public NotificationRequestCmd(Scheduler scheduler) {
        handler = new TaskChain(3);
        
        Request req = new Request(scheduler);
        Responder responder = new Responder(scheduler);
        Executor executor = new Executor(scheduler);
        
        errorHandler = new ErrorHandler(scheduler);
        
        handler.add(req);
        handler.add(responder);
        handler.add(executor);
        
        this.setActionHandler(handler);
        this.setRollbackHandler(errorHandler);
    }
    
    private class Request extends Task {
        
        public Request(Scheduler scheduler) {
            super(scheduler);
        }

        public int getQueueNumber()
        {
        	return scheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            request = (MgcpRequest) getEvent().getMessage();
            //getting the endpoint name
            Text localName = new Text();
            Text domainName = new Text();
            
            //read endpoint name from request and break into parts
            Text[] endpointName = new Text[]{localName, domainName};                
            request.getEndpoint().divide('@', endpointName);
            
            //checking the local name:
            if (localName.contains('*')) {
                throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED, new Text("Wildcard all is not allowed here"));
            }

            try {
                int n = transaction().find(localName, endpoints);
                if (n == 0) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN, new Text("Unknown endpoint"));
                }
            } catch (UnknownEndpointException e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN, new Text("Endpoint not available"));
            }
            
            endpoint = endpoints[0];
            endpoint.getRequest().cancel();
            
            Parameter reqID = request.getParameter(Parameter.REQUEST_ID);
            if (reqID == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, new Text("Request identifier is missing"));
            }
            
            Text callAgent = null;
            Parameter p = request.getParameter(Parameter.NOTIFIED_ENTITY);
            if (p != null) {
                callAgent = p.getValue();
            }
            
            Parameter pe = request.getParameter(Parameter.REQUESTED_EVENTS);
            Parameter ps = request.getParameter(Parameter.REQUESTED_SIGNALS);
            
            Collection<Text> events = null;
            Collection<Text> signals = null;
            
            if (pe != null) {
                events = pe.getValue().split(',');
            }
            
            if (ps != null) {
                signals = ps.getValue().split(',');
            }
            
            if (pe != null || ps != null) {
                try {
                	endpoint.getRequest().accept(reqID.getValue(), callAgent, events, signals);                    
                } catch (UnknownEventException e1) {
                	throw new MgcpCommandException(MgcpResponseCode.CAN_NOT_DETECT_EVENT, new Text(e1.getMessage()));
                } catch (UnknownSignalException e2) {
                	throw new MgcpCommandException(MgcpResponseCode.CAN_NOT_GENERATE_SIGNAL, new Text(e2.getMessage()));
                } catch (UnknownPackageException e) {
                	throw new MgcpCommandException(MgcpResponseCode.CAN_NOT_DETECT_EVENT, new Text(e.getMessage()));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MgcpCommandException(MgcpResponseCode.TRANSIENT_ERROR, new Text(e.getMessage()));
                }
                
            }            
            return 0;
        }
        
    }
    
    private class Responder extends Task {

        public Responder(Scheduler scheduler) {
            super(scheduler);
        }
        
        public int getQueueNumber()
        {
        	return scheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {        	
        	MgcpEvent evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
        	MgcpResponse response = (MgcpResponse) evt.getMessage();
        	response.setResponseCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED);
        	response.setResponseString(SUCCESS);
        	response.setTxID(transaction().getId());        	
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
    
    private class Executor extends Task {

        public Executor(Scheduler scheduler) {
            super(scheduler);
        }
        
        public int getQueueNumber()
        {        	
        	return scheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
        	endpoint.getRequest().execute();        	
        	return 0;
        }
        
    }

    private class ErrorHandler extends Task {

        public ErrorHandler(Scheduler scheduler) {
            super(scheduler);
        }
        
        public int getQueueNumber()
        {
        	return scheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            MgcpEvent evt = null;
            try {                
                code = ((MgcpCommandException) transaction().getLastError()).getCode();
                message = ((MgcpCommandException) transaction().getLastError()).getErrorMessage();

                evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
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
