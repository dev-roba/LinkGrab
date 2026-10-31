#!/usr/bin/env bash
set -euo pipefail

# gen-keys.sh: generate the LinkGrab release keystore. Run it ONCE, from anywhere.
# The keystore goes outside the repo; keystore.properties goes in the repo root (git-ignored).

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_PATH="${KEYSTORE_PATH:-${HOME}/.android/linkgrab-release.jks}"
if [ -n "${1:-}" ]; then
  KEYSTORE_PATH="$1"
fi
PROPS="$REPO_ROOT/keystore.properties"

if [ -e "$KEYSTORE_PATH" ] || [ -e "$PROPS" ]; then
  echo "A keystore or keystore.properties already exists:"
  [ -e "$KEYSTORE_PATH" ] && echo "  $KEYSTORE_PATH"
  [ -e "$PROPS" ] && echo "  $PROPS"
  echo "Refusing to overwrite. Overwriting would make existing installs impossible to update."
  exit 1
fi

mkdir -p "$(dirname "$KEYSTORE_PATH")"

# Random password (hex, so it is safe in shells and YAML). Never printed.
PASSWORD="$(openssl rand -hex 24)"

keytool -genkeypair \
  -keystore "$KEYSTORE_PATH" \
  -alias linkgrab \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -dname "CN=LinkGrab,OU=Personal,O=LinkGrab,L=Unknown,ST=Unknown,C=US" \
  -validity 10000 \
  -keyalg RSA \
  -keysize 4096 \
  -storetype JKS
chmod 600 "$KEYSTORE_PATH"

( umask 177
  cat > "$PROPS" <<PROPS_EOF
storeFile=$KEYSTORE_PATH
storePassword=$PASSWORD
keyAlias=linkgrab
keyPassword=$PASSWORD
PROPS_EOF
)

echo "========================================="
echo "Keystore:   $KEYSTORE_PATH"
echo "Properties: $PROPS (git-ignored)"
echo
echo "BACK UP BOTH NOW (password manager or encrypted storage)."
echo "If you lose them you can never update installed copies of the app."
echo "Never commit either file."
echo "========================================="
