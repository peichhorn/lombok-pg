/*
 * Copyright © 2011 Philipp Eichhorn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
