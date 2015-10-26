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

package org.mobicents.media.server.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

/**
 * Scheduling task.
 * 
 * @author Oifa Yulian
 */
public abstract class Task implements Runnable {

    private Logger logger = Logger.getLogger(Task.class);

    private static final AtomicInteger UNIQUE_ID = new AtomicInteger(0);

    protected int taskId;
    private volatile boolean active = false;
    private volatile boolean heartbeat = false;

    private AtomicBoolean inQueue0;
    private AtomicBoolean inQueue1;

    // error handler instance
    protected TaskListener listener;

    public Task() {
        this.taskId = UNIQUE_ID.incrementAndGet();
        this.active = false;
        this.heartbeat = false;
        this.inQueue0 = new AtomicBoolean(false);
        this.inQueue1 = new AtomicBoolean(false);
    }

    public void storedInQueue0() {
        inQueue0.set(true);
    }

    public void storedInQueue1() {
        inQueue1.set(true);
    }

    public void removeFromQueue0() {
        inQueue0.set(false);
    }

    public void removeFromQueue1() {
        inQueue1.set(false);
    }

    public Boolean isInQueue0() {
        return inQueue0.get();
    }

    public Boolean isInQueue1() {
        return inQueue1.get();
    }

    /**
     * Modifies task listener.
     * 
     * @param listener the handler instance.
     */
    public void setListener(TaskListener listener) {
        this.listener = listener;
    }

    /**
     * Current queue of this task.
     * 
     * @return the value of queue
     */
    public abstract int getQueueNumber();

    /**
     * Gets whether the task is active or not.
     * 
     * @return True if task is active, False otherwise.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Gets whether the task is a heart beat or not.
     * 
     * @return True if task is heart beat, False otherwise.
     */
    public boolean isHeartbeat() {
        return heartbeat;
    }

    /**
     * Executes task.
     * 
     * @return dead line of next execution
     */
    public abstract long perform();

    protected void activate(Boolean isHeartbeat) {
        this.active = true;
        this.heartbeat = isHeartbeat;
    }

    /**
     * Cancels task execution
     */
    public void cancel() {
        this.active = false;
    }

    // call should not be synchronized since can run only once in queue cycle
    @Override
    public void run() {
        if (this.active) {
            try {
                // execute task
                perform();

                // notify listener
                if (this.listener != null) {
                    this.listener.onTerminate();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);

                // notify listener
                if (this.listener != null) {
                    listener.handlerError(e);
                }
            }
        }
    }
}
