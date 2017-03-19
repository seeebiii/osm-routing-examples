package de.sebastianhesse.pbf.viewer;

import de.sebastianhesse.pbf.reader.Accessor;
import de.sebastianhesse.pbf.reader.NodeEdgeReader;
import de.sebastianhesse.pbf.reader.OptimizedNodeEdgeReader;
import de.sebastianhesse.pbf.reader.SimpleNodeEdgeReader;
import de.sebastianhesse.pbf.routing.Dijkstra;
import de.sebastianhesse.pbf.routing.DijkstraOptions;
import de.sebastianhesse.pbf.routing.calculators.CalculationType;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import org.apache.commons.lang3.StringUtils;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.JMapViewerTree;
import org.openstreetmap.gui.jmapviewer.Layer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.events.JMVCommandEvent;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.JMapViewerEventListener;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;


/**
 * A class to start a Swing application which shows a map and is able to interact with imported OSM data.
 */
public class OsmMapViewer extends JFrame implements JMapViewerEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OsmMapViewer.class);
    private static final long serialVersionUID = 1L;

    private JMapViewerTree treeMap;
    private JLabel zoomLabel;
    private JLabel zoomValue;
    private JLabel mperpLabelName;
    private JLabel mperpLabelValue;

    private String osmFilePath = "";
    private boolean optimizeWays = false;
    private Graph graph;
    private Node[] routeNodes = new Node[2];
    private CalculationType calculationType = CalculationType.SHORTEST;
    private Accessor wayAccessor = Accessor.CAR;


    /**
     * Constructs the {@code Viewer}.
     */
    public OsmMapViewer(String osmFilePath, boolean optimize) throws Exception {
        super("JMapViewer Demo");
        this.osmFilePath = osmFilePath;
        this.optimizeWays = optimize;

        logger.info("Starting OsmMapViewer for OSM data of file {}. Optimizing data: {}", this.osmFilePath, this.optimizeWays);

        treeMap = new JMapViewerTree("Zones");
        setupJFrame();
        setupBasicPanels();

        // Listen to the map viewer for user operations so components will
        // receive events and updates
        map().addJMVListener(this);

        // Set some options, e.g. tile source and that markers are visible
        map().setTileSource(new OsmTileSource.Mapnik());
        map().setTileLoader(new OsmTileLoader(map()));
        map().setMapMarkerVisible(true);
        map().setZoomContolsVisible(true);

        // activate map
        treeMap.setTreeVisible(true);
        add(treeMap, BorderLayout.CENTER);

        map().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    map().getAttribution().handleAttribution(e.getPoint(), true);
                    ICoordinate position = map().getPosition(e.getPoint());
                    logger.info("User clicked Point: {" + position.getLat() + ", " + position.getLon() + "}");
                    Optional<Node> nodeOptional = graph.findClosestNode(position.getLat(), position.getLon());
                    nodeOptional.ifPresent(node -> {
                        logger.info("Found closest point: ({},{})", node.getLat(), node.getLon());
                        map().addMapMarker(new MapMarkerDot(node.getLat(), node.getLon()));

                        if (routeNodes[0] == null) {
                            routeNodes[0] = node;
                        } else {
                            if (routeNodes[1] != null) {
                                routeNodes[0] = routeNodes[1];
                            }
                            routeNodes[1] = node;
                        }

                        if (routeNodes[1] != null) {
                            runDijkstra();
                        }
                    });
                }
            }
        });
    }


    private void runDijkstra() {
        try {
            Node source = routeNodes[0];
            Node target = routeNodes[1];
            Dijkstra dijkstra = new Dijkstra(graph, source, target, new DijkstraOptions(this.wayAccessor, this.calculationType));
            dijkstra.start();
            dijkstra.join();

            List<Node> shortestPath = dijkstra.retrieveShortestPath().path;

            Layer routeLayer = new Layer("From " + source.getId() + " to " + target.getId());
            for (Node pathNode : shortestPath) {
                MapMarkerDot marker = new MapMarkerDot(routeLayer, pathNode.getLat(), pathNode.getLon());
                map().addMapMarker(marker);
            }

            treeMap.addLayer(routeLayer);
        } catch (InterruptedException ex) {
            logger.error("", ex);
        }
    }


    private void setupJFrame() {
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }


    private void setupBasicPanels() {
        JPanel panel = new JPanel();
        add(panel, BorderLayout.PAGE_START);

        JPanel panelTop = new JPanel();
        mperpLabelName = new JLabel("Meters/Pixels: ");
        mperpLabelValue = new JLabel(String.format("%s", map().getMeterPerPixel()));
        zoomLabel = new JLabel("Zoom: ");
        zoomValue = new JLabel(String.format("%s", map().getZoom()));
        panelTop.add(zoomLabel);
        panelTop.add(zoomValue);
        panelTop.add(mperpLabelName);
        panelTop.add(mperpLabelValue);
        panel.add(panelTop, BorderLayout.CENTER);

        JPanel helpPanel = new JPanel();
        JLabel helpLabel = new JLabel("Use right mouse button to move,\n "
                + "left double click or mouse wheel to zoom.");
        helpPanel.add(helpLabel);
        add(helpPanel, BorderLayout.PAGE_END);
    }


    public void importOsmData() throws Exception {
        // import graph data
        NodeEdgeReader reader;
        if (this.optimizeWays) {
            reader = new OptimizedNodeEdgeReader(this.osmFilePath);
        } else {
            reader = new SimpleNodeEdgeReader(this.osmFilePath);
        }

        this.graph = reader.importData().getGraph();
        // show nodes and edges on the left
        addNodeEdgePanel();
        // show borders of data on map
        visualizeDataBoundariesOnMap();
    }


    private void addNodeEdgePanel() {
        logger.info("--- Starting to add nodes and edges to panel. ---");
        JPanel nodeEdgePanel = new JPanel(new BorderLayout());

        JPanel nodePanel = new JPanel(new BorderLayout());
        JLabel nodeLabel = new JLabel("Nodes:");
        JScrollPane nodeScrollPane = createNodeScrollPane();
        nodePanel.add(nodeLabel, BorderLayout.PAGE_START);
        nodePanel.add(nodeScrollPane, BorderLayout.CENTER);

        JPanel edgePanel = new JPanel(new BorderLayout());
        JLabel edgeLabel = new JLabel("Edges:");
        JScrollPane edgeScrollPane = createEdgeScrollPane();
        edgePanel.add(edgeLabel, BorderLayout.PAGE_START);
        edgePanel.add(edgeScrollPane, BorderLayout.CENTER);

        nodeEdgePanel.add(nodePanel, BorderLayout.PAGE_START);
        nodeEdgePanel.add(edgePanel, BorderLayout.PAGE_END);
        nodeEdgePanel.setVisible(true);

        add(nodeEdgePanel, BorderLayout.LINE_START);
        addRoutingOptions();
    }


    private void addRoutingOptions() {
        JPanel radioPanel = new JPanel(new BorderLayout());
        radioPanel.setSize(100, 100);

        addRoutingTypePanel(radioPanel);
        addWayAccessorPanel(radioPanel);

        add(radioPanel, BorderLayout.LINE_END);
    }


    private void addRoutingTypePanel(JPanel parent) {
        JPanel routingTypePanel = new JPanel(new BorderLayout());
        JLabel routingTypeLabel = new JLabel("Routing type:");
        routingTypePanel.add(routingTypeLabel, BorderLayout.PAGE_START);

        JRadioButton fastestRoute = new JRadioButton("Fastest");
        fastestRoute.setSelected(false);
        fastestRoute.setActionCommand(CalculationType.FASTEST.name());
        fastestRoute.addActionListener(e -> {
            setCalulcationType(e.getActionCommand());
        });
        routingTypePanel.add(fastestRoute, BorderLayout.LINE_START);

        JRadioButton shortestRoute = new JRadioButton("Shortest");
        shortestRoute.setSelected(true);
        shortestRoute.setActionCommand(CalculationType.SHORTEST.name());
        shortestRoute.addActionListener(e -> {
            setCalulcationType(e.getActionCommand());
        });
        routingTypePanel.add(shortestRoute, BorderLayout.LINE_END);

        ButtonGroup calculationTypeGroup = new ButtonGroup();
        calculationTypeGroup.add(fastestRoute);
        calculationTypeGroup.add(shortestRoute);

        parent.add(routingTypePanel, BorderLayout.PAGE_START);
    }


    private void addWayAccessorPanel(JPanel parent) {
        JPanel wayAccessorPanel = new JPanel(new BorderLayout());

        JLabel wayAccessorLabel = new JLabel("Way Accessor:");
        wayAccessorPanel.add(wayAccessorLabel, BorderLayout.PAGE_START);

        JRadioButton carAccessor = new JRadioButton("Car");
        carAccessor.setSelected(true);
        carAccessor.setActionCommand(Accessor.CAR.name());
        carAccessor.addActionListener(e -> {
            setWayAccessor(e.getActionCommand());
        });
        wayAccessorPanel.add(carAccessor, BorderLayout.LINE_START);

        JRadioButton pedestrianAccessor = new JRadioButton("Pedestrian");
        pedestrianAccessor.setSelected(false);
        pedestrianAccessor.setActionCommand(Accessor.PEDESTRIAN.name());
        pedestrianAccessor.addActionListener(e -> {
            setWayAccessor(e.getActionCommand());
        });
        wayAccessorPanel.add(pedestrianAccessor, BorderLayout.LINE_END);

        ButtonGroup wayAccessorGroup = new ButtonGroup();
        wayAccessorGroup.add(carAccessor);
        wayAccessorGroup.add(pedestrianAccessor);

        parent.add(wayAccessorPanel, BorderLayout.PAGE_END);
    }


    private void setWayAccessor(String actionCommand) {
        this.wayAccessor = Accessor.valueOf(actionCommand);
    }


    private void setCalulcationType(String type) {
        this.calculationType = CalculationType.valueOf(type);
    }


    private JScrollPane createNodeScrollPane() {
        String[] nodeColumnNames = new String[]{"#", "Latitude", "Longitude", "Offset"};
        Node[] nodes = this.graph.getNodes();
        Object[][] rows = new Object[nodes.length][];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new Object[]{i, nodes[i].getLat(), nodes[i].getLon(), nodes[i].getOffsetPointer()};
            if (i % 100000 == 0) {
                logger.debug(i + " nodes.");
            }
        }

        JScrollPane scrollPane = getScrollPane(nodeColumnNames, rows);
        logger.info("Added all nodes.");
        return scrollPane;
    }


    private JScrollPane createEdgeScrollPane() {
        String[] edgeColumnNames = new String[]{"#", "Source", "Target", "Distance", "Speed"};
        Edge[] edges = this.graph.getEdges();
        Object[][] edgeData = new Object[graph.getEdgesSize()][];
        for (int i = 0; i < graph.getEdgesSize(); i++) {
            Edge edge = edges[i];
            if (edge == null) {
                logger.debug("Edge was null at " + i);
            } else {
                edgeData[i] = new Object[]{i, edge.getSourceNode(), edge.getTargetNode(), edge.getDistance(), edge.getSpeed()};
                if (i % 100000 == 0) {
                    logger.debug(i + " edges.");
                }
            }
        }

        JScrollPane scrollPane = getScrollPane(edgeColumnNames, edgeData);
        logger.info("Added all edges.");
        return scrollPane;
    }


    private JScrollPane getScrollPane(String[] columnNames, Object[][] data) {
        JTable table = new JTable(data, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(400, 400));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVisible(true);
        scrollPane.setAutoscrolls(true);
        return scrollPane;
    }


    private void visualizeDataBoundariesOnMap() {
        Node[][] graphBoundaries = this.graph.getGraphBoundaries();
        Node upLeft = graphBoundaries[0][0];
        Node upRight = graphBoundaries[0][1];
        Node lowLeft = graphBoundaries[1][0];
        Node lowRight = graphBoundaries[1][1];
        MapPolygon dataBoundaries = new MapPolygonImpl(c(upLeft), c(upRight), c(lowRight), c(lowLeft));
        map().addMapPolygon(dataBoundaries);
    }


    private JMapViewer map() {
        return treeMap.getViewer();
    }


    /**
     * @param args Main program arguments
     */
    public static void main(String[] args) throws Exception {
        if (args != null && args.length >= 1) {
            OsmMapViewer osmMapViewer = new OsmMapViewer(args[0],
                    StringUtils.containsIgnoreCase(args.length == 2 ? args[1] : null, "optimize=true"));
            osmMapViewer.importOsmData();
            osmMapViewer.setVisible(true);
        } else {
            logger.error("You must provide a path to a file with OSM data.");
            System.exit(1);
        }
    }


    private void updateZoomParameters() {
        if (mperpLabelValue != null)
            mperpLabelValue.setText(String.format("%s", map().getMeterPerPixel()));
        if (zoomValue != null)
            zoomValue.setText(String.format("%s", map().getZoom()));
    }


    private ICoordinate c(double lat, double lon) {
        return new Coordinate(lat, lon);
    }


    private ICoordinate c(Node node) {
        return new Coordinate(node.getLat(), node.getLon());
    }


    @Override
    public void processCommand(JMVCommandEvent command) {
        if (command.getCommand().equals(JMVCommandEvent.COMMAND.ZOOM) ||
                command.getCommand().equals(JMVCommandEvent.COMMAND.MOVE)) {
            updateZoomParameters();
        }
    }
}
