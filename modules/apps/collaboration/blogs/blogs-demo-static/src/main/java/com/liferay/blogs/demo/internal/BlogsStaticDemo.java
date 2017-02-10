/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.blogs.demo.internal;

import com.liferay.blogs.demo.data.creator.BlogsEntryDemoDataCreator;
import com.liferay.portal.instance.lifecycle.BasePortalInstanceLifecycleListener;
import com.liferay.portal.instance.lifecycle.PortalInstanceLifecycleListener;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.security.RandomUtil;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.users.admin.demo.data.creator.BasicUserDemoDataCreator;
import com.liferay.users.admin.demo.data.creator.OmniAdminUserDemoDataCreator;
import com.liferay.users.admin.demo.data.creator.SiteAdminUserDemoDataCreator;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Victor Ware
 * @author Sergio González
 */
@Component(immediate = true, service = PortalInstanceLifecycleListener.class)
public class BlogsStaticDemo extends BasePortalInstanceLifecycleListener {

	@Override
	public void portalInstanceRegistered(Company company) throws Exception {
		Group guestGroup = _groupLocalService.getGroup(
			company.getCompanyId(), "Guest");

		_siteAdminUserDemoDataCreator.create(
				guestGroup.getGroupId(), "siteadmin.user@liferay.com");

		_omniAdminUserDemoDataCreator.create(
				company.getCompanyId(), "omniadmin.user@liferay.com");

		User basicUser = _basicUserDemoDataCreator.create(
				company.getCompanyId(), "basic.user@liferay.com");

		_staticBlogsEntryDemoDataCreator.create(
				basicUser.getUserId(), guestGroup.getGroupId());

		// for (int i = 1; i < 4; i++) {
		// 	// String title = "Blogs Entry Title " + String.valueOf(i);
		// 	// String subtitle = "Blogs Entry Subtitle " + String.valueOf(i);
		// 	// String content = "Blogs Entry Content " + String.valueOf(i);

		// 	_staticBlogsEntryDemoDataCreator.createBlogsEntry(
		// 		basicUser.getUserId(), guestGroup.getGroupId(), title, 
		// 		subtitle, content);
		// }	
	}

	@Deactivate
	protected void deactivate() throws PortalException {
		_basicUserDemoDataCreator.delete();
		_staticBlogsEntryDemoDataCreator.delete();
		_siteAdminUserDemoDataCreator.delete();
	}

	@Reference(unbind = "-")
	protected void setBasicUserDemoDataCreator(
		BasicUserDemoDataCreator basicUserDemoDataCreator) {

		_basicUserDemoDataCreator = basicUserDemoDataCreator;
	}

	@Reference(unbind = "-")
	protected void setGroupLocalService(GroupLocalService groupLocalService) {
		_groupLocalService = groupLocalService;
	}

	@Reference(target = "(source=static)", unbind = "-")
	protected void setStaticBlogsEntryDemoDataCreator(
		BlogsEntryDemoDataCreator blogsEntryDemoDataCreator) {

		_staticBlogsEntryDemoDataCreator = blogsEntryDemoDataCreator;
	}

	@Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED, unbind = "-")
	protected void setModuleServiceLifecycle(
		ModuleServiceLifecycle moduleServiceLifecycle) {
	}

	@Reference(unbind = "-")
	protected void setOmniAdminUserDemoDataCreator(
		OmniAdminUserDemoDataCreator omniAdminUserDemoDataCreator) {

		_omniAdminUserDemoDataCreator = omniAdminUserDemoDataCreator;
	}

	@Reference(unbind = "-")
	protected void setSiteAdminUserDemoDataCreator(
		SiteAdminUserDemoDataCreator siteAdminUserDemoDataCreator) {

		_siteAdminUserDemoDataCreator = siteAdminUserDemoDataCreator;
	}

	private BasicUserDemoDataCreator _basicUserDemoDataCreator;
	private GroupLocalService _groupLocalService;
	private BlogsEntryDemoDataCreator _staticBlogsEntryDemoDataCreator;
	private OmniAdminUserDemoDataCreator _omniAdminUserDemoDataCreator;
	private SiteAdminUserDemoDataCreator _siteAdminUserDemoDataCreator;

}