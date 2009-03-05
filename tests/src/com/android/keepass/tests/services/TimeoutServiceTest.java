/* Copyright 2009 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
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
package com.android.keepass.tests.services;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.test.ServiceTestCase;

import com.android.keepass.services.TimeoutService;

public class TimeoutServiceTest extends ServiceTestCase<TimeoutService> {
	public TimeoutServiceTest() {
		super(TimeoutService.class);
	}
	
	private TimeoutService mService;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		startService(new Intent(getContext(), TimeoutService.class));
		mService = getService();
		assertNotNull(mService);
	}



	public void testTimeout() {
		assertFalse("Timeout is not set at the beginning.", mService.HasTimedOut() );
		
		mService.startTimeout(1000);

		assertFalse("Timeout too early.", mService.HasTimedOut() );
	
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			assertTrue("Thread interrupted.", false);
		}
		
		assertTrue("Timeout was not set.", mService.HasTimedOut());
		
	}
}
