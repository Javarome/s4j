package org.stellarium.ui.components;

/*
* Stellarium for Java
* Copyright (c) 2005-2006 Jerome Beau
*
* Java adaptation of <a href="http://www.stellarium.org">Stellarium</a>
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

// A double to nearest int conversion routine for the systems which
// are not C99 conformant

public class SGUI {
    public static final int SCROLL_SIZE = 15;

    public static final int CT_COMPONENTBASE = 0x0000;

    public static final int CT_LABEL = 0x1000;

    public static final int CT_CURSORBAR = 0x2000;

    public static final int CT_CALLBACK = 0x3000;

    public static final int CT_STRINGLIST = 0x0010;

    public static final int CT_PICTURE = 0x0020;

    public static final int CT_BUTTON = 0x0040;

    public static final int CT_TABHEADER = 0x0001;

    public static final int CT_EDITBOX = 0x0080;

    public static final int CT_CONTAINER = 0x0100;

    public static final int BT_NOTSET = 0;

    public static final int BT_YES = 1;

    public static final int BT_NO = 2;

    public static final int BT_CANCEL = 4;

    public static final int BT_OK = 8;

    public static final int BT_ICON_BLANK = 256;

    public static final int BT_ICON_QUESTION = 512;

    public static final int BT_ICON_ALERT = 1024;

    public static final int STDDLGWIN_MSG = 0;

    public static final int STDDLGWIN_INPUT = 1;

    public static final String UNKNOWN_OBSERVATORY = "Unknown observatory";

    public boolean cursorVisible;

    public EditBox activeEditBox;

}