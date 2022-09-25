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

package org.apache.streampark.plugin.profiling.transformer;

import org.apache.streampark.plugin.profiling.util.AgentLogger;
import org.apache.streampark.plugin.profiling.util.ClassAndMethod;
import org.apache.streampark.plugin.profiling.util.ClassAndMethodFilter;
import org.apache.streampark.plugin.profiling.util.ClassMethodArgument;
import org.apache.streampark.plugin.profiling.util.ClassMethodArgumentFilter;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

public class JavaAgentFileTransformer implements ClassFileTransformer {
    private static final AgentLogger LOGGER =
        AgentLogger.getLogger(JavaAgentFileTransformer.class.getName());

    private final ClassAndMethodFilter durationProfilingFilter;
    private final ClassMethodArgumentFilter argumentFilterProfilingFilter;

    public JavaAgentFileTransformer(
        List<ClassAndMethod> durationProfiling, List<ClassMethodArgument> argumentProfiling) {
        this.durationProfilingFilter = new ClassAndMethodFilter(durationProfiling);
        this.argumentFilterProfilingFilter = new ClassMethodArgumentFilter(argumentProfiling);
    }

    @Override
    public byte[] transform(
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer)
        throws IllegalClassFormatException {
        try {
            if (className == null || className.isEmpty()) {
                LOGGER.debug("Hit null or empty class name");
                return null;
            }
            return transformImpl(loader, className, classfileBuffer);
        } catch (Throwable ex) {
            LOGGER.warn("Failed to transform class " + className, ex);
            return classfileBuffer;
        }
    }

    private byte[] transformImpl(ClassLoader loader, String className, byte[] classfileBuffer) {
        if (durationProfilingFilter.isEmpty() && argumentFilterProfilingFilter.isEmpty()) {
            return null;
        }

        String normalizedClassName = className.replaceAll("/", ".");
        LOGGER.debug("Checking class for transform: " + normalizedClassName);

        if (!durationProfilingFilter.matchClass(normalizedClassName)
            && !argumentFilterProfilingFilter.matchClass(normalizedClassName)) {
            return null;
        }

        byte[] byteCode;

        LOGGER.info("Transforming class: " + normalizedClassName);

        try {
            ClassPool classPool = new ClassPool();
            classPool.appendClassPath(new LoaderClassPath(loader));
            final CtClass ctClass;
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(classfileBuffer)) {
                ctClass = classPool.makeClass(byteArrayInputStream);
            }

            CtMethod[] ctMethods = ctClass.getDeclaredMethods();
            for (CtMethod ctMethod : ctMethods) {
                boolean enableDurationProfiling =
                    durationProfilingFilter.matchMethod(ctClass.getName(), ctMethod.getName());
                List<Integer> enableArgumentProfiler =
                    argumentFilterProfilingFilter.matchMethod(ctClass.getName(), ctMethod.getName());
                transformMethod(
                    normalizedClassName, ctMethod, enableDurationProfiling, enableArgumentProfiler);
            }

            byteCode = ctClass.toBytecode();
            ctClass.detach();

        } catch (Throwable ex) {
            ex.printStackTrace();
            LOGGER.warn("Failed to transform class: " + normalizedClassName, ex);
            byteCode = null;
        }

        return byteCode;
    }

    private void transformMethod(
        String normalizedClassName,
        CtMethod method,
        boolean enableDurationProfiling,
        List<Integer> argumentsForProfile) {
        if (method.isEmpty()) {
            LOGGER.info("Ignored empty class method: " + method.getLongName());
            return;
        }

        if (!enableDurationProfiling && argumentsForProfile.isEmpty()) {
            return;
        }

        try {
            if (enableDurationProfiling) {
                method.addLocalVariable("startMillis_java_agent_instrument", CtClass.longType);
                method.addLocalVariable("durationMillis_java_agent_instrument", CtClass.longType);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");

            if (enableDurationProfiling) {
                sb.append("startMillis_java_agent_instrument = System.currentTimeMillis();");
            }

            for (Integer argument : argumentsForProfile) {
                if (argument >= 1) {
                    sb.append(
                        String.format(
                            "try{org.apache.streampark.plugin.profiling.transformer.MethodProfilerStaticProxy.collectMethodArgument(\"%s\", \"%s\", %s, String.valueOf($%s));}catch(Throwable ex){ex.printStackTrace();}",
                            normalizedClassName, method.getName(), argument, argument));
                } else {
                    sb.append(
                        String.format(
                            "try{org.apache.streampark.plugin.profiling.transformer.MethodProfilerStaticProxy.collectMethodArgument(\"%s\", \"%s\", %s, \"\");}catch(Throwable ex){ex.printStackTrace();}",
                            normalizedClassName, method.getName(), argument, argument));
                }
            }

            sb.append("}");

            method.insertBefore(sb.toString());

            if (enableDurationProfiling) {
                method.insertAfter(
                    "{"
                        + "durationMillis_java_agent_instrument = System.currentTimeMillis() - startMillis_java_agent_instrument;"
                        + String.format(
                        "try{org.apache.streampark.plugin.profiling.transformer.MethodProfilerStaticProxy.collectMethodDuration(\"%s\", \"%s\", durationMillis_java_agent_instrument);}catch(Throwable ex){ex.printStackTrace();}",
                        normalizedClassName, method.getName())
                        +
                        "}");
            }

            LOGGER.info(
                "Transformed class method: "
                    + method.getLongName()
                    + ", durationProfiling: "
                    + enableDurationProfiling
                    + ", argumentProfiling: "
                    + argumentsForProfile);
        } catch (Throwable ex) {
            ex.printStackTrace();
            LOGGER.warn("Failed to transform class method: " + method.getLongName(), ex);
        }
    }
}
