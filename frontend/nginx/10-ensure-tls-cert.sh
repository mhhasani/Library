#!/bin/sh
# Runs from /docker-entrypoint.d before nginx starts.
# If no certificate was mounted at /etc/nginx/certs, generate a self-signed one so the
# service is never exposed over plain HTTP. Replace it with an organization-issued
# certificate by mounting tls.crt / tls.key into /etc/nginx/certs.
set -eu

CERT_DIR=/etc/nginx/certs
if [ -s "$CERT_DIR/tls.crt" ] && [ -s "$CERT_DIR/tls.key" ]; then
    exit 0
fi

mkdir -p "$CERT_DIR"
echo "10-ensure-tls-cert: no certificate mounted, generating a self-signed one" >&2
openssl req -x509 -nodes -newkey rsa:3072 -sha256 -days 825 \
    -subj "/CN=${TLS_COMMON_NAME:-library.local}" \
    -keyout "$CERT_DIR/tls.key" -out "$CERT_DIR/tls.crt" 2>/dev/null
chmod 600 "$CERT_DIR/tls.key"
