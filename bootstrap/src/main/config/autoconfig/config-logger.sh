#!/bin/bash
## Description: Configures the Logger of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

patchPath() {
    if [[ "$1" = /* ]]; then
        echo "$1"
    else
        echo $MS_HOME/$1
    fi
}

configLogPath() {
    local log_path=${1-log/server.log}

    echo "Setting LOG FILE PATH to $(patchPath $log_path)"

    xmlstarlet ed --inplace --pf \
        -u "/Configuration/Appenders/RollingFile[@name='FILE']/@fileName" -v "$(patchPath $log_path)" \
        $MS_HOME/conf/log4j2.xml
}

configRoot() {
    local threshold=${1-INFO}

    xmlstarlet ed --inplace --pf \
        -u "/Configuration/Loggers/Root/@level" -v "$threshold" \
        $MS_HOME/conf/log4j2.xml
}

getCategories() {
    ( set -o posix ; set ) | grep 'LOG_CATEGORY_'
}

configCategories() {
    # Delete all categories
    xmlstarlet ed --inplace --pf -d "/Configuration/Loggers/Logger" $MS_HOME/conf/log4j2.xml

    # Add loaded categories
    local categories=$(getCategories)
    if [ -n "$categories" ]; then
        while read -r category; do
            local category_var=$(echo "$category" | sed -e 's|\(.*\)=.*|\1|' )
            local category_value=$(eval echo '$'"$category_var")
            local category_name=$(echo "$category_value" | sed -e 's|\(.*\):.*|\1|')
            local category_threshold=$(echo "$category_value" | sed -e 's|.*:\(.*\)|\1|')

            echo "Setting LOG_CATEGORY $category_name to $category_threshold"

            xmlstarlet ed --inplace --pf \
                -s "/Configuration/Loggers" -t elem -n "Logger" -v "" \
                -s "/Configuration/Loggers/Logger[last()]" -t attr -n "name" -v "$category_name" \
                -s "/Configuration/Loggers/Logger[last()]" -t attr -n "level" -v "$category_threshold" \
                $MS_HOME/conf/log4j2.xml
        done <<< "$categories"
    fi
}

formatFile() {
    local log_file=$MS_HOME/conf/log4j2.xml
    xmlstarlet fo --indent-spaces 4 $log_file > $log_file.bak
    mv -f $log_file.bak $log_file
}

configRoot $LOG_ROOT
configCategories
configLogPath $LOG_FILE_URL
formatFile

