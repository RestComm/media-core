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

package org.mobicents.media.server.mgcp.pkg.au;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.mgcp.controller.signal.Event;
import org.mobicents.media.server.mgcp.controller.signal.NotifyImmediately;
import org.mobicents.media.server.mgcp.controller.signal.Signal;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.utils.Text;

/**
 * Implements play announcement signal.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class Play extends Signal implements PlayerListener {

    private final static Logger log = Logger.getLogger(Play.class);

    // Response Messages
    private static final Text MSG_NO_PLAYER = new Text("Endpoint has no player");
    private static final Text MSG_RC_301 = new Text("rc=301");
    private static final Text MSG_RC_312 = new Text("rc=312");
    private static final Text MSG_RC_100 = new Text("rc=100");

    // Media Components
    private Player player;
    private volatile Options options;

    // Play Properties
    private Iterator<Text> segments;
    private int repeatCount;
    private int segCount;
    private long delay;
    private String uri;
    private final AtomicBoolean terminated;

    // MGCP Properties
    private final Event oc;
    private final Event of;

    // Concurrency
    private final Object lock;

    public Play(String name) {
        super(name);

        // Play Properties
        this.terminated = new AtomicBoolean(false);

        // MGCP Properties
        this.oc = new Event(new Text("oc"));
        this.oc.add(new NotifyImmediately("N"));
        this.of = new Event(new Text("of"));
        this.of.add(new NotifyImmediately("N"));

        // Concurrency
        this.lock = new ReentrantLock();
    }

    @Override
    public void execute() {
        // get access to player
        player = (Player) getEndpoint().getResource(MediaType.AUDIO, ComponentType.PLAYER);

        // check result
        if (player == null) {
            of.fire(this, MSG_NO_PLAYER);
            complete();
            return;
        }

        // register announcement handler
        try {
            player.addListener(this);
        } catch (TooManyListenersException e) {
            log.error("OPERATION FAILURE", e);
        }

        // get options of the request
        options = Options.allocate(getTrigger().getParams());

        // set initial delay
        delay = 0;

        // get announcement segments
        segments = options.getSegments().iterator();
        repeatCount = options.getRepeatCount();
        segCount = 0;

        uri = segments.next().toString();

        // Need to manually set terminated to false at this point
        // Because object is recycled and reset is always called before this method.
        this.terminated.set(false);

        // start announcement
        startAnnouncementPhase();
    }

    private void startAnnouncementPhase() {
        synchronized (this.lock) {
            // Hotfix for concurrency issues
            // https://github.com/RestComm/mediaserver/issues/162
            if (this.terminated.get()) {
                if (log.isInfoEnabled()) {
                    log.info("Skipping announcement phase because play has been terminated.");
                }
                return;
            }

            if (log.isInfoEnabled()) {
                log.info(String.format("(%s) Start announcement (segment=%d)", getEndpoint().getLocalName(), segCount));
            }

            try {
                player.setURL(uri);
            } catch (MalformedURLException e) {
                if (log.isInfoEnabled()) {
                    log.warn("Invalid URL format: " + uri);
                }
                of.fire(this, MSG_RC_301);
                complete();
                return;
            } catch (ResourceUnavailableException e) {
                if (log.isInfoEnabled()) {
                    log.info("URL cannot be found: " + uri);
                }
                of.fire(this, MSG_RC_312);
                complete();
                return;
            }

            // set max duration if present
            if (options.getDuration() != -1) {
                player.setDuration(options.getDuration());
            }

            // set initial offset
            if (options.getOffset() > 0) {
                player.setMediaTime(options.getOffset());
            }

            // initial delay
            player.setInitialDelay(delay);

            // starting
            player.activate();
        }
    }

    @Override
    public boolean doAccept(Text event) {
        if (!oc.isActive() && oc.matches(event)) {
            return true;
        }

        if (!of.isActive() && of.matches(event)) {
            return true;
        }

        return false;
    }

    @Override
    public void cancel() {
        terminate();
    }

    @Override
    public void reset() {
        super.reset();
        terminate();

        oc.reset();
        of.reset();
    }

    private void terminate() {
        synchronized (this.lock) {
            if (!this.terminated.get()) {
                this.terminated.set(true);
                cleanup();
            }
        }
    }

    private void cleanup() {
        if (player != null) {
            player.removeListener(this);
            player.deactivate();
            player = null;
        }

        if (options != null) {
            Options.recycle(options);
            options = null;
        }
    }

    private void repeat(long delay) {
        this.delay = delay;
        startAnnouncementPhase();
    }

    private void next(long delay) {
        uri = segments.next().toString();
        segCount++;

        this.delay = delay;
        startAnnouncementPhase();
    }

    @Override
    public void process(PlayerEvent event) {
        switch (event.getID()) {
            case PlayerEvent.STOP:
                if (log.isInfoEnabled()) {
                    log.info(String.format("(%s) Announcement (segment=%d) has completed", getEndpoint().getLocalName(),
                            segCount));
                }

                if (repeatCount == -1) {
                    repeat(options.getInterval());
                    return;
                } else {
                    repeatCount--;

                    if (repeatCount > 0) {
                        repeat(options.getInterval());
                        return;
                    }
                }

                if (segments.hasNext()) {
                    repeatCount = options.getRepeatCount();
                    next(options.getInterval());
                    return;
                }

                terminate();
                oc.fire(this, MSG_RC_100);
                this.complete();
                break;

            case PlayerEvent.FAILED:
                terminate();
                oc.fire(this, null);
                this.complete();
                break;
        }
    }
}