package com.streamxhub.repl.flink.interpreter

import java.io.{ByteArrayOutputStream, IOException, OutputStream}

import org.slf4j.Logger

class InterpreterOutput extends OutputStream {

  private val buffer = new ByteArrayOutputStream

  def clear(): Unit = {
    buffer.reset()
  }

  override def write(b: Int): Unit = {
    buffer.write(b)
  }


  @throws[IOException] override def write(b: Array[Byte]): Unit = {
    write(b, 0, b.length)
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    for (i <- off until len) {
      write(b(i))
    }
  }

  private[this] def toByteArray(): Array[Byte] = buffer.toByteArray

  @throws[IOException] override def close(): Unit = {
    flush()
  }

  override def toString = new String(toByteArray)

}


private[interpreter] class InterpreterOutputStream extends LogOutputStream {

  private var logger: Logger = null
  private[this] var interpreterOutput: InterpreterOutput = null
  private[this] var ignoreLeadingNewLinesFromScalaReporter: Boolean = false

  def this(logger: Logger) {
    this()
    this.logger = logger
  }

  def getInterpreterOutput: InterpreterOutput = interpreterOutput

  def setInterpreterOutput(interpreterOutput: InterpreterOutput): Unit = {
    this.interpreterOutput = interpreterOutput
  }

  @throws[IOException] override def write(b: Int): Unit = {
    if (ignoreLeadingNewLinesFromScalaReporter && b == '\n') {
      val error = Thread.currentThread.getStackTrace.filter(x => x.getClassName == "scala.tools.nsc.interpreter.ReplReporter" && x.getMethodName == "error")
      if (error.nonEmpty) {
        return
      }
    } else {
      ignoreLeadingNewLinesFromScalaReporter = false
    }
    super.write(b)
    if (interpreterOutput != null) {
      interpreterOutput.write(b)
    }
  }

  @throws[IOException] override def write(b: Array[Byte]): Unit = {
    write(b, 0, b.length)
  }

  @throws[IOException] override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    for (i <- off until len) write(b(i))
  }

  @Override def processLine(s: String, i: Int): Unit = {
    logger.debug("Interpreter output:" + s)
  }

  @throws[IOException] override def close(): Unit = {
    super.close()
    if (interpreterOutput != null) interpreterOutput.close()
  }

  @throws[IOException] override def flush(): Unit = {
    super.flush()
    if (interpreterOutput != null) interpreterOutput.flush()
  }

  def ignore(): Unit = {
    ignoreLeadingNewLinesFromScalaReporter = true
  }

}

private[this] abstract class LogOutputStream extends OutputStream {
  private val buffer: ByteArrayOutputStream = new ByteArrayOutputStream(132)
  private var skip = false
  private var level = 999

  def this(level: Int) {
    this()
    this.level = level
  }

  @throws[IOException] override def write(cc: Int): Unit = {
    val c = cc.toByte
    if (c != 10 && c != 13) {
      buffer.write(cc)
    } else if (!this.skip) {
      this.processBuffer()
    }
    this.skip = c == 13
  }

  @throws[IOException] override def flush(): Unit = {
    if (this.buffer.size > 0) this.processBuffer()
  }

  @throws[IOException] override def close(): Unit = {
    if (this.buffer.size > 0) this.processBuffer()
    super.close()
  }

  def getMessageLevel: Int = this.level

  @throws[IOException] override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    var offset = off
    var blockStartOffset = off
    var remaining = len
    while (remaining > 0) {
      while (remaining > 0 && b(offset) != 10 && b(offset) != 13) {
        offset += 1
        remaining -= 1
      }
      val blockLength = offset - blockStartOffset
      if (blockLength > 0) {
        this.buffer.write(b, blockStartOffset, blockLength)
      }
      while (remaining > 0 && (b(offset) == 10 || b(offset) == 13)) {
        this.write(b(offset))
        offset += 1
        remaining -= 1
      }
      blockStartOffset = offset
    }
  }

  protected def processBuffer(): Unit = {
    this.processLine(this.buffer.toString)
    this.buffer.reset()
  }

  protected def processLine(line: String): Unit = {
    this.processLine(line, this.level)
  }

  protected def processLine(var1: String, var2: Int): Unit
}

