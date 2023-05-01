# Docker Compose Configurations

An overview of all possible setups can be found in the docs at `docs/dev/setup.rst` in the section
`Alternative: Docker Compose Setup`.


## GitLab and GitLabCI

### Build and Start

```bash
docker-compose -f docker/gitlab-gitlabci.yml --env-file docker/gitlab/gitlab-gitlabci.env up --build -d
```

### Setup

```bash
docker-compose -f docker/gitlab-gitlabci.yml exec gitlab ./setup.sh
```

### Runner 

```bash
docker-compose -f docker/gitlab-gitlabci.yml exec gitlab-runner ./register.sh
```


## Artemis

### Enviornment Variables

```bash
yq -o=props '... comments=""' docker/artemis/application-local.yml | sed -E 's/([a-zA-Z][a-zA-Z0-9-]*\.[a-zA-Z][a-zA-Z0-9-]*(\.[a-zA-Z][a-zA-Z0-9-]*)*)/\U\1/g' | sed -E 's/\./_/g; s/-//g' | sed -E 's/=(.*)/="\1"/g' | tr -d ' ' > docker/artemis/config/prod-application-local.env.tmp
```

```bash
set -a && . docker/.env && set +a; envsubst < docker/artemis/config/prod-application-local.env.tmp > docker/artemis/config/prod-application-local.env && rm docker/artemis/config/prod-application-local.env.tmp
```

### Build

Before we can build Artemis, depending on your hosts resources you need to stop the running containers.

```bash
docker-compose -f docker/gitlab-gitlabci.yml stop
```

```bash
DOCKER_BUILDKIT=1 docker-compose -f docker/artemis-production.docker-compose.yml build --no-cache
```

## 