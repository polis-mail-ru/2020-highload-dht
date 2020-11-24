package ru.mail.polis.service.s3ponia;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.util.merkletree.MerkleTree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class MismatchedNodes {
    private final Queue<MerkleTree.Node> nodeQueue1 = new ArrayDeque<>();
    private final Queue<MerkleTree.Node> nodeQueue2 = new ArrayDeque<>();

    public MismatchedNodes(MerkleTree.Node node1) {
        nodeQueue1.add(node1);
    }

    public static class MismatchedNode {
        private final MerkleTree.Node first;
        private final MerkleTree.Node second;

        public MismatchedNode(MerkleTree.Node first, MerkleTree.Node second) {
            this.first = first;
            this.second = second;
        }


        public MerkleTree.Node second() {
            return second;
        }

        public MerkleTree.Node first() {
            return first;
        }
    }

    public List<MismatchedNode> mismatchedNodes(@NotNull final MerkleTree.Node tree) {
        nodeQueue2.add(tree);
        final var result = new ArrayList<MismatchedNode>();
        while (!nodeQueue2.isEmpty() && !nodeQueue1.isEmpty()) {
            final var node1 = nodeQueue1.poll();
            final var node2 = nodeQueue2.poll();

            final var hash1 = node1.hash();
            assert node2 != null;
            final var hash2 = node2.hash();

            if (Arrays.equals(hash1, hash2)) {
                if (node1.isLeaf() && node2.isLeaf()) {
                    nodeQueue1.add(node1.left());
                    nodeQueue1.add(node1.right());

                    nodeQueue2.add(node2.left());
                    nodeQueue2.add(node2.right());
                }
            } else {
                result.add(new MismatchedNode(node1, node2));
            }
        }

        return result;
    }
}
