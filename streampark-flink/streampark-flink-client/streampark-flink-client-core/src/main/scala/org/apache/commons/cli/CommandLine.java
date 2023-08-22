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

package org.apache.commons.cli;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class CommandLine implements Serializable {

    /** The serial version UID. */
    private static final long serialVersionUID = 1L;

    /** The unrecognized options/arguments */
    private final List<String> args = new LinkedList<>();

    /** The processed options */
    private final List<Option> options = new ArrayList<>();

    /**
     * Creates a command line.
     */
    protected CommandLine() {
        // nothing to do
    }

    /**
     * Retrieve the map of values associated to the option. This is convenient for options specifying Java properties like
     * <code>-Dparam1=value1
     * -Dparam2=value2</code>. The first argument of the option is the key, and the 2nd argument is the value. If the option
     * has only one argument ({@code -Dfoo}) it is considered as a boolean flag and the value is {@code "true"}.
     *
     * @param option name of the option.
     * @return The Properties mapped by the option, never {@code null} even if the option doesn't exists.
     * @since 1.5.0
     */
    public Properties getOptionProperties(final Option option) {
        final Properties props = new Properties();

        for (final Option processedOption : options) {
            if (processedOption.equals(option)) {
                final List<String> values = processedOption.getValuesList();
                if (values.size() >= 2) {
                    // use the first 2 arguments as the key/value pair
                    props.put(values.get(0), values.get(1));
                } else if (values.size() == 1) {
                    // no explicit value, handle it as a boolean
                    props.put(values.get(0), "true");
                }
            }
        }

        return props;
    }

    /**
     * Retrieve the first argument, if any, of this option.
     *
     * @param option the name of the option.
     * @return Value of the argument if option is set, and has an argument, otherwise null.
     * @since 1.5.0
     */
    public String getOptionValue(final Option option) {
        if (option == null) {
            return null;
        }
        final String[] values = getOptionValues(option);
        return values == null ? null : values[0];
    }

    /**
     * Retrieve the first argument, if any, of an option.
     *
     * @param option name of the option.
     * @param defaultValue is the default value to be returned if the option is not specified.
     * @return Value of the argument if option is set, and has an argument, otherwise {@code defaultValue}.
     * @since 1.5.0
     */
    public String getOptionValue(final Option option, final String defaultValue) {
        final String answer = getOptionValue(option);
        return answer != null ? answer : defaultValue;
    }

    /**
     * Retrieves the array of values, if any, of an option.
     *
     * @param option string name of the option.
     * @return Values of the argument if option is set, and has an argument, otherwise null.
     * @since 1.5.0
     */
    public String[] getOptionValues(final Option option) {
        final List<String> values = new ArrayList<>();

        for (final Option processedOption : options) {
            if (processedOption.equals(option)) {
                values.addAll(processedOption.getValuesList());
            }
        }

        return values.isEmpty() ? null : values.toArray(new String[values.size()]);
    }

    /**
     * Return a version of this {@code Option} converted to a particular type.
     *
     * @param opt the name of the option.
     * @return the value parsed into a particular object.
     * @throws ParseException if there are problems turning the option value into the desired type
     * @see PatternOptionBuilder
     * @since 1.5.0
     */
    public Object getParsedOptionValue(final char opt) throws ParseException {
        return getParsedOptionValue(String.valueOf(opt));
    }

    /**
     * Return a version of this {@code Option} converted to a particular type.
     *
     * @param option the name of the option.
     * @return the value parsed into a particular object.
     * @throws ParseException if there are problems turning the option value into the desired type
     * @see PatternOptionBuilder
     * @since 1.5.0
     */
    public Object getParsedOptionValue(final Option option) throws ParseException {
        if (option == null) {
            return null;
        }
        final String res = getOptionValue(option);
        if (res == null) {
            return null;
        }
        return TypeHandler.createValue(res, option.getType());
    }


    /**
     * Tests to see if an option has been set.
     *
     * @param opt the option to check.
     * @return true if set, false if not.
     * @since 1.5.0
     */
    public boolean hasOption(final Option opt) {
        return options.contains(opt);
    }


    /**
     * Return a version of this {@code Option} converted to a particular type.
     *
     * @param opt the name of the option.
     * @return the value parsed into a particular object.
     * @throws ParseException if there are problems turning the option value into the desired type
     * @see PatternOptionBuilder
     * @since 1.2
     */
    public Object getParsedOptionValue(final String opt) throws ParseException {
        return getParsedOptionValue(resolveOption(opt));
    }


    /**
     * Retrieves the option object given the long or short option as a String
     *
     * @param opt short or long name of the option.
     * @return Canonicalized option.
     */
    private Option resolveOption(String opt) {
        opt = Util.stripLeadingHyphens(opt);
        for (final Option option : options) {
            if (opt.equals(option.getOpt()) || opt.equals(option.getLongOpt())) {
                return option;
            }

        }
        return null;
    }

}
