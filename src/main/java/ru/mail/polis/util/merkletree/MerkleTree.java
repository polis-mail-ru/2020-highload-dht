package ru.mail.polis.util.merkletree;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.util.hash.ConcatHash;

import java.util.List;

public class MerkleTree {
    private final byte[] tree;
    private final ConcatHash hash;

    public class Node {
        private final int offset;

        public Node(int offset) {
            this.offset = offset;
        }

        public boolean isLeaf() {
            return 2 * offset + 1 < tree.length;
        }

        public byte[] hash() {
            if (offset >= tree.length) {
                throw new IllegalStateException("Leaf is outside of tree");
            }
            final var result = new byte[leafSize()];
            System.arraycopy(tree, offset, result, 0, leafSize());
            return result;
        }

        public Node left() {
            return new Node(offset * 2 + 1);
        }

        public Node right() {
            return new Node(offset * 2 + 2);
        }

        private void calculateHash() {
            if (isLeaf()) {
                return;
            }

            final var leftLeaf = left();
            final var rightLeaf = right();

            leftLeaf.calculateHash();
            rightLeaf.calculateHash();

            final var sumHash = hash.combine(tree, leftLeaf.offset, rightLeaf.offset);
            System.arraycopy(sumHash, 0, tree, offset, leafSize());
        }
    }

    private int index(final int i) {
        return i * leafSize();
    }

    private int leafSize() {
        return hash.hashSize();
    }

    public MerkleTree(@NotNull final List<byte[]> leaves, @NotNull final ConcatHash hash) {
        this.hash = hash;
        if ((leaves.size() & 1) != 0) {
            leaves.add(new byte[leafSize()]);
        }
        final var startIndex = leaves.size() - 1;
        this.tree = new byte[(leaves.size() * 2 - 1) * leafSize()];
        for (int i = 0; i < leaves.size(); i++) {
            final var leaf = leaves.get(i);
            if (leaf.length != leafSize()) {
                throw new IllegalArgumentException("All hashes must be 64 length");
            }
            System.arraycopy(leaves.get(i), 0, this.tree, index(startIndex + i), leafSize());
        }

        root().calculateHash();
    }

    public Node root() {
        return new Node(0);
    }
}
