#!/bin/sh

cd src/main/docker/cypress

# HOST_HOSTNAME not really necessary for shutdown but otherwise docker-compose complains
export HOST_HOSTNAME=$(hostname)
# show all running docker containers before the cleanup to detect issues
docker ps -a
# first kill ALL containers on the bamboo agent
docker container rm $(docker ps -a -q) || true
# then kill remaining project volumes and networks which should be easy removable as not bound to containers anymore
docker compose -f cypress-E2E-tests.yml down -v
# show all running docker containers after the cleanup to detect issues
docker ps -a
