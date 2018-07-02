/*******************************************************************************
 * Copyright (c) 2015, 2016 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API & implementation
 *     Alexander Nyßen (itemis AG) - refactorings
 *
 * Note: Parts of this class have been transferred from org.eclipse.gef4.zest.core.viewers.GraphViewer
 *
 *******************************************************************************/
package org.eclipse.viatra.integration.zest.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.common.adapt.AdapterKey;
import org.eclipse.gef.fx.swt.canvas.IFXCanvasFactory;
import org.eclipse.gef.graph.Edge;
import org.eclipse.gef.graph.Graph;
import org.eclipse.gef.graph.Node;
import org.eclipse.gef.layout.ILayoutAlgorithm;
import org.eclipse.gef.mvc.fx.domain.IDomain;
import org.eclipse.gef.mvc.fx.viewer.IViewer;
import org.eclipse.gef.mvc.fx.models.SelectionModel;
import org.eclipse.gef.mvc.fx.parts.IContentPart;
import org.eclipse.gef.zest.fx.ZestProperties;
import org.eclipse.gef.zest.fx.jface.IGraphAttributesProvider;
import org.eclipse.gef.zest.fx.jface.IGraphContentProvider;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import javafx.collections.ListChangeListener;
import javafx.embed.swt.FXCanvas;
import javafx.embed.swt.SWTFXUtils;
import javafx.scene.Scene;

/**
 * The {@link ZestContentViewer} is a {@link ContentViewer} that is capable of
 * displaying {@link Graph}s.
 *
 * @author mwienand
 * @author anyssen
 *
 */
public class ZestContentViewer extends ContentViewer {

    private ListChangeListener<IContentPart<? extends javafx.scene.Node>> selectionNotifier = new ListChangeListener<IContentPart<? extends javafx.scene.Node>>() {
        @Override
        public void onChanged(ListChangeListener.Change<? extends IContentPart<? extends javafx.scene.Node>> c) {
            fireSelectionChanged(new SelectionChangedEvent(ZestContentViewer.this, getSelection()));
        }
    };

    private Injector injector;
    private FXCanvas canvas;
    private IDomain domain;
    private IViewer viewer;
    private ILayoutAlgorithm layoutAlgorithm;
    protected BiMap<Object, Node> contentNodeMap = HashBiMap.create();
    protected BiMap<Node, Object> inverseContentNodeMap = contentNodeMap.inverse();
    protected BiMap<Object, Edge> contentEdgeMap = HashBiMap.create();
    protected BiMap<Edge, Object> inverseContentEdgeMap = contentEdgeMap.inverse();

    /**
     * Constructs a new {@link ZestContentViewer}. The given {@link Module} is
     * saved so that it can be later used to create an {@link Injector} that is
     * later used for the injection of members and the construction of the
     * {@link FXDomain}.
     *
     * @param module
     *            The {@link Module} from which an {@link Injector} is created
     *            later.
     */
    public ZestContentViewer(Module module) {
        injector = Guice.createInjector(module);
        // injector.injectMembers(this);
    }

    /**
     * Creates an {@link FXCanvas} inside of the given <i>parent</i>
     * {@link Composite}. The {@link FXCanvas} serves acs the container for the
     * JavaFX {@link Scene} which renders the contents.
     *
     * @param parent
     *            The parent {@link Composite}.
     * @param style
     *            The SWT style bits to be used for the to be created canvas.
     * @return An {@link FXCanvas} inside of the given <i>parent</i>.
     */
    protected FXCanvas createCanvas(final Composite parent, int style) {
        IFXCanvasFactory canvasFactory = injector.getInstance(IFXCanvasFactory.class);
        return canvasFactory.createCanvas(parent, style);
    }

