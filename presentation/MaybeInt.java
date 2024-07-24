public class MaybeInt {
  private Integer value;
  private boolean isNothing;

  public MaybeInt(Integer value) {
    this.value = value;
    this.isNothing = false;
  }

  public MaybeInt() {
    this.isNothing = true;
  }

  public boolean isSome() {return !isNothing;}

  public MaybeInt bind(Function<Integer, MaybeInt> f) {
    if (isNothing) return new MaybeInt();
    else return f.apply(value);
  }
}

