#!/bin/bash
## Description: Configures the MGCP Controller of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configMgcpController() {
	file=$MS_HOME/conf/mediaserver.xml
	address=${1-127.0.0.1}
	port=${2-2427}
	expected_load=${3-50}
	
	echo "Configuring MGCP Controller [Address=$address, Port=$port, Expected Load=$expected_load]"

	xmlstarlet ed -u "/mediaserver/controller[@protocol='mgcp']/address" -v "$address" \
		-u "/mediaserver/controller[@protocol='mgcp']/port" -v "$port" \
		-u "/mediaserver/controller[@protocol='mgcp']/poolSize" -v "$expected_load" \
		-u "/mediaserver/controller[@protocol='mgcp']/endpoints/*/@poolSize" -v "$expected_load" $file
}

configMgcpController $MGCP_ADDRESS $MGCP_PORT $EXPECTED_LOAD
