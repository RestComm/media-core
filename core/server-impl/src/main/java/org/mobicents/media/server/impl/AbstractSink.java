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
package org.mobicents.media.server.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.Outlet;
import org.mobicents.media.server.spi.events.NotifyEvent;

/**
 * The base implementation of the media sink.
 * 
 * <code>AbstractSource</code> and <code>AbstractSink</code> are implement 
 * general wirring contruct. 
 * All media components have to extend one of these classes.
 * 
 * @author Oleg Kulikov
 */
public abstract class AbstractSink extends BaseComponent implements MediaSink {

    protected transient MediaSource otherParty;
    private volatile boolean started = false;
    private long packetsReceived;
    private long bytesReceived;
    private NotifyEvent evtStarted;
    private NotifyEvent evtStopped;
    protected Logger logger;
    private ReentrantLock state = new ReentrantLock();

    /**
     * Creates new instance of sink with specified name.
     * 
     * @param name the name of the sink to be created.
     */
    public AbstractSink(String name) {
        super(name);
        logger = Logger.getLogger(getClass());
        evtStarted = new NotifyEventImpl(this, NotifyEvent.STARTED,"Started");
        evtStopped = new NotifyEventImpl(this, NotifyEvent.STOPPED,"Stoped");
    }

    /**
     * This methods is called by Media Source to determine the preffred format
     * during connection procedure.
     * 
     * @param set the set of capable frmats
     * @return format which is most preffred for this sink or null if this sink
     * can not determine preffred.
     */
    public Format getPreffered(Collection<Format> set) {
        if (set != null) {
            format = selectPreffered(set);
            return format;
        }
        this.format = null;
        return null;
    }

    public void assignPreffered(Format format) {
        if (otherParty != null) {
            ((AbstractSource) otherParty).setPreffered(format);
        }
    }

    /**
     * Implements the strategy of selecting preffred format from specified set.
     * 
     * The default implemntation takes first from set but other components can 
     * override this rule.
     * 
     * @param set the set of formats.
     * @return preffered format or null if sink can not determine it
     */
    protected Format selectPreffered(Collection<Format> set) {
        return set != null ? set.iterator().next() : null;
    }

    public boolean isMultipleConnectionsAllowed() {
        return false;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#connect(MediaStream).
     */
    public void connect(MediaSource otherParty) {
        state.lock();
        try {
            //other party can not be null
            if (otherParty == null) {
                throw new IllegalArgumentException("Other party can not be null");
            }

            //other party has to extend AbstractSource
            if (!(otherParty instanceof AbstractSource)) {
                throw new IllegalArgumentException("Can not connect: " +
                        otherParty + " does not extends AbstractSource");
            }

            //if other party allows multiple connection (like mixer or mux/demux
            //we should delegate connection procedure to other party because other party
            //maintances internal components
            if (otherParty.isMultipleConnectionsAllowed()) {
                otherParty.connect(this);
                return;
            }

            AbstractSource source = ((AbstractSource) otherParty);

            //calculating common formats
            Collection<Format> subset = this.subset(getFormats(), otherParty.getFormats());

            //connection is possible if and only if both components have common formats
            if (subset.isEmpty()) {
                throw new IllegalArgumentException("Format missmatch");
            }

            //now we have to select preffered format
            //if this sink can not determine preffred format at this time it will return null
            //but sink still responsible to assign preffred format to other side
            Format preffred = getPreffered(subset);
            if (preffred != null) {
                this.format = preffred;
                source.setPreffered(preffred);
            }

            source.otherParty = this;
            this.otherParty = source;

            if (logger.isDebugEnabled()) {
                logger.debug(this + " is connected to " + otherParty);
            }
        } finally {
            state.unlock();
        }
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#disconnect(MediaStream).
     */
    public void disconnect(MediaSource otherParty) {
        state.lock();
        try {
            if (otherParty == null) {
                throw new IllegalArgumentException("Other party can not be null");
            }

            //this implementation suppose to work with AbstractSource
            if (!(otherParty instanceof AbstractSource)) {
                throw new IllegalArgumentException("Can not disconnect: " + otherParty + " is not connected");
            }

            //if other party allows multiple connections then we have to deligate call to other party
            if (otherParty.isMultipleConnectionsAllowed()) {
                otherParty.disconnect(this);
                return;
            }

            //in this case we are checking that other party is connected to this component
            if (otherParty != this.otherParty) {
                throw new IllegalArgumentException("Can not disconnect: " + otherParty + " is not connected");
            }

            //the most common case: sink is connected to source
            AbstractSource source = ((AbstractSource) otherParty);

            //cleaning cross references
            source.otherParty = null;
            this.otherParty = null;

            //cleaning formats
            this.getPreffered(null);
            source.setPreffered(null);
        } finally {
            state.unlock();
        }
    }

    public void connect(Outlet outlet) {
        connect(outlet.getOutput());
    }

    public void disconnect(Outlet outlet) {
        disconnect(outlet.getOutput());
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#isConnected().
     */
    public boolean isConnected() {
        return otherParty != null;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#isStarted().
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * This methos is called when new portion of media arrives.
     * 
     * @param buffer the new portion of media data.
     */
    public abstract void onMediaTransfer(Buffer buffer) throws IOException;

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#start().
     */
    public void start() {
        state.lock();
        try {
            started = true;
            resetStats();
            if (otherParty != null && !otherParty.isStarted()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(this + " starting " + otherParty + ", started=" + otherParty.isStarted());
                }
                otherParty.start();
            }
            started();
        } finally {
            state.unlock();
        }
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#stop().
     */
    public void stop() {
        state.lock();
        try {
            started = false;
            if (otherParty != null && otherParty.isStarted()) {
                otherParty.stop();
            }
            stopped();
        } finally {
            state.unlock();
        }
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#receive().
     */
    public void receive(Buffer buffer) throws IOException {
        if (isStarted()) {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace(this + " arrive " + buffer + " from " + otherParty);
                }
                onMediaTransfer(buffer);
                packetsReceived++;
                bytesReceived += buffer.getLength();
            } catch (Exception e) {
                failed(NotifyEvent.RX_FAILED, e);
                e.printStackTrace();
            } finally {
                buffer.dispose();
            }
        } else {
            logger.warn(this + " is not started");
        }
    }

    /**
     * Sends failure notification.
     * 
     * @param eventID failure event identifier.
     * @param e the exception caused failure.
     */
    protected void failed(int eventID, Exception e) {
        FailureEventImpl failed = new FailureEventImpl(this, eventID, e);
        sendEvent(failed);
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#getPacketsReceived().
     */
    public long getPacketsReceived() {
        return packetsReceived;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#getBytesReceived() 
     */
    public long getBytesReceived() {
        return bytesReceived;
    }

    @Override
    public void resetStats() {
        this.packetsReceived = 0;
        this.bytesReceived = 0;
    }

    /**
     * Sends notification that media processing has been started.
     */
    protected void started() {
        sendEvent(evtStarted);
    }

    /**
     * Sends notification that detection is terminated.
     * 
     */
    protected void stopped() {
        sendEvent(evtStopped);
    }

	/* (non-Javadoc)
	 * @see org.mobicents.media.MediaSink#getInterface(java.lang.Class)
	 */
	public <T> T getInterface(Class<T> interfaceType) {
		//should we check default?
		return null;
	}
    
    
}
