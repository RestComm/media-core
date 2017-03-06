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

package org.restcomm.javax.media.mscontrol.container;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpListener;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.ModifyConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.ConflictingParameterException;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.GainControl;
import jain.protocol.ip.mgcp.message.parms.LocalOptionExtension;
import jain.protocol.ip.mgcp.message.parms.LocalOptionValue;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import java.io.Serializable;
import java.util.concurrent.ScheduledExecutorService;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.join.Joinable.Direction;
import javax.media.mscontrol.networkconnection.NetworkConnection;

import org.restcomm.fsm.FSM;
import org.restcomm.fsm.State;
import org.restcomm.fsm.StateEventHandler;
import org.restcomm.fsm.TransitionHandler;
import org.restcomm.fsm.UnknownTransitionException;
import org.restcomm.javax.media.mscontrol.mixer.MediaMixerImpl;
import org.restcomm.javax.media.mscontrol.networkconnection.NetworkConnectionImpl;

/**
 *
 * @author kulikov
 */
public class Link extends FSM {
    
    private ContainerImpl[] containers = new ContainerImpl[2];
    protected ConnectionIdentifier[] connections = new ConnectionIdentifier[2];
    
    protected Direction direction;
    protected Serializable context;
    
    private LinkListener listener;
    
    public Link(ScheduledExecutorService scheduler, ContainerImpl a, ContainerImpl b) {
        super(scheduler);
        
        //remember link ends
        containers[0] = a;
        containers[1] = b;
        
        //define states
        this.createState("NULL").setOnEnter(new OnDeactivate(this));
        this.createState("CONNECTING");
        this.createState("CONNECTED");
        this.createState("ACTIVE").setOnEnter(new OnActivate(this));
        this.createState("DISCONNECTING").setOnEnter(new UnjoinRequest());
        this.createState("DISCONNECTED_1");
        this.createState("CANCELED");
        this.createState("STARTING");
        this.createState("STARTED_1");
        
        //define transitions
        
        //---------------------------------------------------------------------
        //             State NULL
        //---------------------------------------------------------------------
        this.createTransition("join", "NULL", "CONNECTING").setHandler(new JoinRequest());
        this.createTransition("success", "NULL", "CONNECTED"); 
        //---------------------------------------------------------------------
        //             State CONNECTING
        //---------------------------------------------------------------------
        this.createTransition("success", "CONNECTING", "CONNECTED").setHandler(new InitiateTransmission());
        this.createTransition("failure", "CONNECTING", "NULL");
        this.createTransition("release", "CONNECTING", "CANCELED");
        this.createTimeoutTransition("CONNECTING", "NULL", 5000);
        
        //---------------------------------------------------------------------
        //             State CONNECTED
        //---------------------------------------------------------------------
        this.createTransition("join", "CONNECTED", "STARTING").setHandler(new StartRequest());
        this.createTransition("release", "CONNECTED", "DISCONNECTING");
        this.createTransition("success", "CONNECTED", "CONNECTED"); 
        
        //---------------------------------------------------------------------
        //             State DISCONNECTING
        //---------------------------------------------------------------------
        this.createTransition("success", "DISCONNECTING", "DISCONNECTED_1");
        this.createTransition("failure", "DISCONNECTING", "DISCONNECTING");
        
        //---------------------------------------------------------------------
        //             State DISCONNECTED_1
        //---------------------------------------------------------------------
        this.createTransition("success", "DISCONNECTED_1", "NULL");

                
        //---------------------------------------------------------------------
        //             State CANCELED
        //---------------------------------------------------------------------
        this.createTransition("success", "CANCELED", "DISCONNECTING");
        this.createTransition("failure", "CANCELED", "NULL");
        this.createTimeoutTransition("CANCELED", "NULL", 5000);
        
        
        //---------------------------------------------------------------------
        //             State STARTING
        //---------------------------------------------------------------------
        this.createTransition("success", "STARTING", "STARTED_1");
        this.createTransition("failure", "STARTING", "DISCONNECTING");
        this.createTimeoutTransition("STARTING", "DISCONNECTING", 5000);
        
        //---------------------------------------------------------------------
        //             State STARTED_1
        //---------------------------------------------------------------------
        this.createTransition("success", "STARTED_1", "ACTIVE");
        this.createTransition("failure", "STARTED_1", "DISCONNECTING");
        this.createTimeoutTransition("STARTED_1", "DISCONNECTING", 5000);

        
        //---------------------------------------------------------------------
        //             State ACTIVE
        //---------------------------------------------------------------------
        this.createTransition("join", "ACTIVE", "STARTING").setHandler(new StartRequest());
        this.createTransition("release", "ACTIVE", "DISCONNECTING");
        
        setStart("NULL");
        setEnd("NULL");
    }
    
