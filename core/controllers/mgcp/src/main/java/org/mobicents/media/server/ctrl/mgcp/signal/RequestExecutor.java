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

package org.mobicents.media.server.ctrl.mgcp.signal;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import javolution.util.FastList;
import org.apache.log4j.Logger;
import org.mobicents.media.server.ctrl.mgcp.UnknownActivityException;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;

/**
 * Executes request received from call agent.
 * 
 * This class established one-to-one relation between requested MGCP event 
 * and <code>Signal</code> object wich implements executable logic. 
 * 
 * Executable signals are gouped into packages similar to MGCP package definition.
 * Each signals can fire one or more events. All requested events intitaly assigned to signals.
 * 
 * 
 * @author kulikov
 */
public class RequestExecutor implements Dispatcher {
    
    //owner of this executor
    private RequestExecutors pool;
    
    /**
     * Queue of signals wich must be executed.
     * Signal is an object wich implements concrete actions for requested signals.
     */ 
    private FastList<Signal> signals = new FastList();
    
    //List of packages specified by this request
    private SignalExecutor[] executors;
    
    //currently active signal
    private Signal currentSignal;
    
    private Dispatcher dispatcher;
    private Logger logger = Logger.getLogger(RequestExecutor.class);
    /**
     * Constructs executor.
     * 
     * @param controller the reference to the controller.
     */
    public RequestExecutor(RequestExecutors pool, SignalExecutor[] executors) {
        this.pool = pool;
        this.executors = executors;
        
        //register itself as dispatcher
        for (SignalExecutor exec : executors) {
            exec.setDispatcher(this);
        }
    }
    
    /**
     * Assigns callback handler for detected events.
     * 
     * @param dispatcher the handler.
     */
    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
    
    /**
     * Initializion procedure.
     * 
     * @param events the list of requested events.
     * @param signals the liste of requested signals.
     */
    public void accept(RequestedEvent[] events, EventName[] signals) throws UnknownSignalException, UnknownEventException, UnknownPackageException, UnknownActivityException {
        //reset all executors before start
        for (SignalExecutor exec : executors) {
            exec.reset();
        }
        
        //accept events first
        if (events != null) {
            for (int i = 0; i < events.length; i++) {
                queue(events[i]);
            }
        }
        
        //accept signals
        if (signals != null) {
            for (int i = 0; i < signals.length; i++) {
                queue(signals[i]);
            }
        }
    }
    
    /**
     * Starts the execution of this request.
     */
    public void execute() {
        if (signals.size() > 0) {
            currentSignal = signals.remove(0);
            currentSignal.execute();
        } else if (dispatcher != null) {
            dispatcher.completed();
        }
    }

    /**
     * Assigns events for signals.
     * 
     * This methods asks package to accept the requested event. 
     * Package knows how to do it.
     * 
     * @param event the event to detect.
     */
    private void queue(RequestedEvent event) throws UnknownEventException, UnknownPackageException {
        getExecutor(event.getEventName()).accept(event);
    }

    /**
     * Constructs accept from requested signals.
     * 
     * This method getting executable equivalent of requested signal and 
     * puts it into the global accept.
     * 
     * Package knows how select signal. Returnable signal already "charged" with respective 
     * requested event detector.
     * 
     * @param event the name of signal.
     */
    private void queue(EventName event) throws UnknownSignalException, UnknownPackageException, UnknownActivityException {
        signals.add(getExecutor(event).getSignal(event));
    }
    
    /**
     * Cancels execution.
     */
    public void cancel() {
        //cancel current signal
        if (currentSignal != null) {
            currentSignal.cancel();
        }
        
        //clean up
        signals.clear();
        
        //return to the pool
        //pool.recycle(this);
    }
    
    public void recycle() {
        //cancels executions of currently active signal
        cancel();
        
        //deregister dispatcher
        this.dispatcher = null;
        
        //reset all executors
        for (SignalExecutor exec : executors) {
            exec.reset();
        }
        
        pool.recycle(this);
    }
    /**
     * Searches package of the specified event.
     * 
     * @param eventName the event name
     * @return signal package object.
     */
    private SignalExecutor getExecutor(EventName eventName) throws UnknownPackageException {
        //try to search is the local list first
        for (SignalExecutor exec : executors) {
            if (exec.getName().equals(eventName.getPackageName().toString())) {
                return exec;
            }
        }
        
        throw new UnknownPackageException(eventName.getPackageName().toString());
    }

    /**
     * (Non Java-doc.)
     * 
     * @see Dispatcher#onEvent(jain.protocol.ip.mgcp.message.parms.EventName) 
     */
    public void onEvent(EventName event) {
        //just dispatch event 
        if (this.dispatcher != null) {
            dispatcher.onEvent(event);
        }
    }

    public void completed() {
        //if no more signals notify dispatcher
        logger.info("Completed signal, remainder = " + signals.size());
        if (signals.isEmpty()) {
            if (this.dispatcher != null) {
                dispatcher.completed();
            }
            return;
        }
        
        //start next signal
        execute();
    }

    public Endpoint getEndpoint() {
        if (this.dispatcher != null) {
            return dispatcher.getEndpoint();
        } 
        return null;
    }

    public Connection getConnection(String ID) throws UnknownActivityException {
        if (this.dispatcher != null) {
            return dispatcher.getConnection(ID);
        } 
        return null;
    }
}
