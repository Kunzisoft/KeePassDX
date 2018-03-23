/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.password;

import java.security.SecureRandom;

import android.content.Context;

import com.kunzisoft.keepass.R;

public class PasswordGenerator {
	private static final String UPPERCASE_CHARS	= "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
	private static final String DIGIT_CHARS 	= "0123456789";
	private static final String MINUS_CHAR	 	= "-";
	private static final String UNDERLINE_CHAR 	= "_";
	private static final String SPACE_CHAR 		= " ";
	private static final String SPECIAL_CHARS 	= "!\"#$%&'*+,./:;=?@\\^`";
	private static final String BRACKET_CHARS 	= "[]{}()<>";

    // From KeePassXC code https://github.com/keepassxreboot/keepassxc/pull/538
    private String extendedChars() {
        StringBuilder charSet = new StringBuilder();
        // [U+0080, U+009F] are C1 control characters,
        // U+00A0 is non-breaking space
        for(char ch = '\u00A1'; ch <= '\u00AC'; ++ch)
            charSet.append(ch);
        // U+00AD is soft hyphen (format character)
        for(char ch = '\u00AE'; ch < '\u00FF'; ++ch)
            charSet.append(ch);
        charSet.append('\u00FF');
        return charSet.toString();
    }
	
	private Context cxt;
	
	public PasswordGenerator(Context cxt) {
		this.cxt = cxt;
	}
	
	public String generatePassword(int length,
                                   boolean upperCase,
                                   boolean lowerCase,
                                   boolean digits,
                                   boolean minus,
                                   boolean underline,
                                   boolean space,
                                   boolean specials,
                                   boolean brackets,
                                   boolean extended) throws IllegalArgumentException{
		// Desired password length is 0 or less
		if (length <= 0) {
			throw new IllegalArgumentException(cxt.getString(R.string.error_wrong_length));
		}
		
		// No option has been checked
		if (    !upperCase
                && !lowerCase
                && !digits
                && !minus
                && !underline
                && !space
                && !specials
                && !brackets
                && !extended) {
			throw new IllegalArgumentException(cxt.getString(R.string.error_pass_gen_type));
		}
		
		String characterSet = getCharacterSet(
		        upperCase,
                lowerCase,
                digits,
                minus,
                underline,
                space,
                specials,
                brackets,
                extended);
		
		int size = characterSet.length();
		
		StringBuilder buffer = new StringBuilder();

		SecureRandom random = new SecureRandom(); // use more secure variant of Random!
		if (size > 0) {
			for (int i = 0; i < length; i++) {
				char c = characterSet.charAt((char) random.nextInt(size));
				buffer.append(c);
			}
		}
		return buffer.toString();
	}
	
	private String getCharacterSet(boolean upperCase,
								  boolean lowerCase,
								  boolean digits,
								  boolean minus,
								  boolean underline,
								  boolean space,
								  boolean specials,
								  boolean brackets,
                                  boolean extended) {
		StringBuilder charSet = new StringBuilder();
		
		if (upperCase) {
			charSet.append(UPPERCASE_CHARS);
		}
		
		if (lowerCase) {
			charSet.append(LOWERCASE_CHARS);
		}
		
		if (digits) {
			charSet.append(DIGIT_CHARS);
		}
		
		if (minus) {
			charSet.append(MINUS_CHAR);
		}
		
		if (underline) {
			charSet.append(UNDERLINE_CHAR);
		}
		
		if (space) {
			charSet.append(SPACE_CHAR);
		}
		
		if (specials) {
			charSet.append(SPECIAL_CHARS);
		}
		
		if (brackets) {
			charSet.append(BRACKET_CHARS);
		}

		if (extended) {
            charSet.append(extendedChars());
        }

		return charSet.toString();
	}
}
