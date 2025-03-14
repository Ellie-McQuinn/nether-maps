/*
The MIT License (MIT)
Copyright (c) 2020 Joseph Bettendorff aka "Commoble"

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

// DataFixerUpper is Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT license.
*/

package com.duck.elliemcquinn.nethermaps;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.NullObject;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import org.slf4j.Logger;


/**
 * Helpers for creating configs and defining complex objects in configs
 * <p>
 * Stripped down a little to remove methods this project doesn't need.
 * <br>
 * If you would like to use this please see Commoble's <a href="https://github.com/Commoble/databuddy/blob/main/src/main/java/net/commoble/databuddy/config/ConfigHelper.java">upstream</a>.
 * </p>
 */
public class ConfigHelper
{
    private ConfigHelper() {} // utility class

    static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Define a config value for a complex object.
     * @param <T> The type of the thing in the config we are making a listener for
     * @param builder Builder to build configs with
     * @param name The name of the field in your config that will hold objects of this type
     * @param codec A Codec for de/serializing your object type.
     * @param defaultObject The default instance of your config field. The given codec must be able to serialize this;
     * if it cannot, an exception will be intentionally thrown the first time the config attempts to load.
     * If the codec fails to deserialize the config field at a later time, an error message will be logged and this default instance will be used instead.
     * @return A reload-sensitive wrapper around your config object value. Use ConfigObject#get to get the most up-to-date object.
     */
    public static <T> ConfigObject<T> defineObject(ModConfigSpec.Builder builder, String name, Codec<T> codec, T defaultObject)
    {
        DataResult<Object> encodeResult = codec.encodeStart(TomlConfigOps.INSTANCE, defaultObject);
        Object encodedObject = encodeResult.getOrThrow(s -> new IllegalArgumentException(String.format("Unable to encode default value %s: %s", defaultObject, s)));
        ConfigValue<Object> value = builder.define(name, encodedObject);
        return new ConfigObject<>(value, codec, defaultObject, encodedObject);
    }

    /**
     * A config-reload-sensitive wrapper around a config field for a complex object
     **/
    public static class ConfigObject<T> implements Supplier<T>
    {
        private @Nonnull final ConfigValue<Object> value;
        private @Nonnull final Codec<T> codec;
        private @Nonnull Object cachedObject;
        private @Nonnull T parsedObject;
        private @Nonnull T defaultObject;

        private ConfigObject(ConfigValue<Object> value, Codec<T> codec, T defaultObject, Object encodedDefaultObject)
        {
            this.value = value;
            this.codec = codec;
            this.defaultObject = defaultObject;
            this.parsedObject = defaultObject;
            this.cachedObject = encodedDefaultObject;
        }

        @Override
        @Nonnull
        public T get()
        {
            Object freshObject = this.value.get();
            if (!Objects.equals(this.cachedObject, freshObject))
            {
                this.cachedObject = freshObject;
                this.parsedObject = this.getReparsedObject(freshObject);
            }
            return this.parsedObject;
        }

        /**
         * Sets the config field to a new value and saves the config
         * @param value Value to serialize to the config. If object cannot be serialized, an error will be logged and no change will occur.
         */
        public void set(T value)
        {
            this.codec.encodeStart(TomlConfigOps.INSTANCE, value)
                    .resultOrPartial(e -> LOGGER.error("Config failure: Could not save value {} due to encoding error: {}", value, e))
                    .ifPresent(serializedObject -> {
                        this.value.set(serializedObject);
                        this.value.save();
                        this.parsedObject = value;
                        this.cachedObject = serializedObject;
                    });
        }

        private T getReparsedObject(Object obj)
        {
            DataResult<T> parseResult = this.codec.parse(TomlConfigOps.INSTANCE, obj);
            return parseResult.mapOrElse(
                    result -> result,
                    failure ->
                    {
                        LOGGER.error("Config failure: Using default config value due to parsing error: {}", failure.message());
                        return this.defaultObject;
                    });
        }
    }

    /**
     * DynamicOps for using {@link Codec}s to load objects from configs.
     * Particularly helpful for loading maps or maplike objects.
     */
    public static class TomlConfigOps implements DynamicOps<Object>
    {
        /** instance **/
        public static final TomlConfigOps INSTANCE = new TomlConfigOps();

        @Override
        public Object empty()
        {
            return NullObject.NULL_OBJECT;
        }

