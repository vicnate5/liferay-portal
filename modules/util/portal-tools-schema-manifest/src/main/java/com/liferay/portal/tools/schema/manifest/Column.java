/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.schema.manifest;

public class Column {

	public Column(
		String name, String type, boolean nullable, boolean primaryKey) {

		this.name = name;
		this.type = type;
		this.nullable = nullable;
		this.primaryKey = primaryKey;
	}

	public final String name;
	public final String type;
	public final boolean nullable;
	public final boolean primaryKey;

}
