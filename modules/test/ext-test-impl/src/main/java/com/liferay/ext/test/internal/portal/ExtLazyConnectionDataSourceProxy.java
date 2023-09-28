/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ext.test.internal.portal;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * @author Kevin Valencia
 */
public class ExtLazyConnectionDataSourceProxy
	extends LazyConnectionDataSourceProxy {

	@Override
	public Connection getConnection() throws SQLException {
		if (_log.isInfoEnabled() && !_logPrinted) {
			_log.info(
				"Getting connection from ExtLazyConnectionDataSourceProxy");

			_logPrinted = true;
		}

		return super.getConnection();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ExtLazyConnectionDataSourceProxy.class);

	private boolean _logPrinted;

}