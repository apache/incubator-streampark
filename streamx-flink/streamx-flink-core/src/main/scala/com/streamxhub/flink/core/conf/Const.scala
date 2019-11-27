package com.streamxhub.flink.core.conf

object Const {

  /**
   *
   * about parameter...
   */

  val KEY_APP_HOME = "app.home"

  val KEY_HOST = "host"

  val KEY_PORT = "port"

  val KEY_DB = "db"

  val KEY_USER = "user"

  val KEY_PASSWORD = "password"

  val KEY_ES_AUTH_USER = "sink.es.auth.user"

  val KEY_ES_AUTH_PASSWORD = "sink.es.auth.password"

  val KEY_ES_REST_MAX_RETRY = "sink.es.rest.max.retry.timeout"

  val KEY_ES_REST_CONTENT_TYPE = "sink.es.rest.content.type"

  val KEY_ES_CONN_REQ_TIME_OUT = "sink.es.connect.request.timeout"

  val KEY_ES_CONN_TIME_OUT = "sink.es.connect.timeout"

  val KEY_ES_CLUSTER_NAME = "sink.es.cluster.name"

  val KEY_ES_CLIENT_TRANSPORT_SNIFF = "client.transport.sniff"

  val KEY_FLINK_CHECKPOINT_INTERVAL = "flink.checkpoint.interval"

  val KEY_FLINK_CHECKPOINT_MODE = "flink.checkpoint.mode"

  val KEY_FLINK_TIME_CHARACTERISTIC = "flink.time.characteristic"

  /**
   * about config prefix
   */
  val FLINK_CONF = "flink.conf"

  val TOPIC = "topic"

  val APP_NAME = "app.name"

  val SOURCE_KAFKA_PREFIX = "source.kafka.consume."

  val SINK_KAFKA_PREFIX = "sink.kafka.producer."

  val SINK_REDIS_PREFIX = "sink.redis."

  val SINK_ES_PREFIX = "sink.es."

  val SINK_ES_CONF_BULK_PREFIX = "bulk.flush."

  /**
   * sign....
   */
  val SIGN_COLON = ":"

  val SIGN_SEMICOLON = ";"

  val SIGN_COMMA = ","

  val SIGN_EMPTY = ""

}
