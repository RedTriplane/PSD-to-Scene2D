
package com.jfixby.tool.psd2scene2d;

import com.jfixby.cmns.api.file.File;
import com.jfixby.cmns.api.log.L;

public class PNGQuant {
	static String INPUT_FILE_NAME = "INPUT_FILE_NAME";
	static String OUTPUT_FILE_NAME = "OUTPUT_FILE_NAME";
	static String PNGQuant_COMMAND_TEMPLATE = "pngquant --force --output " + OUTPUT_FILE_NAME
		+ " --skip-if-larger --verbose --speed 1 --quality 80-90 " + INPUT_FILE_NAME;

	public static void compressFile (final File inputFile, final File outputFile) {
		final String inputFileName = inputFile.toJavaFile().getAbsolutePath();
		final String outputFileName = outputFile.toJavaFile().getAbsolutePath();
		final String command = PNGQuant_COMMAND_TEMPLATE.replaceAll(INPUT_FILE_NAME, inputFileName).replaceAll(OUTPUT_FILE_NAME,
			outputFileName);
		L.d("", command);
	}
}
