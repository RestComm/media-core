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

package org.restcomm.media.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.media.server.spi.ControlProtocol;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ServerManager;
import org.restcomm.media.network.UdpManager;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Task;

/**
 * Implementation of a Media Server.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @deprecated Use RestCommMediaServer instead
 */
@Deprecated
public class Server implements MediaServer {

    private static final Logger log = Logger.getLogger(Server.class);

    // Core components
    private Clock clock;
    private PriorityQueueScheduler scheduler;
    private UdpManager udpManager;

    // Heart beat
    private HeartBeat heartbeat;
    private int heartbeatTime;
    private volatile long ttl;

    // Media Server State
    private final Map<ControlProtocol, ServerManager> managers;
    private boolean started;

    public Server() {
        this.managers = new HashMap<>(2);
        this.started = false;
        this.heartbeatTime = 0;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void setScheduler(PriorityQueueScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setUdpManager(UdpManager udpManager) {
        this.udpManager = udpManager;
    }

    /**
     * Assigns the heart beat time in minutes
     *
     * @param minutes
     */
    public void setHeartBeatTime(int heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    @Override
    public void start() throws IllegalStateException {
        if (this.started) {
            throw new IllegalStateException("Media Server already started");
        }

        // Validate mandatory dependencies
        if (clock == null) {
            log.error("Timing clock is not defined");
            return;
        }

        // Start Media Server
        this.started = true;
        for (ServerManager controller : managers.values()) {
            log.info("Activating controller " + controller.getControlProtocol().name());
            controller.activate();
        }

        if (heartbeatTime > 0) {
            heartbeat = new HeartBeat();
            heartbeat.restart();
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        if (!this.started) {
            throw new IllegalStateException("Media Server already stopped");
        }

        if (log.isInfoEnabled()) {
            log.info("Stopping UDP Manager");
        }
        udpManager.stop();

        if (heartbeat != null) {
            heartbeat.cancel();
        }

        for (ServerManager controller : managers.values()) {
            log.info("Deactivating controller " + controller.getControlProtocol().name());
            controller.deactivate();
        }

        if (log.isInfoEnabled()) {
            log.info("Stopping scheduler");
        }
        scheduler.stop();
        if (log.isInfoEnabled()) {
            log.info("Stopped media server instance ");
        }
    }

    @Override
    public boolean isRunning() {
        return this.started;
    }
    
    @Override
    public void addManager(ServerManager manager) {
        managers.put(manager.getControlProtocol(), manager);
    }

    @Override
    public void removeManager(ServerManager manager) {
        managers.remove(manager);
    }

    private final class HeartBeat extends Task {

        public HeartBeat() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.HEARTBEAT_QUEUE;
        }

        public void restart() {
            ttl = heartbeatTime * 600;
            scheduler.submitHeatbeat(this);
        }

        @Override
        public long perform() {
            ttl--;
            if (ttl == 0) {
                log.info("Global hearbeat is still alive");
                restart();
            } else {
                scheduler.submitHeatbeat(this);
            }
            return 0;
        }
    }
}
