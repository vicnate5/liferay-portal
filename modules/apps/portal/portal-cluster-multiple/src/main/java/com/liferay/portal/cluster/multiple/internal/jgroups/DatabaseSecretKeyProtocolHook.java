/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.cluster.multiple.internal.jgroups;

import com.liferay.portal.kernel.dao.db.DBInspector;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.security.Key;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.jgroups.protocols.SYM_ENCRYPT;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolHook;

/**
 * Proof of concept. Resolves a shared AES key from the Liferay database and
 * injects it into <code>SYM_ENCRYPT</code> before JGroups calls
 * <code>init()</code>, so that no keystore is ever read and no operator
 * configuration is required.
 *
 * <p>
 * Wired through the JGroups <code>after_creation_hook</code> attribute on the
 * <code>SYM_ENCRYPT</code> element. See
 * <code>jgroups/secure/auto/udp_control.xml</code>.
 * </p>
 *
 * <p>
 * Deliberately uses <code>KeyGenerator</code> and <code>Base64</code> directly
 * instead of Liferay's <code>Encryptor</code>, to keep the startup path free of
 * OSGi service dependencies.
 * </p>
 *
 * <p>
 * Two system properties exist only to test rejection of a mismatched key:
 * </p>
 *
 * <ul>
 * <li>
 * <code>sym.encrypt.poc.key.override</code>: a Base64 AES key to use instead of
 * the database value, or the literal <code>random</code> to generate a throwaway
 * key on every boot
 * </li>
 * </ul>
 */
public class DatabaseSecretKeyProtocolHook implements ProtocolHook {

	@Override
	public void afterCreation(Protocol protocol) throws Exception {
		if (!(protocol instanceof SYM_ENCRYPT)) {
			_log.info(
				_PREFIX + "Ignoring non-SYM_ENCRYPT protocol " +
					protocol.getName());

			return;
		}

		SYM_ENCRYPT symEncrypt = (SYM_ENCRYPT)protocol;

		_log.info(
			_PREFIX + "afterCreation entered for " + symEncrypt.getName() +
				"@" + System.identityHashCode(symEncrypt));

		_log.info(
			_PREFIX + "Pre-init state {secretKey=" + symEncrypt.secretKey() +
				", symVersion=" + _toHex(symEncrypt.symVersion()) +
					", symAlgorithm=" + symEncrypt.symAlgorithm() +
						", symIvLength=" + symEncrypt.simIvLength() + "}");

		_logCallStack();

		SecretKey secretKey = _resolveSecretKey();

		symEncrypt.setSecretKey(secretKey);

		_log.info(
			_PREFIX + "setSecretKey returned {secretKeyFingerprint=" +
				_fingerprint(secretKey) + ", symAlgorithm=" +
					symEncrypt.symAlgorithm() + ", symVersion=" +
						_toHex(symEncrypt.symVersion()) + "}");

		_startPostInitProbe(symEncrypt);
	}

	private String _fingerprint(Key key) {
		byte[] encoded = key.getEncoded();

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < Math.min(4, encoded.length); i++) {
			sb.append(String.format("%02x", encoded[i]));
		}

		sb.append("..(");
		sb.append(encoded.length * 8);
		sb.append(" bits)");

