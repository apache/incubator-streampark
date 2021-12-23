/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.common.util

import org.apache.commons.lang3.ArrayUtils

import java.io.{IOException, PrintWriter, StringWriter}
import java.util
import java.util.concurrent.{CompletionException, ExecutionException}
import java.util.function.Predicate
import java.util.{Optional, StringTokenizer}
import javax.annotation.Nullable


/**
 * A collection of utility functions for dealing with exceptions and exception workflows.
 */
object ExceptionUtils {
  /** The stringified representation of a null exception reference. */
  val STRINGIFIED_NULL_EXCEPTION = "(null)"

  /**
   * Makes a string representation of the exception's stack trace, or "(null)", if the
   * exception is null.
   *
   * <p>This method makes a best effort and never fails.
   *
   * @param e The exception to stringify.
   * @return A string with exception name and call stack.
   */
  def stringifyException(e: Throwable): String = {
    if (e == null) return STRINGIFIED_NULL_EXCEPTION
    try {
      val stm = new StringWriter
      val wrt = new PrintWriter(stm)
      e.printStackTrace(wrt)
      wrt.close()
      stm.toString
    } catch {
      case _: Throwable => e.getClass.getName + " (error while printing stack trace)"
    }
  }

  /**
   * Checks whether the given exception indicates a situation that may leave the
   * JVM in a corrupted state, meaning a state where continued normal operation can only be
   * guaranteed via clean process restart.
   *
   * <p>Currently considered fatal exceptions are Virtual Machine errors indicating
   * that the JVM is corrupted, like {@link InternalError}, {@link UnknownError},
   * and {@link java.util.zip.ZipError} (a special case of InternalError).
   * The {@link ThreadDeath} exception is also treated as a fatal error, because when
   * a thread is forcefully stopped, there is a high chance that parts of the system
   * are in an inconsistent state.
   *
   * @param t The exception to check.
   * @return True, if the exception is considered fatal to the JVM, false otherwise.
   */
  def isJvmFatalError(t: Throwable): Boolean = t.isInstanceOf[InternalError] || t.isInstanceOf[UnknownError] || t.isInstanceOf[ThreadDeath]

  /**
   * Checks whether the given exception indicates a situation that may leave the
   * JVM in a corrupted state, or an out-of-memory error.
   *
   * <p>See {@link ExceptionUtils# isJvmFatalError ( Throwable )} for a list of fatal JVM errors.
   * This method additionally classifies the {@link OutOfMemoryError} as fatal, because it
   * may occur in any thread (not the one that allocated the majority of the memory) and thus
   * is often not recoverable by destroying the particular thread that threw the exception.
   *
   * @param t The exception to check.
   * @return True, if the exception is fatal to the JVM or and OutOfMemoryError, false otherwise.
   */
  def isJvmFatalOrOutOfMemoryError(t: Throwable): Boolean = isJvmFatalError(t) || t.isInstanceOf[OutOfMemoryError]

  /**
   * Tries to enrich the passed exception with additional information.
   *
   * <p>This method improves error message for direct and metaspace {@link OutOfMemoryError}.
   * It adds description of possible causes and ways of resolution.
   *
   * @param exception exception to enrich if not {@code null}
   * @return the enriched exception or the original if no additional information could be added;
   *         {@code null} if the argument was {@code null}
   */
  @Nullable def tryEnrichOutOfMemoryError(@Nullable exception: Throwable, jvmMetaspaceOomNewErrorMessage: String, jvmDirectOomNewErrorMessage: String): Throwable = {
    val isOom = exception.isInstanceOf[OutOfMemoryError]
    if (!isOom) return exception
    val oom = exception.asInstanceOf[OutOfMemoryError]
    if (isMetaspaceOutOfMemoryError(oom)) return changeOutOfMemoryErrorMessage(oom, jvmMetaspaceOomNewErrorMessage)
    else if (isDirectOutOfMemoryError(oom)) return changeOutOfMemoryErrorMessage(oom, jvmDirectOomNewErrorMessage)
    oom
  }

