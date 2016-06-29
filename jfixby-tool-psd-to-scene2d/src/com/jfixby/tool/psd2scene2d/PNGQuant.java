
package com.jfixby.tool.psd2scene2d;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jfixby.cmns.api.file.File;
import com.jfixby.cmns.api.log.L;

public class PNGQuant {

	public static void compressFile (final File inputFile, final File outputFile) {
		final String inputFileName = inputFile.toJavaFile().getAbsolutePath();
		final String outputFileName = outputFile.toJavaFile().getAbsolutePath();
		final String command = "pngquant.exe --force --output " + outputFileName
			+ " --skip-if-larger --verbose --speed 1 --quality 80-90 " + inputFileName;
// L.d("", command);
// executeCommand(command);
// executeCommand("pngquant.exe");
		executeCommand(command.split(" "));
	}

	static private boolean executeCommand (final String... command) {
		L.d("executeCommand", command);

		final StringBuilder output = new StringBuilder();
		boolean success = false;
		final String testString = "writing 256-color image";
		try {

			final ProcessBuilder ps = new ProcessBuilder(command);
			ps.redirectErrorStream(true);
			final Process p = ps.start();
			final InputStream is = p.getInputStream();
			final InputStreamReader reader = new InputStreamReader(is);
			final BufferedReader buff = new BufferedReader(reader);

			while (true) {
				final String last_line = buff.readLine();
				if (last_line == null) {
					break;
				}
				if (last_line.contains(testString)) {
					success = true;
				}
				L.d(last_line);
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}

		final String result = output.toString();
		L.d(result);
		return success;
	}
}
