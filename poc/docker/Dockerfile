# AI Hub Workspace POC - agent container.
# Build context is poc/ so that both agent-sidecar/ and agent-workspace/ are visible.
FROM --platform=linux/amd64 node:22-bookworm

ARG JDK_VERSION=21.0.5+11
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		ca-certificates \
		curl \
		git \
		unzip \
		zip \
	&& rm -rf /var/lib/apt/lists/*

# --- JDK 21 (Temurin) ------------------------------------------------------
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN set -eux; \
	url="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-$(echo "${JDK_VERSION}" | sed 's/+/%2B/')/OpenJDK21U-jdk_x64_linux_hotspot_$(echo "${JDK_VERSION}" | tr '+' '_').tar.gz"; \
	mkdir -p "${JAVA_HOME}"; \
	curl -fsSL "${url}" -o /tmp/jdk.tar.gz; \
	tar -xzf /tmp/jdk.tar.gz -C "${JAVA_HOME}" --strip-components=1; \
	rm /tmp/jdk.tar.gz; \
	java -version

# --- Mount points, owned by the non-root node user -------------------------
RUN mkdir -p /workspace /liferay/deploy /home/node/.claude /opt/sidecar \
	&& chown -R node:node /workspace /liferay /home/node /opt/sidecar

# --- Sidecar ---------------------------------------------------------------
WORKDIR /opt/sidecar
COPY --chown=node:node agent-sidecar/package.json agent-sidecar/package-lock.json* ./
USER node
RUN npm install --omit=dev --no-audit --no-fund
COPY --chown=node:node agent-sidecar/server.js ./server.js

# --- Seed workspace --------------------------------------------------------
# Baked into the image at /opt/seed-workspace. The compose bind mount over
# /workspace shadows this copy; the seed exists so the Gradle warm below has
# something real to resolve against, and so the image is usable without a mount.
COPY --chown=node:node agent-workspace /opt/seed-workspace
RUN cp -a /opt/seed-workspace/. /workspace/

# --- Gradle pre-warm -------------------------------------------------------
# Downloads the Gradle distribution and resolves the Liferay Workspace plugin
# plus the pinned product target platform into ~/.gradle so the first real agent
# deploy does not appear to hang. Best effort: a network hiccup at build time
# must not fail the image, so the warm is allowed to fail soft.
ENV GRADLE_USER_HOME=/home/node/.gradle
WORKDIR /opt/seed-workspace
RUN set -eux; \
	./gradlew --no-daemon --console=plain -g "${GRADLE_USER_HOME}" projects \
		|| echo "WARN: gradle pre-warm did not complete; first agent build will be slow"; \
	./gradlew --no-daemon --console=plain -g "${GRADLE_USER_HOME}" --stop || true

# --- Claude config stub ----------------------------------------------------
# Without this the SDK prints a "configuration file not found" banner on every
# turn and points at a backup inside the mounted Claude home. It is unrelated to
# credentials, but the noise sits right next to real authentication errors.
RUN echo '{}' > /home/node/.claude.json

WORKDIR /workspace
ENV AIHUB_WORKSPACE=/workspace \
	AIHUB_DEPLOY_DIR=/liferay/deploy \
	NODE_ENV=production \
	PORT=8090

EXPOSE 8090

CMD ["node", "/opt/sidecar/server.js"]