    /**
     * Creates the control for this {@link ZestContentViewer} inside of the
     * given <i>parent</i> {@link Composite}.
     *
     * @param parent
     *            The parent {@link Composite}.
     * @param style
     *            The SWT style for this {@link ZestContentViewer}, which will
     *            be forwarded to its {@link FXCanvas} control.
     */
    public void createControl(Composite parent, int style) {
        // create viewer and canvas only after toolkit has been initialized
        canvas = createCanvas(parent, style);

        // inject domain
        domain = injector.getInstance(IDomain.class);

        // hook viewer
        viewer = domain.getAdapter(AdapterKey.get(IViewer.class, IDomain.CONTENT_VIEWER_ROLE));
        canvas.setScene(new Scene(viewer.getCanvas()));

        getSelectionModel().getSelectionUnmodifiable().addListener(selectionNotifier);

        // activate domain
        domain.activate();
    }

    /**
     * Constructs and returns a new {@link Edge} connecting the given
     * <i>sourceNode</i> and <i>targetNode</i>. If the <i>labelProvider</i>
     * implements {@link IGraphAttributesProvider}, then attributes for the edge
     * are determined using the
     * {@link IGraphAttributesProvider#getEdgeAttributes(Object, Object)}
     * methods and inserted into the edge.
     *
     * @param labelProvider
     *            This viewer's {@link ILabelProvider} for convenience.
     * @param contentSourceNode
     *            The content element representing the source node of this edge.
     * @param sourceNode
     *            The already created source {@link Node} of this edge.
     * @param contentTargetNode
     *            The content element representing the target node of this edge.
     * @param targetNode
     *            The already created target {@link Node} of this edge.
     * @return The new {@link Edge}.
     */
    protected Edge createEdge(ILabelProvider labelProvider, Object contentSourceNode, Node sourceNode,
            Object contentTargetNode, Node targetNode) {
        Edge edge = new Edge(sourceNode, targetNode);
        if (labelProvider instanceof IGraphAttributesProvider) {
            IGraphAttributesProvider graphNodeLabelProvider = (IGraphAttributesProvider) labelProvider;
            Map<String, Object> edgeAttributes = graphNodeLabelProvider.getEdgeAttributes(contentSourceNode,
                    contentTargetNode);
            if (edgeAttributes != null) {
                edge.attributesProperty().putAll(edgeAttributes);
            }
        }
        return edge;
    }
    
    /**
     * Constructs and returns a new {@link Edge} connecting the given
     * <i>sourceNode</i> and <i>targetNode</i>. If the <i>labelProvider</i>
     * implements {@link IGraphAttributesProvider}, then attributes for the edge
     * are determined using the
     * {@link IGraphAttributesProvider#getEdgeAttributes(Object, Object)}
     * methods and inserted into the edge.
     *
     * @param labelProvider
     *            This viewer's {@link ILabelProvider} for convenience.
     * @param contentSourceNode
     *            The content element representing the source node of this edge.
     * @param sourceNode
     *            The already created source {@link Node} of this edge.
     * @param contentTargetNode
     *            The content element representing the target node of this edge.
     * @param targetNode
     *            The already created target {@link Node} of this edge.
     * @return The new {@link Edge}.
     */
    protected Edge createEdge(ILabelProvider labelProvider, Object contentEdgeNode, Node sourceNode,
            Node targetNode) {
        Edge edge = new Edge(sourceNode, targetNode);
        doFormatEdge(contentEdgeNode, labelProvider, edge);
        return edge;
    }

