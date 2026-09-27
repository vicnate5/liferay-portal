/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.schema.manifest;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Victor Ware
 */
public class RegistratorParser {

	public static List<UpgradeEdge> parse(File file) throws IOException {
		String source = new String(Files.readAllBytes(file.toPath()));

		// Strip comments then collapse whitespace so all register() calls are
		// on a single logical line — makes balanced-paren extraction reliable.

		source = source.replaceAll("//[^\n]*", "");
		source = source.replaceAll("(?s)/\\*.*?\\*/", " ");
		source = source.replaceAll("\\s+", " ");

		List<UpgradeEdge> edges = new ArrayList<>();

		int idx = 0;

		while (true) {
			int registerIdx = source.indexOf("registry.register(", idx);

			if (registerIdx < 0) {
				break;
			}

			int parenStart = registerIdx + "registry.register".length();

			int parenEnd = _findMatchingParen(source, parenStart);

			if (parenEnd < 0) {
				idx = registerIdx + 1;

				continue;
			}

			String block = source.substring(parenStart + 1, parenEnd);

			UpgradeEdge edge = _parseBlock(block);

			if (edge != null) {
				edges.add(edge);
			}

			idx = parenEnd + 1;
		}

		return edges;
	}

	private static List<ChangeDescriptor> _extractChanges(String block) {
		List<ChangeDescriptor> changes = new ArrayList<>();

		// UpgradeProcessFactory.alterColumnType("Table", "col", "TYPE")

		Matcher alterColTypeMatcher = _alterColTypePattern.matcher(block);

		while (alterColTypeMatcher.find()) {
			ChangeDescriptor c = new ChangeDescriptor("alterColumnType");

			c.table = alterColTypeMatcher.group(1);
			c.column = alterColTypeMatcher.group(2);
			c.toType = alterColTypeMatcher.group(3);
			c.derivation = "UpgradeProcessFactory";
			changes.add(c);
		}

		// UpgradeProcessFactory.addColumns("Table", "col TYPE[, col2 TYPE2]")

		Matcher addColumnsMatcher = _addColumnsPattern.matcher(block);

		while (addColumnsMatcher.find()) {
			String table = addColumnsMatcher.group(1);

			for (String colDef :
					addColumnsMatcher.group(
						2
					).split(
						","
					)) {

				colDef = colDef.trim();

				if (colDef.isEmpty()) {
					continue;
				}

				String[] parts = colDef.split("\\s+", 2);
				ChangeDescriptor c = new ChangeDescriptor("addColumn");

				c.table = table;
				c.column = parts[0];
				c.toType = (parts.length > 1) ? parts[1] : null;
				c.derivation = "UpgradeProcessFactory";
				changes.add(c);
			}
		}

		// UpgradeProcessFactory.runSQL(...) — data-only, no DDL

		if (block.contains("UpgradeProcessFactory.runSQL")) {
			ChangeDescriptor c = new ChangeDescriptor("dataMigration");

			c.derivation = "UpgradeProcessFactory.runSQL";
			c.note = "Data-only migration; no DDL change";
			changes.add(c);
		}

		// MVCCVersionUpgradeProcess — adds mvccVersion LONG to listed tables

		if (block.contains("MVCCVersionUpgradeProcess")) {
			for (String table :
					_tablesFromAnonymousClass(
						block, "MVCCVersionUpgradeProcess")) {

				ChangeDescriptor c = new ChangeDescriptor("addColumn");

				c.table = table;
				c.column = "mvccVersion";
				c.toType = "LONG default 0 not null";
				c.derivation = "MVCCVersionUpgradeProcess";
				changes.add(c);
			}
		}

		// CTModelUpgradeProcess("T1", "T2", ...) — adds ctCollectionId LONG

		if (block.contains("CTModelUpgradeProcess")) {
			for (String table :
					_tablesFromConstructor(block, "CTModelUpgradeProcess")) {

				ChangeDescriptor c = new ChangeDescriptor("addColumn");

				c.table = table;
				c.column = "ctCollectionId";
				c.toType = "LONG default 0 not null";
				c.derivation = "CTModelUpgradeProcess";
				changes.add(c);
			}
		}

		// BaseExternalReferenceCodeUpgradeProcess — adds externalReferenceCode

		if (block.contains("BaseExternalReferenceCodeUpgradeProcess")) {
			for (String table :
					_tablesFromAnonymousClass(
						block, "BaseExternalReferenceCodeUpgradeProcess")) {

				ChangeDescriptor c = new ChangeDescriptor("addColumn");

				c.table = table;
				c.column = "externalReferenceCode";
				c.toType = "VARCHAR(75) null";
				c.derivation = "BaseExternalReferenceCodeUpgradeProcess";
				changes.add(c);
			}
		}

		// BaseSQLServerDatetimeUpgradeProcess — alters datetime columns

		if (block.contains("BaseSQLServerDatetimeUpgradeProcess")) {
			ChangeDescriptor c = new ChangeDescriptor("alterColumnType");

			c.derivation = "BaseSQLServerDatetimeUpgradeProcess";
			c.note = "Converts datetime columns for SQL Server compatibility";
			changes.add(c);
		}

		// Anything unrecognized is flagged as custom —
		// needs @SchemaChange annotation

		if (changes.isEmpty() && !block.contains("DummyUpgradeStep")) {
			Matcher newClassMatcher = _newClassPattern.matcher(block);

			while (newClassMatcher.find()) {
				String full = newClassMatcher.group(1);

				String simple =
					full.contains(".") ?
						full.substring(full.lastIndexOf('.') + 1) : full;

				if (!_skipClasses.contains(simple) && !simple.isEmpty() &&
					Character.isUpperCase(simple.charAt(0))) {

					ChangeDescriptor c = new ChangeDescriptor("custom");

					c.derivation = simple;
					c.note = "@SchemaChange annotation required";
					changes.add(c);
				}
			}
		}

		return changes;
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

	private static UpgradeEdge _parseBlock(String block) {
		List<String> quoted = new ArrayList<>();

		Matcher quotedMatcher = _quotedPattern.matcher(block);

		while (quotedMatcher.find()) {
			quoted.add(quotedMatcher.group(1));
		}

		if (quoted.size() < 2) {
			return null;
		}

		return new UpgradeEdge(
			quoted.get(0), quoted.get(1), _extractChanges(block));
	}

	private static List<String> _quotedStrings(String text) {
		List<String> results = new ArrayList<>();

		Matcher quotedMatcher = _quotedPattern.matcher(text);

		while (quotedMatcher.find()) {
			results.add(quotedMatcher.group(1));
		}

		return results;
	}

	private static List<String> _tablesFromAnonymousClass(
		String block, String className) {

		int classIdx = block.indexOf(className);

		if (classIdx < 0) {
			return new ArrayList<>();
		}

		// Find "return new String" after the class name, then the array brace

		int returnIdx = block.indexOf("return new String", classIdx);

		if (returnIdx < 0) {
			return new ArrayList<>();
		}

		int braceStart = block.indexOf('{', returnIdx);

		int braceEnd = block.indexOf('}', braceStart + 1);

		if ((braceStart < 0) || (braceEnd < 0)) {
			return new ArrayList<>();
		}

		return _quotedStrings(block.substring(braceStart + 1, braceEnd));
	}

	private static List<String> _tablesFromConstructor(
		String block, String className) {

		int classIdx = block.indexOf(className + "(");

		if (classIdx < 0) {
			return new ArrayList<>();
		}

		int parenStart = classIdx + className.length();

		int parenEnd = _findMatchingParen(block, parenStart);

		if (parenEnd < 0) {
			return new ArrayList<>();
		}

		return _quotedStrings(block.substring(parenStart + 1, parenEnd));
	}

	private static final Pattern _addColumnsPattern = Pattern.compile(
		"UpgradeProcessFactory\\.addColumns\\( *\"([^\"]+)\" *, *\"([^\"]+)\"");
	private static final Pattern _alterColTypePattern = Pattern.compile(
		"UpgradeProcessFactory\\.alterColumnType\\( *\"([^\"]+)\"" +
			" *, *\"([^\"]+)\" *, *\"([^\"]+)\"");
	private static final Pattern _newClassPattern = Pattern.compile(
		"new ([\\w.]+)\\(");
	private static final Pattern _quotedPattern = Pattern.compile(
		"\"([^\"]+)\"");
	private static final Set<String> _skipClasses = new HashSet<>(
		Arrays.asList(
			"DummyUpgradeStep", "String", "Class", "Object", "Boolean",
			"Integer", "Long", "Double", "ArrayList", "HashMap",
			"MVCCVersionUpgradeProcess", "CTModelUpgradeProcess",
			"BaseExternalReferenceCodeUpgradeProcess",
			"BaseSQLServerDatetimeUpgradeProcess", "UpgradeProcessFactory",
			"StringBundler"));

}