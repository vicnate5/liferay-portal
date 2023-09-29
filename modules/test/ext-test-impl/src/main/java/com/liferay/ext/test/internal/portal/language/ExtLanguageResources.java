/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ext.test.internal.portal.language;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.language.LanguageResources;

/**
 * @author Kevin Valencia
 */
public class ExtLanguageResources extends LanguageResources {

	@Override
	public void afterPropertiesSet() {
		if (_log.isInfoEnabled()) {
			_log.info("Set portalResourceBundleLoader by ExtLanguageResources");
		}

		super.afterPropertiesSet();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ExtLanguageResources.class);

}