  /**
   * Rewrites the error message of a {@link OutOfMemoryError}.
   *
   * @param oom        original {@link OutOfMemoryError}
   * @param newMessage new error message
   * @return the origianl {@link OutOfMemoryError} if it already has the new error message or
   *         a new {@link OutOfMemoryError} with the new error message
   */
  private def changeOutOfMemoryErrorMessage(oom: OutOfMemoryError, newMessage: String): OutOfMemoryError = {
    if (oom.getMessage == newMessage) return oom
    val newError = new OutOfMemoryError(newMessage)
    newError.initCause(oom.getCause)
    newError.setStackTrace(oom.getStackTrace)
    newError
  }

  /**
   * Checks whether the given exception indicates a JVM metaspace out-of-memory error.
   *
   * @param t The exception to check.
   * @return True, if the exception is the metaspace {@link OutOfMemoryError}, false otherwise.
   */
  def isMetaspaceOutOfMemoryError(@Nullable t: Throwable): Boolean = isOutOfMemoryErrorWithMessageStartingWith(t, "Metaspace")

  /**
   * Checks whether the given exception indicates a JVM direct out-of-memory error.
   *
   * @param t The exception to check.
   * @return True, if the exception is the direct {@link OutOfMemoryError}, false otherwise.
   */
  def isDirectOutOfMemoryError(@Nullable t: Throwable): Boolean = isOutOfMemoryErrorWithMessageStartingWith(t, "Direct buffer memory")

  private def isOutOfMemoryErrorWithMessageStartingWith(@Nullable t: Throwable, prefix: String) = { // the exact matching of the class is checked to avoid matching any custom subclasses of OutOfMemoryError
    // as we are interested in the original exceptions, generated by JVM.
    isOutOfMemoryError(t) && t.getMessage != null && t.getMessage.startsWith(prefix)
  }

  private def isOutOfMemoryError(@Nullable t: Throwable) = t != null && (t.getClass eq classOf[OutOfMemoryError])

  /**
   * Rethrows the given {@code Throwable}, if it represents an error that is fatal to the JVM.
   * See {@link ExceptionUtils# isJvmFatalError ( Throwable )} for a definition of fatal errors.
   *
   * @param t The Throwable to check and rethrow.
   */
  def rethrowIfFatalError(t: Throwable): Unit = {
    if (isJvmFatalError(t)) throw t.asInstanceOf[Error]
  }

  /**
   * Rethrows the given {@code Throwable}, if it represents an error that is fatal to the JVM
   * or an out-of-memory error. See {@link ExceptionUtils# isJvmFatalError ( Throwable )} for a
   * definition of fatal errors.
   *
   * @param t The Throwable to check and rethrow.
   */
  def rethrowIfFatalErrorOrOOM(t: Throwable): Unit = {
    if (isJvmFatalError(t) || t.isInstanceOf[OutOfMemoryError]) throw t.asInstanceOf[Error]
  }

  /**
   * Adds a new exception as a {@link Throwable# addSuppressed ( Throwable ) suppressed exception}
   * to a prior exception, or returns the new exception, if no prior exception exists.
   *
   * <pre>{@code
   *
   * public void closeAllThings() throws Exception {
   * Exception ex = null;
   * try {
   * component.shutdown();
   * } catch (Exception e) {
   * ex = firstOrSuppressed(e, ex);
   * }
   * try {
   * anotherComponent.stop();
   * } catch (Exception e) {
   * ex = firstOrSuppressed(e, ex);
   * }
   * try {
   * lastComponent.shutdown();
   * } catch (Exception e) {
   * ex = firstOrSuppressed(e, ex);
   * }
   *
   * if (ex != null) {
   * throw ex;
   * }
   * }
   * }</pre>
   *
   * @param newException The newly occurred exception
   * @param previous     The previously occurred exception, possibly null.
   *
   * @return The new exception, if no previous exception exists, or the previous exception with the
   * new exception in the list of suppressed exceptions.
   */
  def firstOrSuppressed[T <: Throwable](newException: T, @Nullable previous: T): T = {
    require(newException != null, "newException")
    if (previous == null) newException
    else {
      previous.addSuppressed(newException)
      previous
    }
  }

