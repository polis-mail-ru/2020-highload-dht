package ru.mail.polis.service.s3ponia;

import one.nio.http.Response;
import ru.mail.polis.util.merkletree.MerkleTree;

public interface RepairableService {
    Response repair(final long start, final long end);
    
    MerkleTree merkleTree(final long start, final long end);
}
