
package com.jfixby.tool.psd2scene2d.test;

import com.jfixby.cmns.api.file.File;
import com.jfixby.cmns.api.file.LocalFileSystem;
import com.jfixby.red.desktop.DesktopSetup;
import com.jfixby.tool.psd2scene2d.PNGQuant;

public class TestPNGQuantCompress {
	public static final void main (final String[] arg) {
		DesktopSetup.deploy();

		final String testChamber = "D:\\[DEV]\\[GIT]\\Games\\Tinto\\tinto-assets-packer\\pngquant";
		final File chamberFolder = LocalFileSystem.newFile(testChamber);

		final File inputFile = chamberFolder.child("com.jfixby.tinto.scene-002.psd.raster.gdx-atlas.atlasdata.0.png");
		final File outputFile = chamberFolder.child("result.png");

		PNGQuant.compressFile(inputFile, outputFile);
	}
}
