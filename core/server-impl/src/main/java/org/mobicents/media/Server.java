/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;

import org.mobicents.media.server.impl.clock.Scheduler;
import org.mobicents.media.server.impl.naming.InnerNamingService;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointFactory;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.clock.Timer;
import org.mobicents.media.server.spi.rtp.RtpManager;
import org.mobicents.media.server.spi.rtp.RtpListener;

/**
 *
 * @author kulikov
 */
public class Server implements MediaServer {

    public final static Scheduler scheduler = new Scheduler();
    
    private InnerNamingService namingService;
    private RtpManager rtpFactory;
    private RtpErrorHandler rtpErrorHandler;
    private ArrayList<EndpointFactory> factories = new ArrayList();
    private Logger logger = Logger.getLogger(Server.class);

    public Server() {
        rtpErrorHandler = new RtpErrorHandler();
        namingService = new InnerNamingService();
    }

    public void setTimer(Timer timer) {
        scheduler.setTimer(timer);
    }
    
    public void register(Endpoint endpoint) throws ResourceUnavailableException {
        namingService.addEndpoint(endpoint);
    }


    public RtpManager getRtpManager() {
        return rtpFactory;
    }

    public void setRtpManager(RtpManager rtpFactory) {
        this.rtpFactory = rtpFactory;
        rtpFactory.setListener(rtpErrorHandler);
    }

    public void addFactory(EndpointFactory factory) throws ResourceUnavailableException {
        factories.add(factory);
        factory.setRtpManager(rtpFactory);
        logger.info("Added factory " + factory);

        Collection<Endpoint> list = factory.install();
        logger.info("Installing " + list.size() + "  endpoints");
        for (Endpoint e : list) {
            try {
                e.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new ResourceUnavailableException(ex);
            }
            logger.info("Started endpoint " + e.getLocalName());
            namingService.addEndpoint(e);
        }
    }

    public void removeFactory(EndpointFactory factory) {
        factories.remove(factory);
    }

    public void start() throws Exception {
        scheduler.start();
        
        logger.info("Starting media server");
        if (rtpFactory != null) {
            rtpFactory.start(System.currentTimeMillis());
        }
        logger.info("Started media server instance =======================");
    }

    public void stop() {
        scheduler.stop();
        logger.info("Stopped media server instance ");
    }

    private void restart() {
    }

    private class RtpErrorHandler implements RtpListener {

        public void notify(Exception e) {
            restart();
        }
    }

    public Endpoint lookup(String name, boolean bussy) throws ResourceUnavailableException {
        return namingService.lookup(name, bussy);
    }
    
    public Endpoint[] lookupall(String endpointName) throws ResourceUnavailableException {
    	return namingService.lookupall(endpointName);
    }

    public int getEndpointCount() {
        return namingService.getEndpointCount();
    }
}
