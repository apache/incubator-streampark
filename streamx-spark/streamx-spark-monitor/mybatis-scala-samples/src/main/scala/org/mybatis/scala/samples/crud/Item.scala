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

class Item {
  var id : Int = _
  var description : String = _
  var info : Option[String] = None
  var year : Option[Int] = None
}

object Item {

  def apply(description : String, info : Option[String] = None, year : Option[Int] = None) = {
    val i = new Item
    i.description = description
    i.info = info
    i.year = year
    i
  }

}
