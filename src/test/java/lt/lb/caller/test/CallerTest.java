package lt.lb.caller.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lt.lb.caller.Caller;
import lt.lb.caller.CallerForBuilder;
import lt.lb.caller.CallerWhileBuilder;
import org.junit.Test;

/**
 *
 * @author laim0nas100
 */
public class CallerTest {

    static Random rng = new Random();

    public static void multiAssert(Object... objs) {
        for (int i = 1; i < objs.length; i++) {
            Object a = objs[i - 1];
            Object b = objs[i];
            if (!Objects.equals(a, b)) {
                throw new AssertionError(a + " != " + b);
            }
        }
    }

    @Test
    public void callerTest() {
        int exp = 20;
        BigInteger big = BigInteger.valueOf(100);
        BigInteger fibb = RecursionBuilder.fibb(BigInteger.valueOf(1), BigInteger.valueOf(1), big.pow(exp));
        BigInteger resolve = RecursionBuilder.fibbCaller(BigInteger.valueOf(1), BigInteger.valueOf(1), big.pow(exp)).resolve();

        multiAssert(fibb, resolve);

        BigInteger m = BigInteger.valueOf(2);
        BigInteger n = BigInteger.valueOf(8);
        multiAssert(
                RecursionBuilder.ackermann(m, n),
                RecursionBuilder.ackermannCaller(m, n).resolve(),
                RecursionBuilder.ackermannCaller(m, n).resolveThreaded()
        );

        for (int i = 0; i < 50; i++) {
            Long num = rng.nextLong() % 1000;
            AtomicLong c1 = new AtomicLong();
            AtomicLong c2 = new AtomicLong();
            AtomicLong c3 = new AtomicLong();
            multiAssert(
                    RecursionBuilder.recrazy(num, c1),
                    RecursionBuilder.recrazyCaller(num, c2).resolve(),
                    RecursionBuilder.recrazyCaller(num, c3).resolveThreaded()
            );

            multiAssert(c1.get(), c2.get(), c3.get());
        }
    }

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

    public static class Interval {

        public final int min, max;
        public final Random rnd;

        public Interval(Random r, int min, int max) {
            this.rnd = r;
            this.min = min;
            this.max = max;
        }

        public int getRandom() {
            return rnd.nextInt(max - min) + min;
        }

        public static Interval of(Random r, int i, int j) {
            return new Interval(r, i, j);
        }

    }

    public static TNode generateTree(int layers, Interval childPerLayer, AtomicInteger count) {
        TNode g = new TNode(count.getAndIncrement());
        if (layers <= 0) {
            return g;
        }
        int childs = childPerLayer.getRandom();
        for (int i = 0; i < childs; i++) {
            g.children.add(generateTree(layers - 1, childPerLayer, count));
        }

        return g;
    }

    public static Caller<TNode> generateTreeCaller(int layers, Interval childPerLayer, AtomicInteger count) {
        TNode g = new TNode(count.getAndIncrement());
        if (layers <= 0) {
            return Caller.ofResult(g);
        }
        int childs = childPerLayer.getRandom();

        CallerWhileBuilder<TNode> builderWhile = Caller.builderWhile();
        AtomicInteger i = new AtomicInteger(0);

        return builderWhile
                .whilst(() -> i.get() < childs)
                .forEachCall(() -> generateTreeCaller(layers - 1, childPerLayer, count))
                .evaluate(child -> {
                    i.incrementAndGet();
                    g.children.add(child);
                    return Caller.flowContinue();
                })
                .afterwards(Caller.ofResult(g))
                .build();

    }

    public Interval getLayers(Random r) {
        return Interval.of(r, 5, 8);
    }

    public Interval getChildren(Random r) {
        return Interval.of(r, 5, 8);
    }

    @Test
    public void graphGenerateTest() {

        int seed = rng.nextInt();
        Random rand = new Random(seed);
        AtomicInteger count1 = new AtomicInteger();

        TNode root1 = generateTree(getLayers(rand).getRandom(), getChildren(rand), count1);

        rand.setSeed(seed);
        AtomicInteger count2 = new AtomicInteger();
        TNode root2 = generateTreeCaller(getLayers(rand).getRandom(), getChildren(rand), count2).resolve();

        multiAssert(count1.get(), count2.get());
        multiAssert(root1, root2);

    }

    @Test
    public void graphSearchTest() {

        AtomicInteger count = new AtomicInteger();
        TNode root = generateTree(getLayers(rng).getRandom(), getChildren(rng), count);

        for (int i = 0; i < 200; i++) {
            Integer id = rng.nextInt(count.get() - 10) + 10;
            if (i == 0) {
                id = -1;
            }
            NodeVisitor it = treeVisitor(root, id);

            multiAssert(
                    DFS(it, root, Optional.empty()),
                    PostOrder(it, root, Optional.empty()),
                    DFSCaller(it, root, Optional.empty()).resolve(),
                    PostOrderCaller(it, root, Optional.empty()).resolve()
            );

        }

    }

