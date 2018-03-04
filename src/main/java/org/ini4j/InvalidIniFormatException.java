/*
 * Copyright 2005 [org.ini4j] Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ini4j;

import java.io.IOException;

public class InvalidIniFormatException extends IOException {
    /**
     * P�ld�ny k�pz�s adott �zenettel.
     * <p/>
     * Ez a konstruktor rendszerint �j exception gener�l�s�ra haszn�latos, amikor is
     * valamely felt�tel ellen�rz�se hib�t jelez.
     *
     * @param message hiba sz�veges megnevez�se
     */
    public InvalidIniFormatException(String message) {
        super(message);
    }

    /**
     * P�ld�ny k�pz�s adott kiv�lt� okkal.
     * <p/>
     * Ez a konstruktor minden tov�bbi sz�veges magyar�zat n�lk�l egy exception tov�bb ad�s�ra
     * szolg�l.
     *
     * @param cause a hib�t kiv�lt� exception
     */
    public InvalidIniFormatException(Throwable cause) {
        super();
        initCause(cause);
    }
}
