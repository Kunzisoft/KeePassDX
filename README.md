# Android KeePassDX

<img alt="KeePassDX Icon" src="https://raw.githubusercontent.com/Kunzisoft/KeePassDX/master/art/icon.png"> **Lightweight password safe and manager for Android**, KeePassDX allows editing encrypted data in a single file in KeePass format and fill in the forms in a secure way.

<img alt="KeePassDX Screenshot" src="https://raw.githubusercontent.com/Kunzisoft/KeePassDX/master/art/screen.jpg" width="220">

### Features

 - Create database files / entries and groups.
 - Support for **.kdb** and **.kdbx** files (version 1 to 4) with AES - Twofish - ChaCha20 - Argon2 algorithm.
 - **Compatible** with the majority of alternative programs (KeePass, KeePassXC, KeeWeb, …).
 - Allows opening and **copying URI / URL fields quickly**.
 - **Biometric recognition** for fast unlocking *(fingerprint / face unlock / …)*.
 - **One-Time Password** management *(HOTP / TOTP)* for Two-factor authentication (2FA).
 - Material design with **themes**.
 - **Auto-Fill** and Integration.
 - Field filling **keyboard**.
 - Dynamic **templates** 
 - **History** of each entry.
 - Precise management of **settings**.
 - Code written in **native languages** *(Kotlin / Java / JNI / C)*.

KeePassDX is **open source** and **ad-free**.

## What is KeePassDX?

An alternative to remembering an endless list of passwords manually. This is made more difficult by **using different passwords for each account**. If you use one password everywhere and security fails only one of those places, it grants access to your e-mail account, website, etc, and you may not know about it or notice, before bad things happen.

KeePassDX is a **password manager for Android**, which helps you **manage your passwords in a secure way**. You can put all your passwords in one database, locked with a **master key** and/or a **keyfile**. You **only have to remember one single master password and/or select the keyfile** to unlock the whole database. The databases are encrypted using the best and **most secure encryption algorithms** currently known.

## Small print?

KeePassDX is under **open source GPL3 license**, meaning you can use, study, change and share it at will. Copyleft ensures it stays that way.
From the full source, anyone can build, fork, and check whether for example the encryption algorithms are implemented correctly.
There is **no advertising**.

Do not worry, **the main features remain completely free**.

Optional visual styles are accessible after a contribution (and a congratulatory message (Ո‿Ո) ) or the purchase of an extended version to encourage contribution to the work of open source projects!
*If you contribute to the project and do not have access to the styles, do not hesitate to contact the author at [contact@kunzisoft.com](contact@kunzisoft.com).*

## Contributions

* Add features by making a **[pull request](https://help.github.com/articles/about-pull-requests/)**.
* Help to **[translate](https://hosted.weblate.org/projects/keepass-dx/strings/)** KeePassDX to your language (on [Weblate](https://hosted.weblate.org/projects/keepass-dx/) or by sending a [pull request](https://help.github.com/articles/about-pull-requests/)).
* **[Donate](https://www.keepassdx.com/#donation)**  人◕ ‿‿ ◕人Y for a better service and a quick development of your features.
* Buy the **[Pro version](https://play.google.com/store/apps/details?id=com.kunzisoft.keepass.pro)** of KeePassDX.

## Download

*[F-Droid](https://f-droid.org/packages/com.kunzisoft.keepass.libre/) is the recommended way of installing, a libre software project that verifies that all the libraries and app code is libre software.*

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/com.kunzisoft.keepass.libre/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
      alt="Get it on Google Play"
	height="80">](https://play.google.com/store/apps/details?id=com.kunzisoft.keepass.free)
[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png"
      alt="Get it on Github"
	height="80">](https://github.com/Kunzisoft/KeePassDX/releases)

## Verify the authenticity of the downloaded app from GitHub
1- Download the latest app from [GitHub releases](https://github.com/Kunzisoft/KeePassDX/releases/latest). <br>
2- Open the directory where you saved the downloaded file in the Terminal on Linux/MacOS. <br>
3- You must have `keytool` command installed. <br>
4- Depending on the flavor you downloaded, run:
```
keytool -printcert -jarfile KeePassDX-*-libre.apk | grep '7D:55:B8:AF:21:03:81:AA:BF:96:0F:07:E1:7C:F7:85:7B:6D:2A:64:2C:A2:DA:6B:F0:BD:F1:B2:00:36:2F:04'
```
Or:
```
keytool -printcert -jarfile KeePassDX-*-free.apk | grep '7D:55:B8:AF:21:03:81:AA:BF:96:0F:07:E1:7C:F7:85:7B:6D:2A:64:2C:A2:DA:6B:F0:BD:F1:B2:00:36:2F:04'
```
You should get this output:
```
SHA256: 7D:55:B8:AF:21:03:81:AA:BF:96:0F:07:E1:7C:F7:85:7B:6D:2A:64:2C:A2:DA:6B:F0:BD:F1:B2:00:36:2F:04
```
## Frequently Asked Questions

Other questions? You can read the [FAQ](https://github.com/Kunzisoft/KeePassDX/wiki/FAQ) 
	
## Other devices

- [KeePass](https://keepass.info/) (https://keepass.info/) is the original and official project for the desktop, with technical documentation for standardized database files. It is updated regularly with active maintenance (written in C#).

- [KeePassXC](https://keepassxc.org/) (https://keepassxc.org/) is an alternative integration of KeePass written in C++.

- [KeeWeb](https://keeweb.info/) (https://keeweb.info/) is a web version that is also compatible with KeePass files.

## License

  Copyright © 2024 Jeremy Jamet / [Kunzisoft](https://www.kunzisoft.com).

  This file is part of KeePassDX.

  [KeePassDX](https://www.keepassdx.com) is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  KeePassDX is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
  
  *This project is a fork of [KeePassDroid](https://github.com/bpellin/keepassdroid) by bpellin.*
