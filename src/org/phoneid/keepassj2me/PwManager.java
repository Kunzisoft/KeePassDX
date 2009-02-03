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
import java.util.Vector;

// Bouncy Castle
import org.bouncycastle1.crypto.digests.*;

/**
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwManager {

    // Constants
    // private static final int PWM_SESSION_KEY_SIZE = 12;
    // DB sig from KeePass 1.03 
    static final int PWM_DBSIG_1               = 0x9AA2D903;
    // DB sig from KeePass 1.03
    static final int PWM_DBSIG_2               = 0xB54BFB65;
    // DB sig from KeePass 1.03
    static final int PWM_DBVER_DW              = 0x00030002;

    static final int PWM_FLAG_SHA2             = 1;
    static final int PWM_FLAG_RIJNDAEL         = 2;
    static final int PWM_FLAG_ARCFOUR          = 4;
    static final int PWM_FLAG_TWOFISH          = 8;

    static final int ALGO_AES                  = 0;
    static final int ALGO_TWOFISH              = 1;

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
    byte             masterKey[]            = new byte[32];
    // Algorithm used to encrypt the database
    int              algorithm;
    int              numKeyEncRounds;

    // root group
    PwGroup rootGroup;

    public void setMasterKey( String key ) {
	if( key == null || key.length() == 0 )
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