    private void doFormatEdge(Object contentEdgeNode, ILabelProvider labelProvider, Edge edge) {
        ZestProperties.setLabel(edge, () -> labelProvider.getText(contentEdgeNode));
        if (labelProvider instanceof IGraphAttributesProvider2) {
            IGraphAttributesProvider2 graphNodeLabelProvider = (IGraphAttributesProvider2) labelProvider;
            Map<String, Object> edgeAttributes = graphNodeLabelProvider.getEdgeAttributes(contentEdgeNode);
            if (edgeAttributes != null) {
                edge.attributesProperty().putAll(edgeAttributes);
            }
        }
    }
    /**
     * Constructs and returns a new {@link Graph} and inserts default attributes
     * into it:
     * <ol>
     * <li>layout algorithm</li>
     * </ol>
     *
     * @return A new {@link Graph} with default attributes.
     */
    protected Graph createEmptyGraph() {
        Graph graph = new Graph();
        if (layoutAlgorithm != null) {
            ZestProperties.setLayoutAlgorithm(graph, layoutAlgorithm);
        }
        return graph;
    }

    
    protected void createNestedGraph(Object contentNestingNode, IContentProvider contentProvider,
            ILabelProvider labelProvider) {
        if (contentProvider instanceof IGraphContentProvider && ((IGraphContentProvider) contentProvider).hasNestedGraph(contentNestingNode)) {
            Graph graph = createNestedGraph(contentNestingNode, (IGraphContentProvider)contentProvider, labelProvider);
            graph.setNestingNode(contentNodeMap.get(contentNestingNode));
        } else if (contentProvider instanceof INestedGraphContentProvider && ((INestedGraphContentProvider) contentProvider).hasNestedGraph(contentNestingNode)) {
            Graph graph = createNestedGraph(contentNestingNode, (INestedGraphContentProvider)contentProvider, labelProvider);
            graph.setNestingNode(contentNodeMap.get(contentNestingNode));
        }
    }
    
    /**
     * Creates a {@link Graph} nested in the node represented by the given
     * <i>contentNestingNode</i>.
     *
     * @param contentNestingNode
     *            The content {@link Object} that represents the nesting node.
     * @param graphContentProvider
     *            This viewer's {@link IGraphContentProvider} for convenience.
     * @param labelProvider
     *            This viewer's {@link ILabelProvider} for convenience.
     * @return The new {@link Graph}.
     */
    protected Graph createNestedGraph(Object contentNestingNode, IGraphContentProvider graphContentProvider,
            ILabelProvider labelProvider) {
        Graph graph = createEmptyGraph();
        if (labelProvider instanceof IGraphAttributesProvider) {
            IGraphAttributesProvider nestedGraphLabelProvider = (IGraphAttributesProvider) labelProvider;
            Map<String, Object> nestedGraphAttributes = nestedGraphLabelProvider
                    .getNestedGraphAttributes(contentNestingNode);
            if (nestedGraphAttributes != null) {
                graph.attributesProperty().putAll(nestedGraphAttributes);
            }
        }
        Object[] contentNodes = graphContentProvider.getNestedGraphNodes(contentNestingNode);
        if (contentNodes != null) {
            createNodesAndEdges(graphContentProvider, labelProvider, graph, contentNodes);
        }
        return graph;
    }
    
    /**
     * Creates a {@link Graph} nested in the node represented by the given
     * <i>contentNestingNode</i>.
     *
     * @param contentNestingNode
     *            The content {@link Object} that represents the nesting node.
     * @param graphContentProvider
     *            This viewer's {@link IGraphContentProvider} for convenience.
     * @param labelProvider
     *            This viewer's {@link ILabelProvider} for convenience.
     * @return The new {@link Graph}.
     */
    protected Graph createNestedGraph(Object contentNestingNode, INestedGraphContentProvider graphContentProvider,
            ILabelProvider labelProvider) {
        Graph graph = createEmptyGraph();
        if (labelProvider instanceof IGraphAttributesProvider) {
            IGraphAttributesProvider nestedGraphLabelProvider = (IGraphAttributesProvider) labelProvider;
            Map<String, Object> nestedGraphAttributes = nestedGraphLabelProvider
                    .getNestedGraphAttributes(contentNestingNode);
            if (nestedGraphAttributes != null) {
                graph.attributesProperty().putAll(nestedGraphAttributes);
            }
        }
        Object[] contentNodes = graphContentProvider.getNestedGraphNodes(contentNestingNode);
        if (contentNodes != null) {
            createNodesAndEdges(graphContentProvider, labelProvider, graph, contentNodes);
        }
        return graph;
    }

