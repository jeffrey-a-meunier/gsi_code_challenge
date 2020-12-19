package cc2_fibonacci;

/**
 * This calculates the first 25 Fibonacci numbers Fib(0) to Fib(24).
 *
 * The Fibonacci function is defined over non-negative integers like this:
 *   Fib(0) = 0
 *   Fib(1) = 1
 *   Fib(n>1) = Fib(n-1) + Fib(n-2)
 *
 * This implementation is notable because it calculates the Fibonacci series
 * without using (1) actual numbers, (2) built-in arithmetic operators, or
 * (3) built-in looping constructs: numbers are linked lists, and iteration
 * is through recursion.
 *
 * The only primitives defined in the Number class are:
 *  - the zero value
 *  - successor: analogous to n + 1
 *  - predecessor: analogous to n - 1
 *  - isZero predicate
 *
 * All higher-order methods (add, sub, isEqual) are defined in terms of
 * successor, predecessor, and isZero. Additional methods are defined
 * using only (1) method calls and (2) 'if' statements.
 * 
 * Theoretically even the predecessor method can be written in terms of
 * the successor method, but that would require a primitive isEqual method.
 * I had to choose between implementing a primitive predecessor method or
 * a primitive isEqual method, so I chose predecessor.
 *
 * To run:
 *   $ mkdir cc2_fibonacci
 *   $ (copy Fibonacci.java into cc2_fibonacci)
 *   $ javac cc2_fibonacci/Fibonacci.java
 *   $ java -enableassertions -Xss2m cc2_fibonacci.Fibonacci
 *
 * In Eclipse add '-enableassertions -Xss2m' to VM arguments in Run Configuration.
 *
 * The -Xss2m ensures that the stack is large enough to support some deeply
 * recursive method calls.
 *
 */
public class Fibonacci {

  /**
   * A Number is a linked list, but here we care only about the 'next'
   * links, as each list cell carries no payload.
   *
   * [] = 0
   * []->[] = 1
   * []->[]->[] = 2
   * etc.
   *
   */
  static class Number {
    
    // The number of _pred links that a number has indicates its numeric
    // value. A null here indicates that the number is 0.
    private Number _pred;

    // Every new number starts at 0.
    public Number() { this(null); }

    // Creates a successor to a number by attaching a predecessor to it.
    public Number(Number pred) { _pred = pred; }

    // Gets a number's predecessor.
    public Number pred() { return _pred; }

    // A number is zero if it has no predecessor.
    public boolean isZero() { return _pred == null; }
  }

  //-----------------------------------------------------------------
  // Arithmetic and comparison methods
  //-----------------------------------------------------------------

  static Number add(Number num1, Number num2) {
    if (num2.isZero()) return num1;
    return add(new Number(num1), num2.pred());
  }

  // This method is not needed to calculate the Fibonacci series.
  static Number sub(Number num1, Number num2) {
    if (num2.isZero()) return num1;
    if (num1.isZero())
      throw new RuntimeException("Negative numbers not supported");
    return sub(num1.pred(), num2.pred());
  }

  static boolean isEqual(Number num1, Number num2) {
    if (num1.isZero()) return num2.isZero();
    if (num2.isZero()) return false;
    return isEqual(num1.pred(), num2.pred());
  }

  @Deprecated  // use Number.pred() instead
  static Number pred(Number num1) {
    Number num0 = new Number();
    return pred_aux(num0, num1);
  }

  // Calculates the predecessor of a number by counting upward from 0
  // until you reach that number: the number before that number is
  // the predecessor.
  @Deprecated  // This creates an unbounded recursion with the isEqual method.
  static Number pred_aux(Number num0, Number num1) {
    Number num0_succ = new Number(num0);
    // If successor(num0) == num1, then predecessor(num1) == num0
    if (isEqual(num0_succ, num1)) return num0;
    return pred_aux(num0_succ, num1);
  }

  //-----------------------------------------------------------------
  // Fibonacci
  //-----------------------------------------------------------------
  static Number fib(Number num) {
    if (num.isZero()) return num;
    if (num.pred().isZero()) return num;
    return add(fib(num.pred()), fib(num.pred().pred()));
  }

  //-----------------------------------------------------------------
  // Conversion methods between Number and int
  // Here and in the test methods are the only places where built-in
  // numbers and arithmetic operators are used.
  //-----------------------------------------------------------------

  static Number intToNum(int n) {
    if (n == 0) return new Number();
    return new Number(intToNum(n - 1));
  }

  static int numToInt(Number num) {
    if (num.isZero()) return 0;
    return numToInt(num.pred()) + 1;
  }

  //-----------------------------------------------------------------
  // Main method
  //-----------------------------------------------------------------
  public static void main(String[] args) {
    testNumbers();
    testFibRef();
    testFib(24);
  }

  //-----------------------------------------------------------------
  // Test methods
  //-----------------------------------------------------------------

  static void testNumbers() {
    int N = 50;
    Number nums[] = new Number[N+1];  // include N

    System.out.println("Creating Number instances 0 to " + N);
    for (int n=0; n<=N; n++)
      nums[n] = intToNum(n);

    System.out.println("Testing numToInt");
    // This also ensures symmetry with intToNum.
    for (int n=0; n<=N; n++) {
      assert n == numToInt(nums[n]);
    }

    System.out.println("Testing isZero");
    for (int n=0; n<=N; n++) {
      if (n == 0)
        assert nums[n].isZero();
      else
        assert !nums[n].isZero();
    }

    System.out.println("Testing isEqual");
    for (int n=0; n<=N; n++) {
      assert isEqual(nums[n], nums[n]);
      assert !isEqual(nums[n], nums[(n+N/2)%N]);
    }

    System.out.println("Testing successor");
    // The Number(num) constructor is the successor method.
    for (int n=0; n<N; n++) {
      assert isEqual(nums[n+1], new Number(nums[n]));
    }

    System.out.println("Testing predecessor");
    for (int n=0; n<N; n++) {
      assert isEqual(nums[n], nums[n+1].pred());
    }

    System.out.println("Testing add");
    for (int a=0; a<=N; a+=25) {
      for (int b=0; b<=N; b++) {
        assert (b + a) == numToInt(add(nums[b], nums[a]));
      }
    }

    System.out.println("Testing sub");
    for (int a=0; a<=N; a+=25) {
      for (int b=a; b<=N; b+=25) {
        assert (b - a) == numToInt(sub(nums[b], nums[a]));
      }
    }

    System.out.println("Number test finished");
  }

  // This is the reference version of the Fibonacci function
  static int fib_ref(int n) {
    if (n == 0) return 0;
    if (n == 1) return 1;
    return fib_ref(n - 1) + fib_ref(n - 2);
  }

  static void testFibRef() {
    int fibValues[] = {0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377};
    for (int n=0; n<fibValues.length; n++) {
      assert fibValues[n] == fib_ref(n);
    }
    assert 165580141 == fib_ref(41);
    // assume the rest are correct
    System.out.println("Fibonacci reference method test finished");
  }

  static void testFib(int max) {
    System.out.println("Testing Fibonacci method");
    for (int n=0; n<=max; n++) {
      int fibExpected = fib_ref(n);
      int fibActual = numToInt(fib(intToNum(n)));
      assert fibExpected == fibActual;
      System.out.println("fib(" + n + "): expected = " + fibExpected + ", actual = " + fibActual);
    }
    System.out.println("Fibonacci method test finished");
  }

}
