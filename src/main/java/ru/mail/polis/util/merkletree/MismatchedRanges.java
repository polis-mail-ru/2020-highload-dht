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
    
    public class Range {
        private final long start;
        private final long end;
        
        private Range(long start, long end) {
            this.start = start;
            this.end = end < 0 ? Long.MAX_VALUE : end;
        }
        
        public Range(@NotNull final MerkleTree.Node node) {
            this(
                    node.minValueIndex() * (Long.MAX_VALUE / rangesCount),
                    (node.maxValueIndex() + 1) * (Long.MAX_VALUE / rangesCount)
            );
        }
        
        public long start() {
            return start;
        }
        
        public long end() {
            return end;
        }
    }
    
    public MismatchedRanges(@NotNull final MerkleTree tree) {
        this.tree = tree;
        this.rangesCount = tree.root().maxValueIndex();
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
            
            if (!isEqual) {
                result.add(new Range(node1));
            } else if (!node1.isLeaf() && !node2.isLeaf()) {
                nodeQueue1.add(node1.left());
                nodeQueue1.add(node1.right());
                
                nodeQueue2.add(node2.left());
                nodeQueue2.add(node2.right());
            }
        }
        
        return compact(compact(result));
    }
}