    /**
     * Creates a {@link Node} for the specified <i>contentNode</i> using the
     * {@link IContentProvider} and {@link ILabelProvider}. Moreover, the new
     * node is put into the given <i>contentToGraphMap</i>.
     *
     * @param contentNode
     *            The content {@link Object} that represents the node.
     * @param graphContentProvider
     *            This viewer's {@link IGraphContentProvider} for convenience.
     * @param labelProvider
     *            This viewer's {@link ILabelProvider} for convenience.
     * @return The new {@link Node}.
     */
    protected Node createNode(final Object contentNode, IContentProvider graphContentProvider,
            final ILabelProvider labelProvider) {
        // do not create the same node twice
        if (contentNodeMap.containsKey(contentNode)) {
            throw new IllegalStateException("A node for content <" + contentNode + "> has already been created.");
        }

        Node node = new Node();
        contentNodeMap.put(contentNode, node);

        doFormatNode(contentNode, graphContentProvider, labelProvider, node);

        return node;
    }

    private void doFormatNode(final Object contentNode, IContentProvider graphContentProvider,
            final ILabelProvider labelProvider, Node node) {
        // label
        ZestProperties.setLabel(node, () -> labelProvider.getText(contentNode));

        // icon
        // TODO: use provider
        Image icon = labelProvider.getImage(contentNode);
        if (icon != null) {
            ZestProperties.setIcon(node, SWTFXUtils.toFXImage(icon.getImageData(), null));
        }

        // tooltip
        if (labelProvider instanceof IToolTipProvider) {
            final IToolTipProvider toolTipProvider = (IToolTipProvider) labelProvider;
            ZestProperties.setTooltip(node, () -> toolTipProvider.getToolTipText(contentNode));
        }

        String textCssStyle = "";

        // colors
        if (labelProvider instanceof IColorProvider) {
            IColorProvider colorProvider = (IColorProvider) labelProvider;
            Color foreground = colorProvider.getForeground(contentNode);
            Color background = colorProvider.getBackground(contentNode);
            String rectCssStyle = "";
            if (background != null) {
                rectCssStyle = rectCssStyle + "-fx-fill: " + toCssRgb(background) + ";";
            }
            if (foreground != null) {
                rectCssStyle = rectCssStyle + "-fx-stroke: " + toCssRgb(foreground) + ";";
                textCssStyle = "-fx-fill: " + toCssRgb(foreground) + ";";
            }
            if (!rectCssStyle.isEmpty()) {
                ZestProperties.setShapeCssStyle(node, rectCssStyle);
            }
        }

        // font
        if (labelProvider instanceof IFontProvider) {
            IFontProvider fontProvider = (IFontProvider) labelProvider;
            Font font = fontProvider.getFont(contentNode);
            FontData[] fontData = font == null ? null : font.getFontData();
            if (fontData != null && fontData.length > 0 && fontData[0] != null) {
                String name = fontData[0].getName();
                int size = fontData[0].getHeight();
                int style = fontData[0].getStyle();

                // TODO: support all SWT font styles
                boolean isBold = (style & SWT.BOLD) != 0;
                boolean isItalic = (style & SWT.ITALIC) != 0;

                textCssStyle = textCssStyle + "-fx-font-family: \"" + name + "\";" + "-fx-font-size: " + size + "pt;";
                if (isItalic) {
                    textCssStyle = textCssStyle + "-fx-font-style: italic;";
                }
                if (isBold) {
                    textCssStyle = textCssStyle + "-fx-font-weight: bold;";
                }
            }
        }

        ZestProperties.setLabelCssStyle(node, textCssStyle);

        // custom attributes
        if (labelProvider instanceof IGraphAttributesProvider) {
            IGraphAttributesProvider graphNodeLabelProvider = (IGraphAttributesProvider) labelProvider;
            Map<String, Object> nodeAttributes = graphNodeLabelProvider.getNodeAttributes(contentNode);
            if (nodeAttributes != null) {
                node.attributesProperty().putAll(nodeAttributes);
            }
        }

        // create nested graph (optional)
        createNestedGraph(contentNode, graphContentProvider, labelProvider);
    }
    
