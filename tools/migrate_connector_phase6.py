#!/usr/bin/env python3
"""Generate remaining Java connector files from Scala sources (Phase 6 migration)."""
import os
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONNECTOR = os.path.join(ROOT, "streampark-flink/streampark-flink-connector")

LICENSE = """/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"""

def write_java(rel_path, body):
    path = os.path.join(CONNECTOR, rel_path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(LICENSE + "\n" + body.strip() + "\n")
    print(f"Wrote {rel_path}")

def delete_scala():
    count = 0
    for root, dirs, files in os.walk(CONNECTOR):
        for f in files:
            if f.endswith(".scala"):
                os.remove(os.path.join(root, f))
                count += 1
        # remove empty scala dirs
        for d in list(dirs):
            if d == "scala":
                scala_dir = os.path.join(root, d)
                for sub_root, sub_dirs, sub_files in os.walk(scala_dir, topdown=False):
                    for sf in sub_files:
                        os.remove(os.path.join(sub_root, sf))
                    for sd in sub_dirs:
                        os.rmdir(os.path.join(sub_root, sd))
                try:
                    os.rmdir(scala_dir)
                except OSError:
                    pass
    print(f"Deleted {count} scala files")

def update_poms():
    for root, dirs, files in os.walk(CONNECTOR):
        for f in files:
            if f != "pom.xml":
                continue
            path = os.path.join(root, f)
            with open(path) as fh:
                content = fh.read()
            orig = content
            # Remove scala flink deps
            for artifact in [
                "flink-scala_${scala.binary.version}",
                "flink-streaming-scala_${scala.binary.version}",
            ]:
                start = content.find(f"<artifactId>{artifact}</artifactId>")
                if start == -1:
                    continue
                dep_start = content.rfind("<dependency>", 0, start)
                dep_end = content.find("</dependency>", start) + len("</dependency>")
                if dep_start != -1 and dep_end > dep_start:
                    content = content[:dep_start] + content[dep_end:]
            # Add flink-streaming-java if not present
            if "flink-streaming-java" not in content and "<dependencies>" in content:
                insert = """
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-streaming-java</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

"""
                content = content.replace("<dependencies>", "<dependencies>" + insert, 1)
            # Fix redis typo artifact
            content = content.replace(
                "flink-streaming-java${scala.binary.flink.version}",
                "flink-streaming-java",
            )
            if content != orig:
                with open(path, "w") as fh:
                    fh.write(content)
                print(f"Updated pom: {path}")

if __name__ == "__main__":
    action = sys.argv[1] if len(sys.argv) > 1 else "all"
    if action in ("delete-scala", "all"):
        delete_scala()
    if action in ("update-poms", "all"):
        update_poms()
