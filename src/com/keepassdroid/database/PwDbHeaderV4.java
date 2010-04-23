/*
 * Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database;

public class PwDbHeaderV4 extends PwDbHeader {
	public static final int PWM_DBSIG_PRE2            = 0xB54BFB66;
    public static final int PWM_DBSIG_2               = 0xB54BFB67;

	@Override
	public boolean matchesHeader(int sig1, int sig2) {
		return (sig1 == PWM_DBSIG_1) && ( (sig2 == PWM_DBSIG_2) || (sig2 == PWM_DBSIG_2) );
	}
    
}
