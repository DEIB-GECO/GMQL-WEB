#!/usr/bin/env bash

docker_image_name=${1:-gmql-web}
echo "Building docker image with name ${docker_image_name}"


docker build . -t ${docker_image_name}