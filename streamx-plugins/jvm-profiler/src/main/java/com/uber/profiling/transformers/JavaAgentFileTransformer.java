/*
 * Copyright (c) 2018 Uber Technologies, Inc.
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

package com.uber.profiling.transformers;

import com.uber.profiling.util.AgentLogger;
import com.uber.profiling.util.ClassAndMethod;
import com.uber.profiling.util.ClassAndMethodFilter;
import com.uber.profiling.util.ClassMethodArgument;
import com.uber.profiling.util.ClassMethodArgumentFilter;
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
    private static final AgentLogger logger = AgentLogger.getLogger(JavaAgentFileTransformer.class.getName());

    private ClassAndMethodFilter durationProfilingFilter;
    private ClassMethodArgumentFilter argumentFilterProfilingFilter;

    public JavaAgentFileTransformer(List<ClassAndMethod> durationProfiling, List<ClassMethodArgument> argumentProfiling) {
        this.durationProfilingFilter = new ClassAndMethodFilter(durationProfiling);
        this.argumentFilterProfilingFilter = new ClassMethodArgumentFilter(argumentProfiling);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if (className == null || className.isEmpty()) {
                logger.debug("Hit null or empty class name");
                return null;
            }
            return transformImpl(loader, className, classfileBuffer);
        } catch (Throwable ex) {
            logger.warn("Failed to transform class " + className, ex);
            return classfileBuffer;
        }
    }

    private byte[] transformImpl(ClassLoader loader, String className, byte[] classfileBuffer) {
        if (durationProfilingFilter.isEmpty()
                && argumentFilterProfilingFilter.isEmpty()) {
            return null;
        }

        String normalizedClassName = className.replaceAll("/", ".");
        logger.debug("Checking class for transform: " + normalizedClassName);

        if (!durationProfilingFilter.matchClass(normalizedClassName)
                && !argumentFilterProfilingFilter.matchClass(normalizedClassName)) {
            return null;
        }

        byte[] byteCode;

        logger.info("Transforming class: " + normalizedClassName);

        try {
            ClassPool classPool = new ClassPool();
            classPool.appendClassPath(new LoaderClassPath(loader));
            final CtClass ctClass;
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(classfileBuffer)) {
                ctClass = classPool.makeClass(byteArrayInputStream);
            }
            
            CtMethod[] ctMethods = ctClass.getDeclaredMethods();
            for (CtMethod ctMethod : ctMethods) {
                boolean enableDurationProfiling = durationProfilingFilter.matchMethod(ctClass.getName(), ctMethod.getName());
                List<Integer> enableArgumentProfiler = argumentFilterProfilingFilter.matchMethod(ctClass.getName(), ctMethod.getName());
                transformMethod(normalizedClassName, ctMethod, enableDurationProfiling, enableArgumentProfiler);
            }

            byteCode = ctClass.toBytecode();
            ctClass.detach();

        } catch (Throwable ex) {
            ex.printStackTrace();
            logger.warn("Failed to transform class: " + normalizedClassName, ex);
            byteCode = null;
        }

        return byteCode;
    }

    private void transformMethod(String normalizedClassName, CtMethod method, boolean enableDurationProfiling, List<Integer> argumentsForProfile) {
        if (method.isEmpty()) {
            logger.info("Ignored empty class method: " + method.getLongName());
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
                    sb.append(String.format("try{com.uber.profiling.transformers.MethodProfilerStaticProxy.collectMethodArgument(\"%s\", \"%s\", %s, String.valueOf($%s));}catch(Throwable ex){ex.printStackTrace();}",
                            normalizedClassName,
                            method.getName(),
                            argument,
                            argument));
                } else {
                    sb.append(String.format("try{com.uber.profiling.transformers.MethodProfilerStaticProxy.collectMethodArgument(\"%s\", \"%s\", %s, \"\");}catch(Throwable ex){ex.printStackTrace();}",
                            normalizedClassName,
                            method.getName(),
                            argument,
                            argument));
                }
            }

            sb.append("}");

            method.insertBefore(sb.toString());

            if (enableDurationProfiling) {
                method.insertAfter("{" +
                        "durationMillis_java_agent_instrument = System.currentTimeMillis() - startMillis_java_agent_instrument;" +
                        String.format("try{com.uber.profiling.transformers.MethodProfilerStaticProxy.collectMethodDuration(\"%s\", \"%s\", durationMillis_java_agent_instrument);}catch(Throwable ex){ex.printStackTrace();}", normalizedClassName, method.getName()) +
                        // "System.out.println(\"Method Executed in ms: \" + durationMillis);" +
                        "}");
            }

            logger.info("Transformed class method: " + method.getLongName() + ", durationProfiling: " + enableDurationProfiling + ", argumentProfiling: " + argumentsForProfile);
        } catch (Throwable ex) {
            ex.printStackTrace();
            logger.warn("Failed to transform class method: " + method.getLongName(), ex);
        }
    }
}
