#!/bin/bash
## Description: Configures the Network aspects of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configNetwork() {
    local bind_address=${BIND_ADDRESS-127.0.0.1}
    local external_address=$EXTERNAL_ADDRESS
    local network=${NETWORK-127.0.0.1}
    local subnet=${SUBNET-255.255.255.255}
    local sbc=${USE_SBC-false}

    echo "Configuring Network [BindAddress=$bind_address, ExternalAddress=$external_address, Network=$network, Subnet=$subnet, Use SBC=$sbc]"

    xmlstarlet ed --inplace --pf \
        -u "/mediaserver/network/bindAddress" -v "$bind_address" \
        -u "/mediaserver/network/externalAddress" -v "$external_address" \
        -u "/mediaserver/network/network" -v "$network" \
        -u "/mediaserver/network/subnet" -v "$subnet" \
        -u "/mediaserver/network/sbc" -v "$sbc" \
        $MS_HOME/conf/mediaserver.xml
}

configNetwork