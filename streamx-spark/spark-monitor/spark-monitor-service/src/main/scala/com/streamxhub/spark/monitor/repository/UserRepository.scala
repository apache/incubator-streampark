package com.streamxhub.spark.monitor.repository

import org.springframework.data.jpa.repository.JpaRepository
import java.lang.Long

import com.streamxhub.spark.monitor.domain.User

trait UserRepository extends JpaRepository[User, Long]
