# Releasing LinkGrab

## One-time setup
1. Generate the signing key (once): `./tools/gen-keys.sh`
2. Back up `~/.android/linkgrab-release.jks` and `keystore.properties` somewhere safe. Losing them means installed copies can never be updated.
3. Add the GitHub secrets (needs the `gh` CLI, run from the repo root):

```
gh secret set KEYSTORE_BASE64 --body "$(base64 -w0 ~/.android/linkgrab-release.jks)"
gh secret set KEYSTORE_PASSWORD --body "$(grep '^storePassword=' keystore.properties | cut -d= -f2-)"
gh secret set KEY_PASSWORD --body "$(grep '^keyPassword=' keystore.properties | cut -d= -f2-)"
gh secret set KEY_ALIAS --body "linkgrab"
```

## Every release
1. Raise `versionCode` (+1) and `versionName` in `app/build.gradle.kts`, commit and push.
2. Tag and push: `git tag v0.2.0 && git push origin v0.2.0`
3. GitHub Actions builds the signed APK and attaches `LinkGrab-v0.2.0.apk` to the release.

## Build locally
`./gradlew :app:assembleRelease` gives `app/build/outputs/apk/release/app-release.apk`, signed with your key.
`assembleReleaseTest` also uses your key when `keystore.properties` exists, and the debug key otherwise.

## Note
The first install signed with this key cannot update over a copy signed with an older (debug) key. Uninstall the old copy once. Saved videos and music stay in Movies/Music.
