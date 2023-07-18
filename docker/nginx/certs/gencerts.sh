#!/bin/bash

# hosts for which certificates shall be generated
hosts="artemis.hs-merseburg.de\
       gitlab-artemis.hs-merseburg.de\
       localhost\
       127.0.0.1\
       ::1\
       *.artemis\
       artemis"

# create output directory if it not exists (will be mapped to docker container)
mkdir -p ./gen

# create image from ./Dockerfile
docker build -t mkcert .

# run mkcert in temporary container to generate local CA root certifacte and local certificates for https 
docker run --rm -v "./gen:/gen" mkcert sh\
           -c "mkcert --install && cp /home/mkcertuser/.local/share/mkcert/* . && mkcert $hosts && ls -al"

# remove the docker image
docker rmi mkcert