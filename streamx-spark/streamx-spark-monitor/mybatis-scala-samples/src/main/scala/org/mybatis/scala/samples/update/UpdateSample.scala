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
package org.mybatis.scala.samples.update

import org.mybatis.scala.mapping._
import org.mybatis.scala.config._
import org.mybatis.scala.session._
import org.mybatis.scala.samples.util._

object UpdateSample {

  // Simple Person POJO
  class Person {
    var id : Int = _
    var firstName : String = _
    var lastName : String = _
  }

  // Simple update method
  val updatePerson = new Update[Person] {
    def xsql =
      <xsql>
        UPDATE person
        SET
          first_name_ = #{{firstName}},
          last_name_ = #{{lastName}}
        WHERE
          id_ = #{{id}}
      </xsql>
  }

  // Simple select method
  val findPerson = new SelectOneBy[Int,Person] {
    def xsql =
      <xsql>
        SELECT id_ as id, first_name_ as firstName, last_name_ as lastName
        FROM person
        WHERE id_ = #{{id}}
      </xsql>
  }


  // Load datasource configuration
  val config = Configuration("mybatis.xml")

  // Create a configuration space, add the data access method
  config.addSpace("ns1") { space =>
    space += updatePerson += findPerson
  }

  config ++= DBSchema
  config ++= DBSampleData 
  
  // Build the session manager
  val db = config.createPersistenceContext

  // Do the Magic ...
  def main(args : Array[String]) : Unit = {

    db.transaction { implicit session =>

      DBSchema.create
      DBSampleData.populate      
      
      findPerson(1) match {
        case Some(p) =>
          
          // Show original
          println("Before =>\n\tPerson(%d): %s, %s".format(p.id, p.lastName, p.firstName))
          
          // Update a property
          p.firstName = "Sun (Updated " + new java.util.Date + ")"
          updatePerson(p)     
          
          // Reload to verify
          for (p2 <- findPerson(1)) 
            println( "After =>\n\tPerson(%d): %s, %s".format(p2.id, p2.lastName, p2.firstName) )
          
        case None =>
          println("Person with id=1 does not exists!!!")
      }
      
    }

  }


}
