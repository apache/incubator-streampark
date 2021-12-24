#!/usr/bin/python
# -*- coding: UTF-8 -*-

# update streamx project version, requires python-3.8+
# use case: python3 update_ver.py <expect_ver> <project_path>
# example: python3 update_ver.py 1.2.1

# @author : Al-assad
# @date   : 2021/11/17

import sys
import os
import xml.etree.ElementTree as ET
from os.path import join as os_path
import pprint

expect_ver = '1.2.1'
project_path = '../'

if len(sys.argv) >= 2:
    expect_ver = sys.argv[1]
if len(sys.argv) >= 3:
    project_path = sys.argv[2]

# find all pom.xml in project
poms = []
for path, dirs, files in os.walk(project_path):
    if path == os_path('.git'):
        continue
    for file in files:
        if file != 'pom.xml':
            continue
        poms.append(os_path(path, file))

ns = '{http://maven.apache.org/POM/4.0.0}'
pre_version = ''
# update the version in pom.xml
for pom in poms:
    tree = ET.parse(pom, parser=ET.XMLParser(target=ET.TreeBuilder(insert_comments=True)))
    root = tree.getroot()
    group_id = root.find(ns + 'groupId')
    if group_id is not None and group_id.text != 'com.streamxhub.streamx':
        continue
    version_id = root.find(ns + 'version')
    if version_id is not None:
        pre_version = version_id.text
        version_id.text = expect_ver
    parent = root.find(ns + 'parent')
    if parent is not None:
        if parent.find(ns + 'groupId').text == 'com.streamxhub.streamx':
            parent_version = parent.find(ns + 'version')
            if parent_version is not None:
                parent_version.text = expect_ver
    ET.register_namespace('', 'http://maven.apache.org/POM/4.0.0')
    # no use the xml_declaration content create by ET like <?xml version='1.0' encoding='utf-8'?>
    tree.write(pom, encoding='UTF-8', xml_declaration=False)
    with open(pom, 'r+') as f:
        c = f.read()
        f.seek(0, 0)
        f.write('<?xml version="1.0" encoding="UTF-8"?>\n' + c)
    print('update version in {} to {}'.format(pom, expect_ver))

# update version in print log content
config_const_file = os_path(project_path, 'streamx-common/src/main/scala/com/streamxhub/streamx/common/conf/ConfigConst.scala')
f_content = []
with open(config_const_file, 'r') as f:
    f_content = f.readlines()
    for i in range(len(f_content)):
        line = f_content[i]
        if line.strip().startswith('println("       Ver'):
            f_content[i] = line.replace(pre_version, expect_ver)
            break

with open(config_const_file, 'w') as f:
    f.writelines(f_content)
print('update version in {} to {}'.format(config_const_file, expect_ver))

print('done :)')
