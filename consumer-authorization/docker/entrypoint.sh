#!/bin/sh
set -e

# Check if /app/config_temp exists and is not empty
if [ -d "/app/config_temp" ] && [ "$(ls -A /app/config_temp)" ]; then
	# Copy from /config_temp to /config and remove /config_temp
    cp -r /app/config_temp/* /app/config/
    rm -rf /app/config_temp
fi

# Run
exec java -jar /app/arrowhead-consumer-authorization-5.0.0.jar -Dlog4j.configurationFile=/app/config/log4j2.xml