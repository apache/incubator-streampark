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
package org.mybatis.scala.samples.insert

import org.mybatis.scala.samples.util._
import org.mybatis.scala.mapping._
import org.mybatis.scala.config._
import org.mybatis.scala.session._

object InsertSampleBatch {

  // Simple Group POJO
  class Group {
    var id : Int = _
    var name : String = _
  }

  // Simple Person POJO
  class Person {
    var id : Int = _
    var firstName : String = _
    var lastName : String = _
    var group : Group = _
  }

  // Simple insert method
  val insertPerson = new Insert[Person] {
    keyGenerator = JdbcGeneratedKey(null, "id")
    def xsql =
      <xsql>
        INSERT INTO person (first_name_, last_name_, group_id_)
        VALUES (#{{firstName}}, #{{lastName}}, #{{group.id}})
      </xsql>
  }

  // Simple insert method
  val insertGroup = new Insert[Group] {
    keyGenerator = JdbcGeneratedKey(null, "id")
    def xsql =
      <xsql>
        INSERT INTO people_group (name_)
        VALUES (#{{name}})
      </xsql>
  }

  // Load datasource configuration
  val config = Configuration("mybatis.xml")

  // Create a configuration space, add the data access method
  config ++= Seq(insertPerson, insertGroup)
  config ++= DBSchema  

  // Build the session manager
  val db = config.createPersistenceContext

  // Do the Magic ...
  def main(args : Array[String]) : Unit = {

    db.transaction(ExecutorType.BATCH) { implicit session =>

      DBSchema.create      
      session.flushStatements
      
      val g = new Group
      g.name = "New Group"
      insertGroup(g)
      session.flushStatements

      Seq(("Jonh", "Smith"), ("Mary", "Jane"), ("Pepe", "Grillo")) map { (t) =>
        val p = new Person
        p.firstName = t._1
        p.lastName = t._2
        p.group = g
        insertPerson(p)
      }

      val created = session.flushStatements
      for (result <- created) {
        println( "\n==========================================================" )
        println( result.getSql )
        println( "\nCalled " + result.getUpdateCounts.size + " times")
        println( "Total update counts: " + result.getUpdateCounts.foldLeft(0)((a,b)=>a + b) )
      }
      
    }

  }


}
