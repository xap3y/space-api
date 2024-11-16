#!/bin/sh

source ./vars.sh
source ./common.sh

PASTE_UPLOAD_PATH='/v1/paste/create'
PASTE_GET_PATH='/v1/paste/get'

TEXT=$(xclip -o)

RESPONSE_PASTE_ID_PATH='.uniqueId'

if [ -z "$TEXT" ]; then
    TEXT=$(zenity --text-info --title="Enter text to paste" --editable)
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

echo "BASE_URL: $BASE_URL$PASTE_UPLOAD_PATH" >> $LOG_FILE

RESPONSE=$(curl --silent --location "$BASE_URL$PASTE_UPLOAD_PATH" \
 -X POST \
 --form "body=$(jq -nc --arg text "$TEXT" '{"text": $text}');type=application/json" \
 --header "X-API-Key: $API_KEY")

echo "RESPONSE: $RESPONSE" >> $LOG_FILE

STATUS=$(echo "$RESPONSE" | jq -r .error)

if [ -z "$STATUS" ] || [ "$STATUS" != "false" ]; then
    zenity --error --text="Failed to upload paste:\n$ERROR"
    exit 1
else 
    echo "$(date) [$STATUS] - $RESPONSE" >> $LOG_FILE
fi

notify-send "Paste created successfully!"

LINK="$BASE_URL$PASTE_GET_PATH/$(echo "$RESPONSE" | jq -r $RESPONSE_PASTE_ID_PATH)?raw=true"

echo "$(date) CREATED -> $LINK" >> $LOG_FILE

paplay /usr/share/sounds/freedesktop/stereo/message.oga

TO_COPY=$(echo -n "$LINK" | head -1)
echo "To copy: $TO_COPY" >> $LOG_FILE
echo -n "$TO_COPY" | xclip -selection c