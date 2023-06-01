public class ShowCase {
//递归实现
    interface Handle<T, C extends Context<T>> {
        void handle(T last,C context);

    }

    static class Context<T> {

        private List<Handle<T, Context<T>>> currentPipeline;
        private int index = 0;

        public Context(List<Handle<T, Context<T>>> currentPipeline) {
            this.currentPipeline = currentPipeline;
        }

        void invokeNext(T t){
            if (index == currentPipeline.size()) {
                return ;
            }
            Handle<T, Context<T>> handle = currentPipeline.get(index);
            index++;
            handle.handle(t, this);
        }
    }

    static class DefaultPipeline<T, C extends Context<T>> {
        private List<Handle<T,C>> handles;

        private Function<List<Handle<T,C>>, C> contextFactory;

        public DefaultPipeline(List<Handle<T, C>> handles, Function<List<Handle<T,C>>, C> contextFactory) {
            this.handles = handles;
            this.contextFactory = contextFactory;
        }

        public Context<T> start(T init) {
            C c = contextFactory.apply(handles);
            c.invokeNext(init);
            return c;
        }
    }

    public static void main(String[] args) {
        List<Handle<AtomicInteger, Context<AtomicInteger>>> handles = List.of(
                (a,c) -> {
                    a.incrementAndGet();
                    c.invokeNext(a);
                },
                (a,c) -> {
                    a.incrementAndGet();
                    c.invokeNext(a);
                },
                (a,c) -> {
                    if (a.get() != 2) {
                        System.out.println("invoke");
                        a.incrementAndGet();
                        c.invokeNext(a);
                    }
                }
        );

        DefaultPipeline<AtomicInteger, Context<AtomicInteger>> pipeline = new DefaultPipeline<>(handles, Context::new);

        AtomicInteger integer = new AtomicInteger(10);
        Context<AtomicInteger> started = pipeline.start(integer);

        System.out.println("it should be 13,actual:" + integer.get());

        AtomicInteger integer1 = new AtomicInteger(0);
        Context<AtomicInteger> started1 = pipeline.start(integer1);

        System.out.println("it should be 2,actual:" + integer1.get());
    }
}
