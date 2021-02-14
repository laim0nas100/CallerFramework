package lt.lb.caller.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lt.lb.caller.Caller;
import lt.lb.caller.CallerForBuilder;

/**
 *
 * @author laim0nas100
 */
public class TreeBuilder {

    public static class TNode {

        public int value;
        public List<TNode> children = new ArrayList<>();

        public TNode(int value, TNode... children) {
            this.value = value;
            for (TNode child : children) {
                this.children.add(child);
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.value;
            hash = 37 * hash + Objects.hashCode(this.children);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TNode other = (TNode) obj;
            if (this.value != other.value) {
                return false;
            }
            if (!Objects.equals(this.children, other.children)) {
                return false;
            }
            return true;
        }

    }

    public static interface NodeVisitor {

        public boolean find(TNode node);

    }

    public static NodeVisitor treeVisitor(TNode root, int id) {
        return (TNode node) -> node.value == id;
    }

    public static NodeVisitor treeCollector(TNode root, int id, List<Integer> list) {
        return (TNode node) -> {
            list.add(node.value);
            return node.value == id;
        };
    }

    private static <T> boolean visitedCheck(T node, Collection<T> visited) {
        if (visited != null) {
            if (visited.contains(node)) {
                return true;
            } else {
                visited.add(node);
            }
        }
        return false;
    }

    public static Optional<TNode> PostOrder(NodeVisitor visitor, TNode root, Collection<TNode> visited) {
        if(visitedCheck(root, visited)){
            return Optional.empty();
        }

        for (TNode child : root.children) {
            Optional<TNode> node = PostOrder(visitor, child, visited);
            if (node.isPresent()) {
                return node;
            }
        }

        return visitor.find(root) ? Optional.ofNullable(root) : Optional.empty();
    }

    public static Caller<Optional<TNode>> PostOrderCaller(NodeVisitor visitor, TNode root,Collection<TNode> visited) {
        if(visitedCheck(root, visited)){
            return Caller.ofResult(Optional.empty());
        }
        
        return new CallerForBuilder<TNode, Optional<TNode>>()
                .with(root.children)
                .forEachCall(item -> PostOrderCaller(visitor, item, visited))
                .evaluate(item -> item.isPresent() ? Caller.flowReturn(item) : Caller.flowContinue())
                .afterwards(Caller.ofCallableResult(() -> visitor.find(root) ? Optional.ofNullable(root) : Optional.empty()))
                .build();
    }

    public static Optional<TNode> DFS(NodeVisitor visitor, TNode root, Collection<TNode> visited) {
        if(visitedCheck(root, visited)){
            return Optional.empty();
        }

        if (visitor.find(root)) {
            return Optional.ofNullable(root);
        } else {
            for (TNode n : root.children) {
                Optional<TNode> maybeFound = DFS(visitor, n, visited);
                if (maybeFound.isPresent()) {
                    return maybeFound;
                }
            }
            return Optional.empty();
        }

    }

    public static Caller<Optional<TNode>> DFSCaller(NodeVisitor visitor, TNode root, Collection<TNode> visited) {
        if(visitedCheck(root, visited)){
            return Caller.ofResult(Optional.empty());
        }

        if (visitor.find(root)) {
            return Caller.ofResult(Optional.ofNullable(root));
        } else {
            return new CallerForBuilder<TNode, Optional<TNode>>()
                    .with(root.children)
                    .forEachCall((i, item) -> DFSCaller(visitor, item, visited))
                    .evaluate(item -> item.isPresent() ? Caller.flowReturn(item) : Caller.flowContinue())
                    .afterwards(Caller.ofResult(Optional.empty()))
                    .build();

        }

    }
}
