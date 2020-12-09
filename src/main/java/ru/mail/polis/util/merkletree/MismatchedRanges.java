package ru.mail.polis.util.merkletree;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class MismatchedRanges {
    private final MerkleTree tree;
    private final long rangesCount;
    private final long start;
    private final long end;
    
    public class Range {
        private final long rangeStart;
        private final long rangeEnd;
        
        private Range(final long rangeStart, final long rangeEnd) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd < 0 ? Long.MAX_VALUE : rangeEnd;
        }
    
        /**
         * Creates a new {@link Range} that fits all values in {@link MerkleTree.Node}'s subtree.
         * @param node {@link MerkleTree.Node}
         */
        public Range(@NotNull final MerkleTree.Node node) {
            this(
                    node.minValueIndex() / rangesCount * (end - start) + start,
                    (node.maxValueIndex() + 1) / rangesCount * (end - start) + start
            );
        }
        
        public long start() {
            return rangeStart;
        }
        
        public long end() {
            return rangeEnd;
        }
    }
    
    /**
     * Constructs a new {@link MismatchedRanges} with given tree and range.
     * @param tree merkle tree for comparing
     * @param start range's start
     * @param end range's end
     */
    public MismatchedRanges(@NotNull final MerkleTree tree, final long start, final long end) {
        this.tree = tree;
        this.rangesCount = tree.root().maxValueIndex();
        assert end >= start;
        this.start = start;
        this.end = end;
    }
    
    private List<Range> compact(@NotNull final List<Range> misMatches) {
        if (misMatches.isEmpty()) {
            return misMatches;
        }
        
        final List<Range> result = new ArrayList<>();
        result.add(misMatches.get(0));
        for (int i = 1; i < misMatches.size(); i++) {
            if (misMatches.get(i).start() == result.get(result.size() - 1).end()) {
                final var previousRange = result.get(result.size() - 1);
                result.remove(result.size() - 1);
                result.add(new Range(previousRange.start(), misMatches.get(i).end()));
            } else {
                result.add(misMatches.get(i));
            }
        }
        
        return result;
    }
    
    /**
     * Compares with given {@link MerkleTree} and constructs a {@link List} of mismatched {@link Range}s.
     * @param tree {@link MerkleTree}
     * @return a {@code List<Range>}
     */
    public List<Range> mismatchedNodes(@NotNull final MerkleTree tree) {
        final Queue<MerkleTree.Node> nodeQueue1 = new ArrayDeque<>();
        final Queue<MerkleTree.Node> nodeQueue2 = new ArrayDeque<>();
        nodeQueue1.add(this.tree.root());
        nodeQueue2.add(tree.root());
        final var result = new ArrayList<Range>();
        assert nodeQueue1.peek() != null;
        while (!nodeQueue2.isEmpty() && !nodeQueue1.isEmpty()) {
            final var node1 = nodeQueue1.poll();
            final var node2 = nodeQueue2.poll();
            
            final var hash1 = node1.hash();
            assert node2 != null;
            final var hash2 = node2.hash();
            
            final boolean isEqual = Arrays.equals(hash1, hash2);
    
            if (isEqual) {
                if (!node1.isLeaf() && !node2.isLeaf()) {
                    nodeQueue1.add(node1.left());
                    nodeQueue1.add(node1.right());
                    
                    nodeQueue2.add(node2.left());
                    nodeQueue2.add(node2.right());
                }
            } else {
                result.add(new Range(node1));
            }
        }
        
        return compact(result);
    }
}
