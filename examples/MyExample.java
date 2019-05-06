
class Overload1 {

    public static void main(String[] args) {}

}
class A {	
    public A foo() {
      return (new B());
      }
    }
      
  class B extends A {}