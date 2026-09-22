#!/usr/bin/env bash
set -euo pipefail

# -------------------------------------------------------
# gen-keys.sh — Generate a release keystore for LinkGrab
# -------------------------------------------------------

# Keystore path: outside the repo by default, overridable via $KEYSTORE_PATH or first arg
KEYSTORE_PATH="${KEYSTORE_PATH:-${HOME}/.android/linkgrab-release.jks}"
if [ -n "${1:-}" ]; then
  KEYSTORE_PATH="$1"
fi

# Ensure the parent directory exists
KEYSTORE_DIR="$(dirname "$KEYSTORE_PATH")"
mkdir -p "$KEYSTORE_DIR"

# Generate a strong random password; NEVER print it to stdout
PASSWORD=$(openssl rand -base64 24)

# Generate the keystore with keytool
keytool -genkeypair \
  -keystore "$KEYSTORE_PATH" \
  -alias linkgrab \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -dn "CN=LinkGrab,OU=Personal,O=LinkGrab,L=Unknown,ST=Unknown,C=US" \
  -validity 10000 \
  -keyalg RSA \
  -keysize 4096 \
  -storetype JKS

# Write keystore.properties (git-ignored) — never echo the password
cat > /home/roba/LinkGrab/keystore.properties <<EOF
storeFile=$KEYSTORE_PATH
storePassword=$PASSWORD
keyAlias=linkgrab
keyPassword=$PASSWORD
EOF

# Secure the properties file
chmod 600 /home/roba/LinkGrab/keystore.properties

# Success message — DO NOT commit keystore.properties or the .jks
echo "========================================="
echo "Keystore generated at: $KEYSTORE_PATH"
echo "WARNING: Back up the keystore file and this password securely."
echo "Loss of either means you CANNOT update the app (new signature required)."
echo "Never commit keystore.properties or the .jks file to version control."
echo "========================================="