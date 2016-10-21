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

package org.mobicents.media.control.mgcp.pkg.au.pr;

import java.util.Map;

import org.mobicents.media.control.mgcp.pkg.au.Playlist;
import org.mobicents.media.control.mgcp.pkg.au.SignalParameters;

import com.google.common.base.Optional;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayRecordContext {

    // Signal options
    private final Map<String, String> parameters;

    // Playlists
    private final Playlist initialPrompt;
    private final Playlist reprompt;
    private final Playlist noSpeechReprompt;
    private final Playlist failureAnnouncement;
    private final Playlist successAnnouncement;

    // Runtime data
    private char tone;
    private int attempt;
    private boolean canceled;
    private int returnCode;

    public PlayRecordContext(Map<String, String> parameters) {
        // Signal Parameters
        this.parameters = parameters;
        
        // Playlists
        this.initialPrompt = new Playlist(getInitialPromptSegments(), 1);
        this.reprompt = new Playlist(getRepromptSegments(), 1);
        this.noSpeechReprompt = new Playlist(getNoSpeechRepromptSegments(), 1);
        this.failureAnnouncement = new Playlist(getFailureAnnouncementSegments(), 1);
        this.successAnnouncement = new Playlist(getSuccessAnnouncementSegments(), 1);

        // Runtime Data
        this.returnCode = 0;
        this.attempt = 1;
        this.canceled = false;
        this.tone = ' ';
    }

    /*
     * Signal Options
     */
    private String getParameter(String name) {
        return this.parameters.get(name);
    }

    /**
     * The initial announcement prompting the user to either enter DTMF digits or to speak.
     * <p>
     * Consists of one or more audio segments.<br>
     * If not specified (the default), the event immediately begins digit collection or recording.
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getInitialPromptSegments() {
        String value = Optional.fromNullable(getParameter(SignalParameters.INITIAL_PROMPT.symbol())).or("");
        return value.isEmpty() ? new String[0] : value.split(",");
    }

    public Playlist getInitialPrompt() {
        return initialPrompt;
    }

    /**
     * Played after the user has made an error such as entering an invalid digit pattern or not speaking.
     * <p>
     * Consists of one or more audio segments. <b>Defaults to the Initial Prompt.</b>
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getRepromptSegments() {
        String segments = Optional.fromNullable(getParameter(SignalParameters.REPROMPT.symbol())).or("");
        return segments.isEmpty() ? getInitialPromptSegments() : segments.split(",");
    }

    public Playlist getReprompt() {
        return reprompt;
    }

    /**
     * Played after the user has failed to enter a valid digit pattern during a PlayCollect event.
     * <p>
     * Consists of one or more audio segments. <b>Defaults to the Reprompt.</b>
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getNoSpeechRepromptSegments() {
        String segments = Optional.fromNullable(getParameter(SignalParameters.NO_SPEECH_REPROMTP.symbol())).or("");
        return segments.isEmpty() ? getRepromptSegments() : segments.split(",");
    }

    public Playlist getNoSpeechReprompt() {
        return noSpeechReprompt;
    }

    /**
     * Played when all data entry attempts have failed.
     * <p>
     * Consists of one or more audio segments. No default.
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getFailureAnnouncementSegments() {
        String value = Optional.fromNullable(getParameter(SignalParameters.FAILURE_ANNOUNCEMENT.symbol())).or("");
        return value.isEmpty() ? new String[0] : value.split(",");
    }

    public Playlist getFailureAnnouncement() {
        return failureAnnouncement;
    }

    public boolean hasFailureAnnouncement() {
        return !this.failureAnnouncement.isEmpty();
    }

    /**
     * Played when all data entry attempts have succeeded.
     * <p>
     * Consists of one or more audio segments. No default.
     * </p>
     * 
     * @return The array of audio prompts. Array will be empty if none is specified.
     */
    private String[] getSuccessAnnouncementSegments() {
        String value = Optional.fromNullable(getParameter(SignalParameters.SUCCESS_ANNOUNCEMENT.symbol())).or("");
        return value.isEmpty() ? new String[0] : value.split(",");
    }

    public Playlist getSuccessAnnouncement() {
        return successAnnouncement;
    }

    public boolean hasSuccessAnnouncement() {
        return !this.successAnnouncement.isEmpty();
    }

    /**
     * If set to true, initial prompt is not interruptible by either voice or digits.
     * <p>
     * <b>Defaults to false.</b> Valid values are the text strings "true" and "false".
     * </p>
     * 
     * @return
     */
    public boolean getNonInterruptibleAudio() {
        String value = Optional.fromNullable(getParameter(SignalParameters.NON_INTERRUPTIBLE_PLAY.symbol())).or("false");
        return Boolean.parseBoolean(value);
    }

    /**
     * If set to true, clears the digit buffer before playing the initial prompt.
     * <p>
     * <b>Defaults to false.</b> Valid values are the text strings "true" and "false".
     * </p>
     * 
     * @return
     */
    public boolean getClearDigitBuffer() {
        String value = Optional.fromNullable(getParameter(SignalParameters.CLEAR_DIGIT_BUFFER.symbol())).or("false");
        return Boolean.parseBoolean(value);
    }

    /**
     * Defines a key sequence consisting of a command key optionally followed by zero or more keys. This key sequence has the
     * following action: discard any digits collected or recording in progress, replay the prompt, and resume digit collection
     * or recording.
     * <p>
     * <b>No default.</b> An application that defines more than one command key sequence, will typically use the same command
     * key for all command key sequences.
     * <p>
     * If more than one command key sequence is defined, then all key sequences must consist of a command key plus at least one
     * other key.
     * </p>
     * 
     * @return
     */
    public char getRestartKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.RESTART_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Defines a key sequence consisting of a command key optionally followed by zero or more keys. This key sequence has the
     * following action: discard any digits collected or recordings in progress and resume digit collection or recording.
     * <p>
     * <b>No default.</b>
     * </p>
     * An application that defines more than one command key sequence, will typically use the same command key for all command
     * key sequences.
     * </p>
     * <p>
     * If more than one command key sequence is defined, then all key sequences must consist of a command key plus at least one
     * other key.
     * </p>
     * 
     * @return
     */
    public char getReinputKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.REINPUT_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Defines a key sequence consisting of a command key optionally followed by zero or more keys. This key sequence has the
     * following action: terminate the current event and any queued event and return the terminating key sequence to the call
     * processing agent.
     * <p>
     * <b> No default.</b> An application that defines more than one command key sequence, will typically use the same command
     * key for all command key sequences.
     * <p>
     * If more than one command key sequence is defined, then all key sequences must consist of a command key plus at least one
     * other key.
     * </p>
     * 
     * @return
     */
    public char getReturnKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.RETURN_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Defines a key with the following action. Stop playing the current announcement and resume playing at the beginning of the
     * first, last, previous, next, or the current segment of the announcement.
     * <p>
     * <b>No default. The actions for the position key are fst, lst, prv, nxt, and cur.</b>
     * </p>
     * 
     * @return
     */
    public char getPositionKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.POSITION_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Defines a key with the following action. Terminate playback of the announcement.
     * <p>
     * <b>No default.</b>
     * </p>
     * 
     * @return
     */
    public char getStopKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.STOP_KEY.symbol())).or("");
        return value.isEmpty() ? ' ' : value.charAt(0);
    }

    /**
     * Specifies a key that signals the end of digit collection or voice recording.
     * <p>
     * <b>The default end input key is the # key.</b> To specify that no End Input Key be used the parameter is set to the
     * string "null".
     * <p>
     * <b>The default behavior not to return the End Input Key in the digits returned to the call agent.</b> This behavior can
     * be overridden by the Include End Input Key (eik) parameter.
     * </p>
     * 
     * @return
     */
    public char getEndInputKey() {
        String value = Optional.fromNullable(getParameter(SignalParameters.END_INPUT_KEY.symbol())).or("");
        return value.isEmpty() ? '#' : value.charAt(0);
    }

    /**
     * The number of attempts the user needed to enter a valid digit pattern or to make a recording.
     * <p>
     * <b>Defaults to 1.</b> Also used as a return parameter to indicate the number of attempts the user made.
     * </p>
     * 
     * @return
     */
    public int getNumberOfAttempts() {
        String value = Optional.fromNullable(getParameter(SignalParameters.NUMBER_OF_ATTEMPTS.symbol())).or("1");
        return Integer.parseInt(value);
    }

    /*
     * Runtime Data
     */
    public char getTone() {
        return tone;
    }

    public void setTone(char tone) {
        this.tone = tone;
    }

    public int getAttempt() {
        return attempt;
    }

    public boolean hasMoreAttempts() {
        return this.attempt < getNumberOfAttempts();
    }

    public int getReturnCode() {
        return returnCode;
    }

    protected void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public boolean isCanceled() {
        return canceled;
    }

    protected void cancel() {
        this.canceled = true;
    }

    /**
     * Resets the collected digits and increments the attempts counter.
     */
    protected void newAttempt() {
        this.attempt++;
        this.initialPrompt.rewind();
        this.reprompt.rewind();
        this.noSpeechReprompt.rewind();
        this.successAnnouncement.rewind();
        this.failureAnnouncement.rewind();
        this.tone = ' ';
    }

}
