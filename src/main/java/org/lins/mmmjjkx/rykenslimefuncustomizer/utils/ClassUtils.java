/*
 * RykenSlimefunCustomizer
 * Copyright (C) 2026 lijinhong11(mmmjjjkx) and balugaq
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.lins.mmmjjkx.rykenslimefuncustomizer.utils;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ClassUtils {
    private static final Map<String, Class<?>> cache = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> generateClass(
            Class<T> extendClass,
            String centerName,
            Class<?>[] interfaces,
            @Nullable Function<DynamicType.Builder<?>, DynamicType.Builder<?>> delegation) {

        // Preserve the full current generated class name when chaining attributes.
        // Removing/replacing a common suffix caused distinct attribute combinations
        // to collapse onto the same cached ByteBuddy class name.
        String finalClassName = extendClass.getSimpleName() + centerName;
        if (cache.containsKey(finalClassName)) {
            return (Class<? extends T>) cache.get(finalClassName);
        }

        DynamicType.Builder<?> builder = new ByteBuddy().subclass(extendClass);

        if (delegation != null) {
            builder = delegation.apply(builder);
        }

        builder = builder.implement(interfaces).name(finalClassName);

        Class<?> clazz;
        try (DynamicType.Unloaded<?> unloaded = builder.make()) {
            clazz = unloaded.load(extendClass.getClassLoader()).getLoaded();
        }

        cache.put(finalClassName, clazz);
        return (Class<? extends T>) clazz;
    }
}
