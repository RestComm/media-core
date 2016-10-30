#!/bin/bash
## Description: Configures the Media aspects of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configMedia() {
	readonly file=$MS_HOME/conf/mediaserver.xml
	readonly timeout=${1-0}
	readonly lowPort=${2-34534}
	readonly highPort=${3-65534}
	readonly jitter_size=${4-50}
	
	echo "Configuring MGCP Media [Timeout=$timeout, Lowest Port=$lowPort, Highest Port=$highPort, Jitter Size=$jitter_size]"

	xmlstarlet ed -u "/mediaserver/media/timeout" -v "$timeout" \
	              -u "/mediaserver/media/lowPort" -v "$lowPort" \
	              -u "/mediaserver/media/highPort" -v "$highPort" \
	              -u "/mediaserver/media/jitterBuffer/@size" -v "$jitter_size" $file
}

configCodecs() {
	readonly file=$MS_HOME/conf/mediaserver.xml
	readonly codecs=${1-pcmu,pcma,l16,gsm,g729,telephone-event}
	
	echo "Configuring Media Codecs [$codecs]"
	
	# Delete all codecs
	xmlstarlet ed -d "/mediaserver/media/codecs/node()" $file
	
	# Create codec nodes according to codecs string
	IFS=',' read -ra codecArray <<< "$codecs"
	for codec in "${codecArray[@]}"; do
    	xmlstarlet ed -s "/mediaserver/media/codecs" -t elem -n codec -v "" \
			-s "/mediaserver/media/codecs/codec[last()]" -t attr -n "name" -v "$codec" $file
	done
}

configMedia $MEDIA_TIMEOUT $MEDIA_LOW_PORT $MEDIA_HIGH_PORT $MEDIA_JITTER_SIZE
configCodecs $MEDIA_CODECS