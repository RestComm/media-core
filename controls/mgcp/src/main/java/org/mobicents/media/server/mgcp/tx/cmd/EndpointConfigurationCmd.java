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

import org.apache.log4j.Logger;
import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.controller.MgcpEndpoint;
import org.mobicents.media.server.mgcp.controller.naming.UnknownEndpointException;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.MgcpResponse;
import org.mobicents.media.server.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.mgcp.tx.Action;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.TaskChain;
import org.mobicents.media.server.utils.Text;
/**
 * Endpoint configuration command
 * 
 * @author oifa yulian
 */
public class EndpointConfigurationCmd extends Action {
    //response strings
    private final static Text SUCCESS= new Text("Success");
    private final static Text MU_LAW= new Text("e:mu");
    private final static Text A_LAW= new Text("e:A");
    
    private final static Text BEARER_INFORMATION_MISSING = new Text("Missing bearer information value");
    private final static Text INVALID_BEARER_INFORMATION = new Text("Invalid bearer information value");
    
    private MgcpRequest request;
    
    //local and domain name parts of the endpoint identifier
    private Text localName = new Text();
    private Text domainName = new Text();
            
    //layout local and domain names into endpoint identifier
    private Text[] endpointName = new Text[]{localName, domainName};      
    
    private MgcpEndpoint endpoint;
    private MgcpEndpoint[] endpoints = new MgcpEndpoint[1];

    private TaskChain handler;

    private int code;
    private Text message;
    
    private final static Logger logger = Logger.getLogger(EndpointConfigurationCmd.class);    
    
    public EndpointConfigurationCmd(Scheduler scheduler) {
        handler = new TaskChain(2,scheduler);
        
        Configurator configurator = new Configurator();
        Responder responder = new Responder();
        
        handler.add(configurator);
        handler.add(responder);
        
        ErrorHandler errorHandler = new ErrorHandler();
        
        this.setActionHandler(handler);
        this.setRollbackHandler(errorHandler);        
    }
    
    private class Configurator extends Task {
        
        public Configurator() {
            super();
        }

        @Override
        public int getQueueNumber()
        {
        	return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            request = (MgcpRequest) getEvent().getMessage();                        
            
            Parameter bearerInformation = request.getParameter(Parameter.BARER_INFORMATION);
            if (bearerInformation == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, BEARER_INFORMATION_MISSING);
            }
            
            boolean isALaw=false;
            if(bearerInformation.getValue().equals(MU_LAW))
        	{
        		//do nothing
        	}
        	else if(bearerInformation.getValue().equals(A_LAW))
        		isALaw=true;
        	else
        		throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, INVALID_BEARER_INFORMATION);
        	
            //getting endpoint name
            request.getEndpoint().divide('@', endpointName);
            //searching endpoint
            try {
                int n = transaction().find(localName, endpoints);
                if (n == 0) {                	
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Endpoint not available"));
                }
            } catch (UnknownEndpointException e) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN, new Text("Endpoint not available"));
            }
            
            //extract found endpoint
            endpoint = endpoints[0];
            
            endpoint.configure(isALaw);
            return 0;
        }
        
    }        
    
    private class Responder extends Task {

        public Responder() {
            super();
        }
        
        @Override
        public int getQueueNumber()
        {
        	return PriorityQueueScheduler.MANAGEMENT_QUEUE;
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

    private class ErrorHandler extends Task {

        public ErrorHandler() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return PriorityQueueScheduler.MANAGEMENT_QUEUE;
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
