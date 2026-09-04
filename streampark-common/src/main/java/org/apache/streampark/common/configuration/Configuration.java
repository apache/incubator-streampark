/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.common.configuration;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable configuration snapshot with typed access and value origins.
 *
 * <p>Each key retains only its effective value. Source precedence is enforced while a snapshot is
 * built, so the result does not depend on the order in which different source types are added.
 * Within the same source type, the value added last wins. Conversion is deliberately deferred
 * until an option is read, which keeps parsing independent from the set of options available to a
 * particular module.
 */
public final class Configuration implements ReadableConfig, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String NULL_KEY = "key must not be null";
    private static final String NULL_OPTION = "option must not be null";
    private static final Configuration EMPTY = new Configuration(Collections.emptyMap());

    private final Map<String, Value> values;

    private Configuration(Map<String, Value> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /**
     * Returns the shared empty configuration snapshot.
     *
     * @return immutable empty configuration
     */
    public static Configuration empty() {
        return EMPTY;
    }

    /**
     * Creates an empty snapshot builder.
     *
     * @return new configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with every value and origin from a base snapshot.
     *
     * @param base immutable configuration to copy
     * @return new configuration builder
     */
    public static Builder builder(Configuration base) {
        return new Builder(base);
    }

    @Override
    public <T> T get(ConfigOption<T> option) {
        Optional<T> value = getOptional(option);
        if (value.isPresent()) {
            return value.get();
        }
        if (option.hasDefaultValue()) {
            return option.defaultValue();
        }
        throw new ConfigException("Required configuration option '" + option.key() + "' is missing");
    }

    @Override
    public <T> Optional<T> getOptional(ConfigOption<T> option) {
        Objects.requireNonNull(option, NULL_OPTION);
        // The canonical key always wins over fallbacks. Fallbacks are declaration-time migration
        // metadata and never copied into the effective snapshot.
        String resolvedKey = resolveKey(option);
        if (resolvedKey == null) {
            return Optional.empty();
        }
        Value value = values.get(resolvedKey);
        try {
            return Optional.of(option.convert(value.rawValue));
        } catch (RuntimeException e) {
            String rendered =
                option.sensitive() || SensitiveKeys.isSensitive(resolvedKey)
                    ? SensitiveKeys.MASK
                    : ConfigValueTypes.formatUnknown(value.rawValue);
            throw new ConfigException(
                "Invalid value '"
                    + rendered
                    + "' for configuration option '"
                    + option.key()
                    + "' from "
                    + value.origin,
                e);
        }
    }

    @Override
    public boolean contains(ConfigOption<?> option) {
        return resolveKey(Objects.requireNonNull(option, NULL_OPTION)) != null;
    }

    /**
     * Returns whether the snapshot contains an exact raw key.
     *
     * <p>This method does not inspect option fallback keys.
     *
     * @param key exact configuration key
     * @return whether the key is present
     */
    public boolean containsKey(String key) {
        return values.containsKey(Objects.requireNonNull(key, NULL_KEY));
    }

    /**
     * Returns an exact key as a string, without applying an option default or fallback.
     *
     * @param key exact configuration key
     * @return canonical string representation of the raw value
     * @throws ConfigException when the key is absent
     */
    public String getString(String key) {
        return getOptionalString(key)
            .orElseThrow(() -> new ConfigException("Required configuration key '" + key + "' is missing"));
    }

    /**
     * Returns an exact key as a string when present.
     *
     * @param key exact configuration key
     * @return canonical string representation, or an empty optional when absent
     */
    public Optional<String> getOptionalString(String key) {
        Value value = values.get(Objects.requireNonNull(key, NULL_KEY));
        return value == null
            ? Optional.empty()
            : Optional.of(ConfigValueTypes.formatUnknown(value.rawValue));
    }

    /**
     * Returns source metadata retained for the effective value of an exact key.
     *
     * @param key exact configuration key
     * @return effective value origin, or an empty optional when absent
     */
    public Optional<ConfigOrigin> origin(String key) {
        Value value = values.get(Objects.requireNonNull(key, NULL_KEY));
        return value == null ? Optional.empty() : Optional.of(value.origin);
    }

    /**
     * Returns entries below a namespace with the prefix removed from each key.
     *
     * <p>The returned snapshot retains the origin of each value. Prefix matching is literal so
     * callers should include the desired separator, for example {@code flink.option.}.
     *
     * @param prefix non-blank literal prefix
     * @return immutable configuration containing the matching entries
     */
    public Configuration subset(String prefix) {
        String normalized = requirePrefix(prefix);
        Map<String, Value> subset = new LinkedHashMap<>();
        values.forEach(
            (key, value) -> {
                if (key.startsWith(normalized)) {
                    subset.put(key.substring(normalized.length()), value);
                }
            });
        return subset.isEmpty() ? EMPTY : new Configuration(subset);
    }

    /**
     * Returns an immutable string representation of all effective entries.
     *
     * <p>This view is not redacted and must not be written to logs. Use {@link #toRedactedMap()} for
     * diagnostics.
     *
     * @return unredacted effective entries
     */
    public Map<String, String> toMap() {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(key, ConfigValueTypes.formatUnknown(value.rawValue)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns effective entries with values whose key looks sensitive masked.
     *
     * @return immutable diagnostic view
     */
    public Map<String, String> toRedactedMap() {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach(
            (key, value) -> result.put(
                key,
                SensitiveKeys.isSensitive(key)
                    ? SensitiveKeys.MASK
                    : ConfigValueTypes.formatUnknown(value.rawValue)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns whether the snapshot contains no entries.
     *
     * @return whether no effective values exist
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns the number of effective entries.
     *
     * @return effective entry count
     */
    public int size() {
        return values.size();
    }

    private String resolveKey(ConfigOption<?> option) {
        if (values.containsKey(option.key())) {
            return option.key();
        }
        for (String fallbackKey : option.fallbackKeys()) {
            if (values.containsKey(fallbackKey)) {
                return fallbackKey;
            }
        }
        return null;
    }

    private static String requirePrefix(String prefix) {
        String normalized = Objects.requireNonNull(prefix, "prefix must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        return normalized;
    }

    /**
     * Copies structured values into representations that cannot be changed through caller-owned
     * collections. Scalar configuration types are immutable and can be retained directly.
     */
    @SuppressWarnings("unchecked")
    static <T> T immutableValue(T value) {
        Objects.requireNonNull(value, "configuration value must not be null");
        if (value instanceof Map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            ((Map<?, ?>) value).forEach(
                (key, item) -> copy.put(immutableValue(key), immutableValue(item)));
            return (T) Collections.unmodifiableMap(copy);
        }
        if (value instanceof Collection) {
            List<Object> copy = new ArrayList<>(((Collection<?>) value).size());
            for (Object item : (Collection<?>) value) {
                copy.add(immutableValue(item));
            }
            return (T) Collections.unmodifiableList(copy);
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            Object copy = Array.newInstance(value.getClass().getComponentType(), length);
            for (int index = 0; index < length; index++) {
                Array.set(copy, index, immutableValue(Array.get(value, index)));
            }
            return (T) copy;
        }
        return value;
    }

    private static Serializable serializableValue(Object value) {
        Object immutable = immutableValue(value);
        if (!(immutable instanceof Serializable)) {
            throw new ConfigException(
                "Configuration value of type "
                    + immutable.getClass().getName()
                    + " is not serializable");
        }
        return (Serializable) immutable;
    }

    @Override
    public String toString() {
        return toRedactedMap().toString();
    }

    private static final class Value implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Serializable rawValue;
        private final ConfigOrigin origin;

        private Value(Object rawValue, ConfigOrigin origin) {
            this.rawValue = serializableValue(rawValue);
            this.origin = origin;
        }
    }

    /**
     * Builds an immutable configuration snapshot.
     *
     * <p>Higher {@link ConfigSource} values replace lower ones regardless of insertion order. A
     * later value replaces an earlier value only when both have the same source precedence.
     */
    public static final class Builder {

        private final Map<String, Value> values = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(Configuration base) {
            values.putAll(Objects.requireNonNull(base, "base configuration must not be null").values);
        }

        /**
         * Adds one named layer using the declared source precedence.
         *
         * <p>Entries are consumed immediately. Later layers of the same source replace earlier
         * layers, while lower-precedence sources cannot replace higher-precedence values.
         *
         * @param originName non-blank name retained in diagnostics
         * @param source source category used for precedence
         * @param entries raw configuration entries
         * @return this builder
         */
        public Builder add(String originName, ConfigSource source, Map<String, ?> entries) {
            ConfigOrigin origin = ConfigOrigin.of(source, originName);
            Objects.requireNonNull(entries, "configuration entries must not be null")
                .forEach((key, value) -> putRaw(key, value, origin));
            return this;
        }

        /**
         * Merges another snapshot while retaining each entry's original source metadata.
         *
         * @param configuration immutable snapshot to merge
         * @return this builder
         */
        public Builder add(Configuration configuration) {
            Objects.requireNonNull(configuration, "configuration must not be null").values
                .forEach(this::mergeValue);
            return this;
        }

        /**
         * Sets a validated runtime value for an option.
         *
         * @param option target option metadata
         * @param value non-null converted value
         * @param originName non-blank runtime origin retained in diagnostics
         * @param <T> option value type
         * @return this builder
         * @throws ConfigException when the value violates the option constraint
         */
        public <T> Builder set(ConfigOption<T> option, T value, String originName) {
            Objects.requireNonNull(option, NULL_OPTION).validate(value);
            mergeValue(
                option.key(),
                new Value(value, ConfigOrigin.of(ConfigSource.RUNTIME, originName)));
            return this;
        }

        /**
         * Removes an exact key from the builder.
         *
         * @param key exact key to remove
         * @return this builder
         */
        public Builder remove(String key) {
            values.remove(Objects.requireNonNull(key, NULL_KEY));
            return this;
        }

        /**
         * Produces the immutable configuration snapshot.
         *
         * @return immutable snapshot containing each effective value and its origin
         */
        public Configuration build() {
            return values.isEmpty() ? EMPTY : new Configuration(values);
        }

        private void putRaw(String key, Object value, ConfigOrigin origin) {
            String normalized = Objects.requireNonNull(key, "configuration key must not be null").trim();
            if (normalized.isEmpty()) {
                throw new ConfigException("Configuration key from " + origin + " must not be blank");
            }
            if (value == null) {
                throw new ConfigException("Configuration value for '" + normalized + "' from " + origin + " is null");
            }
            mergeValue(normalized, new Value(value, origin));
        }

        private void mergeValue(String key, Value candidate) {
            Value current = values.get(key);
            // ConfigSource declaration order defines precedence. Equality intentionally permits
            // the last layer within one source category to replace an earlier layer.
            if (current == null
                || candidate.origin.source().ordinal() >= current.origin.source().ordinal()) {
                values.put(key, candidate);
            }
        }
    }
}
