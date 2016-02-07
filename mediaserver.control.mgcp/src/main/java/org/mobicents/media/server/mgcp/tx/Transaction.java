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

package org.mobicents.media.server.mgcp.tx;

import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import org.mobicents.media.server.mgcp.controller.*;
import org.apache.log4j.Logger;
import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.MgcpProvider;
import org.mobicents.media.server.mgcp.controller.naming.UnknownEndpointException;
import org.mobicents.media.server.mgcp.tx.cmd.ActionSelector;
import org.mobicents.media.server.mgcp.tx.cmd.MgcpCommandException;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.utils.Text;

/**
 * Represents transaction.
 * 
 * @author yulian oifa
 */
public class Transaction implements ActionListener {
    //unique identifier of this transaction
    protected int id;
    protected int uniqueId; 
    
    protected boolean completed=false;
    
    //Transaction manager instance
    private TransactionManager txManager;
    
    private ActionSelector selector;
    
    private Action action;    
    private Exception lastError;
        
    //Logger instance
    private final static Logger logger = Logger.getLogger("MGCP");
    
    /**
     * Create new transaction executor.
     * 
     * @param transactions pool of the transaction objects.
     * @param Controller the controller instance.
     */
    protected Transaction(TransactionManager txManager) {
        this.txManager = txManager;
        selector = new ActionSelector(txManager.scheduler());
    }
    
    /**
     * Gets the access to the scheduler.
     * 
     * @return job scheduler.
     */
    public PriorityQueueScheduler scheduler() {
        return txManager.scheduler();
    }
    
    public int getId() {
        return this.id;
    }
    
    /**
     * Provides access to the MGCP provider.
     * 
     * @return protocol provider object.
     */
    public MgcpProvider getProvider() {
        return txManager.provider;
    }

    public MgcpCall getCall(Integer id, boolean isNew) {
        return txManager.callManager.getCall(id, isNew);
    }
    
    
    /**
     * Finds endpoints with specified name pattern.
     * 
     * @param name the endpoint name pattern.     * 
     * @param endpoints array for search result
     * @return number of found endpoints.
     * @throws UnknownEndpointException if name pattern was wrong.
     */
    public int find(Text name, MgcpEndpoint[] endpoints) throws UnknownEndpointException {
        return txManager.namingService.find(name, endpoints);        
    }
    
    /**
     * Gets the last observed error.
     * 
     * @return the exception class.
     */
    public Exception getLastError() {
        return this.lastError;
    }
    
    /**
     * Provides access to the wall clock.
     * 
     * @return time measured by wall clock.
     */
    public long getTime() {
        return txManager.getTime();
    }
    
    /**
     * Handles event.
     * 
     * @param event the event to handle
     */
    public void process(MgcpEvent event) {
        action = selector.getAction(event);
        if (action != null) {
            logger.info("tx=" + id + " Started, message= " + event.getMessage() + ", call agent = " + event.getAddress());
            action.setEvent(event);
            process(action);
        } else {
            event.recycle();
            this.onComplete();
        }
    }
    
    protected void process(Action action) {
        this.action = action;
        action.listener = this;
        action.start(this);
    }
    
    
    protected int nextID() {
        return txManager.nextID();
    }

    public void onComplete() {
        logger.info("tx=" + id + " was executed normaly");
        if (action != null && action.getEvent() != null) {
        	action.getEvent().recycle();
        	action=null;
        }
        
        if(!completed)
        {
        	completed=true;
        	txManager.terminate(this);
        }
    }

    public void onFailure(Exception e) {
        logger.error("tx=" + id + " Failed", e);
        
        if (e != null && e instanceof MgcpCommandException) {
            this.lastError = e;
        } else {
            Text msg = e.getMessage() != null ? new Text(e.getMessage()) : new Text("Unknown");
            this.lastError = new MgcpCommandException(ReturnCode.TRANSIENT_ERROR, msg);
        }
        
        action.rollback();
    }

    public void onRollback() {
        logger.info("tx=" + id + " Rolled back");
        if (action.getEvent() != null) {
            action.getEvent().recycle();
            action=null;
        }
        
        if(!completed)
        {
        	completed=true;
        	txManager.terminate(this);
        }
    }
}
