#!/bin/bash
## Description: Configures the MGCP Controller of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configMgcpController() {
    address=${1-127.0.0.1}
    port=${2-2427}
    channelBuffer=${3-5000}
	
    echo "Configuring MGCP Controller [Address=$address, Port=$port, ChannelBuffer=$channelBuffer]"

    xmlstarlet ed --inplace --pf \
	    -u "/mediaserver/controller[@protocol='mgcp']/address" -v "$address" \
        -u "/mediaserver/controller[@protocol='mgcp']/port" -v "$port" \
        -u "/mediaserver/controller[@protocol='mgcp']/channelBuffer" -v "$channelBuffer" \
        $MS_HOME/conf/mediaserver.xml
}

configMgcpController $MGCP_ADDRESS $MGCP_PORT $MGCP_CHANNELBUFFER
