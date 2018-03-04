/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:03:52 AM
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
package org.stellarium.ui.components;

import org.stellarium.ui.render.SFontIfc;
import org.stellarium.ui.render.STexture;


public class StdDlgWin extends StdWin {
    public StdDlgWin(String _title, STexture _header_tex, SFontIfc _winfont, int headerSize) {
        super(_title, _header_tex, _winfont, headerSize);
    }

    public StdDlgWin(String someLabel) {
        this(someLabel, null, null, 18);
    }

    public void setDialogCallback(StelCallback c) {
        onCompleteCallback = c;
    }

    public void MessageBox(String _title, String _prompt, int _buttons, String _ID) {

    }

    public void InputBox(String _title, String _prompt, String _ID) {

    }

    public void setOnCompleteCallback(StelCallback c) {
        onCompleteCallback = c;
    }

    public String getLastID() {
        return lastID;
    }

    public int getLastType() {
        return lastType;
    }

    public int getLastButton() {
        return lastButton;
    }

    public String getLastInput() {
        return lastInput;
    }

    private StelCallback onCompleteCallback;

    private void resetResponse() {

    }

    private void arrangeButtons() {

    }

    private void onInputReturnKey() {

    }

    private void onFirstBt() {

    }

    private void onSecondBt() {

    }

    private LabeledButton firstBt, secondBt;

    private TextLabel messageLabel;

    private EditBox inputEdit;

    private STexture blankIcon, questionIcon, alertIcon;

    private StelPicture picture;

    private String originalTitle;

    private boolean hasIcon;

    private int numBtns;

    private int firstBtType, secondBtType;

    private String lastID;

    private int lastType;

    private int lastButton;

    private String lastInput;
}
