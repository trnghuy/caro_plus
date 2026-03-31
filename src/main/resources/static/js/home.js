(function () {
  const root = document.querySelector("[data-home-page]");
  if (!root) {
    return;
  }

  const joinModal = document.getElementById("joinModal");
  const roomList = document.getElementById("roomList");
  const rankList = document.getElementById("rankList");
  const roomInput = document.getElementById("roomInput");
  const joinError = document.getElementById("joinError");
  const currentUsername = root.dataset.username || "";

  function openJoinModal() {
    if (!joinModal) return;
    joinModal.classList.add("show");
    if (roomInput) roomInput.focus();
  }

  function closeJoinModal() {
    if (!joinModal) return;
    joinModal.classList.remove("show");
    if (joinError) joinError.textContent = "";
  }

  async function createRoom() {
    const res = await fetch("/api/rooms/create", { method: "POST" });
    if (!res.ok) {
      throw new Error("Không tạo được phòng.");
    }
    const room = await res.json();
    window.location.href = "/room?roomId=" + room.id;
  }

  async function joinRoom(roomId) {
    const normalized = String(roomId || "").trim();
    if (!normalized) {
      if (joinError) joinError.textContent = "Vui lòng nhập mã phòng.";
      return;
    }

    const res = await fetch("/api/rooms/join/" + normalized, { method: "POST" });
    if (!res.ok) {
      const message = res.status === 404 ? "Không tìm thấy phòng." : "Không thể tham gia phòng này.";
      if (joinError) joinError.textContent = message;
      return;
    }

    window.location.href = "/room?roomId=" + normalized;
  }

  function roomStatus(room) {
    if (room.status) {
      return room.status;
    }
    return room.player2 ? "full" : "waiting";
  }

  function roomPlayerCount(room) {
    return (room.host ? 1 : 0) + (room.player2 ? 1 : 0);
  }

  function roomCount(room) {
    return roomPlayerCount(room) + "/2";
  }

  function isCurrentUserMember(room) {
    return !!(
      (room.host && room.host.username === currentUsername) ||
      (room.player2 && room.player2.username === currentUsername)
    );
  }

  function renderRooms(rooms) {
    if (!roomList) return;
    if (!Array.isArray(rooms) || rooms.length === 0) {
      roomList.innerHTML = '<div class="empty-state">Chưa có phòng nào. Hãy tạo phòng đầu tiên để bắt đầu trận đấu.</div>';
      return;
    }

    roomList.innerHTML = rooms
      .map((room) => {
        const status = roomStatus(room);
        const isMember = isCurrentUserMember(room);
        const joinDisabled = (status === "playing" || status === "full") && !isMember;
        const hostName = room.host ? room.host.username : "Chưa có host";
        const title = room.host
          ? "Phòng của " + window.CaroApp.escapeHtml(hostName)
          : "Phòng trống";
        const actionLabel = status === "playing" && isMember ? "Tiếp tục" : (joinDisabled ? "Không thể vào" : "Tham gia");
        const actionClass = status === "playing" && isMember ? "btn btn-secondary" : "btn btn-primary";
        const actionHandler = isMember
          ? "window.location.href='/room?roomId=" + room.id + "'"
          : "window.CaroHome.joinRoom('" + room.id + "')";

        return `
          <article class="room-card">
            <div>
              <div class="room-card-head">
                <div>
                  <p class="room-id">Room #${room.id}</p>
                  <h3 class="room-card-title">${title}</h3>
                </div>
                <span class="badge ${window.CaroApp.statusClass(status)}">${window.CaroApp.statusLabel(status)}</span>
              </div>
              <div class="room-card-meta">
                <span class="chip">Host: ${window.CaroApp.escapeHtml(hostName)}</span>
                <span class="chip">Người chơi: ${roomCount(room)}</span>
                <span class="chip">Tạo lúc: ${window.CaroApp.formatDate(room.createdAt) || "Vừa xong"}</span>
              </div>
            </div>
            <div class="room-card-actions">
              <button class="btn btn-secondary" type="button" onclick="window.location.href='/room?roomId=${room.id}'">Xem phòng</button>
              <button class="${actionClass}" type="button" ${joinDisabled ? "disabled" : ""} onclick="${actionHandler}">
                ${actionLabel}
              </button>
            </div>
          </article>
        `;
      })
      .join("");
  }

  function renderRank(users) {
    if (!rankList) return;
    if (!Array.isArray(users) || users.length === 0) {
      rankList.innerHTML = '<div class="empty-state">Chưa có dữ liệu xếp hạng để hiển thị.</div>';
      return;
    }

    rankList.innerHTML = users
      .map((user, index) => {
        const current = currentUsername && user.username === currentUsername;
        return `
          <article class="rank-item ${current ? "current-user" : ""}">
            <div class="rank-position">#${index + 1}</div>
            <div>
              <p class="rank-name">${window.CaroApp.escapeHtml(user.username)}</p>
              <div class="rank-stats">
                <span class="rank-support">
                  <span class="star-points" aria-hidden="true">★</span>
                  ${window.CaroApp.formatPoints(user.supportPoints ?? 5)}
                </span>
                · Thắng ${user.win}
                · Thua ${user.lose}
                · Hòa ${user.draw}
              </div>
            </div>
            <div class="kpi">
              <span class="kpi-value">${user.rankScore ?? 0}</span>
              <span class="kpi-label">Điểm</span>
            </div>
          </article>
        `;
      })
      .join("");
  }

  async function loadRooms() {
    try {
      const res = await fetch("/api/rooms");
      if (!res.ok) {
        throw new Error("room list error");
      }
      renderRooms(await res.json());
    } catch (error) {
      if (roomList) {
        roomList.innerHTML = '<div class="empty-state">Không tải được danh sách phòng. Hãy thử lại sau.</div>';
      }
      console.error(error);
    }
  }

  async function loadRanks() {
    try {
      const res = await fetch("/api/rank");
      if (!res.ok) {
        throw new Error("rank error");
      }
      renderRank(await res.json());
    } catch (error) {
      if (rankList) {
        rankList.innerHTML = '<div class="empty-state">Không tải được bảng xếp hạng.</div>';
      }
      console.error(error);
    }
  }

  document.querySelectorAll("[data-open-join]").forEach((button) => {
    button.addEventListener("click", openJoinModal);
  });

  document.querySelectorAll("[data-close-join]").forEach((button) => {
    button.addEventListener("click", closeJoinModal);
  });

  const createButtons = document.querySelectorAll("[data-create-room]");
  createButtons.forEach((button) => {
    button.addEventListener("click", async function () {
      try {
        await createRoom();
      } catch (error) {
        console.error(error);
        alert("Không thể tạo phòng lúc này.");
      }
    });
  });

  const joinSubmit = document.querySelector("[data-submit-join]");
  if (joinSubmit) {
    joinSubmit.addEventListener("click", function () {
      joinRoom(roomInput ? roomInput.value : "");
    });
  }

  if (roomInput) {
    roomInput.addEventListener("keydown", function (event) {
      if (event.key === "Enter") {
        event.preventDefault();
        joinRoom(roomInput.value);
      }
    });
  }

  if (joinModal) {
    joinModal.addEventListener("click", function (event) {
      if (event.target === joinModal) {
        closeJoinModal();
      }
    });
  }

  window.CaroHome = {
    joinRoom,
  };

  loadRooms();
  loadRanks();
  setInterval(loadRooms, 5000);
})();
