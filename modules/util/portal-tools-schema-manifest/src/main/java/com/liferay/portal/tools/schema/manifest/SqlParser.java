/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.schema.manifest;

import com.liferay.portal.kernel.util.StringUtil;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Victor Ware
 */
public class SqlParser {

	public static Map<String, TableDef> parse(File file) throws IOException {
		String content = new String(Files.readAllBytes(file.toPath()));

		content = StringUtil.replace(content, "\r\n", "\n");

		content = StringUtil.replace(content, "\t", " ");

		Map<String, TableDef> tables = new LinkedHashMap<>();

		String[] blocks = content.split("(?i)create table ");

		for (int i = 1; i < blocks.length; i++) {
			String block = blocks[i].trim();

			String tableName = block.substring(
				0, _firstWhitespaceOrParen(block)
			).trim();

			int start = block.indexOf('(');

			if (start < 0) {
				continue;
			}

			int end = _findMatchingParen(block, start);

			if (end < 0) {
				continue;
			}

			String body = block.substring(start + 1, end);

			Set<String> pkColumns = new HashSet<>();

			Matcher pkMatcher = _pkPattern.matcher(body);

			if (pkMatcher.find()) {
				for (String pkCol :
						pkMatcher.group(
							1
						).split(
							","
						)) {

					pkColumns.add(pkCol.trim());
				}
			}

			List<Column> columns = new ArrayList<>();

			for (String line : body.split("\n")) {
				line = line.trim();

				if (line.endsWith(",")) {
					line = line.substring(
						0, line.length() - 1
					).trim();
				}

				String lower = StringUtil.toLowerCase(line);

				if (line.isEmpty() || lower.startsWith("primary key") ||
					lower.startsWith("unique") || lower.startsWith("index") ||
					lower.startsWith("key ")) {

					continue;
				}

				String colName = _firstToken(line);

				if (colName.isEmpty()) {
					continue;
				}

				String colType = _secondToken(line, colName);
				boolean nullable = !lower.contains(" not null");
				boolean pK = pkColumns.contains(colName);

				columns.add(new Column(colName, colType, nullable, pK));
			}

			tables.put(tableName, new TableDef(tableName, columns));
		}

		return tables;
	}

	private static int _findMatchingParen(String s, int open) {
		int depth = 0;

		for (int i = open; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c == '(') {
				depth++;
			}
			else if (c == ')') {
				depth--;

				if (depth == 0) {
					return i;
				}
			}
		}

		return -1;
	}

	private static String _firstToken(String line) {
		int end = 0;

		while ((end < line.length()) &&
			   !Character.isWhitespace(line.charAt(end))) {

			end++;
		}

		return line.substring(0, end);
	}

	private static int _firstWhitespaceOrParen(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (Character.isWhitespace(c) || (c == '(')) {
				return i;
			}
		}

		return s.length();
	}

	private static String _secondToken(String line, String firstName) {
		String rest = line.substring(
			firstName.length()
		).trim();

		int end = 0;
		boolean inParen = false;

		while (end < rest.length()) {
			char c = rest.charAt(end);

			if (c == '(') {
				inParen = true;
			}
			else if (c == ')') {
				inParen = false;
			}
			else if (Character.isWhitespace(c) && !inParen) {
				break;
			}

			end++;
		}

		return rest.substring(0, end);
	}

	private static final Pattern _pkPattern = Pattern.compile(
		"primary key \\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);

}