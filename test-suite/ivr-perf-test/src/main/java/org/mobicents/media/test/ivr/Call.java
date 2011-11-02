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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.test.ivr;

import jain.protocol.ip.mgcp.JainMgcpProvider;
import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TooManyListenersException;
import org.apache.log4j.Logger;

/**
 *
 * @author kulikov
 */
public class Call {
    private static int GEN_ID;
    
    private Connection[] connections = new Connection[2];
    private JainMgcpProvider provider;
    
    private int id = 0;
    
    protected CallIdentifier callID;
    private int duration;
    
    private Tester tester;
    public final static Timer timer = new Timer();
    
    private CallReport report;
    private boolean isFalied = false;
    
    private static Logger logger = Logger.getLogger(Call.class);
    
    public Call(Tester tester, JainMgcpProvider provider, int duration) throws TooManyListenersException {
        id = ++GEN_ID;
        
        callID = new CallIdentifier(Integer.toHexString(id));
        
        this.tester = tester;
        this.provider = provider;
        this.duration = duration;
        
        connections[0] = new Connection(0, this, provider);
        connections[1] = new Connection(1, this, provider);
        
        report = new CallReport(id);
        logger.info("Created call " + callID);
    }
    
    public int getID() {
        return id;
    }
    
    public String getReport() {
        return report.toString() + "\n";
    }
    
    public void setup() {
        //we are creating first connection asynchronously
        logger.info("Call=" + callID + ", Creating connection[0]");
        connections[0].create();
    }
    
    protected void onConnectionCreated(Connection connection) {
        if (connection.getID() == 0) {
            //first connection created, we are creating second connection
            logger.info("Call=" + callID + ", Created connection[0], Creating connection[1]");
            connections[1].create(connection.getLocalSDP());
        } 
    }
    
    protected void onConnectionConnected(Connection connection) {
        logger.info("Call=" + callID + ", Connected connection[" + connection.getID() +"]");
        if (connections[0].getState() == Connection.STATE_OPENED && 
                connections[1].getState() == Connection.STATE_OPENED) {
            timer.schedule(new CloseTask(), duration * 1000);
        } else if (connection.getID() == 1) {
            //second connection created, we are connecting it to first connection
            logger.info("Call=" + callID + ", Created and connected connection[1], Modifing connection[0]");
            connections[0].modify(connection.getLocalSDP());
        } 
    }

    protected void onConnectionDisconnected(Connection connection) {
        logger.info("Call=" + callID + ", Disconnected connection[" + connection.getID() +"]");
        report.addStats(connection.bytesReceived, connection.bytesSent, connection.jitter);
        if (connections[0].getState() == Connection.STATE_DELETED && 
                connections[1].getState() == Connection.STATE_DELETED) {
            logger.info("Removing call " + callID);
            tester.removeCall(this);
        }
    }
    
    private boolean needDelete(Connection connection) {
        switch (connection.getState()) {
            case Connection.STATE_NULL :
            case Connection.STATE_DELETED :
            case Connection.STATE_CREATING :
                return false;
            default :
                return true;
        }
    }
    
    public boolean isFailed() {
        return this.isFalied;
    }
    
    protected void onFailed(Connection connection) {
        if (needDelete(connections[0])) {
            connections[0].delete();
        }

        if (needDelete(connections[1])) {
            connections[1].delete();
        }
        this.isFalied = true;
        tester.removeCall(this);
    }
    
    public void startMedia() {
        connections[0].startMedia();
        connections[1].startMedia();
    }
    
    private class CloseTask extends TimerTask {
        @Override
        public void run() {
            logger.info("Call=" + callID + ", Disconning");
            connections[0].delete();
            connections[1].delete();
            cancel();
        }
    }
}
