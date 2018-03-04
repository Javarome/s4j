/*
 * User: freds
 * Date: Nov 25, 2006
 * Time: 12:01:37 AM
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


public class AutoCompleteString {
    public AutoCompleteString() {
        maxMatches = 5;
    }

    public String test(String _text) {
        matches.clear();

        if (options.size() == 0) return "";

        int i = 0;
        while (i < options.size()) {
            if (options.get(i).equals(_text))// match with item i
                matches.add(options.get(i));
            i++;
        }

        if (matches.isEmpty())
            lastMatchPos = 0;
        else
            lastMatchPos = _text.length();

        return getFirstMatch();
    }

    public void setOptions(List<String> _options) {
        options = _options;
    }

    public String getOptions() {
        if (options.size() == 0) return "";

        int i = 0;
        StringBuffer text = new StringBuffer("");

        while (i < options.size()) {
            if (text.equals(""))// first match
                text.append(matches.get(i));
            else
                text.append(", ").append(matches.get(i));
            i++;
        }
        return text.toString();
    }

    public String getFirstMatch() {
        if (matches.size() > 0)
            return matches.get(0);
        else
            return "";
    }

    public void reset() {
        lastMatchPos = 0;
        matches.clear();
    }

    boolean hasMatch() {
        return matches.size() > 0;
    }

    private int lastMatchPos;

    private int maxMatches;

    private List<String> options;

    private List<String> matches = new ArrayList<String>();
}
