package com.streamxhub.spark.monitor.domain

import java.util.Date
import scala.beans.BeanProperty
import javax.persistence.Id
import javax.persistence.GeneratedValue
import javax.persistence.Entity
import javax.persistence.Table
import java.lang.Long
import org.springframework.format.annotation.DateTimeFormat
import javax.validation.constraints.NotNull

@Table(name = "users")
@Entity
class User {

  @Id
  @GeneratedValue
  @BeanProperty
  var id: Long = _

  @BeanProperty
  @NotNull
  var name: String = _

  @BeanProperty
  @NotNull
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  var birthday: Date = _

  @BeanProperty
  @NotNull
  var telephone: String = _

  override def toString = s"User($id, $name, $birthday, $telephone)"
}
