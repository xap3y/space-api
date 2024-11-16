#!/bin/sh

# Spring boot application properties
export SPRING_SERVER_PORT=8012
export SPRING_SERVER_PROTOCOL=http
export SPRING_SERVER_BASEURL=127.0.0.1:$SPRING_SERVER_PORT

# Database
export SPRING_DATASOURCE_HOST=localhost
export SPRING_DATASOURCE_PORT=3306
export SPRING_DATASOURCE_USERNAME=admin
export SPRING_DATASOURCE_PASSWORD=admin
export SPRING_DATASOURCE_SCHEMA=space
export SPRING_JPA_SHOW_SQL=true

# Security
export SPRING_SECURITY_USERNAME=xap3y
export SPRING_SECURITY_PASSWORD=admin

# File upload (For image and pastes)
export SPRING_MAX_FILE_SIZE=8MB

# Debug
export SPRING_DEBUG=true