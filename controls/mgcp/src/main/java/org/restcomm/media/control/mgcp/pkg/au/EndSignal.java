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

package org.restcomm.media.control.mgcp.pkg.au;

import org.apache.log4j.Logger;
import org.restcomm.media.ComponentType;
import org.restcomm.media.control.mgcp.controller.signal.Signal;
import org.restcomm.media.spi.Endpoint;
import org.restcomm.media.spi.MediaType;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.utils.Text;

/**
 * Gracefully terminates Play, PlayRecord or PlayCollect signal.
 * 
 * 
 * @author yulian oifa
 */
public class EndSignal extends Signal {
    
    private volatile Options options;
    
    private DtmfDetector dtmfDetector;
    private final static Logger logger = Logger.getLogger(EndSignal.class);
    
    public EndSignal(String name) {
        super(name);
    }
    
    @Override
    public void execute() {
    	logger.info("Terminating signals");
        //get options of the request
        options = Options.allocate(getTrigger().getParams());
        
        Endpoint endpoint = getEndpoint();        
        if (options.isClearDigits() && endpoint.hasResource(MediaType.AUDIO, ComponentType.DTMF_DETECTOR)) {
            dtmfDetector = (DtmfDetector) getEndpoint().getResource(MediaType.AUDIO, ComponentType.DTMF_DETECTOR);
            dtmfDetector.clearDigits();
            logger.info("Clear digits");
        }                
    }

    @Override
    public boolean doAccept(Text event) {
        return false;
    }

    @Override
    public void cancel() {
    }

    @Override
    public void reset() {
        super.reset();
    }            
}
