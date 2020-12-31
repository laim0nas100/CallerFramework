package lt.lb.caller.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import lt.lb.caller.Caller;
import lt.lb.caller.CallerBuilder;
import lt.lb.caller.SharedCallerBuilder;

/**
 *
 * @author laim0nas100
 */
public class RecursionBuilder {

    public static BigInteger fibb(BigInteger f1, BigInteger f2, BigInteger limit) {
        Comparator<BigInteger> cmp = Comparator.naturalOrder();
        BigInteger add = f1.add(f2);
        if (cmp.compare(f1, limit) >= 0) {
            return f1;
        } else {
            return fibb(add, f1, limit);
        }
    }

    public static BigInteger fibb2(long seq) {
        if (seq == 0) {
            return BigInteger.ZERO;
        }
        if (seq == 1) {
            return BigInteger.ONE;
        }
        return fibb2(seq - 1).add(fibb2(seq - 2));
    }

    public static Caller<BigInteger> fibb2Caller(long seq) {
        if (seq == 0) {
            return Caller.ofResult(BigInteger.ZERO);
        }
        if (seq == 1) {
            return Caller.ofResult(BigInteger.ONE);
        }

        Caller<BigInteger> toResultCall = new CallerBuilder<BigInteger>(2)
                .withDependencyCall(a -> fibb2Caller(seq - 1))
                .withDependencyCall(a -> fibb2Caller(seq - 2))
                .toResultCall(args -> args._0.add(args._1));

        return toResultCall;

    }

    public static BigInteger ackermann(BigInteger m, BigInteger n) {
        if (m.equals(BigInteger.ZERO)) {
            return n.add(BigInteger.ONE);
        }
        Comparator<BigInteger> cmp = Comparator.naturalOrder();
        if (cmp.compare(m, BigInteger.ZERO) > 0 && Objects.equals(n, BigInteger.ZERO)) {
            return ackermann(m.subtract(BigInteger.ONE), BigInteger.ONE);
        }

        if (cmp.compare(m, BigInteger.ZERO) > 0 && cmp.compare(n, BigInteger.ZERO) > 0) {
            return ackermann(m.subtract(BigInteger.ONE), ackermann(m, n.subtract(BigInteger.ONE)));
        }
        throw new IllegalStateException();

    }

    public static Caller<BigInteger> fibbCaller(BigInteger f1, BigInteger f2, BigInteger limit) {
        Comparator<BigInteger> cmp = Comparator.naturalOrder();
        BigInteger add = f1.add(f2);
        if (cmp.compare(f1, limit) > 0) {
            return Caller.ofResult(f1);
        } else {
            return Caller.ofFunction((args) -> fibbCaller(add, f1, limit));
        }
    }

    public static Caller<BigInteger> ackermannCaller(BigInteger m, BigInteger n) {
        if (m.equals(BigInteger.ZERO)) {
            return Caller.ofResult(n.add(BigInteger.ONE));
        }
        Comparator<BigInteger> cmp = Comparator.naturalOrder();
        if (cmp.compare(m, BigInteger.ZERO) > 0 && Objects.equals(n, BigInteger.ZERO)) {
            return Caller.ofFunction(a -> ackermannCaller(m.subtract(BigInteger.ONE), BigInteger.ONE));
        }

        if (cmp.compare(m, BigInteger.ZERO) > 0 && cmp.compare(n, BigInteger.ZERO) > 0) {

            return new CallerBuilder<BigInteger>()
                    .withDependencyCall(args -> ackermannCaller(m, n.subtract(BigInteger.ONE)))
                    .toCall(args -> ackermannCaller(m.subtract(BigInteger.ONE), args._0));
        }
        throw new IllegalStateException();
    }

    public static Caller<Long> ackermannCaller(long m, long n) {
        if (m == 0) {
            return Caller.ofResult(n + 1);
        }
        Comparator<Long> cmp = Comparator.naturalOrder();
        if (cmp.compare(m, 0L) > 0 && cmp.compare(n, 0L) > 0) {
            return Caller.ofFunction(args -> ackermannCaller(m - 1, 1L));
        }

        if (cmp.compare(m, 0L) > 0 && cmp.compare(n, 0L) > 0) {

            return new CallerBuilder<Long>()
                    .withDependencyCall(args -> ackermannCaller(m, n - 1))
                    .toCall(args -> ackermannCaller(m - 1, args._0));
        }
        throw new IllegalStateException();
    }

    public static long recursiveCounter(long c1, long c2, long c3) {
        if (c1 <= 0) {
            return 0;
        } else if (c2 <= 0) {
            c2 = c1;
            c1--;
        } else if (c3 <= 0) {
            c3 = c2;
            c2--;
        } else {
            c3--;
        }

        return recursiveCounter(
                recursiveCounter(c1, recursiveCounter(c1, c2, recursiveCounter(c1, c2, c3)), recursiveCounter(c1, c2, c3)),
                recursiveCounter(c1, c2, recursiveCounter(c1, c2, c3)),
                recursiveCounter(c1, c2, c3)
        );
    }

