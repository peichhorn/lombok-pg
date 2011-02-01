/*
 * Copyright © 2010-2011 Philipp Eichhorn
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
package lombok.core;

import java.lang.instrument.Instrumentation;

import lombok.core.Agent;

public abstract class LombokPGAgent extends Agent {
	public static void agentmain(String agentArgs, Instrumentation instrumentation) throws Throwable {
		Agent.agentmain(agentArgs, instrumentation);
		runMoreAgents(agentArgs, instrumentation, true);
	}

	public static void premain(String agentArgs, Instrumentation instrumentation) throws Throwable {
		Agent.premain(agentArgs, instrumentation);
		runMoreAgents(agentArgs, instrumentation, false);
	}

	private static void runMoreAgents(String agentArgs, Instrumentation instrumentation, boolean injected) throws Throwable {
		AgentInfo info = new LombokPGEclipsePatcherInfo();
		try {
			Class<?> agentClass = Class.forName(info.className());
			Agent agent = (Agent) agentClass.newInstance();
			agent.runAgent(agentArgs, instrumentation, injected);
		} catch (Throwable t) {
			info.problem(t, instrumentation);
		}
	}

	// copy of Agent.AgentInfo, which is private
	private static abstract class AgentInfo {
		abstract String className();

		void problem(Throwable t, Instrumentation instrumentation) throws Throwable {
			if (t instanceof ClassNotFoundException) {
				//That's okay - this lombok-pg evidently is a version with support for something stripped out.
				return;
			}

			if (t instanceof ClassCastException) {
				throw new InternalError("Lombok-PG bug. Class: " + className() + " is not an implementation of lombok.core.Agent");
			}

			if (t instanceof IllegalAccessError) {
				throw new InternalError("Lombok-PG bug. Class: " + className() + " is not public");
			}

			if (t instanceof InstantiationException) {
				throw new InternalError("Lombok-PG bug. Class: " + className() + " is not concrete or has no public no-args constructor");
			}

			throw t;
		}
	}

	private static class LombokPGEclipsePatcherInfo extends AgentInfo {
		@Override String className() {
			return "lombok.eclipse.agent.LombokPGEclipsePatcher";
		}
	}
}
