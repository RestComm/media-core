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

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.mobicents.media.server.mgcp.controller.signal.Event;
import org.mobicents.media.server.mgcp.controller.signal.NotifyImmediately;
import org.mobicents.media.server.mgcp.controller.signal.Signal;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.utils.Text;

/**
 * Implements play announcement signal.
 * 
 * @author kulikov
 */
public class Play extends Signal implements PlayerListener {
    
    private Event oc = new Event(new Text("oc"));
    private Event of = new Event(new Text("of"));
    
    private Player player;
    private volatile Options options;
    
    private int repeatCount;
    private int segCount;
    
    
    private long delay;
    private String uri;
    
    private Iterator<Text> segments;
    
    private final static Logger logger = Logger.getLogger(Play.class);
    
    public Play(String name) {
        super(name);
        oc.add(new NotifyImmediately("N"));
        of.add(new NotifyImmediately("N"));
    }
    
    @Override
    public void execute() {
    	//get access to player
        player = this.getPlayer();

        //check result
        if (player == null) {
            of.fire(this, new Text("Endpoint has no player"));
            complete();
            return;
        }
        
        //register announcement handler
        try {
            player.addListener(this);
        } catch (TooManyListenersException e) {
        }
        
        //get options of the request
        options = new Options(getTrigger().getParams());        
                
        //set initial delay
        delay = 0;
        
        //get announcement segments
        segments = options.getSegments().iterator();
        repeatCount = options.getRepeatCount();
        
        uri = segments.next().toString();
        
        //start announcement
        startAnnouncementPhase();        
    }

    private void startAnnouncementPhase() {
        logger.info(String.format("(%s) Start announcement (segment=%d)", getEndpoint().getLocalName(), segCount));
        
        try {
            player.setURL(uri);
        } catch (MalformedURLException e) {
            of.fire(this, new Text("rc=301"));
            complete();
            return;
        } catch (ResourceUnavailableException e) {
            of.fire(this, new Text("rc=312"));
            complete();
            return;
        }
        
        //set max duration if present
        if (options.getDuration() != -1) {
            player.setDuration(options.getDuration() * 1000000L);
        }

        //set initial offset
        if (options.getOffset() > 0) {
            player.setMediaTime(options.getOffset() * 1000000L);
        }

        //initial delay
        player.setInitialDelay(delay * 1000000L);

        //starting
        player.start();
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
        if (player != null) {
            player.removeListener(this);
            player.stop();
        }
    }

    private Player getPlayer() {
        if (getTrigger().getConnectionID() == null) {
            Endpoint endpoint = getEndpoint();
            return (Player) getEndpoint().getResource(MediaType.AUDIO, Player.class); 
        }
        
        String connectionID = getTrigger().getConnectionID().toString();
        Connection connection = getConnection(connectionID);
        
        if (connection == null) {
            return null;
        }
        
//        MediaSource source = (MediaSource) connection.getComponent(MediaType.AUDIO, Player.class);
//        if (source == null) {
//            return null;
//        }
                
//        return source.getInterface(Player.class);
        return null;
    }
    
    @Override
    public void reset() {
        super.reset();
        if (player != null) {
            player.removeListener(this);
            player.stop();
        }
        
        oc.reset();
        of.reset();
        
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

    public void process(PlayerEvent event) {
        switch (event.getID()) {
            case PlayerEvent.STOP :
                logger.info(String.format("(%s) Announcement (segment=%d) has completed", getEndpoint().getLocalName(), segCount));
                repeatCount--;
                
                if (repeatCount > 0) {
                    repeat(options.getInterval());
                    return;
                }
                
                if (segments.hasNext()) {
                    next(options.getInterval());
                    return;
                }
                
                player.removeListener(this);
                oc.fire(this, new Text("rc=100"));
                this.complete();
                
                break;
            case PlayerEvent.FAILED :
                player.removeListener(this);
                oc.fire(this, null);
                this.complete();
        }
    }
}