		return sb.toString();
	}

	private SecretKey _generateSecretKey() throws Exception {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

		keyGenerator.init(128);

		return keyGenerator.generateKey();
	}

	private void _logCallStack() {
		StackTraceElement[] stackTraceElements =
			new Throwable().getStackTrace();

		StringBuilder sb = new StringBuilder();

		sb.append(_PREFIX);
		sb.append("Call stack at hook time:");

		for (int i = 0; (i < stackTraceElements.length) && (i < 8); i++) {
			sb.append("\n\tat ");
			sb.append(stackTraceElements[i]);
		}

		_log.info(sb.toString());
	}

	private SecretKey _resolveSecretKey() throws Exception {
		String override = System.getProperty(
			"sym.encrypt.poc.key.override");

		if (override != null) {
			if (override.equals("random")) {
				SecretKey secretKey = _generateSecretKey();

				_log.info(
					_PREFIX +
						"OVERRIDE: generated a throwaway random key, ignoring " +
							"the database {fingerprint=" +
								_fingerprint(secretKey) + "}");

				return secretKey;
			}

			SecretKey secretKey = new SecretKeySpec(
				Base64.getDecoder().decode(override), "AES");

			_log.info(
				_PREFIX + "OVERRIDE: using the key from the system property, " +
					"ignoring the database {fingerprint=" +
						_fingerprint(secretKey) + "}");

			return secretKey;
		}

		try (Connection connection = DataAccess.getConnection()) {
			_log.info(
				_PREFIX + "Opened JDBC connection {catalog=" +
					connection.getCatalog() + ", schema=" +
						connection.getSchema() + ", url=" +
							connection.getMetaData().getURL() + "}");

			DBInspector dbInspector = new DBInspector(connection);

			if (!dbInspector.hasTable(_TABLE_NAME)) {
				_log.info(_PREFIX + "Table " + _TABLE_NAME + " is absent");

				_createTable(connection);
			}
			else {
				_log.info(_PREFIX + "Table " + _TABLE_NAME + " already exists");
			}

			SecretKey secretKey = _selectSecretKey(connection);

			if (secretKey != null) {
				_log.info(
					_PREFIX + "REUSED the key already stored in " +
						_TABLE_NAME + " {keyId=" + _KEY_ID +
							", fingerprint=" + _fingerprint(secretKey) + "}");

				return secretKey;
			}

			secretKey = _generateSecretKey();

			_log.info(
				_PREFIX + "GENERATED a new AES key {keyId=" + _KEY_ID +
					", fingerprint=" + _fingerprint(secretKey) + "}");

			try (PreparedStatement preparedStatement =
					connection.prepareStatement(
						"insert into " + _TABLE_NAME +
							" (keyId, encodedKey) values (?, ?)")) {

				preparedStatement.setString(1, _KEY_ID);
				preparedStatement.setString(
					2,
					Base64.getEncoder().encodeToString(
						secretKey.getEncoded()));

				preparedStatement.executeUpdate();

				_log.info(
					_PREFIX + "INSERTED the generated key into " + _TABLE_NAME);

				return secretKey;
			}
			catch (SQLException sqlException) {
				_log.info(
					_PREFIX + "Insert failed, assuming another node won the " +
						"first boot race, reselecting {message=" +
							sqlException.getMessage() + "}");

				SecretKey winningSecretKey = _selectSecretKey(connection);

				if (winningSecretKey == null) {
					throw sqlException;
				}

				_log.info(
					_PREFIX + "REUSED the key inserted by the winning node " +
						"{fingerprint=" + _fingerprint(winningSecretKey) + "}");

				return winningSecretKey;
			}
		}
	}

	private void _createTable(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(
				"create table " + _TABLE_NAME +
					" (keyId varchar(75) not null primary key, encodedKey " +
						"varchar(255) not null)");

			_log.info(_PREFIX + "Created table " + _TABLE_NAME);
		}
		catch (SQLException sqlException) {
			_log.info(
				_PREFIX + "Unable to create table " + _TABLE_NAME +
					", assuming another node created it first {message=" +
						sqlException.getMessage() + "}");
		}
	}

	private SecretKey _selectSecretKey(Connection connection)
		throws SQLException {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				"select encodedKey from " + _TABLE_NAME + " where keyId = ?")) {

			preparedStatement.setString(1, _KEY_ID);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (!resultSet.next()) {
					return null;
				}

				return new SecretKeySpec(
					Base64.getDecoder().decode(resultSet.getString(1)), "AES");
			}
		}
	}

	private void _startPostInitProbe(SYM_ENCRYPT symEncrypt) {
		Thread thread = new Thread(
			() -> {
				try {
					Thread.sleep(20000);
				}
				catch (InterruptedException interruptedException) {
					return;
				}

				_log.info(
					_PREFIX + "POST-INIT probe for " + symEncrypt.getName() +
						"@" + System.identityHashCode(symEncrypt) +
							" {secretKeyFingerprint=" +
								_fingerprint(symEncrypt.secretKey()) +
									", symAlgorithm=" +
										symEncrypt.symAlgorithm() +
											", symIvLength=" +
												symEncrypt.simIvLength() +
													", symVersion=" +
														_toHex(
															symEncrypt.
																symVersion()) +
															"}");
			},
			"SYM_ENCRYPT PoC post-init probe");

		thread.setDaemon(true);

		thread.start();
	}

	private String _toHex(byte[] bytes) {
		if (bytes == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder();

		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}

		return sb.toString();
	}

	private static final String _KEY_ID = "cluster-link-default";

	private static final String _PREFIX = "[SYM-DB-POC] ";

	private static final String _TABLE_NAME = "ClusterSymEncryptKey";

	private static final Log _log = LogFactoryUtil.getLog(
		DatabaseSecretKeyProtocolHook.class);

}
