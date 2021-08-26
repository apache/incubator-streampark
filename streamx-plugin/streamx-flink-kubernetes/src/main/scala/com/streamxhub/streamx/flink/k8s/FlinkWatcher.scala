package com.streamxhub.streamx.flink.k8s

/**
 * auth: Al-assad
 */
trait FlinkWatcher extends AutoCloseable {

  /**
   * Runnable streamline syntax
   */
  protected implicit def funcToRunnable(fun: () => Unit): Runnable = new Runnable() {
    def run(): Unit = fun()
  }

  // todo deplayStart()

  /**
   * start watcher process
   */
  def start()

  /**
   * stop watcher process
   */
  def stop()

  /**
   * restart watcher process
   */
  def restart(): Unit = {
    stop()
    start()
  }

}
