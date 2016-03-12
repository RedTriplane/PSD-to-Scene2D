package com.jfixby.tool.psd2scene2d;

import com.jfixby.cmns.api.collections.Collection;
import com.jfixby.cmns.api.collections.Collections;
import com.jfixby.cmns.api.collections.List;
import com.jfixby.cmns.api.debug.Debug;
import com.jfixby.cmns.api.file.File;

public class PSDRepackerResult {
    final List<File> atlasPackages = Collections.newList();

    public void addPackedAtlasPackageFolder(File atlasPackageFolder) {
	atlasPackages.add(Debug.checkNull(atlasPackageFolder));
    }

    public Collection<File> listAtlasPackages() {
	return atlasPackages;
    }

}
