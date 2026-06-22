#!/bin/sh
set -e

: "${API_UPSTREAM:=http://localhost:8080}"
: "${PORT:=80}"

envsubst '${API_UPSTREAM}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf
sed -i "s/listen 80;/listen ${PORT};/" /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
