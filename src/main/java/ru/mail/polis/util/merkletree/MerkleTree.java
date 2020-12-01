package ru.mail.polis.util.merkletree;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.util.hash.ConcatHash;
import ru.mail.polis.util.hash.TigerHash;

import java.nio.ByteBuffer;
import java.util.List;

public class MerkleTree {
    private final ByteBuffer tree;
    private final ConcatHash hash;
    
    public class Node {
        private final int offset;
        
        public Node(final int offset) {
            this.offset = offset;
        }
        
        public boolean isLeaf() {
            return 2 * offset / leafSize() + 1 >= tree.limit() / leafSize();
        }
        
        /**
         * Calculates hash from current node.
         *
         * @return a {@code byte[]}
         */
        public byte[] hash() {
            if (offset >= tree.limit() || offset < 0 || offset % leafSize() != 0) {
                throw new IllegalStateException("Leaf is outside of tree");
            }
            final var result = new byte[leafSize()];
            System.arraycopy(tree.array(), offset, result, 0, leafSize());
            return result;
        }
        
        public Node parent() {
            return new Node(((offset / leafSize() - 1) / 2) * leafSize());
        }
        
        public Node left() {
            return new Node((offset / leafSize() * 2 + 1) * leafSize());
        }
        
        public Node right() {
            return new Node((offset / leafSize() * 2 + 2) * leafSize());
        }
        
        /**
         * Finds minimum index in subtree.
         *
         * @return a {@code int}
         */
        public int minValueIndex() {
            Node node = this;
            while (!node.isLeaf()) {
                node = node.left();
            }
            
            return node.offset / leafSize() - tree.limit() / leafSize() / 2;
        }
        
        /**
         * Finds maximum index in subtree.
         *
         * @return a {@code int}
         */
        public int maxValueIndex() {
            Node node = this;
            while (!node.isLeaf()) {
                node = node.right();
            }
            
            return node.offset / leafSize() - tree.limit() / leafSize() / 2;
        }
        
        private void calculateHash() {
            if (isLeaf()) {
                return;
            }
            
            final var leftLeaf = left();
            final var rightLeaf = right();
            
            leftLeaf.calculateHash();
            rightLeaf.calculateHash();
            
            final var sumHash = hash.combine(tree.array(), leftLeaf.offset, rightLeaf.offset);
            System.arraycopy(sumHash, 0, tree.array(), offset, leafSize());
        }
    }
    
    private int index(final int i) {
        return i * leafSize();
    }
    
    private int leafSize() {
        return hash.hashSize();
    }
    
    /**
     * Creates a new {@link MerkleTree} with given leaves and hash.
     *
     * @param leaves {@link MerkleTree}'s leaves
     * @param hash   {@link MerkleTree}'s hash
     */
    public MerkleTree(@NotNull final List<byte[]> leaves, @NotNull final ConcatHash hash) {
        this.hash = hash;
        if ((leaves.size() & 1) != 0) {
            leaves.add(new byte[leafSize()]);
        }
        final var startIndex = leaves.size() - 1;
        this.tree = ByteBuffer.allocate((leaves.size() * 2 - 1) * leafSize());
        for (int i = 0; i < leaves.size(); i++) {
            final var leaf = leaves.get(i);
            if (leaf.length != leafSize()) {
                throw new IllegalArgumentException("All hashes must be " + leafSize() + " length");
            }
            System.arraycopy(leaves.get(i), 0, this.tree.array(), index(startIndex + i), leafSize());
        }
        
        root().calculateHash();
    }
    
    /**
     * Wraps {@link MerkleTree}'s byte representation.
     *
     * @param tree {@link MerkleTree}'s byte representation
     * @param hash {@link MerkleTree}'s hash
     */
    public MerkleTree(@NotNull final ByteBuffer tree, @NotNull final ConcatHash hash) {
        this.tree = tree;
        this.hash = hash;
    }
    
    public MerkleTree(@NotNull final ByteBuffer tree) {
        this(tree, new TigerHash());
    }
    
    public ByteBuffer body() {
        return tree.asReadOnlyBuffer();
    }
    
    public Node root() {
        return new Node(0);
    }
}
