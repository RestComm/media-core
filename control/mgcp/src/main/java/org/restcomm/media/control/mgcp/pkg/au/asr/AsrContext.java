/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.pkg.au.asr;

import java.util.Arrays;
import java.util.List;

import org.restcomm.media.control.mgcp.pkg.au.Playlist;

import com.google.common.base.Optional;

/**
 * @author anikiforov
 */
public class AsrContext {

    public static final char NO_DTMF_TONE = ' ';
    private static final String MIXED_INPUT_TYPE = "DTMF_SPEECH";

    // Playlists
    private final Playlist initialPrompt;
    private final Playlist failureAnnouncement;
    private final Playlist successAnnouncement;

    private final Parameters params;

    // Runtime data
    private boolean digitsOnlyMode = false;
    private final StringBuilder collectedDigits;
    private long lastCollectedDigitOn;
    private char lastTone;
    private int returnCode;
    private String lastRecognizedText;
    private String interimRecognizedText;
    private StringBuilder finalRecognizedText;

    public AsrContext(Parameters params) {
        // Signal Options
        this.params = params;

        // Playlists
        this.initialPrompt = new Playlist(params.getInitialPromptSegments(), 1);
        this.failureAnnouncement = new Playlist(params.getFailureAnnouncementSegments(), 1);
        this.successAnnouncement = new Playlist(params.getSuccessAnnouncementSegments(), 1);

        // Runtime Data
        this.collectedDigits = new StringBuilder("");
        this.lastCollectedDigitOn = 0L;
        this.lastTone = NO_DTMF_TONE;
        this.returnCode = 0;
    }

    public Parameters getParams() {
        return params;
    }

    public boolean isMixedInputSupported() {
        return MIXED_INPUT_TYPE.equalsIgnoreCase(params.getInputType());
    }

    public boolean isDigitsOnlyMode() {
        return digitsOnlyMode;
    }

    public Playlist getInitialPrompt() {
        return initialPrompt;
    }

    public Playlist getFailureAnnouncement() {
        return failureAnnouncement;
    }

    public Playlist getSuccessAnnouncement() {
        return successAnnouncement;
    }

    /**
     * The minimum number of digits to collect.
     * <p>
     * <b>Defaults to one.</b> This parameter should not be specified if the Digit Pattern parameter is present.
     * </p>
     *
     * @return
     */
    public int getMinimumNumDigits() {
        return params.getMinimumNumDigits();
    }

    /**
     * The maximum number of digits to collect.
     * <p>
     * <b>Defaults to one.</b> This parameter should not be specified if the Digit Pattern parameter is present.
     * </p>
     *
     * @return
     */
    public int getMaximumNumDigits() {
        return params.getMaximumNumDigits();
    }

    /**
     * A legal digit map as described in <a href="https://tools.ietf.org/html/rfc2885#section-7.1.14">section 7.1.14</a> of the
     * MEGACO protocol using the DTMF mappings associated with the Megaco DTMF Detection Package described in the Megaco
     * protocol document.
     * <p>
     * <b>This parameter should not be specified if one or both of the Minimum # Of Digits parameter and the Maximum Number Of
     * Digits parameter is present.</b>
     * </p>
     *
     * @return The digit pattern or an empty String if not specified.
     */
    public String getDigitPattern() {
        return params.getDigitPattern();
    }

    public boolean hasDigitPattern() {
        return !Optional.fromNullable(params.getDigitPattern()).or("").isEmpty();
    }

    /*
     * Runtime Data
     */
    public void collectDigit(char digit) {
        if (isMixedInputSupported()) {
            this.digitsOnlyMode = true;
            this.collectedDigits.append(digit);
            this.lastCollectedDigitOn = System.currentTimeMillis();
        }
    }

    public String getCollectedDigits() {
        return collectedDigits.toString();
    }

    public int countCollectedDigits() {
        return collectedDigits.length();
    }

    public long getLastCollectedDigitOn() {
        return lastCollectedDigitOn;
    }

    public char getLastTone() {
        return lastTone;
    }

    public boolean isEndInputKeySpecified() {
        return getParams().getEndInputKey() != NO_DTMF_TONE;
    }

    public void setLastTone(char lastTone) {
        this.lastTone = lastTone;
    }

    public int getReturnCode() {
        return returnCode;
    }

    protected void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public void appendRecognizedText(final String recognizedText, final boolean isFinal) {
        lastRecognizedText = recognizedText;
        if (isFinal) {
            if (finalRecognizedText != null) {
                finalRecognizedText.append(System.lineSeparator());
                finalRecognizedText.append(recognizedText);
            } else {
                finalRecognizedText = new StringBuilder(recognizedText);
            }
            interimRecognizedText = null;
        } else {
            interimRecognizedText = recognizedText;
        }
    }

    public String getLastRecognizedText() {
        return lastRecognizedText;
    }

    public String getFinalRecognizedText() {
        StringBuilder result = finalRecognizedText != null ? new StringBuilder(finalRecognizedText.toString())
                : new StringBuilder();
        if (interimRecognizedText != null) {
            if (result.length() > 0) {
                result.append(System.lineSeparator());
            }
            result.append(interimRecognizedText);
        }
        return result.toString();
    }

    public boolean needPartialResult() {
        return params.partialResult;
    }

