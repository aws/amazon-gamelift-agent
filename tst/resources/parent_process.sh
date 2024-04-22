#!/bin/bash
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# This script creates a child process and then sleeps for 10 seconds
# Usage: sh parent_process.sh -p /full/path/to/child_process.sh
while getopts p: flag
do
 case "${flag}" in
   p) child_script_path=${OPTARG};;
 esac
done

echo "Parent process created, starting child process."
sh "$(child_script_path)" &
echo "Parent process sleeping..."
sleep 10
echo "Parent process timed out!"