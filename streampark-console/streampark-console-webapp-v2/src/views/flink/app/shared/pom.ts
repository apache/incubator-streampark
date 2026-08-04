/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export interface PomDependency {
    groupId: string
    artifactId: string
    version: string
    classifier?: string
    exclusions?: Array<{ groupId: string; artifactId: string }>
}

export function toPomString(pom: PomDependency) {
    const { groupId, artifactId, version, classifier, exclusions = [] } = pom
    let exclusionString = ''
    let classifierString = ''
    if (exclusions.length > 0) {
        exclusions.forEach((item) => {
            exclusionString +=
                '      <exclusion>\n' +
                `        <groupId>${item.groupId}</groupId>\n` +
                `        <artifactId>${item.artifactId}</artifactId>\n` +
                '      </exclusion>\n'
        })
        exclusionString = `    <exclusions>\n${exclusionString}    </exclusions>\n`
    }
    if (classifier != null) classifierString = `    <classifier>${classifier}</classifier>\n`

    return (
        `  <dependency>\n` +
        `    <groupId>${groupId}</groupId>\n` +
        `    <artifactId>${artifactId}</artifactId>\n` +
        `    <version>${version}</version>\n` +
        classifierString +
        exclusionString +
        '  </dependency>'
    )
}

export function getPomId(pom: Pick<PomDependency, 'groupId' | 'artifactId' | 'classifier'>) {
    if (pom.classifier != null) return `${pom.groupId}_${pom.artifactId}_${pom.classifier}`
    return `${pom.groupId}_${pom.artifactId}`
}

export function buildDependencyJson(
    pomRecords: PomDependency[],
    jarRecords: string[],
): string | null {
    const dependency: Recordable = {}
    if (pomRecords.length > 0) dependency.pom = pomRecords
    if (jarRecords.length > 0) dependency.jar = jarRecords
    if (dependency.pom === undefined && dependency.jar === undefined) return null
    return JSON.stringify(dependency)
}
