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

package org.mobicents.media.core;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ServerManager;

/**
 *
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class Server implements MediaServer {

    private static final Logger log = Logger.getLogger(Server.class);

    // Core components
    private Clock clock;
    private PriorityQueueScheduler scheduler;
    private UdpManager udpManager;

    // Media Server Controllers
    private final ArrayList<ServerManager> managers = new ArrayList<ServerManager>();

    // Heart beat
    private HeartBeat heartbeat;
    private int heartbeatTime = 0;
    private volatile long ttl;

    public Server() {
        super();
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
        // check clock
        if (clock == null) {
            log.error("Timing clock is not defined");
            return;
        }

        if (heartbeatTime > 0) {
            heartbeat = new HeartBeat();
            heartbeat.restart();
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        if (log.isInfoEnabled()) {
            log.info("Stopping UDP Manager");
        }
        udpManager.stop();

        if (heartbeat != null) {
            heartbeat.cancel();
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
    public void addManager(ServerManager manager) {
        managers.add(manager);
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