  /**
   * Throws the given {@code Throwable} in scenarios where the signatures do not allow you to
   * throw an arbitrary Throwable. Errors and RuntimeExceptions are thrown directly, other exceptions
   * are packed into runtime exceptions
   *
   * @param t The throwable to be thrown.
   */
  def rethrow(t: Throwable): Unit = {
    if (t.isInstanceOf[Error]) throw t.asInstanceOf[Error]
    else if (t.isInstanceOf[RuntimeException]) throw t.asInstanceOf[RuntimeException]
    else throw new RuntimeException(t)
  }

  /**
   * Throws the given {@code Throwable} in scenarios where the signatures do not allow you to
   * throw an arbitrary Throwable. Errors and RuntimeExceptions are thrown directly, other exceptions
   * are packed into a parent RuntimeException.
   *
   * @param t             The throwable to be thrown.
   * @param parentMessage The message for the parent RuntimeException, if one is needed.
   */
  def rethrow(t: Throwable, parentMessage: String): Unit = {
    if (t.isInstanceOf[Error]) throw t.asInstanceOf[Error]
    else if (t.isInstanceOf[RuntimeException]) throw t.asInstanceOf[RuntimeException]
    else throw new RuntimeException(parentMessage, t)
  }

  /**
   * Throws the given {@code Throwable} in scenarios where the signatures do allow to
   * throw a Exception. Errors and Exceptions are thrown directly, other "exotic"
   * subclasses of Throwable are wrapped in an Exception.
   *
   * @param t             The throwable to be thrown.
   * @param parentMessage The message for the parent Exception, if one is needed.
   */
  @throws[Exception]
  def rethrowException(t: Throwable, parentMessage: String): Unit = {
    if (t.isInstanceOf[Error]) throw t.asInstanceOf[Error]
    else if (t.isInstanceOf[Exception]) throw t.asInstanceOf[Exception]
    else throw new Exception(parentMessage, t)
  }

  /**
   * Throws the given {@code Throwable} in scenarios where the signatures do allow to
   * throw a Exception. Errors and Exceptions are thrown directly, other "exotic"
   * subclasses of Throwable are wrapped in an Exception.
   *
   * @param t The throwable to be thrown.
   */
  @throws[Exception]
  def rethrowException(t: Throwable): Unit = {
    if (t.isInstanceOf[Error]) throw t.asInstanceOf[Error]
    else if (t.isInstanceOf[Exception]) throw t.asInstanceOf[Exception]
    else throw new Exception(t.getMessage, t)
  }

  /**
   * Tries to throw the given exception if not null.
   *
   * @param e exception to throw if not null.
   * @throws Exception
   */
  @throws[Exception]
  def tryRethrowException(@Nullable e: Exception): Unit = {
    if (e != null) throw e
  }

  /**
   * Tries to throw the given {@code Throwable} in scenarios where the signatures allows only IOExceptions
   * (and RuntimeException and Error). Throws this exception directly, if it is an IOException,
   * a RuntimeException, or an Error. Otherwise does nothing.
   *
   * @param t The Throwable to be thrown.
   */
  @throws[IOException]
  def tryRethrowIOException(t: Throwable): Unit = {
    if (t.isInstanceOf[IOException]) throw t.asInstanceOf[IOException]
    else if (t.isInstanceOf[RuntimeException]) throw t.asInstanceOf[RuntimeException]
    else if (t.isInstanceOf[Error]) throw t.asInstanceOf[Error]
  }

  /**
   * Re-throws the given {@code Throwable} in scenarios where the signatures allows only IOExceptions
   * (and RuntimeException and Error).
   *
   * <p>Throws this exception directly, if it is an IOException, a RuntimeException, or an Error. Otherwise it
   * wraps it in an IOException and throws it.
   *
   * @param t The Throwable to be thrown.
   */
  @throws[IOException]
  def rethrowIOException(t: Throwable): Unit = {
    if (t.isInstanceOf[IOException]) throw t.asInstanceOf[IOException]
    else if (t.isInstanceOf[RuntimeException]) throw t.asInstanceOf[RuntimeException]
    else if (t.isInstanceOf[Error]) throw t.asInstanceOf[Error]
    else throw new IOException(t.getMessage, t)
  }

