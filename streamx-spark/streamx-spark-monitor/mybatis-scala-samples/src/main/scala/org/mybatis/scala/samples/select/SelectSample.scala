/**
 *    Copyright 2011-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.scala.samples.select

import org.mybatis.scala.mapping._
import org.mybatis.scala.config._
import org.mybatis.scala.session._
import org.mybatis.scala.samples.util._

// Model beans =================================================================

class Person {
  var id : Int = _
  var firstName : String = _
  var lastName : String = _
}

// Data access layer ===========================================================

object DB {

  // Simple select function
  val findAll = new SelectListBy[String,Person] {
    def xsql =
      <xsql>
        <bind name="pattern" value="'%' + _parameter + '%'" />
        SELECT
          id_ as id,
          first_name_ as firstName,
          last_name_ as lastName
        FROM
          person
        WHERE
          first_name_ LIKE #{{pattern}}
      </xsql>
  }

  // Datasource configuration
  val config = Configuration(
    Environment(
      "default", 
      new JdbcTransactionFactory(), 
      new PooledDataSource(
        "org.hsqldb.jdbcDriver",
        "jdbc:hsqldb:mem:scala",
        "sa",
        ""
      )
    )
  )

  // Add the data access method to the default namespace
  config += findAll
  config ++= DBSchema
  config ++= DBSampleData

  // Build the session manager
  lazy val context = config.createPersistenceContext
  
}

// Application code ============================================================

object SelectSample {

  // Do the Magic ...
  def main(args : Array[String]) : Unit = {
    DB.context.transaction { implicit session =>

      DBSchema.create
      DBSampleData.populate

      DB.findAll("a").foreach { p => 
        println( "Person(%d): %s %s".format(p.id, p.firstName, p.lastName) )
      }
      
    }
  }

}
