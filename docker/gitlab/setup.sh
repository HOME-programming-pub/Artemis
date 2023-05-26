#!/bin/bash

GITLAB_API_URL="https://localhost/api/"

# An Array of the Values which are used to create a new Access token via script instead of the GUI
export map=$( printf "%s" \
  "User.find_by_username('$GITLAB_ROOT_USER').personal_access_tokens.create(scopes: [:api, :read_user, :read_api, :read_repository, :write_repository, :sudo], name: 'Artemis Admin Token'); " \
  "token.set_token('$GITLAB_ACCESS_TOKEN'); " \
  "token.save!" \
)

# Create the Admin Access Token
gitlab-rails runner "token = $map"

sleep 10

# Allow outbound requests to local network
 bool=$(curl -s -k --request PUT --header "Authorization: Bearer $GITLAB_ACCESS_TOKEN" "$GITLAB_API_URL/v4/application/settings?allow_local_requests_from_hooks_and_services=true&allow_local_requests_from_web_hooks_and_services=true&allow_local_requests_from_system_hooks=true" | jq -r .allow_local_requests_from_web_hooks_and_services)

if [ "true" != $bool ] ; then
    echo "Failed to allow outbound requests to local network. Go to $GITLAB_HOST:$GITLAB_PORT/admin/application_settings/network → Outbound requests and enable it."
    exit 1
fi

# Set and Get Access Token for Artemis
ARTEMIS_ACCESS_TOKEN=$(curl -s -k --request POST --header "Authorization: Bearer $GITLAB_ACCESS_TOKEN" "$GITLAB_API_URL/v4/users/1/personal_access_tokens" --data "name=Artemis" --data "scopes[]=api,read_user,read_api,read_repository,write_repository,sudo" | jq -r .token)
echo "ARTEMIS_ACCESS_TOKEN=\"$ARTEMIS_ACCESS_TOKEN\""
