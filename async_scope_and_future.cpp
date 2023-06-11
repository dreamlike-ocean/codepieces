
#include <algorithm>
#include <chrono>
#include <concepts>
#include <coroutine>
#include <exception>
#include <functional>
#include <iostream>
#include <list>
#include <memory>
#include <mutex>
#include <optional>
#include <thread>
#include <utility>

template <typename T>
// 这实际就是Future
struct Generator {
  // promise就是awaitable的factory
  struct promise_type {
    T next_value;

    // 开始执行时直接挂起等待外部调用 resume 获取下一个值
    // 必须加的函数
    std::suspend_always initial_suspend() {
      std::cout << __func__ << "\n";
      return {};
    };
    /**
      /// [coroutine.trivial.awaitables]
  struct suspend_always
  { 返回当前是否可以恢复
    constexpr bool await_ready() const noexcept { return false; }

    返回类型为void，跳出当前suspend函数。
    ②返回类型为bool，如果返回true，则跳出当前suspend函数，否则直接恢复当前continuation。
    ③返回类型为coroutine_handle，指定恢复哪个continuation。
    constexpr void await_suspend(coroutine_handle<>) const noexcept {}

    类似于Future<T>.await的返回值
  以上说明中包含恢复continuation最后都会调用到这里来返回一个值 constexpr void
  await_resume() const noexcept {}
  };
    */

    // 执行结束后需要挂起 最后来销毁
    // 必须加的函数
    std::suspend_always final_suspend() noexcept { return {}; }

    // 为了简单，我们认为序列生成器当中不会抛出异常，这里不做任何处理
    void unhandled_exception() {}

    // Promise对象构造完成后，通过用户提供的Promise::get_return_object接口来构造Future对象，
    // 也就是协程的返回对象，该对象将在第一次挂起后返回给协程的调用者
    Generator get_return_object() {
      std::cout << __func__ << "\n";
      return Generator(
          std::coroutine_handle<promise_type>::from_promise(*this));
    }
    /**
    首先是检查协程用户定义的Promise对象是否存在成员函数await_transform，如果存在，
    则令Awaitable对象为await_transform(expr)；
    如果不存在，则令Awaitable对象为expr，这一步让协程拥有控制co_await表达式行为的能力，
    。*/
    /**
    首先是检查协程用户定义的Promise对象是否存在成员函数await_transform，如果存在，
    则令Awaitable对象为await_transform(expr)；如果不存在，则令Awaitable对象为expr，

    这一步让协程拥有控制co_await表达式行为的

    */
    std::suspend_always await_transform(T value) {
      std::cout << __func__ << "\n";
      this->next_value = value;
      return {};
    }
    // 没有返回值 这个就是co_return 的返回值
    void return_void() {}
  };
  std::coroutine_handle<promise_type> handle;

  Generator(std::coroutine_handle<promise_type> h) { handle = h; };

  Generator(Generator &&rhs) : handle(std::exchange(rhs.handle, {})) {}
  ~Generator() {
    // 销毁协程
    if (handle) {
      handle.destroy();
    }
  }
  int next() {
    handle.resume();
    return handle.promise().next_value;
  }
};

Generator<int> sequence() {
  int i = 0;
  while (true) {
    co_await i++;
  }
}

template <typename T> struct Result {
  // 初始化为默认值
  explicit Result() = default;

  // 当 Task 正常返回时用结果初始化 Result
  explicit Result(T &&value) : _value(value) {}

  // 当 Task 抛异常时用异常初始化 Result
  explicit Result(std::exception_ptr &&exception_ptr)
      : _exception_ptr(exception_ptr) {}

  // 读取结果，有异常则抛出异常
  T get_or_throw() {
    if (_exception_ptr) {
      std::rethrow_exception(_exception_ptr);
    }
    return _value;
  }

private:
  T _value{};
  std::exception_ptr _exception_ptr;
};

template <typename T> struct AsyncResult {
  explicit AsyncResult() = default;

  // 当 Task 正常返回时用结果初始化 Result
  explicit AsyncResult(T &&value)
      : res(value),
        completion_callbacks(std::move(value.completion_callbacks)) {}

  // 这里先暂时一把大锁控制一下 好理解
  void complete(Result<T> &&async_value) {
    std::cout << __func__ << "AsyncResult 准备触发回调 \n";
    auto scope = std::lock_guard(queue_lock);
    done = true;
    res = async_value;
    for (auto fn : completion_callbacks) {
      fn(res);
    }
    completion_callbacks.clear();
  }

  void on_completed(std::function<void(Result<T>)> &&func) {
    if (done) {
      // result 已经有值
      auto value = res;
      // 解锁之后再调用 func
      func(value);
    } else {
      // 否则添加回调函数，等待调用
      completion_callbacks.push_back(func);
    }
  }

  bool is_done() { return done; }

  Result<T> get_result() { return res; }

private:
  std::mutex queue_lock;
  std::list<std::function<void(Result<T>)>> completion_callbacks;
  Result<T> res;
  bool done = false;
};

