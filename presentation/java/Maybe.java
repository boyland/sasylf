public class Maybe<T> {
  private T value;
  private boolean isNothing;

  public Maybe(T value) {
    this.value = value;
    this.isNothing = false;
  }

  public Maybe() {
    this.isNothing = true;
  }

  public boolean isSome() {return !isNothing;}

  public <U> Maybe<U> bind(Function<T, Maybe<U>> f) {
    if (isNothing) return new Maybe<U>();
    else return f.apply(value);
  }
}


