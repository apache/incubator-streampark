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
package org.mybatis.scala.samples.nestedselect

import org.mybatis.scala.mapping._
import org.mybatis.scala.config._
import org.mybatis.scala.session._
import org.mybatis.scala.samples.util._

// Model beans =================================================================

// Simple Group POJO
class Group {
  var id : Int = _
  var name : String = _
}

// Simple ContactInfo POJO
class ContactInfo {
  var id : Int = _
  var address : String = _
  var phone : String = _
}

// Simple Person POJO with OneToMany to ContactInfo
class Person {
  var id : Int = _
  var firstName : String = _
  var lastName : String = _
  var group : Group = _
  var contact : Seq[ContactInfo] = _
}

// Data access layer ===========================================================

object Persistence {

  // Query for a Group
  val selectGroup = new SelectOneBy[Int,Group] {
    def xsql = 
      <xsql>
      SELECT id_ as id, name_ as name 
      FROM people_group 
      WHERE id_ = #{{id}}
      </xsql>
  }
  
  // Query for a list of contact info
  val selectContact = new SelectListBy[Int,ContactInfo] {
    resultMap = new ResultMap[ContactInfo] {
      id(property="id", column="id_")
      result(property="address", column="street_address_")
      result(property="phone", column="phone_number_")
    }
    def xsql = <xsql>SELECT * FROM contact_info WHERE owner_id_ = #{{id}}</xsql>
  }
  
  // Query for a list of all persons
  val findAll = new SelectList[Person] {

    // Define the result mapping
    resultMap = new ResultMap[Person] {

      id(property="id", column="id_")
      result(property="firstName", column="first_name_")
      result(property="lastName", column="last_name_")

      association[Group] (property="group", column="group_id_", select=selectGroup)

      collection[ContactInfo] (property="contact", column="id_", select=selectContact)  

    }

    // Define the actual query
    def xsql =
      <xsql>
        SELECT *
        FROM person p
        ORDER BY first_name_
      </xsql>
  }

  // Load datasource configuration from an external file
  val config = Configuration("mybatis.xml")

  // Add the data access function to the default namespace
  config += findAll
  config ++= DBSchema
  config ++= DBSampleData

  // Build the session manager
  lazy val context = config.createPersistenceContext
  
}

// Application code ============================================================

object NestedSelectSample {

  // Do the Magic ...
  def main(args : Array[String]) : Unit = {
    Persistence.context.transaction { implicit session =>

      DBSchema.create
      DBSampleData.populate
      
      for (p <- Persistence.findAll()) {
        println("\nPerson(%d): %s %s (%s)".format(p.id, p.firstName, p.lastName, p.group.name))
        for (contact <- p.contact) {
          println("  Address: %s, Phone: %s".format(contact.address, contact.phone))
        }
      }
      
    }
  }

}