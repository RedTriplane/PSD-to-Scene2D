package com.jfixby.tool.psd2scene2d;

import com.jfixby.scarabei.api.collections.Collection;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.file.File;

public class PSDRepackingStatus {

	private List<File> related_folders;

	public void setRelatedFolders(List<File> related_folders) {
		this.related_folders = related_folders;
	}

	public PSDRepackingStatus() {

	}

	public Collection<File> getRelatedFolders() {
		return related_folders;
	}

}
