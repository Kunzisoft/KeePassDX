# F.A.Q.

## Why KeePass DX?

KeePass DX is an **Android password manager** implemented from Keepass password manager.

KeePass DX was created to meet the security and usability needs of a KeePass application on Android :

 - To be easy to use with **secure password management and form filling tools**.
 - To use only tools under **open source license** to guarantee the security of the application (With [open source store](https://f-droid.org/en/) and no closed API).
 - To be in a **native langage** (java) for weight, security and a better integration of the application.
 - To respect **Android design, architecture and ergonomic**.

## What makes KeePass DX stand out from other password managers?

 - We **do not recover your sensitive data** on a private server or a closed cloud, you have control of your passwords.
 - We respect **KeePass file standards** to maintain compatibility and data porting on different devices (computers and portable devices with different operating system).
 - The code is **open source**, which implies increased **security**, you can check how the encryption algorithms are implemented.
 - We remain attentive to **your needs** and we can even integrate the features that you have defined.
 - We **do not put advertising** even in the free version.

## How am I sure my passwords are safely stored on the application?

- We allow users to save and use passwords, keys and digital identities in a secure way by **integrating the last encryption algorithms** and **Android architecture standards**.
- You can increase the security of your database by increasing the rounds of encryption keys. *(In Settings -> Database Settings when your database is open)* **Warning**: *Increase the number of rounds sparingly to have a reasonable opening time.*

## Can I store my data on a cloud storage?

**Yes** this is possible. Otherwise, we **recommend using cloud with personal server and open source license**, like [NextCloud](https://f-droid.org/en/packages/com.nextcloud.client/) to be sure how your databases are stored.

## Can I recover my passwords on another device if I loose my main device?

**Yes** you can, but you **must first save the .kdb or .kdbx file from your database to an external storage** *(like a hardrive or a cloud)*.
We recommend you save your data after each modification so incase you loose your android device you could retrieve the data and import it into the new KeePass DX installed on the new android device. 

## Why not an online version?

The offline and online client concepts only exists with Keepass2Android because the file access network tools are directly integrated into the code of the main application. Which is a very dubious choice knowing that **it is not normally the purpose of a password management application to take care of external file synchronization on clouds** (which can be under closed licensed and recover your data base), it is rather the purpose of the [file management application](https://developer.android.com/guide/topics/providers/document-provider).

## Can I open my database easily other than with a password?

**Yes**, we have integrated a secure openning option of fingerprint for android devices that support this feature, so no one can access the application without scanning his/her fingerprint or fill a master key.

## Can I open my database without my master key (master password and/or key file)?

**No**, you can not open a database file without the master password (and / or) the associated key file. Be sure to remember your master password and save the key file in a safe place.

## Can I suggest features and report bugs for the application?
**Yes**, we welcome this you could go ahead and do that on our github: 
https://github.com/Kunzisoft/KeePassDX