    public static Caller<Long> recursiveCounterCaller(long c1, long c2, long c3) {
        if (c1 <= 0) {
            return Caller.ofResult(0L);
        } else if (c2 <= 0) {
            c2 = c1;
            c1--;
        } else if (c3 <= 0) {
            c3 = c2;
            c2--;
        } else {
            c3--;
        }

        final long fc1 = c1;
        final long fc2 = c2;
        final long fc3 = c3;
        Caller<Long> call_1 = Caller.ofFunction(args -> recursiveCounterCaller(fc1, fc2, fc3));
        Caller<Long> call_2 = new CallerBuilder<Long>().with(call_1).toCall(args -> recursiveCounterCaller(fc1, fc2, args.get(0)));
        Caller<Long> call_3 = new CallerBuilder<Long>().with(call_1, call_2).toCall(args -> recursiveCounterCaller(fc1, args.get(0), args.get(1)));
        return new CallerBuilder<Long>().with(call_3, call_2, call_1).toCall(a -> recursiveCounterCaller(a.get(0), a.get(1), a.get(2)));

    }

    public static long recursiveCounter2(long c1, long c2, long c3) {
        if (c1 <= 0) {
            return 0;
        } else if (c2 <= 0) {
            c2 = c1;
            c1--;
        } else if (c3 <= 0) {
            c2--;
        }

        c3 = 0L;
        c2 = recursiveCounter2(c1, c2, c3);
        c1 = recursiveCounter2(c1, c2, c3);
        return recursiveCounter2(c1, c2, c3);
    }

    public static Caller<Long> recursiveCounterCaller3(long c1, long c2, long c3, String st) {
        if (c1 <= 0) {
            return Caller.ofResult(0L);
        } else if (c2 <= 0) {
            c2 = c1;
            c1--;
        } else if (c3 <= 0) {
            c2--;
        }

        final long fc1 = c1;
        final long fc2 = c2;
        final long fc3 = 0L;
        Caller<Long> call_2 = new SharedCallerBuilder<Long>().with(
                Caller.ofResult(fc1),
                Caller.ofResult(fc2),
                Caller.ofResult(fc3)
        )
                .toCall(a -> {
                    return recursiveCounterCaller3(a._0, a._1, a._2, st + ".");
                });
        Caller<Long> call_1 = new SharedCallerBuilder<Long>()
                .with(
                        Caller.ofResult(fc1),
                        call_2,
                        Caller.ofResult(fc3)
                )
                .toCall(a -> {
                    return recursiveCounterCaller3(a._0, a._1, a._2, st + ".");
                });

        return new CallerBuilder<Long>().with(call_1, call_2, Caller.ofResult(fc3))
                .toCall(a -> {
                    return recursiveCounterCaller3(a._0, a._1, a._2, st + ".");
                });

    }

    public static List<Long> list1 = new ArrayList<>();
    public static List<Long> list2 = new ArrayList<>();

    public static long rec2(long numb) {
        if (numb <= 0) {
            return 0;
        }
        list1.add(numb);

        numb--;
        long n1 = rec2(numb);
        long n2 = rec2(numb - n1);
        return rec2(n1 - n2);
    }

    public static Caller<Long> rec2Caller(long numb) {
        if (numb <= 0) {
            return Caller.ofResult(0L);
        }
        list2.add(numb);
        final long n = numb - 1;

        Caller<Long> toCall = new SharedCallerBuilder<Long>().toCall(arg -> rec2Caller(n));

        Caller<Long> toCall1 = new CallerBuilder<Long>().with(toCall).toCall(args -> rec2Caller(n - args.get(0)));

        return new CallerBuilder<Long>().with(toCall, toCall1).toCall(args -> rec2Caller(args.get(0) - args.get(1)));
    }

    public static Long recrazy(long number, AtomicLong counter) {
        counter.incrementAndGet();
        if (number % 4 == 0) {
            return 0L;
        } else {
            if (number > 5000) {
                return number;
            } else {
                long n1 = recrazy(number * 3, counter);
                long n2 = recrazy(n1 + 1, counter);
                long n3 = recrazy(n1 + 2, counter);
                return recrazy(n1 + n2 + n3, counter);
            }
        }
    }
    static CallerBuilder<Long> sharedBuilder = new SharedCallerBuilder<Long>();

    public static Caller<Long> recrazyCaller(long number, AtomicLong counter) {
        counter.incrementAndGet();
        if (number % 4 == 0) {
            return Caller.ofResult(0L);
        } else {
            if (number > 5000) {
                return Caller.ofResult(number);
            } else {
                Caller<Long> n1 = sharedBuilder.toCall(a -> recrazyCaller(number * 3, counter));
                CallerBuilder<Long> builder = new CallerBuilder<Long>().with(n1);
                Caller<Long> n2 = builder.toCall(a -> recrazyCaller(a._0 + 1, counter));
                Caller<Long> n3 = builder.toCall(a -> recrazyCaller(a._0 + 2, counter));
                return new CallerBuilder<Long>()
                        .with(n1, n2, n3)
                        .toCall(a -> recrazyCaller(a._0 + a._1 + a._2, counter));
            }
        }
    }

    public static void safeSleep(long sleepy) {
        long s = (long) (sleepy / 500d);
        try {
            Thread.sleep(s);
        } catch (InterruptedException ex) {

        }
    }
}
