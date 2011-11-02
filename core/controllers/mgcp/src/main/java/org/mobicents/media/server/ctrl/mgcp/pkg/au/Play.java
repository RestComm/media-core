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
package org.mobicents.media.server.ctrl.mgcp.pkg.au;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;
import org.apache.log4j.Logger;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.ctrl.mgcp.signal.Comparator;
import org.mobicents.media.server.ctrl.mgcp.signal.Signal;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mobicents.media.server.spi.player.PlayerListener;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 * Implements play announcement signal.
 * 
 * @author kulikov
 */
public class Play extends Signal implements PlayerListener {
    
    private EventName oc = new EventName(PackageName.factory("AU"), MgcpEvent.factory("oc"));
    private EventName of = new EventName(PackageName.factory("AU"), MgcpEvent.factory("of"));
    
    private Player player;
    private volatile Options options;
    
    private int repeatCount;
    private String uri;
    
    private final static Logger logger = Logger.getLogger(Play.class);
    
    public Play(String name) {
        super(name);
    }
    
    @Override
    public void execute() {
        logger.info("Executing...");
        
        player = this.getPlayer();
        
        try {
            player.addListener(this);
        } catch (TooManyListenersException e) {
            this.sendEvent(of);
            return;
        }
        
        //get options of the request
        options = new Options(getTrigger().getEventIdentifier().getParms());

        this.repeatCount = options.getRepeatCount();
        this.uri = options.next();
        
        try {
            player.setURL(uri);
        } catch (Exception e) {
            this.sendEvent(of);
            return;
        }
        
        //set max duration if present
        if (options.getDuration() != -1) {
            player.setMaxDuration(options.getDuration());
        }

        //set initial offset
        player.setMediaTime(options.getOffset());
        player.setInitialDelay(0);
        
        player.start();
    }

    @Override
    public boolean doAccept(RequestedEvent event) {
        if (!Comparator.matches(event, oc)) {
            return false;
        }
        if (!Comparator.matches(event, of)) {
            return false;
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
        if (getTrigger().getConnectionIdentifier() == null) {
            MediaSource source = getEndpoint().getSource(MediaType.AUDIO);
            return source.getInterface(Player.class);
        }
        
        String connectionID = getTrigger().getConnectionIdentifier().toString();
        Connection connection = getConnection(connectionID);
        
        if (connection == null) {
            return null;
        }
        
        MediaSource source = (MediaSource) connection.getComponent(MediaType.AUDIO, Player.class);
        if (source == null) {
            return null;
        }
                
        return source.getInterface(Player.class);
    }
    
    @Override
    public void reset() {
        super.reset();
        if (player != null) {
            player.removeListener(this);
            player.stop();
        }
    }
    
    private void repeat(long delay) {
        logger.info("Repeat PLAY... " + repeatCount);
        try {
            player.setURL(uri);
            player.setInitialDelay(delay);
            player.start();
        } catch (Exception e) {
            this.sendEvent(of);
            return;
        }
    }
    
    private void next(long delay) {
        uri = options.next();
        logger.info("Next segment... " + uri);
        try {
            player.setURL(uri);
            player.setInitialDelay(delay);
            player.start();
        } catch (Exception e) {
            this.sendEvent(of);
            return;
        }
    }

    public void process(PlayerEvent event) {
        switch (event.getID()) {
            case PlayerEvent.STOP :
                repeatCount--;
                
                if (repeatCount > 0) {
                    repeat(options.getInterval());
                    return;
                }
                
                if (options.hasMoreSegments()) {
                    next(options.getInterval());
                    return;
                }
                
                player.removeListener(this);
                this.sendEvent(oc);
                this.complete();
                break;
            case PlayerEvent.FAILED :
                player.removeListener(this);
                this.sendEvent(of);
                this.complete();
        }
    }
}
