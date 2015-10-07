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
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.mobicents.media.core.endpoints.BaseEndpointImpl;
import org.mobicents.media.core.endpoints.VirtualEndpointInstaller;
import org.mobicents.media.core.naming.NamingService;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointInstaller;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.ServerManager;

/**
 *
 * @author Oifa Yulian
 */
public class Server implements MediaServer {

    //timing clock
    private Clock clock;

    //job scheduler
    private PriorityQueueScheduler scheduler;

    //resources pool
    private ResourcesPool resourcesPool;
    
    //udp manager
    private UdpManager udpManager;
    
    private NamingService namingService;
    
    //endpoint installers
    private ArrayList<EndpointInstaller> installers = new ArrayList<EndpointInstaller>();
    
    //endpoints
    private HashMap<String, Endpoint> endpoints = new HashMap<String, Endpoint>();
    
    //managers
    private ArrayList<ServerManager> managers = new ArrayList<ServerManager>();
    
    private HeartBeat heartbeat;
    private int heartbeatTime=0;
    private volatile long ttl;
    
    public  Logger logger = Logger.getLogger(Server.class);
    
    public Server() {
        namingService = new NamingService();
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
    
    public void setResourcesPool(ResourcesPool resourcesPool) {
        this.resourcesPool=resourcesPool;
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
     * Installs endpoints defined by specified installer.
     *
     * @param installer the endpoints installer
     */
    public void addInstaller(EndpointInstaller installer) {
    	((VirtualEndpointInstaller)installer).setServer(this);
    	installers.add(installer);
        installer.install();        
    }

    /**
     * Uninstalls endpoint defined by specified endpoint installer.
     *
     * @param installer the endpoints installer.
     */
    public void removeInstaller(EndpointInstaller installer) {
        installers.remove(installer);
        installer.uninstall();
    }

    /**
     * Installs the specified endpoint.
     *
     * @param endpoint the endpoint to installed.
     */
    public void install(Endpoint endpoint,EndpointInstaller installer) {
        //check endpoint first
        if (endpoint == null) {
            logger.error("Unknown endpoint");
            return;
        }

        //The endpoint implementation must extend BaseEndpointImpl class
        BaseEndpointImpl baseEndpoint = null;
        try {
            baseEndpoint = (BaseEndpointImpl) endpoint;
        } catch (ClassCastException e) {
            logger.error("Unsupported endpoint implementation " + endpoint.getLocalName());
            return;
        }


        //assign scheduler to the endpoint
        baseEndpoint.setScheduler(scheduler);
        baseEndpoint.setResourcesPool(resourcesPool);

        logger.info("Installing " + endpoint.getLocalName());

        //starting endpoint
        try {
            endpoint.start();
        } catch (Exception e) {
            logger.error("Couldn't start endpoint " + endpoint.getLocalName(), e);
            return;
        }

        //register endpoint with naming service
        try {
            namingService.register(endpoint);
        } catch (Exception e) {
            endpoint.stop();
            logger.error("Could not register endpoint " + endpoint.getLocalName(), e);
        }
        
        //register endpoint localy
        endpoints.put(endpoint.getLocalName(), endpoint);
        
        //send notification to manager
        for (ServerManager manager : managers) {
            manager.onStarted(endpoint,installer);
        }
    }


    /**
     * Uninstalls the endpoint.
     *
     * @param name the local name of the endpoint to be uninstalled
     */
    public void uninstalls(String name) {
        //unregister localy
        Endpoint endpoint = endpoints.remove(name);

        //send notification to manager
        for (ServerManager manager : managers) {
            manager.onStopped(endpoint);
        }
        
        try {
            //TODO: lookup irrespective of endpoint usage
            endpoint = namingService.lookup(name, true);
            if (endpoint != null) {
                endpoint.stop();
                namingService.unregister(endpoint);
            }
        } catch (Exception e) {
        	logger.error(e);
        }
        
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
        
        ServiceScheduler.getInstance().start();
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
        ServiceScheduler.getInstance().stop();
        try {
            ServiceScheduler.getInstance().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Scheduler has shutdown because its thread was interrupted.");
        }
        logger.info("Stopped media server instance ");                
    }

    @Override
    public Endpoint lookup(String name, boolean bussy) throws ResourceUnavailableException {
        return null;//return namingService.lookup(name, bussy);
    }
    
    @Override
    public Endpoint[] lookupall(String endpointName) throws ResourceUnavailableException {
    	return null;//return namingService.lookupall(endpointName);
    }

    @Override
    public int getEndpointCount() {
        return 0;//return namingService.getEndpointCount();
    }
    
    @Override
    public Collection<Endpoint> getEndpoints() {
        return endpoints.values();
    }

    @Override
    public void addManager(ServerManager manager) {
        managers.add(manager);
    }

    @Override
    public void removeManager(ServerManager manager) {
        managers.remove(manager);
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
