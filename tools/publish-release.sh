#!/usr/bin/env bash
# publish-release.sh: build the signed APK and publish it as a GitHub release, so the website's
# download button stops returning 404. Run it from anywhere inside the LinkGrab repo.
#
#   tools/publish-release.sh                      publish to the repo this folder pushes to
#   tools/publish-release.sh --make-public        also make a private repo public (after a secrets scan)
#   tools/publish-release.sh --repo OWNER/NAME    publish the APK to a different, public repo
#   tools/publish-release.sh --skip-build         reuse the APK from the last build
#   tools/publish-release.sh --allow-dirty        publish even with uncommitted changes
set -euo pipefail

REPO=""
MAKE_PUBLIC=0
SKIP_BUILD=0
ALLOW_DIRTY=0
while [ $# -gt 0 ]; do
  case "$1" in
    --make-public) MAKE_PUBLIC=1 ;;
    --skip-build) SKIP_BUILD=1 ;;
    --allow-dirty) ALLOW_DIRTY=1 ;;
    --repo) REPO="${2:-}"; shift ;;
    -h|--help) sed -n '2,9p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1"; exit 2 ;;
  esac
  shift
done

fail() { echo "ERROR: $*" >&2; exit 1; }

# Run a command up to 4 times: home Wi-Fi to GitHub drops now and then.
retry() {
  local n=0
  until "$@"; do
    n=$((n + 1))
    [ "$n" -ge 4 ] && return 1
    echo "Network problem, retrying in 15 seconds ($n of 3)..."
    sleep 15
  done
}

cd "$(git rev-parse --show-toplevel)"

command -v gh >/dev/null || fail "GitHub CLI not installed. Install it with: sudo pacman -S github-cli"
gh auth status >/dev/null 2>&1 || fail "Not logged in to GitHub. Run: gh auth login"

curl -sI --max-time 15 https://github.com >/dev/null 2>&1 || fail "Cannot reach github.com right now. Check your internet (try: curl -I https://github.com) and run this again."

ORIGIN_REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)"
[ -n "$REPO" ] || REPO="$ORIGIN_REPO"
[ -n "$REPO" ] || fail "Could not work out the GitHub repo. Pass it with --repo OWNER/NAME"

VERSION="$(grep -m1 'versionName' app/build.gradle.kts | sed -E 's/.*"([^"]+)".*/\1/')"
CODE="$(grep -m1 'versionCode' app/build.gradle.kts | sed -E 's/[^0-9]*([0-9]+).*/\1/')"
[ -n "$VERSION" ] || fail "Could not read versionName from app/build.gradle.kts"
TAG="v$VERSION"
echo "Release $TAG (versionCode $CODE) -> $REPO"

[ -f keystore.properties ] || fail "keystore.properties not found. Run tools/gen-keys.sh first, so the APK is signed with your real key (an APK signed with the debug key can never be updated later)."

# ---- 1. Visibility: visitors can only download from a PUBLIC repo -------------------------------
VIS="$(gh repo view "$REPO" --json visibility -q .visibility)"
echo "Repo visibility: $VIS"
if [ "$VIS" != "PUBLIC" ]; then
  if [ "$MAKE_PUBLIC" -ne 1 ]; then
    cat <<MSG

$REPO is PRIVATE, so everyone who visits your website gets a 404 on the download button.
Pick one:

  A) Make this repo public (your source code and history become public):
       tools/publish-release.sh --make-public

  B) Keep the source private and publish only the APK to a separate public repo:
       gh repo create $(echo "$REPO" | cut -d/ -f1)/linkgrab-releases --public --add-readme
       tools/publish-release.sh --repo $(echo "$REPO" | cut -d/ -f1)/linkgrab-releases
MSG
    exit 1
  fi
  [ "$REPO" = "$ORIGIN_REPO" ] || fail "--make-public only applies to the repo this folder pushes to."

  echo "Scanning git history for secrets before making the repo public..."
  BAD_FILES="$(git log --all --name-only --pretty=format: | sort -u | grep -Ei '(\.jks|\.keystore|keystore\.properties|\.env|id_rsa|\.pem)$' || true)"
  BAD_TEXT="$(git log --all -p 2>/dev/null | grep -E 'ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY|storePassword=[A-Za-z0-9+/]{16,}' | head -3 || true)"
  if [ -n "$BAD_FILES$BAD_TEXT" ]; then
    echo "Possible secrets found in the git history:"
    [ -z "$BAD_FILES" ] || echo "$BAD_FILES"
    [ -z "$BAD_TEXT" ] || echo "$BAD_TEXT" | cut -c1-80
    fail "Not making the repo public. Remove them from history first, or use option B."
  fi
  echo "Scan clean."

  read -r -p "Make $REPO PUBLIC now? Anyone can read the code and its history. [y/N] " ans
  [ "$ans" = "y" ] || fail "Cancelled."
  gh repo edit "$REPO" --visibility public --accept-visibility-change-consequences
  echo "$REPO is now public."
