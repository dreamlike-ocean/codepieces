package com.example;

import io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext;
import io.smallrye.context.api.CurrentThreadContext;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.RequestImpl;
import org.jboss.resteasy.reactive.server.vertx.VertxResteasyReactiveRequestContext;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.ws.rs.core.Request;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class RequestSessionConfig {
    private ConcurrentHashMap<String,QuarkusSession> sessions = new ConcurrentHashMap<>();

    @RequestScoped
    @Produces
    public QuarkusSession getSession(){
        ResteasyReactiveRequestContext context = CurrentRequestManager.get();
        String header = context.serverRequest().getRequestHeader("token");
        if (header == null){
            String token = UUID.randomUUID().toString();
            context.serverResponse().addResponseHeader("token", token);
            QuarkusSession quarkusSession = new QuarkusSession(token);

            sessions.put(token, quarkusSession);
            return quarkusSession;
        }

        return sessions.compute(header,(k,v) -> {
            if (v != null) return v;
            String token = UUID.randomUUID().toString();
            System.out.println(context.serverResponse()== null);
            context.serverResponse().addResponseHeader("token", token);
            return new QuarkusSession(token);
        });
    }


}
