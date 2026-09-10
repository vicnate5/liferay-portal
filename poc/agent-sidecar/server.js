// AI Hub Workspace POC - agent sidecar.
// Implements the sidecar half of poc/CONTRACT.md on port 8090. No auth.
import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { query } from "@anthropic-ai/claude-agent-sdk";

const PORT = Number(process.env.PORT || 8090);
const WORKSPACE = process.env.AIHUB_WORKSPACE || "/workspace";
const DEPLOY_DIR = process.env.AIHUB_DEPLOY_DIR || "/liferay/osgi/client-extensions";
const SDK_VERSION = JSON.parse(
	fs.readFileSync(
		new URL("./node_modules/@anthropic-ai/claude-agent-sdk/package.json", import.meta.url)
	)
).version;

const PREVIEW_MAX = 2000;
const sessions = new Map();

const log = (...a) => console.log(new Date().toISOString(), ...a);
const truncate = (s) => (s.length > PREVIEW_MAX ? s.slice(0, PREVIEW_MAX) : s);

function blocksToText(content) {
	if (typeof content === "string") return content;
	if (!Array.isArray(content)) return JSON.stringify(content ?? "");
	return content
		.map((b) => (typeof b === "string" ? b : b?.type === "text" ? b.text : JSON.stringify(b)))
		.join("\n");
}

// ---------------------------------------------------------------- session ---

function createSession(title) {
	const session = {
		id: randomUUID(),
		title: title || "Untitled session",
		createdAt: new Date().toISOString(),
		state: "idle",
		events: [],
		clients: new Set(),
		agent: null,
		inbox: [],
		waiter: null,
		closed: false,
		deploys: new Map()
	};
	sessions.set(session.id, session);
	return session;
}

