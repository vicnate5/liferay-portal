/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.schema.manifest;

import java.util.List;

public class TableDef {

	public TableDef(String name, List<Column> columns) {
		this.name = name;
		this.columns = columns;
	}

	public final String name;
	public final List<Column> columns;

}
