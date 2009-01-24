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

import java.util.*;

/**
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwGroup {
  public PwGroup() {
  }
  
  public String toString() {
    return name;
  }


  /** Size of byte buffer needed to hold this struct. */
  public static final int BUF_SIZE = 124;

    // for tree traversing
    public Vector childGroups = null;
    public Vector childEntries = null;
    public PwGroup parent = null;

  public int              groupId;
  public int              imageId;
  public String           name;

  public Date             tCreation;
  public Date             tLastMod;
  public Date             tLastAccess;
  public Date             tExpire;

  public int              level;       //short

  /** Used by KeePass internally, don't use */
  public int              flags;
}
