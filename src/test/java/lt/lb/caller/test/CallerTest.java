package lt.lb.caller.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lt.lb.caller.Caller;
import lt.lb.caller.CallerWhileBuilder;
import static lt.lb.caller.test.TreeBuilder.DFS;
import static lt.lb.caller.test.TreeBuilder.DFSCaller;
import lt.lb.caller.test.TreeBuilder.NodeVisitor;
import static lt.lb.caller.test.TreeBuilder.PostOrder;
import static lt.lb.caller.test.TreeBuilder.PostOrderCaller;
import lt.lb.caller.test.TreeBuilder.TNode;
import static lt.lb.caller.test.TreeBuilder.treeCollector;
import static lt.lb.caller.test.TreeBuilder.treeVisitor;
import org.junit.Test;

/**
 *
 * @author laim0nas100
 */
public class CallerTest {

    static Random rng = new Random();

    public static void multiAssert(Object... objs) {
        for (int i = 1; i < objs.length; i++) {
            int j = i - 1;
            Object a = objs[j];
            Object b = objs[i];
            if (!Objects.deepEquals(a, b)) {
                throw new AssertionError(a + " != " + b + " in objects:" + Arrays.asList(objs) + " at index:" + j + "," + i);
            }
        }
    }

    @Test
    public void fibbTest() {
        int exp = 20;
        BigInteger big = BigInteger.valueOf(100);
        BigInteger fibb = RecursionBuilder.fibb(BigInteger.valueOf(1), BigInteger.valueOf(1), big.pow(exp));
        BigInteger resolve = RecursionBuilder.fibbCaller(BigInteger.valueOf(1), BigInteger.valueOf(1), big.pow(exp)).resolve();

        multiAssert(fibb, resolve);
    }

    @Test
    public void ackermannTest() {
        BigInteger m = BigInteger.valueOf(2);
        BigInteger n = BigInteger.valueOf(8);
        multiAssert(
                RecursionBuilder.ackermann(m, n),
                RecursionBuilder.ackermannCaller(m, n).resolve(),
                RecursionBuilder.ackermannCaller(m, n).resolveThreaded()
        );
    }

    @Test
    public void binarySearchTest() {
        int bound = Interval.of(rng, 10000, 20000).getRandom();
        Integer[] data = new Integer[bound];
        for (int i = 0; i < bound; i++) {
            data[i] = rng.nextInt();
        }

        int toFind = data[rng.nextInt(bound)];

        Arrays.sort(data);

        multiAssert(
                RecursionBuilder.binarySearch(data, toFind, 0, bound),
                RecursionBuilder.binarySearchCaller(data, toFind, 0, bound).resolve()
        );
    }

    @Test
    public void mergeSortTest() {
        int bound = Interval.of(rng, 10000, 20000).getRandom();
        Integer[] data = new Integer[bound];
        for (int i = 0; i < bound; i++) {
            data[i] = rng.nextInt();
        }
        Integer[] base = Arrays.copyOf(data, data.length);

        Integer[] copy1 = Arrays.copyOf(data, data.length);
        Integer[] copy2 = Arrays.copyOf(data, data.length);
        Integer[] copy3 = Arrays.copyOf(data, data.length);

        Arrays.sort(base);
        MergeSort.sort(copy1, 0, bound - 1);
        MergeSort.sortCaller(copy2, 0, bound - 1).resolve();
        MergeSort.sortCaller(copy3, 0, bound - 1).resolveThreaded();

        multiAssert(base, copy1, copy2, copy3);
    }

    @Test
    public void factorialTest() {
        int num = rng.nextInt(10) + 10;
        multiAssert(
                RecursionBuilder.factorial(num),
                RecursionBuilder.factorialCaller(num).resolve()
        );
    }

    @Test
    public void crazyRecursionTest1() {

        for (int i = 0; i < 50; i++) {
            Long num = rng.nextLong() % 500;
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

    @Test
    public void crazierRecursionTest2() {

        long a = 2;
        long b = 1;
        long c = 1;
        AtomicLong c1 = new AtomicLong();
        AtomicLong c2 = new AtomicLong();
        AtomicLong c3 = new AtomicLong();
        multiAssert(
                RecursionBuilder.recursiveCounter(c1, a, b, c),
                RecursionBuilder.recursiveCounterCaller(c2, a, b, c).resolve(),
                RecursionBuilder.recursiveCounterCaller(c3, a, b, c).resolveThreaded()
        );

        multiAssert(c1.get(), c2.get(), c3.get());

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
        return Interval.of(r, 4, 6);
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
                    DFS(it, root, null),
                    PostOrder(it, root, null),
                    DFSCaller(it, root, null).resolve(),
                    PostOrderCaller(it, root, null).resolve()
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
                    DFS(treeCollector(root, id, dfs_1), root, null),
                    DFSCaller(treeCollector(root, id, dfs_2), root, null).resolve()
            );
            multiAssert(
                    PostOrder(treeCollector(root, id, post_1), root, null),
                    PostOrderCaller(treeCollector(root, id, post_2), root, null).resolve()
            );
        }
    }

}
