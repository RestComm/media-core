#!/bin/bash
## Description: Configures the Media aspects of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configMedia() {
	readonly timeout=${1-0}
    readonly max_duration=${2-14440}
	readonly lowPort=${3-54534}
	readonly highPort=${4-65534}
	readonly jitter_size=${5-50}
	
	echo "Configuring MGCP Media [Timeout=$timeout, Max Duration=$max_duration, Lowest Port=$lowPort, Highest Port=$highPort, Jitter Size=$jitter_size]"

	xmlstarlet ed --inplace --pf \
        -u "/mediaserver/media/timeout" -v "$timeout" \
        -u "/mediaserver/media/maxDuration" -v "$max_duration" \
        -u "/mediaserver/media/lowPort" -v "$lowPort" \
        -u "/mediaserver/media/highPort" -v "$highPort" \
        -u "/mediaserver/media/jitterBuffer/@size" -v "$jitter_size" \
        $MS_HOME/conf/mediaserver.xml
}

configCodecs() {
	readonly codecs=${1-pcmu,pcma,l16,gsm,g729,telephone-event}
	
	echo "Configuring Media Codecs [$codecs]"
	
	# Delete all codecs
	xmlstarlet ed --inplace --pf -d "/mediaserver/media/codecs/node()" $MS_HOME/conf/mediaserver.xml
	
	# Create codec nodes according to codecs string
	IFS=',' read -ra codecArray <<< "$codecs"
	for codec in "${codecArray[@]}"; do
    	xmlstarlet ed --inplace --pf \
            -s "/mediaserver/media/codecs" -t elem -n codec -v "" \
			-s "/mediaserver/media/codecs/codec[last()]" -t attr -n "name" -v "$codec" \
            $MS_HOME/conf/mediaserver.xml
	done
}

configMedia $MEDIA_TIMEOUT $MEDIA_MAX_DURATION $MEDIA_LOW_PORT $MEDIA_HIGH_PORT $MEDIA_JITTER_SIZE
configCodecs $MEDIA_CODECS