#!/bin/sh

# Spring boot application properties
export SPRING_SERVER_PORT=8012
export SPRING_SERVER_PROTOCOL=http
export SPRING_SERVER_BASEURL=192.168.100.100:$SPRING_SERVER_PORT
#export SPRING_SERVER_BASEURL=api.xap3y.tech

# Database
export SPRING_DATASOURCE_PREFIX=mariadb
export SPRING_DATASOURCE_HOST=localhost
export SPRING_DATASOURCE_PORT=3306
export SPRING_DATASOURCE_SCHEMA=space
export SPRING_JPA_SHOW_SQL=FALSE

# Security
export SPRING_SECURITY_USERNAME=xap3y
export SPRING_SECURITY_PASSWORD=admin

# File upload (For image and pastes)
# If using nginx, make sure to set client_max_body_size to the same value
# If using S3 or R2, ignore this
export SPRING_MAX_FILE_SIZE=5000MB

# Debug
export SPRING_DEBUG=false
export SPRING_ENV=dev
export CORS_TESTING_URL=http://127.0.0.1
export FRONTEND_URL=http://localhost:3000

# Discord bot
export USE_DISCORD_WEBHOOK=false
export USE_DISCORD_BOT=true
export DISCORD_BOT_ID=1367593639152455792
export REMOTE_DISCORD_BOT_GUILD_ID=1218647784119599255
export REMOTE_DISCORD_BOT_CHANNEL_ID=1383182173670215766

# Telegram bot
export USE_TELEGRAM_BOT=false
export USE_TELEGRAM_VERIFY_BOT=true

# Short urls
export SHORT_IMAGE_URL=https://i1.xap3y.tech
export SHORT_PASTE_URL=https://p1.xap3y.tech
export SHORT_SHORTENER_URL=https://r1.xap3y.tech

# TODO
export NAMESPACE_TAG=local_xap

export USE_DISCORD_BOT_A=false

export LOKI_URL=http://localhost:3100/loki/api/v1/push