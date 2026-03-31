package com.example.caro_plus.config;

import com.example.caro_plus.dto.GameSnapshotResponse;
import com.example.caro_plus.model.GameMessage;
import org.springframework.stereotype.Component;

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
        private boolean playerXConnected = true;
        private boolean playerOConnected = true;
        private Integer lastMoveX;
        private Integer lastMoveY;

        private RoomSession(String playerX, String playerO) {
            this.playerX = playerX;
            this.playerO = playerO;
        }
    }
}
