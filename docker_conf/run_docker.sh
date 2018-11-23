#!/usr/bin/env bash

docker_image_name="gmql-web"
docker_container_name="gmql-web"
port=12121

echo "[GMQL-WEB] Running ${docker_image_name} container at port ${port}"


# --rm      when the container stops, it is removed
# -d        run in detached mode
# --name    name of the container during execution
# --mount   mounting volume, called gmql_repository and bound to the /app/volume folder in the container
# -p        listening port of the GMQL service
docker run \
    --rm \
    -d \
    --name ${docker_container_name} \
    --mount source=gmql_repository,target=/app/volume,type=volume \
    -p ${port}:8000 \
    ${docker_image_name}