# Docker Compose Configurations

An overview of all possible setups can be found in the docs at `docs/dev/setup.rst` in the section
`Alternative: Docker Compose Setup`.

## Usefull note for future maintainers!

> HoMe Artemis ONLY

If the firewall rules get improved to allow docker network interfaces to access the internet (via the proxy), then the following changes NEED to be done.

Currently we export hosts to access containers from another, if we can use a custom network, this options is useless and can be replaced.
(Most of the replacements are already existent, they just need to be uncommented and the useless options can be deleted.)

### Files that are impaced:
> maybe a few more but those are the files where it is 100% needed to be changed.

 - artemis.yml
 - gitlab.yml
 - gitlabci.yml
 - mysql.yml
 - nginx.yml
 - gitlab/register.sh

## Certificates

To generate new Certificates yo can use the following command, lateron using a real certifacte should be better!
```bash
docker run --rm -v ${PWD}/docker/nginx/certs:/certs $(docker build -q docker/nginx/certs/) /certs/generate-certs.sh artemis-nginx artemis artemis.hs-merseburg.de gitlab.artemis gitlab.artemis.hs-merseburg.de localhost 127.0.0.1 ::1 `docker network inspect bridge --format='{{(index .IPAM.Config 0).Gateway}}'`
```

## GitLab and GitLabCI

### Build and Start

The following command starts GitLab and its Runner container which is needed to generate an Access Token for Artemis.
```bash
docker compose -f docker/gitlab-gitlabci.yml --env-file docker/.env up --build -d
```

### Setup

To generate the token, you can etiher do it via the GitLab API using your webbrowser, or use this setpscript, which uses the default configuration we need.
```bash
docker compose -f docker/gitlab-gitlabci.yml --env-file docker/gitlab/gitlab-gitlabci.env exec gitlab ./setup.sh
```

The copy and paste the output (`ARTEMIS_ACCESS_TOKEN`) into `docker/.env`.

## Artemis

### Enviornment Variables

Artemis has man differnt variables to configure it as we need.
Some of them need enviornment variables and some of the are preconfigured.
The template can be found at `dokcer/artemis/application-local.yml` and configured there.

This script takes the `YAML` file and creates an `.env` from it which can be used to easily change some variables after the build process. 
```bash
yq -o=props '... comments=""' docker/artemis/application-local.yml | sed -E 's/([a-zA-Z][a-zA-Z0-9-]*\.[a-zA-Z][a-zA-Z0-9-]*(\.[a-zA-Z][a-zA-Z0-9-]*)*)/\U\1/g' | sed -E 's/\./_/g; s/-//g' | sed -E 's/=(.*)/="\1"/g' | tr -d ' ' > docker/artemis/config/prod-application-local.env.tmp
```

Now we need to replace the placeholder varibales with our enviornment varibales from `docker/.env` and we are good to go. 
```bash
set -a && . docker/.env && set +a; envsubst < docker/artemis/config/prod-application-local.env.tmp > docker/artemis/config/prod-application-local.env && rm docker/artemis/config/prod-application-local.env.tmp
```

### Build

Before we can build Artemis, depending on your hosts resources you need to stop the running containers.

```bash
docker compose -f docker/gitlab-gitlabci.yml stop
```
#### Build Locally First

If you choosed to build locally and then use the reslting `WAR` file on the new host, then run the following commands locally (might be differnt depending on your operating system / distribtion).

```bash
sudo ./gradlew -Pprod -Pwar clean bootWar --no-daemon --max-workers=2
```

And to move it to the server:
`scp buil/libs/*.war <user>@<host>:/path/to/project/build/libs/Artemis.war`

```bash
scp build/libs/*.war root@artemis.hs-merseburg.de:/root/Artemis/build/libs/Artemis.war
```

Or

```bash
rsync -r build/libs/*.war root@artemis.hs-merseburg.de:/root/Artemis/build/libs/Artemis.war
```

### Continue Build

Now we can either use this repository to build artemis, or we can build artemis locally, `scp` it into `build/libs/Artemis.war`, which is the preconfigured way, to change it edit `docker/artemis.yml` and change the build argument `WAR_FILE_STAGE` to `builder`.
```bash
DOCKER_BUILDKIT=1 docker compose -f docker/artemis-prod-mysql.yml build --no-cache artemis
```

## Start

With everything build and configured we can now start GitLab and its Runner and once they are started up we can start the Database, MailServer, Artemis and NGINX.
```bash
docker compose -f docker/gitlab-gitlabci.yml --env-file docker/.env up -d

DOCKER_BUILDKIT=1 docker compose -f docker/artemis-prod-mysql.yml up -d
```

### Runner 

> This command needs nginx to be available, thats why we call it here!

We also need atleast one Runner Instance to run tests. 
You can run this command multiple times, to create multiple runners if needed. 
```bash
docker compose -f docker/gitlab-gitlabci.yml exec gitlab-runner ./register.sh
```
> You might need to run this twice, the first attempt might fail, but the second works.

## Done

To test if everything works check [Artemis](https://artemis.hs-merseburg.de) or [GitLab](https://gitlab.artemis.hs-merseburg.de).

(You might need to edit yor `hosts` file to redirect artemis and gitlab to localhost or the IP of the NGINX container.)

## Making everything ready

Once all services are running and tested, there is just one more step to make sure all services are started correctly after a reboot.
This step is neccessary because the order to start the service is really important and can not be changed.

Simply we need to all services:

```bash
docker stop `docker ps -a -q`
```

And then start them with:

```bash
cd docker && docker compose --env-file .env up -d && cd ..
```