(function () {
  const root = document.querySelector("[data-room-page]");
  if (!root) {
    return;
  }

  const roomId = root.dataset.roomId;
  const currentUserId = Number(root.dataset.userId);
  const currentUsername = root.dataset.username || "";
  const player1 = document.getElementById("player1");
  const player2 = document.getElementById("player2");
  const roomStatus = document.getElementById("roomStatus");
  const roomTitle = document.getElementById("roomTitle");
  const roomHost = document.getElementById("roomHost");
  const roomCode = document.getElementById("roomCode");
  const roomPlayers = document.getElementById("roomPlayers");
  const roomHint = document.getElementById("roomHint");
  const startButton = document.getElementById("startButton");
  const chatFeed = document.getElementById("chatMessages");
  const chatInput = document.getElementById("chatInput");
  const chatStatus = document.getElementById("chatStatus");
  const renderedIds = new Set();
  let stompClient = null;

  function renderPlayerCard(element, user, symbol, role, isCurrentUser) {
    if (!element) return;
    const waiting = !user;
    const badge = waiting
      ? '<span class="badge badge-waiting">Đang chờ</span>'
      : `<span class="badge ${isCurrentUser ? "badge-ready" : "badge-online"}">${isCurrentUser ? "Bạn" : "Đã vào phòng"}</span>`;

    element.innerHTML = `
      <div class="player-top">
        <div>
          <p class="eyebrow">${role}</p>
          <h3 class="player-name">${waiting ? "Đang chờ người chơi" : window.CaroApp.escapeHtml(user.username)}</h3>
        </div>
        <div class="player-symbol ${waiting ? "empty" : "symbol-" + symbol.toLowerCase()}">${symbol}</div>
      </div>
      <div class="stack">
        ${badge}
        <p class="player-note">${waiting ? "Phòng đang chờ người chơi thứ hai tham gia." : "Sẵn sàng bước vào trận đấu Caro trực tuyến."}</p>
      </div>
    `;
  }

  function updateRoomSummary(room) {
    if (!room) return;
    const status = room.status || (room.player2 ? "full" : "waiting");
    if (roomTitle) roomTitle.textContent = "Room #" + room.id;
    if (roomHost) roomHost.textContent = room.host ? room.host.username : "--";
    if (roomCode) roomCode.textContent = "RM-" + String(room.id).padStart(4, "0");
    if (roomPlayers) roomPlayers.textContent = room.player2 ? "2 / 2" : "1 / 2";
    if (roomStatus) {
      roomStatus.className = "badge " + window.CaroApp.statusClass(status);
      roomStatus.textContent = window.CaroApp.statusLabel(status);
    }

    const ready = !!room.player2;
    if (roomHint) {
      roomHint.textContent = ready
        ? "Đội hình đã đủ. Host có thể bắt đầu trận bất cứ lúc nào."
        : "Chờ người chơi thứ hai tham gia để mở nút bắt đầu.";
    }

    if (startButton) {
      startButton.disabled = !ready || !room.host || room.host.username !== currentUsername;
    }

    renderPlayerCard(player1, room.host, "X", "Host", room.host && room.host.username === currentUsername);
    renderPlayerCard(player2, room.player2, "O", "Người chơi 2", room.player2 && room.player2.username === currentUsername);
  }

  async function loadRoom() {
    const res = await fetch("/api/rooms/" + roomId + "?t=" + Date.now(), { cache: "no-store" });
    if (!res.ok) {
      throw new Error("room load failed");
    }
    updateRoomSummary(await res.json());
  }

  function renderChatMessage(message) {
    if (!message || !message.id || renderedIds.has(message.id) || !chatFeed) {
      return;
    }
    renderedIds.add(message.id);

    const own = message.senderUsername === currentUsername;
    const line = document.createElement("div");
    line.className =
      "chat-line" +
      (message.type === "SYSTEM" ? " system" : own ? " own" : " other");

    if (message.type === "SYSTEM") {
      line.innerHTML = `
        <span class="badge badge-waiting">${window.CaroApp.escapeHtml(message.content || "Thông báo hệ thống")}</span>
      `;
    } else {
      line.innerHTML = `
        <div class="chat-meta ${own ? "own" : "other"}">
          <span class="chat-author">${window.CaroApp.escapeHtml(message.senderUsername || "Unknown")}</span>
          · ${window.CaroApp.formatDate(message.createdAt)}
        </div>
        <div class="chat-bubble ${own ? "own-message" : "other-message"}">${window.CaroApp.escapeHtml(message.content || "")}</div>
      `;
    }

    chatFeed.appendChild(line);
    chatFeed.scrollTop = chatFeed.scrollHeight;
  }

  async function loadMessages() {
    const res = await fetch("/api/rooms/" + roomId + "/messages");
    if (!res.ok) {
      throw new Error("chat load failed");
    }
    const messages = await res.json();
    messages.forEach(renderChatMessage);
  }

  async function sendChatMessage() {
    const content = (chatInput?.value || "").trim();
    if (!content) return;

    const res = await fetch("/api/rooms/" + roomId + "/messages", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ senderId: currentUserId, content }),
    });

    if (!res.ok) {
      throw new Error("send message failed");
    }

    renderChatMessage(await res.json());
    chatInput.value = "";
  }

  function connectWebSocket() {
    const socket = new SockJS("/ws");
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
      if (chatStatus) {
        chatStatus.textContent = "Realtime connected";
      }

      stompClient.subscribe("/topic/room/" + roomId, function (response) {
        const message = JSON.parse(response.body);
        if (message.type === "START") {
          window.location.href = "/game?roomId=" + roomId;
          return;
        }
        loadRoom().catch(console.error);
      });

      stompClient.subscribe("/topic/rooms/" + roomId + "/chat", function (frame) {
        renderChatMessage(JSON.parse(frame.body));
      });
    }, function (error) {
      console.error(error);
      if (chatStatus) {
        chatStatus.textContent = "Realtime disconnected";
      }
    });
  }

  async function startGame() {
    const res = await fetch("/api/rooms/start/" + roomId, { method: "POST" });
    if (!res.ok) {
      alert("Chưa thể bắt đầu trận đấu.");
    }
  }

  async function leaveRoom() {
    const res = await fetch("/api/rooms/leave/" + roomId, { method: "POST" });
    if (res.ok) {
      window.location.href = "/home";
      return;
    }
    alert("Không thể rời phòng lúc này.");
  }

  if (chatInput) {
    chatInput.addEventListener("keydown", function (event) {
      if (event.key === "Enter") {
        event.preventDefault();
        sendChatMessage().catch(function (error) {
          console.error(error);
        });
      }
    });
  }

  const sendButton = document.getElementById("sendChatButton");
  if (sendButton) {
    sendButton.addEventListener("click", function () {
      sendChatMessage().catch(function (error) {
        console.error(error);
      });
    });
  }

  if (startButton) {
    startButton.addEventListener("click", startGame);
  }

  const leaveButton = document.getElementById("leaveButton");
  if (leaveButton) {
    leaveButton.addEventListener("click", leaveRoom);
  }

  loadRoom().catch(console.error);
  loadMessages().catch(console.error);
  connectWebSocket();
  setInterval(function () {
    loadRoom().catch(console.error);
    loadMessages().catch(console.error);
  }, 4000);
})();