  /**
   * Checks whether a throwable chain contains a specific type of exception and returns it.
   *
   * @param throwable  the throwable chain to check.
   * @param searchType the type of exception to search for in the chain.
   * @return Optional throwable of the requested type if available, otherwise empty
   */
  def findThrowable[T <: Throwable](throwable: Throwable, searchType: Class[T]): Optional[T] = {
    if (throwable == null || searchType == null) Optional.empty[T] else {
      var t = throwable
      var r: Optional[T] = null
      while (t != null && r == null) {
        if (searchType.isAssignableFrom(t.getClass)) {
          r = Optional.of(searchType.cast(t))
        } else t = t.getCause
      }
      r
    }
  }


  /**
   * Checks whether a throwable chain contains an exception matching a predicate and returns it.
   *
   * @param throwable the throwable chain to check.
   * @param predicate the predicate of the exception to search for in the chain.
   * @return Optional throwable of the requested type if available, otherwise empty
   */
  def findThrowable(throwable: Throwable, predicate: Predicate[Throwable]): Optional[Throwable] = {
    if (throwable == null || predicate == null) Optional.empty[Throwable] else {
      var t = throwable
      var r: Optional[Throwable] = null
      while (t != null && r == null) {
        if (predicate.test(t)) {
          r = Optional.of(t)
        } else t = t.getCause
      }
      r
    }
  }

  /**
   * Checks whether a throwable chain contains a specific error message and returns the corresponding throwable.
   *
   * @param throwable     the throwable chain to check.
   * @param searchMessage the error message to search for in the chain.
   * @return Optional throwable containing the search message if available, otherwise empty
   */
  def findThrowableWithMessage(throwable: Throwable, searchMessage: String): Optional[Throwable] = {
    if (throwable == null || searchMessage == null) Optional.empty[Throwable] else {
      var t = throwable
      var r: Optional[Throwable] = null
      while (t != null && r == null) {
        if (t.getMessage != null && t.getMessage.contains(searchMessage)) {
          r = Optional.of(t)
        }
        else t = t.getCause
      }
      r
    }
  }

  /**
   * Unpacks an {@link ExecutionException} and returns its cause. Otherwise the given
   * Throwable is returned.
   *
   * @param throwable to unpack if it is an ExecutionException
   * @return Cause of ExecutionException or given Throwable
   */
  def stripExecutionException(throwable: Throwable): Throwable = stripException(throwable, classOf[ExecutionException])

  /**
   * Unpacks an {@link CompletionException} and returns its cause. Otherwise the given
   * Throwable is returned.
   *
   * @param throwable to unpack if it is an CompletionException
   * @return Cause of CompletionException or given Throwable
   */
  def stripCompletionException(throwable: Throwable): Throwable = stripException(throwable, classOf[CompletionException])

  /**
   * Unpacks an specified exception and returns its cause. Otherwise the given
   * {@link Throwable} is returned.
   *
   * @param throwableToStrip to strip
   * @param typeToStrip      type to strip
   * @return Unpacked cause or given Throwable if not packed
   */
  def stripException(throwableToStrip: Throwable, typeToStrip: Class[_ <: Throwable]): Throwable = {
    var throwable: Throwable = null
    while (typeToStrip.isAssignableFrom(throwableToStrip.getClass()) && throwableToStrip.getCause() != null) {
      throwable = throwableToStrip.getCause()
    }
    throwable
  }

  /**
   * Checks whether the given exception is a {@link InterruptedException} and sets
   * the interrupted flag accordingly.
   *
   * @param e to check whether it is an {@link InterruptedException}
   */
  def checkInterrupted(e: Throwable): Unit = {
    if (e.isInstanceOf[InterruptedException]) Thread.currentThread.interrupt()
  }

  /**
   *
   * @param stackTrace
   * @return
   */
  def getStackFrames(stackTrace: String): Array[String] = {
    val linebreak: String = System.lineSeparator
    val frames: StringTokenizer = new StringTokenizer(stackTrace, linebreak)
    val list: util.List[String] = new util.ArrayList[String]
    while (frames.hasMoreTokens) {
      list.add(frames.nextToken)
    }
    list.toArray(ArrayUtils.EMPTY_STRING_ARRAY)
  }

}

