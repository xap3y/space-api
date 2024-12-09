#!/bin/sh

# Spring boot application properties
export SPRING_SERVER_PORT=8013
export SPRING_SERVER_PROTOCOL=http
export SPRING_SERVER_BASEURL=127.0.0.1:$SPRING_SERVER_PORT

# Database
export SPRING_DATASOURCE_HOST=internal.2.db.xap3y.eu
export SPRING_DATASOURCE_PORT=3306
export SPRING_DATASOURCE_SCHEMA=space_test
export SPRING_JPA_SHOW_SQL=true

# Security
export SPRING_SECURITY_USERNAME=space
export SPRING_SECURITY_PASSWORD=test

# File upload (For image and pastes)
# If using nginx, make sure to set client_max_body_size to the same value
export SPRING_MAX_FILE_SIZE=50MB

# Debug
export SPRING_DEBUG=true
export SPRING_ENV=dev
#export CORS_TESTING_URL=http://127.0.0.1
export FRONTEND_URL=https://test.xap3y.space