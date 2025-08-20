package io.github.dreamlike;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ZGCBenchmark {

    static class Case {

        private static final int BATCH_SIZE = 10_000;  // 每批次分配对象数量
        private static final int OBJECT_SIZE = 1024;    // 每个对象的大小 (1KB)
        private static final double SURVIVAL_RATE = 0.8; // 80% 的对象会存活（部分强化）
        private static final int MAX_SURVIVORS = 10_0000; // 控制 survivor 列表大小

        private static final int loop = 1_00;

        private final Random random = new Random();
        private List<byte[]> survivors = new ArrayList<>();

        public void run(Blackhole blackhole) {
            for (int i = 0; i < loop; i++) {
                run0(blackhole);
            }
        }

        private void run0(Blackhole blackhole) {
            for (int i = 0; i < BATCH_SIZE; i++) {
                byte[] obj = new byte[OBJECT_SIZE]; // 分配新对象
                if (random.nextDouble() < SURVIVAL_RATE) {
                    // 部分对象“强化存活”，放进 survivors
                    survivors.add(obj);
                }
            }
            // 限制 survivors 大小，避免 OOM
            if (survivors.size() > MAX_SURVIVORS) {
                survivors = new ArrayList<>(survivors.subList(survivors.size() / 2, survivors.size()));
            }
            blackhole.consume(survivors);
        }
    }

    @Fork(value = 1, jvm = "/Users/dreamlike/.sdkman/candidates/java/17.0.16-amzn/bin/java", jvmArgs = {"-XX:+UseZGC", "-Xms16g", "-Xmx16g", "-XX:+AlwaysPreTouch"})
    @Benchmark
    public void runOnJDK17(Blackhole blackhole) {
      zgcCase(blackhole);
    }

    // 用 JDK22 的 java 执行
    @Fork(value = 1, jvm = "/Users/dreamlike/.sdkman/candidates/java/24.0.2-amzn/bin/java", jvmArgs = {"-XX:+UseZGC", "-Xms16g", "-Xmx16g", "-XX:+AlwaysPreTouch", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompactObjectHeaders"})
    @Benchmark
    public void runOnJDK24(Blackhole blackhole) {
        zgcCase(blackhole);
    }
    
    private void zgcCase(Blackhole blackhole) {
        new Case().run(blackhole);
    }
}
