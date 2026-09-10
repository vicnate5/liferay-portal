#!/usr/bin/env bash
# Seed the container Claude home with the host credentials. Idempotent.
set -euo pipefail

SRC="${HOME}/.claude/.credentials.json"
DEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/claude-home"
DEST="${DEST_DIR}/.credentials.json"

if [[ ! -f "${SRC}" ]]; then
	echo "WARNING: ${SRC} does not exist." >&2
	echo "WARNING: The agent container will start but every turn will fail unauthenticated." >&2
	echo "WARNING: Run 'claude' on the host and log in, then run this script again." >&2
	exit 1
fi

mkdir -p "${DEST_DIR}"
chmod 700 "${DEST_DIR}"

if [[ -f "${DEST}" ]] && cmp -s "${SRC}" "${DEST}"; then
	echo "Credentials already current at ${DEST}"
	exit 0
fi

cp "${SRC}" "${DEST}"
chmod 600 "${DEST}"

# The container runs as uid 1000 (node). Match it when the host uid differs so
# the agent can read the file. It cannot write it: compose mounts this file read
# only, because host and container share one refresh token and the container
# loses the rotation race, then writes back an empty record that never recovers.
chown 1000:1000 "${DEST_DIR}" "${DEST}" 2>/dev/null || true

echo "Seeded ${DEST}"
echo "Restart the container to pick it up: docker compose -f docker/docker-compose.yml up -d"
