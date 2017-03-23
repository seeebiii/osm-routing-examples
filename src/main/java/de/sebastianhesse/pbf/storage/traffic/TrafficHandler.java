package de.sebastianhesse.pbf.storage.traffic;


import de.sebastianhesse.pbf.storage.Edge;
import de.sebastianhesse.pbf.storage.Graph;
import de.sebastianhesse.pbf.storage.Node;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Handling all traffic related stuff: updating the graph with traffic data (i.e. additional weights on edges) and
 * removing the data. Contains a state which data has been changed the last.
 */
public class TrafficHandler {

    private static final Logger logger = LoggerFactory.getLogger(TrafficHandler.class);

    private LocationList locationList;
    private EventList eventList;
    private TmcMessageMap tmcMessageMap;
    private Graph graph;
    private short lastHour = -1;
    private List<Pair<Node, Node>> lastUpdatedWays = new ArrayList<>();


    public TrafficHandler(LocationList locationList, EventList eventList, TmcMessageMap tmcMessageMap) {
        this.locationList = locationList;
        this.eventList = eventList;
        this.tmcMessageMap = tmcMessageMap;
    }


    public void setGraph(Graph graph) {
        this.graph = graph;
    }


    /**
     * Updates the traffic data on the graph for a given hour. Loads the traffic data from the related file for
     * a given hour.
     *
     * @param hour 0 - 23
     * @return a list of node-node pairs indicating which ways have been updated
     * @throws IOException in case an error occurred during reading the traffic data file
     */
    public List<Pair<Node, Node>> updateTraffic(short hour) throws IOException {
        logger.debug("Updating traffic data for hour {}", hour);
        // first, remove existing traffic data!
        removeTrafficData();

        // then read traffic data from file
        List<TmcEvent> events = this.tmcMessageMap.readFile(hour);
        List<Pair<Node, Node>> updatedWays = new ArrayList<>(events.size() * 2);

        // for each event, find the related event type and location.
        events.forEach(event -> {
            logger.debug("Current event: {}", event);
            List<LocationRow> affectedLocations = new ArrayList<>();
            try {
                EventRow eventRow = eventList.get(event.getEventCode());
                if (eventRow.getWeight() > 0) {
                    // just use events which add some additional weight
                    affectedLocations.add(locationList.get(event.getLocationCode()));
                    addAffectedLocationRows(affectedLocations, event.getExtend(), event.isDirection());

                    logger.debug("EventRow: {}. Found {} affected locations.", eventRow, affectedLocations.size());
                    // now search for the location in our graph and update the connected edges accordingly
                    handleAffectedLocations(updatedWays, affectedLocations, eventRow);
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Skipping event {}, because location or event type could not be found.", event);
                logger.debug("Related exception: ", e);
            }
        });

        // save the state of the last change
        this.lastHour = hour;
        this.lastUpdatedWays = updatedWays;

        logger.debug("Updated {} ways for hour {}.", lastUpdatedWays.size(), lastHour);

        return updatedWays;
    }


    private void handleAffectedLocations(final List<Pair<Node, Node>> updatedWays, final List<LocationRow> affectedLocations,
                                         final EventRow eventRow) {
        for (LocationRow row : affectedLocations) {
            logger.debug("Using location row {}.", row);
            if (row.getLat() > 0 && row.getLon() > 0) {
                Optional<Node> closestNode = graph.findClosestNode(row.getLat(), row.getLon());
                closestNode.ifPresent(node -> {
                    List<Edge> neighboursOfNode = graph.getNeighboursOfNode(node);
                    logger.debug("Found node {} with {} neighbours in graph.", node, neighboursOfNode.size());
                    neighboursOfNode.forEach(edge -> {
                        edge.setAdditionalWeight(eventRow.getWeight());
                        updatedWays.add(new ImmutablePair<>(node, graph.getNodes()[edge.getTargetNode()]));
                    });
                });
            }
        }
    }


    private void addAffectedLocationRows(List<LocationRow> rowList, int extend, boolean direction) {
        LocationRow row = rowList.get(0);
        while (extend-- > 0 && row != null) {
            row = getNext(row, direction);
            if (row != null) {
                rowList.add(row);
            }
        }
    }


    private LocationRow getNext(LocationRow row, boolean direction) {
        if (direction) {
            return row.getNegOffset() > 0 ? locationList.get(row.getNegOffset()) : null;
        } else {
            return row.getPosOffset() > 0 ? locationList.get(row.getPosOffset()) : null;
        }
    }


    /**
     * Removes all traffic related data from all edges.
     */
    public void removeTrafficData() {
        this.lastUpdatedWays.forEach(nodePair -> {
            // update both neighbours, because otherwise the edges are not updated appropriately...
            this.graph.getNeighboursOfNode(nodePair.getKey()).forEach(edge -> edge.setAdditionalWeight(0));
            this.graph.getNeighboursOfNode(nodePair.getValue()).forEach(edge -> edge.setAdditionalWeight(0));
        });
        this.lastHour = -1;
        this.lastUpdatedWays.clear();
    }


    public short getLastHour() {
        return this.lastHour;
    }


    public List<Pair<Node, Node>> getLastUpdatedWays() {
        return this.lastUpdatedWays;
    }
}
