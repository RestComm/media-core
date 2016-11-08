#!/bin/bash
## Description: Configures the Logger of the Media Server.
## Author: Henrique Rosa (henrique.rosa@telestax.com)

getAppenders() {
    ( set -o posix ; set ) | grep LOG_APPENDER_ | sed -e 's|LOG_APPENDER_\(.*\)=.*|\1|'
}

configAppenders() {
    local appenders=$(getAppenders)
    if [ -n "$appenders" ]; then
        while read -r appender; do
            local appender_var="LOG_APPENDER_$appender"
            local log_threshold=$(eval echo '$'"$appender_var")
            echo "Setting $appender_var to $log_threshold"

        xmlstarlet ed --inplace --pf \
            -u "/log4j:configuration/appender[@name='$appender']/param[@name='Threshold']/@value" -v "$log_threshold" \
            $MS_HOME/conf/log4j.xml
        done <<< "$appenders"
    fi
}

getCategories() {
    ( set -o posix ; set ) | grep 'LOG_CATEGORY_'
}

configCategories() {
    # Delete all categories
    xmlstarlet ed --inplace --pf -d "/log4j:configuration/category" $MS_HOME/conf/log4j.xml

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
                -s "/log4j:configuration" -t elem -n "category" -v "" \
                -s "/log4j:configuration/category[last()]" -t attr -n "name" -v "$category_name" \
                -s "/log4j:configuration/category[last()]" -t elem -n "priority" -v "" \
                -s "/log4j:configuration/category[last()]/priority" -t attr -n "value" -v "$category_threshold" \
                $MS_HOME/conf/log4j.xml
        done <<< "$categories"
    fi
}

formatFile() {
    local log_file=$MS_HOME/conf/log4j.xml
    xmlstarlet fo --indent-spaces 4 $log_file > $log_file.bak
    mv -f $log_file.bak $log_file
}

configAppenders
configCategories
formatFile

