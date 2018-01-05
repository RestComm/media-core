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

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * Component that triggers input timeout events during Speech Detection process.
 * 
 * @author anikiforov
 *
 */
public class InputTimeoutDetector {

    private final static Logger logger = LogManager.getLogger(SpeechDetectorImpl.class);

    private final static long INVALID_TIME = -1L;

    private final ListeningScheduledExecutorService executor;
    private InputTimeoutListener inputTimeoutListener;
    private long nextInputTimeout = -1L; // in nanoseconds
    private long firstInputTimeout = -1L; // in nanoseconds
    // maximum recrding time. -1 means until stopped.
    private long maxInputTimeout = -1; // in nanoseconds
    private long startWaitingForFirstInputTimestamp = INVALID_TIME; // in nanoseconds
    private long firstInputTimestamp = INVALID_TIME; // in nanoseconds
    private long lastInputTimestamp = INVALID_TIME; // in nanoseconds
    private long startSessionTimestamp = INVALID_TIME;

    public InputTimeoutDetector(final ListeningScheduledExecutorService executor) {
        this.executor = executor;
    }

    /* InputTimeoutDetector interface implementation: */

    public void startSession(final InputTimeoutListener listener, final long firstInputTimeout, final long maxInputTimeout, final long nextInputTimeout) {
        this.inputTimeoutListener = listener;
        startWaitingForFirstInputTimestamp = INVALID_TIME;
        firstInputTimestamp = INVALID_TIME;
        lastInputTimestamp = INVALID_TIME;
        startSessionTimestamp = getCurrentTimestamp();
        configureTimersInMilliseconds(firstInputTimeout, maxInputTimeout, nextInputTimeout);
    }

    public void startWaitingForFirstInput() {
        if (isSessionStarted()) {
            if (!isSpeechDetected()) {
                startWaitingForFirstInputTimestamp = getCurrentTimestamp();
                if (logger.isTraceEnabled()) {
                    logger.trace("startWaitingForFirstInput [startWaitingForFirstInputTimestamp="
                            + startWaitingForFirstInputTimestamp + "]");
                }
                executor.schedule(new FirstInputTimeoutDetector(), firstInputTimeout, TimeUnit.NANOSECONDS);
            } else {
                if(logger.isTraceEnabled()) {
                    logger.trace("startWaitingForFirstInput: We already have first input");
                }
            }
        } else {
            logger.warn("startWaitingForFirstInput: Session is not started");
        }
    }

    public void processInput() {
        if (isSessionStarted()) {
            final long currentTimestamp = getCurrentTimestamp();
            if (!isSpeechDetected()) {
                firstInputTimestamp = currentTimestamp;
                if (logger.isTraceEnabled()) {
                    logger.trace("processInput [firstInputTimestamp=" + firstInputTimestamp + "]");
                }
                executor.schedule(new MaxInputTimeoutDetector(), maxInputTimeout, TimeUnit.NANOSECONDS);
            }
            lastInputTimestamp = currentTimestamp;
            if (logger.isTraceEnabled()) {
                logger.trace("processInput [lastInputTimestamp=" + lastInputTimestamp + "]");
            }
            executor.schedule(new NextInputTimeoutDetector(), nextInputTimeout, TimeUnit.NANOSECONDS);
        } else {
            logger.warn("processInput: Session is not started");
        }
    }

    public long getIdleTimeInNanoseconds() {
        long result = INVALID_TIME;
        if (isTimestampInitialized(lastInputTimestamp)) {
            result = getCurrentTimestamp() - lastInputTimestamp;
        }
        return result;
    }

    public void stopSession() {
        startSessionTimestamp = INVALID_TIME;
        lastInputTimestamp = INVALID_TIME;
        firstInputTimestamp = INVALID_TIME;
        startWaitingForFirstInputTimestamp = INVALID_TIME;
        inputTimeoutListener = null;
    }

    public boolean isSpeechDetected() {
        return isTimestampInitialized(firstInputTimestamp);
    }

    private boolean isTimestampInitialized(final long timestamp) {
        return (timestamp != INVALID_TIME);
    }

    private boolean isSessionStarted() {
        return isTimestampInitialized(startSessionTimestamp);
    }

    private long getCurrentTimestamp() {
        return System.nanoTime();
    }

