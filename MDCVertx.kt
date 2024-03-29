package com.example.demo;

//这个没有意义
import io.netty.channel.*;
import io.netty.util.concurrent.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.impl.VertxImpl;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class DemoApplication  {
    public static ThreadLocal<String> threadLocal = new ThreadLocal<>();
    public static void main(String[] args) throws InterruptedException, NoSuchFieldException, IllegalAccessException {

        VertxOptions options = new VertxOptions()
                .setFileSystemOptions(new FileSystemOptions().setClassPathResolvingEnabled(false));
        Vertx vertx = Vertx.vertx(options);
        EventLoopGroup executors = vertx.nettyEventLoopGroup();
        MDCEventLoopGroup eventExecutors = new MDCEventLoopGroup(executors, r -> {
            String context = threadLocal.get();
            return () -> {
                threadLocal.set(context);
                r.run();
            };
        });
        Field field = VertxImpl.class.getDeclaredField("eventLoopGroup");
        field.setAccessible(true);
        field.set(vertx, eventExecutors);
        vertx.deployVerticle(new TestVerticle());
        Thread.sleep(10_000);

    }

    public static class TestVerticle extends AbstractVerticle {
        @Override
        public void start(io.vertx.core.Promise<Void> startPromise) throws Exception {
            threadLocal.set("1");
            request(2000)
                    .flatMap(v -> {
                        System.out.println("expect 1,real is "+threadLocal.get());
                       return request(1000);
                    })
                    .onComplete(c -> {
                            System.out.println("expect 1,real is "+threadLocal.get());
                    });

            threadLocal.set("2");
            request(1000)
                    .flatMap(v -> {
                        System.out.println("expect 2,real is "+threadLocal.get());
                        return request(2000);
                    })
                    .onComplete(c -> {
                        System.out.println("expect 2,real is "+threadLocal.get());
                    });
        }

        public io.vertx.core.Future<Void> request(long delay){
            io.vertx.core.Promise<Void> promise = io.vertx.core.Promise.promise();
            vertx.setTimer(delay, (l) -> {
                promise.complete();
            });
            return promise.future();
        }
    }
    public static class MDCEventLoopGroup implements EventLoopGroup{
        private EventLoopGroup target;
        private ConcurrentHashMap<EventLoop,EventLoop> hasEnhance;

        private Function<Runnable,Runnable> enhanceFunction;

        public MDCEventLoopGroup(EventLoopGroup target, Function<Runnable, Runnable> enhanceFunction) {
            this.target = target;
            this.enhanceFunction = enhanceFunction;
            hasEnhance = new ConcurrentHashMap<>();
        }

        @Override
        public EventLoop next() {
            EventLoop wait = target.next();
            if (hasEnhance.contains(wait)){
                return hasEnhance.get(wait);
            }
            synchronized (this){
                if (hasEnhance.contains(wait)){
                    return hasEnhance.get(wait);
                }
                MDCEventLoop eventLoop = new MDCEventLoop(wait, enhanceFunction);
                hasEnhance.put(wait,eventLoop);
                return eventLoop;
            }
        }

        @Override
        public ChannelFuture register(Channel channel) {
            return target.register(channel);
        }

        @Override
        public ChannelFuture register(ChannelPromise promise) {
            return target.register(promise);
        }

        @Override
        @Deprecated
        public ChannelFuture register(Channel channel, ChannelPromise promise) {
            return target.register(channel, promise);
        }

        @Override
        public boolean isShuttingDown() {
            return target.isShuttingDown();
        }

        @Override
        public Future<?> shutdownGracefully() {
            return target.shutdownGracefully();
        }

        @Override
        public Future<?> shutdownGracefully(long l, long l1, TimeUnit timeUnit) {
            return target.shutdownGracefully(l, l1, timeUnit);
        }

        @Override
        public Future<?> terminationFuture() {
            return target.terminationFuture();
        }

        @Override
        @Deprecated
        public void shutdown() {
            target.shutdown();
        }

        @Override
        @Deprecated
        public List<Runnable> shutdownNow() {
            return target.shutdownNow();
        }

        @Override
        public Iterator<EventExecutor> iterator() {
            return target.iterator();
        }

        @Override
        public Future<?> submit(Runnable runnable) {
            return target.submit(runnable);
        }

        @Override
        public <T> Future<T> submit(Runnable runnable, T t) {
            return target.submit(runnable, t);
        }

        @Override
        public <T> Future<T> submit(Callable<T> callable) {
            return target.submit(callable);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable runnable, long l, TimeUnit timeUnit) {
            return target.schedule(runnable, l, timeUnit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long l, TimeUnit timeUnit) {
            return target.schedule(callable, l, timeUnit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
            return target.scheduleAtFixedRate(runnable, l, l1, timeUnit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
            return target.scheduleWithFixedDelay(runnable, l, l1, timeUnit);
        }

        @Override
        public boolean isShutdown() {
            return target.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return target.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return target.awaitTermination(timeout, unit);
        }

        @Override
        public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return target.invokeAll(tasks);
        }

        @Override
        public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return target.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return target.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return target.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void close() {
            target.close();
        }

        @Override
        public void execute(Runnable command) {
            target.execute(command);
        }

        @Override
        public void forEach(Consumer<? super EventExecutor> action) {
            target.forEach(action);
        }

        @Override
        public Spliterator<EventExecutor> spliterator() {
            return target.spliterator();
        }
    }

    public static class MDCEventLoop implements EventLoop {
        private EventLoop target;
        private Function<Runnable,Runnable> enhanceFunction;

        public MDCEventLoop(EventLoop target, Function<Runnable, Runnable> enhanceFunction) {
            this.target = target;
            this.enhanceFunction = enhanceFunction;
        }

        @Override
        public EventLoopGroup parent() {
            return target.parent();
        }

        @Override
        public EventLoop next() {
            return this;
        }

        @Override
        public boolean inEventLoop() {
            return target.inEventLoop();
        }

        @Override
        public boolean inEventLoop(Thread thread) {
            return target.inEventLoop(thread);
        }

        @Override
        public <V> Promise<V> newPromise() {
            return target.newPromise();
        }

        @Override
        public <V> ProgressivePromise<V> newProgressivePromise() {
            return target.newProgressivePromise();
        }

        @Override
        public <V> Future<V> newSucceededFuture(V v) {
            return target.newSucceededFuture(v);
        }

        @Override
        public <V> Future<V> newFailedFuture(Throwable throwable) {
            return target.newFailedFuture(throwable);
        }

        @Override
        public boolean isShuttingDown() {
            return target.isShuttingDown();
        }

        @Override
        public Future<?> shutdownGracefully() {
            return target.shutdownGracefully();
        }

        @Override
        public Future<?> shutdownGracefully(long l, long l1, TimeUnit timeUnit) {
            return target.shutdownGracefully(l, l1, timeUnit);
        }

        @Override
        public Future<?> terminationFuture() {
            return target.terminationFuture();
        }

        @Override
        @Deprecated
        public void shutdown() {
            target.shutdown();
        }

        @Override
        @Deprecated
        public List<Runnable> shutdownNow() {
            return target.shutdownNow();
        }

        @Override
        public Iterator<EventExecutor> iterator() {
            return target.iterator();
        }

        @Override
        public Future<?> submit(Runnable runnable) {
            runnable = enhanceFunction.apply(runnable);
            return target.submit(runnable);
        }

        @Override
        public <T> Future<T> submit(Runnable runnable, T t) {
            runnable = enhanceFunction.apply(runnable);
            return target.submit(runnable, t);
        }

        @Override
        public <T> Future<T> submit(Callable<T> callable) {
            return target.submit(callable);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable runnable, long l, TimeUnit timeUnit) {
            runnable = enhanceFunction.apply(runnable);
            return target.schedule(runnable, l, timeUnit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long l, TimeUnit timeUnit) {
            return target.schedule(callable, l, timeUnit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
            runnable = enhanceFunction.apply(runnable);
            return target.scheduleAtFixedRate(runnable, l, l1, timeUnit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
            runnable = enhanceFunction.apply(runnable);
            return target.scheduleWithFixedDelay(runnable, l, l1, timeUnit);
        }

        @Override
        public boolean isShutdown() {
            return target.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return target.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return target.awaitTermination(timeout, unit);
        }

        @Override
        public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return target.invokeAll(tasks);
        }

        @Override
        public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return target.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return target.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return target.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void close() {
            target.close();
        }

        @Override
        public void execute(Runnable command) {
            command = enhanceFunction.apply(command);
            target.execute(command);
        }

        @Override
        public void forEach(Consumer<? super EventExecutor> action) {
            target.forEach(action);
        }

        @Override
        public Spliterator<EventExecutor> spliterator() {
            return target.spliterator();
        }

        @Override
        public ChannelFuture register(Channel channel) {
            return target.register(channel);
        }

        @Override
        public ChannelFuture register(ChannelPromise promise) {
            return target.register(promise);
        }

        @Override
        @Deprecated
        public ChannelFuture register(Channel channel, ChannelPromise promise) {
            return target.register(channel, promise);
        }
    }

}
