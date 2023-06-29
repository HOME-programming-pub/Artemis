# Docker Configurations

The following content explains the need for each step and should be read as a step-by-step explanation of how to create the necessary containers to run `Artemis` using `Docker`.

All commands are written to be executed from the root of this repository (`Artemis`) and not from the current path (`Artemis/docker`), this includes all file paths mentioned in the following text and need to be respected to work properly!

## 1. Prerequisite

Completing these steps successfully requires some patience and tools on the host machine to work as intended, and may change with future updates.

First, `Docker` and the `Docker Compose` plugin must be installed and running on the host machine.
To set this up in [Ubuntu](https://ubuntu.com/) I recommend reading this article for the installation and testing process: [Install the Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/). In some cases, the Docker Compose plugin is not up to date, so I recommend installing/upgrading it manually using the following article: [Install the plugin manually](https://docs.docker.com/compose/install/linux/#install-the-plugin-manually).

Docker Engine version `24.0.x+` and Docker Compose version `2.18.x+` are necessary and recommended.

Another step requires some external tools to convert a YAML file to another format.
This step is not absolutely necessary, but from experience it is easier than doing it the "standard" way.

The tools are
- yq (version 4.34.x+) - a command line YAML processor
- sed (version 4.8+) - a line-oriented text processor
- gettext (version 0.21+) - for `envsubst`, which substitutes the values of environment variables
- rsync (version 3.2.x+) - a utility for synchronising files between remote and local servers (optional) 

## 2. Environment files

There are two files that need to be created before we can run the containers for the first time.
These files contain sensitive data and will never be committed.

1. docker/.env
2. `docker/gitlab/gitlab-gitlabci.env`

The first file is used as a global configuration for all containers.
To create it, we can copy the `docker/example.env` file, which contains all the necessary variables with some default values and some values that need to be changed (marked with `<TEXT>`, where `TEXT` gives a rule if needed).

> Changing the values of variables not marked with `<TEXT>` must be considered dangerous and should only be changed if the cause is fully understood, as they may break the current setup if used differently.

The second file uses slightly different variables in case we want `GitLab` to run independently and on a different internal port. We need to make sure that the `GITLAB_ACCESS_TOKEN` and `GITLAB_ROOT_PASSWORD` are the same as those used in `docker/.env` to work properly.

> The idea behind repeating these variables is to keep things simple in case GitLab and Artemis are not running on the same machine, since with a higher user load, more CPU/RAM may be needed for GitLab and the GitLab runners.

### 2.1. Example

`ARTEMIS_ADMIN_PASSWORD="<artemis-password-50-chars-long>"` means that the variable `ARTEMIS_ADMIN_PASSWORD` should be a password with a maximum length of 50 characters. 

Make sure you are __NOT__ using any `$` characters as they can/will be interpreted as another environment variable and may cause problems.

## 3. Certificates

All services communicate mainly via HTTPS and therefore need the correct certificates.
It is possible to use self-signed certificates as explained in the Artemis documentation, but each service would need to be modified to allow self-signed certificates, which is a pain to get right.

If possible, we should use real certificates, which must contain `artemis.hs-mersebrg.de` and `gitlab.artemis.hs-merseburg.de`.

Once these certificates have been obtained, they need to be placed in `/etc/ssl/`.

The files and their paths must be as follows
- artemis_hs-merseburg_de.pem - `/etc/ssl/certs_genh/artemis_hs-merseburg_de.pem`
- artemis.hs-merseburg.de-key.pem - `/etc/ssl/artemis.hs-merseburg.de-key.pem`

## 4. GitLab and GitLab CI (Runners)

In this section we will create and configure the containers for GitLab and GitLab Runners.
The reason we need to start them first is that in order to access the GitLab API from Artemis, an access token needs to be generated.
And Artemis will run some configurations on startup that require this token, so we need to create this container before we can run the others.

### 4.1. Building and running

The following command will start the two containers with the configuration from `docker/gitlab-gitlabci.yml`.
The `--build` flag is needed for the `gitlab` service because it uses a modified image from the dockerfile in `docker/gitlab/Dockerfile`:

```bash
docker-compose -f docker/gitlab-gitlabci.yml up --build -d
```

This command may look like it is stuck, but it is not (if it does not take more than 5 minutes to complete) because it is waiting for the `GitLab` service to be `healthy`, which means it is configured and ready to use.

We need to wait for it to finish as the next step depends on it.

### 4.2. Setup

We now need to create the access token for Artemis, in the Artemis documentation there is an example of how to do this manually, but it involves many steps and the need to access the website.
To make it easier, I have prepared a script to run that automatically performs the necessary steps and returns the access token to the terminal.

The script can be found in `docker/gitlab/setup.sh` and should be run as follows
```bash
docker-compose -f docker/gitlab-gitlabci.yml exec gitlab ./setup.sh
```

This command may also take some time to run (about 30 seconds) and should return something like the following

```
ARTEMIS_ACCESS_TOKEN="glpat-..."
```

This line can be copied from a terminal and added to the end of the `docker/.env` file created earlier.

GitLab is now configured and we can proceed with launching Artemis.

## 5. Artemis

Before starting the container, we need to create some additional environment variables, as described in the next section.

### 5.1 Environment Variables

Artemis used YAML files to configure different parts, but once the `.WAR` file is built it is hard to change some configurations if needed.
To make it easier, Spring also offers the option to provide these variables as environment variables.
To take advantage of the simplicity of the YAML format, we need a little hack to convert them to environment variables.

First, we can edit the template configuration in `docker/artemis/application-local.yml`, which does not need to be changed, but can be if needed.
If we want to use environment variables in it, to protect them from being leaked to a repository, they need to be used as such (`$VARIABLE_NAME`).

And can be added to the `docker/.env` file (when adding a new variable, it is always helpful to add it to the `example.env` file as well).

Once we are done configuring the file, we need to convert it, which requires the tools mentioned in the `Prerequisites` section.

```bash
yq -o=props '... comments=""' docker/artemis/application-local.yml | sed 's/^\([a-zA-Z0-9\.\-]*\) = \(. *\)/\U\1\E="\2"/g' | sed -e ':a' -e 's/^\([^=]*\)\-/\1/;t a' | sed -e ':a' -e 's/^\([^=]*\)\./\1_/;t a' > docker/artemis/config/prod-application-local.env.tmp
```

This command converts the YAML format into the environment variable format, first we convert the nested YAML into concatenated values.
Then we convert the YAML properties to uppercase, removing any dashes in the names, and finally we replace the dots with underscores, but only for YAML properties.

Now we need to replace the placeholder variables with the environment variables from `docker/.env` using

```bash
set -a && . docker/.env && set +a; envsubst < docker/artemis/config/prod-application-local.env.tmp > docker/artemis/config/prod-application-local.env && rm docker/artemis/config/prod-application-local.env.tmp
```

This command exports the environment variables defined in `docker/.env`, with `envsubst` we search for the placeholder variables, if we find a matching environment variable then we replace it with its value and then we save everything in `docker/artemis/config/prod-application-local.env`.

### 5.2. Building

This is the only step that needs to be done each time a new change is made and should be used by the production server.

As building the project on the production server sounds like something that should not be done, we should build the `.WAR` file on another host (development machine)
and then push it to the machine running the production services.

> All commands in this section are for the development machine and __NOT__ for the production host!

> If we run this configuration locally, we can edit `docker/artmis.yml` and change the build argument `WAR_FILE_STAGE` to `"builder"` without having to build separately and then send the file anywhere!

To build the `.WAR` file, use the following command
```bash
./gradlew -Pprod -Pwar clean bootWar --no-daemon --max-workers=2
```

The options: `--no-daemon` `--max-workers=2` are not required, but my machine sometimes ran out of memory while running the build script, and these options helped me to build without problems.

If the build was successful, the `.WAR` file is created in `build/libs/` and is named Artemis + version + `.war`.

To use this new war file, we need to send it to the host using SSH.

There are two ways to do this, if its the first time we need to create a directory called `build/libs` in the root of the Artemis repository and use

```bash
scp build/libs/*.war root@artemis.hs-merseburg.de:/root/Artemis/build/libs/Artemis.war
```

Or we can use `rsync`, where we do not have to worry about the existing directory:

```bash
rsync -r build/libs/*.war root@artemis.hs-merseburg.de:/root/Artemis/build/libs/Artemis.war
```

> The user and path `root` are the current configuration for the host, but may change in the future.

### 5.3. Building the container

Once the `.WAR` file has been uploaded to the host, we can build the Artemis container using

```bash
DOCKER_BUILDKIT=1 docker-compose -f docker/artemis-prod-mysql.yml build --no-cache artemis
```

This container needs build arguments to build the right thing, but this option is not available in older Docker engines, which is why we need a relatively new version of the Docker engine.

It is also not a default option, which is why we need to enable the `DOCKER_BUILDKIT`, at least for the version I was using, maybe in a newer version this is possible by default.

### 5.4. Starting the services

Now we can finally start everything else we need.

We just need to make sure that the GitLab services are running before Artemis.

```bash
DOCKER_BUILDKIT=1 docker-compose -f docker/artemis-prod-mysql.yml up -d
```

## 6. GitLab runner

Before we can run our first automated tests using GitLab runners, we need to start at least one runner.

The following command creates a runner with some specific configurations set in `docker/gitlab/runner.sh

```bash
docker-compose -f docker/gitlab-gitlabci.yml exec gitlab-runner ./register.sh
```

This command creates a register token for a runner and uses it to start a new runner.

To create another/more runners, just run the command multiple times, depending on how many you need.

> Two runners means that a maximum of two exercises can be tested at the same time, if there are more than two the others will be queued and run when one runner has finished its job.

## 7. Done

Now everything should be running and working.

To test if everything is working, check out [Artemis](https://artemis.hs-merseburg.de) or [GitLab](https://gitlab.artemis.hs-merseburg.de).

(You may need to edit your `hosts` file to redirect artemis (artemis.hs-merseburg.de) and gitlab (gitlab.artemis.hs-merseburg.de) to localhost or the IP of the NGINX container if you are testing locally with this setup).