    protected void createNodesAndEdges(IContentProvider contentProvider, ILabelProvider labelProvider, Graph graph, Object[] contentNodes) {
        if (contentProvider instanceof IGraphContentProvider) {
            createNodesAndEdges((IGraphContentProvider)contentProvider, labelProvider, graph, contentNodes);
        } else if (contentProvider instanceof IGraphEdgeContentProvider) {
            createNodesAndEdges((IGraphEdgeContentProvider)contentProvider, labelProvider, graph, contentNodes);
        } else if (contentProvider instanceof IGraphEntityRelationshipContentProvider) {
            createNodesAndEdges((IGraphEntityRelationshipContentProvider)contentProvider, labelProvider, graph, contentNodes);
        } else {
            throw new UnsupportedOperationException("Content provider type " + contentProvider.getClass().getName() + " is not supported.");
        }
    }
    
    /**
     * Creates graph {@link Node nodes} and {@link Edge edges} from the given
     * array of <i>contentNodes</i>.
     *
     * @param graphContentProvider
     *            This viewer's {@link IGraphContentProvider} for convenience.
     * @param labelProvider
     *            This viewer's {@link ILabelProvider} for convenience.
     * @param graph
     *            The {@link Graph} for which nodes and edges are created.
     * @param contentNodes
     *            Content elements which represent nodes that are to be created
     *            together with the edges between them.
     */
    protected void createNodesAndEdges(IGraphContentProvider graphContentProvider, ILabelProvider labelProvider,
            Graph graph, Object[] contentNodes) {
        // create nodes
        for (Object node : contentNodes) {
            Node graphNode = createNode(node, graphContentProvider, labelProvider);
            graph.getNodes().add(graphNode);
        }
        // create edges
        for (Object contentSourceNode : contentNodes) {
            Node sourceNode = contentNodeMap.get(contentSourceNode);
            Object[] connectedTo = graphContentProvider.getAdjacentNodes(contentSourceNode);
            if (connectedTo != null) {
                for (Object contentTargetNode : connectedTo) {
                    Node targetNode = contentNodeMap.get(contentTargetNode);
                    Edge edge = createEdge(labelProvider, contentSourceNode, sourceNode, contentTargetNode, targetNode);
                    graph.getEdges().add(edge);
                }
            }
        }
    }
    
    /**
     * Creates graph {@link Node nodes} and {@link Edge edges} from the given
     * array of <i>contentNodes</i>.
     *
     * @param graphContentProvider
     *            This viewer's {@link IGraphContentProvider} for convenience.
     * @param labelProvider
     *            This viewer's {@link ILabelProvider} for convenience.
     * @param graph
     *            The {@link Graph} for which nodes and edges are created.
     * @param contentNodes
     *            Content elements which represent nodes that are to be created
     *            together with the edges between them.
     */
    protected void createNodesAndEdges(IGraphEdgeContentProvider graphContentProvider, ILabelProvider labelProvider,
            Graph graph, Object[] contentNodes) {
        // create nodes
        for (Object node : contentNodes) {
            Node graphNode = createNode(node, graphContentProvider, labelProvider);
            graph.getNodes().add(graphNode);
        }
        // create edges
        for (Object contentSourceEdge : graphContentProvider.getEdges()) {
            Object contentSourceNode = graphContentProvider.getSource(contentSourceEdge);
            Node sourceNode = getContentNodeMap().get(contentSourceNode); 
            Object contentTargetNode = graphContentProvider.getTarget(contentSourceEdge);
            Node targetNode = getContentNodeMap().get(contentTargetNode); 
            
            Edge edge = createEdge(labelProvider, contentSourceEdge, sourceNode, targetNode);
            contentEdgeMap.put(contentSourceEdge, edge);
            graph.getEdges().add(edge);
        }
    }
    
