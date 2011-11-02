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
package org.mobicents.media.server.impl.resource.zap;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.mobicents.media.server.ConnectionFactory;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointFactory;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.rtp.RtpManager;

/**
 *
 * @author kulikov
 */
public class Trunk implements EndpointFactory {

    private ArrayList<Endpoint> endpoints = new ArrayList();
    private String name;
    private RtpManager rtpFactory;
    private ConnectionFactory connectionFactory;
    private int span;
    private String ranges;
    private int[] firstCIC;
    private boolean linkUp = false;

    private static Logger logger = Logger.getLogger(Trunk.class);
    
    public Trunk() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFirstCIC(String cic) {
        String tokens[] = cic.split(",");
        firstCIC = new int[tokens.length];

        for (int i = 0; i < tokens.length; i++) {
            firstCIC[i] = Integer.parseInt(tokens[i]);
        }
    }

    public void setSpan(int span) {
        this.span = span;
    }

    public void setChannels(String ranges) {
        this.ranges = ranges;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setRtpManager(RtpManager rtpFactory) {
        this.rtpFactory = rtpFactory;
    }

    public RtpManager getRtpManager() {
        return this.rtpFactory;
    }


    public void linkUp() {
        if (logger.isInfoEnabled()) {
            logger.info("Received L4 Up event from layer3.");
        }
        this.linkUp = true;
//        this.streamData(_LINK_STATE_UP);
    }

    public void linkDown() {
        if (logger.isInfoEnabled()) {
            logger.info("Received L4 Down event from layer3.");
        }
        this.linkUp = false;
        // FIXME: proper actions here.
        // this.txBuff.clear();
        // this.txBuff.limit(0);
        // this.readBuff.clear();
//        this.streamData(_LINK_STATE_DOWN);
    }

    public void receive(byte[] arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Endpoint> install() throws ResourceUnavailableException {
        String[] subranges = ranges.split(",");
        for (int i = 0; i < subranges.length; i++) {
            String tokens[] = subranges[i].split("-");
            int low = Integer.parseInt(tokens[0]);
            int high = Integer.parseInt(tokens[1]);

            for (int k = low; k <= high; k++) {
                int zapid = 31 * (span - 1) + k;
                String path = "/dev/dahdi/" + zapid;
                
                DahdiEndpointImpl endpoint = new DahdiEndpointImpl(name + "/" + k, path);
                endpoint.setRtpManager(rtpFactory);
                endpoint.setConnectionFactory(connectionFactory);
                endpoints.add(endpoint);
            }
        }
        return endpoints;
    }

    public void uninstall() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
