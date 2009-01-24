package org.phoneid;

// import org.bouncycastle1.util.encoders.Hex;

public class Definition {
    public static final String KDBRecordStoreName = "KeePassKDB";
    public static final int NUM_ICONS = 65;
    public static final String TITLE = new String ("KeePass for J2ME");
    public static final int USER_CODE_LEN = 4;
    public static final int PASS_CODE_LEN = 4;
    public static final int ENC_CODE_LEN = 16;
    public static final String DEFAULT_KDB_URL = "http://keepassserver.info/download.php";
    public static final int MAX_TEXT_LEN = 128;
    public static final int PASSWORD_KEY_SHA_ROUNDS = 6000;
    public static final int KDB_HEADER_LEN = 124;
    public static final boolean CONFIG_NO_WEB = false; // disable web sync feature
    public static final boolean DEBUG = false;

    public static final byte[] ZeroIV = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
}