    public ContainerImpl getContainer(int i) {
        return containers[i];
    }
    
    public void setListener(LinkListener listener) {
        this.listener = listener;
    }
    
    public Direction getDirection() {
        return this.direction;
    }
    
    private ConnectionMode getMode0(ContainerImpl container) {
        if (container instanceof MediaMixerImpl) {
            return ConnectionMode.Confrnce;
        } else switch (direction) {
            case DUPLEX:
                return ConnectionMode.SendRecv;
            case RECV :
                return ConnectionMode.RecvOnly;
            default :
                return ConnectionMode.SendOnly;
        }
    }

    private ConnectionMode getMode1(ContainerImpl container) {
        if (container instanceof MediaMixerImpl) {
            return ConnectionMode.Confrnce;
        } else switch (direction) {
            case DUPLEX:
                return ConnectionMode.SendRecv;
            case RECV :
                return ConnectionMode.SendOnly;
            default :
                return ConnectionMode.RecvOnly;
        }
    }
    
    private Direction inversion(Direction direction) {
        switch (direction) {
            case SEND :
                return Direction.RECV;
            case RECV :
                return Direction.SEND;
            default :
                return Direction.DUPLEX;
        }
    }
    
    private class InitiateTransmission implements TransitionHandler {

        public void process(State state) {
            signalAsync("join");
        }
        
    }
    
    private class JoinRequest implements TransitionHandler {

        public void process(State state) {
            //wait if another transaction is in progress for endpoint containers[0]
            if (containers[0].endpoint.concreteNameExpectedSoon()) {
                try {
                    containers[0].endpoint.await();
                } catch (InterruptedException e) {
                    signalAsync("failure");
                    return;
                }
            }
            
            //block others who want to access containers[0]
            if (!containers[0].endpoint.hasConcreteName()) {
                containers[0].endpoint.expectingConcreteName();
            }

            
            //wait if another transaction is in progress for endpoint containers[1]
            if (containers[1].endpoint.concreteNameExpectedSoon()) {
                try {
                    containers[1].endpoint.await();
                } catch (InterruptedException e) {
                    signalAsync("failure");
                    return;
                }
            }
            
            //block others who want to access containers[0]
            if (!containers[1].endpoint.hasConcreteName()) {
                containers[1].endpoint.expectingConcreteName();
            }
            
            
            //prepare and send request for creating two connections.
            int txID = containers[0].session.getDriver().getNextTxID();

            CreateConnection crcx = new CreateConnection(this, containers[0].session.getCallID(),
                    containers[0].endpoint.getIdentifier(), ConnectionMode.Inactive);
            crcx.setTransactionHandle(txID);
            crcx.setNotifiedEntity(containers[0].session.getDriver().getCallAgent());

            try {
                crcx.setSecondEndpointIdentifier(containers[1].endpoint.getIdentifier());
            } catch (ConflictingParameterException e) {
            }

            containers[0].session.getDriver().attach(txID, new JoinResponse());
            containers[0].session.getDriver().send(crcx);
        }        
    }
    
    public class JoinResponse implements JainMgcpListener {

        public void processMgcpCommandEvent(JainMgcpCommandEvent evt) {
        }

        public void processMgcpResponseEvent(JainMgcpResponseEvent evt) {
            CreateConnectionResponse resp = (CreateConnectionResponse) evt;
            switch (resp.getReturnCode().getValue()) {
                case ReturnCode.TRANSACTION_BEING_EXECUTED:
                    return;
                case ReturnCode.TRANSACTION_EXECUTED_NORMALLY:
                    containers[0].setConcreteName(resp.getSpecificEndpointIdentifier());
                    containers[1].setConcreteName(resp.getSecondEndpointIdentifier());
                    
                    connections[0] = resp.getConnectionIdentifier();
                    connections[1] = resp.getSecondConnectionIdentifier();
                                        
                    try {
                        signal("success");
                    } catch (Exception e) {
                    }
                    
                    break;
                default:
                    try {
                        signal("failure");
                    } catch (Exception e) {
                    }
            }
        }
        
    }

    private class StartRequest implements TransitionHandler {

