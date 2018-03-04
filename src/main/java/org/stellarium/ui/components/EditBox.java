/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:01:53 AM
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

import org.stellarium.ui.SglAccess;
import org.stellarium.ui.render.SColor;
import org.stellarium.ui.render.SFontIfc;
import org.stellarium.vecmath.Vector2i;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;


public class EditBox extends Button {

    EditBox activeEditBox;

    public EditBox(String _label, SFontIfc font) {
        label = new StelLabel(_label, font);
        setSize(label.getSize().plus(new Vector2i(4, 2)));
        text = _label;
        lastText = text;
        setPrompt();
        type += SGUI.CT_EDITBOX;
        autoFocus = true;
    }

    public EditBox() {
        this("", null);
    }

    public void mouseClicked(MouseEvent e) {
        if (!visible) return;
        if (e.getButton() == MouseEvent.BUTTON1) {
            int x = e.getX();
            int y = e.getY();
            if (isIn(x, y)) {
                setEditing(true);
                e.consume();
            } else if (isEditing) {
                setEditing(false);
            }
        }
        super.mouseClicked(e);
    }

    public void setOnReturnKeyCallback(StelCallback c) {
        onReturnKeyCallback = c;
    }

    public void setOnKeyCallback(StelCallback c) {
        onKeyCallback = c;
    }

    public void setOnAutoCompleteCallback(StelCallback c) {
        onAutoCompleteCallback = c;
    }

    public void draw() {
        if (!visible) return;

        // may have been snatched away!
        if (isEditing && focusing && !focus)
            setEditing(false);

        super.draw();
        SglAccess.glPushMatrix();
        SglAccess.glTranslatef(pos.i0, pos.i1, 0.f);
        scissor.push(pos, size);
        label.setPos(6, (size.i1 - label.getSizeY()) / 2 + 1);

        refreshLabel();
        if (isEditing) {
            label.draw();
        } else {
            final float fadedIntensity = 0.3f;
            label.draw(fadedIntensity);
        }

        scissor.pop();
        SglAccess.glPopMatrix();
    }

    public void setColorScheme(SColor baseColor, SColor textColor) {
        if (!guiColorSchemeMember) return;

        label.setColorScheme(baseColor, textColor);
        super.setColorScheme(baseColor, textColor);
    }

    public void setFont(SFontIfc f) {
        super.setFont(f);
        label.setFont(f);
    }

    public void setTextColor(SColor c) {
        super.setTextColor(c);
        label.setTextColor(c);
    }

    public void setPainter(Painter p) {
        super.setPainter(p);
        label.setPainter(p);
    }

    public void keyPressed(KeyEvent e) {
        if (!isEditing) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                if (autoComplete.hasMatch()) {
                    text = autoComplete.getFirstMatch();
                }
                history.add(text);
                if (onReturnKeyCallback != null) {
                    onReturnKeyCallback.execute();
                }
                e.consume();
                break;
            case KeyEvent.VK_TAB:
                if (autoComplete.hasMatch()) {
                    text = autoComplete.getFirstMatch();
                }
                break;
            case KeyEvent.VK_UP:
                text = history.prev();
                cursorPos = text.length();
                break;
            case KeyEvent.VK_DOWN:
                text = history.next();
                cursorPos = text.length();
                break;
            case KeyEvent.VK_LEFT:
                if (e.isControlDown()) {
                    cursorToPrevWord();
                } else if (cursorPos > 0) {
                    cursorPos--;
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (e.isControlDown()) {
                    cursorToNextWord();
                } else if (cursorPos < text.length()) {
                    cursorPos++;
                }
                break;
            case KeyEvent.VK_HOME:
                cursorPos = 0;
                break;
            case KeyEvent.VK_END:
                cursorPos = text.length();
                break;
            case KeyEvent.VK_DELETE:
                text = lastText;
                if (cursorPos < text.length()) {
                    text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
                }
                break;
            case KeyEvent.VK_BACK_SPACE:
                text = lastText;
                if (cursorPos > 0) {
                    cursorPos--;
                    text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                }
                break;
            case KeyEvent.VK_ESCAPE:
                setText("");
                break;
            case KeyEvent.VK_0:
            case KeyEvent.VK_1:
            case KeyEvent.VK_2:
            case KeyEvent.VK_3:
            case KeyEvent.VK_4:
            case KeyEvent.VK_5:
            case KeyEvent.VK_6:
            case KeyEvent.VK_7:
            case KeyEvent.VK_8:
            case KeyEvent.VK_9:
            case KeyEvent.VK_A:
            case KeyEvent.VK_B:
            case KeyEvent.VK_C:
            case KeyEvent.VK_D:
            case KeyEvent.VK_E:
            case KeyEvent.VK_F:
            case KeyEvent.VK_G:
            case KeyEvent.VK_H:
            case KeyEvent.VK_I:
            case KeyEvent.VK_J:
            case KeyEvent.VK_K:
            case KeyEvent.VK_L:
            case KeyEvent.VK_M:
            case KeyEvent.VK_N:
            case KeyEvent.VK_O:
            case KeyEvent.VK_P:
            case KeyEvent.VK_Q:
            case KeyEvent.VK_R:
            case KeyEvent.VK_S:
            case KeyEvent.VK_T:
            case KeyEvent.VK_U:
            case KeyEvent.VK_V:
            case KeyEvent.VK_W:
            case KeyEvent.VK_X:
            case KeyEvent.VK_Y:
            case KeyEvent.VK_Z:
            case 224:
            case 225:/* */
            case 254:
            case 255:
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_UNDERSCORE:
                text = lastText;
                String newtext = text.substring(0, cursorPos);
                newtext += e.getKeyCode();
                newtext += text.substring(cursorPos);
                text = newtext;
                cursorPos++;
        }
    }

