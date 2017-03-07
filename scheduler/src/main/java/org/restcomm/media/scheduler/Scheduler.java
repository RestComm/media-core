/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.restcomm.media.scheduler;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A Scheduler allows users to submit tasks for executions.
 * <p>
 * Task executions can be scheduled to run immediately or given a certain delay or even at regular intervals.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface Scheduler {

    /**
     * Gets the wall clock of the scheduler.
     * 
     * @return The wall clock
     */
    Clock getWallClock();

    /**
     * Submits a task for immediate execution.
     * 
     * @param task The task to be executed
     * @return A Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     */
    Future<?> submit(Runnable task) throws RejectedExecutionException;

    /**
     * Schedules a task to be executed in a given time.
     * 
     * @param task The task to be executed.
     * @param delay The initial amount of time to wait until task is executed.
     * @param unit The time unit of the delay.
     * @return a ScheduledFuture representing pending completion of the task and whose <tt>get()</tt> method will return
     *         <tt>null</tt> upon completion
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     */
    ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) throws RejectedExecutionException;

    /**
     * Schedules a task to be executed repeatedly, given a certain delay between executions.
     * 
     * @param command The task to be executed.
     * @param initialDelay The time to delay first execution
     * @param period the delay between the termination of one execution and the commencement of the next
     * @param unit The time unit of the delay parameters.
     * @return a ScheduledFuture representing pending completion of the task and whose <tt>get()</tt> method will return
     *         <tt>null</tt> upon completion
     * @throws IllegalArgumentException If period is equal or less than zero.
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     */
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long period, TimeUnit unit)
            throws IllegalArgumentException, RejectedExecutionException;

    /**
     * Starts the scheduler.
     * <p>
     * Tasks can only be submited for executed once the Scheduler is running.
     * </p>
     */
    void start();

    /**
     * Stops the scheduler.
     * <p>
     * Queued tasks will be canceled, but executing tasks will be given an opportunity to finish their execution.
     * </p>
     */
    void stop();

    /**
     * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return <tt>true</tt> if this executor terminated and <tt>false</tt> if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

}
