/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package git.prayoadmii.viaproxyplus.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Collection;
import java.util.Properties;

public class I18n {

    private static final String DEFAULT_LOCALE = "en_US";
    private static final Properties ENGLISH_STRINGS = new Properties();

    static {
        try (InputStream inputStream = I18n.class.getClassLoader().getResourceAsStream("assets/viaproxy/language/en_US.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("English translation file not found");
            }
            ENGLISH_STRINGS.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load English strings", e);
        }
    }

    public static String get(final String key) {
        return getSpecific(DEFAULT_LOCALE, key);
    }

    public static String get(final String key, String... args) {
        return String.format(getSpecific(DEFAULT_LOCALE, key), (Object[]) args);
    }

    public static String getSpecific(final String locale, final String key) {
        String value = ENGLISH_STRINGS.getProperty(key);
        if (value == null) {
            return "Missing translation for key: " + key;
        }
        return value;
    }

    public static String getCurrentLocale() {
        return DEFAULT_LOCALE;
    }

    public static void setLocale(final String locale) {
        // English-only build; locale switching is intentionally unavailable.
    }

    public static Collection<String> getAvailableLocales() {
        return Collections.singleton(DEFAULT_LOCALE);
    }

}
