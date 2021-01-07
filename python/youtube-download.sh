#!/bin/bash
queue="https://sqs.us-east-1.amazonaws.com/711575676778/cloudActionQueue"
tmp_msg_file="/tmp/message"
default_instance="i-0f2bb8a4ee9b05bf0"
instance_id=$(wget -q -O - http://169.254.169.254/latest/meta-data/instance-id || echo "$default_instance")
wait_seconds=1800
last_message_time=$(date +%s)
arg_region="us-east-1"

while true; do
  echo "Polling SQS messages... one at a time"
  aws sqs receive-message --wait-time-seconds 20 --queue-url "$queue" --max-number-of-messages 1 --region "$arg_region" >"$tmp_msg_file"
  cat $tmp_msg_file
  body=$(cat $tmp_msg_file | jq -r '.Messages[0].Body')
  target_link="$body"
  link_field=$(echo "$body" | jq -r '.Link')
  if [ ! -z "$link_field" ]; then
      target_link="$link_field"
  fi
  target_folder=$(echo "$body" | jq -r '.Folder')
  if [ ! -z "$target_folder" ]; then
      target_folder="TechTalks"
  fi

  if [ -z "$target_link" ]; then
    cur_time=$(date +%s)
    seconds_since_last=$(expr $cur_time - $last_message_time)
    if [ "$seconds_since_last" -gt "$wait_seconds" ]; then
      echo "No message for more than $wait_seconds seconds, kill instance now"
      break
    else
      echo "No message for $seconds_since_last seconds, wait for another round..."
      continue
    fi
  fi
  last_message_time=$(date +%s)
  tmp_output="/tmp/$(date +%s)"
  /usr/local/bin/youtube-dl "$target_link" -o "$tmp_output"'/%(title)s-%(id)s.%(ext)s' && \
          /home/ec2-user/scripts/volume.py --update -d "$tmp_output" && \
          aws s3 cp "$tmp_output" "s3://purelandmusic/$target_folder/" --recursive --region "$arg_region"
  if [ "$?" -eq "0" ]; then
    receipt=$(cat $tmp_msg_file | jq -r '.Messages[0].ReceiptHandle')
    aws sqs delete-message --queue-url "$queue" --receipt-handle "$receipt" --region "$arg_region"
  fi
  rm -rf $tmp_output
done

echo "Stopping instance in 30 seconds ..."
sleep 30
aws ec2 stop-instances --instance-ids "$instance_id" --region "$arg_region"
