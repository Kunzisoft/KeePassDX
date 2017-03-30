/*
* Copyright 2013-2017 Brian Pellin.
*
* This file is part of KeePassDroid.
*
* KeePassDroid is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* KeePassDroid is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with KeePassDroid. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.keepassdroid.tests.crypto;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import junit.framework.TestCase;

import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.crypto.engine.AesEngine;
import com.keepassdroid.crypto.engine.CipherEngine;
import com.keepassdroid.stream.BetterCipherInputStream;
import com.keepassdroid.stream.LEDataInputStream;

public class CipherTest extends TestCase {
	private Random rand = new Random();
	
	public void testCipherFactory() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		byte[] key = new byte[32];
		byte[] iv = new byte[16];
		
		byte[] plaintext = new byte[1024];
		
		rand.nextBytes(key);
		rand.nextBytes(iv);
		rand.nextBytes(plaintext);
		
		CipherEngine aes = CipherFactory.getInstance(AesEngine.CIPHER_UUID);
		Cipher encrypt = aes.getCipher(Cipher.ENCRYPT_MODE, key, iv);
		Cipher decrypt = aes.getCipher(Cipher.DECRYPT_MODE, key, iv);

		byte[] secrettext = encrypt.doFinal(plaintext);
		byte[] decrypttext = decrypt.doFinal(secrettext);
		
		assertArrayEquals("Encryption and decryption failed", plaintext, decrypttext);
	}

	public void testCipherStreams() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {
		final int MESSAGE_LENGTH = 1024;
		
		byte[] key = new byte[32];
		byte[] iv = new byte[16];
		
		byte[] plaintext = new byte[MESSAGE_LENGTH];
		
		rand.nextBytes(key);
		rand.nextBytes(iv);
		rand.nextBytes(plaintext);

		CipherEngine aes = CipherFactory.getInstance(AesEngine.CIPHER_UUID);
		Cipher encrypt = aes.getCipher(Cipher.ENCRYPT_MODE, key, iv);
		Cipher decrypt = aes.getCipher(Cipher.DECRYPT_MODE, key, iv);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CipherOutputStream cos = new CipherOutputStream(bos, encrypt);
		cos.write(plaintext);
		cos.close();
		
		byte[] secrettext = bos.toByteArray();
		
		ByteArrayInputStream bis = new ByteArrayInputStream(secrettext);
		BetterCipherInputStream cis = new BetterCipherInputStream(bis, decrypt);
		LEDataInputStream lis = new LEDataInputStream(cis);
		
		byte[] decrypttext = lis.readBytes(MESSAGE_LENGTH);
		
		assertArrayEquals("Encryption and decryption failed", plaintext, decrypttext);
	}
}
