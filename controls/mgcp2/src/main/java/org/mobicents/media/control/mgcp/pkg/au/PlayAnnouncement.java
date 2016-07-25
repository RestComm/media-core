/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.mobicents.media.control.mgcp.pkg.au;

import java.net.MalformedURLException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.mobicents.media.control.mgcp.pkg.SignalType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.player.Player;
import org.mobicents.media.server.spi.player.PlayerEvent;
import org.mobicents.media.server.spi.player.PlayerListener;

/**
 * Plays an announcement in situations where there is no need for interaction with the user.
 * 
 * <p>
 * Because there is no need to monitor the incoming media stream this event is an efficient mechanism for treatments,
 * informational announcements, etc.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayAnnouncement extends AbstractMgcpSignal implements PlayerListener {

    private static final Logger log = Logger.getLogger(PlayAnnouncement.class);

    // Play Parameters (default values)
    private static final String SIGNAL = "pa";

    /**
     * The maximum number of times an announcement is to be played.
     * 
     * <p>
     * A value of minus one (-1) indicates the announcement is to be repeated forever. Defaults to one (1).
     * </p>
     */
    private static final int ITERATIONS = 1;

    /**
     * The interval of silence to be inserted between iterative plays.
     * <p>
     * Specified in units of 100 milliseconds. Defaults to 10 (1 second).
     * </p>
     */
    private static final int INTERVAL = 10;

    // Media Components
    private final Player player;
    private final Playlist playlist;

    // Play operation
    private final long duration;
    private final long interval;

    public PlayAnnouncement(Player player, Map<String, String> parameters) {
        super(AudioPackage.PACKAGE_NAME, SIGNAL, SignalType.TIME_OUT, parameters);

        // Setup Play Parameters
        String[] segments = getParameter(SignalParameters.ANNOUNCEMENT.symbol()).split(",");
        String iterationsParam = getParameter(SignalParameters.ITERATIONS.symbol());
        int iterations = iterationsParam == null ? ITERATIONS : Integer.parseInt(iterationsParam);
        String durationParam = getParameter(SignalParameters.DURATION.symbol());
        this.duration = (durationParam == null) ? -1L : Long.parseLong(durationParam);
        String intervalParam = getParameter(SignalParameters.INTERVAL.symbol());
        this.interval = ((intervalParam == null) ? INTERVAL : Long.parseLong(intervalParam)) * 1000000L;

        // Setup Playlist
        this.playlist = new Playlist(segments, iterations);
        
        // Media Player
        this.player = player;
        this.player.setDuration(this.duration);
        this.player.setMediaTime(0);
        try {
            this.player.addListener(this);
        } catch (TooManyListenersException e) {
            log.error("Too many listeners for audio player", e);
        }
    }

    @Override
    protected boolean isParameterSupported(String name) {
        // Check if parameter is valid
        SignalParameters parameter = SignalParameters.fromSymbol(name);
        if (parameter == null) {
            return false;
        }

        // Check if parameter is supported
        switch (parameter) {
            case ANNOUNCEMENT:
            case ITERATIONS:
            case INTERVAL:
            case DURATION:
            case SPEED:
            case VOLUME:
                return true;

            default:
                return false;
        }
    }

    private void playAnnouncement(String segment, long delay) {
        if (log.isInfoEnabled()) {
            log.info("Playing announcement " + segment);
        }

        try {
            this.player.setInitialDelay(delay);
            this.player.setURL(segment);
            this.player.activate();
        } catch (MalformedURLException e) {
            log.error("Cannot play audio track. Malformed URL: " + segment);
            fireOF(ReturnCode.BAD_AUDIO_ID.code());
        } catch (ResourceUnavailableException e) {
            fireOF(ReturnCode.MISMATCH_BETWEEN_PLAY_SPECIFICATION_AND_PROVISIONED_DATA.code());
        }
    }

    @Override
    public void execute() {
        if (this.executing.getAndSet(true)) {
            throw new IllegalStateException("Already executing.");
        }

        // Play announcements
        String announcement = this.playlist.next();
        if (announcement == null) {
            this.executing.set(false);
            fireOF(ReturnCode.BAD_AUDIO_ID.code());
        } else {
            playAnnouncement(announcement, 0);
        }
    }

    @Override
    public void cancel() {
        if (this.executing.getAndSet(false)) {
            this.player.deactivate();
        }
    }

    @Override
    public void process(PlayerEvent event) {
        switch (event.getID()) {
            case PlayerEvent.STOP:
                if (log.isInfoEnabled()) {
                    log.info("Announcement " + this.playlist.current() + " has completed.");
                }

                String announcement = this.playlist.next();
                if (announcement == null || !isExecuting()) {
                    fireOC(ReturnCode.SUCCESS.code());
                } else {
                    playAnnouncement(announcement, this.interval);
                }
                break;

            case PlayerEvent.FAILED:
                if (this.executing.getAndSet(false)) {
                    fireOF(ReturnCode.UNSPECIFIED_FAILURE.code());
                }
                break;

            default:
                // Ignore other event types
                break;
        }
    }

    private void fireOC(int code) {
        fire(new OperationComplete(getSymbol(), code));
    }

    private void fireOF(int code) {
        fire(new OperationFailed(getSymbol(), code));
    }

}
