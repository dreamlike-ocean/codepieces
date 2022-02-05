import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
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
      //调用链上的context
        var query_context = Context.empty();
        mySQLPool.getConnection()
                .withContext((sc,context) -> sc
                            //context塞sqlconnection
                        .invoke(sq ->  context.put(CONNECTION_KEY,sq))
                        .flatMap(SqlConnection::begin)
                            //塞transaction
                        .invoke(ts -> context.put(TRANSACTION_KEY, ts)))
          
                .flatMap(ts -> query_context.<SqlConnection>get(CONNECTION_KEY).query("delete from score").execute())
                .flatMap(rs -> query_context.<SqlConnection>get(CONNECTION_KEY).query("select count(*) as count from score").execute())
          //查看在当前事务下统计条数  是0 因为全delete了
                .invoke(rs -> System.out.println(rs.iterator().next().getInteger("count")))
          //回滚
                .call(rs -> query_context.<Transaction>get(TRANSACTION_KEY).rollback())
          //回滚后返回连接到池中
                .call(rs -> query_context.<SqlConnection>get(CONNECTION_KEY).close())
          //—————————————————————————————————以上为惰性上游操作————————————————————————————————————————————————————————————————————
          //订阅操作 传入自定义context 实际上不传参也行。。。默认给一个Context.empty()
          // 这边是下游操作，元素是select语句产生的rowset作为元素
                .subscribe().with(query_context, rs -> System.out.println("end"), Throwable::printStackTrace);
        return Uni.createFrom().item("d");
    }


}
