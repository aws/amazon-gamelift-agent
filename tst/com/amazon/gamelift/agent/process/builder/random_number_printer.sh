#!/bin/bash
# This script generates a bunch of random numbers
# Usage: sh random_number_printer.sh -c 10000
while getopts c: flag
do
  case "${flag}" in
    c) count=${OPTARG};;
  esac
done

RANDOM=$$
for i in `seq $count`
do
  echo $RANDOM
done