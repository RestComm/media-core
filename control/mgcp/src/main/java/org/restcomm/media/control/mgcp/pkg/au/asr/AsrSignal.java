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
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.asr.AsrEngine;
import org.restcomm.media.asr.InputTimeoutListener;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.restcomm.media.control.mgcp.pkg.SignalType;
import org.restcomm.media.control.mgcp.pkg.au.AudioPackage;
import org.restcomm.media.control.mgcp.pkg.au.AudioSignalType;
import org.restcomm.media.control.mgcp.pkg.au.SignalParameters;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.player.Player;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author anikiforov
 */
public class AsrSignal extends AbstractMgcpSignal {

    private static final Logger log = LogManager.getLogger(AsrSignal.class);

    static final String SYMBOL = AudioSignalType.ASR_COLLECT.symbol();

    protected final AsrContext context;

    // Finite State Machine
    private final AsrFsm fsm;

    public AsrSignal(Player player, DtmfDetector detector, AsrEngine asrEngine, int requestId, NotifiedEntity notifiedEntity,
            Map<String, String> parameters, ListeningScheduledExecutorService executor) {
        super(AudioPackage.PACKAGE_NAME, SYMBOL, SignalType.TIME_OUT, requestId, notifiedEntity, parameters);
        // Execution Context
        this.context = new AsrContext(new ParameterParser().parse());

        // Build FSM
        this.fsm = AsrFsmBuilder.INSTANCE.build(detector, player, asrEngine, this, executor, context);
    }

    @Override
    public void execute() {
        if (!this.fsm.isStarted()) {
            this.fsm.start(this.context);
        }
    }

    @Override
    public void cancel() {
        if (this.fsm.isStarted()) {
            fsm.fire(AsrEvent.CANCEL, this.context);
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
            case INITIAL_PROMPT:
            case FAILURE_ANNOUNCEMENT:
            case SUCCESS_ANNOUNCEMENT:
            case MAXIMUM_NUM_DIGITS:
            case MINIMUM_NUM_DIGITS:
            case DIGIT_PATTERN:
            case END_INPUT_KEY:
            case WAITING_TIME_FOR_INPUT:
            case MAXIMUM_RECOGNITION_TIME:
            case POST_SPEECH_TIMER:
            case DRIVER:
            case HOT_WORDS:
            case LANG:
            case INPUT:
            case PARTIAL_RESULT:
                return true;
            default:
                return false;
        }
    }

    public InputTimeoutListener getInputTimeoutDetectorListener() { // Method to use only in unit tests
        return ((AsrFsmImpl) fsm).getInputTimeoutDetectorListener();
    }

    private class ParameterParser {
        AsrContext.Parameters parse() {
            return new AsrContext.Parameters(getInitialPromptSegments(), getFailureAnnouncementSegments(),
                    getSuccessAnnouncementSegments(), getEndInputKey(), getMaximumRecognitionTime(), getDriver(), getHotWords(),
                    getWaitingTimeForInput(), getPostSpeechTimer(), getMinimumNumDigits(), getMaximumNumDigits(),
                    getDigitPattern(), getLang(), getInputType(), needPartialResult());
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
            final String NO_VALUE = "null";
            final String value = Optional.fromNullable(getParameter(SignalParameters.END_INPUT_KEY.symbol())).or("");
            char result = '#'; // default value
            if (NO_VALUE.equals(value.trim())) {
                result = AsrContext.NO_DTMF_TONE;
            } else if (!value.isEmpty()) {
                result = value.charAt(0);
            }
            return result;
        }

        public int getMaximumRecognitionTime() {
            final String value = Optional.fromNullable(getParameter(SignalParameters.MAXIMUM_RECOGNITION_TIME.symbol()))
                    .or("0");
            return Integer.parseInt(value) * 100;
        }

        public String getDriver() {
            return getParameter(SignalParameters.DRIVER.symbol());
        }

        public List<String> getHotWords() {
            String hw = getParameter(SignalParameters.HOT_WORDS.symbol());
            List<String> hotWords = null;
            if (!StringUtils.isEmpty(hw)) {
                try {
                    hw = new String(Hex.decodeHex(hw.toCharArray()));
                    hotWords = Arrays.asList(hw.split(","));
                } catch (DecoderException e) {
                    log.error("Hot words can not be decoded");
                }
            }
            return hotWords;
        }

        /**
         * The waiting time to detect user input.
         * <p>
         * Specified in units of 100 milliseconds. <b>Defaults to 30 (3 seconds).</b>
         * </p>
         *
         * @return
         */
        public int getWaitingTimeForInput() {
            String value = Optional.fromNullable(getParameter(SignalParameters.WAITING_TIME_FOR_INPUT.symbol())).or("30");
            return Integer.parseInt(value) * 100;
        }

        /**
         * The amount of silence necessary after the end of speech.
         * <p>
         * Specified units of 100 milliseconds seconds. <b>Defaults to 20 (2 seconds).</b>
         * </p>
         *
         * @return
         */
        public int getPostSpeechTimer() {
            String value = Optional.fromNullable(getParameter(SignalParameters.POST_SPEECH_TIMER.symbol())).or("20");
            return Integer.parseInt(value) * 100;
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
            String value = Optional.fromNullable(getParameter(SignalParameters.MINIMUM_NUM_DIGITS.symbol())).or("1");
            return Integer.parseInt(value);
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
            String value = Optional.fromNullable(getParameter(SignalParameters.MAXIMUM_NUM_DIGITS.symbol())).or("1");
            return Integer.parseInt(value);
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
            String pattern = Optional.fromNullable(getParameter(SignalParameters.DIGIT_PATTERN.symbol())).or("");
            if (!pattern.isEmpty()) {
                // Replace pattern to comply with MEGACO digitMap
                pattern = pattern.replace(".", "*").replace("x", "\\d");
            }
            return pattern;
        }

        public String getLang() {
            return Optional.fromNullable(getParameter(SignalParameters.LANG.symbol())).or("en-US");
        }

        public String getInputType() {
            return getParameter(SignalParameters.INPUT.symbol());
        }

        public boolean needPartialResult() {
            return StringUtils.isEmpty(getParameter(SignalParameters.PARTIAL_RESULT.symbol())) ? false
                    : Boolean.parseBoolean(getParameter(SignalParameters.PARTIAL_RESULT.symbol()));
        }
    }

}
