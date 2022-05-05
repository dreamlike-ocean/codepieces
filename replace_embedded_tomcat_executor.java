class TomcatCustomer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>{
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
       factory.addConnectorCustomizers(c -> {
           ProtocolHandler handler = c.getProtocolHandler();
           if (handler instanceof AbstractProtocol) {
               ExecutorService single = Executors.newFixedThreadPool(1, r -> new Thread(r, "Dreamlike"));

               ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(100, Integer.MAX_VALUE, 100L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), (r) -> {
                   return Thread.ofVirtual().scheduler(single).unstarted(r);
               });
               poolExecutor.allowCoreThreadTimeOut(true);
               handler.setExecutor(poolExecutor);
           }
       });
    }
}
