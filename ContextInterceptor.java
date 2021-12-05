import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
//拦截异步操作
public class ContextInterceptor implements ThreadContextProvider {

    public static ThreadLocal<Map> threadLocal = new ThreadLocal<>();

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return snapshot(threadLocal.get());
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return snapshot(null);
    }

    @Override
    public String getThreadContextType() {
        return "context";
    }


    private ThreadContextSnapshot snapshot(Map label){
        return ()->{
          var old = threadLocal.get();
          threadLocal.set(label);
          return ()->{
            threadLocal.set(old);
          };
        };
    }
}
