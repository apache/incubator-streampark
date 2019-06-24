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

// Model beans (Immutable case class) ==========================================

case class CPerson(id : Int, firstName : String, lastName : String)

// Data access layer ===========================================================

object CDB {

  // Simple select function
  val findAll = new SelectListBy[String,CPerson] {
    
    // CPerson Constructor Mapping
    resultMap = new ResultMap[CPerson] {
      // Warning: Order is important (constructor arguments in order)
      idArg ("id_",         javaType=T[Int])
      arg   ("first_name_", javaType=T[String])
      arg   ("last_name_",  javaType=T[String])
    }
    
    def xsql =
      """
        SELECT
          id_, first_name_, last_name_
        FROM
          person
        WHERE
          first_name_ LIKE #{name}
      """
  }

  // Main configuration
  object ConfigurationSpec extends Configuration.Builder {    
    // Connection settings
    environment(
      id = "default", 
      transactionFactory = new JdbcTransactionFactory(), 
      dataSource = new PooledDataSource("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:scala", "sa", "")
    )
    // Add the data access methods to default namespace
    statements(findAll)
    mappers(DBSchema, DBSampleData)    
  }
  
  // Build the session manager
  lazy val context = Configuration(ConfigurationSpec).createPersistenceContext
  
}

// Application code ============================================================

object SelectImmutableSample {

  import CDB._

  // Do the Magic ...
  def main(args: Array[String]): Unit = context.transaction { implicit s =>
    
    // Create database and populate it with sample data
    DBSchema.create
    DBSampleData.populate
    
    // Query
    findAll("%a%").foreach {
      case CPerson(id, firstName, lastName) =>
        println("Person(%d): %s %s" format (id, firstName, lastName))
    }
    
  }

}
