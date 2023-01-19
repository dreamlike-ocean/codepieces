
public class main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx(new VertxOptions().setEventLoopPoolSize(4));
        Router router = Router.router(vertx);

        vertx.deployVerticle(new EventBusVerticle());

        vertx.exceptionHandler(Throwable::printStackTrace);

        EventBus bus = vertx.eventBus();
        bus.addOutboundInterceptor(dc -> {
            String o = Vertx.currentContext().getLocal("123");
            dc.message().headers().set("123", o);
            dc.next();
        });
        bus.addInboundInterceptor(dc -> {
            String s = dc.message().headers().get("123");
            Vertx.currentContext().putLocal("123", s);
            dc.next();
        });

        router.get("/request")
                .handler(rc -> {

                    UUID value = UUID.randomUUID();
                    Vertx.currentContext().putLocal("123", value.toString());
                    System.out.println("http:"+value.toString());
                    System.out.println(Thread.currentThread() + ":" + System.identityHashCode(Vertx.currentContext()) + ":http");
                    bus.send("address","message");

                });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(80);
    }


    public static Future<Integer> longTimeOp(Vertx vertx) {
        ContextInternal contextInternal = (ContextInternal) Vertx.currentContext();
        PromiseInternal<Integer> res = contextInternal.promise();
//        Promise<Integer> res = Promise.promise();
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            res.complete(2233);
        }).start();
        return res.future();
    }
}
class EventBusVerticle extends AbstractVerticle{
    @Override
    public void start() throws Exception {
        vertx.eventBus()
                .consumer("address")
                .handler(m -> {
                    System.out.println("consumer recv:"+m.body());
                    System.out.println(Thread.currentThread() + ":" + System.identityHashCode(Vertx.currentContext()) + ":EventBus");
                    System.out.println("eventbus:"+Vertx.currentContext().getLocal("123"));
                });
    }
}
