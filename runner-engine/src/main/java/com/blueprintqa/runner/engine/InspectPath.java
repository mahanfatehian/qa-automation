package com.blueprintqa.runner.engine;

import com.blueprintqa.runner.model.GraphEdge;
import com.blueprintqa.runner.model.GraphNode;
import com.blueprintqa.runner.model.WorkflowJob;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InspectPath {
    private InspectPath() {
    }

    public static List<GraphNode> prefix(WorkflowJob job, String stopBefore) {
        GraphNode start = job.start();
        if (stopBefore == null || stopBefore.isBlank() || stopBefore.equals(start.id)) {
            return List.of(start);
        }
        Map<String, String> parent = new HashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        queue.add(start.id);
        seen.add(start.id);
        boolean found = false;
        while (!queue.isEmpty() && !found) {
            String id = queue.removeFirst();
            for (GraphNode child : execChildren(job, id)) {
                if (child == null || !seen.add(child.id)) {
                    continue;
                }
                parent.put(child.id, id);
                if (child.id.equals(stopBefore)) {
                    found = true;
                    break;
                }
                queue.add(child.id);
            }
        }
        if (!found) {
            return untilFork(job, start);
        }
        LinkedList<GraphNode> path = new LinkedList<>();
        String cursor = parent.get(stopBefore);
        while (cursor != null) {
            GraphNode node = job.node(cursor);
            if (node != null) {
                path.addFirst(node);
            }
            if (cursor.equals(start.id)) {
                break;
            }
            cursor = parent.get(cursor);
        }
        return path;
    }

    private static List<GraphNode> untilFork(WorkflowJob job, GraphNode start) {
        List<GraphNode> path = new ArrayList<>();
        GraphNode cursor = start;
        Set<String> seen = new HashSet<>();
        while (cursor != null && seen.add(cursor.id)) {
            path.add(cursor);
            List<GraphNode> next = execChildren(job, cursor.id);
            if (next.size() != 1) {
                break;
            }
            cursor = next.get(0);
        }
        return path;
    }

    private static List<GraphNode> execChildren(WorkflowJob job, String id) {
        List<GraphNode> next = new ArrayList<>();
        if (job.edges == null) {
            return next;
        }
        for (GraphEdge edge : job.edges) {
            if (edge == null || !id.equals(edge.source) || !edge.sourceHandleOrDefault().startsWith("exec_")) {
                continue;
            }
            if (!edge.targetHandleOrDefault().startsWith("exec_")) {
                continue;
            }
            GraphNode child = job.node(edge.target);
            if (child != null) {
                next.add(child);
            }
        }
        return next;
    }
}
