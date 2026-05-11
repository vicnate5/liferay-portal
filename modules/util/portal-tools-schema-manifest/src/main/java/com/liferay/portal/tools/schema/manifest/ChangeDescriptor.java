/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.schema.manifest;

public class ChangeDescriptor {

	public ChangeDescriptor(String op) {
		this.op = op;
	}

	public String op;
	public String table;
	public String column;
	public String toType;
	public String derivation;
	public String note;

}