    /**
     * Creates graph {@link Node nodes} and {@link Edge edges} from the given
     * array of <i>contentNodes</i>.
     *
     * @param graphContentProvider
     *            This viewer's {@link IGraphContentProvider} for convenience.
     * @param labelProvider
     *            This viewer's {@link ILabelProvider} for convenience.
     * @param graph
     *            The {@link Graph} for which nodes and edges are created.
     * @param contentNodes
     *            Content elements which represent nodes that are to be created
     *            together with the edges between them.
     */
    protected void createNodesAndEdges(IGraphEntityRelationshipContentProvider graphContentProvider, ILabelProvider labelProvider,
            Graph graph, Object[] contentNodes) {
        // create nodes
        for (Object node : contentNodes) {
            Node graphNode = createNode(node, graphContentProvider, labelProvider);
            graph.getNodes().add(graphNode);
        }
        // create edges
        for (Object contentSourceNode : contentNodes) {
            Node sourceNode = getContentNodeMap().get(contentSourceNode); 
            for (Object contentTargetNode : contentNodes) {
                Node targetNode = getContentNodeMap().get(contentTargetNode); 
                for (Object contentSourceEdge : graphContentProvider.getEdges(contentSourceNode, contentTargetNode)) {
                    Edge edge = createEdge(labelProvider, contentSourceEdge, sourceNode, targetNode);
                    graph.getEdges().add(edge);
                }
            }
        }
    }

    /**
     * Creates a complete {@link Graph} using the given {@link IContentProvider}
     * and {@link ILabelProvider}.
     *
     * @param contentProvider
     *            The {@link IContentProvider} for this viewer.
     * @param labelProvider
     *            The {@link ILabelProvider} for this viewer.
     * @return A complete {@link Graph} constructed by using the given
     *         providers.
     */
    protected Graph createRootGraph(IContentProvider contentProvider, ILabelProvider labelProvider) {
        Graph graph = createEmptyGraph();
        if (labelProvider instanceof IGraphAttributesProvider) {
            IGraphAttributesProvider graphNodeLabelProvider = (IGraphAttributesProvider) labelProvider;
            Map<String, Object> rootGraphAttributes = graphNodeLabelProvider.getGraphAttributes();
            if (rootGraphAttributes != null) {
                graph.attributesProperty().putAll(rootGraphAttributes);
            }
        }
        if (contentProvider instanceof IGraphEdgeContentProvider) {
            IGraphEdgeContentProvider graphNodeProvider = (IGraphEdgeContentProvider) contentProvider;
            Object[] nodes = graphNodeProvider.getNodes();
            if (nodes != null) {
                createNodesAndEdges(graphNodeProvider, labelProvider, graph, nodes);
            }
        } else if (contentProvider instanceof IGraphEntityRelationshipContentProvider) {
            IGraphEntityRelationshipContentProvider graphNodeProvider = (IGraphEntityRelationshipContentProvider) contentProvider;
            Object[] nodes = graphNodeProvider.getNodes();
            if (nodes != null) {
                createNodesAndEdges(graphNodeProvider, labelProvider, graph, nodes);
            }
        } else if (contentProvider instanceof IGraphContentProvider) {
            IGraphContentProvider graphNodeProvider = (IGraphContentProvider) contentProvider;
            Object[] nodes = graphNodeProvider.getNodes();
            if (nodes != null) {
                createNodesAndEdges(graphNodeProvider, labelProvider, graph, nodes);
        }
    }
        return graph;
    }

    /**
     * Returns an unmodifiable view of the content-node-map.
     *
     * @return An unmodifiable view of the content-node-map.
     */
    public Map<Object, Node> getContentNodeMap() {
        return Collections.unmodifiableMap(contentNodeMap);
    }

    @Override
    public FXCanvas getControl() {
        return canvas;
    }

    /**
     * Returns the {@link FXViewer} that displays the contents.
     *
     * @return The {@link FXViewer} that displays the contents.
     */
    public IViewer getFXViewer() {
        return viewer;
    }

    @Override
    public ILabelProvider getLabelProvider() {
        return (ILabelProvider) super.getLabelProvider();
    }

    /**
     * Returns the {@link ILayoutAlgorithm} that is used for laying out the
     * contents.
     *
     * @return The {@link ILayoutAlgorithm} that is used for laying out the
     *         contents.
     */
    public ILayoutAlgorithm getLayoutAlgorithm() {
        return layoutAlgorithm;
    }

