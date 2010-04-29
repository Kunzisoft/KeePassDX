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

package com.keepassdroid.database;

// Java
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Vector;



/**
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwDatabaseV3 extends PwDatabase {
	// TODO: delete ME
	public byte[] postHeader;
	
    // Constants
    // private static final int PWM_SESSION_KEY_SIZE = 12;

    // Descriptive name for database, used in GUI.
    public  String   name = "KeePass database";
  
    // Special entry for settings
    public PwEntryV3   metaInfo;

    // all entries
    public Vector<PwEntryV3> entries = new Vector<PwEntryV3>();
    // all groups
    public Vector<PwGroupV3> groups = new Vector<PwGroupV3>();
    // Algorithm used to encrypt the database
    public int              algorithm;
    public int              numKeyEncRounds;
    
    // Debugging entries
    public PwDbHeaderV3 dbHeader;
    //public long paddingBytes;
    public byte[] finalKey;

    // root group
    public PwGroupV3 rootGroup;
   
    public int getAlgorithm() {
    	return algorithm;
    }
    
    public int getNumKeyEncRecords() {
    	return numKeyEncRounds;
    }
    
    public Vector<PwGroupV3> getGrpRoots() {
	int target = 0;
	Vector<PwGroupV3> kids = new Vector<PwGroupV3>();
	for( int i=0; i < groups.size(); i++ ) {
	    PwGroupV3 grp = groups.elementAt( i );
	    if( grp.level == target )
		kids.addElement( grp );
	}
	return kids;
    }
    
    public int getRootGroupId() {
    	for ( int i = 0; i < groups.size(); i++ ) {
    		PwGroupV3 grp = groups.elementAt(i);
    		if ( grp.level == 0 ) {
    			return grp.groupId;
    		}
    	}
    	
    	return -1;
    }

    public Vector<PwGroupV3> getGrpChildren( PwGroupV3 parent ) {
	int idx = groups.indexOf( parent );
	int target = parent.level + 1;
	Vector<PwGroupV3> kids = new Vector<PwGroupV3>();
	while( ++idx < groups.size() ) {
	    PwGroupV3 grp = groups.elementAt( idx );
	    if( grp.level < target )
		break;
	    else
		if( grp.level == target )
		    kids.addElement( grp );
	}
	return kids;
    }

    public Vector<PwEntryV3> getEntries( PwGroupV3 parent ) {
	Vector<PwEntryV3> kids = new Vector<PwEntryV3>();
	/*for( Iterator i = entries.iterator(); i.hasNext(); ) {
	    PwEntryV3 ent = (PwEntryV3)i.next();
	    if( ent.groupId == parent.groupId )
		kids.add( ent );
		}*/
	for (int i=0; i<entries.size(); i++) {
	    PwEntryV3 ent = entries.elementAt(i);
	    if( ent.groupId == parent.groupId )
		kids.addElement( ent );
	}
	return kids;
    }

  public String toString() {
    return name;
  }



    public void addGroup(PwGroupV3 group)
    {
	groups.addElement(group);
    }
    
    public void addEntry(PwEntryV3 entry)
    {
	entries.addElement(entry);
    }
    
    public void constructTree(PwGroupV3 currentGroup)
    {
	// I'm in root
	if (currentGroup == null) {
	    rootGroup = new PwGroupV3();
		
	    Vector<PwGroupV3> rootChildGroups = getGrpRoots();
	    rootGroup.childGroups = rootChildGroups;
	    rootGroup.childEntries = new Vector<PwEntryV3>();
	    rootGroup.level = -1;
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
    
    public PwGroupV3 newGroup(String name, PwGroupV3 parent) {
    	// Initialize group    	
    	PwGroupV3 group = new PwGroupV3();

    	group.parent = parent;
    	group.groupId = newGroupId();
    	group.imageId = 0;
    	group.name = name;
    	
    	Date now = Calendar.getInstance().getTime();
    	group.tCreation = new PwDate(now);
    	group.tLastAccess = new PwDate(now);
    	group.tLastMod = new PwDate(now);
    	group.tExpire = new PwDate(PwGroupV3.NEVER_EXPIRE);
    	
   		group.level = parent.level + 1;

   		group.childEntries = new Vector<PwEntryV3>();
   		group.childGroups = new Vector<PwGroupV3>();
   		
   		// Add group PwDatabaseV3 and Parent
   		parent.childGroups.add(group);
   		groups.add(group);
   		
    	return group;
    }
    
    public void removeGroup(PwGroupV3 group) {
    	group.parent.childGroups.remove(group);
    	groups.remove(group);
    }
    
    /** Generates an unused random group id
     * @return new group id
     */
    private int newGroupId() {
    	boolean foundUnusedId = false;
    	int newId = 0;
    	
    	Random random = new Random();
    	
    	while ( ! foundUnusedId ) {
    		newId = random.nextInt();
    		
    		if ( ! isGroupIdUsed(newId) ) {
    			foundUnusedId = true;
    		}
    	}
    	
    	return newId;
    }
    
    /** Determine if an id number is already in use
     * @param id ID number to check for
     * @return True if the ID is used, false otherwise
     */
    private boolean isGroupIdUsed(int id) {
    	for ( int i = 0; i < groups.size(); i++ ) {
    		if ( groups.get(i).groupId == id ) {
    			return true;
    		}
    	}
    	
    	return false;
    }
}
