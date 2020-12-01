package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import ru.mail.polis.util.merkletree.MerkleTree;

public interface RepairableService {
    /**
     * Repairs service within given range.
     * @param start range's start
     * @param end range's end
     * @return a {@link Response}
     */
    Response repair(final long start, final long end);
    
    /**
     * Constructs {@link MerkleTree} for given range.
     * @param start range's start
     * @param end range's end
     * @return a {@link MerkleTree}
     */
    MerkleTree merkleTree(final long start, final long end);
}
