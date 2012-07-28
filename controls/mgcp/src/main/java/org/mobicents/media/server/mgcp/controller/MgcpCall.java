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

import org.mobicents.media.server.utils.Text;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * MGCP call.
 * 
 * @author kulikov
 */
public class MgcpCall {

    private CallManager callManager;
    protected Text id = new Text(new byte[30], 0, 30);
    protected ConcurrentHashMap<Text,MgcpConnection> connections=new ConcurrentHashMap(20);
    
    protected MgcpCall(CallManager callManager, Text id) {
        id.duplicate(this.id);
        this.id.trim();
        
        this.callManager = callManager;
    }

    public MgcpConnection getMgcpConnection(Text id) {
    	Text currText;
    	for (Enumeration<Text> e = connections.keys() ; e.hasMoreElements() ;) {
    		currText=e.nextElement();
    		if(currText.equals(id))
    			return connections.get(currText);    		
        }
    	
    	return null;    	
    }
    
    /**
     * Excludes connection activity from this call.
     * 
     * @param activity the activity to be excluded.
     */
    public void exclude(MgcpConnection activity) {
    	connections.remove(activity.id);

        //if no more connections terminate the entire call
        if (connections.isEmpty()) {
           callManager.terminate(this);
        }
    }
    
    /**
     * The amount of connections related to this call.
     * 
     * @return the amount.
     */
    public int size() {
        return connections.size();
    }
    
    public void deleteConnections() { 
    	MgcpConnection currConnection;
    	for (Enumeration<Text> e = connections.keys() ; e.hasMoreElements() ;) {
    		currConnection=connections.remove(e.nextElement());
    		currConnection.mgcpEndpoint.deleteConnection(currConnection.getID());    
        }   
    	
    	if (connections.isEmpty()) {
            callManager.terminate(this);
        }
    }
    
    @Override
    public String toString() {
        return "call[" + id + "]";
    }
}
