/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.media.bootstrap.main;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.restcomm.media.network.UdpManager;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.Task;
import org.restcomm.media.spi.ControlProtocol;
import org.restcomm.media.spi.MediaServer;
import org.restcomm.media.spi.ServerManager;

import com.google.inject.Inject;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RestCommMediaServer implements MediaServer {

    private static final Logger log = Logger.getLogger(RestCommMediaServer.class);

    // Media Server State
    private boolean started;
    private Map<ControlProtocol, ServerManager> controllers;

    // Core Components
    private final PriorityQueueScheduler mediaScheduler;
    private final Scheduler taskScheduler;
    private final UdpManager udpManager;

    // Heart beat
    private final HeartBeat heartbeat;
    private final int heartbeatTime;
    private volatile long ttl;

    @Inject
    public RestCommMediaServer(PriorityQueueScheduler mediaScheduler, Scheduler taskScheduler, UdpManager udpManager,
            ServerManager controller) {
        // Core Components
        this.mediaScheduler = mediaScheduler;
        this.taskScheduler = taskScheduler;
        this.udpManager = udpManager;

        // Media Server State
        this.started = false;
        this.controllers = new HashMap<>(2);
        addManager(controller);

        // Heartbeat
        this.heartbeat = new HeartBeat();
        this.heartbeatTime = 1;
    }

    @Override
    public void addManager(ServerManager manager) {
        if (manager != null) {
            ControlProtocol protocol = manager.getControlProtocol();
            if (this.controllers.containsKey(protocol)) {
                throw new IllegalArgumentException(protocol + " controller is already registered");
            } else {
                this.controllers.put(protocol, manager);
            }
        }
    }

    @Override
    public void removeManager(ServerManager manager) {
        if (manager != null) {
            this.controllers.remove(manager.getControlProtocol());
        }
    }

    @Override
    public void start() throws IllegalStateException {
        if (this.started) {
            throw new IllegalStateException("Media Server already started.");
        }

        this.started = true;
        this.heartbeat.restart();
        this.mediaScheduler.start();
        this.taskScheduler.start();
        this.udpManager.start();
        for (ServerManager controller : this.controllers.values()) {
            controller.activate();
        }

        if (log.isInfoEnabled()) {
            log.info("Media Server started");
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        if (!this.started) {
            throw new IllegalStateException("Media Server already stopped.");
        }

        this.started = false;
        this.udpManager.stop();
        this.taskScheduler.stop();
        this.mediaScheduler.stop();
        this.heartbeat.cancel();
        for (ServerManager controller : this.controllers.values()) {
            controller.deactivate();
        }
        if (log.isInfoEnabled()) {
            log.info("Media Server stopped");
        }
    }
    
    @Override
    public boolean isRunning() {
        return this.started;
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
            mediaScheduler.submitHeatbeat(this);
        }

        @Override
        public long perform() {
            ttl--;
            if (ttl == 0) {
                log.info("Global hearbeat is still alive");
                restart();
            } else {
                mediaScheduler.submitHeatbeat(this);
            }
            return 0;
        }
    }

}
