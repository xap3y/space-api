#!/bin/sh

# Spring boot application properties
export SPRING_SERVER_PORT=8012
export SPRING_SERVER_PROTOCOL=http
export SPRING_SERVER_BASEURL=192.168.100.100:$SPRING_SERVER_PORT
#export SPRING_SERVER_BASEURL=api.xap3y.tech

# Database
export SPRING_DATASOURCE_HOST=localhost
export SPRING_DATASOURCE_PORT=3306
export SPRING_DATASOURCE_SCHEMA=space
export SPRING_JPA_SHOW_SQL=true

# Security
export SPRING_SECURITY_USERNAME=xap3y
export SPRING_SECURITY_PASSWORD=admin

# File upload (For image and pastes)
# If using nginx, make sure to set client_max_body_size to the same value
export SPRING_MAX_FILE_SIZE=50MB

# Debug
export SPRING_DEBUG=false
export SPRING_ENV=dev
export CORS_TESTING_URL=http://127.0.0.1
export FRONTEND_URL=https://s.xap3y.tech

export SHORT_IMAGE_URL=https://i0.xap3y.tech
export SHORT_PASTE_URL=https://p0.xap3y.tech
export SHORT_SHORTENER_URL=https://r0.xap3y.tech

export NAMESPACE_TAG=local
