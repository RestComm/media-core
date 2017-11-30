#!/bin/bash
## Description: Configures the Resources of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configAudioCache() {
    readonly cache_enabled=${1-false}
    readonly cache_size=${2-100}

    echo "Configuring Audio Cache [Enabled=$cache_enabled, Size=$cache_size]"

    xmlstarlet ed --inplace --pf \
        -u "/mediaserver/resources/player/cache/cacheSize" -v "$cache_size" \
        -u "/mediaserver/resources/player/cache/cacheEnabled" -v "$cache_enabled" \
        $MS_HOME/conf/mediaserver.xml
}

configDtmfDetector() {
    readonly dbi=${1--30}
    readonly duration=${2-80}
    readonly interval=${3-400}

    echo "Configuring DTMF Detector [Dbi=$dbi, Tone Duration=$duration, Tone Interval=$interval]"

    xmlstarlet ed --inplace --pf \
        -u "/mediaserver/resources/dtmfDetector/@dbi" -v "$dbi" \
        -u "/mediaserver/resources/dtmfDetector/@toneDuration" -v "$duration" \
        -u "/mediaserver/resources/dtmfDetector/@toneInterval" -v "$interval" \
        $MS_HOME/conf/mediaserver.xml
}

configSpeechDetector() {
    readonly silence_level=${1-10}

    echo "Configuring Speech Detector [Silence Level=$silence_level]"

    xmlstarlet ed --inplace --pf \
        -u "/mediaserver/resources/speechDetector/@silenceLevel" -v "$silence_level" \
        $MS_HOME/conf/mediaserver.xml
}

configAudioCache $AUDIO_CACHE_ENABLED $AUDIO_CACHE_SIZE
configDtmfDetector $DTMF_DETECTOR_DBI $DTMF_DETECTOR_TONE_DURATION $DTMF_DETECTOR_TONE_INTERVAL
configSpeechDetector $SPEECH_DETECTOR_SILENCE_LEVEL
