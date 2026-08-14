/*
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

package org.apache.streampark.flink.core.lineage;

import org.apache.streampark.common.util.StreamParkLoggerFactory;

import org.apache.streampark.shaded.org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the physical OpenLineage dataset identity for every source/sink table in a Flink {@code
 * CompiledPlan}, paired per sink rather than flattened across the whole plan (see {@link
 * LineagePipeline} for why per-sink pairing matters for {@code STATEMENT SET} jobs).
 *
 * <p>Pure Jackson, no Flink API — the plan is already a JSON string by the time it reaches this
 * class, produced by {@code StatementSet.compilePlan().asJsonString()} in the per-Flink-version
 * extractor.
 */
public final class CompiledPlanLineageParser {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(CompiledPlanLineageParser.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern IDENTIFIER = Pattern.compile("`([^`]+)`\\.`([^`]+)`\\.`([^`]+)`");

    private CompiledPlanLineageParser() {
    }

    /**
     * @param compiledPlanJson the CompiledPlan JSON string
     * @param tempTables tables declared in this job's own SQL text via {@code CREATE [TEMPORARY]
     *     TABLE ... WITH (...)}, keyed by local (unqualified) table name — see {@link
     *     SqlWithOptionsParser}
     * @param catalogTypes the {@code 'type'} of every catalog this job's own SQL text attached via
     *     {@code CREATE CATALOG}, keyed by catalog name — see {@link #resolveOne}
     * @return one {@link LineagePipeline} per sink found in the plan; a sink or input whose
     *     identity cannot be resolved is dropped (logged), never thrown — see class javadoc on
     *     {@link DatasetIdentityRegistry} for the fail-open rationale
     * @throws IllegalArgumentException if {@code compiledPlanJson} itself is not valid JSON — that
     *     is a structural/version-mismatch problem worth surfacing, not a per-dataset gap
     */
    public static List<LineagePipeline> parse(
                                              String compiledPlanJson,
                                              Map<String, SqlWithOptionsParser.WithOptions> tempTables,
                                              Map<String, String> catalogTypes) {
        JsonNode root;
        try {
            root = MAPPER.readTree(compiledPlanJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CompiledPlan JSON", e);
        }

        Map<Integer, JsonNode> nodesById = new LinkedHashMap<>();
        for (JsonNode node : root.path("nodes")) {
            nodesById.put(node.path("id").asInt(), node);
        }
        Map<Integer, List<Integer>> predecessors = new LinkedHashMap<>();
        for (JsonNode edge : root.path("edges")) {
            int source = edge.path("source").asInt();
            int target = edge.path("target").asInt();
            predecessors.computeIfAbsent(target, k -> new ArrayList<>()).add(source);
        }

        List<LineagePipeline> pipelines = new ArrayList<>();
        for (JsonNode node : nodesById.values()) {
            JsonNode sink = node.path("dynamicTableSink").path("table").path("identifier");
            if (!sink.isTextual()) {
                continue;
            }
            LineageDataset output = resolveOne(sink.asText(), tempTables, catalogTypes, "output");
            if (output == null) {
                continue;
            }

            Set<LineageDataset> inputs = new LinkedHashSet<>();
            Set<Integer> visited = new LinkedHashSet<>();
            Deque<Integer> pending = new ArrayDeque<>();
            pending.push(node.path("id").asInt());
            while (!pending.isEmpty()) {
                int currentId = pending.pop();
                if (!visited.add(currentId)) {
                    continue;
                }
                JsonNode current = nodesById.get(currentId);
                if (current == null) {
                    // An edge referencing a node absent from "nodes" would be a malformed plan;
                    // skip it rather than NPE on a path contracted to only throw on bad JSON.
                    LOG.warn("[lineage] CompiledPlan edge references unknown node id {}, skipping it", currentId);
                    continue;
                }
                JsonNode source = current.path("scanTableSource").path("table").path("identifier");
                if (source.isTextual()) {
                    LineageDataset input = resolveOne(source.asText(), tempTables, catalogTypes, "input");
                    if (input != null) {
                        inputs.add(input);
                    }
                }
                // Lookup joins (stream-exec-lookup-join) don't produce a scanTableSource node — the
                // temporal table they read is nested under temporalTable.lookupTableSource instead.
                JsonNode lookupSource =
                    current.path("temporalTable").path("lookupTableSource").path("table").path("identifier");
                if (lookupSource.isTextual()) {
                    LineageDataset input = resolveOne(lookupSource.asText(), tempTables, catalogTypes, "input");
                    if (input != null) {
                        inputs.add(input);
                    }
                }
                for (int predecessorId : predecessors.getOrDefault(currentId, List.of())) {
                    pending.push(predecessorId);
                }
            }
            pipelines.add(new LineagePipeline(output, inputs));
        }
        return pipelines;
    }

    /**
     * Resolves one {@code `catalog`.`database`.`table`} plan identifier to a dataset identity, by
     * whichever of the two routes applies:
     *
     * <ul>
     *   <li>the table was declared in this job's own SQL via {@code CREATE TABLE ... WITH (...)} —
     *       its connector options carry the physical location, so {@link DatasetIdentityRegistry}
     *       decides;
     *   <li>the table lives in an attached catalog — the plan names the catalog but not its kind,
     *       so the {@code 'type'} captured from that catalog's {@code CREATE CATALOG} becomes the
     *       namespace scheme ({@code paimon://catalog/db}, {@code hive://catalog/db}, ...).
     * </ul>
     *
     * <p>Returns {@code null} (logged) when neither applies — a table from a catalog attached
     * outside this job's SQL, whose kind is therefore unknowable here. Guessing a scheme would be
     * worse than reporting nothing: dataset identity is deduplicated by exact string match, so a
     * wrong guess silently splits one physical table into two nodes in the graph instead of failing
     * loudly. Same rationale as {@link DatasetIdentityRegistry}'s refusal of a generic fallback.
     */
    private static LineageDataset resolveOne(
                                             String rawIdentifier,
                                             Map<String, SqlWithOptionsParser.WithOptions> tempTables,
                                             Map<String, String> catalogTypes,
                                             String role) {
        Matcher matcher = IDENTIFIER.matcher(rawIdentifier);
        if (!matcher.matches()) {
            LOG.warn("[lineage] unexpected CompiledPlan table identifier shape ({}): {}", role, rawIdentifier);
            return null;
        }
        String catalog = matcher.group(1);
        String database = matcher.group(2);
        String table = matcher.group(3);

        SqlWithOptionsParser.WithOptions tableOptions = tempTables.get(table);
        if (tableOptions != null) {
            LineageDataset resolved = DatasetIdentityRegistry.resolve(table, tableOptions.options());
            if (resolved != null) {
                LOG.info(
                    "[lineage] dataset resolved from WITH options (connector={}, role={}): {}",
                    tableOptions.options().get("connector"),
                    role,
                    resolved);
            }
            return resolved;
        }

        String catalogType = catalogTypes.get(catalog);
        if (catalogType == null) {
            LOG.warn(
                "[lineage] table `{}` belongs to catalog `{}`, whose type is not declared in this job's SQL"
                    + " (role={}), skipping lineage for it",
                table,
                catalog,
                role);
            return null;
        }
        LineageDataset resolved = new LineageDataset(catalogType + "://" + catalog + "/" + database, table);
        LOG.info("[lineage] dataset resolved from catalog identifier (role={}): {}", role, resolved);
        return resolved;
    }
}
