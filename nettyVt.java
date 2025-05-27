class Main {

  private static final class LoomSupport {
        private static final boolean SUPPORTED;
        private static Throwable FAILURE;
        private static final MethodHandle SCHEDULER_METHODHANDLE;

        static {
            boolean sup;
            MethodHandle scheduler;
            try {
                // this is required to override the default scheduler
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                Field schedulerField = Class.forName("java.lang.ThreadBuilders$VirtualThreadBuilder").getDeclaredField("scheduler");
                schedulerField.setAccessible(true);
                scheduler = lookup.unreflectSetter(schedulerField);

                // this is to make sure we fail earlier!
                Thread.Builder.OfVirtual virualThreadBuilder = Thread.ofVirtual();
                scheduler.invoke(virualThreadBuilder, new Executor() {
                    @Override
                    public void execute(Runnable command) {

                    }
                });
                FAILURE = null;
                sup = true;
            } catch (Throwable e) {
                scheduler = null;
                sup = false;
                FAILURE = e;
            }
            SCHEDULER_METHODHANDLE = scheduler;
            SUPPORTED = sup;
        }

        private LoomSupport() {}

        public static boolean isSupported() {
            return SUPPORTED;
        }

        public static Thread.Builder.OfVirtual setVirtualThreadFactoryScheduler(Thread.Builder.OfVirtual builder, Executor vthreadScheduler) {
            try {
                SCHEDULER_METHODHANDLE.invoke(builder, vthreadScheduler);
                return builder;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class VirtualThreadEventLoopGroup extends AbstractEventExecutorGroup {
        private final EventLoop scheduler;
        private final ThreadFactory vtFactory;

        private VirtualThreadEventLoopGroup(EventLoop scheduler) {
            this.scheduler = scheduler;
            this.vtFactory = LoomSupport.setVirtualThreadFactoryScheduler(Thread.ofVirtual(), scheduler)
                    .factory();
        }

        @Override
        public boolean isShuttingDown() {
            return scheduler.isShuttingDown();
        }

        @Override
        public Future<?> shutdownGracefully() {
            return scheduler.newSucceededFuture(null);
        }

        @Override
        public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
            return scheduler.newSucceededFuture(null);
        }

        @Override
        public Future<?> terminationFuture() {
            return scheduler.terminationFuture();
        }

        @Override
        public void shutdown() {

        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return scheduler.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return scheduler.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return scheduler.awaitTermination(timeout, unit);
        }

        @Override
        public EventExecutor next() {
            return new AbstractEventExecutor() {
                private static final ThreadLocal<EventLoop> CURRENT_SCHEDULER = new ThreadLocal<>();
                @Override
                public void shutdown() {

                }

                @Override
                public boolean inEventLoop() {
                    //todo 如果就是在eventLoop上有没有办法直接进行调用？
                    if (Thread.currentThread().isVirtual()) {
                        EventLoop eventLoop = CURRENT_SCHEDULER.get();
                        return eventLoop == scheduler;
                    }
                    return false;
                }

                @Override
                public boolean inEventLoop(Thread thread) {
                    return false;
                }

                @Override
                public boolean isShuttingDown() {
                    return scheduler.isShuttingDown();
                }

                @Override
                public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
                    return scheduler.newSucceededFuture(null);
                }

                @Override
                public Future<?> terminationFuture() {
                   return scheduler.terminationFuture();
                }

                @Override
                public boolean isShutdown() {
                    return scheduler.isShutdown();
                }

                @Override
                public boolean isTerminated() {
                    return scheduler.isShuttingDown();
                }

                @Override
                public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                    return scheduler.awaitTermination(timeout, unit);
                }

                @Override
                public void execute(Runnable command) {
                    vtFactory.newThread(() -> {
                        try {
                            CURRENT_SCHEDULER.set(scheduler);
                            command.run();
                        } finally {
                            CURRENT_SCHEDULER.remove();
                        }
                    }).start();
                }
            };
        }

        @Override
        public Iterator<EventExecutor> iterator() {
            return Collections.emptyIterator();
        }

    }
}
