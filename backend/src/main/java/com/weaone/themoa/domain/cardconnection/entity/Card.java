package com.weaone.themoa.domain.cardconnection.entity;

import com.weaone.themoa.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카드 1장. {@code card_connection_id + card_number_masked} UNIQUE가 재수집 시 같은 카드를 같은 행으로
 * 찾기 위한 식별키다(cardtransaction.md §2) — CODEF 응답에 안정적인 카드 식별자가 없어 마스킹 번호가
 * 유일한 자연키다. 이게 없으면 재수집마다 카드가 새로 생겨 card_id가 바뀌고, 거래 UNIQUE가 무력화된다.
 */
@Entity
@Table(name = "cards",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cards_connection_masked",
                        columnNames = {"card_connection_id", "card_number_masked"}),
                @UniqueConstraint(name = "uk_cards_id_member", columnNames = {"id", "member_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_connection_id", nullable = false)
    private CardConnection cardConnection;

    @Column(name = "card_name", nullable = false, length = 100)
    private String cardName;

    @Column(name = "card_number_masked", nullable = false, length = 20)
    private String cardNumberMasked;

    private Card(Member member, CardConnection cardConnection, String cardName, String cardNumberMasked) {
        this.member = member;
        this.cardConnection = cardConnection;
        this.cardName = cardName;
        this.cardNumberMasked = cardNumberMasked;
    }

    public static Card observe(Member member, CardConnection cardConnection, String cardName, String cardNumberMasked) {
        return new Card(member, cardConnection, cardName, cardNumberMasked);
    }
}
