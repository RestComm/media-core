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

import java.util.concurrent.ConcurrentHashMap;
import java.util.Enumeration;
import org.mobicents.media.server.utils.Text;

/**
 * Maintains MGCP calls.
 * 
 * @author kulikov
 */
public class CallManager {
    //list of active calls
	private ConcurrentHashMap<Text,MgcpCall> calls=new ConcurrentHashMap();
    
    public MgcpCall getCall(Text id, boolean allowNew) {
    	Text currText;
    	for (Enumeration<Text> e = calls.keys() ; e.hasMoreElements() ;) {
    		currText=e.nextElement();
    		if(currText.equals(id))
    			return calls.get(currText);    		
        }
    	
    	if (!allowNew) return null;
        
    	MgcpCall call = new MgcpCall(this, id);
    	calls.put(call.id,call); 
        
    	return call;    	
    }
    
    /**
     * Terminates specified call.
     * 
     * @param call the call to be terminated
     */
    protected void terminate(MgcpCall call) {
    	calls.remove(call.id);    	
    }    
    
}
