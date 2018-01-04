/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Tamas Szabo - initial API and implementation
 *******************************************************************************/

package org.eclipse.viatra.query.runtime.base.itc.graphimpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.viatra.query.runtime.base.itc.alg.misc.scc.SCC;
import org.eclipse.viatra.query.runtime.base.itc.alg.misc.scc.SCCResult;
import org.eclipse.viatra.query.runtime.base.itc.igraph.IBiDirectionalGraphDataSource;
import org.eclipse.viatra.query.runtime.base.itc.igraph.IGraphDataSource;
import org.eclipse.viatra.query.runtime.base.itc.igraph.IGraphObserver;
import org.eclipse.viatra.query.runtime.matchers.util.CollectionsFactory;

public class Graph<V> implements IGraphDataSource<V>, IBiDirectionalGraphDataSource<V> {

    // source -> target -> count
    private Map<V, Map<V, Integer>> outgoingEdges;
    // target -> source -> count
    private Map<V, Map<V, Integer>> incomingEdges;
    private List<IGraphObserver<V>> observers;

    public Graph() {
        outgoingEdges = CollectionsFactory.createMap();
        incomingEdges = CollectionsFactory.createMap();
        observers = CollectionsFactory.createObserverList();
    }

    public void insertEdge(V source, V target) {
        Map<V, Integer> outgoing = outgoingEdges.get(source);
        if (outgoing == null) {
            outgoing = CollectionsFactory.createMap();
            outgoingEdges.put(source, outgoing);
        }
        Integer count = outgoing.get(target);
        if (count == null) {
            count = 0;
        }
        count++;
        outgoing.put(target, count);

        Map<V, Integer> incoming = incomingEdges.get(target);
        if (incoming == null) {
            incoming = CollectionsFactory.createMap();
            incomingEdges.put(target, incoming);
        }
        count = incoming.get(source);
        if (count == null) {
            count = 0;
        }
        count++;
        incoming.put(source, count);

        for (IGraphObserver<V> go : observers) {
            go.edgeInserted(source, target);
        }
    }

    public void deleteEdge(V source, V target) {
        boolean containedEdge = false;
        Integer count = null;

        Map<V, Integer> outgoing = outgoingEdges.get(source);
        if (outgoing != null) {
            count = outgoing.get(target);
            if (count != null) {
                containedEdge = true;
                count--;

                if (count == 0) {
                    outgoing.remove(target);
                } else {
                    outgoing.put(target, count);
                }
            }
        }

        Map<V, Integer> incoming = incomingEdges.get(target);
        if (incoming != null) {
            count = incoming.get(source);
            if (count != null) {
                count--;

                if (count == 0) {
                    incoming.remove(source);
                } else {
                    incoming.put(source, count);
                }
            }
        }

        if (containedEdge) {
            for (IGraphObserver<V> go : observers) {
                go.edgeDeleted(source, target);
            }
        }
    }

    public void insertNode(V node) {
        if (!outgoingEdges.containsKey(node)) {
            outgoingEdges.put(node, null);
        }
        if (!incomingEdges.containsKey(node)) {
            incomingEdges.put(node, null);
        }

        for (IGraphObserver<V> go : observers) {
            go.nodeInserted(node);
        }
    }

    public void deleteNode(V node) {
        boolean containedNode = outgoingEdges.containsKey(node);
        Map<V, Integer> incoming = incomingEdges.get(node);
        Map<V, Integer> outgoing = outgoingEdges.get(node);

        if (incoming != null) {
            Map<V, Integer> _incoming = CollectionsFactory.createMap(incoming);

            for (Entry<V, Integer> entry : _incoming.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    deleteEdge(entry.getKey(), node);
                }
            }
        }

