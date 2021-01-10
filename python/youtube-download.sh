#!/bin/bash
queue="https://sqs.us-east-1.amazonaws.com/711575676778/cloudActionQueue"
tmp_msg_file="/tmp/message"
default_instance="i-0f2bb8a4ee9b05bf0"
instance_id=$(wget -q -O - http://169.254.169.254/latest/meta-data/instance-id || echo "$default_instance")
wait_seconds=$1
if [ -z "$wait_seconds" ]; then
  wait_seconds=1800
fi
last_message_time=$(date +%s)
arg_region="us-east-1"
output_template='%(title)s-%(id)s-%(duration)ss.%(ext)s'

echo "Job started, max wait $wait_seconds seconds"

function adjust_volume() {
  for f in "$1"/*.mp3; do
    echo "Adjusting volume for $f..."
    /home/ec2-user/scripts/volume.py --update -d "$f"
  done
  echo "Volume adjusting done."
}

while true; do
  echo "Polling SQS messages... one at a time"
  aws sqs receive-message --wait-time-seconds 20 --queue-url "$queue" --max-number-of-messages 1 --region "$arg_region" >"$tmp_msg_file"
  cat $tmp_msg_file
  body=$(jq -r '.Messages[0].Body' $tmp_msg_file)
  target_link="$body"
  link_field=$(echo "$body" | jq -r '.Link | select (.!=null)')
  extra_args=$(echo "$body" | jq -r '.Args | select (.!=null)')
  if [ -n "$link_field" ]; then
    target_link="$link_field"
  fi
  target_folder=$(echo "$body" | jq -r '.Folder| select (.!=null)')
  if [ -z "$target_folder" ]; then
    target_folder="TechTalks"
  fi

  if [ -z "$target_link" ]; then
    cur_time=$(date +%s)
    seconds_since_last=$((cur_time - last_message_time))
    if [ "$seconds_since_last" -gt "$wait_seconds" ]; then
      echo "No message for more than $wait_seconds seconds, kill instance now"
      break
    else
      echo "No message for $seconds_since_last seconds, wait for another round..."
      continue
    fi
  fi
  last_message_time=$(date +%s)
  tmp_output="/tmp/$(echo -n "$target_link" | md5sum | awk '{print $1}')"
  echo "Downloading and transforming $target_link with youtube-dl..."
  /usr/local/bin/youtube-dl "$target_link" -o "$tmp_output/$output_template" $extra_args &&
    adjust_volume "$tmp_output" &&
    aws s3 cp "$tmp_output" "s3://purelandmusic/$target_folder/" --recursive --region "$arg_region"
  if [ "$?" -eq "0" ]; then
    receipt=$(jq -r '.Messages[0].ReceiptHandle' $tmp_msg_file)
    aws sqs delete-message --queue-url "$queue" --receipt-handle "$receipt" --region "$arg_region"
    rm -rf "$tmp_output"
  fi
done

echo "Stopping instance in 30 seconds ..."
sleep 30
aws ec2 stop-instances --instance-ids "$instance_id" --region "$arg_region"
