/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.calendar.collab.rest.resource.v1_0;

import com.liferay.calendar.collab.rest.dto.v1_0.CalendarView;
import com.liferay.calendar.collab.rest.dto.v1_0.EventInstance;
import com.liferay.calendar.collab.rest.dto.v1_0.EventInvitation;
import com.liferay.calendar.collab.rest.dto.v1_0.RecurrencePattern;

import jakarta.annotation.Generated;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @author Liferay
 * @generated
 */
@Generated("")
@ProviderType
public interface CalendarCollabResource {

	public List<EventInstance> getCalendarEventRecurrence(
			String calendarId, String eventId)
		throws Exception;

	public List<EventInstance> parseRecurrencePattern(
			String calendarId, RecurrencePattern recurrencePattern)
		throws Exception;

	public List<EventInvitation> getEventInvitations(
			String calendarId, String eventId)
		throws Exception;

	public Response addEventInvitations(
			String calendarId, String eventId,
			List<EventInvitation> eventInvitations)
		throws Exception;

	public CalendarView getCalendarView(
			String calendarId, String viewType, String startDate,
			String endDate)
		throws Exception;

	public void setContextAcceptLanguage(
		com.liferay.portal.vulcan.accept.language.AcceptLanguage
			contextAcceptLanguage);

	public void setContextCompany(
		com.liferay.portal.kernel.model.Company contextCompany);

	public void setContextHttpServletRequest(
		HttpServletRequest contextHttpServletRequest);

	public void setContextHttpServletResponse(
		HttpServletResponse contextHttpServletResponse);

	public void setContextUriInfo(UriInfo contextUriInfo);

	public void setContextUser(
		com.liferay.portal.kernel.model.User contextUser);

}

