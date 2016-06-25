
package com.jfixby.tool.psd2scene2d;

import com.jfixby.cmns.api.log.L;
import com.jfixby.cmns.api.math.FloatMath;

public class CompressionInfo {

	private final String file_name;
	private final long originalSize;
	private final long newSize;
	private final float compression;

	public CompressionInfo (final String file_name, final long originalSize, final long newSize) {
		this.file_name = file_name;
		this.originalSize = originalSize;
		this.newSize = newSize;
		this.compression = (float)FloatMath.roundToDigit((newSize * 100f / originalSize), 0);

	}

	@Override
	public String toString () {
		return "FileCompressed: " + this.file_name + " ratio: " + this.compression + "%";
	}

	public void print () {
		L.d(this);
	}

}
