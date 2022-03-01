     @Inject
    TransactionSynchronizationRegistry registry;


    @GET
    @Consumes
    @Path("/getR")
    @Transactional
    public Uni<String> s() throws SystemException, NotSupportedException {
        //        <dependency>
        //            <groupId>io.quarkus</groupId>
        //            <artifactId>quarkus-narayana-jta</artifactId>
        //        </dependency>
        registry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                System.out.println("before");
            }

            @Override
            public void afterCompletion(int i) {
                System.out.println(i);
            }
        });
        eventEvent.fire(new CustomerEvent());
       return Uni.createFrom().item("response")
               .invoke((Consumer<String>) System.out::println);
    }



    public void listenTransaction(@Observes(during = TransactionPhase.AFTER_COMPLETION)CustomerEvent customerEvent){
        System.out.println("after transaction");
    }
