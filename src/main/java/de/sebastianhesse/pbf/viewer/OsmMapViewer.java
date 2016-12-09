package de.sebastianhesse.pbf.viewer;

import de.sebastianhesse.pbf.reader.OptimizedNodeEdgeReader;
import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.JMapViewerTree;
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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    private Graph graph;


    /**
     * Constructs the {@code Viewer}.
     */
    public OsmMapViewer() throws Exception {
        super("JMapViewer Demo");
        treeMap = new JMapViewerTree("Zones");
        setupJFrame();
        setupPanels();

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
                    });
                }
            }
        });
    }

    private void setupJFrame() {
        setSize(400, 400);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }


    private void setupPanels() {
        JPanel panel = new JPanel();
        JPanel panelTop = new JPanel();
        JPanel helpPanel = new JPanel();

        mperpLabelName = new JLabel("Meters/Pixels: ");
        mperpLabelValue = new JLabel(String.format("%s", map().getMeterPerPixel()));
        zoomLabel = new JLabel("Zoom: ");
        zoomValue = new JLabel(String.format("%s", map().getZoom()));

        add(panel, BorderLayout.NORTH);
        add(helpPanel, BorderLayout.SOUTH);
        panel.add(panelTop, BorderLayout.NORTH);
        JLabel helpLabel = new JLabel("Use right mouse button to move,\n "
                + "left double click or mouse wheel to zoom.");
        helpPanel.add(helpLabel);

        panelTop.add(zoomLabel);
        panelTop.add(zoomValue);
        panelTop.add(mperpLabelName);
        panelTop.add(mperpLabelValue);
    }


    public void importOsmData() throws Exception {
        // import graph data
        OptimizedNodeEdgeReader reader = new OptimizedNodeEdgeReader("C:\\Projects\\graphhopper-files\\stuttgart.osm.pbf");
        this.graph = reader.importData().getGraph();
        // show nodes and edges on the left
        addNodeEdgePanel();
        // show borders of data on map
        visualizeDataBoundariesOnMap();
    }


    private void addNodeEdgePanel() {
        logger.info("--- Starting to add nodes and edges to panel. ---");
        JPanel nodeEdgePanel = new JPanel();

        JScrollPane nodeScrollPane = createNodeScrollPane();
        JScrollPane edgeScrollPane = createEdgeScrollPane();

        nodeEdgePanel.add(nodeScrollPane);
        nodeEdgePanel.add(edgeScrollPane);
        nodeEdgePanel.setVisible(true);

        add(nodeEdgePanel, BorderLayout.WEST);
    }


    private JScrollPane createNodeScrollPane() {
        String[] nodeColumnNames = new String[]{"#", "Latitude", "Longitude"};
        Node[] nodes = this.graph.getNodes();
        Object[][] rows = new Object[nodes.length][];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new Object[] {i, nodes[i].getLat(), nodes[i].getLon()};
            if (i % 100000 == 0) {
                logger.debug(i + " nodes.");
            }
        }

        JScrollPane scrollPane = getScrollPane(nodeColumnNames, rows);
        logger.info("Added all nodes.");
        return scrollPane;
    }


    private JScrollPane createEdgeScrollPane() {
        String[] edgeColumnNames = new String[] {"#", "Source", "Target"};
        Edge[] edges = this.graph.getEdges();
        Object[][] edgeData = new Object[edges.length][];
        for (int i = 0; i < edges.length; i++) {
            Edge edge = edges[i];
            if (edge == null) {
                logger.debug("Edge was null at " + i);
            } else {
                edgeData[i] = new Object[]{i, edge.getSourceNode(), edge.getTargetNode()};
                if (i % 100000 == 0) {
                    logger.debug(i + " edges.");
                }
            }
        }

        JScrollPane scrollPane = getScrollPane(edgeColumnNames, edgeData);
        logger.info("Added all edges.");
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


    private JScrollPane getScrollPane(String[] columnNames, Object[][] data) {
        JTable table = new JTable(data, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(400, 420));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setVisible(true);
        scrollPane.setAutoscrolls(true);
        return scrollPane;
    }


    private JMapViewer map() {
        return treeMap.getViewer();
    }


    /**
     * @param args Main program arguments
     */
    public static void main(String[] args) throws Exception {
        OsmMapViewer osmMapViewer = new OsmMapViewer();
        osmMapViewer.importOsmData();
        osmMapViewer.setVisible(true);
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
