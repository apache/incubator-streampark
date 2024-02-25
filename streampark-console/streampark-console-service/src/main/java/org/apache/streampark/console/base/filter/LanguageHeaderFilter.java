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

package org.apache.streampark.console.base.filter;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * Parse the request, get the language type set by the user.
 */
public class LanguageHeaderFilter extends OncePerRequestFilter {
    private static final ThreadLocal<String> languageCache = new ThreadLocal<>();
    @Override
    protected void doFilterInternal(@NotNull final HttpServletRequest request,
                                    @NotNull final HttpServletResponse response,
                                    @NotNull final FilterChain filterChain) throws ServletException, IOException {
        
        String language = request.getHeader("Language");
        
        if (null != language) {
            languageCache.set(language);
        } else {
            languageCache.set(Locale.getDefault().getLanguage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    @NotNull
    public static String getLanguage() {
        return languageCache.get();
    }
}
