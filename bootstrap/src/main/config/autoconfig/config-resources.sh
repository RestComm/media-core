#!/bin/bash
## Description: Configures the Resource Pooling of the Media Server.
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

    echo "Configuring DTMF Detector [Dbi=$dbi]"

    xmlstarlet ed --inplace --pf -u "/mediaserver/resources/dtmfDetector/@dbi" -v "$dbi" $MS_HOME/conf/mediaserver.xml
}

configAudioCache $AUDIO_CACHE_ENABLED $AUDIO_CACHE_SIZE
configDtmfDetector $DTMF_DETECTOR_DBI