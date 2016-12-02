#!/bin/bash
## Description: Configures the JAVA_OPTS for the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

configJavaOpts() {
    local config_file="$MS_HOME/bin/run.sh"
    local ms_opts="$MS_OPTS"

    if [ -n "$ms_opts" ]; then
        echo "Configuring JAVA_OPTS [$ms_opts]"

        # Update configuration
        sed -e "/# Setup MMS specific properties/ {
            N; s|JAVA_OPTS=.*|JAVA_OPTS=\"-Dprogram\.name=\\\$PROGNAME $ms_opts\"|
        }" $config_file > $config_file.bak
        mv $config_file.bak $config_file

        # Make script runnable again
        chmod +x $config_file
    fi
}

configJavaOpts
