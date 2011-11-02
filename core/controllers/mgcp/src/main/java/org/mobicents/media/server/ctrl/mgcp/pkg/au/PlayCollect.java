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
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.ctrl.mgcp.signal.Comparator;
import org.mobicents.media.server.ctrl.mgcp.signal.Signal;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.events.NotifyEvent;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mobicents.media.server.spi.player.PlayerListener;

/**
 * Implements play announcement signal.
 * 
 * Plays a prompt and collects DTMF digits entered by a user.  If no
 * digits are entered or an invalid digit pattern is entered, the
 * user may be reprompted and given another chance to enter a correct
 * pattern of digits.  The following digits are supported:  0-9, *,
 * #, A, B, C, D.  By default PlayCollect does not play an initial
 * prompt, makes only one attempt to collect digits, and therefore
 * functions as a simple Collect operation.  Various special purpose
 * keys, key sequences, and key sets can be defined for use during
 * the PlayCollect operation.
 * 
 * 
 * @author kulikov
 */
public class PlayCollect extends Signal implements PlayerListener, BufferListener {
    
    private EventName oc = new EventName(PackageName.factory("AU"), MgcpEvent.factory("oc"));
    private EventName of = new EventName(PackageName.factory("AU"), MgcpEvent.factory("of"));
    
    private Player player;
    private DtmfDetector dtmfDetector;
    
    private Options options;
    private EventBuffer buffer = new EventBuffer();

    private boolean isNonInterruptablePlay = false;

    private final static Logger logger = Logger.getLogger(PlayCollect.class);
    
    public PlayCollect(String name) {
        super(name);
    }
    
    @Override
    public void execute() {
        buffer.setListener(this);
        logger.info("Executing...");
        
        player = this.getPlayer();
        dtmfDetector = this.getDetector();
        dtmfDetector.flushBuffer();
        
        //start dtmf detector
        try {
            dtmfDetector.addListener(buffer);
        } catch (TooManyListenersException e) {
            this.sendEvent(of);
            return;
        }
        
        dtmfDetector.start();
        
        //play prompt if present
        try {        
            player.addListener(this);
        } catch (TooManyListenersException e) {
            this.sendEvent(of);
            return;
        }
        
        //get options of the request
        options = new Options(getTrigger().getEventIdentifier().getParms());

        this.isNonInterruptablePlay = options.isNonInterruptablePlay();
        
        if (options.getDigitPattern() != null) {
            buffer.setPatterns(options.getDigitPattern());
        }
                
        buffer.setCount(options.getDigitsNumber());
        
        if (options.getPrompt() == null) {
            return;
        }
        logger.info("Starting prompt");
        try {
            player.setURL(options.getPrompt());
        } catch (Exception e) {
            this.sendEvent(of);
            return;
        }
        
        player.start();
    }

    @Override
    public boolean doAccept(RequestedEvent event) {
        if (Comparator.matches(event, oc)) {
            return true;
        }
        if (Comparator.matches(event, of)) {
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
        
        if (dtmfDetector != null) {
            dtmfDetector.removeListener(buffer);
        }        
    }

    public void update(NotifyEvent event) {
        switch (event.getEventID()) {
            case NotifyEvent.COMPLETED :
                break;
            case NotifyEvent.RX_FAILED :
            case NotifyEvent.TX_FAILED :
                this.sendEvent(of);
                this.complete();
                break;
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

    private DtmfDetector getDetector() {
        if (getTrigger().getConnectionIdentifier() == null) {
            MediaSink sink = getEndpoint().getSink(MediaType.AUDIO);
            return sink.getInterface(DtmfDetector.class);
        }
        
        String connectionID = getTrigger().getConnectionIdentifier().toString();
        Connection connection = getConnection(connectionID);
        
        if (connection == null) {
            return null;
        }
        
        MediaSink sink = (MediaSink) connection.getComponent(MediaType.AUDIO, DtmfDetector.class);
        if (sink == null) {
            return null;
        }
                
        return sink.getInterface(DtmfDetector.class);
    }
    
    @Override
    public void reset() {
        super.reset();
        if (player != null) {
            player.removeListener(this);
        }
        
        if (dtmfDetector != null) {
            dtmfDetector.removeListener(buffer);
        }
    }

    public void patternMatches(int index, String s) {
        sendEvent(new EventName(PackageName.factory("AU"), MgcpEvent.factory("oc").withParm("rc=100 dc=" + s + " pi=" + index)));
        dtmfDetector.removeListener(buffer);
        complete();
    }

    public void countMatches(String s) {
        sendEvent(new EventName(PackageName.factory("AU"), MgcpEvent.factory("oc").withParm("rc=100 dc=" + s)));
        dtmfDetector.removeListener(buffer);
        complete();
    }

    public void process(PlayerEvent event) {
        switch (event.getID()) {
            case PlayerEvent.STOP :
                break;
            case PlayerEvent.FAILED :
                this.sendEvent(of);
                this.complete();
                break;
        }
    }

    public void singleTone(String s) {
        logger.info("Tone detected: non interrupt" + this.isNonInterruptablePlay);
        if (!this.isNonInterruptablePlay) {
            player.stop();
        }
    }
}