        @Override
        public <U> U convertTo(DynamicOps<U> outOps, Object input)
        {
            if (input instanceof Config)
            {
                return this.convertMap(outOps, input);
            }
            if (input instanceof Collection)
            {
                return this.convertList(outOps, input);
            }
            if (input == null || input instanceof NullObject)
            {
                return outOps.empty();
            }
            if (input instanceof Enum)
            {
                return outOps.createString(((Enum<?>)input).name());
            }
            if (input instanceof Temporal)
            {
                return outOps.createString(input.toString());
            }
            if (input instanceof String s)
            {
                return outOps.createString(s);
            }
            if (input instanceof Boolean b)
            {
                return outOps.createBoolean(b);
            }
            if (input instanceof Number n)
            {
                return outOps.createNumeric(n);
            }
            throw new UnsupportedOperationException("TomlConfigOps was unable to convert toml value: " + input);
        }

        @Override
        public DataResult<Number> getNumberValue(Object input)
        {
            return input instanceof Number n
                    ? DataResult.success(n)
                    : DataResult.error(() -> "Not a number: " + input);
        }

        @Override
        public DataResult<Boolean> getBooleanValue(Object input)
        {
            if (input instanceof Boolean b)
                return DataResult.success(b);
            else if (input instanceof Number n) // ensures we don't reset old configs that were serializing 1/0 for bool fields
            {
                return DataResult.success(n.intValue() > 0);
            }
            else
                return DataResult.error(() -> "Not a boolean: " + input);
        }

        @Override
        public Object createBoolean(boolean value)
        {
            return Boolean.valueOf(value);
        }

        @Override
        public boolean compressMaps()
        {
            return false;
        }

        @Override
        public Object createNumeric(Number i)
        {
            return i;
        }

        @Override
        public DataResult<String> getStringValue(Object input)
        {
            if (input instanceof Config || input instanceof Collection)
            {
                return DataResult.error(() -> "Not a string: " + input);
            }
            else
            {
                return DataResult.success(String.valueOf(input));
            }
        }

        @Override
        public Object createString(String value)
        {
            return value;
        }

        @Override
        public DataResult<Object> mergeToList(Object list, List<Object> values)
        {
            // default mergeToList returns the null object if list is empty;
            // toml doesn't support null values so we need to convert to an empty list
            return DynamicOps.super.mergeToList(list, values)
                    .map(obj -> obj == this.empty()
                            ? new ArrayList<>()
                            : obj);
        }

        @Override
        public DataResult<Object> mergeToList(Object list, Object value)
        {
            if (!(list instanceof Collection) && list != this.empty())
            {
                return DataResult.error(() -> "mergeToList called with not a list: " + list, list);
            }
            final Collection<Object> result = new ArrayList<>();
            if (list != this.empty())
            {
                @SuppressWarnings("unchecked")
                Collection<Object> listAsCollection = (Collection<Object>)list;
                result.addAll(listAsCollection);
            }
            result.add(value);
            return DataResult.success(result);
        }

        @Override
        public DataResult<Object> mergeToMap(Object map, Object key, Object value)
        {
            if (!(map instanceof Config) && map != this.empty())
            {
                return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
            }
            DataResult<String> stringResult = this.getStringValue(key);
            Optional<DataResult.Error<String>> badResult = stringResult.error();
            if (badResult.isPresent())
            {
                return DataResult.error(() -> "key is not a string: " + key, map);
            }
            return stringResult.flatMap(s ->{

                final Config output = TomlFormat.newConfig();
                if (map != this.empty())
                {
                    Config oldConfig = (Config)map;
                    output.addAll(oldConfig);
                }
                output.add(s, value);
                return DataResult.success(output);
            });
        }

        @Override
        public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object input)
        {
            if (!(input instanceof Config))
            {
                return DataResult.error(() -> "Not a Config: " + input);
            }
            final Config config = (Config)input;
            return DataResult.success(config.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())));
        }

        @Override
        public Object createMap(Stream<Pair<Object, Object>> map)
        {
            final Config result = TomlFormat.newConfig();
            map.forEach(p -> result.add(this.getStringValue(p.getFirst()).getOrThrow(), p.getSecond()));
            return result;
        }

        @Override
        public DataResult<Stream<Object>> getStream(Object input)
        {
            if (input instanceof Collection)
            {
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>)input;
                return DataResult.success(collection.stream());
            }
            return DataResult.error(() -> "Not a collection: " + input);
        }

        @Override
        public Object createList(Stream<Object> input)
        {
            return input.toList();
        }

        @Override
        public Object remove(Object input, String key)
        {
            if (input instanceof Config oldConfig)
            {
                final Config result = TomlFormat.newConfig();
                oldConfig.entrySet().stream()
                        .filter(entry -> !Objects.equals(entry.getKey(), key))
                        .forEach(entry -> result.add(entry.getKey(), entry.getValue()));
                return result;
            }
            return input;
        }

        @Override
        public String toString()
        {
            return "TOML";
        }
    }
}