function emit(session, event, data) {
	const frame = { event, data };
	session.events.push(frame);
	const payload = `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
	for (const res of session.clients) res.write(payload);
}

function setState(session, state) {
	if (session.state === state) return;
	session.state = state;
	emit(session, "status", { state });
}

// ------------------------------------------------------------ deploy hooks ---

function deployInfo(command) {
	const scoped = command.match(/:client-extensions:([A-Za-z0-9._-]+):deploy/);
	if (scoped) return scoped[1];
	const plain = command.match(/([A-Za-z0-9._-]+):deploy/);
	return plain ? plain[1] : "workspace";
}

function resolveDeployPath(artifact) {
	try {
		const hits = fs
			.readdirSync(DEPLOY_DIR)
			.filter((f) => f.startsWith(artifact))
			.map((f) => {
				const full = path.join(DEPLOY_DIR, f);
				return { full, mtime: fs.statSync(full).mtimeMs };
			})
			.sort((a, b) => b.mtime - a.mtime);
		if (hits.length) return hits[0].full;
	} catch {
		/* deploy dir may not be mounted in a dev run */
	}
	return path.join(DEPLOY_DIR, `${artifact}.zip`);
}

// ------------------------------------------------------------- agent loop ---

async function* inputStream(session) {
	while (!session.closed) {
		if (session.inbox.length) {
			yield session.inbox.shift();
			continue;
		}
		await new Promise((resolve) => {
			session.waiter = resolve;
		});
	}
}

function startAgent(session) {
	session.agent = query({
		prompt: inputStream(session),
		options: {
			cwd: WORKSPACE,
			permissionMode: "bypassPermissions",
			allowDangerouslySkipPermissions: true,
			includePartialMessages: true,
			settingSources: ["user", "project", "local"],
			skills: "all",
			tools: { type: "preset", preset: "claude_code" },
			env: { ...process.env },
			stderr: (d) => log("[sdk]", d.trimEnd())
		}
	});
	consume(session).catch((err) => {
		log("agent loop failed", err);
		emit(session, "error", { message: String(err?.message || err) });
		setState(session, "error");
	});
}

function handleToolUse(session, block) {
	emit(session, "tool_use", { id: block.id, name: block.name, input: block.input ?? {} });
	const command = block.name === "Bash" ? String(block.input?.command || "") : "";
	if (command.includes("gradlew") && command.includes("deploy")) {
		const artifact = deployInfo(command);
		session.deploys.set(block.id, artifact);
		setState(session, "deploying");
		emit(session, "deploy", { artifact, state: "started", path: "" });
	} else if (session.state !== "deploying") {
		setState(session, "working");
	}
}

function handleToolResult(session, block) {
	const ok = block.is_error !== true;
	emit(session, "tool_result", {
		id: block.tool_use_id,
		ok,
		preview: truncate(blocksToText(block.content))
	});
	const artifact = session.deploys.get(block.tool_use_id);
	if (artifact) {
		session.deploys.delete(block.tool_use_id);
		emit(session, "deploy", {
			artifact,
			state: ok ? "copied" : "failed",
			path: ok ? resolveDeployPath(artifact) : ""
		});
		setState(session, "working");
	}
}

async function consume(session) {
	for await (const msg of session.agent) {
		if (msg.type === "stream_event") {
			const ev = msg.event;
			if (ev?.type === "content_block_delta" && ev.delta?.type === "text_delta") {
				emit(session, "assistant", { text: ev.delta.text });
			}
			continue;
		}
		if (msg.type === "assistant") {
			// Text arrives as deltas above; only tool_use blocks are read here.
			for (const block of msg.message?.content ?? []) {
				if (block.type === "tool_use") handleToolUse(session, block);
			}
			continue;
		}
		if (msg.type === "user") {
			const content = msg.message?.content;
			if (Array.isArray(content)) {
				for (const block of content) {
					if (block.type === "tool_result") handleToolResult(session, block);
				}
			}
			continue;
		}
		if (msg.type === "result") {
			if (msg.is_error) emit(session, "error", { message: String(msg.result ?? "turn failed") });
			emit(session, "result", {
				ok: !msg.is_error,
				durationMs: msg.duration_ms ?? 0,
				costUsd: msg.total_cost_usd ?? 0,
				turns: msg.num_turns ?? 0
			});
			setState(session, msg.is_error ? "error" : "idle");
		}
	}
}

function submit(session, text) {
	if (!session.agent) startAgent(session);
	session.inbox.push({
		type: "user",
		message: { role: "user", content: text },
		parent_tool_use_id: null,
		session_id: session.id
	});
	if (session.waiter) {
		const resolve = session.waiter;
		session.waiter = null;
		resolve();
	}
	setState(session, "thinking");
}

// ----------------------------------------------------------------- routing ---

const json = (res, code, body) => {
	const payload = body === undefined ? "" : JSON.stringify(body);
	res.writeHead(code, {
		"content-type": "application/json",
		"content-length": Buffer.byteLength(payload)
	});
	res.end(payload);
};

function readBody(req) {
	return new Promise((resolve) => {
		let raw = "";
		req.on("data", (c) => (raw += c));
		req.on("end", () => {
			try {
				resolve(raw ? JSON.parse(raw) : {});
			} catch {
				resolve({});
			}
		});
	});
}

function openStream(session, req, res) {
	res.writeHead(200, {
		"content-type": "text/event-stream",
		"cache-control": "no-cache, no-transform",
		connection: "keep-alive",
		"x-accel-buffering": "no"
	});
	if (typeof res.flushHeaders === "function") res.flushHeaders();
	req.socket.setNoDelay(true);
	res.write(":open\n\n");
	session.clients.add(res);
	const ping = setInterval(() => res.write(":ping\n\n"), 15000);
	const close = () => {
		clearInterval(ping);
		session.clients.delete(res);
	};
	req.on("close", close);
	res.on("error", close);
}

const server = http.createServer(async (req, res) => {
	const url = new URL(req.url, "http://localhost");
	const parts = url.pathname.replace(/^\/+|\/+$/g, "").split("/");
	const method = req.method;

	if (method === "GET" && url.pathname === "/api/health") {
		return json(res, 200, {
			status: "ok",
			workspace: WORKSPACE,
			agent: "claude-agent-sdk",
			version: SDK_VERSION
		});
	}

	if (url.pathname === "/api/sessions" && method === "POST") {
		const body = await readBody(req);
		const session = createSession(body.title);
		log("session created", session.id);
		return json(res, 201, {
			sessionId: session.id,
			title: session.title,
			createdAt: session.createdAt
		});
	}

	if (url.pathname === "/api/sessions" && method === "GET") {

		// Newest first. The UI seeds its selection from the head of this list,
		// so insertion order would land a reload on the oldest session.

		return json(
			res,
			200,
			[...sessions.values()]
				.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
				.map((s) => ({
					sessionId: s.id,
					title: s.title,
					createdAt: s.createdAt,
					state: s.state
				}))
		);
	}

	// /api/sessions/{id}/{action}
	if (parts[0] === "api" && parts[1] === "sessions" && parts.length === 4) {
		const session = sessions.get(parts[2]);
		if (!session) return json(res, 404, { message: "unknown session" });
		const action = parts[3];

		if (action === "stream" && method === "GET") return openStream(session, req, res);

		if (action === "messages" && method === "GET") return json(res, 200, session.events);

		if (action === "messages" && method === "POST") {
			if (session.state !== "idle") return json(res, 409, { message: `session is ${session.state}` });
			const body = await readBody(req);
			const text = String(body.text ?? "").trim();
			if (!text) return json(res, 400, { message: "text is required" });
			submit(session, text);
			res.writeHead(202, { "content-length": 0 });
			return res.end();
		}

		if (action === "interrupt" && method === "POST") {
			try {
				if (session.agent) await session.agent.interrupt();
			} catch (err) {
				log("interrupt failed", err);
			}
			setState(session, "idle");
			res.writeHead(204);
			return res.end();
		}
	}

	return json(res, 404, { message: "not found" });
});

server.keepAliveTimeout = 0;
server.headersTimeout = 0;
server.requestTimeout = 0;
server.listen(PORT, "0.0.0.0", () =>
	log(`sidecar listening on :${PORT} workspace=${WORKSPACE} sdk=${SDK_VERSION}`)
);
