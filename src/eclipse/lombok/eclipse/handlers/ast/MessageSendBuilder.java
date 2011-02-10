package lombok.eclipse.handlers.ast;

import static lombok.eclipse.handlers.Eclipse.setGeneratedByAndCopyPos;
import static lombok.eclipse.handlers.ast.Arrays.buildArray;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.eclipse.EclipseNode;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class MessageSendBuilder implements ExpressionBuilder<MessageSend> {
	private ExpressionBuilder<? extends Expression> receiver = new ThisReferenceBuilder(true);
	private final String name;
	private final List<ExpressionBuilder<? extends Expression>> args = new ArrayList<ExpressionBuilder<? extends Expression>>();
	
	MessageSendBuilder(final ExpressionBuilder<? extends Expression> receiver, final String name) {
		this.receiver = receiver;
		this.name = name;
	}
	
	public MessageSendBuilder withArgument(final ExpressionBuilder<? extends Expression> argument) {
		args.add(argument);
		return this;
	}
	
	public MessageSendBuilder withArguments(final List<ExpressionBuilder<? extends Expression>> arguments) {
		args.addAll(arguments);
		return this;
	}
	
	@Override
	public MessageSend build(final EclipseNode node, final ASTNode source) {
		MessageSend messageSend = new MessageSend();
		setGeneratedByAndCopyPos(messageSend, source);
		messageSend.receiver = receiver.build(node, source);
		messageSend.selector = name.toCharArray();
		messageSend.arguments = buildArray(args, new Expression[0], node, source);
		return messageSend;
	}
}
