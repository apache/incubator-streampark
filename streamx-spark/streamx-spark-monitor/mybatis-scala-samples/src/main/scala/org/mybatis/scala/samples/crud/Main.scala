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
package org.mybatis.scala.samples.crud

import org.mybatis.scala.session._

object Main extends App {

  val db = Config.context

  db.transaction { implicit session =>

    // Init the DB: create table etc...
    ItemDAO.initdb

    // Insert some items
    ItemDAO insert Item("BMW")
    ItemDAO insert Item("Ford", Some("USA"), Some(1900))
    ItemDAO insert Item("Renault", Some("France"))
    ItemDAO insert Item("Chevrolet")
    ItemDAO insert Item("Hyundai", Some("Korea"))
    ItemDAO insert Item("Honda", year=Some(1997))

    // Show ...
    println("== Initial values ====================")
    ItemDAO.findAll() foreach (printItem _)

    // Change something ...
    for (item <- ItemDAO findById(3)) {
      item.description = "-CHANGED-"
      item.year = Some(2001)
      item.info = Some("-MEXICO-")
      ItemDAO update item
    }

    // Show again ...
    println("== With some changes =================")
    ItemDAO.findAll() foreach (printItem _)

    // Delete something ...
    for (item <- ItemDAO findById(3)) {
      ItemDAO delete item
    }

    // Show again ...
    println("== With some items removed ===========")
    ItemDAO.findAll() foreach (printItem _)

    // Show filtered ...
    println("== Filtered by H% ====================")
    ItemDAO.findByDescription("H%") foreach (printItem _)

  }

  def printItem(i : Item) =
    println("%d: %10s\t %10s\t %5s" format (i.id, i.description, i.info.getOrElse("-"), i.year.getOrElse("-")))

}
