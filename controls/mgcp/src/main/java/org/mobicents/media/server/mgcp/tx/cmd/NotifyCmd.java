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

import org.mobicents.media.server.mgcp.tx.Action;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.TaskChain;
import org.apache.log4j.Logger;

/**
 *
 * @author Yulian Oifa
 */
public class NotifyCmd extends Action {
    
    private TaskChain handler;
    
    private final static Logger logger = Logger.getLogger(NotifyCmd.class);    
    
    public NotifyCmd(PriorityQueueScheduler scheduler) {
        handler = new TaskChain(1,scheduler);
        handler.add(new Sender());
        this.setActionHandler(handler);
    }
    
    private class Sender extends Task {
        
        public Sender() {
            super();
        }

        @Override
        public int getQueueNumber() {
        	return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            try {
                transaction().getProvider().send(getEvent());
            } catch (IOException e) {
            	logger.error(e);
            } 
            return 0;
        }
        
    }
}
