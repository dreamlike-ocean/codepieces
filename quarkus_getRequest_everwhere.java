@GET
    @Consumes
    @Path("/getR")
    @Transactional
    public Uni<String> s(){
     
        ResteasyReactiveRequestContext requestContext = CurrentRequestManager.get();
        ServerHttpRequest serverRequest = requestContext.serverRequest();
        VertxResteasyReactiveRequestContext vertxContext = (VertxResteasyReactiveRequestContext) requestContext;
        RoutingContext context = vertxContext.getContext();
        context.put("dddd", "Ddd");
        fun();
        return Uni.createFrom().item( serverRequest.getAllRequestHeaders().toString());
    }

    public void fun(){
        ResteasyReactiveRequestContext requestContext = CurrentRequestManager.get();
        VertxResteasyReactiveRequestContext vertxContext = (VertxResteasyReactiveRequestContext) requestContext;
        RoutingContext context = vertxContext.getContext();
        System.out.println((String) context.get("dddd"));
    }
