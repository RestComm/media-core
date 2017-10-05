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
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.*;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.WebSocket;
import org.apache.log4j.Logger;
import org.restcomm.media.drivers.asr.AsrDriver;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;
import org.restcomm.media.drivers.asr.AsrDriverException;


/**
 * @author Ricardo Limonta
 */
public class WatsonAsrDriver implements AsrDriver {

    private static final Logger log = Logger.getLogger(WatsonAsrDriver.class);

    private SpeechToText service;
    private PipedInputStream inputStream;
    private PipedOutputStream outputStream;
    private WebSocket ws;
    private RecognizeOptions options;
    private int responseTimeout;
    private int hertz;
    private boolean interimResults;
    private AsrDriverEventListener listener;
    private Map<String, String> languages;

    @Override
    public void configure(Map<String, String> parameters) {

        log.info("Configuring  WatsonAsrDriver...");

        //create service instance
        this.service = new SpeechToText(parameters.get("apiUsername"), parameters.get("apiPassword"));

        //configure response timeout
        if (parameters.containsKey("responseTimeout")) {
            this.responseTimeout = Integer.parseInt(parameters.get("responseTimeout"));
        } else {
            this.responseTimeout = 2000;
        }

        //configure interim results
        if (parameters.containsKey("interimResults")) {
            this.interimResults = Boolean.parseBoolean(parameters.get("interimResults"));
        }

        //configure media hertz
        if (parameters.containsKey("hertz")) {
            this.hertz = Integer.parseInt(parameters.get("hertz"));
        } else {
            hertz = 8000;
        }
        
        log.info("interimResults: " + this.interimResults);
        log.info("hertz: " + this.hertz);
        log.info("responseTimeout: " + this.responseTimeout);

        //create a list of supported languages (supports only NarrowbandModel 8000KHz)
        languages = new HashMap<>();
        languages.put("en-GB", "en-GB_NarrowbandModel");
        languages.put("en-US", "en-US_NarrowbandModel");
        languages.put("es-ES", "es-ES_NarrowbandModel");
        languages.put("ja-JP", "ja-JP_NarrowbandModel");
        languages.put("pt-BR", "pt-BR_NarrowbandModel");
        //TODO confirm with Henrique
        languages.put("pt-PT", "pt-BR_NarrowbandModel");
        languages.put("zh-CN", "zh-CN_NarrowbandModel");
    }

    @Override
    public void startRecognizing(String lang, List<String> hints) {

        log.info("start recognizing...");
        
        //verify if language is supported
        if (!languages.containsKey(lang)) {
            //if not supported, set english as default
            lang = languages.get("en-US");
        }

        log.info("lang: " + lang);
        
        //create the recognize options
        options = new RecognizeOptions.Builder().contentType(HttpMediaType.createAudioRaw(hertz))
                                                .maxAlternatives(1)
                                                .continuous(true)
                                                .model(languages.get(lang))
                                                .interimResults(interimResults).build();

        // Setup streams
        this.inputStream = new PipedInputStream();
        this.outputStream = new PipedOutputStream();
        try {
            this.inputStream.connect(outputStream);
        } catch (IOException e) {
            log.error(e);
        }

        // Establish session with watson
        ws = service.recognizeUsingWebSocket(this.inputStream, options, new BaseRecognizeCallback() {

            @Override
            public void onTranscription(SpeechResults speechResults) {
                StringBuilder result = new StringBuilder();
                for (Transcript transcript : speechResults.getResults()) {
                    for (SpeechAlternative alternative : transcript.getAlternatives()) {
                        result.append(alternative.getTranscript());
                    }
                }

                log.info("speech result: " + result.toString() + ", isFinal: " + speechResults.isFinal());

                listener.onSpeechRecognized(result.toString(), speechResults.isFinal());
            }
            
            @Override
            public void onError(Exception e) {
                log.error("Watson Driver Error", e);
                listener.onError(new AsrDriverException(e));
            }
        });
    }

    @Override
    public void write(byte[] data) {
        this.write(data, 0, data.length);
    }

    @Override
    public void write(byte[] data, int offset, int len) {
        try {
            this.outputStream.write(data, offset, len);
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Override
    public void finishRecognizing() {
        //destroy websocket connection
        if (this.ws != null) {
            //Section 7.4 of RFC 6455.
            this.ws.close(1000, "finishing speech recognition");
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