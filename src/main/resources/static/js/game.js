(function () {
  const root = document.querySelector("[data-game-page]");
  if (!root) {
    return;
  }

  const roomId = Number(root.dataset.roomId);
  const currentUsername = root.dataset.username || "guest";
  const currentUserId = Number(root.dataset.userId || 0);
  const playerSymbol = root.dataset.playerSymbol || "";
  const hostUsername = root.dataset.hostUsername || "host";
  const playerTwoUsername = root.dataset.playerTwoUsername || "player2";
  const roomStatus = root.dataset.roomStatus || "playing";
  const renderedIds = new Set();
  const BOARD_SIZE = 20;
  const UNDO_COST = 2;
  const SUGGEST_COST = 3;
  let board = createEmptyBoard();
  let currentTurn = root.dataset.currentTurn || "X";
  let winner = "";
  let winnerSymbol = "";
  let lastMove = null;
  let suggestedMove = null;
  let stompClient = null;
  let opponentConnected = root.dataset.opponentConnected !== "false";
  let shouldAutoDisconnect = true;
  let disconnectSent = false;
  let supportActionInFlight = false;
  let pendingSupportAction = null;
  let currentSupportPoints = 0;
  let localTypingActive = false;
  let typingSendTimeout = null;
  let remoteTypingTimeout = null;

  const boardElement = document.getElementById("board");
  const gameSummary = document.getElementById("gameSummary");
  const turnTitle = document.getElementById("turnTitle");
  const turnCopy = document.getElementById("turnCopy");
  const chatFeed = document.getElementById("chatMessages");
  const chatInput = document.getElementById("chatInput");
  const chatStatus = document.getElementById("chatStatus");
  const chatTypingIndicator = document.getElementById("chatTypingIndicator");
  const winnerModal = document.getElementById("winnerModal");
  const winnerMessage = document.getElementById("winnerMessage");
  const replayRequestModal = document.getElementById("replayRequestModal");
  const replayRequestMessage = document.getElementById("replayRequestMessage");
  const waitingReplayModal = document.getElementById("waitingReplayModal");
  const waitingReplayMessage = document.getElementById("waitingReplayMessage");
  const disconnectModal = document.getElementById("disconnectModal");
  const disconnectMessage = document.getElementById("disconnectMessage");
  const hostSupportPoints = document.getElementById("hostSupportPoints");
  const playerTwoSupportPoints = document.getElementById("playerTwoSupportPoints");
  const currentSupportPointsElement = document.getElementById("currentSupportPoints");
  const supportFeedback = document.getElementById("supportFeedback");
  const undoSupportButton = document.getElementById("undoSupportButton");
  const suggestSupportButton = document.getElementById("suggestSupportButton");
  const supportConfirmModal = document.getElementById("supportConfirmModal");
  const supportConfirmTitle = document.getElementById("supportConfirmTitle");
  const supportConfirmMessage = document.getElementById("supportConfirmMessage");
  const cancelSupportConfirmButton = document.getElementById("cancelSupportConfirmButton");
  const confirmSupportConfirmButton = document.getElementById("confirmSupportConfirmButton");

  function createEmptyBoard() {
    return Array.from({ length: BOARD_SIZE }, () => Array(BOARD_SIZE).fill(null));
  }

  function getOwnSupportPointsElement() {
    return currentUsername === hostUsername ? hostSupportPoints : playerTwoSupportPoints;
  }

  function setCurrentSupportPoints(value) {
    const numeric = Number(value);
    currentSupportPoints = Number.isNaN(numeric) ? 0 : numeric;

    const formatted = window.CaroApp.formatPoints(currentSupportPoints);
    const ownPointsElement = getOwnSupportPointsElement();
    if (ownPointsElement) {
      ownPointsElement.textContent = formatted;
    }
    if (currentSupportPointsElement) {
      currentSupportPointsElement.textContent = formatted;
    }

    updateSupportButtons();
  }

  function hasAnyMoves() {
    return board.some((row) => row.some(Boolean));
  }

  function hasAnyEmptyCell() {
    return board.some((row) => row.some((cell) => cell == null));
  }

  function clearSuggestionHighlight() {
    suggestedMove = null;
  }


  function showTypingIndicator(senderUsername) {
    if (!chatTypingIndicator || !senderUsername || senderUsername === currentUsername) {
      return;
    }

    chatTypingIndicator.hidden = false;
    chatTypingIndicator.textContent = senderUsername + " đang nhập";

    clearTimeout(remoteTypingTimeout);
    remoteTypingTimeout = setTimeout(hideTypingIndicator, 1600);
  }

  function hideTypingIndicator() {
    if (!chatTypingIndicator) {
      return;
    }

    chatTypingIndicator.hidden = true;
    chatTypingIndicator.textContent = "";
  }

  function sendTypingStatus(isTyping) {
    if (!stompClient || !stompClient.connected) {
      return;
    }

    localTypingActive = isTyping;
    stompClient.send(
      "/app/rooms/" + roomId + "/chat.typing",
      {},
      JSON.stringify({ typing: isTyping })
    );
  }

  function handleTypingInput() {
    const hasText = !!(chatInput?.value || "").trim();

    if (!hasText) {
      if (localTypingActive) {
        sendTypingStatus(false);
      }
      clearTimeout(typingSendTimeout);
      return;
    }

    if (!localTypingActive) {
      sendTypingStatus(true);
    }

    clearTimeout(typingSendTimeout);
    typingSendTimeout = setTimeout(function () {
      if (localTypingActive) {
        sendTypingStatus(false);
      }
    }, 1200);
  }
  function renderBoard() {
    if (!boardElement) return;
    boardElement.innerHTML = "";

    for (let i = 0; i < BOARD_SIZE; i++) {
      for (let j = 0; j < BOARD_SIZE; j++) {
        const value = board[i][j];
        const cell = document.createElement("button");
        cell.type = "button";
        cell.className = "cell";

        if (value === "X") {
          cell.classList.add("symbol-x");
          cell.textContent = "X";
        } else if (value === "O") {
          cell.classList.add("symbol-o");
          cell.textContent = "O";
        } else if (suggestedMove && suggestedMove.x === i && suggestedMove.y === j) {
          cell.classList.add("suggested-move");
          cell.setAttribute("aria-label", "Gợi ý nước đi");
        }

        if (lastMove && lastMove.x === i && lastMove.y === j) {
          cell.classList.add("last-move");
        }

        cell.addEventListener("click", function () {
          sendMove(i, j);
        });
        boardElement.appendChild(cell);
      }
    }
  }

  function updateSupportButtons() {
    const undoDisabled =
      supportActionInFlight ||
      currentSupportPoints < UNDO_COST ||
      !!winner ||
      !opponentConnected ||
      !hasAnyMoves();

    const suggestDisabled =
      supportActionInFlight ||
      currentSupportPoints < SUGGEST_COST ||
      !!winner ||
      !opponentConnected ||
      playerSymbol !== currentTurn ||
      !hasAnyEmptyCell();

    if (undoSupportButton) {
      undoSupportButton.disabled = undoDisabled;
      undoSupportButton.textContent = supportActionInFlight && pendingSupportAction?.type === "undo" ? "Đang xử lý..." : "Sử dụng";
    }

    if (suggestSupportButton) {
      suggestSupportButton.disabled = suggestDisabled;
      suggestSupportButton.textContent = supportActionInFlight && pendingSupportAction?.type === "suggestion" ? "Đang xử lý..." : "Sử dụng";
    }
  }

  function showSupportFeedback(message, type) {
    if (!supportFeedback) {
      return;
    }

    if (!message) {
      supportFeedback.hidden = true;
      supportFeedback.textContent = "";
      supportFeedback.className = "support-feedback";
      return;
    }

    supportFeedback.hidden = false;
    supportFeedback.textContent = message;
    supportFeedback.className = "support-feedback support-feedback--" + (type || "info");
  }

  function updateStatusPanel() {
    if (!turnTitle || !turnCopy) return;

    if (gameSummary) {
      gameSummary.classList.remove("state-your-turn", "state-opponent-turn", "state-finished", "state-paused");
    }

    if (winner) {
      if (gameSummary) {
        gameSummary.classList.add("state-finished");
      }
      turnTitle.textContent = "Trận đấu đã kết thúc";
      turnCopy.textContent = winner === currentUsername
        ? "Bạn vừa giành chiến thắng. Có thể dùng chơi lại để bước vào ván mới."
        : winner + " đã giành chiến thắng. Bạn vẫn có thể dùng trợ giúp ở ván kế tiếp.";
      updateSupportButtons();
      return;
    }

    if (!opponentConnected) {
      if (gameSummary) {
        gameSummary.classList.add("state-paused");
      }
      turnTitle.textContent = "Đối thủ tạm rời trận";
      turnCopy.textContent = "Bạn có thể chờ họ quay lại hoặc rời trận. Bàn cờ hiện tại vẫn được giữ nguyên.";
      updateSupportButtons();
      return;
    }

    const yours = playerSymbol === currentTurn;
    if (gameSummary) {
      gameSummary.classList.add(yours ? "state-your-turn" : "state-opponent-turn");
    }

    turnTitle.textContent = yours ? "Đến lượt của bạn" : "Đến lượt đối thủ";
    turnCopy.textContent =
      "Ký hiệu của bạn là " +
      playerSymbol +
      (yours ? ". Bạn có thể đánh trực tiếp hoặc dùng trợ giúp nếu cần." : ". Chờ đối thủ hoàn thành lượt trước khi hành động.");
    updateSupportButtons();
  }

  function getOpponentUsername() {
    return currentUsername === hostUsername ? playerTwoUsername : hostUsername;
  }

  function showModal(element) {
    if (element) {
      element.classList.add("show");
    }
  }

  function hideModal(element) {
    if (element) {
      element.classList.remove("show");
    }
  }

  function hideAllModals() {
    hideModal(winnerModal);
    hideModal(replayRequestModal);
    hideModal(waitingReplayModal);
    hideModal(disconnectModal);
    hideModal(supportConfirmModal);
  }

  function showWinnerModal(winnerUsername) {
    if (winnerMessage) {
      const win = winnerUsername === currentUsername;
      winnerMessage.textContent = win ? "Chiến thắng" : "Thua";
      winnerMessage.className = "result-modal-text " + (win ? "result-win" : "result-lose");
    }
    showModal(winnerModal);
  }

  function showReplayRequestModal(requesterUsername) {
    if (replayRequestMessage) {
      replayRequestMessage.textContent =
        requesterUsername + " muốn đấu lại ngay bây giờ. Bạn có đồng ý không?";
    }
    showModal(replayRequestModal);
  }

  function showWaitingReplayModal(message) {
    if (waitingReplayMessage) {
      waitingReplayMessage.textContent = message;
    }
    showModal(waitingReplayModal);
  }

  function showDisconnectModal(senderUsername) {
    if (disconnectMessage) {
      disconnectMessage.textContent =
        senderUsername + " vừa tạm rời khỏi màn hình thi đấu. Bạn muốn chờ họ quay lại hay rời trận luôn?";
    }
    showModal(disconnectModal);
  }

  function openSupportConfirm(type) {
    const supportMap = {
      undo: {
        type: "undo",
        title: "Xác nhận quay lại nước đi?",
        message: "Bạn sẽ mất 2 sao để hoàn tác 2 nước gần nhất.",
        endpoint: "/api/games/" + roomId + "/support/undo",
      },
      suggestion: {
        type: "suggestion",
        title: "Xác nhận dùng gợi ý nước đi?",
        message: "Bạn sẽ mất 3 sao để nhận gợi ý nước đi.",
        endpoint: "/api/games/" + roomId + "/support/suggestion",
      },
    };

    pendingSupportAction = supportMap[type] || null;
    if (!pendingSupportAction) {
      return;
    }

    if (supportConfirmTitle) {
      supportConfirmTitle.textContent = pendingSupportAction.title;
    }
    if (supportConfirmMessage) {
      supportConfirmMessage.textContent = pendingSupportAction.message;
    }

    showModal(supportConfirmModal);
  }

  function applySnapshot(snapshot) {
    if (!snapshot) {
      return;
    }

    board = Array.isArray(snapshot.board)
      ? snapshot.board.map((row) => Array.isArray(row) ? row.slice() : Array(BOARD_SIZE).fill(null))
      : createEmptyBoard();
    currentTurn = snapshot.currentTurn || currentTurn;
    winner = snapshot.winner || "";
    lastMove = snapshot.lastMoveX != null && snapshot.lastMoveY != null
      ? { x: snapshot.lastMoveX, y: snapshot.lastMoveY }
      : null;
    opponentConnected = snapshot.opponentConnected !== false;
    clearSuggestionHighlight();
    renderBoard();
    updateStatusPanel();
  }

  function updatePlayerPoints(room) {
    if (!room) {
      return;
    }

    if (hostSupportPoints && room.host) {
      hostSupportPoints.textContent = window.CaroApp.formatPoints(room.host.supportPoints ?? 5);
    }

    if (playerTwoSupportPoints && room.player2) {
      playerTwoSupportPoints.textContent = window.CaroApp.formatPoints(room.player2.supportPoints ?? 5);
    }

    if (currentUsername === hostUsername && room.host) {
      setCurrentSupportPoints(room.host.supportPoints ?? 5);
      return;
    }

    if (currentUsername === playerTwoUsername && room.player2) {
      setCurrentSupportPoints(room.player2.supportPoints ?? 5);
    }
  }

  async function loadPlayerPoints() {
    const res = await fetch("/api/rooms/" + roomId + "?t=" + Date.now(), { cache: "no-store" });
    if (!res.ok) {
      throw new Error("room points load failed");
    }

    updatePlayerPoints(await res.json());
  }

  function resetGame(nextTurn) {
    board = createEmptyBoard();
    currentTurn = nextTurn || "X";
    winner = "";
    winnerSymbol = "";
    lastMove = null;
    opponentConnected = true;
    clearSuggestionHighlight();
    hideAllModals();
    renderBoard();
    updateStatusPanel();
    loadPlayerPoints().catch(console.error);
  }

  function handleSocketMessage(data) {
    if (!data || !data.type) {
      return;
    }

    if (data.type === "MOVE" || data.type === "WIN") {
      if (data.x == null || data.y == null || !data.player) {
        return;
      }

      clearSuggestionHighlight();
      board[data.x][data.y] = data.player;
      currentTurn = data.currentTurn || currentTurn;
      lastMove = { x: data.x, y: data.y };
      if (data.type === "WIN") {
        winner = data.winner || "";
        winnerSymbol = data.player;
        loadPlayerPoints().catch(console.error);
        showWinnerModal(winner);
      }
      renderBoard();
      updateStatusPanel();
      return;
    }

    if (data.type === "UNDO") {
      board = Array.isArray(data.board)
        ? data.board.map((row) => Array.isArray(row) ? row.slice() : Array(BOARD_SIZE).fill(null))
        : createEmptyBoard();
      currentTurn = data.currentTurn || currentTurn;
      winner = "";
      winnerSymbol = "";
      lastMove = data.lastMoveX != null && data.lastMoveY != null
        ? { x: data.lastMoveX, y: data.lastMoveY }
        : null;
      clearSuggestionHighlight();
      renderBoard();
      updateStatusPanel();
      loadPlayerPoints().catch(console.error);
      showSupportFeedback(
        data.sender === currentUsername
          ? "Đã hoàn tác 2 nước gần nhất."
          : data.sender + " vừa hoàn tác 2 nước gần nhất.",
        data.sender === currentUsername ? "success" : "info"
      );
      return;
    }

    if (data.type === "SUPPORT_POINTS_UPDATED") {
      loadPlayerPoints().catch(console.error);
      if (data.sender !== currentUsername && data.content === "suggestion") {
        showSupportFeedback(data.sender + " vừa dùng gợi ý nước đi.", "info");
      }
      return;
    }

    if (data.type === "PLAYER_DISCONNECTED" && data.sender !== currentUsername) {
      opponentConnected = false;
      updateStatusPanel();
      showDisconnectModal(data.sender);
      return;
    }

    if (data.type === "PLAYER_RECONNECTED" && data.sender !== currentUsername) {
      opponentConnected = true;
      hideModal(disconnectModal);
      updateStatusPanel();
      return;
    }

    if (data.type === "REPLAY_REQUEST") {
      hideModal(winnerModal);
      if (data.sender === currentUsername) {
        showWaitingReplayModal("Đang chờ " + getOpponentUsername() + " xác nhận yêu cầu chơi lại...");
      } else {
        showReplayRequestModal(data.sender);
      }
      return;
    }

    if (data.type === "REPLAY_ACCEPTED") {
      resetGame(data.currentTurn || "X");
      return;
    }

    if (data.type === "REPLAY_DECLINED") {
      hideModal(waitingReplayModal);
      hideModal(replayRequestModal);
      if (winner && winnerSymbol) {
        showWinnerModal(winner);
      }
      return;
    }

    if (data.type === "PLAYER_LEFT" && data.sender !== currentUsername) {
      shouldAutoDisconnect = false;
      disconnectSent = true;
      window.location.href = "/room?roomId=" + roomId;
    }
  }

  function sendDisconnectBeacon() {
    if (!shouldAutoDisconnect || disconnectSent) {
      return;
    }

    disconnectSent = true;
    const url = "/api/games/" + roomId + "/disconnect";

    if (navigator.sendBeacon) {
      navigator.sendBeacon(url, new Blob([], { type: "text/plain" }));
      return;
    }

    fetch(url, {
      method: "POST",
      keepalive: true,
    }).catch(function () {
      // Ignore unload errors.
    });
  }

  function connectWebSocket() {
    const socket = new SockJS("/ws");
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
      if (chatStatus) {
        chatStatus.textContent = "Realtime connected";
      }

      stompClient.subscribe("/topic/game/" + roomId, function (frame) {
        handleSocketMessage(JSON.parse(frame.body));
      });

      stompClient.subscribe("/topic/rooms/" + roomId + "/chat", function (frame) {
        renderChatMessage(JSON.parse(frame.body));
      });

      stompClient.subscribe("/topic/rooms/" + roomId + "/chat.typing", function (frame) {
        const payload = JSON.parse(frame.body);
        if (!payload || payload.senderUsername === currentUsername) {
          return;
        }

        if (payload.typing) {
          showTypingIndicator(payload.senderUsername);
        } else {
          hideTypingIndicator();
        }
      });
    }, function (error) {
      console.error(error);
      if (chatStatus) {
        chatStatus.textContent = "Realtime disconnected";
      }
    });
  }

  function sendMove(x, y) {
    if (!stompClient || !stompClient.connected) return;
    if (winner || !opponentConnected) return;
    if (playerSymbol !== currentTurn) return;
    if (board[x][y]) return;

    clearSuggestionHighlight();
    renderBoard();

    stompClient.send(
      "/app/game.move/" + roomId,
      {},
      JSON.stringify({
        roomId: String(roomId),
        x,
        y,
        sender: currentUsername,
      })
    );
  }

  async function loadGameState() {
    const res = await fetch("/api/games/" + roomId + "/state", { cache: "no-store" });
    if (!res.ok) {
      throw new Error("game state load failed");
    }

    applySnapshot(await res.json());
  }

  async function loadChatMessages() {
    const res = await fetch("/api/rooms/" + roomId + "/messages");
    if (!res.ok) {
      throw new Error("chat load failed");
    }
    const messages = await res.json();
    messages.forEach(renderChatMessage);
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
      line.innerHTML = `<span class="badge badge-waiting">${window.CaroApp.escapeHtml(message.content || "Thông báo hệ thống")}</span>`;
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

    if (message.senderUsername && message.senderUsername !== currentUsername) {
      hideTypingIndicator();
    }
  }

  async function sendChatMessage() {
    const content = (chatInput?.value || "").trim();
    if (!content) return;

    const res = await fetch("/api/rooms/" + roomId + "/messages", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        senderId: currentUserId,
        content,
      }),
    });

    if (!res.ok) {
      throw new Error("chat send failed");
    }

    renderChatMessage(await res.json());
    chatInput.value = "";
    if (localTypingActive) {
      sendTypingStatus(false);
    }
    clearTimeout(typingSendTimeout);
  }

  function requestReplay() {
    if (!stompClient || !stompClient.connected || !opponentConnected) return;
    hideModal(winnerModal);
    showWaitingReplayModal("Đang chờ " + getOpponentUsername() + " xác nhận yêu cầu chơi lại...");
    stompClient.send("/app/game.replay.request/" + roomId, {}, "{}");
  }

  function acceptReplay() {
    if (!stompClient || !stompClient.connected) return;
    stompClient.send("/app/game.replay.accept/" + roomId, {}, "{}");
  }

  function declineReplay() {
    if (!stompClient || !stompClient.connected) return;
    hideModal(replayRequestModal);
    if (winner && winnerSymbol) {
      showWinnerModal(winner);
    }
    stompClient.send("/app/game.replay.decline/" + roomId, {}, "{}");
  }

  function waitForReconnect() {
    hideModal(disconnectModal);
    updateStatusPanel();
  }

  async function leaveRoom() {
    shouldAutoDisconnect = false;
    disconnectSent = true;

    const res = await fetch("/api/rooms/leave/" + roomId, { method: "POST" });
    if (res.ok) {
      window.location.href = "/home";
      return;
    }

    shouldAutoDisconnect = true;
    disconnectSent = false;
    showSupportFeedback("Không thể rời trận đấu lúc này.", "error");
  }

  async function runSupportAction() {
    if (!pendingSupportAction || supportActionInFlight) {
      return;
    }

    supportActionInFlight = true;
    hideModal(supportConfirmModal);
    updateSupportButtons();

    try {
      const res = await fetch(pendingSupportAction.endpoint, { method: "POST" });
      let payload = {};
      try {
        payload = await res.json();
      } catch (error) {
        payload = {};
      }

      if (!res.ok) {
        throw new Error(payload.message || "Hiện tại chưa thể dùng trợ giúp này.");
      }

      if (typeof payload.supportPoints !== "undefined") {
        setCurrentSupportPoints(payload.supportPoints);
      }

      if (pendingSupportAction.type === "suggestion") {
        suggestedMove = payload.suggestedX != null && payload.suggestedY != null
          ? { x: payload.suggestedX, y: payload.suggestedY }
          : null;
        renderBoard();
      }

      showSupportFeedback(payload.message || "Đã dùng trợ giúp thành công.", "success");
    } catch (error) {
      showSupportFeedback(error.message || "Có lỗi xảy ra khi dùng trợ giúp.", "error");
    } finally {
      supportActionInFlight = false;
      pendingSupportAction = null;
      updateSupportButtons();
    }
  }

  const replayButton = document.getElementById("requestReplayButton");
  if (replayButton) replayButton.addEventListener("click", requestReplay);

  const leaveButton = document.getElementById("leaveGameButton");
  if (leaveButton) leaveButton.addEventListener("click", leaveRoom);

  const acceptButton = document.getElementById("acceptReplayButton");
  if (acceptButton) acceptButton.addEventListener("click", acceptReplay);

  const declineButton = document.getElementById("declineReplayButton");
  if (declineButton) declineButton.addEventListener("click", declineReplay);

  const winnerReplayButton = document.getElementById("winnerReplayButton");
  if (winnerReplayButton) winnerReplayButton.addEventListener("click", requestReplay);

  const winnerLeaveButton = document.getElementById("winnerLeaveButton");
  if (winnerLeaveButton) winnerLeaveButton.addEventListener("click", leaveRoom);

  const waitButton = document.getElementById("waitForReconnectButton");
  if (waitButton) waitButton.addEventListener("click", waitForReconnect);

  const leaveAfterDisconnectButton = document.getElementById("leaveAfterDisconnectButton");
  if (leaveAfterDisconnectButton) leaveAfterDisconnectButton.addEventListener("click", leaveRoom);

  if (undoSupportButton) {
    undoSupportButton.addEventListener("click", function () {
      openSupportConfirm("undo");
    });
  }

  if (suggestSupportButton) {
    suggestSupportButton.addEventListener("click", function () {
      openSupportConfirm("suggestion");
    });
  }

  if (cancelSupportConfirmButton) {
    cancelSupportConfirmButton.addEventListener("click", function () {
      pendingSupportAction = null;
      hideModal(supportConfirmModal);
      updateSupportButtons();
    });
  }

  if (confirmSupportConfirmButton) {
    confirmSupportConfirmButton.addEventListener("click", function () {
      runSupportAction().catch(console.error);
    });
  }

  const sendButton = document.getElementById("sendChatButton");
  if (sendButton) {
    sendButton.addEventListener("click", function () {
      sendChatMessage().catch(console.error);
    });
  }

  if (chatInput) {
    chatInput.addEventListener("input", handleTypingInput);
    chatInput.addEventListener("blur", function () {
      if (localTypingActive) {
        sendTypingStatus(false);
      }
      clearTimeout(typingSendTimeout);
    });
    chatInput.addEventListener("keydown", function (event) {
      if (event.key === "Enter") {
        event.preventDefault();
        sendChatMessage().catch(console.error);
      }
    });
  }

  window.addEventListener("pagehide", sendDisconnectBeacon);

  setCurrentSupportPoints(Number(getOwnSupportPointsElement()?.textContent || 0));
  connectWebSocket();
  renderBoard();
  updateStatusPanel();
  loadGameState().catch(console.error);
  loadPlayerPoints().catch(console.error);
  loadChatMessages().catch(console.error);
  setInterval(function () {
    loadChatMessages().catch(console.error);
  }, 4000);
})();










