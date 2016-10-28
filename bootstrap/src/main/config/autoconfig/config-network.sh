#!/bin/bash
## Description: Configures the Network aspects of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configNetwork() {
	readonly file=$MS_HOME/conf/mediaserver.xml
	readonly bind_address=$1
	readonly external_address=$2
	readonly network=$3
	readonly subnet=$4
	
	echo "Configuring Network..."
	echo "BindAddress=$bind_address, ExternalAddress=$external_address, Network=$network, Subnet=$subnet"
	


}


configServerBeans() {
	FILE=$MMS_HOME/deploy/server-beans.xml
	MSERVER_EXTERNAL_ADDRESS="$MEDIASERVER_EXTERNAL_ADDRESS"

	if [ "$MSERVER_EXTERNAL_ADDRESS" = "$1" ]; then
   		MSERVER_EXTERNAL_ADDRESS="\<null\/\>"
	fi

	#Check for Por Offset
	local REMOTEMGCP=$((REMOTEMGCP + PORT_OFFSET))

	sed -i 's|<property name="port">.*</property>|<property name="port">'"${REMOTEMGCP}"'</property>|' $FILE


	sed -e "s|<property name=\"bindAddress\">.*<\/property>|<property name=\"bindAddress\">$1<\/property>|" \
	    -e "s|<property name=\"localBindAddress\">.*<\/property>|<property name=\"localBindAddress\">$1<\/property>|" \
		-e "s|<property name=\"externalAddress\">.*</property>|<property name=\"externalAddress\">$MSERVER_EXTERNAL_ADDRESS</property>|" \
	    -e "s|<property name=\"localNetwork\">.*<\/property>|<property name=\"localNetwork\">$2<\/property>|" \
	    -e "s|<property name=\"localSubnet\">.*<\/property>|<property name=\"localSubnet\">$3<\/property>|" \
	    -e "s|<property name=\"useSbc\">.*</property>|<property name=\"useSbc\">$USESBC</property>|" \
	    -e "s|<property name=\"dtmfDetectorDbi\">.*</property>|<property name=\"dtmfDetectorDbi\">$DTMFDBI</property>|" \
	    -e "s|<property name=\"lowestPort\">.*</property>|<property name=\"lowestPort\">$MEDIASERVER_LOWEST_PORT</property>|" \
	    -e "s|<property name=\"highestPort\">.*</property>|<property name=\"highestPort\">$MEDIASERVER_HIGHEST_PORT</property>|" \
	    $FILE > $FILE.bak
	mv $FILE.bak $FILE
	echo 'Configured UDP Manager'
}

