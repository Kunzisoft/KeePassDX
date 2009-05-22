/*

KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package org.phoneid.keepassj2me;

// Java
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import org.bouncycastle.crypto.digests.SHA256Digest;

import com.android.keepass.keepasslib.InvalidKeyFileException;

/**
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwManager {
	// TODO: delete ME
	public byte[] postHeader;
	
    // Constants
    // private static final int PWM_SESSION_KEY_SIZE = 12;

    // Descriptive name for database, used in GUI.
    public  String   name = "KeePass database";
  
    // Special entry for settings
    public PwEntry   metaInfo;

    // all entries
    public Vector<PwEntry> entries = new Vector<PwEntry>();
    // all groups
    public Vector<PwGroup> groups = new Vector<PwGroup>();
    // Last modified entry, use GetLastEditedEntry() to get it
    // PwEntry          lastEditedEntry        = null;
    // Pseudo-random number generator
    //CNewRandom m_random;
    // Used for in-memory encryption of passwords
    // private byte     sessionKey[]           = new byte[PWM_SESSION_KEY_SIZE];
    // Master key used to encrypt the whole database
    public byte             masterKey[]            = new byte[32];
    // Algorithm used to encrypt the database
    int              algorithm;
    public int              numKeyEncRounds;
    
    // Debugging entries
    public PwDbHeader dbHeader;
    public long paddingBytes;
    public byte[] finalKey;

    // root group
    PwGroup rootGroup;
   
    public int getAlgorithm() {
    	return algorithm;
    }
    
    public int getNumKeyEncRecords() {
    	return numKeyEncRounds;
    }
    
    public void setMasterKey( String key, String keyFileName ) throws InvalidKeyFileException, IOException {
    	assert( key != null && keyFileName != null );
    	
    	if ( key.length() > 0 && keyFileName.length() > 0 ) {
    		setCompositeKey(key, keyFileName);
    	} else if ( key.length() > 0 ) {
    		setPasswordKey(key);
    	} else if ( keyFileName.length() > 0 ) {
    		setFileKey(keyFileName);
    	} else {
    		throw new IllegalArgumentException( "Key cannot be empty." );
    	}
    }
    
    private void setCompositeKey( String key, String keyFileName) throws InvalidKeyFileException, IOException {
    	assert(key != null && keyFileName != null);
    	
    	byte[] fileKey = new byte[32];
    	setFileKey(keyFileName);
    	System.arraycopy(masterKey, 0, fileKey, 0, 32);
    	
    	byte[] passwordKey = new byte[32];
    	setPasswordKey(key);
    	System.arraycopy(masterKey, 0, passwordKey, 0, 32);
    	
    	SHA256Digest md = new SHA256Digest();
    	md.update(passwordKey, 0, 32);
    	md.update(fileKey, 0, 32);
    	masterKey = new byte[md.getDigestSize()];
    	md.doFinal(masterKey, 0);
    	
    }
    
    private void setFileKey(String fileName) throws InvalidKeyFileException, IOException {
		assert(fileName != null);
		
		File keyfile = new File(fileName);
		long fileSize = keyfile.length();
	
		if ( ! keyfile.exists() ) {
			throw new InvalidKeyFileException("Key file does not exist.");
		}
		
		
		FileInputStream fis;
		try {
			fis = new FileInputStream(keyfile);
		} catch (FileNotFoundException e) {
			throw new InvalidKeyFileException("Key file does not exist.");
		}
		
		if ( fileSize == 0 ) {
			throw new InvalidKeyFileException("Key file is empty.");
		} else if ( fileSize == 32 ) {
			masterKey = new byte[32];
			if ( fis.read(masterKey, 0, 32) != 32 ) {
				throw new IOException("Error reading key.");
			}
			
			return;
		} else if ( fileSize == 64 ) {
			byte[] hex = new byte[64];
			
			if ( fis.read(hex, 0, 64) != 64 ) {
				throw new IOException("Error reading key.");
			}

			masterKey = hexStringToByteArray(new String(hex));
			return;
		}
	
		SHA256Digest md = new SHA256Digest();
		byte[] buffer = new byte[2048];
		int offset = 0;
		
		try {
			while (true) {
				int bytesRead = fis.read(buffer, 0, 2048);
				if ( bytesRead == -1 ) break;  // End of file
				
				md.update(buffer, 0, bytesRead);
				offset += bytesRead;
				
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		masterKey = new byte[md.getDigestSize()];
		md.doFinal(masterKey, 0);
    }
    
    
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
   
    private void setPasswordKey(String key ) {
    	assert(key!=null);
    	
		if ( key.length() == 0 )
		    throw new IllegalArgumentException( "Key cannot be empty." );
		
		SHA256Digest md = new SHA256Digest();
		md.update( key.getBytes(), 0, key.getBytes().length );
		masterKey = new byte[md.getDigestSize()];
		md.doFinal(masterKey, 0);
    }
    	
  /*
  //
  // Erase all members and buffers, then null pointers.
  // Ensures no memory (that we control) contains leftover keys.
  //
  void secureErase() {
    // TODO finish this!
  }

    */

    public Vector<PwGroup> getGrpRoots() {
	int target = 0;
	Vector<PwGroup> kids = new Vector<PwGroup>();
	for( int i=0; i < groups.size(); i++ ) {
	    PwGroup grp = groups.elementAt( i );
	    if( grp.level == target )
		kids.addElement( grp );
	}
	return kids;
    }
    
    public int getRootGroupId() {
    	for ( int i = 0; i < groups.size(); i++ ) {
    		PwGroup grp = groups.elementAt(i);
    		if ( grp.level == 0 ) {
    			return grp.groupId;
    		}
    	}
    	
    	return -1;
    }

    public Vector<PwGroup> getGrpChildren( PwGroup parent ) {
	int idx = groups.indexOf( parent );
	int target = parent.level + 1;
	Vector<PwGroup> kids = new Vector<PwGroup>();
	while( ++idx < groups.size() ) {
	    PwGroup grp = groups.elementAt( idx );
	    if( grp.level < target )
		break;
	    else
		if( grp.level == target )
		    kids.addElement( grp );
	}
	return kids;
    }

    public Vector<PwEntry> getEntries( PwGroup parent ) {
	Vector<PwEntry> kids = new Vector<PwEntry>();
	/*for( Iterator i = entries.iterator(); i.hasNext(); ) {
	    PwEntry ent = (PwEntry)i.next();
	    if( ent.groupId == parent.groupId )
		kids.add( ent );
		}*/
	for (int i=0; i<entries.size(); i++) {
	    PwEntry ent = entries.elementAt(i);
	    if( ent.groupId == parent.groupId )
		kids.addElement( ent );
	}
	return kids;
    }

  public String toString() {
    return name;
  }



    public void addGroup(PwGroup group)
    {
	groups.addElement(group);
    }
    
    public void addEntry(PwEntry entry)
    {
	entries.addElement(entry);
    }
    
    public void constructTree(PwGroup currentGroup)
    {
	// I'm in root
	if (currentGroup == null) {
	    rootGroup = new PwGroup();
		
	    Vector<PwGroup> rootChildGroups = getGrpRoots();
	    rootGroup.childGroups = rootChildGroups;
	    rootGroup.childEntries = new Vector<PwEntry>();
	    for (int i=0; i<rootChildGroups.size(); i++) {
		rootChildGroups.elementAt(i).parent = rootGroup;
		constructTree(rootChildGroups.elementAt(i));
	    }
	    return;
	}

	// I'm in non-root
	// get child groups
	currentGroup.childGroups = getGrpChildren(currentGroup);
	currentGroup.childEntries = getEntries(currentGroup);

	// set parent in child entries
	for (int i=0; i<currentGroup.childEntries.size(); i++) {
	    currentGroup.childEntries.elementAt(i).parent = currentGroup;
	}
	// recursively construct child groups
	for (int i=0; i<currentGroup.childGroups.size(); i++) {
	    currentGroup.childGroups.elementAt(i).parent = currentGroup;
	    constructTree(currentGroup.childGroups.elementAt(i));
	}
	return;
    }
}
