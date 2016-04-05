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

package org.mobicents.media.server.mgcp.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.MgcpListener;
import org.mobicents.media.server.mgcp.MgcpProvider;
import org.mobicents.media.server.mgcp.controller.naming.NamingTree;
import org.mobicents.media.server.mgcp.tx.GlobalTransactionManager;
import org.mobicents.media.server.mgcp.tx.Transaction;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointInstaller;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ServerManager;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 * The MGCP access point.
 *  
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class Controller implements MgcpListener, ServerManager {

	private final static String HOME_DIR = "MMS_HOME";
	
    private static final Logger logger = Logger.getLogger("MGCP");
    
    //network interface
    protected UdpManager udpInterface;
    
    //MGCP port number
    protected int port;
    
    protected PriorityQueueScheduler scheduler;
    
    //MGCP protocol provider
    protected MgcpProvider mgcpProvider;
       
    //server under control
    protected MediaServer server;
    
    //endpoints
    protected NamingTree endpoints = new NamingTree();

    //Endpoint configurator
    private Configurator configurator;
    
    protected GlobalTransactionManager txManager;
    
    protected int poolSize;
    
    /**
     * Assigns UDP network interface.
     * 
     * @param udpInterface  the UDP interface .
     */
    public void setUdpInterface(UdpManager udpInterface) {
        this.udpInterface = udpInterface;
        this.poolSize = 10;
    }
    
    /**
     * Assigns pool size.
     * 
     * @param poolSize the size of Transaction pool.
     */
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
    
    /**
     * Assigns MGCP port number.
     * 
     * @param port the port number.
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * Set server to control.
     * 
     * @param server the server instance.
     */
    public void setServer(MediaServer server) {
        logger.info("Set server");
        this.server = server;
        server.addManager(this);        
    }
    
    /**
     * Sets job scheduler.
     * 
     * @param scheduler the scheduler instance.
     */
    public void setScheduler(PriorityQueueScheduler scheduler) {
        logger.info("Set scheduler: " + scheduler);
        this.scheduler = scheduler;
    }
    
    /**
     * Modifies path to the configuration file.
     * The file must be present in the folder <MMS_HOME>/conf/ directory
     * 
     * @param fileName the name of the file.
     * @throws Exception 
     */
    public void setConfiguration(String url) throws Exception {
        try {
            if (url != null) {            	
                //getting the full path to the configuration file
            	String home = getHomeDir();
                
                if (home == null) {
                	throw new IOException(HOME_DIR + " not set");
                }
        
                String path = home + "/conf/" + url;        
                FileInputStream stream = new FileInputStream(path);
                configurator = new Configurator(stream);
            }
        } catch (Exception e) {
            logger.error("Could not configure MGCP controller", e);
            throw e;
        }
    }
    
    public void setConfigurationByURL(URL url) throws Exception {
    	try {
    		configurator = new Configurator(url.openStream());
    	} catch (Exception e) {
            logger.error("Could not configure MGCP controller", e);
            throw e;
        }
    }
    
    /**
    * Gets the Media Server Home directory.
    * 
    * @TODO This method duplicates the logic in org.mobicents.media.server.bootstrap.Main
    * 
    * @return the path to the home directory.
    */
    private static String getHomeDir() {
    	String mmsHomeDir = System.getProperty(HOME_DIR);
    	if (mmsHomeDir == null) {
    		mmsHomeDir = System.getenv(HOME_DIR);
    	};
    	return mmsHomeDir;
    }
    
    public void createProvider() {
    	mgcpProvider = new MgcpProvider(udpInterface, port);
    }
    
    public void createGlobalTransactionManager() {
    	txManager = new GlobalTransactionManager(scheduler);
    	txManager.setPoolSize(poolSize);
        txManager.setNamingService(endpoints);        
        txManager.setMgcpProvider(mgcpProvider);
    }
    
    /**
     * Starts controller.
     */
    public void start() {
        logger.info("Starting MGCP provider");
        createProvider();  
        mgcpProvider.activate();
        
        try {
            mgcpProvider.addListener(this);
        } catch (TooManyListenersException e) {
        	logger.error(e);
        }
                
        //initialize transaction subsystem                
        createGlobalTransactionManager();
        
        logger.info("Controller started");
    }
    
    /**
     * Stops controller.
     */
    public void stop() {
        mgcpProvider.shutdown();
        logger.info("Controller stopped");
    }

    @Override
    public void process(MgcpEvent event) {
        // Find transaction
        int transactionId = event.getMessage().getTxID();
        Transaction tx = findTransaction(event.getEventID(), transactionId, (InetSocketAddress) event.getAddress());

        // Process transaction
        if (tx != null) {
            tx.process(event);
        } else {
            logger.warn("Could not find MGCP transaction id=" + transactionId);
        }
    }
    
    private Transaction findTransaction(int eventId, int transactionId, InetSocketAddress remoteAddress) {
        if (eventId == MgcpEvent.REQUEST) {
            return txManager.allocateNew(remoteAddress, transactionId);
        } else {
            // TODO find by transaction number - hrosa
            return txManager.find(remoteAddress, transactionId);
        }
    }
     
    public void onStarted(Endpoint endpoint,EndpointInstaller installer) {
        try {
            MgcpEndpoint mgcpEndpoint = configurator.activate(endpoint, mgcpProvider, udpInterface.getLocalBindAddress(), port);
            mgcpEndpoint.setMgcpListener(this);
            endpoints.register(mgcpEndpoint,installer);
            logger.info("Endpoint restarted: " + endpoint.getLocalName());
        } catch (Exception e) {
        	logger.error("Could not register endpoint: " + endpoint.getLocalName());
        }
    }

    public void onStopped(Endpoint endpoint) {
    }
    
}
