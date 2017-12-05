# Android Keepass DX

<img src="https://raw.githubusercontent.com/Kunzisoft/KeePassDX/master/art/logo.png"> Keepass DX is a material design Keepass Client for manage keys and passwords in crypt database for your android device.

### Features

- Simplified creation of the database file
- Create entries and groups
- Open database, copy username / password, open URI / URL
- Fingerprint for fast unlocking
- Material design with themes
- Device integration and AutoFill (In progress)

<img src="https://raw.githubusercontent.com/Kunzisoft/KeePassDX/master/art/screen1.jpg" width="220">
<img src="https://raw.githubusercontent.com/Kunzisoft/KeePassDX/master/art/screen2.jpg" width="220">

## What is KeePass?

Today you need to remember many passwords. You need a password for the Windows network logon, your e-mail account, your website's FTP password, online passwords (like website member account), etc. etc. etc. The list is endless. Also, you should use different passwords for each account. Because if you use only one password everywhere and someone gets this password you have a problem... A serious problem. The thief would have access to your e-mail account, website, etc. Unimaginable.

KeePass is a free open source password manager, which helps you to manage your passwords in a secure way. You can put all your passwords in one database, which is locked with one master key or a key file. So you only have to remember one single master password or select the key file to unlock the whole database. The databases are encrypted using the best and most secure encryption algorithms currently known (AES and Twofish). For more information, see the features page. 

## Is it really free?

Yes, KeePass is really free, and more than that: it is open source (OSI certified). You can have a look at its full source and check whether the encryption algorithms are implemented correctly.

## Donation

Even if the application is free, to maintain the application, you can make donations.

[![Donation Paypal](https://4.bp.blogspot.com/-ncaIbUGaHOk/WfhaThYUPGI/AAAAAAAAAVQ/_HidNgdB1q4DaC24ujaKNzH64KUUJiQewCLcBGAs/s1600/pay-with-paypal.png)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KM6QMDAXZM3UU "Kunzisoft Paypal Donation")

[![Donation Liberapay](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/Kunzisoft/donate "Kunzisoft Liberapay Donation")

<img src="https://raw.githubusercontent.com/Kunzisoft/KeePassDX/master/art/screen4.jpg" width="220">
<img src="https://raw.githubusercontent.com/Kunzisoft/KeePassDX/master/art/screen5.jpg" width="220">

## Download

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/en/packages/com.kunzisoft.keepass.libre/)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
      alt="Get it on Google Play"
	height="80">](https://play.google.com/store/apps/details?id=com.kunzisoft.keepass.free)

### JNI

Native library build instructions:
1. Make sure you have the latest MIPS Android NDK installed: 
   https://developer.android.com/tools/sdk/ndk/index.html
2. From KeePassDroid/app/src/main/jni, call prep_build.sh to download and unpack the crypto sources.
3. The standard gradle files build everything now.

This project is a fork of [KeepassDroid](https://github.com/bpellin/keepassdroid) by bpellin.

## License

 Copyright (c) 2017 Jeremy Jamet / Kunzisoft.

 This file is part of KeePass DX.

  KeePass DX is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 2 of the License, or
  (at your option) any later version.

  KeePass DX is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
