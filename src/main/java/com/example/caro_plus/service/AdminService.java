package com.example.caro_plus.service;

import com.example.caro_plus.config.GameState;
import com.example.caro_plus.dto.AdminDashboardView;
import com.example.caro_plus.dto.AdminGameDetailView;
import com.example.caro_plus.dto.AdminOverviewMetrics;
import com.example.caro_plus.dto.AdminRoomDetailView;
import com.example.caro_plus.dto.AdminUserDetailView;
import com.example.caro_plus.model.ChatMessage;
import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.RoomPlayer;
import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.ChatMessageRepository;
import com.example.caro_plus.repository.GameRepository;
import com.example.caro_plus.repository.MoveRepository;
import com.example.caro_plus.repository.RoomPlayerRepository;
import com.example.caro_plus.repository.RoomRepository;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final MoveRepository moveRepository;
    private final RoomService roomService;
    private final GameState gameState;

    public AdminService(UserRepository userRepository,
                        RoomRepository roomRepository,
                        GameRepository gameRepository,
                        ChatMessageRepository chatMessageRepository,
                        RoomPlayerRepository roomPlayerRepository,
                        MoveRepository moveRepository,
                        RoomService roomService,
                        GameState gameState) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.gameRepository = gameRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.moveRepository = moveRepository;
        this.roomService = roomService;
        this.gameState = gameState;
    }

    @Transactional(readOnly = true)
    public AdminDashboardView buildDashboard(String userQuery,
                                             String userRole,
                                             String userSort,
                                             int userPage,
                                             String roomQuery,
                                             String roomStatus,
                                             int roomPage,
                                             String gameQuery,
                                             String gameStatus,
                                             int gamePage,
                                             String messageQuery,
                                             Long messageRoomId,
                                             int messagePage) {
        AdminDashboardView view = new AdminDashboardView();

        view.setOverview(buildOverviewMetrics());
        view.setUsers(userRepository.searchAdminUsers(normalize(userQuery), normalize(userRole), userPageable(userPage, userSort)));
        view.setRooms(roomRepository.searchAdminRooms(normalize(roomQuery), normalize(roomStatus),
                PageRequest.of(Math.max(roomPage, 0), 6, Sort.by(Sort.Direction.DESC, "createdAt", "id"))));
        view.setGames(gameRepository.searchAdminGames(normalize(gameQuery), normalize(gameStatus),
                PageRequest.of(Math.max(gamePage, 0), 6, Sort.by(Sort.Direction.DESC, "createdAt", "id"))));
        view.setMessages(chatMessageRepository.searchAdminMessages(normalize(messageQuery), messageRoomId,
                PageRequest.of(Math.max(messagePage, 0), 8, Sort.by(Sort.Direction.DESC, "createdAt", "id"))));
        view.setRecentUsers(userRepository.findTop5ByOrderByCreatedAtDesc());
        view.setRecentRooms(roomRepository.findTop5ByOrderByCreatedAtDesc());
        view.setRecentGames(gameRepository.findTop5ByOrderByCreatedAtDescIdDesc());
        view.setRecentMessages(chatMessageRepository.findTop5ByOrderByCreatedAtDescIdDesc());
        view.setTopPointUsers(userRepository.findTop5ByOrderByRankScoreDescWinDescDrawDescLoseAscIdAsc());
        view.setTopStarUsers(userRepository.findTop5ByOrderBySupportPointsDescRankScoreDescIdAsc());
        return view;
    }

    @Transactional(readOnly = true)
    public AdminOverviewMetrics getOverviewMetrics() {
        return buildOverviewMetrics();
    }

    @Transactional(readOnly = true)
    public Page<User> getUsersPage(String userQuery, String userRole, String userSort, int userPage) {
        return userRepository.searchAdminUsers(normalize(userQuery), normalize(userRole), userPageable(userPage, userSort));
    }

    @Transactional(readOnly = true)
    public Page<Room> getRoomsPage(String roomQuery, String roomStatus, int roomPage) {
        return roomRepository.searchAdminRooms(normalize(roomQuery), normalize(roomStatus),
                PageRequest.of(Math.max(roomPage, 0), 6, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Transactional(readOnly = true)
    public Page<Game> getGamesPage(String gameQuery, String gameStatus, int gamePage) {
        return gameRepository.searchAdminGames(normalize(gameQuery), normalize(gameStatus),
                PageRequest.of(Math.max(gamePage, 0), 6, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Transactional(readOnly = true)
    public Page<ChatMessage> getMessagesPage(String messageQuery, Long messageRoomId, int messagePage) {
        return chatMessageRepository.searchAdminMessages(normalize(messageQuery), messageRoomId,
                PageRequest.of(Math.max(messagePage, 0), 8, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Transactional(readOnly = true)
    public List<User> getRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Room> getRecentRooms() {
        return roomRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Game> getRecentGames() {
        return gameRepository.findTop5ByOrderByCreatedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getRecentMessages() {
        return chatMessageRepository.findTop5ByOrderByCreatedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<User> getTopPointUsers() {
        return userRepository.findTop5ByOrderByRankScoreDescWinDescDrawDescLoseAscIdAsc();
    }

    @Transactional(readOnly = true)
    public List<User> getTopStarUsers() {
        return userRepository.findTop5ByOrderBySupportPointsDescRankScoreDescIdAsc();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailView getUserDetail(Long userId) {
        User user = getUserOrThrow(userId);
        AdminUserDetailView view = new AdminUserDetailView();
        view.setUser(user);
        view.setRecentGames(gameRepository.findTop10ByPlayerXOrPlayerOOrderByCreatedAtDescIdDesc(user, user));
        view.setActiveRoomMemberships(roomPlayerRepository.findByPlayer(user));
        return view;
    }

    @Transactional(readOnly = true)
    public AdminRoomDetailView getRoomDetail(Long roomId) {
        Room room = roomService.syncRoomState(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Không tìm thấy phòng.");
        }

        AdminRoomDetailView view = new AdminRoomDetailView();
        view.setRoom(room);
        view.setRoomPlayers(roomPlayerRepository.findByRoom(room));
        view.setLatestGame(gameRepository.findTopByRoomOrderByIdDesc(room).orElse(null));
        view.setRecentMessages(chatMessageRepository.findTop20ByRoomOrderByCreatedAtDescIdDesc(room));
        return view;
    }

    @Transactional(readOnly = true)
    public AdminGameDetailView getGameDetail(Long gameId) {
        Game game = getGameOrThrow(gameId);
        AdminGameDetailView view = new AdminGameDetailView();
        view.setGame(game);
        view.setMoves(moveRepository.findByGameOrderByMoveOrderAsc(game));
        return view;
    }

    @Transactional
    public void updateUserRole(Long userId, String role, String actingUsername) {
        User user = getUserOrThrow(userId);
        String normalizedRole = normalizeRole(role);
        if (user.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new IllegalStateException("Bạn không thể tự đổi quyền của chính mình.");
        }
        if ("ADMIN".equals(user.getRole()) && "USER".equals(normalizedRole) && userRepository.countByRole("ADMIN") <= 1) {
            throw new IllegalStateException("Hệ thống phải luôn còn ít nhất một tài khoản admin.");
        }
        user.setRole(normalizedRole);
        userRepository.save(user);
    }

    @Transactional
    public void lockUser(Long userId, String actingUsername) {
        User user = getUserOrThrow(userId);
        if (user.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new IllegalStateException("Bạn không thể tự khóa tài khoản của mình.");
        }
        if ("ADMIN".equals(user.getRole()) && userRepository.countByRole("ADMIN") <= 1) {
            throw new IllegalStateException("Không thể khóa admin cuối cùng của hệ thống.");
        }
        user.setLocked(true);
        userRepository.save(user);
    }

    @Transactional
    public void unlockUser(Long userId) {
        User user = getUserOrThrow(userId);
        user.setLocked(false);
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void resetUserStars(Long userId) {
        User user = getUserOrThrow(userId);
        user.setSupportPoints(5.0);
        userRepository.save(user);
    }

    @Transactional
    public void resetUserStats(Long userId) {
        User user = getUserOrThrow(userId);
        user.setRankScore(0);
        user.setWin(0);
        user.setLose(0);
        user.setDraw(0);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId, String actingUsername) {
        User user = getUserOrThrow(userId);
        if (user.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new IllegalStateException("Bạn không thể tự xóa tài khoản của mình.");
        }
        if ("ADMIN".equals(user.getRole()) && userRepository.countByRole("ADMIN") <= 1) {
            throw new IllegalStateException("Không thể xóa admin cuối cùng của hệ thống.");
        }
        if (gameRepository.existsRelatedToUser(user)
                || chatMessageRepository.existsBySender(user)
                || !roomPlayerRepository.findByPlayer(user).isEmpty()
                || isReferencedByRoom(user)) {
            throw new IllegalStateException("Tài khoản này đang có lịch sử hoặc liên kết phòng/trận, không thể xóa an toàn.");
        }
        userRepository.delete(user);
    }

    @Transactional
    public void closeRoom(Long roomId) {
        Room room = getRoomOrThrow(roomId);
        markPlayingGamesAbandoned(room);
        List<RoomPlayer> players = roomPlayerRepository.findByRoom(room);
        if (!players.isEmpty()) {
            roomPlayerRepository.deleteAll(players);
        }
        room.setHost(null);
        room.setPlayer2(null);
        room.setStatus("waiting");
        roomRepository.save(room);
        gameState.removeRoom(roomId);
    }

    @Transactional
    public void resetRoom(Long roomId) {
        closeRoom(roomId);
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        Room room = getRoomOrThrow(roomId);
        if (gameRepository.countByRoom(room) > 0 || chatMessageRepository.existsByRoom(room)
                || roomPlayerRepository.countByRoom(room) > 0 || room.getHost() != null || room.getPlayer2() != null) {
            throw new IllegalStateException("Phòng còn dữ liệu liên quan, hãy force reset trước thay vì xóa.");
        }
        roomRepository.delete(room);
    }

    @Transactional
    public void cancelGame(Long gameId) {
        Game game = getGameOrThrow(gameId);
        if ("FINISHED".equalsIgnoreCase(game.getStatus())) {
            throw new IllegalStateException("Không thể hủy trận đã hoàn tất vì sẽ làm sai lệch điểm và thống kê.");
        }
        game.setStatus("ABANDONED");
        game.setFinishedAt(new Date());
        gameRepository.save(game);
        if (game.getRoom() != null) {
            gameState.removeRoom(game.getRoom().getId());
            roomService.syncRoomState(game.getRoom().getId());
        }
    }

    @Transactional
    public void deleteGame(Long gameId) {
        Game game = getGameOrThrow(gameId);
        if ("FINISHED".equalsIgnoreCase(game.getStatus())) {
            throw new IllegalStateException("Không thể xóa trận đã hoàn tất vì sẽ làm sai lệch điểm và thống kê.");
        }
        moveRepository.deleteByGame(game);
        if (game.getRoom() != null) {
            gameState.removeRoom(game.getRoom().getId());
            roomService.syncRoomState(game.getRoom().getId());
        }
        gameRepository.delete(game);
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn."));
        chatMessageRepository.delete(message);
    }

    private AdminOverviewMetrics buildOverviewMetrics() {
        AdminOverviewMetrics metrics = new AdminOverviewMetrics();
        List<User> allUsers = userRepository.findAll();
        metrics.setTotalUsers(allUsers.size());
        metrics.setAdminUsers(userRepository.countByRole("ADMIN"));
        metrics.setLockedUsers(userRepository.countByLockedTrue());
        metrics.setActiveUsers(roomPlayerRepository.countDistinctPlayersInRooms());
        metrics.setTotalRooms(roomRepository.count());
        metrics.setWaitingRooms(roomRepository.countByStatus("waiting"));
        metrics.setFullRooms(roomRepository.countByStatus("full"));
        metrics.setPlayingRooms(roomRepository.countByStatus("playing"));
        metrics.setTotalMatches(gameRepository.count());
        metrics.setPlayingMatches(gameRepository.countByStatus("PLAYING"));
        metrics.setFinishedMatches(gameRepository.countByStatus("FINISHED"));
        metrics.setTotalMessages(chatMessageRepository.count());
        metrics.setAveragePoints(allUsers.stream().mapToInt(User::getRankScore).average().orElse(0));
        metrics.setAverageStars(allUsers.stream().mapToDouble(User::getSupportPoints).average().orElse(0));
        metrics.setTotalWins(allUsers.stream().mapToInt(User::getWin).sum());
        metrics.setTotalLosses(allUsers.stream().mapToInt(User::getLose).sum());
        metrics.setTotalDraws(allUsers.stream().mapToInt(User::getDraw).sum());
        metrics.setTopPlayer(userRepository.findTop5ByOrderByRankScoreDescWinDescDrawDescLoseAscIdAsc()
                .stream()
                .findFirst()
                .orElse(null));
        return metrics;
    }

    private Pageable userPageable(int page, String sort) {
        String normalizedSort = normalize(sort);
        Sort userSort = switch (normalizedSort) {
            case "points" -> Sort.by(Sort.Direction.DESC, "rankScore").and(Sort.by(Sort.Direction.DESC, "win"));
            case "stars" -> Sort.by(Sort.Direction.DESC, "supportPoints").and(Sort.by(Sort.Direction.DESC, "rankScore"));
            case "wins" -> Sort.by(Sort.Direction.DESC, "win").and(Sort.by(Sort.Direction.DESC, "rankScore"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        };
        return PageRequest.of(Math.max(page, 0), 8, userSort);
    }

    private String normalizeRole(String role) {
        String normalized = normalize(role).toUpperCase();
        if (!"ADMIN".equals(normalized) && !"USER".equals(normalized)) {
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }

    private Room getRoomOrThrow(Long roomId) {
        Room room = roomService.syncRoomState(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Không tìm thấy phòng.");
        }
        return room;
    }

    private Game getGameOrThrow(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trận đấu."));
    }

    private boolean isReferencedByRoom(User user) {
        return roomRepository.findAll().stream().anyMatch(room ->
                (room.getHost() != null && room.getHost().getId().equals(user.getId()))
                        || (room.getPlayer2() != null && room.getPlayer2().getId().equals(user.getId())));
    }

    private void markPlayingGamesAbandoned(Room room) {
        List<Game> playingGames = gameRepository.findByRoomAndStatus(room, "PLAYING");
        if (playingGames.isEmpty()) {
            return;
        }

        Date finishedAt = new Date();
        for (Game game : playingGames) {
            game.setStatus("ABANDONED");
            game.setFinishedAt(finishedAt);
        }
        gameRepository.saveAll(playingGames);
    }
}
