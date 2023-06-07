#include <algorithm>
#include <any>
#include <bits/ranges_base.h>
#include <concepts>
#include <iostream>
#include <iterator>
#include <limits>
#include <map>
#include <memory>
#include <mutex>
#include <optional>
#include <ostream>
#include <ranges>
#include <set>
#include <span>
#include <string>
#include <type_traits>
#include <utility>
#include <variant>
#include <vector>

class Range {
public:
  int start;
  int end;

public:
  explicit constexpr Range(int _start, int _end) : start{_start}, end{_end} {
    std::cout << "Range()\n";
  }

  Range(Range &&range) : start{range.start}, end{range.end} {
    std::cout << "Range move()\n";
  }

  auto operator()(int const &x) noexcept { return start <= x && end < x; }

  virtual std::pair<int, int> fetchConfig() {
    return std::make_pair(start, end);
  }

  virtual ~Range() noexcept { std::cout << "drop"; }
};

void print() {}
template <class first, typename... others>
void print(const first &firstArg, const others &...args) {
  std::cout << firstArg;
  print(args...);
}

template <class T, class U> struct IS_SAME {
  static constexpr bool value = false;
};

template <class T> struct IS_SAME<T, T> {
  static constexpr bool value = true;
};

template <typename wait, typename t, typename... all> struct IS_ONE_OF {
  static constexpr bool value =
      IS_ONE_OF<wait, t>::value || IS_ONE_OF<wait, all...>::value;
};

template <typename T, typename U> struct IS_ONE_OF<T, U> : IS_SAME<T, U> {};

#include <functional>

void MyFunc1() { std::cout << "\n\nDelay execute function.\n\n"; }
void MyFunc2(int a, int b) { std::cout << std::endl << a + b << std::endl; }
void MyFunc3(int a, int b, int c, int d) {
  std::cout << a << " " << b << " " << c << " " << d << std::endl;
}

class A {
public:
  int a;
  A(int _a) : a{_a} {}

  auto printA() -> int { return a; }
};

// template <class T, class K, class V>
// auto trans_to_map(std::vector<T> &vector,
//                   std::function<std::pair<K, V>(T &)> &map_fn)
//     -> std::map<K, V> {
//   std::map<K, V> map;
//   std::ranges::for_each(vector | std::views::transform(map_fn),
//                         [&](auto t) { map.insert(t); });
//   return map;
// }

template <class T, class K, class V, class MapFn>
requires requires(T t, MapFn fn, K k, V v) {
  std::invocable<MapFn, T>;
  { fn(t) } -> std::same_as<std::pair<K, V>>;
}
auto trans_to_map(std::vector<T> &vector, MapFn map_fn) -> std::map<K, V> {
  std::map<K, V> map;
  std::ranges::for_each(vector | std::views::transform(map_fn),
                        [&](auto t) { map.insert(t); });
  return map;
}

template<class T>
class Convertor {
  public:
  T t;
  Convertor(T&& _t) {
    t = _t;
  }

  auto convert() -> std::string;
};

template<> 
auto Convertor<int>::convert() -> std::string {
  return std::to_string(t);
}

template<> 
auto Convertor<std::string>::convert() -> std::string {
  return std::move(t);
}


template<class T>
auto uniform_convert_fn(Convertor<T> t) {
  std::cout<< t.convert() <<"\n";
}
int main() {
  test_a();
 
}