fi

# The release tag points at a commit, so the APK must come from committed code.
if [ "$ALLOW_DIRTY" -ne 1 ] && [ -n "$(git status --porcelain)" ]; then
  echo "You have uncommitted changes:"
  git status --short | head -15
  fail "Commit them first (git add -A && git commit -m \"Release $TAG\"), or run again with --allow-dirty."
fi

# ---- 2. Build the signed APK --------------------------------------------------------------------
if [ -d /usr/lib/jvm/java-17-openjdk ]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
fi
APK="app/build/outputs/apk/releaseTest/app-releaseTest.apk"
if [ "$SKIP_BUILD" -eq 1 ] && [ -f "$APK" ]; then
  echo "Reusing the APK from the last build."
else
  echo "Building the signed APK (this takes a while)..."
  ./gradlew :app:assembleReleaseTest
fi
[ -f "$APK" ] || fail "APK not found at $APK"

APKSIGNER="$(ls -d "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)"
if [ -n "$APKSIGNER" ]; then
  CERT="$("$APKSIGNER" verify --print-certs "$APK" 2>&1 || true)"
  if echo "$CERT" | grep -q "Android Debug"; then
    fail "The APK is signed with the DEBUG key. Check that keystore.properties is valid."
  fi
  echo "$CERT" | head -2
fi

OUT="build/release"
mkdir -p "$OUT"
ASSET="$OUT/LinkGrab-$TAG.apk"
cp "$APK" "$ASSET"
SHA="$(sha256sum "$ASSET" | cut -d' ' -f1)"
echo "SHA-256: $SHA"

# ---- 3. Publish ------------------------------------------------------------------------------------
read -r -p "Publish $TAG to $REPO now? [y/N] " go
[ "$go" = "y" ] || fail "Cancelled. The APK is at $ASSET"

TARGET_ARGS=()
if [ "$REPO" = "$ORIGIN_REPO" ]; then
  BRANCH="$(git rev-parse --abbrev-ref HEAD)"
  retry git push origin "$BRANCH" || fail "Could not push to GitHub after 4 tries. Check your internet, then run again with --skip-build."
  TARGET_ARGS=(--target "$BRANCH")
fi

NOTES="Signed APK for Android 8.0 or newer. SHA-256: $SHA"
if gh release view "$TAG" -R "$REPO" >/dev/null 2>&1; then
  echo "Release $TAG already exists; replacing its APK."
  retry gh release upload "$TAG" "$ASSET" --clobber -R "$REPO" || fail "Upload failed after 4 tries. Run again with --skip-build."
else
  retry gh release create "$TAG" "$ASSET" -R "$REPO" --title "LinkGrab $VERSION" --notes "$NOTES" "${TARGET_ARGS[@]}" || fail "Release creation failed after 4 tries. Run again with --skip-build."
fi

# ---- 4. Check what visitors will actually see -------------------------------------------------------
echo "Checking the public download, the same way your website does..."
sleep 3
API="https://api.github.com/repos/$REPO/releases/latest"
API_CODE="$(curl -s -o /dev/null -w '%{http_code}' "$API" || true)"
APK_URL="$(curl -s "$API" | python3 -c "import sys,json; d=json.load(sys.stdin); a=[x for x in d.get('assets',[]) if x['name'].endswith('.apk')]; print(a[0]['browser_download_url'] if a else '')" 2>/dev/null || true)"
DL_CODE="000"
[ -z "$APK_URL" ] || DL_CODE="$(curl -sIL -o /dev/null -w '%{http_code}' "$APK_URL" || true)"

echo "  releases/latest API : $API_CODE (want 200)"
echo "  APK download link   : $DL_CODE (want 200)"
echo "  APK URL             : ${APK_URL:-none}"
if [ "$API_CODE" = "200" ] && [ "$DL_CODE" = "200" ]; then
  echo "Done. Your website's download button now works."
else
  fail "The public check failed. If the repo is private the check can never pass; run this script again after choosing option A or B above."
fi
