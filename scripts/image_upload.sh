#!/bin/sh

source ./vars.sh
source ./common.sh

SCREENSHOT_PATH=$1

IMAGE_UPLOAD_PATH='/v1/image/upload'
IMAGE_GET_PATH='/v1/image/get'
RESPONSE_IMAGE_ID_PATH='.uniqueId'

if [ -z "$SCREENSHOT_PATH" ]; then
    SCREENSHOT_PATH=$(zenity --file-selection --title="Select a file to upload")
fi

echo "Checking status.."
STATUS_CODE=$(check_status "$BASE_URL")
if [ "$STATUS_CODE" -ne 200 ]; then
    BASE_URL="$BASE_URL_FALLBACK"
fi

echo "Checking status.."
STATUS_CODE=$(check_status "$BASE_URL")
if [ "$STATUS_CODE" -ne 200 ]; then
    zenity --error --text="APIs are down!"
    exit 1
fi

RESPONSE=$(curl --silent --location "$BASE_URL$IMAGE_UPLOAD_PATH" -X POST -F "file=@$SCREENSHOT_PATH" --header "X-API-Key: $API_KEY")
echo "RESPONSE: $RESPONSE" >> $LOG_FILE

STATUS=$(echo "$RESPONSE" | jq -r .error)

if [ -z "$STATUS" ] || [ "$STATUS" != "false" ]; then
    INTERNET=$(wget "$BASE_URL/status")
    if [ -z "$INTERNET" ]; then
        zenity --error --text="Host is down!:\n$BASE_URL"
        exit 1
    fi
    ERROR=$(echo "$RESPONSE" | jq -r .message)
    zenity --error --text="Failed to upload screenshot:\n$ERROR"
    exit 1
else 
    echo "$(date) [$STATUS] - $RESPONSE" >> $LOG_FILE
fi

notify-send "Image uploaded successfully!"

LINK="$BASE_URL$IMAGE_GET_PATH/$(echo "$RESPONSE" | jq -r $RESPONSE_IMAGE_ID_PATH)"

echo "$(date) UPLOADED -> $LINK" >> $LOG_FILE

paplay /usr/share/sounds/freedesktop/stereo/message.oga
echo -n "$LINK" | head -1 | xclip -selection c
