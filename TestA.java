public class TestA {

	void hello ( TypeA t ) {
		System.out.println( "hello "+t.myType()+", it's me, TypeA!" );
	}
}

class TestB extends TestA {
	
	void hello ( TypeB t ) {
		super.hello( t );
		System.out.println( "hello "+t.myType()+", it's me, TypeB!" );
	}
	
	public static void main (String[] args) {
		
		TestA a = new TestA();
		TestB b = new TestB();
	
		a.hello( new TypeB() );
		System.out.println();
		b.hello( new TypeB() );
		System.out.println();
		b.hello( new TypeBB() );
	}
}
