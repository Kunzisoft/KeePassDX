fastlane documentation
================
# Installation

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew cask install fastlane`

# Available Actions
## Android
```
fastlane tests
```
Runs all the tests

```
fastlane build_beta_google_free storefile:"" storepass:"" keyalias:"" keypass:""
```
Build a new Beta version
```
fastlane deploy_beta_google_free
```
Deploy a new Beta version to the Google Play

----

More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
