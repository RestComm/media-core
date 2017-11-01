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
package org.restcomm.media.drivers.asr.driver.google;

import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.restcomm.media.drivers.asr.AsrDriver;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;
import java.util.List;
import java.util.Map;
import org.restcomm.media.drivers.asr.AsrDriverException;
import static org.restcomm.media.drivers.asr.driver.google.GoogleDriverParameter.*;
/**
 * @author Ricardo Limonta (ricardo.limonta@gmail.com)
 */
public class GoogleAsrDriver implements AsrDriver {

    private static final Logger log = Logger.getLogger(GoogleAsrDriver.class);

    private static final int DEFAULT_RESPONSE_TIMEOUT = 2000;
    private static final int DEFAULT_HERTZ = 8000;
    private static final int DEFAULT_ALTERNATIVES = 1;
    private static final boolean DEFAULT_INTERIM_RESULTS = false;
    private static final String DEFAULT_LANGUAGE = "en-US";

    // Configuration
    private final List<String> languages;

    private int responseTimeout;
    private int hertz;
    private boolean interimResults;
    private String language;
    private int alternatives;

    private boolean running;

    // Result Listener
    private AsrDriverEventListener listener;

    private SpeechClient speech;
    private ApiStreamObserver requestObserver;
    private ApiStreamObserver responseObserver;

