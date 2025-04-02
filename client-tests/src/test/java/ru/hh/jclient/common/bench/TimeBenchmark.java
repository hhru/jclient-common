package ru.hh.jclient.common.bench;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
public class TimeBenchmark {

  public static void main(String[] args) throws RunnerException {
    var opt = new OptionsBuilder()
        .include(TimeBenchmark.class.getSimpleName())
        .forks(1)
        .jvmArgsAppend("-DrootLoggingLevel=WARN")
        .build();
    new Runner(opt).run();
  }

  @Benchmark
  public void nano(Blackhole blackhole) {
    blackhole.consume(System.nanoTime());
  }

  @Benchmark
  public void ms(Blackhole blackhole) {
    blackhole.consume(System.currentTimeMillis());
  }
}
