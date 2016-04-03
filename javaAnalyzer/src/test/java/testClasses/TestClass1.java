package testClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@MyAnnotation1(author="Ryan Wiles", currentRevision=1, date="5/28/14", lastModified="5/28/14", lastModifiedBy="Ryan Wiles", reviewers={})
public class TestClass1 implements TestInterface1{

	static String fieldVar1; // no assignment
	String fieldVar2 = "foo"; // has assignment
	String fieldVar3, fieldVar4; // multiple vars on one line
	HashMap<String, List<String>> foo;
	List<TestClass2> list = new ArrayList<TestClass2>();
	int somePrimativeType;
	
	{ // static initializer
		fieldVar1 = "someValue";
	}
	
	public enum MyEnum1 {
		A, B;
	}
	
	public TestClass1() { // No arg constructor
		
	}
	
	public TestClass1(String var1) { // Constructor with an arg
		this.fieldVar1 = var1;
		this.fieldVar2 = this.fieldVar2;
	}
	
	protected void method1(String var2, List<HashMap<Long, Integer>> genericParameter) {
		System.out.println(fieldVar1);
		
		int[] foo = new int[] { 0, 1, 2 };
		
		long localVar1;
		long localVar2 = 0;
		
		if (var2 instanceof Object) {
			// do nothing
		}
		
		if (System.currentTimeMillis() > 0) {
			long localVar3 = System.currentTimeMillis();
			localVar2 = localVar3;
			localVar1 = this.currentTime();
			localVar1 = foo[(int) this.currentTime()];
		}
		
		localVar1 = localVar2;
		
		String strVar1 = "foo".toUpperCase(Locale.US).toLowerCase();
	}
	
	public long currentTime() {
		return System.currentTimeMillis();
	}
	
	private class TestInnerClass {
		
	}
}

class TestClass3 {
	
}