    public void setVisible(boolean _visible) {
        label.setVisible(_visible);
        super.setVisible(_visible);
        draw();
    }

    public void setAutoCompleteOptions(List<String> _autocomplete) {
        autoComplete.setOptions(_autocomplete);
    }

    public String getAutoCompleteOptions() {
        return autoComplete.getOptions();
    }

    public String getText() {
        return text;
    }

    public void setEditing(boolean _b) {
        if (isEditing) {
            isEditing = true;
            cursorVisible = true;
            autoComplete.reset();

            if (activeEditBox != this && activeEditBox != null) {
                activeEditBox.setEditing(false);
            }
            activeEditBox = this;

            //    	SDL_SetTimer(0, NULL);
            //                SDL_SetTimer(600, (SDL_TimerCallback) toggleBlink);
        } else {
            isEditing = false;
            cursorVisible = false;
            autoComplete.reset();
            activeEditBox = null;
            //                SDL_SetTimer(0, null);
        }
    }

    public void refreshLabel() {
        final String EDITBOX_CURSOR = "\7";
        if (!isEditing) label.setLabel(prompt + text);
        else {
            if (!cursorVisible) {
                label.setLabel(prompt + text);
            } else {
                if (cursorPos == text.length())// at end
                    label.setLabel(prompt + text + EDITBOX_CURSOR);
                else
                    label.setLabel(prompt + text.substring(0, cursorPos) + EDITBOX_CURSOR + text.substring(cursorPos));
            }
        }
    }

    final String EDITBOX_DEFAULT_PROMPT = "> ";

    public void setPrompt(String p) {
        if (p == null)
            prompt = EDITBOX_DEFAULT_PROMPT;
        else
            prompt = p;
    }

    public void setPrompt() {
        setPrompt("");
    }

    public void setText(String _text) {
        text = _text;
        lastText = text;
        cursorPos = _text.length();
    }

    public void clearText() {
        setText("");
    }

    public String getDefaultPrompt() {
        return EDITBOX_DEFAULT_PROMPT;
    }

    public int getLastKey() {
        return lastKey;
    }

    public void setAutoFocus(boolean _b) {
        autoFocus = _b;
    }

    public boolean getAutoFocus() {
        return autoFocus;
    }

    protected StelCallback onReturnKeyCallback;

    protected StelCallback onKeyCallback;

    protected StelCallback onAutoCompleteCallback;

    protected AutoCompleteString autoComplete;

    protected History history;

    protected void cursorToNextWord() {
        while (cursorPos < text.length()) {
            cursorPos++;
            if (text.charAt(cursorPos) == ' ') {
                if (cursorPos + 1 < text.length() && text.charAt(cursorPos + 1) != ' ') {
                    cursorPos++;
                    break;
                }
            }
        }
    }

    protected void cursorToPrevWord() {
        while (cursorPos > 0) {
            cursorPos--;
            // cater for the cursor at the start of the word already
            while (cursorPos > 0 && text.charAt(cursorPos) == ' ' && text.charAt(cursorPos - 1) == ' ')
                cursorPos--;

            if (text.charAt(cursorPos) == ' ' && text.charAt(cursorPos + 1) != ' ') {
                cursorPos++;
                break;
            }
        }
    }

    protected StelLabel label;

    protected boolean isEditing;

    protected String text;

    protected int cursorPos;

    protected String lastText;

    protected String prompt;

    protected int lastKey;

    protected boolean autoFocus;

    protected boolean cursorVisible;
}
