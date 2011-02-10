package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.ast.ASTBuilder.*;

public class Example {
	public void test() {
		Throw(New(Type(RuntimeException.class)).withArgument(String("Shit so ca$h!")).withArgument(Null()));
	}
}
