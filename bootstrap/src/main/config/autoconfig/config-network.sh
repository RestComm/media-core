#!/bin/bash
## Description: Configures the Network aspects of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configNetwork() {
	file=$MS_HOME/conf/mediaserver.xml
	bind_address=${1-127.0.0.1}
	external_address=${2-127.0.0.1}
	network=${3-127.0.0.1}
	subnet=${4-255.255.255.255}
	sbc=${5-false}
	
	echo "Configuring Network [BindAddress=$bind_address, ExternalAddress=$external_address, Network=$network, Subnet=$subnet, Use SBC=$sbc]"

	xmlstarlet ed -u "/mediaserver/network/bindAddress" -v "$bind_address" \
		-u "/mediaserver/network/externalAddress" -v "$external_address" \
		-u "/mediaserver/network/network" -v "$network" \
		-u "/mediaserver/network/subnet" -v "$subnet" \
		-u "/mediaserver/network/sbc" -v "$sbc" $file
}

configNetwork $BIND_ADDRESS $EXTERNAL_ADDRESS $NETWORK $SUBNET $USE_SBC