    private void configureTimersInMilliseconds(final long firstInputTimeout, final long maxInputTimeout,
            final long nextInputTimeout) {
        if (logger.isTraceEnabled()) {
            logger.trace(
                    "configureTimersInMilliseconds: firstInputTimeout=" + this.firstInputTimeout + " msec, maxInputTimeout="
                            + this.maxInputTimeout + " msec, nextInputTimeout= " + this.nextInputTimeout + " msec");
        }
        this.firstInputTimeout = TimeUnit.NANOSECONDS.convert(firstInputTimeout, TimeUnit.MILLISECONDS);
        this.maxInputTimeout = TimeUnit.NANOSECONDS.convert(maxInputTimeout, TimeUnit.MILLISECONDS);
        this.nextInputTimeout = TimeUnit.NANOSECONDS.convert(nextInputTimeout, TimeUnit.MILLISECONDS);
        if (logger.isTraceEnabled()) {
            logger.trace("Timers are configured: firstInputTimeout=" + this.firstInputTimeout + " nsec, maxInputTimeout="
                    + this.maxInputTimeout + " nsec, nextInputTimeout= " + this.nextInputTimeout + " nsec");
        }
    }

    class FirstInputTimeoutDetector implements Runnable {
        private final long startMySessionTimestamp;

        public FirstInputTimeoutDetector() {
            startMySessionTimestamp = startSessionTimestamp;
        }

        @Override
        public void run() {
            if (isTimestampInitialized(startMySessionTimestamp)) {
                if (startMySessionTimestamp == startSessionTimestamp) {
                    final long currentTimestamp = getCurrentTimestamp();
                    if (isTimestampInitialized(startWaitingForFirstInputTimestamp) && !isSpeechDetected()
                            && (firstInputTimeout > 0)) {
                        final long waitForInputTime = currentTimestamp - startWaitingForFirstInputTimestamp;
                        // Abort recording operation if user did not speak during initial detection period
                        if (waitForInputTime > firstInputTimeout) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("WIT [firstInputTimeout=" + firstInputTimeout + " nsec, currentTimestamp="
                                        + currentTimestamp + " nsec, firstInputTimestamp=" + firstInputTimestamp
                                        + " nsec, waitForInputTime=" + waitForInputTime + " nsec, lastInputTimestamp= "
                                        + lastInputTimestamp + ", speechDetected=" + isSpeechDetected() + "]");
                            }
                            if (inputTimeoutListener != null) {
                                inputTimeoutListener.onPreSpeechTimer();
                            }
                        }
                    }
                }
            } else {
                logger.warn("Session is not initialized");
            }
        }
    }

    class NextInputTimeoutDetector implements Runnable {
        private final long startMySessionTimestamp;

        public NextInputTimeoutDetector() {
            startMySessionTimestamp = startSessionTimestamp;
        }

        @Override
        public void run() {
            if (isTimestampInitialized(startMySessionTimestamp)) {
                if (startMySessionTimestamp == startSessionTimestamp) {
                    final long currentTimestamp = getCurrentTimestamp();
                    if (isSpeechDetected() && (nextInputTimeout > 0)) {
                        final long idleTime = currentTimestamp - lastInputTimestamp;
                        // Abort recording operation if user did not speak for a while
                        if (idleTime > nextInputTimeout) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("PST [nextInputTimeout=" + nextInputTimeout + " nsec, currentTimestamp="
                                        + currentTimestamp + " nsec, firstInputTimestamp=" + firstInputTimestamp
                                        + " nsec, idleTime=" + idleTime + " nsec, lastInputTimestamp = " + lastInputTimestamp
                                        + ", speechDetected=" + isSpeechDetected() + "]");
                            }
                            if (inputTimeoutListener != null) {
                                inputTimeoutListener.onPostSpeechTimer();
                            }
                        }
                    }
                }
            } else {
                logger.warn("Session is not initialized");
            }
        }
    }

    class MaxInputTimeoutDetector implements Runnable {
        private final long startMySessionTimestamp;

        public MaxInputTimeoutDetector() {
            startMySessionTimestamp = startSessionTimestamp;
        }

        @Override
        public void run() {
            if (isTimestampInitialized(startMySessionTimestamp)) {
                if (startMySessionTimestamp == startSessionTimestamp) {
                    final long currentTimestamp = getCurrentTimestamp();
                    // Abort recording if maximum time limit is reached
                    if (isSpeechDetected() && (maxInputTimeout > 0)) {
                        final long duration = currentTimestamp - firstInputTimestamp;
                        if (maxInputTimeout <= duration) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("MRT [maxInputTimeout=" + maxInputTimeout + " nsec, duration=" + duration
                                        + " nsec, currentTimestamp=" + currentTimestamp + " nsec, firstInputTimestamp="
                                        + firstInputTimestamp + " nsec, lastInputTimestamp = " + lastInputTimestamp
                                        + ", speechDetected=" + isSpeechDetected() + "]");
                            }
                            if (inputTimeoutListener != null) {
                                inputTimeoutListener.onMaximumRecognitionTime();
                            }
                        }
                    }
                }
            } else {
                logger.warn("Session is not initialized");
            }
        }
    }

}
