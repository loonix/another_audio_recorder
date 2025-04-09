## 1.3.0

### Fix for AAC Recording Metering (Issue #15)
- AAC recording metering values now properly update during recording
- Fixed an issue where peakPower and averagePower remained at -120 in AAC format

#### Improvements

- Added proper implementation of updatePowers() method in AACRecordThread.java
- Calculation now uses all audio samples to determine true peak and RMS values
- Applied proper dB scale with iOS compatibility factor
- Better silent case detection

### Build fix for deprecated PluginRegistry.Registrar (cf587ee)

* Fix Build error caused by deprecated PluginRegistry.Registrar (thanks to @Coder-Manuel)

## 1.1.2
* Reverted some changes from 1.1.1

## 1.1.1 - Android Build Busted - DO NOT USE
Breaking changes:
* Android SDK version updated to 34
Commits:
* Fix to #10 - https://github.com/loonix/another_audio_recorder/pull/10


## 1.1.0
## What's Changed
* Fix related to Android 13 (API level 33) permissions by @mselmanyildirim in https://github.com/loonix/another_audio_recorder/pull/4
* fix: example to support flutter v2/3 changes by @loonix in https://github.com/loonix/another_audio_recorder/pull/8

## New Contributors
* @mselmanyildirim made their first contribution in https://github.com/loonix/another_audio_recorder/pull/4

**Full Changelog**: https://github.com/loonix/another_audio_recorder/compare/1.0.1...1.1.0

## 1.0.1
* Added null check to recording, thanks @electrooz
## 1.0.0+1
* Fix to Dart formatting requirements
## 1.0.0
* Fix to example IOS and Android builds
* Null Safety update
* @hnvn - Migrate to Android embedding v2 + Support AAC codec (https://github.com/rmbrone/flutter_audio_recorder/pull/52)
* @PerrchicK - error handling instead of app crash (https://github.com/rmbrone/flutter_audio_recorder/pull/45/files)
* Renamed to another_flutter_recorder

## 0.5.5
migrate to Android X.

## 0.5.4
* Remove default result call on permission call back for Android

## 0.5.3
* Fix Android sample rate issue
* Fix `Stop` API
* Fix metering API for Android and iOS

## 0.5.2
* Explicitly asking for WRITE_EXTERNAL_STORAGE permission as it's required in Android
* Updated documentation for Android Permission Section
* Add null check for temp file deletion

## 0.5.1
* Fix Android duration API
* Complete plugin public API doc

## 0.5.0
* Support AndroidX
* Update documentation

## 0.4.9
* Fixed minor issues for Android

## 0.4.8
* Fix version number in .podspec

## 0.4.7
* Remove extra pkg from lib
* Update example app

## 0.4.6
* Add swift version compiler code

## 0.4.5
* Update documentation of usage
* Specify Swift version in podspec

## 0.4.4
* fix another issue of iOS hasPermission;

## 0.4.3
* fix iOS hasPermission result issue;

## 0.4.2
* fix the Android's audio file path, now you can get it from *current* method

## 0.4.1

* Change Android dependencies version

## 0.4.0

* Supports Android

## 0.3.0

* Support Sample Rate.


## 0.2.2

* Correct iOS deployment target to 8.0.


## 0.2.1

* Simplify usage example for initialize recorder.

## 0.2.0

* Fix issue of init method.
* Fix issue of wav format recording initialization.
* audioFormat of AnotherAudioRecorder now is optional when instantiating.
* Overwrite file extension when there is conflicts between extension and audioFormat.
* Update Example app.
* Update README.

## 0.1.3

* Fix README.md errors.

## 0.1.2

* Updated package description.

## 0.1.1

* Chinese documentation added.

## 0.1.0

* Documentation added.

## 0.0.1

* Initial release.
