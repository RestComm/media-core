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

import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.TaskChain;
import org.mobicents.media.server.scheduler.TaskChainListener;
import org.mobicents.media.server.scheduler.TaskListener;

/**
 * Defines action.
 * 
 * @author kulikov
 */
public class Action implements TaskChainListener {
    //action listener instance
    protected ActionListener listener;
    
    private TaskChain actionHandler;
    private Task rollbackHandler;
    
    private Transaction tx;
    private MgcpEvent event;
    
    private RollbackListener rollbackListener = new RollbackListener();
    /**
     * Creates new instance of the action.
     */
    protected Action() {
        super();
    }
    
    /**
     * Assigns the event caused this action.
     * 
     * @param event the object describes the event
     */
    protected void setEvent(MgcpEvent event) {
        this.event = event;
    }
    
    /**
     * Gets the event caused this action.
     * 
     * @return the event object
     */
    public MgcpEvent getEvent() {
        return event;
    }
    
    /**
     * Assigns action handler.
     * 
     * @param handler action handler
     */
    public void setActionHandler(TaskChain handler) {
        this.actionHandler = handler;
        this.actionHandler.setListener(this);
    }
    
    /**
     * Assigns rollback handler
     * 
     * @param handler rollback handler.
     */
    public void setRollbackHandler(Task handler) {
        this.rollbackHandler = handler;
        this.rollbackHandler.setListener(rollbackListener);
    }
    
    /**
     * 
     * Starts action execution.
     * 
     * @param context transaction context
     */
    public void start(Transaction tx) {
        this.tx = tx;
        
        actionHandler.setListener(this);        
        tx.scheduler().submit(actionHandler);
    }
    
    /**
     * Rollback previously made changes.
     * 
     */
    public void rollback() {
        if (rollbackHandler != null) {
            //rollbackHandler.setDeadLine(tx.getTime() + 1000000L);
            tx.scheduler().submit(rollbackHandler, PriorityQueueScheduler.MANAGEMENT_QUEUE);        
        } else {
            tx.onRollback();
        }
    }

    @Override
    public void onTermination() {
        if (listener != null) {
            listener.onComplete();
        }
    }

    @Override
    public void onException(Exception e) {
        if (listener != null) {
            listener.onFailure(e);
        }
    }
    
    protected Transaction transaction() {
        return tx;
    }
    
    private class RollbackListener implements TaskListener {

    	@Override
        public void onTerminate() {
            tx.onRollback();
        }

    	@Override
        public void handlerError(Exception e) {
        }
        
    }
}
