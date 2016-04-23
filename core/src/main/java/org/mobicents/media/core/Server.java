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

    //timing clock
    private Clock clock;

    //job scheduler
    private PriorityQueueScheduler scheduler;

    //udp manager
    private UdpManager udpManager;
    
    //managers
    private final ArrayList<ServerManager> managers = new ArrayList<ServerManager>();
    
    private HeartBeat heartbeat;
    private int heartbeatTime=0;
    private volatile long ttl;
    
    private static final Logger logger = Logger.getLogger(Server.class);
    
    public Server() {
        super();
    }
    
    /**
     * Assigns clock instance.
     *
     * @param clock
     */
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public void setScheduler(PriorityQueueScheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    public void setUdpManager(UdpManager udpManager) {
        this.udpManager=udpManager;
    }
    
    /**
     * Assigns the heartbeat time in minutes
     *
     * @param minutes
     */
    public void setHeartBeatTime(int heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    /**
     * Starts the server.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        //check clock
        if (clock == null) {
            logger.error("Timing clock is not defined");
            return;
        }

        if(heartbeatTime>0)
        {
        	heartbeat=new HeartBeat();
        	heartbeat.restart();
        }
    }

    /**
     * Stops the server.
     *
     */
    public void stop() {
        logger.info("Stopping UDP Manager");
        udpManager.stop();

        if(heartbeat!=null)
        	heartbeat.cancel();
        
        logger.info("Stopping scheduler");
        scheduler.stop();
        logger.info("Stopped media server instance ");                
    }

    @Override
    public void addManager(ServerManager manager) {
        managers.add(manager);
        logger.info(manager.toString());
    }

    @Override
    public void removeManager(ServerManager manager) {
        managers.remove(manager);
        logger.info(manager.toString());
    }
    
    private class HeartBeat extends Task {

        public HeartBeat() {
            super();
        }        

        @Override
        public int getQueueNumber()
        {
        	return PriorityQueueScheduler.HEARTBEAT_QUEUE;
        }   
        
        public void restart()
        {
        	ttl=heartbeatTime*600;
        	scheduler.submitHeatbeat(this);
        }
        
        @Override
        public long perform() {
        	ttl--;
            if (ttl == 0) {
            	logger.info("Global hearbeat is still alive");
            	restart();
            } else {
                scheduler.submitHeatbeat(this);
            }            
            return 0;
        }
    }
}