    public GoogleAsrDriver() {
        // Configuration
        this.languages = new ArrayList<>();
        this.languages.add("af-ZA"); //Afrikaans (South Africa)
        this.languages.add("am-ET"); //Amharic (Ethiopia)
        this.languages.add("hy-AM"); //Armenian (Armenia)
        this.languages.add("az-AZ"); //Azerbaijani (Azerbaijan)
        this.languages.add("id-ID"); //Indonesian (Indonesia)
        this.languages.add("ms-MY"); //Malay (Malaysia)
        this.languages.add("bn-BD"); //Bengali (Bangladesh)
        this.languages.add("bn-IN"); //Bengali (India)
        this.languages.add("ca-ES"); //Catalan (Spain)
        this.languages.add("cs-CZ"); //Czech (Czech Republic)
        this.languages.add("da-DK"); //Danish (Denmark)
        this.languages.add("de-DE"); //German (Germany)
        this.languages.add("en-AU"); //English (Australia)
        this.languages.add("en-CA"); //English (Canada)
        this.languages.add("en-GH"); //English (Ghana)
        this.languages.add("en-GB"); //English (United Kingdom)
        this.languages.add("en-IN"); //English (India)
        this.languages.add("en-IE"); //English (Ireland)
        this.languages.add("en-KE"); //English (Kenya)
        this.languages.add("en-NZ"); //English (New Zealand)
        this.languages.add("en-NG"); //English (Nigeria)
        this.languages.add("en-PH"); //English (Philippines)
        this.languages.add("en-ZA"); //English (South Africa)
        this.languages.add("en-TZ"); //English (Tanzania)
        this.languages.add("en-US"); //English (United States)
        this.languages.add("es-AR"); //Spanish (Argentina)
        this.languages.add("es-BO"); //Spanish (Bolivia)
        this.languages.add("es-CL"); //Spanish (Chile)
        this.languages.add("es-CO"); //Spanish (Colombia)
        this.languages.add("es-CR"); //Spanish (Costa Rica)
        this.languages.add("es-EC"); //Spanish (Ecuador)
        this.languages.add("es-SV"); //Spanish (El Salvador)
        this.languages.add("es-ES"); //Spanish (Spain)
        this.languages.add("es-US"); //Spanish (United States)
        this.languages.add("es-GT"); //Spanish (Guatemala)
        this.languages.add("es-HN"); //Spanish (Honduras)
        this.languages.add("es-MX"); //Spanish (Mexico)
        this.languages.add("es-NI"); //Spanish (Nicaragua)
        this.languages.add("es-PA"); //Spanish (Panama)
        this.languages.add("es-PY"); //Spanish (Paraguay)
        this.languages.add("es-PE"); //Spanish (Peru)
        this.languages.add("es-PR"); //Spanish (Puerto Rico)
        this.languages.add("es-DO"); //Spanish (Dominican Republic)
        this.languages.add("es-UY"); //Spanish (Uruguay)
        this.languages.add("es-VE"); //Spanish (Venezuela)
        this.languages.add("eu-ES"); //Basque (Spain)
        this.languages.add("fil-PH"); //Filipino (Philippines)
        this.languages.add("fr-CA"); //French (Canada)
        this.languages.add("fr-FR"); //French (France)
        this.languages.add("gl-ES"); //Galician (Spain)
        this.languages.add("ka-GE"); //Georgian (Georgia)
        this.languages.add("gu-IN"); //Gujarati (India)
        this.languages.add("hr-HR"); //Croatian (Croatia)
        this.languages.add("zu-ZA"); //Zulu (South Africa)
        this.languages.add("is-IS"); //Icelandic (Iceland)
        this.languages.add("it-IT"); //Italian (Italy)
        this.languages.add("jv-ID"); //Javanese (Indonesia)
        this.languages.add("kn-IN"); //Kannada (India)
        this.languages.add("km-KH"); //Khmer (Cambodia)
        this.languages.add("lo-LA"); //Lao (Laos)
        this.languages.add("lv-LV"); //Latvian (Latvia)
        this.languages.add("lt-LT"); //Lithuanian (Lithuania)
        this.languages.add("hu-HU"); //Hungarian (Hungary)
        this.languages.add("ml-IN"); //Malayalam (India)
        this.languages.add("mr-IN"); //Marathi (India)
        this.languages.add("nl-NL"); //Dutch (Netherlands)
        this.languages.add("ne-NP"); //Nepali (Nepal)
        this.languages.add("nb-NO"); //Norwegian Bokmål (Norway)
        this.languages.add("pl-PL"); //Polish (Poland)
        this.languages.add("pt-BR"); //Portuguese (Brazil)
        this.languages.add("pt-PT"); //Portuguese (Portugal)
        this.languages.add("ro-RO"); //Romanian (Romania)
        this.languages.add("si-LK"); //Sinhala (Srilanka)
        this.languages.add("sk-SK"); //Slovak (Slovakia)
        this.languages.add("sl-SI"); //Slovenian (Slovenia)
        this.languages.add("su-ID"); //Sundanese (Indonesia)
        this.languages.add("sw-TZ"); //Swahili (Tanzania)
        this.languages.add("sw-KE"); //Swahili (Kenya)
        this.languages.add("fi-FI"); //Finnish (Finland)
        this.languages.add("sv-SE"); //Swedish (Sweden)
        this.languages.add("ta-IN"); //Tamil (India)
        this.languages.add("ta-SG"); //Tamil (Singapore)
        this.languages.add("ta-LK"); //Tamil (Sri Lanka)
        this.languages.add("ta-MY"); //Tamil (Malaysia)
        this.languages.add("te-IN"); //Telugu (India)
        this.languages.add("vi-VN"); //Vietnamese (Vietnam)
        this.languages.add("tr-TR"); //Turkish (Turkey)
        this.languages.add("ur-PK"); //Urdu (Pakistan)
        this.languages.add("ur-IN"); //Urdu (India)
        this.languages.add("el-GR"); //Greek (Greece)
        this.languages.add("bg-BG"); //Bulgarian (Bulgaria)
        this.languages.add("ru-RU"); //Russian (Russia)
        this.languages.add("sr-RS"); //Serbian (Serbia)
        this.languages.add("uk-UA"); //Ukrainian (Ukraine)
        this.languages.add("he-IL"); //Hebrew (Israel)
        this.languages.add("ar-IL"); //Arabic (Israel)
        this.languages.add("ar-JO"); //Arabic (Jordan)
        this.languages.add("ar-AE"); //Arabic (United Arab Emirates)
        this.languages.add("ar-BH"); //Arabic (Bahrain)
        this.languages.add("ar-DZ"); //Arabic (Algeria)
        this.languages.add("ar-SA"); //Arabic (Saudi Arabia)
        this.languages.add("ar-IQ"); //Arabic (Iraq)
        this.languages.add("ar-KW"); //Arabic (Kuwait)
        this.languages.add("ar-MA"); //Arabic (Morocco)
        this.languages.add("ar-TN"); //Arabic (Tunisia)
        this.languages.add("ar-OM"); //Arabic (Oman)
        this.languages.add("ar-PS"); //Arabic (State of Palestine)
        this.languages.add("ar-QA"); //Arabic (Qatar)
        this.languages.add("ar-LB"); //Arabic (Lebanon)
        this.languages.add("ar-EG"); //Arabic (Egypt)
        this.languages.add("fa-IR"); //Persian (Iran)
        this.languages.add("hi-IN"); //Hindi (India)
        this.languages.add("th-TH"); //Thai (Thailand)
        this.languages.add("ko-KR"); //Korean (South Korea)
        this.languages.add("cmn-Hant-TW"); //Chinese, Mandarin (Traditional, Taiwan)
        this.languages.add("yue-Hant-HK"); //Chinese, Cantonese (Traditional, Hong Kong)
        this.languages.add("ja-JP"); //Japanese (Japan)
        this.languages.add("cmn-Hans-HK"); //Chinese, Mandarin (Simplified, Hong Kong)
        this.languages.add("cmn-Hans-CN"); //Chinese, Mandarin (Simplified, China)

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
        try {
            speech = SpeechClient.create();
        } catch (IOException ex) {
            log.warn("Error on create Google SpeechClient", ex);
        }

        // Configure response timeout
        this.responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
        
        String responseTimeoutParam = null;
        if (parameters.containsKey(RESPONSE_TIMEOUT.symbol())) {
            try {
                responseTimeoutParam = parameters.get(RESPONSE_TIMEOUT.symbol());
                this.responseTimeout = Integer.parseInt(responseTimeoutParam);
            } catch (NumberFormatException e) {
                log.warn("Could not apply " + RESPONSE_TIMEOUT.symbol() + " parameter: " + responseTimeoutParam + ". Defaulting to " + DEFAULT_RESPONSE_TIMEOUT);
            }
        }
        
        // Configure interim results
        this.interimResults = DEFAULT_INTERIM_RESULTS;
        
        if (parameters.containsKey(INTERIM_RESULTS.symbol())) {
            this.interimResults = Boolean.parseBoolean(parameters.get(INTERIM_RESULTS.symbol()));
        }

        //configure media hertz
        this.hertz = DEFAULT_HERTZ;

        String hertzParameter = null;
        if (parameters.containsKey(HERTZ.symbol())) {
            try {
                hertzParameter = parameters.get(HERTZ.symbol());
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
        if (!languages.contains(lang)) {
            //if not supported, stop the recognition process
            final AsrDriverException e = new AsrDriverException("Language " + lang + " not supported");
            this.listener.onError(e);
            return;
        }
        
        // Start execution
        this.running = true;
        
        // Configure request with local raw PCM audio
        RecognitionConfig recConfig = RecognitionConfig.newBuilder()
                .setEncoding(AudioEncoding.LINEAR16)
                .setLanguageCode(lang)
                .setSampleRateHertz(this.hertz)
                .build();

        StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder()
                .setConfig(recConfig)
                .setInterimResults(this.interimResults)
                .build();

        
        class ResponseApiStreamingObserver<T> implements ApiStreamObserver<T> {
            @Override
            public void onNext(T message) {                        
                StreamingRecognitionResult result = ((StreamingRecognizeResponse)message).getResultsList().get(0);
                if (listener != null) {
                    listener.onSpeechRecognized(result.getAlternativesList().get(0).getTranscript(), 
                                                result.getIsFinal());
                    log.info("Transcript: " + result.getAlternativesList().get(0).getTranscript() + ", isFinal: " + result.getIsFinal());
                }
            }

            @Override
            public void onError(Throwable t) {
                if (listener != null) {
                    listener.onError(new AsrDriverException("Unexpected driver error.", t));
                }
            }

            @Override
            public void onCompleted() {}
        }

        responseObserver = new ResponseApiStreamingObserver<>();

        BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable = speech.streamingRecognizeCallable();

        requestObserver = callable.bidiStreamingCall(responseObserver);

        // The first request must **only** contain the audio configuration:
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(config).build());        
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
        
        //send audio data
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data)).build());
    }

    @Override
    public void finishRecognizing() {
        try {
            speech.close();
        } catch (Exception ex) {
            log.warn("Error on close Google SpeechClient", ex);
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