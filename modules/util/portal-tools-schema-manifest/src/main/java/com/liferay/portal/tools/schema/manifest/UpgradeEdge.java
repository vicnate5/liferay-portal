/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.schema.manifest;

import java.util.List;

public class UpgradeEdge {

	public UpgradeEdge(
		String fromVersion, String toVersion,
		List<ChangeDescriptor> changes) {

		this.fromVersion = fromVersion;
		this.toVersion = toVersion;
		this.changes = changes;
	}

	public final String fromVersion;
	public final String toVersion;
	public final List<ChangeDescriptor> changes;

}
