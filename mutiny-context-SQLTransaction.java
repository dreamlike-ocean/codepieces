package com.example;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Transaction;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("")
public class ExampleResource {

    private static final String CONNECTION_KEY = "connection";
    private static final String TRANSACTION_KEY = "transaction";
    @Inject
    MySQLPool mySQLPool;

    @Path("/get")
    @GET
    public Uni<String> get(){

        Uni<RowSet<Row>> upstream = mySQLPool.getConnection()
                .withContext((sc, context) -> sc
                        .invoke(sq -> context.put(CONNECTION_KEY, sq))
                        .flatMap(SqlConnection::begin)
                        .invoke(ts -> context.put(TRANSACTION_KEY, ts))
                        .flatMap(ts -> context.<SqlConnection>get(CONNECTION_KEY).query("delete from score").execute())
                        .flatMap(rs -> context.<SqlConnection>get(CONNECTION_KEY).query("select count(*) as count from score").execute())
                );
        //————————————————————————————————上方为上游 执行sql操作（包含事务）————————————————————————————————
        //下游订阅 将连接和事务管理权移交到下游操作
        var query_context = Context.empty();
        upstream.subscribe().with(query_context, rs -> {
            //获取该事务下的查询计数 此时为0
                    System.out.println(rs.iterator().next().getInteger("count"));
                    //提交+归还连接
                    query_context.<Transaction>get(TRANSACTION_KEY).commit()
                            .flatMap(v -> query_context.<SqlConnection>get(CONNECTION_KEY).close())
                            .subscribe().with(UniHelper.NOOP);
                },
                //失败回滚+归还
                t -> {
                    query_context.<Transaction>get(TRANSACTION_KEY).rollback()
                            .flatMap(v -> query_context.<SqlConnection>get(CONNECTION_KEY).close())
                            .subscribe().with(UniHelper.NOOP);
                });


        return Uni.createFrom().item("d");
    }


}

