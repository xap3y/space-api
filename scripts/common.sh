#!/bin/sh

check_status() {
    local BASE_URL=$1 

    STATUS_CODE=$(curl --silent --output /dev/null --write-out "%{http_code}" "$BASE_URL/status")

    echo "$STATUS_CODE"
}

