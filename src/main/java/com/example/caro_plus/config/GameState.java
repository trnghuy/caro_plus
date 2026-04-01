package com.example.caro_plus.config;

import com.example.caro_plus.dto.GameSnapshotResponse;
import com.example.caro_plus.model.GameMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameState {

    private static final int BOARD_SIZE = 20;
    private final Map<Long, RoomSession> sessions = new ConcurrentHashMap<>();

    public synchronized void initializeRoom(Long roomId, String playerX, String playerO) {
        sessions.put(roomId, new RoomSession(playerX, playerO));
    }

    public synchronized boolean hasRoom(Long roomId) {
        return sessions.containsKey(roomId);
    }

    public synchronized void removeRoom(Long roomId) {
        sessions.remove(roomId);
    }

    public synchronized String getTurn(Long roomId) {
        RoomSession session = sessions.get(roomId);
        return session == null ? "X" : session.currentTurn;
    }

    public synchronized String getPlayerSymbol(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null) {
            return null;
        }
        return resolvePlayerSymbol(session, username);
    }

    public synchronized boolean reconnectPlayer(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null) {
            return false;
        }

        if (username.equals(session.playerX)) {
            boolean wasDisconnected = !session.playerXConnected;
            session.playerXConnected = true;
            return wasDisconnected;
        }

        if (username.equals(session.playerO)) {
            boolean wasDisconnected = !session.playerOConnected;
            session.playerOConnected = true;
            return wasDisconnected;
        }

        return false;
    }

    public synchronized boolean disconnectPlayer(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null) {
            return false;
        }

        if (username.equals(session.playerX)) {
            boolean wasConnected = session.playerXConnected;
            session.playerXConnected = false;
            return wasConnected;
        }

        if (username.equals(session.playerO)) {
            boolean wasConnected = session.playerOConnected;
            session.playerOConnected = false;
            return wasConnected;
        }

        return false;
    }

    public synchronized boolean isOpponentConnected(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null) {
            return true;
        }

        if (username.equals(session.playerX)) {
            return session.playerOConnected;
        }

        if (username.equals(session.playerO)) {
            return session.playerXConnected;
        }

        return true;
    }

    public synchronized boolean areAllPlayersDisconnected(Long roomId) {
        RoomSession session = sessions.get(roomId);
        if (session == null) {
            return false;
        }

        return !session.playerXConnected && !session.playerOConnected;
    }

    public synchronized GameSnapshotResponse getSnapshot(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null || resolvePlayerSymbol(session, username) == null) {
            return null;
        }

        GameSnapshotResponse response = new GameSnapshotResponse();
        response.setBoard(copyBoard(session.board));
        response.setCurrentTurn(session.currentTurn);
        response.setWinner(session.winner);
        response.setLastMoveX(session.lastMoveX);
        response.setLastMoveY(session.lastMoveY);
        response.setOpponentConnected(isOpponentConnected(roomId, username));
        return response;
    }

    public synchronized GameMessage makeMove(Long roomId, String username, Integer x, Integer y) {
        RoomSession session = sessions.get(roomId);
        if (session == null || x == null || y == null) {
            return null;
        }

        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE || session.winner != null) {
            return null;
        }

        if (!session.playerXConnected || !session.playerOConnected) {
            return null;
        }

        String symbol = resolvePlayerSymbol(session, username);
        if (symbol == null || !symbol.equals(session.currentTurn) || session.board[x][y] != null) {
            return null;
        }

        session.board[x][y] = symbol;
        session.moveHistory.add(new MoveRecord(username, symbol, x, y));
        session.lastMoveX = x;
        session.lastMoveY = y;

        GameMessage response = new GameMessage();
        response.setRoomId(roomId.toString());
        response.setSender(username);
        response.setPlayer(symbol);
        response.setX(x);
        response.setY(y);

        if (isWinningMove(session.board, x, y, symbol)) {
            session.winner = username;
            response.setType("WIN");
            response.setWinner(username);
            response.setCurrentTurn(symbol);
            response.setContent(username + " wins");
            return response;
        }

        session.currentTurn = "X".equals(symbol) ? "O" : "X";
        response.setType("MOVE");
        response.setCurrentTurn(session.currentTurn);
        return response;
    }

    public synchronized GameMessage undoLastMove(Long roomId, String username) {
        if (!canUndoLastMove(roomId, username)) {
            return null;
        }

        RoomSession session = sessions.get(roomId);
        String requesterSymbol = resolvePlayerSymbol(session, username);
        MoveRecord lastMove = session.moveHistory.remove(session.moveHistory.size() - 1);
        MoveRecord previousMove = session.moveHistory.remove(session.moveHistory.size() - 1);
        session.board[lastMove.x][lastMove.y] = null;
        session.board[previousMove.x][previousMove.y] = null;
        session.currentTurn = requesterSymbol;
        session.winner = null;
        session.replayRequester = null;

        if (session.moveHistory.isEmpty()) {
            session.lastMoveX = null;
            session.lastMoveY = null;
        } else {
            MoveRecord remainingLastMove = session.moveHistory.get(session.moveHistory.size() - 1);
            session.lastMoveX = remainingLastMove.x;
            session.lastMoveY = remainingLastMove.y;
        }

        GameMessage response = new GameMessage();
        response.setType("UNDO");
        response.setRoomId(roomId.toString());
        response.setSender(username);
        response.setCurrentTurn(session.currentTurn);
        response.setBoard(copyBoard(session.board));
        response.setLastMoveX(session.lastMoveX);
        response.setLastMoveY(session.lastMoveY);
        response.setContent(lastMove.sender + "," + previousMove.sender);
        return response;
    }

    public synchronized boolean canUndoLastMove(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        String symbol = session == null ? null : resolvePlayerSymbol(session, username);
        if (session == null || symbol == null) {
            return false;
        }

        if (session.winner != null || session.moveHistory.size() < 2) {
            return false;
        }

        return session.playerXConnected && session.playerOConnected && symbol.equals(session.currentTurn);
    }

    public synchronized SuggestedMove suggestMove(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null) {
            return null;
        }

        String symbol = resolvePlayerSymbol(session, username);
        if (symbol == null || session.winner != null) {
            return null;
        }

        if (!session.playerXConnected || !session.playerOConnected) {
            return null;
        }

        if (!symbol.equals(session.currentTurn)) {
            return null;
        }

        if (isBoardFull(session.board)) {
            return null;
        }

        int[] move = findWinningMove(session.board, symbol);
        if (move == null) {
            String opponentSymbol = "X".equals(symbol) ? "O" : "X";
            move = findWinningMove(session.board, opponentSymbol);
        }
        if (move == null) {
            move = findStrategicMove(session.board, symbol);
        }
        if (move == null) {
            move = findFirstEmptyCell(session.board);
        }
        if (move == null) {
            return null;
        }

        return new SuggestedMove(move[0], move[1]);
    }

    public synchronized GameMessage requestReplay(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null) {
            return null;
        }

        String symbol = resolvePlayerSymbol(session, username);
        if (symbol == null || session.replayRequester != null) {
            return null;
        }

        if (!session.playerXConnected || !session.playerOConnected) {
            return null;
        }

        session.replayRequester = username;

        GameMessage response = new GameMessage();
        response.setType("REPLAY_REQUEST");
        response.setRoomId(roomId.toString());
        response.setSender(username);
        response.setPlayer(symbol);
        return response;
    }

    public synchronized GameMessage acceptReplay(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null || session.replayRequester == null || session.replayRequester.equals(username)) {
            return null;
        }

        if (resolvePlayerSymbol(session, username) == null) {
            return null;
        }

        resetBoard(session);

        GameMessage response = new GameMessage();
        response.setType("REPLAY_ACCEPTED");
        response.setRoomId(roomId.toString());
        response.setSender(username);
        response.setCurrentTurn(session.currentTurn);
        return response;
    }

    public synchronized GameMessage declineReplay(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null || session.replayRequester == null || session.replayRequester.equals(username)) {
            return null;
        }

        if (resolvePlayerSymbol(session, username) == null) {
            return null;
        }

        String requester = session.replayRequester;
        session.replayRequester = null;

        GameMessage response = new GameMessage();
        response.setType("REPLAY_DECLINED");
        response.setRoomId(roomId.toString());
        response.setSender(username);
        response.setContent(requester);
        return response;
    }

    public synchronized GameMessage createPlayerLeftMessage(Long roomId, String username) {
        RoomSession session = sessions.get(roomId);
        if (session == null || resolvePlayerSymbol(session, username) == null) {
            return null;
        }

        GameMessage response = new GameMessage();
        response.setType("PLAYER_LEFT");
        response.setRoomId(roomId.toString());
        response.setSender(username);
        return response;
    }

    private String resolvePlayerSymbol(RoomSession session, String username) {
        if (username.equals(session.playerX)) {
            return "X";
        }
        if (username.equals(session.playerO)) {
            return "O";
        }
        return null;
    }

    private boolean isWinningMove(String[][] board, int x, int y, String symbol) {
        return countLine(board, x, y, 1, 0, symbol) + countLine(board, x, y, -1, 0, symbol) - 1 >= 5
                || countLine(board, x, y, 0, 1, symbol) + countLine(board, x, y, 0, -1, symbol) - 1 >= 5
                || countLine(board, x, y, 1, 1, symbol) + countLine(board, x, y, -1, -1, symbol) - 1 >= 5
                || countLine(board, x, y, 1, -1, symbol) + countLine(board, x, y, -1, 1, symbol) - 1 >= 5;
    }

    private int[] findWinningMove(String[][] board, String symbol) {
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] != null) {
                    continue;
                }

                board[x][y] = symbol;
                boolean winningMove = isWinningMove(board, x, y, symbol);
                board[x][y] = null;

                if (winningMove) {
                    return new int[] { x, y };
                }
            }
        }
        return null;
    }

    private int[] findStrategicMove(String[][] board, String symbol) {
        if (isBoardEmpty(board)) {
            return new int[] { BOARD_SIZE / 2, BOARD_SIZE / 2 };
        }

        String opponentSymbol = "X".equals(symbol) ? "O" : "X";
        List<int[]> candidates = new ArrayList<>();
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] == null && hasAdjacentStone(board, x, y, 2)) {
                    candidates.add(new int[] { x, y });
                }
            }
        }

        if (candidates.isEmpty()) {
            return findFirstEmptyCell(board);
        }

        return candidates.stream()
                .max(Comparator.comparingInt(candidate -> evaluateStrategicMove(board, candidate[0], candidate[1], symbol, opponentSymbol)))
                .orElse(candidates.get(0));
    }

    private int evaluateStrategicMove(String[][] board, int x, int y, String symbol, String opponentSymbol) {
        int ownPressure = scorePlacement(board, x, y, symbol);
        int defensivePressure = scorePlacement(board, x, y, opponentSymbol);
        int centerBias = BOARD_SIZE - (Math.abs(x - BOARD_SIZE / 2) + Math.abs(y - BOARD_SIZE / 2));
        return ownPressure * 4 + defensivePressure * 2 + centerBias;
    }

    private int scorePlacement(String[][] board, int x, int y, String symbol) {
        board[x][y] = symbol;
        int score = 0;
        score += evaluateLine(board, x, y, 1, 0, symbol);
        score += evaluateLine(board, x, y, 0, 1, symbol);
        score += evaluateLine(board, x, y, 1, 1, symbol);
        score += evaluateLine(board, x, y, 1, -1, symbol);
        board[x][y] = null;
        return score;
    }

    private int evaluateLine(String[][] board, int x, int y, int dx, int dy, String symbol) {
        int forward = countDirection(board, x, y, dx, dy, symbol);
        int backward = countDirection(board, x, y, -dx, -dy, symbol);
        int total = forward + backward + 1;
        int openEnds = 0;

        if (isOpenEnd(board, x + dx * (forward + 1), y + dy * (forward + 1))) {
            openEnds++;
        }
        if (isOpenEnd(board, x - dx * (backward + 1), y - dy * (backward + 1))) {
            openEnds++;
        }

        if (total >= 5) {
            return 200000;
        }
        if (total == 4 && openEnds == 2) {
            return 40000;
        }
        if (total == 4 && openEnds == 1) {
            return 16000;
        }
        if (total == 3 && openEnds == 2) {
            return 7000;
        }
        if (total == 3 && openEnds == 1) {
            return 2400;
        }
        if (total == 2 && openEnds == 2) {
            return 550;
        }
        if (total == 2 && openEnds == 1) {
            return 160;
        }
        if (openEnds == 2) {
            return 30;
        }
        if (openEnds == 1) {
            return 10;
        }
        return 0;
    }

    private int countDirection(String[][] board, int startX, int startY, int dx, int dy, String symbol) {
        int count = 0;
        int x = startX + dx;
        int y = startY + dy;

        while (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE && symbol.equals(board[x][y])) {
            count++;
            x += dx;
            y += dy;
        }

        return count;
    }

    private boolean isOpenEnd(String[][] board, int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE && board[x][y] == null;
    }

    private boolean hasAdjacentStone(String[][] board, int startX, int startY, int radius) {
        for (int x = Math.max(0, startX - radius); x <= Math.min(BOARD_SIZE - 1, startX + radius); x++) {
            for (int y = Math.max(0, startY - radius); y <= Math.min(BOARD_SIZE - 1, startY + radius); y++) {
                if (!(x == startX && y == startY) && board[x][y] != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBoardFull(String[][] board) {
        for (String[] row : board) {
            for (String cell : row) {
                if (cell == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isBoardEmpty(String[][] board) {
        for (String[] row : board) {
            for (String cell : row) {
                if (cell != null) {
                    return false;
                }
            }
        }
        return true;
    }

    private int[] findFirstEmptyCell(String[][] board) {
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] == null) {
                    return new int[] { x, y };
                }
            }
        }
        return null;
    }

    private int countLine(String[][] board, int startX, int startY, int dx, int dy, String symbol) {
        int count = 0;
        int x = startX;
        int y = startY;

        while (x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE && symbol.equals(board[x][y])) {
            count++;
            x += dx;
            y += dy;
        }

        return count;
    }

    private void resetBoard(RoomSession session) {
        session.board = new String[BOARD_SIZE][BOARD_SIZE];
        session.currentTurn = "X";
        session.winner = null;
        session.replayRequester = null;
        session.moveHistory = new ArrayList<>();
        session.lastMoveX = null;
        session.lastMoveY = null;
    }

    private String[][] copyBoard(String[][] source) {
        String[][] snapshot = new String[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(source[i], 0, snapshot[i], 0, BOARD_SIZE);
        }
        return snapshot;
    }

    private static class RoomSession {
        private String[][] board = new String[BOARD_SIZE][BOARD_SIZE];
        private final String playerX;
        private final String playerO;
        private String currentTurn = "X";
        private String winner;
        private String replayRequester;
        private List<MoveRecord> moveHistory = new ArrayList<>();
        private boolean playerXConnected = true;
        private boolean playerOConnected = true;
        private Integer lastMoveX;
        private Integer lastMoveY;

        private RoomSession(String playerX, String playerO) {
            this.playerX = playerX;
            this.playerO = playerO;
        }
    }

    public static class SuggestedMove {
        private final int x;
        private final int y;

        public SuggestedMove(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    private static class MoveRecord {
        private final String sender;
        private final String symbol;
        private final int x;
        private final int y;

        private MoveRecord(String sender, String symbol, int x, int y) {
            this.sender = sender;
            this.symbol = symbol;
            this.x = x;
            this.y = y;
        }
    }
}
