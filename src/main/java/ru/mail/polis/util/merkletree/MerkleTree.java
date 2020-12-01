package ru.mail.polis.util.merkletree;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.util.hash.ConcatHash;
import ru.mail.polis.util.hash.TigerHash;

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
            return 2 * offset / leafSize() + 1 >= tree.length / leafSize();
        }
        
        /**
         * Calculates hash from current node.
         *
         * @return a {@code byte[]}
         */
        public byte[] hash() {
            if (offset >= tree.length || offset < 0 || offset % leafSize() != 0) {
                throw new IllegalStateException("Leaf is outside of tree");
            }
            final var result = new byte[leafSize()];
            System.arraycopy(tree, offset, result, 0, leafSize());
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
            
            return node.offset / leafSize() - tree.length / leafSize() / 2;
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
            
            return node.offset / leafSize() - tree.length / leafSize() / 2;
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
    
    public int leafSize() {
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
        this.tree = new byte[(leaves.size() * 2 - 1) * leafSize()];
        for (int i = 0; i < leaves.size(); i++) {
            final var leaf = leaves.get(i);
            if (leaf.length != leafSize()) {
                throw new IllegalArgumentException("All hashes must be " + leafSize() + " length");
            }
            System.arraycopy(leaves.get(i), 0, this.tree, index(startIndex + i), leafSize());
        }
        
        root().calculateHash();
    }
    
    /**
     * Wraps {@link MerkleTree}'s byte representation.
     *
     * @param tree {@link MerkleTree}'s byte representation
     * @param hash {@link MerkleTree}'s hash
     */
    public MerkleTree(@NotNull final byte[] tree, @NotNull final ConcatHash hash) {
        this.tree = tree;
        this.hash = hash;
    }
    
    public MerkleTree(@NotNull final byte[] tree) {
        this(tree, new TigerHash());
    }
    
    public byte[] body() {
        return tree;
    }
    
    public Node root() {
        return new Node(0);
    }
}