        public void process(State state) {
            //first leg
            int txID = containers[0].session.getDriver().getNextTxID();

            ConnectionMode mode0 = getMode0(containers[0]);
            ModifyConnection mdcx = new ModifyConnection(this, containers[0].session.getCallID(),
                    containers[0].endpoint.getIdentifier(), connections[0]);
            mdcx.setMode(mode0);
            mdcx.setTransactionHandle(txID);

            if(containers[0] instanceof MediaMixerImpl && ((MediaMixerImpl)containers[0]).hasDtmfClamp())
            {
            	LocalOptionExtension currExtention=new LocalOptionExtension("x-dc","true");
            	mdcx.setLocalConnectionOptions(new LocalOptionValue[]{currExtention});
            }
            
            containers[0].session.getDriver().attach(txID, new StartResponse());
            containers[0].session.getDriver().send(mdcx);
            
            
            //second leg
            txID = containers[1].session.getDriver().getNextTxID();
            ConnectionMode mode1 = getMode1(containers[1]);

            mdcx = new ModifyConnection(this, containers[1].session.getCallID(),
                    containers[1].endpoint.getIdentifier(), connections[1]);
            mdcx.setMode(mode1);
            mdcx.setTransactionHandle(txID);

            if(containers[1] instanceof MediaMixerImpl && ((MediaMixerImpl)containers[1]).hasDtmfClamp())
            {
            	LocalOptionExtension currExtention=new LocalOptionExtension("x-dc","true");
            	mdcx.setLocalConnectionOptions(new LocalOptionValue[]{currExtention});
            }
            
            containers[0].session.getDriver().attach(txID, new StartResponse());
            containers[0].session.getDriver().send(mdcx);
        }
    }

    public class StartResponse implements JainMgcpListener {

        private MediaErr error;
        private String errorMsg;

        public StartResponse() {
        }

        public void processMgcpCommandEvent(JainMgcpCommandEvent evt) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void processMgcpResponseEvent(JainMgcpResponseEvent evt) {
            ModifyConnectionResponse resp = (ModifyConnectionResponse) evt;
            switch (resp.getReturnCode().getValue()) {
                case ReturnCode.TRANSACTION_BEING_EXECUTED:
                    return;
                case ReturnCode.TRANSACTION_EXECUTED_NORMALLY:                    
                    try {
                        signal("success");
                    } catch (UnknownTransitionException e) {
                    }
                    break;
                default:
                    try {
                        signal("failure");
                    } catch (UnknownTransitionException e) {
                    }
                    break;
            }
        }
    }

    private class UnjoinRequest implements StateEventHandler {

        public void onEvent(State state) {
            //first leg
            if (connections[0] != null) {
                int txID = containers[0].getMediaSession().getDriver().getNextTxID();
                DeleteConnection req = new DeleteConnection(this,
                        containers[0].session.getCallID(), containers[0].endpoint.getIdentifier(),
                        connections[0]);
                req.setTransactionHandle(txID);

                containers[0].session.getDriver().attach(txID, new UnjoinResponse());
                containers[0].session.getDriver().send(req);
            } else {
                signalAsync("success");
            }

            //second leg
            if (connections[1] != null) {
                int txID = containers[1].getMediaSession().getDriver().getNextTxID();
                DeleteConnection req = new DeleteConnection(this,
                        containers[1].session.getCallID(), containers[1].endpoint.getIdentifier(),
                        connections[1]);
                req.setTransactionHandle(txID);

                containers[1].session.getDriver().attach(txID, new UnjoinResponse());
                containers[1].session.getDriver().send(req);
            } else {
                signalAsync("success");
            }
        }
    }

    private class UnjoinResponse implements JainMgcpListener {

        public void processMgcpCommandEvent(JainMgcpCommandEvent evt) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void processMgcpResponseEvent(JainMgcpResponseEvent evt) {
            try {
                signal("success");
            } catch (UnknownTransitionException e) {
            }
        }
    }
    
    public class OnActivate implements StateEventHandler {
        private Link link;
        
        public OnActivate(Link link) {
            this.link = link;
        }
        
        public void onEvent(State state) {
            if (listener != null)  listener.joined(link);
        }
    }
    
    public class OnDeactivate implements StateEventHandler {
        private Link link;
        
        public OnDeactivate(Link link) {
            this.link = link;
        }
        
        public void onEvent(State state) {
            if (listener != null)  listener.unjoined(link);
        }
    }
}