    @Test
    public void graphCollectTest() {

        AtomicInteger count = new AtomicInteger();
        TNode root = generateTree(getLayers(rng).getRandom(), getChildren(rng), count);

        for (int i = 0; i < 200; i++) {
            Integer id = rng.nextInt(count.get() - 10) + 10;
            if (i == 0) {
                id = -1;
            }

            List<Integer> dfs_1 = new ArrayList<>();
            List<Integer> dfs_2 = new ArrayList<>();

            List<Integer> post_1 = new ArrayList<>();
            List<Integer> post_2 = new ArrayList<>();

            multiAssert(
                    DFS(treeCollector(root, id, dfs_1), root, Optional.empty()),
                    DFSCaller(treeCollector(root, id, dfs_2), root, Optional.empty()).resolve()
            );
            multiAssert(
                    PostOrder(treeCollector(root, id, post_1), root, Optional.empty()),
                    PostOrderCaller(treeCollector(root, id, post_2), root, Optional.empty()).resolve()
            );
        }
    }

    public static interface NodeVisitor {

        public boolean find(TNode node);

        public default List<TNode> getChildren(TNode node) {
            return node.children;
        }
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

    private static <T> Optional<Caller<Optional<T>>> visitedCheckCaller(T node, Optional<Collection<T>> visited) {
        if (visited.isPresent()) {
            Collection<T> get = visited.get();
            if (get.contains(node)) {
                return Optional.of(Caller.ofResult(Optional.empty())); // prevent looping
            } else {
                get.add(node);
            }
        }
        return Optional.empty();
    }

    private static <T> Optional<T> visitedCheck(T node, Optional<Collection<T>> visited) {
        if (visited.isPresent()) {
            Collection<T> get = visited.get();
            if (get.contains(node)) {
                return Optional.of(node); // prevent looping
            } else {
                get.add(node);
            }
        }
        return Optional.empty();
    }

    public static Optional<TNode> PostOrder(NodeVisitor visitor, TNode root, Optional<Collection<TNode>> visited) {
        Optional<TNode> check = visitedCheck(root, visited);
        if (check.isPresent()) {
            return Optional.empty();//looping
        }

        for (TNode child : visitor.getChildren(root)) {
            Optional<TNode> node = PostOrder(visitor, child, visited);
            if (node.isPresent()) {
                return node;
            }
        }

        return visitor.find(root) ? Optional.ofNullable(root) : Optional.empty();
    }

    public static Caller<Optional<TNode>> PostOrderCaller(NodeVisitor visitor, TNode root, Optional<Collection<TNode>> visited) {
        Optional<Caller<Optional<TNode>>> check = visitedCheckCaller(root, visited);
        if (check.isPresent()) {
            return check.get();// return empty optional wrapped in caller
        }
        return new CallerForBuilder<TNode, Optional<TNode>>()
                .with(visitor.getChildren(root))
                .forEachCall(item -> PostOrderCaller(visitor, item, visited))
                .evaluate(item -> item.isPresent() ? Caller.flowReturn(item) : Caller.flowContinue())
                .afterwards(Caller.ofCallableResult(() -> visitor.find(root) ? Optional.ofNullable(root) : Optional.empty()))
                .build();
    }

    public static Optional<TNode> DFS(NodeVisitor visitor, TNode root, Optional<Collection<TNode>> visited) {
        Optional<TNode> check = visitedCheck(root, visited);
        if (check.isPresent()) {
            return Optional.empty();//looping
        }

        if (visitor.find(root)) {
            return Optional.ofNullable(root);
        } else {
            for (TNode n : visitor.getChildren(root)) {
                Optional<TNode> maybeFound = DFS(visitor, n, visited);
                if (maybeFound.isPresent()) {
                    return maybeFound;
                }
            }
            return Optional.empty();
        }

    }

    public static Caller<Optional<TNode>> DFSCaller(NodeVisitor visitor, TNode root, Optional<Collection<TNode>> visited) {
        Optional<Caller<Optional<TNode>>> check = visitedCheckCaller(root, visited);
        if (check.isPresent()) {
            return check.get(); // return empty optional wrapped in caller
        }

        if (visitor.find(root)) {
            return Caller.ofResult(Optional.ofNullable(root));
        } else {
            return new CallerForBuilder<TNode, Optional<TNode>>()
                    .with(visitor.getChildren(root))
                    .forEachCall((i, item) -> DFSCaller(visitor, item, visited))
                    .evaluate(item -> item.isPresent() ? Caller.flowReturn(item) : Caller.flowContinue())
                    .afterwards(Caller.ofResult(Optional.empty()))
                    .build();

        }

    }

}
