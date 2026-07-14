package com.weaone.themoa.domain.cardconnection.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CardConnectionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 14, 10, 0);

    @Test
    @DisplayName("최초 연결은 ACTIVE 상태로 시작한다")
    void connectStartsActive() {
        CardConnection connection = CardConnection.connect(null, null, "connected-id-1", NOW);

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
        assertThat(connection.getConnectedId()).isEqualTo("connected-id-1");
        assertThat(connection.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("재연결에 성공하면 connectedId를 갱신하고 ACTIVE로 되돌아간다")
    void reconnectRestoresActive() {
        CardConnection connection = CardConnection.connect(null, null, "old-id", NOW);
        connection.markLocked();
        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.LOCKED);

        connection.reconnect("new-id");

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
        assertThat(connection.getConnectedId()).isEqualTo("new-id");
    }

    @Test
    @DisplayName("카드사 계정 잠금 신호를 받으면 LOCKED로 전환된다")
    void marksLocked() {
        CardConnection connection = CardConnection.connect(null, null, "connected-id-1", NOW);

        connection.markLocked();

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.LOCKED);
    }
}
