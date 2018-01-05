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

package org.restcomm.media.asr;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.ComponentType;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.drivers.asr.AsrDriver;
import org.restcomm.media.drivers.asr.AsrDriverConfigurationException;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;
import org.restcomm.media.drivers.asr.AsrDriverException;
import org.restcomm.media.drivers.asr.AsrDriverManager;
import org.restcomm.media.drivers.asr.UnknownAsrDriverException;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Task;
import org.restcomm.media.spi.memory.Frame;

/**
 * @author gdubina
 *
 */
public class AsrEngineImpl extends SpeechDetectorImpl implements AsrEngine {

    private static final long serialVersionUID = -4340167932532917193L;

    private static final Logger logger = LogManager.getLogger(AsrEngineImpl.class);

    private final AsrDriverManager driverManager;
    private final PriorityQueueScheduler scheduler;

    private final AudioOutput output;

    private AsrDriver driver;
    private String lang;
    private List<String> hints;
    private AsrEngineListener listener;

    private boolean isDriverStarted = false;

    public AsrEngineImpl(final String name, final PriorityQueueScheduler scheduler, final AsrDriverManager driverManager,
            final int silenceLevel) {
        super(name, silenceLevel);
        this.driverManager = driverManager;
        this.scheduler = scheduler;

        output = new AudioOutput(scheduler, ComponentType.ASR_ENGINE.getType());
        output.join(this);
    }

    /*
     * Overridden SpeechDetectorImpl methods
     */

    @Override
    public void stopSpeechDetection() {
        super.stopSpeechDetection();
        try {
            output.stop();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /*
     * AsrEngine interface implementation
     */

    @Override
    public void configure(String driverName, String language, List<String> hints)
            throws UnknownAsrDriverException, AsrDriverConfigurationException {
        this.driver = driverManager.getDriver(driverName);
        this.lang = language;
        this.hints = hints;
    }

    @Override
    public void setListener(AsrEngineListener listener) {
        this.listener = listener;
    }

    @Override
    public int getResponseTimeoutInMilliseconds() {
        final int result = driver.getResponseTimeoutInMilliseconds();
        if (logger.isTraceEnabled()) {
            logger.trace("We have called AsrDriver.getResponseTimeoutInMilliseconds(): " + result);
        }
        return result;
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        if (isSpeechDetectionOn()) {
            driver.write(frame.getData(), frame.getOffset(), frame.getLength());
            if (logger.isTraceEnabled()) {
                logger.trace("We have called AsrDriver.write(<...>, " + frame.getOffset() + ", " + frame.getLength() + ")");
            }
        }
        super.onMediaTransfer(frame);
    }

    @Override
    public void activate() {
        this.driver.setListener(driverEventListener);
        if (logger.isTraceEnabled()) {
            logger.trace("We have called AsrDriver.setListener(" + driverEventListener + ")");
        }
        this.driver.startRecognizing(lang, hints);
        isDriverStarted = true;
        output.start();
        if (logger.isTraceEnabled()) {
            final StringBuilder hintsString = new StringBuilder();
            hintsString.append("[");
            boolean isFirstEntry = true;
            if (hints != null) {
                for (final String hint : hints) {
                    if (!isFirstEntry) {
                        hintsString.append(", ");
                    }
                    hintsString.append("'");
                    hintsString.append(hint);
                    hintsString.append("'");
                    isFirstEntry = false;
                }
            }
            hintsString.append("]");
            if (logger.isTraceEnabled()) {
                logger.trace("We have called AsrDriver.startRecognizing('" + lang + "', " + hintsString.toString() + ")");
            }
        }
        super.activate();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (!isDriverStarted) {
            return;
        }
        driver.finishRecognizing();
        if (logger.isTraceEnabled()) {
            logger.trace("We have called AsrDriver.finishRecognizing()");
        }
        driver.setListener(null);
        if (logger.isTraceEnabled()) {
            logger.trace("We have called AsrDriver.setListener(null)");
        }
    }

    public AudioOutput getAudioOutput() {
        return output;
    }

    private void fireSpeechRecognizedEvent(final String text, final boolean isFinal) {
        FireSpeechRecognizedEventTask task = new FireSpeechRecognizedEventTask(text, isFinal);
        scheduler.submit(task, task.getQueueNumber());
    }

    private void fireDriverErrorEvent(AsrDriverException error) {
        FireDriverErrorTask task = new FireDriverErrorTask(error);
        scheduler.submit(task, task.getQueueNumber());
    }

    private AsrDriverEventListener driverEventListener = new AsrDriverEventListener() {
        @Override
        public void onSpeechRecognized(final String text, final boolean isFinal) {
            if (logger.isTraceEnabled()) {
                logger.trace("ASR driver recognized text: \'" + text + "\', isFinal=" + isFinal);
            }
            fireSpeechRecognizedEvent(text, isFinal);
        }

        @Override
        public void onError(final AsrDriverException error) {
            logger.warn("ASR driver error: " + error.getMessage());
            fireDriverErrorEvent(error);
        }
    };

    private class FireSpeechRecognizedEventTask extends Task {

        private final String text;
        private final boolean isFinal;

        public FireSpeechRecognizedEventTask(String text, boolean isFinal) {
            super();
            this.text = text;
            this.isFinal = isFinal;
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.INPUT_QUEUE;
        }

        @Override
        public long perform() {
            if (AsrEngineImpl.this.listener != null) {
                AsrEngineImpl.this.listener.onSpeechRecognized(this.text, this.isFinal);
            }
            return 0;
        }
    }

    private class FireDriverErrorTask extends Task {

        private final AsrDriverException error;

        public FireDriverErrorTask(AsrDriverException error) {
            super();
            this.error = error;
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.INPUT_QUEUE;
        }

        @Override
        public long perform() {
            if (AsrEngineImpl.this.listener != null) {
                AsrEngineImpl.this.listener.onDriverError(this.error);
            }
            return 0;
        }
    }

}
