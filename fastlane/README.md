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
fastlane android tests
```
Runs all the tests
### android build_beta_free
```
fastlane android build_beta_free storefile:"" storepass:"" keyalias:"" keypass:""
```
Build a new Free Beta version
### android build_beta_pro
```
fastlane android build_beta_pro storefile:"" storepass:"" keyalias:"" keypass:""
```
Build a new Pro Beta version
### android deploy_beta_google_free
```
fastlane android deploy_beta_google_free
```
Deploy a new Free Beta version to the Google Play
### android deploy_beta_google_pro
```
fastlane android deploy_beta_google_pro
```
Deploy a new Pro Beta version to the Google Play

----

More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