template <typename T> struct AsyncResultAwaiter {

  AsyncResultAwaiter(AsyncResultAwaiter &) = delete;
  AsyncResultAwaiter &operator=(AsyncResultAwaiter &) = delete;

  AsyncResultAwaiter(AsyncResultAwaiter &&completion) noexcept
      : res(std::exchange(completion.res, {})),
        handle(std::move(completion.handle)) {}

  AsyncResultAwaiter(std::shared_ptr<AsyncResult<T>> _res,
                     std::coroutine_handle<> &&handle) noexcept
      : res(_res), handle(handle) {}

  bool await_ready() const noexcept { return res->is_done(); }

  void await_suspend(std::coroutine_handle<> handle) noexcept {
    // 当 task 执行完之后调用 resume

    res->on_completed([handle](auto a) {
      std::cout << __func__ << "AsyncResultAwaiter  回调 \n";
      std::cout << handle.address();
      handle.resume();
    });
  }

  Result<T> await_resume() noexcept {
    std::cout << "await_resume"
              << " " << res.get() << "\n";
    return res.get()->get_result();
  }

  std::shared_ptr<AsyncResult<T>> res;
  std::coroutine_handle<> handle;
};

template <typename ResultType> struct Task {

  template <typename R> struct TaskAwaiter {
    // 这个task对应的是co_await的expr 即子任务
    explicit TaskAwaiter(Task<R> &&task) noexcept : task(std::move(task)) {}

    TaskAwaiter(TaskAwaiter &&completion) noexcept
        : task(std::exchange(completion.task, {})) {}

    // 防止无意间的拷贝构造 这里跟随Rust哲学直接用move语义
    TaskAwaiter(TaskAwaiter &) = delete;

    TaskAwaiter &operator=(TaskAwaiter &) = delete;

    bool await_ready() const noexcept {
      return task.handle.promise().done;
    }

    void await_suspend(std::coroutine_handle<> handle) noexcept {
      // 当 task 执行完之后调用 resume
      task.finally([handle]() { handle.resume(); });
    }

    // 协程恢复执行时，被等待的 Task 已经执行完，调用 get_result 来获取结果
    R await_resume() noexcept { return task.get_result(); }

  private:
    Task<R> task;
  };

  struct promise_type {
    auto initial_suspend() { return std::suspend_never{}; }
    auto final_suspend() noexcept { return std::suspend_always{}; }
    auto unhandled_exception() {
      done = true;
      res = Result<ResultType>(std::current_exception());
      for (auto &callback : completion_callbacks) {
        callback(res);
      }
    }
    // 定制co_return行为 当调用这个方法时意味着此时
    void return_value(ResultType value) {
      done = true;
      res = Result<ResultType>(std::move(value));
      for (auto &callback : completion_callbacks) {
        callback(res);
      }
    }

    Task<ResultType> get_return_object() {
      return Task{std::coroutine_handle<promise_type>::from_promise(*this)};
    }

    template <typename _ResultType>
    TaskAwaiter<_ResultType> await_transform(Task<_ResultType> &&task) {
      return TaskAwaiter<_ResultType>(std::move(task));
    }

    template <typename _ResultType>
    AsyncResultAwaiter<_ResultType>
    await_transform(std::shared_ptr<AsyncResult<_ResultType>> future) {
      return AsyncResultAwaiter(
          future, std::coroutine_handle<promise_type>::from_promise(*this));
    }

    void on_completed(std::function<void(Result<ResultType>)> &&func) {
      if (done) {
        // result 已经有值
        auto value = res;
        // 解锁之后再调用 func
        func(value);
      } else {
        // 否则添加回调函数，等待调用
        completion_callbacks.push_back(func);
      }
    }

    auto get_result() { return res.get_or_throw(); }

  public:
    Result<ResultType> res;
    bool done = false;
    std::list<std::function<void(Result<ResultType>)>> completion_callbacks;
  };

public:
  std::coroutine_handle<promise_type> handle;

  ResultType get_result() { return handle.promise().get_result(); }

  explicit Task(std::coroutine_handle<promise_type> handle) noexcept
      : handle(handle) {}
  Task(Task &&task) noexcept : handle(std::exchange(task.handle, {})) {}

  Task(Task &) = delete;

  Task &operator=(Task &) = delete;

  ~Task() {
    if (handle)
      handle.destroy();
  }

  Task &finally(std::function<void()> &&func) {
    handle.promise().on_completed([func](auto result) { func(); });
    return *this;
  }
};

Task<int> simple_task2() {
  std::cout << __func__ << "\n";
  co_return 2;
}
Task<int> simple_task1() {
  std::cout << __func__ << "\n";
  co_return 1;
}

Task<int> simple_task() {
  // result2 == 2

  auto result2 = co_await simple_task1();
  std::cout << __func__ << " co_await task1 \n";

  // result3 == 3
  auto result3 = co_await simple_task2();
  std::cout << __func__ << " co_await task2 \n";

  co_return 1 + result2 + result3;
}

Task<int> async_task() {
  // result2 == 2
  auto shared_ptr = std::make_shared<AsyncResult<int>>();
  auto thread = std::thread([shared_ptr]() {
    std::cout << "thread :in lambda \n";
    std::this_thread::sleep_for(std::chrono::seconds(1));
    shared_ptr->complete(Result(12));
  });
  thread.detach();
  std::cout << "before await count:" << shared_ptr.use_count() << "\n";
  auto res = co_await shared_ptr;
  std::cout << "res:" << res.get_or_throw() << "\n";
  co_return res.get_or_throw() + 3;
}

auto main(int argc, const char **argv) -> int {
  auto task = async_task();
  // task.finally([]{
  //   std::cout<<"end\n";
  // });
  std::this_thread::sleep_for(std::chrono::seconds(100));
}
