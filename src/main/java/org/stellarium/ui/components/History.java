/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:01:20 AM
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

import java.util.ArrayList;
import java.util.List;


public class History {
    public History(int _items) {
        if (_items == 0) {
            maxItems = 1;
        } else {
            maxItems = _items;
        }
    }

    public void clear() {
        pos = 0;
        history.clear();
    }

    public void add(String _text) {
        if (_text == null || _text.length() == 0) return;

        if (history.size() == maxItems) {
            history.remove(0);
        }
        history.add(_text);
        pos = history.size();
    }

    public String prev() {
        if (pos > 0) {
            pos--;
            return history.get(pos);
        }
        pos = -1;
        return "";
    }

    public String next() {
        if (pos < history.size() - 1) {
            pos++;
            return history.get(pos);
        }
        pos = history.size();
        return "";
    }

    private int maxItems;

    private int pos;

    private List<String> history = new ArrayList<String>();
}
