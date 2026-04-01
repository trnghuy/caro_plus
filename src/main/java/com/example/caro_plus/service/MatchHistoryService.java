package com.example.caro_plus.service;

import com.example.caro_plus.dto.MatchHistoryItemResponse;
import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.GameRepository;
import com.example.caro_plus.repository.MoveRepository;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatchHistoryService {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id"));

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final UserRepository userRepository;

    public MatchHistoryService(GameRepository gameRepository, MoveRepository moveRepository, UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<MatchHistoryItemResponse> getMatchHistoryForCurrentUser(String username, Pageable pageable) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nguoi choi khong ton tai"));

        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);

        Page<Game> gamePage = gameRepository.findByPlayerXOrPlayerO(currentUser, currentUser, effectivePageable);
        List<Game> games = gamePage.getContent();
        Map<Long, Long> moveCounts = loadMoveCounts(games);

        List<MatchHistoryItemResponse> items = games.stream()
                .map(game -> toHistoryItem(game, currentUser, moveCounts.getOrDefault(game.getId(), 0L)))
                .toList();

        return new PageImpl<>(items, effectivePageable, gamePage.getTotalElements());
    }

    private Map<Long, Long> loadMoveCounts(List<Game> games) {
        if (games.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> gameIds = games.stream()
                .map(Game::getId)
                .toList();

        Map<Long, Long> moveCounts = new HashMap<>();
        for (Object[] row : moveRepository.countMovesByGameIds(gameIds)) {
            Long gameId = (Long) row[0];
            Long totalMoves = (Long) row[1];
            moveCounts.put(gameId, totalMoves);
        }
        return moveCounts;
    }

    private MatchHistoryItemResponse toHistoryItem(Game game, User currentUser, long totalMoves) {
        MatchHistoryItemResponse item = new MatchHistoryItemResponse();
        boolean isPlayerX = game.getPlayerX() != null && game.getPlayerX().getId().equals(currentUser.getId());
        User opponent = isPlayerX ? game.getPlayerO() : game.getPlayerX();

        item.setGameId(game.getId());
        item.setRoomId(game.getRoom() != null ? game.getRoom().getId() : null);
        item.setOpponentUsername(opponent != null ? opponent.getUsername() : "Chua co doi thu");
        item.setPlayerSymbol(isPlayerX ? "X" : "O");
        item.setResult(resolveResult(game, currentUser));
        item.setStartedAt(game.getCreatedAt());
        item.setEndedAt(game.getFinishedAt());
        item.setTotalMoves(totalMoves);
        item.setStatus(game.getStatus());
        item.setRankScoreDelta(resolveRankScoreDelta(game, currentUser));
        item.setSupportPointsDelta(resolveSupportPointsDelta(game, currentUser));
        return item;
    }

    private String resolveResult(Game game, User currentUser) {
        if (!"FINISHED".equalsIgnoreCase(game.getStatus())) {
            return null;
        }

        if (game.getWinner() == null) {
            return "DRAW";
        }

        return game.getWinner().getId().equals(currentUser.getId()) ? "WIN" : "LOSE";
    }

    private Integer resolveRankScoreDelta(Game game, User currentUser) {
        if (!isReliableFinishedMatch(game)) {
            return null;
        }

        if (game.getWinner() == null) {
            return 1;
        }

        return game.getWinner().getId().equals(currentUser.getId()) ? 3 : -1;
    }

    private Double resolveSupportPointsDelta(Game game, User currentUser) {
        if (!isReliableFinishedMatch(game)) {
            return null;
        }

        if (game.getWinner() == null) {
            return 0.5;
        }

        return game.getWinner().getId().equals(currentUser.getId()) ? 1.0 : -1.0;
    }

    private boolean isReliableFinishedMatch(Game game) {
        return "FINISHED".equalsIgnoreCase(game.getStatus())
                && game.getPlayerX() != null
                && game.getPlayerO() != null;
    }
}
