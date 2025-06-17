        ReentrantLock reentrantLock = new ReentrantLock();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        var factory = LoomSupport.setVirtualThreadFactoryScheduler(Thread.ofVirtual(), executorService)
                .factory();
        // warm
        executorService.submit(() -> {}).get();
        new Thread(() -> {
            reentrantLock.lock();
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
            reentrantLock.unlock();
            System.out.println("other unlock");
        }).start();
        Thread.sleep(100);
        factory.newThread(() -> {
            reentrantLock.lock();
            System.out.println("vt lock!");
            reentrantLock.unlock();
        }).start();

        Thread.sleep(100);
        executorService.submit(() -> {
            reentrantLock.lock();
            System.out.println("carrier lock!");
            reentrantLock.unlock();
        });
