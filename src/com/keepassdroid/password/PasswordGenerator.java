package com.keepassdroid.password;

import java.util.Random;

import android.content.Context;

import com.android.keepass.R;

public class PasswordGenerator {
	private static final String upperCaseChars	= "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String lowerCaseChars 	= "abcdefghijklmnopqrstuvwxyz";
	private static final String digitChars 		= "0123456789";
	private static final String minusChars 		= "-";
	private static final String underlineChars 	= "_";
	private static final String spaceChars 		= " ";
	private static final String specialChars 	= "!\"#$%&'*+,./:;=?@\\^`";
	private static final String bracketChars 	= "[]{}()<>";
	
	private Context cxt;
	
	public PasswordGenerator(Context cxt) {
		this.cxt = cxt;
	}
	
	public String generatePassword(int length, boolean upperCase, boolean lowerCase, boolean digits, boolean minus, boolean underline, boolean space, boolean specials, boolean brackets) throws IllegalArgumentException{
		if (length <= 0)
			throw new IllegalArgumentException(cxt.getString(R.string.error_wrong_length));
		
		if (!upperCase && !lowerCase && !digits && !minus && !underline && !space && !specials && !brackets)
			throw new IllegalArgumentException(cxt.getString(R.string.error_pass_gen_type));
		
		String characterSet = getCharacterSet(upperCase, lowerCase, digits, minus, underline, space, specials, brackets);
		
		int size = characterSet.length();
		
		StringBuffer buffer = new StringBuffer();

		Random random = new Random();
		if (size > 0) {
			
			for (int i = 0; i < length; i++) {
				char c = characterSet.charAt((char) random.nextInt(size));
				
				buffer.append(c);
			}
		}
		
		return buffer.toString();
	}
	
	public String getCharacterSet(boolean upperCase, boolean lowerCase, boolean digits, boolean minus, boolean underline, boolean space, boolean specials, boolean brackets) {
		StringBuffer charSet = new StringBuffer();
		
		if (upperCase)
			charSet.append(upperCaseChars);
		
		if (lowerCase)
			charSet.append(lowerCaseChars);
		
		if (digits)
			charSet.append(digitChars);
		
		if (minus)
			charSet.append(minusChars);
		
		if (underline)
			charSet.append(underlineChars);
		
		if (space)
			charSet.append(spaceChars);
		
		if (specials)
			charSet.append(specialChars);
		
		if (brackets)
			charSet.append(bracketChars);
		
		return charSet.toString();
	}
}
