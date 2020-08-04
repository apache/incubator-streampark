#!/usr/bin/env python
#
# Copyright (c) 2016 Cloudera, Inc. All rights reserved.
#

'''
This script is provided by CMF for hadoop to determine network/rack topology.
It is automatically generated and could be replaced at any time. Any changes
made to it will be lost when this happens.
'''

import os
import sys
import xml.dom.minidom

try:
  xrange
except NameError:
  # support for python3, which basically renamed xrange to range
  xrange = range

def main():
  MAP_FILE = '{{CMF_CONF_DIR}}/topology.map'
  DEFAULT_RACK = '/default'

  if 'CMF_CONF_DIR' in MAP_FILE:
    # variable was not substituted. Use this file's dir
    MAP_FILE = os.path.join(os.path.dirname(__file__), "topology.map")

  # We try to keep the default rack to have the same
  # number of elements as the other hosts available.
  # There are bugs in some versions of Hadoop which
  # make the system error out.
  max_elements = 1

  map = dict()

  try:
    mapFile = open(MAP_FILE, 'r')

    dom = xml.dom.minidom.parse(mapFile)
    for node in dom.getElementsByTagName("node"):
      rack = node.getAttribute("rack")
      max_elements = max(max_elements, rack.count("/"))
      map[node.getAttribute("name")] = node.getAttribute("rack")
  except:
    default_rack = "".join([ DEFAULT_RACK for _ in xrange(max_elements)])
    print(default_rack)
    return -1

  default_rack = "".join([ DEFAULT_RACK for _ in xrange(max_elements)])
  if len(sys.argv)==1:
    print(default_rack)
  else:
    print(" ".join([map.get(i, default_rack) for i in sys.argv[1:]]))
  return 0

if __name__ == "__main__":
  sys.exit(main())
