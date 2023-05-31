#!/bin/bash

GITLAB_INTERNAL="$GITLAB_HOST:$GITLAB_PORT"

REGISTRATION_TOKEN=$( \
  curl -s -k --request POST --header "Authorization: Bearer $GITLAB_ACCESS_TOKEN" "$GITLAB_INTERNAL/api/graphql" \
  -H 'content-type: application/json' -H "x-csrf-token: $GITLAB_ACCESS_TOKEN" \
  --data-raw '[{"operationName":"runnersRegistrationTokenReset","variables":{"input":{"type":"INSTANCE_TYPE"}},"query":"mutation runnersRegistrationTokenReset($input: RunnersRegistrationTokenResetInput!) {\n  runnersRegistrationTokenReset(input: $input) {\n    token\n    errors\n    __typename\n  }\n}\n"}]' \
  | grep -Po '"token":*\K"[^"]*"' \
  | tr -d '""' \
)

gitlab-runner register \
  --non-interactive \
  --executor "docker" \
  --docker-image alpine:latest \
  --url "$GITLAB_INTERNAL" \
  --registration-token "$REGISTRATION_TOKEN" \
  --description "docker-runner" \
  --maintenance-note "Just a random local test runner" \
  --tag-list "docker,artemis" \
  --run-untagged="true" \
  --locked="false" \
  --access-level="not_protected" \
  --docker-network-mode artemis
  # --clone-url http://gateway.docker.internal:80 \