    public static class Parameters {
        private final String[] initialPromptParam;
        private final String[] failureAnnSegments;
        private final String[] successAnnSegments;
        private char endInputKey;
        private final int maximumRecognitionTime;
        private final String driver;
        private final List<String> hotWords;
        private final int waitingTimeForInput;
        private final int postSpeechTimer;
        private final int minimumNumDigits;
        private final int maximumNumDigits;
        private final String digitPattern;
        private final String lang;
        private final String inputType;
        private final boolean partialResult;

        public Parameters(String[] initialPromptParam, String[] failureAnnSegments, String[] successAnnSegments,
                char endInputKey, final int maximumRecognitionTime, final String driver, final List<String> hotWords,
                final int waitingTimeForInput, final int postSpeechTimer, final int minimumNumDigits,
                final int maximumNumDigits, final String digitPattern, final String lang, final String inputType,
                final boolean partialResult) {
            this.initialPromptParam = initialPromptParam;
            this.failureAnnSegments = failureAnnSegments;
            this.successAnnSegments = successAnnSegments;
            this.endInputKey = endInputKey;
            this.maximumRecognitionTime = maximumRecognitionTime;
            this.driver = driver;
            this.hotWords = hotWords;
            this.waitingTimeForInput = waitingTimeForInput;
            this.postSpeechTimer = postSpeechTimer;
            this.minimumNumDigits = minimumNumDigits;
            this.maximumNumDigits = maximumNumDigits;
            this.digitPattern = digitPattern;
            this.lang = lang;
            this.inputType = inputType;
            this.partialResult = partialResult;
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
            return this.initialPromptParam;
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
            return this.successAnnSegments;
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
            return this.failureAnnSegments;
        }

        /**
         * Specifies a key that signals the end of digit collection or voice recording.
         * <p>
         * <b>The default end input key is the # key.</b> To specify that no End Input Key be used the parameter is set to the
         * string "null".
         * <p>
         * <b>The default behavior not to return the End Input Key in the digits returned to the call agent.</b> This behavior
         * can be overridden by the Include End Input Key (eik) parameter.
         * </p>
         *
         * @return
         */
        public char getEndInputKey() {
            return this.endInputKey;
        }

        public int getWaitingTimeForInput() {
            return this.waitingTimeForInput;
        }

        public int getPostSpeechTimer() {
            return this.postSpeechTimer;
        }

        public int getMaximumRecognitionTime() {
            return this.maximumRecognitionTime;
        }

        public String getDriver() {
            return driver;
        }

        /**
         * The minimum number of digits to collect.
         * <p>
         * <b>Defaults to one.</b> This parameter should not be specified if the Digit Pattern parameter is present.
         * </p>
         *
         * @return
         */
        public int getMinimumNumDigits() {
            return minimumNumDigits;
        }

        /**
         * The maximum number of digits to collect.
         * <p>
         * <b>Defaults to one.</b> This parameter should not be specified if the Digit Pattern parameter is present.
         * </p>
         *
         * @return
         */
        public int getMaximumNumDigits() {
            return maximumNumDigits;
        }

        /**
         * A legal digit map as described in <a href="https://tools.ietf.org/html/rfc2885#section-7.1.14">section 7.1.14</a> of
         * the MEGACO protocol using the DTMF mappings associated with the Megaco DTMF Detection Package described in the Megaco
         * protocol document.
         * <p>
         * <b>This parameter should not be specified if one or both of the Minimum # Of Digits parameter and the Maximum Number
         * Of Digits parameter is present.</b>
         * </p>
         *
         * @return The digit pattern or an empty String if not specified.
         */
        public String getDigitPattern() {
            return digitPattern;
        }

        public String getLang() {
            return lang;
        }

        public List<String> getHotWords() {
            return hotWords;
        }

        public String getInputType() {
            return inputType;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("ASR Parameters {");
            builder.append("initialPrompt=").append(Arrays.toString(this.initialPromptParam)).append(System.lineSeparator());
            builder.append("failurePrompt=").append(Arrays.toString(this.failureAnnSegments)).append(System.lineSeparator());
            builder.append("successPrompt=").append(Arrays.toString(this.successAnnSegments)).append(System.lineSeparator());
            builder.append("endInputKey=").append(this.endInputKey).append(System.lineSeparator());
            builder.append("maxRecognitionTime=").append(this.maximumRecognitionTime).append(System.lineSeparator());
            builder.append("driver=").append(this.driver).append(System.lineSeparator());
            builder.append("hotwords=").append(this.hotWords).append(System.lineSeparator());
            builder.append("preSpeechTimer=").append(this.waitingTimeForInput).append(System.lineSeparator());
            builder.append("postSpeechTimer=").append(this.postSpeechTimer).append(System.lineSeparator());
            builder.append("minNumDigits=").append(this.minimumNumDigits).append(System.lineSeparator());
            builder.append("maxNumDigits=").append(this.maximumNumDigits).append(System.lineSeparator());
            builder.append("digitPattern=").append(this.digitPattern).append(System.lineSeparator());
            builder.append("language=").append(this.lang).append(System.lineSeparator());
            builder.append("inputType=").append(this.inputType).append(System.lineSeparator());
            builder.append("partialResult=").append(this.partialResult).append(System.lineSeparator());
            builder.append("}");
            return builder.toString();
        }

    }

}
