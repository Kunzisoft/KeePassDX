package com.kunzisoft.keepass.credentialprovider.util

class AppRelyingPartyRelation {

    companion object {
        fun isRelationValid(relyingParty: String, apkSigningCertificate: ByteArray?): Boolean {
            /*
            TODO
            to implement this, a request to https://$rp/.well-known/assetlinks.json,
            parsing the result and matching the hash of the apkSigningCertificate is needed.
            This is needed to make sure that a malicious app can not act as an arbitrary relying party.
            In short: prevent phishing
            */
            return false
        }

    }
}