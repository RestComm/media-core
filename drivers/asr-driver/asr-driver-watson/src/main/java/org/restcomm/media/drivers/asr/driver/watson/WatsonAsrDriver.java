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
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.Transcript;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.WebSocket;
import org.restcomm.media.drivers.asr.AsrDriver;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;
import org.restcomm.media.drivers.asr.AsrDriverException;

/**
 * @author Ricardo Limonta
 */
public class WatsonAsrDriver implements AsrDriver {

    private SpeechToText service;
    private WebSocket ws;
    private RecognizeOptions options;
    private int responseTimeout;
    private boolean interimResults;
    private AsrDriverEventListener listener;
    private Map<String, String> languages;
    
    @Override
    public void configure(Map<String, String> parameters) {
        //create service instance
        this.service = new SpeechToText(parameters.get("apiUsername"), 
                                        parameters.get("apiPassword"));
        
        //configure response timeout
        if (parameters.containsKey("responseTimeout")) {
            this.responseTimeout = Integer.parseInt(parameters.get("responseTimeout"));
        }
        
        //configure interim results
        if (parameters.containsKey("interimResults")) {
            this.interimResults = Boolean.parseBoolean(parameters.get("interimResults"));
        }

        
        //create a list of supported languages (supports only NarrowbandModel 8000KHz)
        languages = new HashMap<>();
        languages.put("en-GB", "en-GB_NarrowbandModel");
        languages.put("en-US", "en-US_NarrowbandModel");
        languages.put("es-ES", "es-ES_NarrowbandModel");
        languages.put("ja-JP", "ja-JP_NarrowbandModel");
        languages.put("pt-BR", "pt-BR_NarrowbandModel");
        languages.put("zh-CN", "zh-CN_NarrowbandModel");
    }

    @Override
    public void startRecognizing(String lang, List<String> hints)  {
        
        //verify if language is supported
        if (!languages.containsKey(lang)) {
            //if not supported, set english as default
            lang = languages.get("en-US");
        }
        
        //create the recognize options
        options = new RecognizeOptions.Builder().contentType(HttpMediaType.AUDIO_WAV)
                                                .model(languages.get(lang))
                                                .interimResults(interimResults)
                                                .build();
    }

    @Override
    public void write(byte[] data) {
        this.write(data, 0, 0);
    }
    
    @Override
    public void write(byte[] data, int offset, int len) {
        ws = service.recognizeUsingWebSocket(new ByteArrayInputStream(data), options, new BaseRecognizeCallback() 
            {
                @Override
                public void onTranscription(SpeechResults speechResults) {
                    StringBuilder result = new StringBuilder();
                    for (Transcript transcript : speechResults.getResults()) {
                        for (SpeechAlternative alternative : transcript.getAlternatives()) {
                            result.append(alternative.getTranscript());
                        }
                    }
                    
                    listener.onSpeechRecognized(result.toString(), speechResults.isFinal());
                }
                
                @Override
                public void onError(Exception e) {
                    listener.onError(new AsrDriverException(e)); 
                }   
            }
        );
    }

    @Override
    public void finishRecognizing() {
        //destroy websocket connection
        this.ws.cancel();
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