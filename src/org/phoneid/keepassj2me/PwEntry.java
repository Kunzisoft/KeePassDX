/*
KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package org.phoneid.keepassj2me;

// PhoneID
import org.phoneid.*;

// Java
import java.util.*;


/**
 * Structure containing information about one entry.
 * 
 * <PRE>
 * One entry: [FIELDTYPE(FT)][FIELDSIZE(FS)][FIELDDATA(FD)]
 *            [FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)]...
 *            
 * [ 2 bytes] FIELDTYPE
 * [ 4 bytes] FIELDSIZE, size of FIELDDATA in bytes
 * [ n bytes] FIELDDATA, n = FIELDSIZE
 * 
 * Notes:
 *  - Strings are stored in UTF-8 encoded form and are null-terminated.
 *  - FIELDTYPE can be one of the FT_ constants.
 * </PRE>
 *
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwEntry {

    // for tree traversing
    public PwGroup parent = null;
    
  public PwEntry() {
  }



  /**
   * @return the actual password byte array.
   */
  public byte[] getPassword() {
    return password;
  }



  /** Securely erase old password before copying new. */
  public void setPassword( byte[] buf, int offset, int len ) {
    if( password != null ) {
	PhoneIDUtil.fill( password, (byte)0 );
	password = null;
    }
    password = new byte[len];
    System.arraycopy( buf, offset, password, 0, len );
  }



  /**
   * @return the actual binaryData byte array.
   */
  public byte[] getBinaryData() {
    return binaryData;
  }



  /** Securely erase old data before copying new. */
  public void setBinaryData( byte[] buf, int offset, int len ) {
    if( binaryData != null ) {
	PhoneIDUtil.fill( binaryData, (byte)0 );
	binaryData = null;
    }
    binaryData = new byte[len];
    System.arraycopy( buf, offset, binaryData, 0, len );
  }


  /** Size of byte buffer needed to hold this struct. */
  public static final int BUF_SIZE = 124;


  public byte             uuid[]   = new byte[16];
  public int              groupId;
  public int              imageId;

  public String           title;
  public String           url;
  public String           username;

  private byte[]          password;
  
  public String           additional;

  public Date             tCreation;
  public Date             tLastMod;
  public Date             tLastAccess;
  public Date             tExpire;

  /** A string describing what is in pBinaryData */
  public String           binaryDesc;
  private byte[]          binaryData;
}
