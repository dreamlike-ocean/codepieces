public class LoomClassInitDeadLock {


    //需要添加--add-opens java.base/java.lang=ALL-UNNAMED启动
    private static final Executor EXECUTOR = Executors.newFixedThreadPool(1);

    private static final ThreadFactory FACTORY;

    static {
        try {
            var virtual = Thread.ofVirtual();
            Field scheduler = virtual.getClass().getDeclaredField("scheduler");
            scheduler.setAccessible(true);
            scheduler.set(virtual, EXECUTOR);
            FACTORY = virtual.factory();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }

    }

    public static void main(String[] args) throws Throwable {
        Thread thread = FACTORY.newThread(() -> {
            new A();
            System.out.println("end");
        });
        thread.start();
        thread.join();
    }

    private static final Runnable RUNNABLE = () -> {
        System.out.println("start");
        LockSupport.parkNanos(1000000000);
    };

    static class A {

        static {
            System.out.println("start init A");
            CompletableFuture<Void> voidCompletableFuture = new CompletableFuture<>();
            FACTORY.newThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("ddd");
                    voidCompletableFuture.complete(null);
                }
            }).start();
            voidCompletableFuture.join();
            System.out.println("end init A");
        }
    }
}
