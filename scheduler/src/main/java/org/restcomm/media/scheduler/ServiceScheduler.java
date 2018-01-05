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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler implementation that relies on a {@link ScheduledExecutorService} to manage the thread pool as well as the scheduled
 * tasks.
 * 
 * The ServiceScheduler is a Singleton because its meant to be used as the core element of the Media Server, so it relies on a
 * ServiceExecutor that will allocate a thread pool as big as the system allows (within recommended values).
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ServiceScheduler implements Scheduler {
    
    private static final Logger LOGGER = LogManager.getLogger(ServiceScheduler.class);

    public static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private volatile boolean started;
    private final Clock wallClock;
    private ScheduledExecutorService executor;
    private final ThreadFactory threadFactory = new ThreadFactory() {

        private AtomicInteger index = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "service-scheduler-" + index.incrementAndGet());
        }
    };

    public ServiceScheduler(final Clock wallClock) {
        this.started = false;
        this.wallClock = new WallClock();
    }

    public ServiceScheduler() {
        this(new WallClock());
    }

    @Override
    public Clock getWallClock() {
        return this.wallClock;
    }

    @Override
    public Future<?> submit(Runnable task) throws RejectedExecutionException {
        if (!this.started) {
            throw new RejectedExecutionException("Scheduler is not running.");
        }
        return this.executor.submit(task);
    }
    
    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) throws RejectedExecutionException {
        if (!this.started) {
            throw new RejectedExecutionException("Scheduler is not running.");
        }
        return this.executor.schedule(task, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long period, TimeUnit unit)
            throws IllegalArgumentException, RejectedExecutionException {
        if (!this.started) {
            throw new RejectedExecutionException("Scheduler is not running.");
        }
        return this.executor.scheduleWithFixedDelay(task, initialDelay, period, unit);
    }

    @Override
    public void start() {
        if (!this.started) {
            this.started = true;
            this.executor = Executors.newScheduledThreadPool(POOL_SIZE, threadFactory);
            ((ScheduledThreadPoolExecutor) this.executor).setRemoveOnCancelPolicy(true);
            ((ScheduledThreadPoolExecutor) this.executor).prestartAllCoreThreads();
            LOGGER.info("Started scheduler!");
        }
    }

    @Override
    public void stop() {
        if (this.started) {
            this.started = false;
            this.executor.shutdownNow();
            LOGGER.info("Stopped scheduler!");
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.executor.awaitTermination(timeout, unit);
    }

}