    @Override
    public ISelection getSelection() {
        // construct a new selection by using the selection model contents
        List<Object> selectedContents = new ArrayList<>();
        SelectionModel selectionModel = getSelectionModel();
        for (IContentPart<? extends javafx.scene.Node> selectedPart : selectionModel
                .getSelectionUnmodifiable()) {
            Object contentNode = selectedPart.getContent();
            if (inverseContentEdgeMap.containsKey(contentNode)) {
                selectedContents.add(inverseContentEdgeMap.get(contentNode));
            } else if (inverseContentNodeMap.containsKey(contentNode)) {
                selectedContents.add(inverseContentNodeMap.get(contentNode));
            } else {
                selectedContents.add(contentNode);
            }
        }
        return new StructuredSelection(selectedContents);
    }

    /**
     * Retrieves the {@link SelectionModel} used by the viewer (
     * {@link #getFXViewer()})
     *
     * @return The {@link SelectionModel} adapted to the viewer (
     *         {@link #getFXViewer()}).
     */
    protected SelectionModel getSelectionModel() {
        SelectionModel selectionModel = viewer.getAdapter(SelectionModel.class);
        if (selectionModel == null) {
            throw new IllegalStateException("No SelectionModel bound.");
        }
        return selectionModel;
    }

    @Override
    protected void handleDispose(DisposeEvent event) {
        getSelectionModel().getSelectionUnmodifiable().removeListener(selectionNotifier);

        domain.deactivate();
        domain.dispose();
        super.handleDispose(event);
    }

    @Override
    protected void inputChanged(Object input, Object oldInput) {
        if (input != oldInput && (input == null || !input.equals(oldInput))) {
            refresh();
        }
    }
    
   protected Graph getRootGraph() {
        return (Graph) viewer.getContents().get(0);
    }
    
    @Override
    public void refresh() {
        contentNodeMap.clear();
        contentEdgeMap.clear();
        viewer.getContents()
                .setAll(Collections.singletonList(createRootGraph(getContentProvider(), getLabelProvider())));
    }

    /**
     * Changes the {@link ILayoutAlgorithm} that is used for laying out the
     * contents to the given value.
     *
     * @param layoutAlgorithm
     *            The new {@link ILayoutAlgorithm} to use.
     */
    public void setLayoutAlgorithm(ILayoutAlgorithm layoutAlgorithm) {
        this.layoutAlgorithm = layoutAlgorithm;
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        if (selection.isEmpty()) {
            getSelectionModel().clearSelection();
        } else if (selection instanceof StructuredSelection) {
            StructuredSelection structuredSelection = (StructuredSelection) selection;
            if (!structuredSelection.isEmpty()) {
                List<IContentPart<? extends javafx.scene.Node>> toBeSelectedParts = new ArrayList<>();
                for (Object toBeSelectedContent : structuredSelection.toArray()) {
                    IContentPart<? extends javafx.scene.Node> toBeSelectedPart = viewer
                            .getContentPartMap().get(toBeSelectedContent);
                    if (toBeSelectedPart != null) {
                        toBeSelectedParts.add(toBeSelectedPart);
                        if (reveal) {
                            // TODO: we need to reveal all in a single step
                            viewer.reveal(toBeSelectedPart);
                        }
                    } else {
                        throw new IllegalArgumentException(
                                toBeSelectedContent + " is not visualized by a content part of this viewer.");
                    }
                }
                getSelectionModel().prependToSelection(toBeSelectedParts);
            }
        } else {
            throw new IllegalArgumentException(
                    "A non-empty selection of unsupported type '" + selection.getClass() + "' was passed in.");
        }
    }

    /**
     * Converts the given {@link Color} into a CSS string:
     * <code>"rgb(red,green,blue)"</code>.
     *
     * @param color
     *            The {@link Color} to convert.
     * @return The corresponding CSS string.
     */
    protected String toCssRgb(Color color) {
        return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
    }

    public void refresh(Object element) {
        final Node node = contentNodeMap.get(element);
        if (node == null) {
            final Edge edge = contentEdgeMap.get(element);
            if (edge != null) {
                doFormatEdge(element, getLabelProvider(), edge);
            }
        } else {
            doFormatNode(element, getContentProvider(), getLabelProvider(), node);
        }
    }
}
