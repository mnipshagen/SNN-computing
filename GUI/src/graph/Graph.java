package graph;

import edges.IEdge;
import cells.ICell;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import layout.Layout;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

public class Graph {

    private final MainApp app;
    private final Model model;
    private final PannableCanvas pannableCanvas;
    private final Map<IGraphNode, Region> graphics;
    private final NodeGestures nodeGestures;
    private final ViewportGestures viewportGestures;
    private final BooleanProperty useNodeGestures;
    private final BooleanProperty useViewportGestures;

    public Graph(MainApp app) {
        this(app, new Model());
    }

    public Graph(MainApp app, Model model) {
        this.app = app;
        this.model = model;
        this.model.setGraph(this);

        nodeGestures = new NodeGestures(this);
        useNodeGestures = new SimpleBooleanProperty(true);
        useNodeGestures.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                model.getAllCells().forEach(cell -> nodeGestures.makeDraggable(getGraphic(cell)));
            } else {
                model.getAllCells().forEach(cell -> nodeGestures.makeUndraggable(getGraphic(cell)));
            }
        });

        pannableCanvas = new PannableCanvas();
        viewportGestures = new ViewportGestures(pannableCanvas);
        useViewportGestures = new SimpleBooleanProperty(true);
        useViewportGestures.addListener((obs, oldVal, newVal) -> {
            final Parent parent = pannableCanvas.parentProperty().get();
            if (parent == null) {
                return;
            }
            if (newVal) {
                parent.addEventHandler(MouseEvent.MOUSE_PRESSED, viewportGestures.getOnMousePressedEventHandler());
                parent.addEventHandler(MouseEvent.MOUSE_DRAGGED, viewportGestures.getOnMouseDraggedEventHandler());
                parent.addEventHandler(ScrollEvent.ANY, viewportGestures.getOnScrollEventHandler());
            } else {
                parent.removeEventHandler(MouseEvent.MOUSE_PRESSED, viewportGestures.getOnMousePressedEventHandler());
                parent.removeEventHandler(MouseEvent.MOUSE_DRAGGED, viewportGestures.getOnMouseDraggedEventHandler());
                parent.removeEventHandler(ScrollEvent.ANY, viewportGestures.getOnScrollEventHandler());
            }
        });
        pannableCanvas.parentProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null) {
                oldVal.removeEventHandler(MouseEvent.MOUSE_PRESSED, viewportGestures.getOnMousePressedEventHandler());
                oldVal.removeEventHandler(MouseEvent.MOUSE_DRAGGED, viewportGestures.getOnMouseDraggedEventHandler());
                oldVal.removeEventHandler(ScrollEvent.ANY, viewportGestures.getOnScrollEventHandler());
            }
            if (newVal != null) {
                newVal.addEventHandler(MouseEvent.MOUSE_PRESSED, viewportGestures.getOnMousePressedEventHandler());
                newVal.addEventHandler(MouseEvent.MOUSE_DRAGGED, viewportGestures.getOnMouseDraggedEventHandler());
                newVal.addEventHandler(ScrollEvent.ANY, viewportGestures.getOnScrollEventHandler());
            }
        });

        graphics = new HashMap<>();

        addEdges(getModel().getAllEdges());
        addCells(getModel().getAllCells());
    }

    public MainApp getApp() {
        return app;
    }

    public Model getModel() {
        return model;
    }

    public PannableCanvas getCanvas() {
        return pannableCanvas;
    }

    public void beginUpdate() {
        pannableCanvas.getChildren().clear();
        this.model.beginUpdate();
    }

    public void endUpdate() {
        // add components to graph pane
        addEdges(model.getAddedEdges());
        addCells(model.getAddedCells());

        // remove components to graph pane
        model.getRemovedCells().stream().map(cell -> getGraphic(cell)).forEach(cellGraphic -> pannableCanvas.getChildren().remove(cellGraphic));
        model.getRemovedEdges().stream().map(edge -> getGraphic(edge)).forEach(edgeGraphic -> pannableCanvas.getChildren().remove(edgeGraphic));

        // clean up the model
        model.endUpdate();
        
        System.out.println("__________");
        System.out.println(pannableCanvas.getChildren().size());
        for (Object child: pannableCanvas.getChildren()) {
            System.out.println(child);
        }
    }

    private void addEdges(List<IEdge> edges) {
        edges.stream().map(edge -> getGraphic(edge)).forEach(edgeGraphic -> 
                pannableCanvas.getChildren().add(edgeGraphic));
    }

    private void addCells(List<ICell> cells) {
        cells.stream().map(cell -> getGraphic(cell)).forEach(cellGraphic -> {
            pannableCanvas.getChildren().add(cellGraphic);
            if (useNodeGestures.get()) {
                nodeGestures.makeDraggable(cellGraphic);
            }
        });
    }

    public Region getGraphic(IGraphNode node) {
        if (!graphics.containsKey(node)) {
            graphics.put(node, createGraphic(node));
        }
        return graphics.get(node);
    }

    public Region createGraphic(IGraphNode node) {
        return node.getGraphic(this);
    }

    public double getScale() {
        return pannableCanvas.getScale();
    }

    public void layout(Layout layout) {
        layout.execute(this);
    }

    public NodeGestures getNodeGestures() {
        return nodeGestures;
    }

    public BooleanProperty getUseNodeGestures() {
        return useNodeGestures;
    }

    public ViewportGestures getViewportGestures() {
        return viewportGestures;
    }

    public BooleanProperty getUseViewportGestures() {
        return useViewportGestures;
    }
}