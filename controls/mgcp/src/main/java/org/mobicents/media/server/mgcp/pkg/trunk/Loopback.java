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

package org.mobicents.media.server.mgcp.pkg.trunk;

import org.mobicents.media.core.endpoints.BaseSS7EndpointImpl;
import org.mobicents.media.server.mgcp.controller.signal.Event;
import org.mobicents.media.server.mgcp.controller.signal.NotifyImmediately;
import org.mobicents.media.server.mgcp.controller.signal.Signal;
import org.mobicents.media.server.utils.Text;
/**
 * Implements loopback signal.
 * 
 * @author Oifa Yulian
 */
public class Loopback extends Signal {    
	
	private Event of = new Event(new Text("of"));
	
    private volatile Options options;
    
    public Loopback(String name) {
        super(name);  
        of.add(new NotifyImmediately("N"));        
    }
    
    @Override
    public void execute() {
    	//check result
        if (!(getEndpoint() instanceof BaseSS7EndpointImpl)) {
            of.fire(this, new Text("Endpoint is not ss7 endpoint"));
            complete();
            return;
        }
        
        //get options of the request
        options = new Options(getTrigger().getParams());        
                
        if(options.isDeactivation())
        {
        	//deactivate pipe
        	((BaseSS7EndpointImpl) getEndpoint()).setLoop(false);
        }
        else
        {
        	//activate pipe
        	((BaseSS7EndpointImpl) getEndpoint()).setLoop(true);
        }
        
        //signal does not have anything else , only looping ss7 channel
    }
    
    @Override
    public boolean doAccept(Text event) {
        if (!of.isActive() && of.matches(event)) {
            return true;
        }
        
        return false;
    }

    @Override
    public void reset() {
        super.reset();
        
        ((BaseSS7EndpointImpl) getEndpoint()).setLoop(false);
        
        of.reset();        
    }
    
    @Override
    public void cancel() {    
    	((BaseSS7EndpointImpl) getEndpoint()).setLoop(false);
    }    
}
