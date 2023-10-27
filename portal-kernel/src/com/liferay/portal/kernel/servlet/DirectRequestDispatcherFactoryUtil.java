/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.servlet;

import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.WebKeys;

import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * @author Raymond Augé
 */
public class DirectRequestDispatcherFactoryUtil {

	public static RequestDispatcher getRequestDispatcher(
		ServletContext servletContext, String path) {

		RequestDispatcher requestDispatcher = _getRequestDispatcher(
			servletContext, path);

		return new ClassLoaderRequestDispatcherWrapper(
			servletContext, requestDispatcher);
	}

	public static RequestDispatcher getRequestDispatcher(
		ServletRequest servletRequest, String path) {

		ServletContext servletContext =
			(ServletContext)servletRequest.getAttribute(WebKeys.CTX);

		if (servletContext == null) {
			return servletRequest.getRequestDispatcher(path);
		}

		return getRequestDispatcher(servletContext, path);
	}

	private static RequestDispatcher _getetRequestDispatcher(
		ServletContext servletContext, String path) {

		if (!GetterUtil.getBoolean(
				PropsUtil.get(PropsKeys.DIRECT_SERVLET_CONTEXT_ENABLED))) {

			return servletContext.getRequestDispatcher(path);
		}

		if ((path == null) || (path.length() == 0)) {
			return null;
		}

		if (path.charAt(0) != CharPool.SLASH) {
			throw new IllegalArgumentException(
				"Path " + path + " is not relative to context root");
		}

		String contextPath = servletContext.getContextPath();

		String fullPath = contextPath.concat(path);

		String queryString = null;

		int pos = fullPath.indexOf(CharPool.QUESTION);

		if (pos != -1) {
			queryString = fullPath.substring(pos + 1);

			fullPath = fullPath.substring(0, pos);
		}

		Servlet servlet = DirectServletRegistryUtil.getServlet(fullPath);

		if (servlet == null) {
			if (_log.isDebugEnabled()) {
				_log.debug("No servlet found for " + fullPath);
			}

			RequestDispatcher requestDispatcher = null;

			try {
				requestDispatcher = servletContext.getRequestDispatcher(path);

				synchronized (System.err) {
					System.err.println("###Inspect for good case");

					_inspectServletContext(servletContext);
				}
			}
			catch (NullPointerException nullPointerException) {
				synchronized (System.err) {
					System.err.println(
						"####Caught npe on servletContext : " + servletContext +
							" with " + path);

					_inspectServletContext(servletContext);
				}

				throw nullPointerException;
			}

			return new DirectServletPathRegisterDispatcher(
				path, requestDispatcher);
		}

		if (_log.isDebugEnabled()) {
			_log.debug("Servlet found for " + fullPath);
		}

		return new DirectRequestDispatcher(servlet, path, queryString);
	}

	private static void _inspectServletContext(ServletContext servletContext) {
		InvocationHandler invocationHandler =
			ProxyUtil.getInvocationHandler(servletContext);

		System.err.println("Unwrapped " + servletContext + " to get " + invocationHandler);

		try {
			Object defaultObject = _getFieldValue(invocationHandler, "_defaultObject");

			System.err.println("Unwrapped " + invocationHandler + " to get " + defaultObject);

			Object applicationContext = _getFieldValue(defaultObject, "context");

			System.err.println("Unwrapped " + defaultObject + " to get " + applicationContext);

			Object context = _getFieldValue(applicationContext, "context");

			System.err.println("Unwrapped " + applicationContext + " to get " + context);

			Object service = _getFieldValue(applicationContext, "service");

			System.err.println("Unwrapped " + applicationContext + " to get " + service);

			Object mapper = _invoke(service, "getMapper", new Class<?>[0]);

			System.err.println("Invoked " + service + " to get " + mapper);

			Map<?, ?> contextObjectToContextVersionMap = _getFieldValue(mapper, "contextObjectToContextVersionMap");

			System.err.println("Unwrapped " + mapper + " to get " + contextObjectToContextVersionMap);

			System.err.println("Lookup with " + context + ", " + contextObjectToContextVersionMap.get(context));
		}
		catch (Exception exception) {
			System.err.println("Inspection stopped with " + exception.getMessage());
		}
	}

	private static <T> T _getFieldValue(Object instance, String fieldName) {
		try {
			Field field = ReflectionUtil.getDeclaredField(
				instance.getClass(), fieldName);

			return (T)field.get(instance);
		}
		catch (Exception exception) {
			return ReflectionUtil.throwException(exception);
		}
	}

	private static <T> T _invoke(
		Object instance, String methodName, Class<?>[] parameterTypes,
		Object... parameters) {

		try {
			Method method = ReflectionUtil.getDeclaredMethod(
				instance.getClass(), methodName, parameterTypes);

			return (T)method.invoke(instance, parameters);
		}
		catch (InvocationTargetException invocationTargetException) {
			return ReflectionUtil.throwException(
				invocationTargetException.getCause());
		}
		catch (Exception exception) {
			return ReflectionUtil.throwException(exception);
		}
	}

	private static RequestDispatcher _getRequestDispatcher(
		ServletContext servletContext, String path) {

		return new IndirectRequestDispatcher(
			_getetRequestDispatcher(servletContext, path));
	}

	private static final String _EQUINOX_REQUEST_CLASS_NAME =
		"org.eclipse.equinox.http.servlet.internal.servlet." +
			"HttpServletRequestWrapperImpl";

	private static final Log _log = LogFactoryUtil.getLog(
		DirectRequestDispatcherFactoryUtil.class);

	private static class DirectRequestDispatcherServletRequest
		extends HttpServletRequestWrapper {

		@Override
		public ServletContext getServletContext() {
			return _servletContext;
		}

		private DirectRequestDispatcherServletRequest(
			ServletRequest servletRequest, ServletContext servletContext) {

			super((HttpServletRequest)servletRequest);

			_servletContext = servletContext;
		}

		private final ServletContext _servletContext;

	}

	/**
	 * See LPS-79937. We need to protect against redispatch from the module
	 * framework back to the portal, which means we have to unwrap the request.
	 */
	private static class IndirectRequestDispatcher
		implements RequestDispatcher {

		@Override
		public void forward(
				ServletRequest servletRequest, ServletResponse servletResponse)
			throws IOException, ServletException {

			Class<?> clazz = servletRequest.getClass();

			if (_EQUINOX_REQUEST_CLASS_NAME.equals(clazz.getName())) {
				HttpServletRequestWrapper wrapper =
					(HttpServletRequestWrapper)servletRequest;

				servletRequest = new DirectRequestDispatcherServletRequest(
					wrapper.getRequest(), wrapper.getServletContext());
			}

			_requestDispatcher.forward(servletRequest, servletResponse);
		}

		@Override
		public void include(
				ServletRequest servletRequest, ServletResponse servletResponse)
			throws IOException, ServletException {

			Class<?> clazz = servletRequest.getClass();

			if (_EQUINOX_REQUEST_CLASS_NAME.equals(clazz.getName())) {
				HttpServletRequestWrapper wrapper =
					(HttpServletRequestWrapper)servletRequest;

				servletRequest = new DirectRequestDispatcherServletRequest(
					wrapper.getRequest(), wrapper.getServletContext());
			}

			_requestDispatcher.include(servletRequest, servletResponse);
		}

		private IndirectRequestDispatcher(RequestDispatcher requestDispatcher) {
			_requestDispatcher = requestDispatcher;
		}

		private final RequestDispatcher _requestDispatcher;

	}

}