        if (outgoing != null) {
            Map<V, Integer> _outgoing = CollectionsFactory.createMap(outgoing);

            for (Entry<V, Integer> entry : _outgoing.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    deleteEdge(node, entry.getKey());
                }
            }
        }

        if (containedNode) {
            for (IGraphObserver<V> go : observers) {
                go.nodeDeleted(node);
            }
        }
    }

    @Override
    public void attachObserver(IGraphObserver<V> go) {
        observers.add(go);
    }

    @Override
    public void attachAsFirstObserver(IGraphObserver<V> observer) {
        observers.add(0, observer);
    }

    @Override
    public void detachObserver(IGraphObserver<V> go) {
        observers.remove(go);
    }

    @Override
    public Set<V> getAllNodes() {
        return outgoingEdges.keySet();
    }

    @Override
    public Map<V, Integer> getTargetNodes(V source) {
        Map<V, Integer> result = outgoingEdges.get(source);
        if (result == null) {
            return Collections.emptyMap();
        } else {
            return result;
        }
    }

    @Override
    public Map<V, Integer> getSourceNodes(V target) {
        Map<V, Integer> result = incomingEdges.get(target);
        if (result == null) {
            return Collections.emptyMap();
        } else {
            return result;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("nodes = ");
        for (V n : outgoingEdges.keySet()) {
            sb.append(n.toString());
            sb.append(" ");
        }
        sb.append(" edges = ");
        for (Entry<V, Map<V, Integer>> outgoingEntry: outgoingEdges.entrySet()) {
            V source = outgoingEntry.getKey();
            Map<V, Integer> targets = outgoingEntry.getValue();
            if (targets != null) {
                for (Entry<V, Integer> targetEntry : targets.entrySet()) {
                    for (int i = 0; i < targetEntry.getValue(); i++) {
                        sb.append("(" + source + "," + targetEntry.getKey() + ") ");
                    }
                }
            }
        }
        return sb.toString();
    }

    private static final String[] colors = new String[] { "yellow", "blue", "red", "green", "gray", "cyan" };

    /**
     * @since 2.0
     */
    public String generateDot(boolean colorSCCs, Function<V, String> nameMapper, Function<V, String> colorMapper) {
        Map<V, String> colorMap = new HashMap<V, String>();

        if (colorSCCs) {
            SCCResult<V> result = SCC.computeSCC(this);
            Set<Set<V>> sccs = result.getSccs();

            int i = 0;
            for (Set<V> scc : sccs) {
                if (scc.size() > 1) {
                    for (V node : scc) {
                        String color = colorMap.get(node);
                        if (color == null) {
                            colorMap.put(node, colors[i % colors.length]);
                        } else {
                            colorMap.put(node, colorMap.get(node) + ":" + colors[i % colors.length]);
                        }
                    }
                    i++;
                }
            }

            // if a node has no color yet, then make it white
            for (V node : outgoingEdges.keySet()) {
                if (!colorMap.containsKey(node)) {
                    colorMap.put(node, "white");
                }
            }
        } else {
            for (V node : outgoingEdges.keySet()) {
                colorMap.put(node, "white");
            }
        }

        if (colorMapper != null) {
            for (V node : outgoingEdges.keySet()) {
                colorMap.put(node, colorMapper.apply(node));
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("digraph g {\n");

        for (V node : outgoingEdges.keySet()) {
            String nodePresentation = nameMapper == null ? node.toString() : nameMapper.apply(node);
            builder.append("\"" + nodePresentation + "\"");
            builder.append("[style=filled,fillcolor=" + colorMap.get(node) + "]");
            builder.append(";\n");
        }

        for (Entry<V, Map<V, Integer>> outgoingEntry : outgoingEdges.entrySet()) {
            V source = outgoingEntry.getKey();
            Map<V, Integer> targets = outgoingEntry.getValue();
            if (targets != null) {
                for (Entry<V, Integer> entry : targets.entrySet()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        String sourcePresentation = nameMapper == null ? source.toString() : nameMapper.apply(source);
                        String targetPresentation = nameMapper == null ? entry.getKey().toString()
                                : nameMapper.apply(entry.getKey());
                        builder.append("\"" + sourcePresentation + "\" -> \"" + targetPresentation + "\";\n");
                    }
                }
            }
        }

        builder.append("}");
        return builder.toString();
    }

    /**
     * @since 1.6
     */
    public String generateDot() {
        return generateDot(false, null, null);
    }

    public Integer[] deleteRandomEdge() {
        return null;
    }

    public Integer[] insertRandomEdge() {
        return null;
    }
}
