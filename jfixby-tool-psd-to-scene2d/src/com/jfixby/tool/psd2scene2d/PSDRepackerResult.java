
package com.jfixby.tool.psd2scene2d;

import com.jfixby.scarabei.api.collections.Collection;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.debug.Debug;
import com.jfixby.scarabei.api.file.File;

public class PSDRepackerResult {
	final List<File> atlasPackages = Collections.newList();

	public void addPackedAtlasPackageFolder (final File atlasPackageFolder) {
		this.atlasPackages.add(Debug.checkNull(atlasPackageFolder));
	}

	public Collection<File> listAtlasPackages () {
		return this.atlasPackages;
	}

	final List<CompressionInfo> compressedPNG = Collections.newList();;

	public void addCompressionInfo (final String file_name, final long originalSize, final long newSize) {
		this.compressedPNG.add(new CompressionInfo(file_name, originalSize, newSize));
		this.compressedPNG.getLast().print();
	}

	public Collection<CompressionInfo> listCompressions () {
		return this.compressedPNG;
	}

}
