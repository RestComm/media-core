#!/bin/bash
## Description: Configures the MGCP Controller of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configMgcpController() {
    address=${1-127.0.0.1}
    port=${2-2427}
	
    echo "Configuring MGCP Controller [Address=$address, Port=$port]"

    xmlstarlet ed --inplace --pf \
	    -u "/mediaserver/controller[@protocol='mgcp']/address" -v "$address" \
        -u "/mediaserver/controller[@protocol='mgcp']/port" -v "$port" \
        $MS_HOME/conf/mediaserver.xml
}

configMgcpController $MGCP_ADDRESS $MGCP_PORT
