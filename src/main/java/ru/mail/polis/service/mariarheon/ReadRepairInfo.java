package ru.mail.polis.service.mariarheon;

import java.util.HashSet;
import java.util.Set;

public class ReadRepairInfo {
    private final Record rightRecord;
    private final Set<String> nodes;

    /**
     * Create data for repairing the nodes.
     *
     * @param rightRecord - record with the newest value, which should be used for repairing.
     */
    public ReadRepairInfo(Record rightRecord) {
        this.rightRecord = rightRecord;
        this.nodes = new HashSet<>();
    }

    /**
     * Add node, which should be repaired.
     *
     * @param node - node.
     */
    public void addNode(String node) {
        nodes.add(node);
    }

    /**
     * Returns nodes, which should be repaired.
     *
     * @return nodes, which should be repaired.
     */
    public Set<String> getNodes() {
        return nodes;
    }

    /**
     * Returns record, which should be used as the new value for repaired nodes.
     *
     * @return record, which should be used as the new value for repaired nodes.
     */
    public Record getRightRecord() {
        return rightRecord;
    }
}
