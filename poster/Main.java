

class Node<T> {
  T data; Node<T> next;
  T fold(T init, BiFunction<T, T, T> f)
  { /* Implementation ommitted */ }
}


public class Main {
  public static void main(String[] args) {
    Node<Integer> list = new Node<>(1, new Node<>(2, new Node<>(3, null)));
    System.out.println(list.fold(0, (a, b) -> a + b)); // 6
  }
}
