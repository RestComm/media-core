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
package org.restcomm.media.drivers.asr.driver.watson;

import com.ibm.watson.developer_cloud.http.HttpMediaType;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechAlternative;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Transcript;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import okhttp3.WebSocket;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.restcomm.media.drivers.asr.AsrDriver;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;
import org.restcomm.media.drivers.asr.AsrDriverException;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.restcomm.media.drivers.asr.driver.watson.WatsonDriverParameter.*;

/**
 * @author Ricardo Limonta
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class WatsonAsrDriver implements AsrDriver {

    private static final Logger log = Logger.getLogger(WatsonAsrDriver.class);

    private static final int DEFAULT_RESPONSE_TIMEOUT = 2000;
    private static final int DEFAULT_HERTZ = 8000;
    private static final int DEFAULT_ALTERNATIVES = 1;
    private static final boolean DEFAULT_INTERIM_RESULTS = false;
    private static final String DEFAULT_LANGUAGE = "en-US";

    // Configuration
    private final Map<String, String> languages;

    private int responseTimeout;
    private int hertz;
    private boolean interimResults;
    private String language;
    private int alternatives;

    // Execution Context
    private SpeechToText service;
    private WebSocket webSocket;
    private PipedInputStream inputStream;
    private PipedOutputStream outputStream;

    private boolean running;

    // Result Listener
    private AsrDriverEventListener listener;

    public WatsonAsrDriver() {
        // Configuration
        this.languages = new HashMap<>(6);
        this.languages.put("en-GB", "en-GB_NarrowbandModel");
        this.languages.put("en-US", "en-US_NarrowbandModel");
        this.languages.put("es-ES", "es-ES_NarrowbandModel");
        this.languages.put("ja-JP", "ja-JP_NarrowbandModel");
        this.languages.put("pt-BR", "pt-BR_NarrowbandModel");
        this.languages.put("zh-CN", "zh-CN_NarrowbandModel");

        this.responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
        this.hertz = DEFAULT_HERTZ;
        this.interimResults = DEFAULT_INTERIM_RESULTS;
        this.language = DEFAULT_LANGUAGE;
        this.alternatives = DEFAULT_ALTERNATIVES;

        // Execution Context
        this.running = false;
    }

    @Override
    public void configure(Map<String, String> parameters) {
        // Create new ASR Service
        this.service = new SpeechToText(parameters.get(API_USERNAME.symbol()), parameters.get(API_PASSWORD.symbol()));

        // Configure response timeout
        this.responseTimeout = DEFAULT_RESPONSE_TIMEOUT;

        final String responseTimeoutParam = parameters.get(RESPONSE_TIMEOUT.symbol());
        if (!StringUtils.isEmpty(responseTimeoutParam)) {
            try {
                this.responseTimeout = Integer.parseInt(responseTimeoutParam);
            } catch (NumberFormatException e) {
                log.warn("Could not apply " + RESPONSE_TIMEOUT.symbol() + " parameter: " + responseTimeoutParam + ". Defaulting to " + DEFAULT_RESPONSE_TIMEOUT);
            }
        }

        // Configure interim results
        final String interimResultsParam = parameters.get(INTERIM_RESULTS.symbol());
        this.interimResults = Boolean.parseBoolean(interimResultsParam);

        //configure media hertz
        this.hertz = DEFAULT_HERTZ;

        final String hertzParameter = parameters.get(HERTZ.symbol());
        if (!StringUtils.isEmpty(hertzParameter)) {
            try {
                this.hertz = Integer.parseInt(hertzParameter);
            } catch (NumberFormatException e) {
                log.warn("Could not apply " + HERTZ.symbol() + " parameter: " + hertzParameter + ". Defaulting to " + DEFAULT_HERTZ);
            }
        }
    }

    @Override
    public void startRecognizing(String lang, List<String> hints) {
        if (this.running) {
            throw new IllegalStateException("Driver is already running.");
        }

        //verify if language is supported
        if (!languages.containsKey(lang)) {
            //if not supported, stop the recognition process
            final AsrDriverException e = new AsrDriverException("Language " + lang + " not supported");
            this.listener.onError(e);
            return;
        }

        // Start execution
        this.running = true;

        //create the recognize options
        final String[] hintsArray = hints == null ? new String[0] : hints.toArray(new String[hints.size()]);
        final RecognizeOptions options = new RecognizeOptions.Builder().contentType(HttpMediaType.createAudioRaw(this.hertz)).maxAlternatives(this.alternatives).model(this.languages.get(lang)).interimResults(this.interimResults).continuous(true).keywords(hintsArray).build();

        // Setup streams
        try {
            // TODO calculate buffer size automatically
            this.inputStream = new PipedInputStream(320 * 40);
            this.outputStream = new PipedOutputStream(inputStream);
        } catch (IOException e) {
            listener.onError(new AsrDriverException("Could not open streams for ASR operation.", e));
        }

        // Establish session with watson
        this.webSocket = service.recognizeUsingWebSocket(this.inputStream, options, new BaseRecognizeCallback() {

            @Override
            public void onTranscription(SpeechResults speechResults) {
                final StringBuilder result = new StringBuilder();
                for (Transcript transcript : speechResults.getResults()) {
                    for (SpeechAlternative alternative : transcript.getAlternatives()) {
                        result.append(alternative.getTranscript());
                    }
                }

                if (WatsonAsrDriver.this.listener != null) {
                    listener.onSpeechRecognized(result.toString(), speechResults.isFinal());
                }
            }

            @Override
            public void onError(Exception e) {
                if (WatsonAsrDriver.this.listener != null) {
                    listener.onError(new AsrDriverException("Unexpected driver error.", e));
                }
            }
        });
    }

    @Override
    public void write(byte[] data) {
        this.write(data, 0, data.length);
    }

    @Override
    public void write(byte[] data, int offset, int len) {
        if(!this.running) {
            return;
        }

        try {
            this.outputStream.write(data, offset, len);
            this.outputStream.flush();
        } catch (IOException e) {
            final AsrDriverException error = new AsrDriverException("Could not write into live stream.", e);
            if (this.listener != null) {
                listener.onError(error);
            }
        }
    }

    @Override
    public void finishRecognizing() {
        if (this.running) {
            this.running = false;

            if(log.isDebugEnabled()) {
                log.debug("Stopping recognition");
            }

            // Destroy websocket connection
            if (this.webSocket != null) {
                //Section 7.4 of RFC 6455.
                this.webSocket.close(1000, "finishing speech recognition");
                this.webSocket = null;
            }

            // Close input stream
            if (this.inputStream != null) {
                try {
                    this.inputStream.close();
                } catch (IOException e) {
                    log.error("Failed to close input stream", e);
                }
                this.inputStream = null;
            }

            // Close output stream
            if (this.outputStream != null) {
                try {
                    this.outputStream.close();
                } catch (IOException e) {
                    log.error("Failed to close output stream", e);
                }
                this.outputStream = null;
            }
        }
    }

    @Override
    public void setListener(AsrDriverEventListener listener) {
        this.listener = listener;
    }

    @Override
    public int getResponseTimeoutInMilliseconds() {
        return responseTimeout;
    }

}