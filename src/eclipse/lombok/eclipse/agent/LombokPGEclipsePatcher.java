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
package lombok.eclipse.agent;

import java.lang.instrument.Instrumentation;

import lombok.core.Agent;
import lombok.patcher.ScriptManager;
import lombok.patcher.equinox.EquinoxClassLoader;

public final class LombokPGEclipsePatcher extends Agent {
	@Override
	public void runAgent(final String agentArgs, final Instrumentation instrumentation, final boolean injected) throws Exception {
		String[] args = agentArgs == null ? new String[0] : agentArgs.split(":");
		boolean forceEcj = false;
		boolean forceEclipse = false;
		for (String arg : args) {
			if (arg.trim().equalsIgnoreCase("ECJ")) forceEcj = true;
			if (arg.trim().equalsIgnoreCase("ECLIPSE")) forceEclipse = true;
		}
		if (forceEcj && forceEclipse) {
			forceEcj = false;
			forceEclipse = false;
		}

		boolean ecj;

		if (forceEcj) ecj = true;
		else if (forceEclipse) ecj = false;
		else ecj = injected;
		
		registerPatchScripts(instrumentation, injected, ecj);
	}

	private void registerPatchScripts(final Instrumentation instrumentation, final boolean reloadExistingClasses, final boolean ecjOnly) {
		ScriptManager sm = new ScriptManager();
		sm.registerTransformer(instrumentation);
		if (!ecjOnly) {
			EquinoxClassLoader.addPrefix("lombok.");
			EquinoxClassLoader.registerScripts(sm);
		}
		patchEcjTransformers(sm, ecjOnly);

		if (reloadExistingClasses) sm.reloadClasses(instrumentation);
	}

	private void patchEcjTransformers(final ScriptManager sm, final boolean ecj) {
		PatchAutoGenMethodStub.addPatches(sm, ecj);
		PatchExtensionMethod.addPatches(sm, ecj);
		PatchListenerSupport.addPatches(sm, ecj);
		PatchFunction.addPatches(sm, ecj);
		PatchAction.addPatches(sm, ecj);
		PatchVisibleForTesting.addPatches(sm, ecj);
	}
}
